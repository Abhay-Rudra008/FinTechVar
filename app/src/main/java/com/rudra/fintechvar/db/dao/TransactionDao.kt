package com.rudra.fintechvar.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rudra.fintechvar.db.modal.TransactionEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE amount = :amount
        AND type = :type
        AND ABS(date - :time) < :windowMs
        """
    )
    suspend fun isDuplicateWithinWindow(
        amount: Double,
        type: String,
        time: Long,
        windowMs: Long
    ): Int

    @Query(
        """
        SELECT * FROM transactions
        WHERE date BETWEEN :start AND :end
        AND categoryId IN (:categoryIds)
        ORDER BY date DESC
        """
    )
    fun getByCategoriesAndDate(
        categoryIds: List<Int>,
        start: Long,
        end: Long
    ): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions 
        WHERE date BETWEEN :start AND :end
        ORDER BY date DESC
        """
    )
    fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT IFNULL(SUM(amount), 0) 
        FROM transactions 
        WHERE type = 'INCOME'
        """
    )
    fun getAllTimeIncome(): Flow<Double>

    @Query(
        """
        SELECT IFNULL(SUM(amount), 0) 
        FROM transactions 
        WHERE type = 'EXPENSE'
        """
    )
    fun getAllTimeExpense(): Flow<Double>

    @Query("SELECT * FROM transactions WHERE transactionId = :id LIMIT 1")
    suspend fun getTransactionById(id: Int): TransactionEntity?

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND date(date/1000, 'unixepoch') = date('now')")
    suspend fun getTodayExpense(): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND date(date/1000, 'unixepoch') = date('now')")
    suspend fun getTodayIncome(): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND date(date/1000, 'unixepoch') >= date('now', '-6 days')")
    suspend fun getWeeklyExpense(): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND date(date/1000, 'unixepoch') >= date('now', '-6 days')")
    suspend fun getWeeklyIncome(): Double?
}

