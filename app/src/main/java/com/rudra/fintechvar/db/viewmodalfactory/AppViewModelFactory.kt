package com.rudra.fintechvar.db.viewmodalfactory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rudra.fintechvar.db.database.AppDatabase
import com.rudra.fintechvar.db.repository.CategoryRepository
import com.rudra.fintechvar.db.repository.ExpenseRepository
import com.rudra.fintechvar.db.viewmodal.CategoryViewModel
import com.rudra.fintechvar.db.viewmodal.ExpenseViewModel

class AppViewModelFactory(
    private val app: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(CategoryViewModel::class.java)) {
            return CategoryViewModel(
                CategoryRepository(AppDatabase.getDatabase(app).categoryDao())
            ) as T
        }

        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            return ExpenseViewModel(
                ExpenseRepository(AppDatabase.getDatabase(app).categoryDao(),AppDatabase.getDatabase(app).transactionDao())
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
