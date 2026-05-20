package com.rudra.fintechvar.training

import android.Manifest
import android.app.Notification
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
import com.rudra.fintechvar.db.dao.CategoryDao
import com.rudra.fintechvar.db.database.AppDatabase
import com.rudra.fintechvar.db.modal.CategoryEntity
import com.rudra.fintechvar.db.modal.NotificationEntity
import com.rudra.fintechvar.db.modal.TransactionEntity
import com.rudra.fintechvar.db.repository.NotificationRepository
import com.rudra.fintechvar.notification.NotificationUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class TransactionNotificationService : NotificationListenerService() {

    private lateinit var appDatabase: AppDatabase

    override fun onCreate() {
        super.onCreate()
        appDatabase = AppDatabase.getDatabase(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        // Ignore own app notifications
        if (sbn.packageName == packageName) return

        val text = extractText(sbn) ?: return

        if (isIgnorableMessage(text)) {
            return
        }

        //  EXTRACT AMOUNT
        val amount = extractAmount(text)
        if (amount == null || amount <= 0) {
            return
        }

        val transactionType = ruleBasedType(text) ?: return


        //  AUTO-SAVE TRANSACTION
        CoroutineScope(Dispatchers.IO).launch {
            saveTransactionFromNotification(
                text = text, amount = amount, type = transactionType
            )

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationUtil.showAutoSaved(
                    context = applicationContext, amount = amount, type = transactionType
                )
            }
        }
    }

    private fun isIgnorableMessage(text: String): Boolean {
        val t = text.lowercase()
        val ignoreWords = listOf(
            "otp",
            "failed",
            "declined",
            "request",
            "requested",
            "reminder",
            "due",
            "upcoming",
            "bounce",
            "insufficient",
            "report",
            "code is",
            "verification"
        )
        return ignoreWords.any { t.contains(it) }
    }


    private fun ruleBasedType(text: String): String? {
        val t = text.lowercase()

        val isExpense = listOf(
            "debited",
            "paid",
            "spent",
            "sent to",
            "send to",
            "send",
            "purchase",
            "charged",
            "deducted"
        ).any { t.contains(it) }

        val isIncome = listOf(
            "credited", "received", "refund", "cashback", "added", "deposited"
        ).any { t.contains(it) }

        return when {
            isExpense && !isIncome -> "EXPENSE"
            isIncome && !isExpense -> "INCOME"
            else -> null
        }
    }

    private suspend fun saveTransactionFromNotification(
        text: String, amount: Double, type: String
    ): Boolean {

        val categoryName = inferCategoryName(text)

        val categoryId = getOrCreateCategory(
            appDatabase.categoryDao(), categoryName, type
        )

        val now = System.currentTimeMillis()

        //  Prevent duplicate saves within 2 seconds
        val isDuplicate = appDatabase.transactionDao().isDuplicateWithinWindow(
                amount = amount, type = type, time = now, windowMs = 2000
            ) > 0

        if (isDuplicate) {
            return false
        }

        // Insert Transaction
        val transaction = TransactionEntity(
            amount = amount, type = type, date = now, categoryId = categoryId
        )
        appDatabase.transactionDao().insert(transaction)

        val notificationRepository = NotificationRepository(appDatabase.notificationDao())
        notificationRepository.insert(
            NotificationEntity(
                title = "Transaction Auto Saved",
                description = "₹$amount saved as $type in $categoryName",
                time = System.currentTimeMillis(),
                isRead = false
            )
        )

        return true
    }


    private fun inferCategoryName(text: String): String {
        val t = text.lowercase()
        return when {
            listOf(
                "swiggy", "zomato", "eatfit", "mcdonalds", "dominos"
            ).any { t.contains(it) } -> "Food"

            listOf(
                "blinkit", "zepto", "instamart", "bigbasket", "grocery", "dmart"
            ).any { t.contains(it) } -> "Grocery"

            listOf("uber", "ola", "rapido", "namma metro", "irctc", "makemytrip").any {
                t.contains(
                    it
                )
            } -> "Travel"

            listOf(
                "amazon", "flipkart", "myntra", "meesho", "ajio", "reliance"
            ).any { t.contains(it) } -> "Shopping"

            listOf("netflix", "spotify", "prime", "bookmyshow", "hotstar", "pvr").any {
                t.contains(
                    it
                )
            } -> "Entertainment"

            listOf(
                "jio", "airtel", "vi", "recharge", "electricity", "bescom", "bill"
            ).any { t.contains(it) } -> "Utility"

            listOf("salary", "bonus", "dividend").any { t.contains(it) } -> "Salary"

            else -> "Others"
        }
    }

    private suspend fun getOrCreateCategory(
        categoryDao: CategoryDao, categoryName: String, categoryType: String
    ): Int {
        val existing = categoryDao.getCategoryByName(categoryName)
        if (existing != null) return existing.categoryId

        val defaultImage = if (categoryType == "INCOME") "ic_salary" else "ic_expense"

        val newCategory = CategoryEntity(
            categoryName = categoryName, categoryType = categoryType, categoryImage = defaultImage
        )

        return categoryDao.insertCategory(newCategory).toInt()
    }


    private fun extractAmount(text: String): Double? {
        val regex = Regex(
            "(?:₹|INR|Rs\\.?)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE
        )

        val match = regex.find(text) ?: return null

        val amountRaw = match.groupValues[1].replace(",", "") // Remove commas (e.g., 1,000 -> 1000)

        return amountRaw.toDoubleOrNull()
    }

    private fun extractText(sbn: StatusBarNotification): String? {
        val extras = sbn.notification.extras
        return extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
    }

}