package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.CyberPosViewModel
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasirScreen(
    viewModel: CyberPosViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()
    val cart by viewModel.cart.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var barcodeInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showMobileCartSheet by remember { mutableStateOf(false) }
    var showBarcodeScanDialog by remember { mutableStateOf(false) }

    // Categories derived from available products
    val categories = remember(products) {
        val cats = products.map { it.category }.distinct().sorted()
        listOf("Semua") + cats
    }

    // Filtered products for sales checkout
    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            (selectedCategory == "Semua" || p.category == selectedCategory) &&
                    (searchQuery.isBlank() ||
                            p.name.contains(searchQuery, ignoreCase = true) ||
                            p.barcode.contains(searchQuery, ignoreCase = true) ||
                            p.category.contains(searchQuery, ignoreCase = true))
        }
    }

    // Function to handle barcode scan or submit
    val onBarcodeSubmit: (String) -> Unit = { code ->
        val trimmed = code.trim()
        if (trimmed.isNotBlank()) {
            val found = products.firstOrNull { it.barcode.equals(trimmed, ignoreCase = true) }
            if (found != null) {
                viewModel.addToCart(found, 1.0)
                viewModel.showMessage("Ditambahkan: ${found.name}")
                barcodeInput = ""
            } else {
                viewModel.showMessage("Barcode '$trimmed' tidak ditemukan di Katalog Produk!")
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp

        if (isWideScreen) {
            // Tablet / Desktop Split Screen Layout
            Row(modifier = Modifier.fillMaxSize()) {
                // Left side: Catalog & Search
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    KasirHeaderSection(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        barcodeInput = barcodeInput,
                        onBarcodeChange = { barcodeInput = it },
                        onBarcodeSubmit = onBarcodeSubmit,
                        onCameraScanClick = { showBarcodeScanDialog = true },
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelect = { selectedCategory = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    KasirProductGrid(
                        products = filteredProducts,
                        onProductClick = { viewModel.addToCart(it, 1.0) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Vertical Divider
                Divider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                )

                // Right side: Active Cart & Checkout Panel
                Surface(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    CartPanel(
                        cart = cart,
                        onUpdateQty = { id, qty -> viewModel.updateCartQuantity(id, qty) },
                        onRemove = { viewModel.removeFromCart(it) },
                        onClear = { viewModel.clearCart() },
                        onCheckoutClick = { showCheckoutDialog = true },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }
        } else {
            // Mobile Vertical Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                KasirHeaderSection(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    barcodeInput = barcodeInput,
                    onBarcodeChange = { barcodeInput = it },
                    onBarcodeSubmit = onBarcodeSubmit,
                    onCameraScanClick = { showBarcodeScanDialog = true },
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelect = { selectedCategory = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                KasirProductGrid(
                    products = filteredProducts,
                    onProductClick = { viewModel.addToCart(it, 1.0) },
                    modifier = Modifier.weight(1f)
                )

                // Mobile Bottom Cart Summary Bar
                if (cart.isNotEmpty()) {
                    val totalItems = cart.sumOf { it.quantity }.toInt()
                    val totalAmount = cart.sumOf { it.subtotal }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CyberPrimary,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showMobileCartSheet = true }
                            .testTag("mobile_cart_bar")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.25f), CircleShape)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$totalItems Item",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = "Lihat Keranjang",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = formatRupiah(totalAmount),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Mobile Cart Modal BottomSheet
            if (showMobileCartSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showMobileCartSheet = false },
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    CartPanel(
                        cart = cart,
                        onUpdateQty = { id, qty -> viewModel.updateCartQuantity(id, qty) },
                        onRemove = { viewModel.removeFromCart(it) },
                        onClear = { viewModel.clearCart() },
                        onCheckoutClick = {
                            showMobileCartSheet = false
                            showCheckoutDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                            .padding(16.dp)
                    )
                }
            }
        }

        // Checkout Dialog Modal
        if (showCheckoutDialog && cart.isNotEmpty()) {
            CheckoutDialog(
                cart = cart,
                onDismiss = { showCheckoutDialog = false },
                onConfirm = { name, phone, note, method, paid, dueDate ->
                    viewModel.checkoutTransaction(name, phone, note, method, paid, dueDate)
                    showCheckoutDialog = false
                }
            )
        }

        // Barcode Scanner Dialog / Simulator
        if (showBarcodeScanDialog) {
            BarcodeScanDialog(
                onDismiss = { showBarcodeScanDialog = false },
                onBarcodeScanned = { code ->
                    onBarcodeSubmit(code)
                    showBarcodeScanDialog = false
                },
                availableBarcodes = products
            )
        }
    }
}

@Composable
fun KasirHeaderSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    barcodeInput: String,
    onBarcodeChange: (String) -> Unit,
    onBarcodeSubmit: (String) -> Unit,
    onCameraScanClick: () -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Search & Barcode input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Cari produk / barcode...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Hapus", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_product_input")
            )

            // Direct Barcode Hardware Scanner Input (press Enter to add)
            OutlinedTextField(
                value = barcodeInput,
                onValueChange = onBarcodeChange,
                placeholder = { Text("Scan Barcode", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onBarcodeSubmit(barcodeInput) }),
                modifier = Modifier
                    .width(150.dp)
                    .testTag("barcode_scanner_input")
            )

            // Camera / Quick Scanner Trigger
            FilledIconButton(
                onClick = onCameraScanClick,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = CyberPrimary),
                modifier = Modifier.testTag("camera_scan_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Scan Kamera Barcode",
                    tint = Color.White
                )
            }
        }

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val isSelected = cat == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelect(cat) },
                    label = { Text(cat, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("category_chip_$cat")
                )
            }
        }
    }
}

@Composable
fun KasirProductGrid(
    products: List<Product>,
    onProductClick: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    if (products.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Tidak ada produk ditemukan",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
        ) {
            items(products, key = { it.id }) { product ->
                ProductCard(product = product, onClick = { onProductClick(product) })
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit
) {
    val isLowStock = product.stock <= product.minStock

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = product.category,
                    fontSize = 10.sp,
                    color = CyberPrimary,
                    fontWeight = FontWeight.Bold
                )

                if (isLowStock) {
                    StatusBadge(
                        label = "Sisa ${product.stock.toInt()}",
                        backgroundColor = StatusError
                    )
                } else {
                    Text(
                        text = "Stok: ${product.stock.toInt()} ${product.unit}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatRupiah(product.sellingPrice),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.barcode,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(CyberPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CartPanel(
    cart: List<CartItem>,
    onUpdateQty: (String, Double) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onCheckoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalAmount = cart.sumOf { it.subtotal }
    val totalQty = cart.sumOf { it.quantity }.toInt()

    Column(modifier = modifier) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = CyberPrimary)
                Text(
                    text = "Keranjang Penjualan ($totalQty)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            if (cart.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Text("Kosongkan", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        if (cart.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RemoveShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Keranjang masih kosong",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Klik produk atau scan barcode untuk menambahkan",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cart, key = { it.product.id }) { item ->
                    CartItemRow(
                        item = item,
                        onIncrease = { onUpdateQty(item.product.id, item.quantity + 1.0) },
                        onDecrease = { onUpdateQty(item.product.id, item.quantity - 1.0) },
                        onRemove = { onRemove(item.product.id) }
                    )
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Total & Checkout Button
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Tagihan:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = formatRupiah(totalAmount),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = onCheckoutClick,
                enabled = cart.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("checkout_button")
            ) {
                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lanjut Pembayaran",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatRupiah(item.product.sellingPrice)} / ${item.product.unit}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatRupiah(item.subtotal),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberPrimary
                )
            }

            // Stepper
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilledTonalIconButton(
                    onClick = onDecrease,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (item.quantity <= 1.0) Icons.Default.Delete else Icons.Default.Remove,
                        contentDescription = "Kurang",
                        modifier = Modifier.size(14.dp)
                    )
                }

                Text(
                    text = item.quantity.toInt().toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                FilledTonalIconButton(
                    onClick = onIncrease,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutDialog(
    cart: List<CartItem>,
    onDismiss: () -> Unit,
    onConfirm: (customerName: String, customerPhone: String, note: String, paymentMethod: PaymentMethod, amountPaid: Long, dueDate: Long?) -> Unit
) {
    val totalAmount = cart.sumOf { it.subtotal }

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var transactionNote by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.TUNAI) }
    var amountPaidText by remember { mutableStateOf(totalAmount.toString()) }
    var dueDays by remember { mutableStateOf(7) }

    val amountPaid = amountPaidText.toLongOrNull() ?: 0L

    // Auto status and balance
    val changeAmount = if (amountPaid > totalAmount) amountPaid - totalAmount else 0L
    val remainingBalance = if (amountPaid < totalAmount) totalAmount - amountPaid else 0L
    val calculatedStatus = when {
        amountPaid >= totalAmount -> PaymentStatus.LUNAS
        amountPaid > 0 -> PaymentStatus.KURANG_BAYAR
        else -> PaymentStatus.BELUM_DIBAYAR
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pencatatan Pembayaran", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // Total Box
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Total yang Harus Dibayar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                text = formatRupiah(totalAmount),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                item {
                    Text("Metode Pembayaran:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaymentMethod.values().forEach { method ->
                            val isSelected = selectedPaymentMethod == method
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedPaymentMethod = method
                                    if (method == PaymentMethod.CICILAN && amountPaidText == totalAmount.toString()) {
                                        amountPaidText = "0"
                                    } else if (method != PaymentMethod.CICILAN && amountPaidText == "0") {
                                        amountPaidText = totalAmount.toString()
                                    }
                                },
                                label = {
                                    Text(
                                        when (method) {
                                            PaymentMethod.TUNAI -> "Tunai"
                                            PaymentMethod.NON_TUNAI -> "Non-Tunai"
                                            PaymentMethod.CICILAN -> "Cicilan / Tempo"
                                        },
                                        fontSize = 11.sp
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    // Cash Presets for Tunai
                    if (selectedPaymentMethod == PaymentMethod.TUNAI) {
                        Text("Nominal Cepat:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val presets = listOf(totalAmount, 5000L, 10000L, 20000L, 50000L, 100000L).distinct()
                            presets.take(4).forEach { p ->
                                SuggestionChip(
                                    onClick = { amountPaidText = p.toString() },
                                    label = { Text(if (p == totalAmount) "Pas" else formatRupiah(p), fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = amountPaidText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) amountPaidText = input
                        },
                        label = { Text("Jumlah Dibayar (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("amount_paid_input")
                    )
                }

                item {
                    // Live Calculation result: Kembalian or Sisa Piutang
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (amountPaid >= totalAmount) "Kembalian:" else "Sisa Piutang:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (amountPaid >= totalAmount) formatRupiah(changeAmount) else formatRupiah(remainingBalance),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (amountPaid >= totalAmount) StatusSuccess else StatusError
                            )
                        }

                        StatusBadge(
                            label = calculatedStatus.label.uppercase(),
                            backgroundColor = when (calculatedStatus) {
                                PaymentStatus.LUNAS -> StatusSuccess
                                PaymentStatus.KURANG_BAYAR -> StatusWarning
                                PaymentStatus.BELUM_DIBAYAR -> StatusError
                            }
                        )
                    }
                }

                // If Tempo/Cicilan or Piutang, show due date selector
                if (remainingBalance > 0 || selectedPaymentMethod == PaymentMethod.CICILAN) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Jatuh Tempo Piutang:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(7, 14, 30).forEach { days ->
                                    FilterChip(
                                        selected = dueDays == days,
                                        onClick = { dueDays = days },
                                        label = { Text("$days Hari", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Data Pelanggan (Opsional):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                item {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Nama Pelanggan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text("Nomor WhatsApp (misal: 08123456789)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = transactionNote,
                        onValueChange = { transactionNote = it },
                        label = { Text("Catatan Transaksi (misal: Kode Voucher / No HP)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dueDateMs = if (remainingBalance > 0) {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_YEAR, dueDays)
                        cal.timeInMillis
                    } else null

                    onConfirm(
                        customerName,
                        customerPhone,
                        transactionNote,
                        selectedPaymentMethod,
                        amountPaid,
                        dueDateMs
                    )
                },
                modifier = Modifier.testTag("confirm_checkout_button")
            ) {
                Text("Proses Transaksi & Cetak Struk")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun BarcodeScanDialog(
    onDismiss: () -> Unit,
    onBarcodeScanned: (String) -> Unit,
    availableBarcodes: List<Product>
) {
    var manualCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = CyberPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan / Masukkan Barcode", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Arahkan scanner atau pilih barcode dari daftar cepat di bawah:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = manualCode,
                    onValueChange = { manualCode = it },
                    label = { Text("Kode Barcode / SKU") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Barcode Cepat Produk:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                LazyColumn(
                    modifier = Modifier.height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(availableBarcodes) { p ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBarcodeScanned(p.barcode) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(p.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(p.barcode, fontSize = 11.sp, color = CyberPrimary)
                                }
                                Text(formatRupiah(p.sellingPrice), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (manualCode.isNotBlank()) onBarcodeScanned(manualCode)
                }
            ) {
                Text("Pilih / Tambahkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
