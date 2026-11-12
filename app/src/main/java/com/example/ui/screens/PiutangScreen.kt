package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.Transaction
import com.example.ui.CyberPosViewModel
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatRupiah
import com.example.ui.components.formatTanggal
import com.example.ui.components.formatTanggalPendek
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PiutangScreen(
    viewModel: CyberPosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val receiptSettings by viewModel.receiptSettings.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Belum Lunas") } // "Belum Lunas", "Semua", "Lunas"
    var transactionToPay by remember { mutableStateOf<Transaction?>(null) }

    // Filter transactions
    val piutangList = remember(transactions, selectedFilter, searchQuery) {
        transactions.filter { trx ->
            val matchStatus = when (selectedFilter) {
                "Belum Lunas" -> trx.status == PaymentStatus.KURANG_BAYAR || trx.status == PaymentStatus.BELUM_DIBAYAR
                "Lunas" -> trx.status == PaymentStatus.LUNAS
                else -> true
            }
            val matchQuery = searchQuery.isBlank() ||
                    trx.customerName.contains(searchQuery, ignoreCase = true) ||
                    trx.customerPhone.contains(searchQuery, ignoreCase = true) ||
                    trx.id.contains(searchQuery, ignoreCase = true)

            matchStatus && matchQuery
        }
    }

    val totalPiutangBelumLunas = remember(transactions) {
        transactions.filter { it.status != PaymentStatus.LUNAS }.sumOf { it.remainingBalance }
    }
    val countPelangganPiutang = remember(transactions) {
        transactions.filter { it.status != PaymentStatus.LUNAS }.size
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Piutang Belum Lunas",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = formatRupiah(totalPiutangBelumLunas),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Box(
                    modifier = Modifier
                        .background(StatusError.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$countPelangganPiutang Nota Tertunda",
                        color = StatusError,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Search & Filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari nama / WA / nota...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_piutang_input")
            )
        }

        // Status Filter Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("Belum Lunas", "Semua", "Lunas").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 12.sp) },
                    modifier = Modifier.testTag("filter_piutang_$filter")
                )
            }
        }

        // List
        if (piutangList.isEmpty()) {
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
                        imageVector = Icons.Default.CheckCircleOutline,
                        contentDescription = null,
                        tint = StatusSuccess,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (selectedFilter == "Belum Lunas") "Hebat! Tidak ada piutang yang belum lunas." else "Tidak ada transaksi ditemukan.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(piutangList, key = { it.id }) { trx ->
                    PiutangCard(
                        transaction = trx,
                        onPayInstallment = { transactionToPay = trx },
                        onViewReceipt = { viewModel.openReceipt(trx) },
                        onSendReminder = {
                            sendWhatsAppReminder(context, trx, receiptSettings.storeName)
                        }
                    )
                }
            }
        }
    }

    // Installment Payment Dialog
    if (transactionToPay != null) {
        val trx = transactionToPay!!
        BayarCicilanDialog(
            transaction = trx,
            onDismiss = { transactionToPay = null },
            onConfirm = { amount, method, note ->
                viewModel.payInstallment(trx.id, amount, method, note)
                transactionToPay = null
            }
        )
    }
}

@Composable
fun PiutangCard(
    transaction: Transaction,
    onPayInstallment: () -> Unit,
    onViewReceipt: () -> Unit,
    onSendReminder: () -> Unit
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
            // Header Row: Customer Name & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (transaction.customerName.isNotBlank()) transaction.customerName else "Pelanggan Tanpa Nama",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (transaction.customerPhone.isNotBlank()) {
                        Text(
                            text = "WA: ${transaction.customerPhone}",
                            fontSize = 11.sp,
                            color = CyberPrimary
                        )
                    }
                }

                StatusBadge(
                    label = transaction.status.label.uppercase(),
                    backgroundColor = when (transaction.status) {
                        PaymentStatus.LUNAS -> StatusSuccess
                        PaymentStatus.KURANG_BAYAR -> StatusWarning
                        PaymentStatus.BELUM_DIBAYAR -> StatusError
                    }
                )
            }

            // Nota ID & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = transaction.id,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTanggalPendek(transaction.timestamp),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Financial Summary Row: Total, Sudah Dibayar, Sisa Piutang
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Tagihan", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatRupiah(transaction.totalAmount), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column {
                    Text("Sudah Dibayar", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatRupiah(transaction.amountPaid), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StatusSuccess)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Sisa Piutang", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatRupiah(transaction.remainingBalance),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = if (transaction.remainingBalance > 0) StatusError else StatusSuccess
                    )
                }
            }

            if (transaction.dueDate != null && transaction.remainingBalance > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusWarning)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Jatuh Tempo: ${formatTanggalPendek(transaction.dueDate)}",
                        fontSize = 11.sp,
                        color = StatusWarning,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onViewReceipt) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Struk", fontSize = 12.sp)
                }

                if (transaction.customerPhone.isNotBlank() && transaction.remainingBalance > 0) {
                    TextButton(
                        onClick = onSendReminder,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF25D366))
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ingatkan WA", fontSize = 12.sp)
                    }
                }

                if (transaction.remainingBalance > 0) {
                    Button(
                        onClick = onPayInstallment,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bayar Cicilan", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BayarCicilanDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onConfirm: (amount: Long, method: PaymentMethod, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf(transaction.remainingBalance.toString()) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.TUNAI) }
    var note by remember { mutableStateOf("Pembayaran Cicilan") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = CyberPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bayar Cicilan / Pelunasan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Nota: ${transaction.id} | ${transaction.customerName}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sisa Piutang: ${formatRupiah(transaction.remainingBalance)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = StatusError
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                    label = { Text("Jumlah Pembayaran (Rp)") },
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
                        label = { Text("Non-Tunai / QRIS") }
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan Pembayaran") },
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
                        onConfirm(amount, selectedMethod, note)
                    }
                }
            ) {
                Text("Simpan Pembayaran")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

fun sendWhatsAppReminder(
    context: Context,
    transaction: Transaction,
    storeName: String
) {
    try {
        var cleanPhone = transaction.customerPhone.replace("[^0-9]".toRegex(), "")
        if (cleanPhone.startsWith("0")) {
            cleanPhone = "62" + cleanPhone.substring(1)
        }

        val dueDateStr = if (transaction.dueDate != null) formatTanggalPendek(transaction.dueDate) else "-"

        val reminderText = """
Halo Kak ${transaction.customerName.ifBlank { "Pelanggan" }},
Salam hangat dari *$storeName*.

Kami ingin menginformasikan rincian tagihan pembelian Anda:
- No. Nota: *${transaction.id}*
- Tanggal Transaksi: ${formatTanggalPendek(transaction.timestamp)}
- Total Belanja: ${formatRupiah(transaction.totalAmount)}
- Sudah Dibayar: ${formatRupiah(transaction.amountPaid)}
- *Sisa Piutang: ${formatRupiah(transaction.remainingBalance)}*
- Tanggal Jatuh Tempo: $dueDateStr

Mohon konfirmasi jika pembayaran telah dilakukan. Terima kasih atas kerja samanya! 🙏
        """.trimIndent()

        val encoded = URLEncoder.encode(reminderText, StandardCharsets.UTF_8.toString())
        val url = if (cleanPhone.isNotBlank()) "https://wa.me/$cleanPhone?text=$encoded" else "https://wa.me/?text=$encoded"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Pengingat Piutang untuk ${transaction.customerName}: Sisa ${formatRupiah(transaction.remainingBalance)}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(shareIntent, "Kirim Pengingat"))
    }
}
