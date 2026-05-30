package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.Product
import com.example.data.database.Sale
import com.example.data.database.Expense
import com.example.data.database.ShopDatabase
import com.example.data.database.ShopRepository
import com.example.ui.CartItem
import com.example.ui.ShopViewModel
import com.example.ui.ShopViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File
import java.io.FileOutputStream
import android.net.Uri
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ShoppingBag
import coil.compose.AsyncImage

// Enumeration for screen destinations
enum class ScreenState(val arabicTitle: String) {
    DASHBOARD("لوحة التحكم"),
    INVENTORY("إدارة المخزن"),
    POS("نقاط البيع"),
    HISTORY("سجل الفواتير")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create database, DAO, repository, and ViewModel factory
        val database = ShopDatabase.getDatabase(this)
        val repository = ShopRepository(database.shopDao())
        val factory = ShopViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[ShopViewModel::class.java]

        setContent {
            MyApplicationTheme {
                // Force layout direction to RTL for beautiful Arabic flow
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    ShopApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopApp(viewModel: ShopViewModel) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(ScreenState.DASHBOARD) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect app state from ViewModel
    val products by viewModel.products.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val message by viewModel.message.collectAsState()

    // Handle Toast messages reactively
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Modal state controllers
    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingCart,
                            contentDescription = "Logo",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "نظام تسيير المحل",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    // Quick stats indicators
                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        val lowStockCount = products.count { it.stock <= it.minStockThreshold }
                        if (lowStockCount > 0) {
                            BadgedBox(badge = { Badge { Text(lowStockCount.toString()) } }) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = "Warning",
                                    tint = Color.Yellow
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == ScreenState.DASHBOARD,
                    onClick = { currentScreen = ScreenState.DASHBOARD },
                    label = { Text(ScreenState.DASHBOARD.arabicTitle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Filled.Analytics, contentDescription = ScreenState.DASHBOARD.arabicTitle) },
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )
                NavigationBarItem(
                    selected = currentScreen == ScreenState.INVENTORY,
                    onClick = { currentScreen = ScreenState.INVENTORY },
                    label = { Text(ScreenState.INVENTORY.arabicTitle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Filled.Inventory, contentDescription = ScreenState.INVENTORY.arabicTitle) },
                    modifier = Modifier.testTag("nav_tab_inventory")
                )
                NavigationBarItem(
                    selected = currentScreen == ScreenState.POS,
                    onClick = { currentScreen = ScreenState.POS },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(ScreenState.POS.arabicTitle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            if (cart.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "(${cart.size})",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (cart.isNotEmpty()) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text(cart.sumOf { it.quantity }.toString(), color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.PointOfSale, contentDescription = ScreenState.POS.arabicTitle)
                        }
                    },
                    modifier = Modifier.testTag("nav_tab_pos")
                )
                NavigationBarItem(
                    selected = currentScreen == ScreenState.HISTORY,
                    onClick = { currentScreen = ScreenState.HISTORY },
                    label = { Text(ScreenState.HISTORY.arabicTitle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Filled.History, contentDescription = ScreenState.HISTORY.arabicTitle) },
                    modifier = Modifier.testTag("nav_tab_history")
                )
            }
        },
        floatingActionButton = {
            if (currentScreen == ScreenState.INVENTORY) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_product_fab")
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "إضافة منتج")
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                ScreenState.DASHBOARD -> DashboardScreen(
                    products = products,
                    sales = sales,
                    expenses = expenses,
                    onAddExpense = { title, amount, isLoss -> viewModel.addExpense(title, amount, isLoss) },
                    onDeleteExpense = { viewModel.deleteExpense(it) },
                    onNavigateToPos = { currentScreen = ScreenState.POS },
                    onNavigateToInventory = { currentScreen = ScreenState.INVENTORY }
                )
                ScreenState.INVENTORY -> InventoryScreen(
                    products = products,
                    viewModel = viewModel,
                    onEdit = { productToEdit = it }
                )
                ScreenState.POS -> POSScreen(
                    products = products,
                    cart = cart,
                    viewModel = viewModel
                )
                ScreenState.HISTORY -> HistoryScreen(
                    sales = sales,
                    viewModel = viewModel
                )
            }
        }

        // Add Product Dialog
        if (showAddDialog) {
            AddOrEditProductDialog(
                onDismiss = { showAddDialog = false },
                onSave = { barcode, name, purchase, sale, wholesale, stock, minStock, cat, imgPath ->
                    viewModel.addProduct(barcode, name, purchase, sale, wholesale, stock, minStock, cat, imgPath)
                    showAddDialog = false
                }
            )
        }

        // Edit Product Dialog
        productToEdit?.let { product ->
            AddOrEditProductDialog(
                productToEdit = product,
                onDismiss = { productToEdit = null },
                onSave = { barcode, name, purchase, sale, wholesale, stock, minStock, cat, imgPath ->
                    viewModel.updateProduct(product.id, barcode, name, purchase, sale, wholesale, stock, minStock, cat, imgPath)
                    productToEdit = null
                }
            )
        }
    }
}

// ==================== DASHBOARD SCREEN ====================
@Composable
fun DashboardScreen(
    products: List<Product>,
    sales: List<Sale>,
    expenses: List<com.example.data.database.Expense>,
    onAddExpense: (String, Double, Boolean) -> Unit,
    onDeleteExpense: (com.example.data.database.Expense) -> Unit,
    onNavigateToPos: () -> Unit,
    onNavigateToInventory: () -> Unit
) {
    val totalSales = sales.sumOf { it.totalAmount }
    val grossProfit = sales.sumOf { it.totalProfit }
    val totalExpenses = expenses.filter { !it.isLoss }.sumOf { it.amount }
    val totalLosses = expenses.filter { it.isLoss }.sumOf { it.amount }
    val totalExpensesAndLosses = totalExpenses + totalLosses
    val actualNetProfit = grossProfit - totalExpensesAndLosses
    val lowStockList = products.filter { it.stock <= it.minStockThreshold }

    var showAddExpenseDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "مرحباً بك في المحل التجاري 👋",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "نظّم مبيعاتك وراقب مخزنك في مكان واحد وبشكل مبسط.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // Key Metrics Grid
        item {
            Text(
                text = "الإحصائيات والملخص المالي العام",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1: Sales / Net Profit
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Total Sales Widget
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = "مبيعات",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "إجمالي المبيعات", fontSize = 13.sp, color = Color.DarkGray)
                            Text(
                                text = String.format(Locale.US, "%.2f", totalSales) + " د.ج",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Net profit Widget
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (actualNetProfit >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Analytics,
                                contentDescription = "أرباح",
                                tint = if (actualNetProfit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "صافي الأرباح الفعلي", fontSize = 13.sp, color = Color.DarkGray)
                            Text(
                                text = String.format(Locale.US, "%.2f", actualNetProfit) + " د.ج",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (actualNetProfit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                }

                // Row 2: Expenses / Losses
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Total Expenses Widget
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingBag,
                                contentDescription = "مصروفات",
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "المصروفات اليومية", fontSize = 13.sp, color = Color.DarkGray)
                            Text(
                                text = String.format(Locale.US, "%.2f", totalExpenses) + " د.ج",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                    }

                    // Total Losses Widget
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "خسارة",
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "الخسائر والسلع التالفة", fontSize = 13.sp, color = Color.DarkGray)
                            Text(
                                text = String.format(Locale.US, "%.2f", totalLosses) + " د.ج",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828)
                            )
                        }
                    }
                }

                // Row 3: Items / Low Stock
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Unique Items Widget
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Inventory,
                                contentDescription = "أصناف",
                                tint = Color(0xFF7B1FA2),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "عدد السلع", fontSize = 13.sp, color = Color.DarkGray)
                            Text(
                                text = "${products.size} صنف متاح",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7B1FA2)
                            )
                        }
                    }

                    // Low Stock Alert Widget
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (lowStockList.isNotEmpty()) Color(0xFFFFEBEE) else Color(0xFFF1F8E9)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(
                                imageVector = if (lowStockList.isNotEmpty()) Icons.Filled.Warning else Icons.Filled.Inventory,
                                contentDescription = "تنبيه",
                                tint = if (lowStockList.isNotEmpty()) Color(0xFFC62828) else Color(0xFF558B2F),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "نواقص المخزن", fontSize = 13.sp, color = Color.DarkGray)
                            Text(
                                text = if (lowStockList.isNotEmpty()) "${lowStockList.size} سلع نفدت" else "المخزن كافٍ",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (lowStockList.isNotEmpty()) Color(0xFFB71C1C) else Color(0xFF33691E)
                            )
                        }
                    }
                }
            }
        }

        // Quick POS Navigator Button
        item {
            Button(
                onClick = onNavigateToPos,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("quick_pos_action_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Filled.PointOfSale, contentDescription = "بيع")
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "تسجيل عملية بيع جديدة (POS)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        // Expenses & Losses Row Title & Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل المصروفات والخسائر اليومية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Button(
                    onClick = { showAddExpenseDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.testTag("add_expense_button")
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "إضافة", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تسجيل مصروف/خسارة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (expenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingBag,
                            contentDescription = "لا يوجد مصروفات",
                            tint = Color.LightGray,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد مصروفات أو خسائر مسجلة اليوم.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(expenses) { expense ->
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val timeStr = timeFormat.format(java.util.Date(expense.timestamp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (expense.isLoss) Color(0xFFFFEBEE) else Color(0xFFEBF5FB)
                    ),
                    border = BorderStroke(1.dp, if (expense.isLoss) Color(0xFFFFCDD2) else Color(0xFFD4E6F1)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                androidx.compose.material3.SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = if (expense.isLoss) "خسارة" else "مصروف",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (expense.isLoss) Color(0xFFC62828) else Color(0xFF2980B9),
                                        labelColor = Color.White
                                    ),
                                    border = null
                                )
                                Text(
                                    text = expense.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "الوقت: $timeStr", fontSize = 11.sp, color = Color.Gray)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${expense.amount} د.ج",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (expense.isLoss) Color(0xFFC62828) else Color(0xFF2980B9)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { onDeleteExpense(expense) },
                                modifier = Modifier.size(36.dp).testTag("delete_expense_${expense.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "حذف المورد",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Out of stock listing section
        if (lowStockList.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "منتجات على وشك النفاد (تحتاج تزويد)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                    TextButton(onClick = onNavigateToInventory) {
                        Text(text = "عرض الكل", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            items(lowStockList.take(4)) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9F9)),
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "القسم: ${item.category} | باركود: ${if (item.barcode.isEmpty()) "لا يوجد" else item.barcode}", fontSize = 11.sp, color = Color.Gray)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFEBEE))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "المتبقي: ${item.stock} قطع",
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Inventory,
                        contentDescription = "مستقر",
                        tint = Color.LightGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ممتاز! جميع المنتجات في المخزن كمياتها سليمة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onSave = { title, amount, isLoss ->
                onAddExpense(title, amount, isLoss)
                showAddExpenseDialog = false
            }
        )
    }
}

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var isLoss by remember { mutableStateOf(false) } // false = مصروف, true = خسارة

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إضافة مصروف أو خسارة جديدة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Description field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("الوصف أو السبب (مثال: أكياس بلاستيك)*") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_expense_title"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Amount field
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("المبلغ المالي (د.ج)*") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_expense_amount"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )

                // Section selectors
                Text(
                    text = "نوع تسجيل العملية:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isLoss = false },
                        shape = RoundedCornerShape(8.dp),
                        color = if (!isLoss) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF5F5F5),
                        border = if (!isLoss) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "مصروف يومي",
                                color = if (!isLoss) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    androidx.compose.material3.Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isLoss = true },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isLoss) Color(0xFFFFEBEE) else Color(0xFFF5F5F5),
                        border = if (isLoss) BorderStroke(1.dp, Color(0xFFC62828)) else null
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "خسارة / تلف",
                                color = if (isLoss) Color(0xFFC62828) else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amount > 0) {
                        onSave(title, amount, isLoss)
                    }
                },
                modifier = Modifier.testTag("dialog_expense_save_button")
            ) {
                Text("حفظ وتسجيل")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_expense_cancel_button")
            ) {
                Text("إلغاء")
            }
        }
    )
}

// Helper background modifier for Box
fun Modifier.background(color: Color): Modifier = this.clip(RoundedCornerShape(4.dp)).clickable {  }

// ==================== INVENTORY SCREEN ====================
@Composable
fun InventoryScreen(
    products: List<Product>,
    viewModel: ShopViewModel,
    onEdit: (Product) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }

    val categories = listOf("الكل", "عام", "مواد غذائية", "مشروبات", "خضار وفواكه", "منظفات", "إلكترونيات", "أخرى")

    // Filter list based on search and category
    val filteredProducts = products.filter {
        val matchesSearch = it.name.contains(searchQuery, ignoreCase = true) ||
                it.barcode.contains(searchQuery)
        val matchesCategory = selectedCategory == "الكل" || it.category == selectedCategory
        matchesSearch && matchesCategory
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Input Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("بحث باسم السلعة أو الباركود...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("inventory_search_field"),
            leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = "بحث") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "مسح")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Categories List (Horizontal chips container)
        ScrollableTabRow(
            selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Tab(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    text = {
                        Text(
                            text = cat,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid Content
        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Category,
                        contentDescription = "لا توجد سلع",
                        tint = Color.LightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد منتجات مسجلة تطابق فلترة البحث.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        onEdit = { onEdit(product) },
                        onDelete = { viewModel.deleteProduct(product) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isLowStock = product.stock <= product.minStockThreshold
    val margin = product.salePrice - product.purchasePrice

    var showConfirmDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Heading Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product Thumbnail Image
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val imagePath = product.imagePath
                    if (imagePath != null) {
                        AsyncImage(
                            model = File(imagePath),
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = product.category,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (product.barcode.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "کود: ${product.barcode}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Edit and Delete triggers
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "تعديل", tint = Color.Gray)
                    }
                    IconButton(onClick = { showConfirmDelete = true }) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "حذف", tint = Color(0xFFC62828))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details and values Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row {
                        Text(text = "سعر الشراء: ", fontSize = 12.sp, color = Color.Gray)
                        Text(text = "${product.purchasePrice} د.ج", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row {
                        Text(text = "التجزئة: ", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            text = "${product.salePrice} د.ج",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row {
                        Text(text = "الجملة: ", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            text = "${product.wholesalePrice} د.ج",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isLowStock) Color(0xFFFFEBEE) else Color(0xFFE8F5E9))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isLowStock) "مخزن منخفض: ${product.stock}" else "المخزون: ${product.stock} قطة",
                            color = if (isLowStock) Color(0xFFC62828) else Color(0xFF2E7D32),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "الربح الفردي: +${String.format(Locale.US, "%.1f", margin)} د.ج",
                        fontSize = 11.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("حذف السلعة؟") },
            text = { Text("هل أنت متأكد من رغبتك في حذف المنتج '${product.name}' نهائياً من المخزون التابع للمحل؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showConfirmDelete = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("نعم، كف عن عرض السلعة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// ==================== POINT OF SALE SCREEN ====================
@Composable
fun POSScreen(
    products: List<Product>,
    cart: List<CartItem>,
    viewModel: ShopViewModel
) {
    val grandTotal = cart.sumOf { (if (it.isWholesale) it.product.wholesalePrice else it.product.salePrice) * it.quantity }
    var searchQuery by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var barcodeToSimulate by remember { mutableStateOf("") }
    var amountPaidStr by remember(grandTotal) { mutableStateOf(String.format(Locale.US, "%.2f", grandTotal)) }

    // Quick catalog listing to tap-add to cart
    val activeAvailableProducts = products.filter {
        it.stock > 0 && (it.name.contains(searchQuery, ignoreCase = true) || it.barcode.contains(searchQuery))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Split POS interface into top components and bottom visual cards
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Simulated Barcode Scanning
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.QrCodeScanner,
                                contentDescription = "باركود",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "محاكي قارئ الباركود (مسح الباركود السريع)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = barcodeToSimulate,
                                onValueChange = { barcodeToSimulate = it },
                                placeholder = { Text("أدخل الكود أو اختر سريعا...", fontSize = 13.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pos_barcode_scanner_field"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Button(
                                onClick = {
                                    if (barcodeToSimulate.isNotBlank()) {
                                        viewModel.handleBarcodeScan(barcodeToSimulate)
                                        barcodeToSimulate = ""
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp)
                            ) {
                                Text("تأكيد كرمز")
                            }
                        }

                        // Hot clickable simulation buttons
                        if (products.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "كود سريع لمحاكاة الجهاز بييب 🔊", fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                products.filter { it.barcode.isNotEmpty() }.take(3).forEach { prod ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                viewModel.handleBarcodeScan(prod.barcode)
                                            }
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${prod.name} [${prod.barcode}]",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Catalog Quick Finder
            item {
                Text(
                    text = "البحث اليدوي وإضافة منتجات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("ابحث عن سلعة لإضافتها بكبسة زر...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_item_search"),
                    leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = "بحث") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Filled.Close, contentDescription = "مسح")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Available catalog matching list
            if (activeAvailableProducts.isNotEmpty()) {
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeAvailableProducts) { prod ->
                            Card(
                                modifier = Modifier
                                    .clickable { viewModel.addToCart(prod, 1) }
                                    .testTag("catalog_pos_click_${prod.id}"),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Custom small photo thumbnail
                                    Box(
                                        modifier = Modifier
                                            .size(45.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(
                                                width = 0.5.dp,
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val imagePath = prod.imagePath
                                        if (imagePath != null) {
                                            AsyncImage(
                                                model = File(imagePath),
                                                contentDescription = prod.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.ShoppingBag,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prod.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${prod.salePrice} د.ج",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "المتوفر: ${prod.stock} قطع",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (products.isEmpty()) {
                item {
                    Text(
                        text = "⚠️ لم تسجل سلع بعد بمخزن المحل للبيع! أضف معروضاتك بالتبويب الثاني أولاً.",
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            // Section 3: Registered Shopping Cart
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سلة المشتريات الحالية (${cart.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (cart.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearCart() },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) {
                            Icon(imageVector = Icons.Filled.ClearAll, contentDescription = "إفراغ")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إفراغ السلة")
                        }
                    }
                }
            }

            if (cart.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingCart,
                                contentDescription = "سلة فارغة",
                                tint = Color.LightGray,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "السلة فارغة. ابدأ بإضافة السلع هنا لحسابها.", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(cart) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .testTag("cart_item_${item.product.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Custom small photo thumbnail
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                val imagePath = item.product.imagePath
                                if (imagePath != null) {
                                    AsyncImage(
                                        model = File(imagePath),
                                        contentDescription = item.product.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.ShoppingBag,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                val currentItemPrice = if (item.isWholesale) item.product.wholesalePrice else item.product.salePrice
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    androidx.compose.material3.SuggestionChip(
                                        onClick = { viewModel.toggleCartItemPriceType(item.product.id) },
                                        label = {
                                            Text(
                                                text = if (item.isWholesale) "جملة" else "تجزئة",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (item.isWholesale) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                            labelColor = if (item.isWholesale) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        border = null
                                    )
                                    Text(
                                        text = "$currentItemPrice د.ج",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "إجمالي: ${currentItemPrice * item.quantity} د.ج",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            // Quantity Adjusters
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF1F1F1))
                                ) {
                                    IconButton(
                                        onClick = { viewModel.updateCartItemQuantity(item.product.id, item.quantity - 1) },
                                        modifier = Modifier.size(28.dp).testTag("cart_minus_${item.product.id}")
                                    ) {
                                        Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    Text(
                                        text = item.quantity.toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    )
                                    IconButton(
                                        onClick = { viewModel.updateCartItemQuantity(item.product.id, item.quantity + 1) },
                                        modifier = Modifier.size(28.dp).testTag("cart_plus_${item.product.id}")
                                    ) {
                                        Text("+", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.removeFromCart(item.product.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "مسح",
                                        tint = Color(0xFFC62828),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Checkout Total & Client name
        AnimatedVisibility(visible = cart.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Total Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "المبلغ الإجمالي المستحق:", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = String.format(Locale.US, "%.2f", grandTotal) + " د.ج",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Client details
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("اسم الزبون (اختياري / لتتبع الديون والمبيعات)", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_name_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Payment Field (المبلغ المدفوع)
                    OutlinedTextField(
                        value = amountPaidStr,
                        onValueChange = { amountPaidStr = it },
                        label = { Text("المبلغ المدفوع لهاته الفاتورة *", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("amount_paid_field"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = {
                            TextButton(
                                onClick = { amountPaidStr = String.format(Locale.US, "%.2f", grandTotal) }
                            ) {
                                Text("كامل المبلغ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live calculations dynamic indicators
                    val parsedAmountPaid = amountPaidStr.toDoubleOrNull() ?: 0.0
                    val leftRemaining = grandTotal - parsedAmountPaid

                    if (leftRemaining > 0 && customerName.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "تحذير",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "يُفضل كتابة اسم الزبون لتسجيل الدين باسمه!",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Row(
                        modifier = Modifier
                             .fillMaxWidth()
                             .clip(RoundedCornerShape(8.dp))
                             .background(
                                 when {
                                     leftRemaining > 0 -> Color(0xFFFFF3E0)
                                     leftRemaining < 0 -> Color(0xFFE8F5E9)
                                     else -> Color(0xFFE1F5FE)
                                 }
                             )
                             .padding(10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconVector = when {
                            leftRemaining > 0 -> Icons.Default.Warning
                            else -> Icons.Default.CheckCircle
                        }
                        val iconColor = when {
                            leftRemaining > 0 -> Color(0xFFE65100)
                            leftRemaining < 0 -> Color(0xFF2E7D32)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        val statusText = when {
                            leftRemaining > 0 -> String.format(Locale.US, "المتبقي كدين على العميل: %.2f د.ج", leftRemaining)
                            leftRemaining < 0 -> String.format(Locale.US, "الفكة المسترجعة للزبون: %.2f د.ج", -leftRemaining)
                            else -> "تم تصفية ودفع كامل مبلغ الفاتورة"
                        }

                        Icon(
                            imageVector = iconVector,
                            contentDescription = "الحالة",
                            tint = iconColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            color = iconColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val parsedPaid = amountPaidStr.toDoubleOrNull() ?: grandTotal
                            viewModel.checkout(customerName, parsedPaid)
                            customerName = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("checkout_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.ShoppingCart, contentDescription = "تأكيد")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "تأكيد عملية البيع وإصدار الفاتورة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

// ==================== HISTORY LOG SCREEN ====================
@Composable
fun HistoryScreen(
    sales: List<Sale>,
    viewModel: ShopViewModel
) {
    val context = LocalContext.current

    if (sales.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = "لا توجد فواتير",
                    tint = Color.LightGray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "لا توجد فواتير أو معاملات مسجلة بعد.",
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "سجل أي عملية بيع من تبويب 'نقاط البيع' لتظهر الفواتير هنا.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "سجل الفواتير والمعاملات السابقة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            items(sales, key = { it.id }) { sale ->
                InvoiceCard(
                    sale = sale,
                    onDelete = { viewModel.deleteSale(sale) },
                    onShare = {
                        val receiptText = generateArabicReceipt(sale)
                        shareReceipt(context, receiptText)
                    }
                )
            }
        }
    }
}

@Composable
fun InvoiceCard(
    sale: Sale,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showConfirmRefund by remember { mutableStateOf(false) }

    val formattedDate = remember(sale.timestamp) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        sdf.format(Date(sale.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("invoice_card_${sale.id}")
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "فاتورة بيع #${sale.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(text = formattedDate, fontSize = 11.sp, color = Color.Gray)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${sale.totalAmount} د.ج",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Customer tag and debt info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (sale.customerName != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "الزبون: ", fontSize = 12.sp, color = Color.Gray)
                        Text(text = sale.customerName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "المدفوع: ", fontSize = 11.sp, color = Color.Gray)
                    Text(text = "${sale.amountPaid} د.ج", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    if (sale.amountRemaining > 0.0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "المتبقي: ", fontSize = 11.sp, color = Color.Gray)
                        Text(text = "${sale.amountRemaining} د.ج", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            Row {
                Text(text = "صافي ربح الفاتورة: ", fontSize = 11.sp, color = Color.Gray)
                Text(
                    text = "+${String.format(Locale.US, "%.1f", sale.totalProfit)} د.ج",
                    fontSize = 11.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Control Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expanded items visibility toggler
                TextButton(onClick = { expanded = !expanded }) {
                    Text(text = if (expanded) "إخفاء التفاصيل" else "تفاصيل السلع (${sale.items.sumOf { it.quantity }})")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onShare) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = "مشاركة", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showConfirmRefund = true }) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "استرجاع", tint = Color(0xFFC62828))
                    }
                }
            }

            // Expanded Itemizations List
            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF9F9F9))
                            .padding(8.dp)
                ) {
                    Text(text = "السلع المشمولة بالفاتورة:", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                    sale.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "• ${item.productName} (العدد: ${item.quantity})", fontSize = 12.sp, color = Color.DarkGray)
                            Text(text = "${item.salePrice * item.quantity} د.ج", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }

    if (showConfirmRefund) {
        AlertDialog(
            onDismissRequest = { showConfirmRefund = false },
            title = { Text("استرجاع الفاتورة وإلغاؤها؟") },
            text = { Text("عند إلغاء هذه الفاتورة، سيتم إرجاع كافة السلع المبيعة للمخزون تلقائياً وحذف السجل المالي لها من التقارير.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showConfirmRefund = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("نعم، استرجع السعة وألغِ الفاتورة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmRefund = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// Generate an elegant text-based receipt in Arabic
fun generateArabicReceipt(sale: Sale): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val dateStr = sdf.format(Date(sale.timestamp))

    val sb = StringBuilder()
    sb.append("🧾 *فاتورة مبيعات محل تجاري*\n")
    sb.append("----------------------------\n")
    sb.append("رقم الفاتورة: #${sale.id}\n")
    sb.append("التاريخ: $dateStr\n")
    if (sale.customerName != null) {
        sb.append("الزبون الكريم: ${sale.customerName}\n")
    }
    sb.append("----------------------------\n")
    sb.append("*السلع والمنتجات:*\n")
    sale.items.forEach { item ->
        sb.append("- ${item.productName} x${item.quantity} (${item.salePrice} د.ج) = ${item.salePrice * item.quantity} د.ج\n")
    }
    sb.append("----------------------------\n")
    sb.append("*المبلغ الإجمالي المطلوب: ${sale.totalAmount} د.ج*\n")
    sb.append("المبلغ المدفوع: ${sale.amountPaid} د.ج\n")
    if (sale.amountRemaining > 0.0) {
        sb.append("المبلغ المتبقي (دين على الزبون): ${sale.amountRemaining} د.ج\n")
    } else if (sale.amountPaid > sale.totalAmount) {
        sb.append("الفكة المسترجعة للزبون: ${sale.amountPaid - sale.totalAmount} د.ج\n")
    }
    sb.append("----------------------------\n")
    sb.append("شكراً لزيارتكم الكريمة وتسوقكم معنا! 🙏🌸")
    return sb.toString()
}

// Intent sharing system
fun shareReceipt(context: Context, text: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "مشاركة الفاتورة عبر")
    context.startActivity(shareIntent)
}

// ==================== IMAGE SAVE HELPERS ====================
fun saveCapturedImage(context: Context, srcFile: File): String? {
    return try {
        val directory = File(context.filesDir, "product_images")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val destFile = File(directory, "prod_${System.currentTimeMillis()}.jpg")
        srcFile.copyTo(destFile, overwrite = true)
        destFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun saveGalleryImage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val directory = File(context.filesDir, "product_images")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val destFile = File(directory, "prod_${System.currentTimeMillis()}.jpg")
        destFile.outputStream().use { output ->
            inputStream.use { input ->
                input.copyTo(output)
            }
        }
        destFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// ==================== ADD / EDIT PRODUCT DIALOG ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditProductDialog(
    productToEdit: Product? = null,
    onDismiss: () -> Unit,
    onSave: (
        barcode: String,
        name: String,
        purchasePrice: Double,
        salePrice: Double,
        wholesalePrice: Double,
        stock: Int,
        minStockThreshold: Int,
        category: String,
        imagePath: String?
    ) -> Unit
) {
    val isEditMode = productToEdit != null
    val context = LocalContext.current

    var barcode by remember { mutableStateOf(productToEdit?.barcode ?: "") }
    var name by remember { mutableStateOf(productToEdit?.name ?: "") }
    var purchasePriceStr by remember { mutableStateOf(productToEdit?.purchasePrice?.toString() ?: "") }
    var salePriceStr by remember { mutableStateOf(productToEdit?.salePrice?.toString() ?: "") }
    var wholesalePriceStr by remember { mutableStateOf(productToEdit?.wholesalePrice?.toString() ?: "") }
    var stockStr by remember { mutableStateOf(productToEdit?.stock?.toString() ?: "") }
    var minStockThresholdStr by remember { mutableStateOf(productToEdit?.minStockThreshold?.toString() ?: "3") }
    var category by remember { mutableStateOf(productToEdit?.category ?: "عام") }
    var imagePath by remember { mutableStateOf(productToEdit?.imagePath) }

    val categories = listOf("عام", "مواد غذائية", "مشروبات", "خضار وفواكه", "منظفات", "إلكترونيات", "أخرى")

    // Image capture and selection stuff
    val tempFile = remember { File(context.cacheDir, "temp_camera_capture.jpg") }
    val cameraUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val savedPath = saveCapturedImage(context, tempFile)
            if (savedPath != null) {
                imagePath = savedPath
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(cameraUri)
            } catch (e: Exception) {
                Toast.makeText(context, "فشل فتح الكاميرا: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "يجب منح صلاحية الكاميرا للتصوير والمتابعة", Toast.LENGTH_LONG).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val savedPath = saveGalleryImage(context, uri)
            if (savedPath != null) {
                imagePath = savedPath
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (isEditMode) "تعديل تفاصيل السلعة" else "إضافة سلعة جديدة للمخزن") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Product Image Field (top section)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (imagePath != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = File(imagePath!!),
                                contentDescription = "صورة جارية",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            IconButton(
                                onClick = { imagePath = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .clip(CircleShape)
                                    .background(color = MaterialTheme.colorScheme.errorContainer)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "مسح الصورة",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = "تصوير",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "صورة المنتج (اختياري)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        val isGranted = ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.CAMERA
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (isGranted) {
                                            try {
                                                cameraLauncher.launch(cameraUri)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "فشل فتح الكاميرا: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تصوير منتج", fontSize = 11.sp)
                                }
                                TextButton(
                                    onClick = {
                                        galleryLauncher.launch("image/*")
                                    }
                                ) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("معرض الصور", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم السلعة / المنتج *") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_product_name"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("الباركود (اختياري / كود يدوي للبحث)") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_product_barcode"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = purchasePriceStr,
                        onValueChange = { purchasePriceStr = it },
                        label = { Text("سعر الشراء *") },
                        modifier = Modifier.weight(1f).testTag("dialog_product_purchase_price"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = salePriceStr,
                        onValueChange = { salePriceStr = it },
                        label = { Text("سعر التجزئة (بيع عادي) *") },
                        modifier = Modifier.weight(1f).testTag("dialog_product_sale_price"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = wholesalePriceStr,
                        onValueChange = { wholesalePriceStr = it },
                        label = { Text("سعر الجملة *") },
                        modifier = Modifier.weight(1f).testTag("dialog_product_wholesale_price"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text("الكمية المتوفرة *") },
                        modifier = Modifier.weight(1f).testTag("dialog_product_stock"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = minStockThresholdStr,
                        onValueChange = { minStockThresholdStr = it },
                        label = { Text("الحد الأدنى للتنبيه") },
                        modifier = Modifier.weight(1f).testTag("dialog_product_min_stock"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Simple Category Chooser Chips Row
                Text(text = "القسم / الصنف المعين للسلعة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                ScrollableTabRow(
                    selectedTabIndex = categories.indexOf(category).coerceAtLeast(0),
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    categories.forEach { cat ->
                        Tab(
                            selected = category == cat,
                            onClick = { category = cat },
                            text = { Text(text = cat, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val purchase = purchasePriceStr.toDoubleOrNull() ?: 0.0
                    val sale = salePriceStr.toDoubleOrNull() ?: 0.0
                    val wholesale = wholesalePriceStr.toDoubleOrNull() ?: 0.0
                    val stock = stockStr.toIntOrNull() ?: 0
                    val minStock = minStockThresholdStr.toIntOrNull() ?: 3

                    if (name.isNotBlank()) {
                        onSave(barcode, name, purchase, sale, wholesale, stock, minStock, category, imagePath)
                    }
                }
            ) {
                Text(text = if (isEditMode) "حفظ التعديلات" else "إضافة معروض")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "إلغاء")
            }
        }
    )
}

@Composable
fun Greeting(name: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
