package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.Product
import com.example.data.database.Sale
import com.example.data.database.SaleItem
import com.example.data.database.Expense
import com.example.data.database.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CartItem(
    val product: Product,
    val quantity: Int,
    val isWholesale: Boolean = false
)

class ShopViewModel(private val repository: ShopRepository) : ViewModel() {

    // Lists loaded directly from Database reactive Flow
    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val sales: StateFlow<List<Sale>> = repository.allSales
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val expenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Active Checkout Cart
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    // Temporary messages/notifications (e.g. Success, Low stock warning)
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun showMessage(msg: String) {
        _message.value = msg
    }

    fun clearMessage() {
        _message.value = null
    }

    // --- PRODUCT CRUD ---
    fun addProduct(
        barcode: String,
        name: String,
        purchasePrice: Double,
        salePrice: Double,
        wholesalePrice: Double,
        stock: Int,
        minStockThreshold: Int,
        category: String,
        imagePath: String? = null
    ) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _message.value = "الرجاء إدخال اسم المنتج"
                return@launch
            }
            if (salePrice < purchasePrice || (wholesalePrice > 0.0 && wholesalePrice < purchasePrice)) {
                _message.value = "تنبيه: سعر البيع أقل من سعر الشراء!"
            }
            // Check if barcode already exists
            if (barcode.isNotBlank()) {
                val existing = repository.getProductByBarcode(barcode.trim())
                if (existing != null) {
                    _message.value = "خطأ: هناك منتج آخر مسجل بنفس هذا الباركود"
                    return@launch
                }
            }

            val product = Product(
                barcode = barcode.trim(),
                name = name.trim(),
                purchasePrice = purchasePrice,
                salePrice = salePrice,
                wholesalePrice = wholesalePrice,
                stock = stock,
                minStockThreshold = minStockThreshold,
                category = category,
                imagePath = imagePath
            )
            repository.insertProduct(product)
            _message.value = "تمت إضافة المنتج بنجاح!"
        }
    }

    fun updateProduct(
        id: Int,
        barcode: String,
        name: String,
        purchasePrice: Double,
        salePrice: Double,
        wholesalePrice: Double,
        stock: Int,
        minStockThreshold: Int,
        category: String,
        imagePath: String? = null
    ) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _message.value = "الرجاء إدخال اسم المنتج"
                return@launch
            }
            // If barcode changed, check uniqueness
            if (barcode.isNotBlank()) {
                val existing = repository.getProductByBarcode(barcode.trim())
                if (existing != null && existing.id != id) {
                    _message.value = "خطأ: هذا الباركود مستخدم لمنتج آخر بالفعل"
                    return@launch
                }
            }

            val product = Product(
                id = id,
                barcode = barcode.trim(),
                name = name.trim(),
                purchasePrice = purchasePrice,
                salePrice = salePrice,
                wholesalePrice = wholesalePrice,
                stock = stock,
                minStockThreshold = minStockThreshold,
                category = category,
                imagePath = imagePath
            )
            repository.updateProduct(product)
            _message.value = "تم تحديث بيانات المنتج!"
            
            // Sync with cart if the item is present
            _cart.value = _cart.value.map {
                if (it.product.id == id) {
                    it.copy(product = product)
                } else {
                    it
                }
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            _message.value = "تم حذف المنتج للموجب من المخزن"
            // Remove from cart if present
            _cart.value = _cart.value.filter { it.product.id != product.id }
        }
    }

    // --- CART POS ACTIONS ---
    fun addToCart(product: Product, quantity: Int = 1) {
        val currentItems = _cart.value.toMutableList()
        val index = currentItems.indexOfFirst { it.product.id == product.id }

        if (index != -1) {
            val existingItem = currentItems[index]
            val newQuantity = existingItem.quantity + quantity
            if (newQuantity > product.stock) {
                _message.value = "المخزون غير كافٍ! متاح فقط ${product.stock} قطعة"
                return
            }
            currentItems[index] = existingItem.copy(quantity = newQuantity)
        } else {
            if (quantity > product.stock) {
                _message.value = "المخزون غير كافٍ! متاح فقط ${product.stock} قطعة"
                return
            }
            currentItems.add(CartItem(product, quantity))
        }
        _cart.value = currentItems
        _message.value = "تمت إضافة المنتج إلى السلة"
    }

    fun toggleCartItemPriceType(productId: Int) {
        val currentItems = _cart.value.toMutableList()
        val index = currentItems.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            val item = currentItems[index]
            currentItems[index] = item.copy(isWholesale = !item.isWholesale)
            _cart.value = currentItems
        }
    }

    fun updateCartItemQuantity(productId: Int, quantity: Int) {
        if (quantity <= 0) {
            removeFromCart(productId)
            return
        }
        val currentItems = _cart.value.toMutableList()
        val index = currentItems.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            val item = currentItems[index]
            if (quantity > item.product.stock) {
                _message.value = "الكمية المطلوبة تتجاوز المتاح بمخزن المنتج!"
                return
            }
            currentItems[index] = item.copy(quantity = quantity)
            _cart.value = currentItems
        }
    }

    fun removeFromCart(productId: Int) {
        _cart.value = _cart.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    fun checkout(customerName: String?, amountPaid: Double) {
        val currentCart = _cart.value
        if (currentCart.isEmpty()) {
            _message.value = "السلة فارغة مسبقاً!"
            return
        }

        viewModelScope.launch {
            val saleItems = currentCart.map { cartItem ->
                val appliedPrice = if (cartItem.isWholesale) cartItem.product.wholesalePrice else cartItem.product.salePrice
                SaleItem(
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    quantity = cartItem.quantity,
                    purchasePrice = cartItem.product.purchasePrice,
                    salePrice = appliedPrice
                )
            }

            val totalAmount = currentCart.sumOf { (if (it.isWholesale) it.product.wholesalePrice else it.product.salePrice) * it.quantity }
            val totalProfit = currentCart.sumOf { ((if (it.isWholesale) it.product.wholesalePrice else it.product.salePrice) - it.product.purchasePrice) * it.quantity }

            val remaining = if (totalAmount > amountPaid) totalAmount - amountPaid else 0.0

            val sale = Sale(
                items = saleItems,
                totalAmount = totalAmount,
                totalProfit = totalProfit,
                customerName = customerName?.trim()?.ifEmpty { null },
                amountPaid = amountPaid,
                amountRemaining = remaining
            )

            // Save sale which reduces products stock levels automatically
            repository.insertSale(sale)

            _cart.value = emptyList()
            _message.value = "تم إكمال عملية البيع بنجاح!"
        }
    }

    // Lookup product by scanning/manually typing a barcode
    fun handleBarcodeScan(barcodeText: String) {
        val query = barcodeText.trim()
        if (query.isBlank()) return
        
        viewModelScope.launch {
            val product = repository.getProductByBarcode(query)
            if (product != null) {
                if (product.stock > 0) {
                    addToCart(product, 1)
                } else {
                    _message.value = "تنبيه: المنتج '${product.name}' نفد مخزونه بالكامل!"
                }
            } else {
                _message.value = "لم يتم العثور على أي منتج يحمل الكود: $query"
            }
        }
    }

    // --- TRANSACTION ACTIONS ---
    fun deleteSale(sale: Sale) {
        viewModelScope.launch {
            repository.deleteSale(sale)
            _message.value = "تم إلغاء واسترجاع عملية البيع والمخزون بنجاح"
        }
    }

    // --- EXPENSES & LOSSES ---
    fun addExpense(title: String, amount: Double, isLoss: Boolean) {
        viewModelScope.launch {
            if (title.isBlank()) {
                _message.value = "الرجاء إدخال وصف المصروف/الخسارة"
                return@launch
            }
            if (amount <= 0.0) {
                _message.value = "الرجاء إدخال قيمة صالحة أكبر من الصفر"
                return@launch
            }
            val expense = Expense(
                title = title.trim(),
                amount = amount,
                isLoss = isLoss
            )
            repository.insertExpense(expense)
            _message.value = if (isLoss) "تم تسجيل الخسارة بنجاح!" else "تم تسجيل المصروف بنجاح!"
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _message.value = "تم حذف المصروف/الخسارة بنجاح!"
        }
    }
}

class ShopViewModelFactory(private val repository: ShopRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShopViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShopViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
