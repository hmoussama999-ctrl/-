package com.example.data.database

import kotlinx.coroutines.flow.Flow

class ShopRepository(private val shopDao: ShopDao) {
    val allProducts: Flow<List<Product>> = shopDao.getAllProducts()
    val allSales: Flow<List<Sale>> = shopDao.getAllSales()
    val allExpenses: Flow<List<Expense>> = shopDao.getAllExpenses()

    suspend fun getProductByBarcode(barcode: String): Product? = shopDao.getProductByBarcode(barcode)
    suspend fun getProductById(id: Int): Product? = shopDao.getProductById(id)
    suspend fun insertProduct(product: Product) = shopDao.insertProduct(product)
    suspend fun updateProduct(product: Product) = shopDao.updateProduct(product)
    suspend fun deleteProduct(product: Product) = shopDao.deleteProduct(product)

    suspend fun insertExpense(expense: Expense) = shopDao.insertExpense(expense)
    suspend fun deleteExpense(expense: Expense) = shopDao.deleteExpense(expense)

    suspend fun insertSale(sale: Sale) {
        // Automatically reduce the items stock level
        sale.items.forEach { item ->
            shopDao.reduceProductStock(item.productId, item.quantity)
        }
        shopDao.insertSale(sale)
    }

    suspend fun deleteSale(sale: Sale) {
        // Revert stock increment when a sale is refunded or deleted
        sale.items.forEach { item ->
            shopDao.increaseProductStock(item.productId, item.quantity)
        }
        shopDao.deleteSale(sale)
    }
}
