package com.rudra.fintechvar.notification


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rudra.fintechvar.db.database.AppDatabase
import com.rudra.fintechvar.db.modal.NotificationEntity




class WeeklyReportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)

            // Fetch weekly totals
            val weeklyExpense = db.transactionDao().getWeeklyExpense() ?: 0.0
            val weeklyIncome = db.transactionDao().getWeeklyIncome() ?: 0.0

            val message = "Weekly Income: ₹%.2f\nWeekly Expense: ₹%.2f".format(
                weeklyIncome,
                weeklyExpense
            )

            // Save notification locally
            db.notificationDao().insert(
                NotificationEntity(
                    title = "Weekly Report",
                    description = message,
                    time = System.currentTimeMillis(),
                    isRead = false
                )
            )

            // Show system notification
            if (canPostNotifications(applicationContext)) {
                NotificationUtil.show(
                    applicationContext,
                    "Weekly Expense Report",
                    message
                )
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }



    private fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

}

