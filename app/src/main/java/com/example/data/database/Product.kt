package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val barcode: String,
    val name: String,
    val purchasePrice: Double,
    val salePrice: Double,
    val wholesalePrice: Double = 0.0,
    val stock: Int,
    val minStockThreshold: Int = 3,
    val category: String = "عام",
    val imagePath: String? = null
)
