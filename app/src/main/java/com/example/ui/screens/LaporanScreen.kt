package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentStatus
import com.example.ui.CyberPosViewModel
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatRupiah
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanScreen(
    viewModel: CyberPosViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val products by viewModel.products.collectAsState()
    val purchases by viewModel.purchases.collectAsState()

    var timeFilter by remember { mutableStateOf("Hari Ini") } // "Hari Ini", "Minggu Ini", "Bulan Ini", "Semua"

    // Time filter bounds
    val filterStartTime = remember(timeFilter) {
        val cal = Calendar.getInstance()
        when (timeFilter) {
            "Hari Ini" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            "Minggu Ini" -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            "Bulan Ini" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            else -> 0L
        }
    }

    // Filtered data by selected period
    val periodTransactions = remember(transactions, filterStartTime) {
        transactions.filter { it.timestamp >= filterStartTime }
    }
    val periodExpenses = remember(expenses, filterStartTime) {
        expenses.filter { it.timestamp >= filterStartTime }
    }

    // Metrics
    val totalOmzet = remember(periodTransactions) {
        periodTransactions.sumOf { it.totalAmount }
    }
    val totalCost = remember(periodTransactions) {
        periodTransactions.sumOf { it.totalCost }
    }
    val labaKotor = totalOmzet - totalCost
    val totalOperasional = remember(periodExpenses) {
        periodExpenses.sumOf { it.amount }
    }
    val labaBersihAkhir = labaKotor - totalOperasional

    val totalPiutangBelumLunas = remember(transactions) {
        transactions.filter { it.status != PaymentStatus.LUNAS }.sumOf { it.remainingBalance }
    }
    val totalHutangPemasok = remember(purchases) {
        purchases.sumOf { it.remainingDebt }
    }

    // Top Selling Products
    val topProducts = remember(periodTransactions) {
        val countMap = mutableMapOf<String, Pair<String, Pair<Double, Long>>>() // id -> (name, (qty, revenue))
        for (trx in periodTransactions) {
            for (item in trx.items) {
                val current = countMap[item.product.id]
                val prevQty = current?.second?.first ?: 0.0
                val prevRev = current?.second?.second ?: 0L
                countMap[item.product.id] = item.product.name to (prevQty + item.quantity to prevRev + item.subtotal)
            }
        }
        countMap.values.sortedByDescending { it.second.first }
    }

    // Top Customers
    val topCustomers = remember(periodTransactions) {
        val custMap = mutableMapOf<String, Pair<Int, Long>>() // name -> (orders, spend)
        for (trx in periodTransactions) {
            if (trx.customerName.isNotBlank()) {
                val current = custMap[trx.customerName]
                val prevOrders = current?.first ?: 0
                val prevSpend = current?.second ?: 0L
                custMap[trx.customerName] = (prevOrders + 1) to (prevSpend + trx.totalAmount)
            }
        }
        custMap.entries.sortedByDescending { it.value.second }
    }

    // Low stock products
    val lowStockProducts = remember(products) {
        products.filter { it.stock <= it.minStock }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Period Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Hari Ini", "Minggu Ini", "Bulan Ini", "Semua").forEach { filter ->
                    FilterChip(
                        selected = timeFilter == filter,
                        onClick = { timeFilter = filter },
                        label = { Text(filter, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("time_filter_$filter")
                    )
                }
            }
        }

        // Key Financial KPI Cards
        item {
            Text("Kinerja Finansial ($timeFilter):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                KpiCard(
                    title = "Omzet Penjualan",
                    value = formatRupiah(totalOmzet),
                    color = CyberPrimary,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Laba Kotor",
                    value = formatRupiah(labaKotor),
                    color = StatusSuccess,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                KpiCard(
                    title = "Total Pengeluaran",
                    value = formatRupiah(totalOperasional),
                    color = StatusError,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Laba Bersih Riil",
                    value = formatRupiah(labaBersihAkhir),
                    color = if (labaBersihAkhir >= 0) StatusSuccess else StatusError,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Piutang & Hutang Summary
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Sisa Piutang Pelanggan", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatRupiah(totalPiutangBelumLunas), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusWarning)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Sisa Hutang ke Pemasok", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatRupiah(totalHutangPemasok), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusError)
                    }
                }
            }
        }

        // Produk Paling Laris
        item {
            Text("Produk Paling Laris:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))

            if (topProducts.isEmpty()) {
                Text("Belum ada penjualan dalam periode $timeFilter.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        topProducts.take(5).forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "#${index + 1}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = CyberPrimary
                                    )
                                    Text(item.first, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                }
                                Text(
                                    text = "${item.second.first.toInt()} terjual (${formatRupiah(item.second.second)})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (index < topProducts.take(5).size - 1) Divider()
                        }
                    }
                }
            }
        }

        // Pelanggan Sering Beli
        item {
            Text("Pelanggan Paling Aktif:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))

            if (topCustomers.isEmpty()) {
                Text("Belum ada transaksi dengan nama pelanggan dalam periode $timeFilter.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        topCustomers.take(5).forEachIndexed { index, entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = CyberPrimary, modifier = Modifier.size(16.dp))
                                    Text(entry.key, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                }
                                Text(
                                    text = "${entry.value.first}x Beli (${formatRupiah(entry.value.second)})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (index < topCustomers.take(5).size - 1) Divider()
                        }
                    }
                }
            }
        }

        // Peringatan Stok Menipis
        item {
            Text("Peringatan Stok Menipis (${lowStockProducts.size}):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))

            if (lowStockProducts.isEmpty()) {
                Text("Semua stok produk dalam kondisi aman.", fontSize = 12.sp, color = StatusSuccess)
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        lowStockProducts.forEachIndexed { index, p ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(p.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Min: ${p.minStock.toInt()} ${p.unit} • ${p.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                StatusBadge(
                                    label = "Sisa ${p.stock.toInt()} ${p.unit}",
                                    backgroundColor = StatusError
                                )
                            }
                            if (index < lowStockProducts.size - 1) Divider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}
