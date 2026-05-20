package com.rudra.fintechvar.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rudra.fintechvar.db.modal.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insert(category: CategoryEntity): Long


    @Query("SELECT * FROM categories WHERE LOWER(categoryName) = LOWER(:name) LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?


    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)


    @Delete
    suspend fun delete(category: CategoryEntity)


    @Query("SELECT * FROM categories WHERE categoryId = :id LIMIT 1")
    suspend fun getCategoryById(id: Int): CategoryEntity?

    @Query("SELECT * FROM categories ORDER BY categoryId ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>


    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

}