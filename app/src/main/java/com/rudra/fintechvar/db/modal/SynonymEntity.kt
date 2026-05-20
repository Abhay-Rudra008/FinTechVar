package com.rudra.fintechvar.db.modal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "synonyms")
data class SynonymEntity(
    @PrimaryKey val rawWord: String,
    val categoryName: String // Links to the CategoryEntity
)