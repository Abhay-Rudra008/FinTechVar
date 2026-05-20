package com.rudra.fintechvar.db.viewmodal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.fintechvar.db.modal.ProgressState
import com.rudra.fintechvar.db.modal.TransactionEntity
import com.rudra.fintechvar.db.repository.ExpenseRepository
import com.rudra.fintechvar.db.utils.ExpenseStatus
import com.rudra.fintechvar.db.utils.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar


class ExpenseViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {
    private val selectedCategoryIds = MutableStateFlow<Set<Int>>(emptySet())

    fun setCategories(categoryIds: Set<Int>) {
        selectedCategoryIds.value = categoryIds
    }

    private val dateRange = MutableStateFlow(getDefaultDateRange())

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionEntity>> =
        combine(selectedCategoryIds, dateRange) { categories, range ->
            Triple(categories, range.first, range.second)
        }.flatMapLatest { (categories, start, end) ->
            repository.getFilteredTransactions(
                categoryIds = categories,
                start = start,
                end = end
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )


    val incomeExpense: StateFlow<Pair<Double, Double>> =
        transactions.map { list ->
            val income = list
                .filter { it.type == TransactionType.INCOME.name }
                .sumOf { it.amount }

            val expense = list
                .filter { it.type == TransactionType.EXPENSE.name }
                .sumOf { it.amount }

            income to expense
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            0.0 to 0.0
        )


    val allTimeIncome = repository.getAllTimeIncome()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            0.0
        )

    val allTimeExpense = repository.getAllTimeExpense()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            0.0
        )


    val allTimeProgress: Flow<ProgressState> =
        combine(allTimeIncome, allTimeExpense) { income, expense ->
            calculateProgressState(income, expense)
        }

    val expenseProgress: StateFlow<ProgressState> =
        incomeExpense.map { (income, expense) ->
            calculateProgressState(income, expense)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProgressState(0, ExpenseStatus.SAFE)
        )

    fun setDateRange(start: Long, end: Long) {
        dateRange.value = start to end
    }

    fun addTransaction(transaction: TransactionEntity) =
        viewModelScope.launch { repository.insertTransaction(transaction) }

    fun updateTransaction(transaction: TransactionEntity) =
        viewModelScope.launch { repository.updateTransaction(transaction) }

    fun deleteTransaction(transaction: TransactionEntity) =
        viewModelScope.launch { repository.deleteTransaction(transaction) }

    fun getTransactionsForBottomSheet(
        start: Long,
        end: Long,
        type: String,
        categoryIds: Set<Int>? = null
    ): Flow<List<TransactionEntity>> {

        return repository
            .getTransactionsByDateRange(start, end)
            .map { list ->
                list.filter { tx ->
                    tx.type == type &&
                            (categoryIds == null || categoryIds.contains(tx.categoryId))
                }
            }
    }


    suspend fun getTransactionById(id: Int): TransactionEntity? =
        repository.getTransactionById(id)

    private fun getDefaultDateRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        cal.add(Calendar.MONTH, -1)
        val start = cal.timeInMillis
        return start to end
    }

    private fun calculateProgressState(income: Double, expense: Double): ProgressState {
        //  No income, but you have expenses
        if (income <= 0.0 && expense > 0.0) {
            return ProgressState(percentage = 100, status = ExpenseStatus.DANGER)
        }

        //  No income, no expense
        if (income <= 0.0 && expense <= 0.0) {
            return ProgressState(percentage = 0, status = ExpenseStatus.SAFE)
        }

        //  Normal calculation
        val rawPercentage = ((expense / income) * 100).toInt()
        val cappedPercentage = rawPercentage.coerceIn(0, 100)

        val status = when {
            rawPercentage < 80 -> ExpenseStatus.SAFE
            rawPercentage <= 100 -> ExpenseStatus.WARNING
            else -> ExpenseStatus.DANGER
        }

        return ProgressState(percentage = cappedPercentage, status = status)
    }

}
