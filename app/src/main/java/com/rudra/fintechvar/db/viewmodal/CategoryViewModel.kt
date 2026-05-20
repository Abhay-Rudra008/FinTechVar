package com.rudra.fintechvar.db.viewmodal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.fintechvar.db.modal.CategoryEntity
import com.rudra.fintechvar.db.repository.CategoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val repository: CategoryRepository
) : ViewModel() {

    val categories = repository.getAllCategories()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun addCategory(name: String, types: String, image: String?) {
        viewModelScope.launch {
            repository.insert(
                CategoryEntity(
                    categoryName = name,
                    categoryType = types,
                    categoryImage = image
                )
            )
        }
    }



    suspend fun getCategoryById(id: Int): CategoryEntity? {
        return repository.getCategoryById(id)
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.update(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.delete(category)
        }
    }
}
