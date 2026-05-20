package com.rudra.fintechvar.db.modal

import com.rudra.fintechvar.db.utils.ExpenseStatus

data class ProgressState(
    val percentage: Int,
    val status: ExpenseStatus
)