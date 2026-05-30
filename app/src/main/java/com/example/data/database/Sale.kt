package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

data class SaleItem(
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val purchasePrice: Double,
    val salePrice: Double
)

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<SaleItem>, // This will be serialized to/from JSON in converters
    val totalAmount: Double,
    val totalProfit: Double,
    val customerName: String? = null,
    val amountPaid: Double = totalAmount,
    val amountRemaining: Double = 0.0
)
