package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.CyberPosViewModel
import com.example.ui.components.BarcodeView
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatRupiah
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import java.util.UUID
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KatalogScreen(
    viewModel: CyberPosViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productForBarcodePreview by remember { mutableStateOf<Product?>(null) }
    var productForStockEdit by remember { mutableStateOf<Product?>(null) }

    val filteredList = remember(products, searchQuery) {
        products.filter { p ->
            searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.barcode.contains(searchQuery, ignoreCase = true) ||
                    p.category.contains(searchQuery, ignoreCase = true)
        }
    }

    val lowStockCount = remember(products) {
        products.count { it.stock <= it.minStock }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // TOP HEADER AREA: Page Title & Top "+ Tambah Produk" Button (Requirement 3)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Katalog Produk",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${products.size} Total Produk Terdaftar",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Compact Top Add Product Button (Clean "+" icon only with aria-label)
            FilledIconButton(
                onClick = {
                    productToEdit = null
                    showAddDialog = true
                },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = CyberPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .size(42.dp)
                    .testTag("tambah_produk_top_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Produk",
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
        }

        // Search Bar & Low Stock Warning
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari nama, SKU, atau kategori...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_katalog_input")
            )

            if (lowStockCount > 0) {
                StatusBadge(
                    label = "$lowStockCount Stok Menipis",
                    backgroundColor = StatusError
                )
            }
        }

        // Product List
        if (filteredList.isEmpty()) {
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
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Belum ada produk ditemukan",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Tekan tombol + di atas untuk menambahkan produk",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { product ->
                    KatalogProductCard(
                        product = product,
                        onEdit = {
                            productToEdit = product
                            showAddDialog = true
                        },
                        onDelete = { viewModel.deleteProduct(product.id) },
                        onBarcodeClick = { productForBarcodePreview = product },
                        onQuickStockClick = { productForStockEdit = product }
                    )
                }
            }
        }
    }

    // Add or Edit Product Dialog
    if (showAddDialog) {
        ProductFormDialog(
            initialProduct = productToEdit,
            onDismiss = {
                showAddDialog = false
                productToEdit = null
            },
            onSave = { product ->
                viewModel.saveProduct(product)
                showAddDialog = false
                productToEdit = null
            }
        )
    }

    // Barcode Visualizer Preview Dialog
    if (productForBarcodePreview != null) {
        val p = productForBarcodePreview!!
        AlertDialog(
            onDismissRequest = { productForBarcodePreview = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = CyberPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Barcode Produk", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(p.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Kategori: ${p.category} | Satuan: ${p.unit}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    BarcodeView(
                        code = p.barcode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Harga Jual: ${formatRupiah(p.sellingPrice)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Stok: ${p.stock.toInt()} ${p.unit}", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { productForBarcodePreview = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Quick Stock Update Dialog
    if (productForStockEdit != null) {
        val p = productForStockEdit!!
        var newStockText by remember { mutableStateOf(p.stock.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { productForStockEdit = null },
            title = { Text("Ubah Stok Cepat: ${p.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Stok saat ini: ${p.stock.toInt()} ${p.unit}", fontSize = 12.sp)
                    OutlinedTextField(
                        value = newStockText,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) newStockText = it },
                        label = { Text("Jumlah Stok Baru (${p.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val stockVal = newStockText.toDoubleOrNull() ?: p.stock
                        viewModel.quickUpdateStock(p.id, stockVal)
                        productForStockEdit = null
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { productForStockEdit = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun KatalogProductCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBarcodeClick: () -> Unit,
    onQuickStockClick: () -> Unit
) {
    val isLowStock = product.stock <= product.minStock

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Category, Name & Low Stock Warning
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.category,
                        fontSize = 11.sp,
                        color = CyberPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isLowStock) {
                    StatusBadge(label = "Menipis", backgroundColor = StatusError)
                } else {
                    StatusBadge(label = "Aman", backgroundColor = StatusSuccess)
                }
            }

            // Barcode click pill
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onBarcodeClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyberPrimary)
                    Text(product.barcode, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Text("(Lihat Barcode)", fontSize = 10.sp, color = CyberPrimary)
                }
            }

            // Price & Stock Info Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Harga Jual:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatRupiah(product.sellingPrice),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text("Harga Modal:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatRupiah(product.costPrice), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Stok Saat Ini:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${product.stock.toInt()} ${product.unit}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isLowStock) StatusError else MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onQuickStockClick, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Ubah Stok", modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Action Buttons: Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Data", fontSize = 12.sp)
                }

                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hapus", fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormDialog(
    initialProduct: Product?,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var barcode by remember {
        mutableStateOf(initialProduct?.barcode ?: ("899" + Random.nextInt(100000000, 999999999).toString()))
    }
    var category by remember { mutableStateOf(initialProduct?.category ?: "Voucher") }
    var sellingPriceText by remember { mutableStateOf(initialProduct?.sellingPrice?.toString() ?: "0") }
    var costPriceText by remember { mutableStateOf(initialProduct?.costPrice?.toString() ?: "0") }
    var stockText by remember { mutableStateOf(initialProduct?.stock?.toInt()?.toString() ?: "100") }
    var minStockText by remember { mutableStateOf(initialProduct?.minStock?.toInt()?.toString() ?: "5") }
    // Default unit set to "lembar" (Requirement 2)
    var unit by remember { mutableStateOf(initialProduct?.unit ?: "lembar") }

    val unitOptions = listOf("lembar", "pcs", "pack", "box", "rim", "voucher", "kartu", "bundle")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialProduct != null) "Edit ${initialProduct.name}" else "Tambah Produk Baru",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Produk") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_name_input")
                    )
                }

                item {
                    // Barcode with Auto-Generate Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text("Kode Barcode / SKU") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                barcode = "899" + Random.nextInt(100000000, 999999999).toString()
                            }
                        ) {
                            Icon(Icons.Default.Autorenew, contentDescription = "Acak Barcode")
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Kategori (misal: Voucher, Member, Paket, Pulsa, Aksesoris)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = sellingPriceText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) sellingPriceText = it },
                        label = { Text("Harga Jual (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = costPriceText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) costPriceText = it },
                        label = { Text("Harga Modal / Beli (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = stockText,
                            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) stockText = it },
                            label = { Text("Stok Saat Ini") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minStockText,
                            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) minStockText = it },
                            label = { Text("Stok Minimum") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Text("Satuan Unit (Pilih):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(unitOptions) { opt ->
                            FilterChip(
                                selected = unit == opt,
                                onClick = { unit = opt },
                                label = { Text(opt, fontSize = 11.sp, fontWeight = if (unit == opt) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button

                    val product = Product(
                        id = initialProduct?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        barcode = barcode.trim().ifEmpty { "899" + Random.nextInt(100000000, 999999999) },
                        category = category.trim().ifEmpty { "Umum" },
                        sellingPrice = sellingPriceText.toLongOrNull() ?: 0L,
                        costPrice = costPriceText.toLongOrNull() ?: 0L,
                        stock = stockText.toDoubleOrNull() ?: 0.0,
                        minStock = minStockText.toDoubleOrNull() ?: 5.0,
                        unit = unit
                    )
                    onSave(product)
                },
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
