package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val isLoss: Boolean = false, // false = مصروف (expense), true = خسارة (loss)
    val timestamp: Long = System.currentTimeMillis()
)
