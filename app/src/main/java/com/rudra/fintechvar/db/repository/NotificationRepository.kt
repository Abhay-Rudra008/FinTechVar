package com.rudra.fintechvar.db.repository

import com.rudra.fintechvar.db.dao.NotificationDao
import com.rudra.fintechvar.db.modal.NotificationEntity
import kotlinx.coroutines.flow.Flow

class NotificationRepository(
    private val dao: NotificationDao
) {
    val allNotifications: Flow<List<NotificationEntity>> = dao.getAll()

    val unreadCount: Flow<Int> = dao.getCount()

    suspend fun insert(notification: NotificationEntity) {
        dao.insert(notification)
    }

    suspend fun markAllAsRead() {
        dao.markAllAsRead()
    }

    suspend fun delete(id: Int) {
        dao.deleteById(id)
    }

    suspend fun update(notification: NotificationEntity) {
        dao.update(notification)
    }

    suspend fun deleteMultipleNotifications(ids: List<Int>) {
        dao.deleteMultipleNotifications(ids)
    }

    suspend fun deleteOldNotifications(thresholdTime: Long) {
        dao.deleteOldNotifications(thresholdTime)
    }
}