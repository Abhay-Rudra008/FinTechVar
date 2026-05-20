package com.rudra.fintechvar.db.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rudra.fintechvar.db.dao.CategoryDao
import com.rudra.fintechvar.db.dao.NotificationDao
import com.rudra.fintechvar.db.dao.SynonymDao
import com.rudra.fintechvar.db.dao.TransactionDao
import com.rudra.fintechvar.db.modal.CategoryEntity
import com.rudra.fintechvar.db.modal.NotificationEntity
import com.rudra.fintechvar.db.modal.SynonymEntity
import com.rudra.fintechvar.db.modal.TransactionEntity


@Database(
    entities = [CategoryEntity::class, TransactionEntity::class, NotificationEntity::class, SynonymEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {


    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun notificationDao(): NotificationDao
    abstract fun synonymDao(): SynonymDao // 🔥 ADD THIS

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "expense_db"
                )
                    .fallbackToDestructiveMigration().build()

                INSTANCE = instance
                instance
            }
        }
    }
}
