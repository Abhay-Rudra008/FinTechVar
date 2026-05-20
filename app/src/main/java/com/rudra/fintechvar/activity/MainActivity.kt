package com.rudra.fintechvar.activity

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rudra.fintechvar.R
import com.rudra.fintechvar.databinding.ActivityMainBinding
import com.rudra.fintechvar.db.database.AppDatabase
import com.rudra.fintechvar.db.repository.ExpenseRepository
import com.rudra.fintechvar.db.viewmodal.ExpenseViewModel
import com.rudra.fintechvar.db.viewmodalfactory.ExpenseViewModelFactory
import com.rudra.fintechvar.notification.DailyReportWorker
import com.rudra.fintechvar.notification.WeeklyReportWorker
import com.rudra.fintechvar.utils.BaseActivity
import java.util.Calendar
import java.util.concurrent.TimeUnit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rudra.fintechvar.utils.Permissions


class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    lateinit var expenseViewModel: ExpenseViewModel
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupViewModel()
        setupNavigation()

        Permissions.checkNotificationPermission(this) {
            scheduleReports()
        }

        checkNotificationListenerPermission()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = systemBars.bottom)
            insets
        }
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(this)
        val repository = ExpenseRepository(
            categoryDao = database.categoryDao(),
            transactionDao = database.transactionDao()
        )
        val factory = ExpenseViewModelFactory(repository)

        expenseViewModel = ViewModelProvider(this, factory)[ExpenseViewModel::class.java]
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        if (intent.getBooleanExtra("openWalletFragment", false)) {
            navController.navigate(R.id.categoryFragment)
        }
    }

    private fun checkNotificationListenerPermission() {
        val isListenerEnabled = NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName)

        if (!isListenerEnabled) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Permission Required")
                .setMessage("This app collects and reads your incoming notification data from banking and UPI apps to automatically track your expenses and income. This data is processed locally on your device to generate your financial reports.\n\nDo you agree to grant Notification Access?")
                .setCancelable(false)
                .setPositiveButton("Agree & Continue") { _, _ ->
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                .setNegativeButton("No, Thanks", null)
                .show()
        }
    }

    private fun scheduleReports() {
        scheduleDailyReport()
        scheduleWeeklyReport()
    }

    private fun scheduleDailyReport() {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val delay = target.timeInMillis - now.timeInMillis

        val dailyWork = PeriodicWorkRequestBuilder<DailyReportWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("daily_report")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_report",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWork
        )
    }

    private fun scheduleWeeklyReport() {
        val now = Calendar.getInstance()
        val nextSunday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (before(now)) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        val delay = nextSunday.timeInMillis - now.timeInMillis

        val weeklyWork = PeriodicWorkRequestBuilder<WeeklyReportWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("weekly_report")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "weekly_report",
            ExistingPeriodicWorkPolicy.KEEP,
            weeklyWork
        )
    }
}