package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.PurchaseItem
import com.example.data.model.SupplierPurchase
import com.example.ui.CyberPosViewModel
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatRupiah
import com.example.ui.components.formatTanggalPendek
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KulakanScreen(
    viewModel: CyberPosViewModel,
    modifier: Modifier = Modifier
) {
    val purchases by viewModel.purchases.collectAsState()
    val products by viewModel.products.collectAsState()

    var showAddPurchaseDialog by remember { mutableStateOf(false) }
    var purchaseToPayDebt by remember { mutableStateOf<SupplierPurchase?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val totalKulakan = remember(purchases) { purchases.sumOf { it.totalAmount } }
    val totalHutangPemasok = remember(purchases) { purchases.sumOf { it.remainingDebt } }

    val filteredPurchases = remember(purchases, searchQuery) {
        purchases.filter { p ->
            searchQuery.isBlank() ||
                    p.supplierName.contains(searchQuery, ignoreCase = true) ||
                    p.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    p.id.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddPurchaseDialog = true },
                icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = null) },
                text = { Text("Catat Kulakan Baru") },
                containerColor = CyberPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("tambah_kulakan_fab")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Metrics Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Total Pembelian (Kulakan)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(formatRupiah(totalKulakan), fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Total Hutang Pemasok", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(formatRupiah(totalHutangPemasok), fontSize = 16.sp, fontWeight = FontWeight.Black, color = StatusError)
                    }
                }
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari pemasok / no faktur...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Purchase List
            if (filteredPurchases.isEmpty()) {
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
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Text("Belum ada riwayat kulakan stok", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Text("Tekan tombol di bawah untuk mencatat pembelian barang", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredPurchases, key = { it.id }) { purchase ->
                        PurchaseCard(
                            purchase = purchase,
                            onPayDebtClick = { purchaseToPayDebt = purchase }
                        )
                    }
                }
            }
        }
    }

    // Add Purchase Dialog
    if (showAddPurchaseDialog) {
        AddPurchaseDialog(
            products = products,
            onDismiss = { showAddPurchaseDialog = false },
            onConfirm = { supplier, invoice, items, paid, dueDate ->
                viewModel.recordPurchase(supplier, invoice, items, paid, dueDate)
                showAddPurchaseDialog = false
            }
        )
    }

    // Pay Debt Dialog
    if (purchaseToPayDebt != null) {
        val pur = purchaseToPayDebt!!
        var debtAmountText by remember { mutableStateOf(pur.remainingDebt.toString()) }
        var debtNote by remember { mutableStateOf("Pembayaran Hutang Pemasok") }

        AlertDialog(
            onDismissRequest = { purchaseToPayDebt = null },
            title = { Text("Bayar Hutang: ${pur.supplierName}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Sisa Hutang: ${formatRupiah(pur.remainingDebt)}", fontWeight = FontWeight.Bold, color = StatusError)
                    OutlinedTextField(
                        value = debtAmountText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) debtAmountText = it },
                        label = { Text("Jumlah Dibayar (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = debtNote,
                        onValueChange = { debtNote = it },
                        label = { Text("Catatan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = debtAmountText.toLongOrNull() ?: 0L
                        if (amount > 0) {
                            viewModel.paySupplierDebt(pur.id, amount, debtNote)
                            purchaseToPayDebt = null
                        }
                    }
                ) {
                    Text("Simpan Pembayaran")
                }
            },
            dismissButton = {
                TextButton(onClick = { purchaseToPayDebt = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun PurchaseCard(
    purchase: SupplierPurchase,
    onPayDebtClick: () -> Unit
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(purchase.supplierName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Faktur: ${purchase.invoiceNumber} • ${purchase.id}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                StatusBadge(
                    label = purchase.status.label.uppercase(),
                    backgroundColor = when (purchase.status) {
                        PaymentStatus.LUNAS -> StatusSuccess
                        PaymentStatus.KURANG_BAYAR -> StatusWarning
                        PaymentStatus.BELUM_DIBAYAR -> StatusError
                    }
                )
            }

            // Items breakdown
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                purchase.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.productName} (${item.quantity.toInt()} ${item.unit})",
                            fontSize = 11.sp
                        )
                        Text(
                            text = formatRupiah(item.subtotal),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Financial Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Kulakan", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatRupiah(purchase.totalAmount), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column {
                    Text("Sudah Dibayar", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatRupiah(purchase.amountPaid), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StatusSuccess)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Sisa Hutang", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatRupiah(purchase.remainingDebt),
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = if (purchase.remainingDebt > 0) StatusError else StatusSuccess
                    )
                }
            }

            if (purchase.remainingDebt > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onPayDebtClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bayar Hutang", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPurchaseDialog(
    products: List<Product>,
    onDismiss: () -> Unit,
    onConfirm: (supplier: String, invoice: String, items: List<PurchaseItem>, amountPaid: Long, dueDate: Long?) -> Unit
) {
    var supplierName by remember { mutableStateOf("") }
    var invoiceNumber by remember { mutableStateOf("") }
    val itemsList = remember { mutableStateListOf<PurchaseItem>() }

    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var itemQtyText by remember { mutableStateOf("10") }
    var itemCostText by remember { mutableStateOf(selectedProduct?.costPrice?.toString() ?: "0") }

    var amountPaidText by remember { mutableStateOf("0") }
    var isFullPayment by remember { mutableStateOf(true) }

    val totalAmount = itemsList.sumOf { it.subtotal }

    LaunchedEffect(selectedProduct) {
        if (selectedProduct != null) {
            itemCostText = selectedProduct!!.costPrice.toString()
        }
    }

    LaunchedEffect(totalAmount, isFullPayment) {
        if (isFullPayment) {
            amountPaidText = totalAmount.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catat Kulakan / Pembelian Stok", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = supplierName,
                        onValueChange = { supplierName = it },
                        label = { Text("Nama Pemasok / Supplier") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = invoiceNumber,
                        onValueChange = { invoiceNumber = it },
                        label = { Text("Nomor Faktur / Nota Pembelian") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Divider()
                    Text("Pilih Barang yang Dibeli:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Add item inputs
                item {
                    // Product picker chips
                    LazyColumn(modifier = Modifier.height(100.dp)) {
                        items(products) { p ->
                            val isSel = selectedProduct?.id == p.id
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { selectedProduct = p }
                            ) {
                                Row(
                                    modifier = Modifier.padding(6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${p.name} (${p.category})", fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                    Text(p.unit, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = itemQtyText,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) itemQtyText = it },
                            label = { Text("Qty Dibeli") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = itemCostText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) itemCostText = it },
                            label = { Text("Harga Modal (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }

                item {
                    Button(
                        onClick = {
                            if (selectedProduct != null) {
                                val qty = itemQtyText.toDoubleOrNull() ?: 1.0
                                val cost = itemCostText.toLongOrNull() ?: selectedProduct!!.costPrice
                                itemsList.add(
                                    PurchaseItem(
                                        productId = selectedProduct!!.id,
                                        productName = selectedProduct!!.name,
                                        quantity = qty,
                                        costPrice = cost,
                                        unit = selectedProduct!!.unit
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah Barang ke Daftar")
                    }
                }

                // Selected items list
                if (itemsList.isNotEmpty()) {
                    item {
                        Text("Daftar Barang Kulakan (${itemsList.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    items(itemsList) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${item.productName}: ${item.quantity.toInt()} ${item.unit} x ${formatRupiah(item.costPrice)}", fontSize = 11.sp)
                            IconButton(onClick = { itemsList.remove(item) }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus", modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Tagihan Kulakan:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(formatRupiah(totalAmount), fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }
                    }

                    item {
                        Text("Status Pembayaran ke Pemasok:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = isFullPayment,
                                onClick = {
                                    isFullPayment = true
                                    amountPaidText = totalAmount.toString()
                                },
                                label = { Text("Lunas") }
                            )
                            FilterChip(
                                selected = !isFullPayment,
                                onClick = {
                                    isFullPayment = false
                                    amountPaidText = "0"
                                },
                                label = { Text("Hutang / Tempo") }
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = amountPaidText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) amountPaidText = it },
                            label = { Text("Jumlah Dibayar Saat Ini (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (supplierName.isNotBlank() && itemsList.isNotEmpty()) {
                        val paid = amountPaidText.toLongOrNull() ?: 0L
                        val dueDate = if (paid < totalAmount) {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.DAY_OF_YEAR, 14)
                            cal.timeInMillis
                        } else null

                        onConfirm(supplierName, invoiceNumber, itemsList.toList(), paid, dueDate)
                    }
                },
                enabled = supplierName.isNotBlank() && itemsList.isNotEmpty()
            ) {
                Text("Simpan Kulakan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
