package com.rudra.fintechvar.db.modal

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CategorySummary(
    val categoryId: Int,
    val categoryName: String,
    val totalAmount: Double
) : Parcelable
