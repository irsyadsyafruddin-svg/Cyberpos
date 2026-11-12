package com.example.ui.screens

import androidx.compose.foundation.background
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
import com.example.data.model.Expense
import com.example.data.model.PaymentMethod
import com.example.ui.CyberPosViewModel
import com.example.ui.components.formatRupiah
import com.example.ui.components.formatTanggal
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengeluaranScreen(
    viewModel: CyberPosViewModel,
    modifier: Modifier = Modifier
) {
    val expenses by viewModel.expenses.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val purchases by viewModel.purchases.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("Semua") }

    // Real-Time Arus Kas (Cash Flow) Calculation:
    // Total Kas Masuk:
    // Direct transaction payments and all installments recorded
    val totalKasMasuk = remember(transactions) {
        transactions.sumOf { trx ->
            if (trx.installments.isNotEmpty()) {
                trx.installments.sumOf { it.amount }
            } else {
                trx.amountPaid
            }
        }
    }

    // Total Kas Keluar:
    // 1. Operational expenses
    val totalPengeluaranOperasional = remember(expenses) {
        expenses.sumOf { it.amount }
    }
    // 2. Supplier purchases paid (kulakan dibayar)
    val totalBayarKulakan = remember(purchases) {
        purchases.sumOf { pur ->
            if (pur.debtRepayments.isNotEmpty()) {
                pur.debtRepayments.sumOf { it.amount }
            } else {
                pur.amountPaid
            }
        }
    }

    val totalKasKeluar = totalPengeluaranOperasional + totalBayarKulakan
    val saldoKasBersih = totalKasMasuk - totalKasKeluar

    val categories = listOf("Semua", "Listrik & Air", "Sewa Tempat", "Gaji Karyawan", "Transportasi", "Makan & Minum", "Pemeliharaan", "Lain-lain")

    val filteredExpenses = remember(expenses, selectedCategoryFilter) {
        if (selectedCategoryFilter == "Semua") expenses
        else expenses.filter { it.category == selectedCategoryFilter }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Catat Pengeluaran") },
                containerColor = CyberPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("tambah_pengeluaran_fab")
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
            // Arus Kas (Cash Flow) Real-Time Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = CyberPrimary)
                            Text("Ringkasan Arus Kas (Cash Flow)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Text(
                            text = "Real-Time",
                            fontSize = 11.sp,
                            color = CyberPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Kas Masuk (+)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatRupiah(totalKasMasuk), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusSuccess)
                            Text("Penjualan & Piutang", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Kas Keluar (-)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatRupiah(totalKasKeluar), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusError)
                            Text("Operasional & Kulakan", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Saldo Kas Bersih", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = formatRupiah(saldoKasBersih),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = if (saldoKasBersih >= 0) StatusSuccess else StatusError
                            )
                            Text("Arus Kas Riil", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Category Filter
            Text("Kategori Pengeluaran:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.take(4).forEach { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = { selectedCategoryFilter = cat },
                        label = { Text(cat, fontSize = 11.sp) }
                    )
                }
            }

            // Expenses List
            if (filteredExpenses.isEmpty()) {
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
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(44.dp)
                        )
                        Text("Belum ada catatan pengeluaran", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredExpenses, key = { it.id }) { exp ->
                        ExpenseItemCard(
                            expense = exp,
                            onDelete = { viewModel.deleteExpense(exp.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(
            categories = categories.filter { it != "Semua" },
            onDismiss = { showAddDialog = false },
            onConfirm = { cat, amount, note, method ->
                viewModel.recordExpense(cat, amount, note, method)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ExpenseItemCard(
    expense: Expense,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(expense.category, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("• ${expense.paymentMethod.label}", fontSize = 10.sp, color = CyberPrimary)
                }
                if (expense.note.isNotBlank()) {
                    Text(expense.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = "${formatTanggal(expense.timestamp)} oleh ${expense.recordedBy}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatRupiah(expense.amount),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = StatusError
                )

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    categories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (category: String, amount: Long, note: String, method: PaymentMethod) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: "Lain-lain") }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.TUNAI) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catat Pengeluaran Operasional", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Kategori Pengeluaran:", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                // Category chips grid
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) amountText = it },
                    label = { Text("Nominal Pengeluaran (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Metode Pembayaran:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedMethod == PaymentMethod.TUNAI,
                        onClick = { selectedMethod = PaymentMethod.TUNAI },
                        label = { Text("Tunai") }
                    )
                    FilterChip(
                        selected = selectedMethod == PaymentMethod.NON_TUNAI,
                        onClick = { selectedMethod = PaymentMethod.NON_TUNAI },
                        label = { Text("Transfer / Bank") }
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan / Keterangan (misal: Token listrik toko)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    if (amount > 0) {
                        onConfirm(selectedCategory, amount, note, selectedMethod)
                    }
                }
            ) {
                Text("Simpan Pengeluaran")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
