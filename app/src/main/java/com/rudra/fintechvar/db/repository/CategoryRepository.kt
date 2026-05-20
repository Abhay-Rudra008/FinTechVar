package com.rudra.fintechvar.db.repository

import com.rudra.fintechvar.db.dao.CategoryDao
import com.rudra.fintechvar.db.modal.CategoryEntity
import kotlinx.coroutines.flow.Flow


class CategoryRepository(
    private val dao: CategoryDao
) {


    fun getAllCategories(): Flow<List<CategoryEntity>> = dao.getAllCategories()

    suspend fun getCategoryById(id: Int): CategoryEntity? = dao.getCategoryById(id)


    suspend fun insert(category: CategoryEntity): Long = dao.insert(category)


    suspend fun update(category: CategoryEntity) = dao.update(category)

    suspend fun delete(category: CategoryEntity) = dao.delete(category)


}
