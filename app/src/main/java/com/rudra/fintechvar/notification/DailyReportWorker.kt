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


class DailyReportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)

            // Fetch today's totals
            val todayExpense = db.transactionDao().getTodayExpense() ?: 0.0
            val todayIncome = db.transactionDao().getTodayIncome() ?: 0.0

            val message = "Income: ₹%.2f\nExpense: ₹%.2f".format(todayIncome, todayExpense)

            // Save notification locally
            db.notificationDao().insert(
                NotificationEntity(
                    title = "Daily Report",
                    description = message,
                    time = System.currentTimeMillis(),
                    isRead = false
                )
            )

            // Show system notification
            if (canPostNotifications(applicationContext)) {
                NotificationUtil.show(
                    applicationContext,
                    "Daily Expense Report",
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