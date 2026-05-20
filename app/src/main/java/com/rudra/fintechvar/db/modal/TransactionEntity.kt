package com.rudra.fintechvar.db.modal

import androidx.room.*
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["categoryId"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("categoryId"),
        Index("type"),
        Index("date")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val transactionId: Int = 0,

    val amount: Double,

    val type: String,   // INCOME / EXPENSE (type-safe)

    val date: Long,
    val description : String ="",
    val categoryId: Int
)
