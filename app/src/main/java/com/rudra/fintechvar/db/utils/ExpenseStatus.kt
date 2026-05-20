package com.rudra.fintechvar.db.utils

enum class ExpenseStatus {
    SAFE,      // Expense < 80% of Income (Green)
    WARNING,   // Expense 80% - 100% of Income (Orange)
    DANGER     // Expense > Income OR spending with 0 income (Red)
}