package com.rudra.fintechvar.db.viewmodal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.fintechvar.db.modal.NotificationEntity
import com.rudra.fintechvar.db.repository.NotificationRepository
import com.rudra.fintechvar.notification.NotificationUiItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class NotificationViewModel(
    private val repository: NotificationRepository,
) : ViewModel() {

    init {
        deleteNotificationsOlderThan15Days()
    }

    val notifications: StateFlow<List<NotificationEntity>> = repository.allNotifications.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    val notificationCount: StateFlow<Int> = repository.unreadCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 0
    )


    fun deleteSelectedNotifications(ids: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMultipleNotifications(ids)
        }
    }

    private fun deleteNotificationsOlderThan15Days() {
        viewModelScope.launch(Dispatchers.IO) {
            val fifteenDaysInMillis = 15L * 24 * 60 * 60 * 1000
            val thresholdTime = System.currentTimeMillis() - fifteenDaysInMillis

            repository.deleteOldNotifications(thresholdTime)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    fun markAsRead(notification: NotificationEntity) {
        viewModelScope.launch {
            repository.update(notification.copy(isRead = true))
        }
    }

    fun mapNotifications(notifications: List<NotificationEntity>): List<NotificationUiItem> {
        val result = mutableListOf<NotificationUiItem>()

        // Group by date label (Today / Yesterday / Date)
        val grouped = notifications.groupBy { getDateLabel(it.time) }

        grouped.forEach { (dateLabel, items) ->
            result.add(NotificationUiItem.DateHeader(dateLabel))
            items.forEach { result.add(NotificationUiItem.NotificationItem(it)) }
        }

        return result
    }

    private fun getDateLabel(time: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = time }
        val today = Calendar.getInstance()

        return when {
            isSameDay(cal, today) -> "Today"
            isSameDay(
                cal, Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }) -> "Yesterday"

            else -> SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(time))
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(
            Calendar.DAY_OF_YEAR
        )
    }
}