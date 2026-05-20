package com.rudra.fintechvar.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.rudra.fintechvar.db.modal.NotificationEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY time DESC")
    fun getAll(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getCount(): Flow<Int>

    @Insert
    suspend fun insert(notification: NotificationEntity)

    @Query("DELETE FROM notifications WHERE id IN (:idList)")
    suspend fun deleteMultipleNotifications(idList: List<Int>)

    @Query("DELETE FROM notifications WHERE time < :thresholdTime")
    suspend fun deleteOldNotifications(thresholdTime: Long)

    @Query("UPDATE notifications SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Update
    suspend fun update(notification: NotificationEntity)
}