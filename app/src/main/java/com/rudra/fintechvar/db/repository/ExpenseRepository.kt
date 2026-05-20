package com.rudra.fintechvar.db.repository

import com.rudra.fintechvar.db.dao.CategoryDao
import com.rudra.fintechvar.db.dao.TransactionDao
import com.rudra.fintechvar.db.modal.CategoryEntity
import com.rudra.fintechvar.db.modal.TransactionEntity
import com.rudra.fintechvar.db.utils.TransactionType
import kotlinx.coroutines.flow.Flow


class ExpenseRepository(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) {


    suspend fun categoryExists(name: String): Boolean {
        return categoryDao.getCategoryByName(name) != null
    }

    suspend fun addCategoryByName(name: String, type: String): Boolean {
        if (categoryExists(name)) return false

        val newCategory = CategoryEntity(
            categoryName = name,
            categoryType = type,
            categoryImage = null
        )
        val id = categoryDao.insert(newCategory)
        return id > 0
    }

    suspend fun deleteCategoryByName(name: String): Boolean {
        val category = categoryDao.getCategoryByName(name) ?: return false
        categoryDao.delete(category)
        return true
    }

    suspend fun addTransactionByVoice(
        amount: Double,
        type: TransactionType,
        categoryName: String,
        cmd: String
    ): Boolean {
        val category = categoryDao.getCategoryByName(categoryName) ?: return false

        val transaction = TransactionEntity(
            amount = amount,
            type = type.name,
            date = System.currentTimeMillis(),
            description = cmd,
            categoryId = category.categoryId
        )

        val id = transactionDao.insert(transaction)
        return id > 0
    }


    suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insert(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.update(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.delete(transaction)

    fun getTransactionsByDateRange(
        start: Long,
        end: Long
    ): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByDateRange(start, end)

    fun getFilteredTransactions(
        categoryIds: Set<Int>,
        start: Long,
        end: Long
    ): Flow<List<TransactionEntity>> {
        return if (categoryIds.isEmpty()) {
            transactionDao.getTransactionsByDateRange(start, end)
        } else {
            transactionDao.getByCategoriesAndDate(
                categoryIds = categoryIds.toList(),
                start = start,
                end = end
            )
        }
    }

    fun getAllTimeIncome(): Flow<Double> {
        return transactionDao.getAllTimeIncome()
    }

    fun getAllTimeExpense(): Flow<Double> {
        return transactionDao.getAllTimeExpense()
    }

    suspend fun getTransactionById(id: Int): TransactionEntity? =
        transactionDao.getTransactionById(id)


}