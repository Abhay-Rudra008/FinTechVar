package com.rudra.fintechvar.db.modal

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["categoryName"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val categoryId: Int = 0,
    val categoryName: String,
    val categoryType: String,
    val categoryImage: String?
)
