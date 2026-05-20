package com.rudra.fintechvar.notification

import com.rudra.fintechvar.db.modal.NotificationEntity

sealed class NotificationUiItem {
    data class DateHeader(val title: String) : NotificationUiItem()
    data class NotificationItem(val notification: NotificationEntity) : NotificationUiItem()
}
