package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PaymentStatus
import com.example.data.model.ReceiptSettings
import com.example.data.model.Transaction
import com.example.ui.components.BarcodeView
import com.example.ui.components.formatRupiah
import com.example.ui.components.formatTanggal
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun StrukDialog(
    transaction: Transaction,
    settings: ReceiptSettings,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedWidthMm by remember { mutableStateOf(settings.paperWidthMm) }

    val receiptText = remember(transaction, settings, selectedWidthMm) {
        buildReceiptString(transaction, settings, selectedWidthMm)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar with width selector and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = CyberPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Pratinjau Struk Thermal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                // Width Toggle (58mm vs 80mm)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ukuran Kertas:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedWidthMm == 58,
                            onClick = { selectedWidthMm = 58 },
                            label = { Text("58 mm (Standar)", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedWidthMm == 80,
                            onClick = { selectedWidthMm = 80 },
                            label = { Text("80 mm (Lebar)", fontSize = 11.sp) }
                        )
                    }
                }

                Divider()

                // Thermal Receipt Paper Visual Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val paperWidth = if (selectedWidthMm == 58) 280.dp else 360.dp

                    Card(
                        modifier = Modifier
                            .width(paperWidth)
                            .wrapContentHeight()
                            .verticalScroll(rememberScrollState()),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Store Header
                            Text(
                                text = settings.storeName.uppercase(),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )

                            if (settings.showAddress && settings.address.isNotBlank()) {
                                Text(
                                    text = settings.address,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (settings.showPhone && settings.phoneNumber.isNotBlank()) {
                                Text(
                                    text = "Telp/WA: ${settings.phoneNumber}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (settings.openingMessage.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = settings.openingMessage,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }

                            DashedDivider()

                            // Transaction Info
                            ReceiptRow("No. Nota", transaction.id)
                            ReceiptRow("Tanggal", formatTanggal(transaction.timestamp))
                            if (settings.showCashier) {
                                ReceiptRow("Kasir", transaction.cashierName)
                            }
                            if (transaction.customerName.isNotBlank()) {
                                ReceiptRow("Pelanggan", transaction.customerName)
                            }
                            if (transaction.customerPhone.isNotBlank()) {
                                ReceiptRow("No. WA", transaction.customerPhone)
                            }
                            if (settings.showNote && transaction.transactionNote.isNotBlank()) {
                                ReceiptRow("Catatan", transaction.transactionNote)
                            }

                            DashedDivider()

                            // Itemized Products
                            transaction.items.forEach { item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item.product.name,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "  ${item.quantity.toInt()} x ${formatRupiah(item.product.sellingPrice)}",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.DarkGray
                                        )
                                        Text(
                                            text = formatRupiah(item.subtotal),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }

                            DashedDivider()

                            // Totals and Payment
                            ReceiptRow("TOTAL", formatRupiah(transaction.totalAmount), isBold = true)
                            ReceiptRow("Metode Bayar", transaction.paymentMethod.label)
                            ReceiptRow("Jumlah Dibayar", formatRupiah(transaction.amountPaid))

                            if (transaction.changeAmount > 0) {
                                ReceiptRow("Kembalian", formatRupiah(transaction.changeAmount), isBold = true)
                            }

                            if (settings.showRemainingBalance && transaction.remainingBalance > 0) {
                                ReceiptRow("Sisa Piutang", formatRupiah(transaction.remainingBalance), isBold = true, isWarning = true)
                            }

                            val statusText = when (transaction.status) {
                                PaymentStatus.LUNAS -> "LUNAS"
                                PaymentStatus.KURANG_BAYAR -> "KURANG BAYAR"
                                PaymentStatus.BELUM_DIBAYAR -> "BELUM DIBAYAR"
                            }
                            ReceiptRow("Status Nota", statusText, isBold = true)

                            // Installment history if any
                            if (transaction.installments.isNotEmpty()) {
                                DashedDivider()
                                Text(
                                    text = "RIWAYAT PEMBAYARAN:",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray
                                )
                                transaction.installments.forEach { ins ->
                                    ReceiptRow(
                                        formatTanggal(ins.date),
                                        formatRupiah(ins.amount) + " (" + ins.paymentMethod.label + ")"
                                    )
                                }
                            }

                            DashedDivider()

                            // Barcode visual representation
                            BarcodeView(
                                code = transaction.id,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                barColor = Color.Black,
                                backgroundColor = Color.White
                            )

                            // Footer Note
                            if (settings.closingMessage.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = settings.closingMessage,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Action Buttons: Kirim ke WA, Salin, Cetak/Bagikan
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // WhatsApp Button (CRITICAL Requirement)
                    Button(
                        onClick = {
                            sendReceiptToWhatsApp(context, transaction, receiptText)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("kirim_wa_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (transaction.customerPhone.isNotBlank()) "Kirim Struk ke WA (${transaction.customerPhone})" else "Kirim Struk ke WhatsApp",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Struk CyberPos", receiptText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Teks struk berhasil disalin!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salin Struk")
                        }

                        OutlinedButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Struk Pembelian - ${settings.storeName}")
                                    putExtra(Intent.EXTRA_TEXT, receiptText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Bagikan Struk"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bagikan")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashedDivider() {
    Text(
        text = "------------------------------------------",
        color = Color.Gray,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        maxLines = 1,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
fun ReceiptRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    isWarning: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = Color.Black
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isWarning) StatusError else Color.Black
        )
    }
}

fun buildReceiptString(
    transaction: Transaction,
    settings: ReceiptSettings,
    widthMm: Int
): String {
    val sep = if (widthMm == 80) "========================================" else "--------------------------------"
    val sb = StringBuilder()
    sb.appendLine("*${settings.storeName.uppercase()}*")
    if (settings.showAddress && settings.address.isNotBlank()) sb.appendLine(settings.address)
    if (settings.showPhone && settings.phoneNumber.isNotBlank()) sb.appendLine("WA/Telp: ${settings.phoneNumber}")
    if (settings.openingMessage.isNotBlank()) sb.appendLine(settings.openingMessage)
    sb.appendLine(sep)
    sb.appendLine("No. Nota : ${transaction.id}")
    sb.appendLine("Tanggal  : ${formatTanggal(transaction.timestamp)}")
    if (settings.showCashier) sb.appendLine("Kasir    : ${transaction.cashierName}")
    if (transaction.customerName.isNotBlank()) sb.appendLine("Pelanggan: ${transaction.customerName}")
    if (transaction.customerPhone.isNotBlank()) sb.appendLine("No. WA   : ${transaction.customerPhone}")
    if (settings.showNote && transaction.transactionNote.isNotBlank()) sb.appendLine("Catatan  : ${transaction.transactionNote}")
    sb.appendLine(sep)

    for (item in transaction.items) {
        sb.appendLine(item.product.name)
        sb.appendLine("  ${item.quantity.toInt()} x ${formatRupiah(item.product.sellingPrice)} = ${formatRupiah(item.subtotal)}")
    }

    sb.appendLine(sep)
    sb.appendLine("TOTAL          : ${formatRupiah(transaction.totalAmount)}")
    sb.appendLine("Metode Bayar   : ${transaction.paymentMethod.label}")
    sb.appendLine("Jumlah Bayar   : ${formatRupiah(transaction.amountPaid)}")
    if (transaction.changeAmount > 0) {
        sb.appendLine("Kembalian      : ${formatRupiah(transaction.changeAmount)}")
    }
    if (settings.showRemainingBalance && transaction.remainingBalance > 0) {
        sb.appendLine("SISA PIUTANG   : ${formatRupiah(transaction.remainingBalance)}")
    }
    sb.appendLine("STATUS         : ${transaction.status.label.uppercase()}")

    if (transaction.installments.isNotEmpty()) {
        sb.appendLine(sep)
        sb.appendLine("Riwayat Cicilan:")
        for (ins in transaction.installments) {
            sb.appendLine("- ${formatTanggal(ins.date)}: ${formatRupiah(ins.amount)} (${ins.paymentMethod.label})")
        }
    }

    if (settings.closingMessage.isNotBlank()) {
        sb.appendLine(sep)
        sb.appendLine(settings.closingMessage)
    }

    return sb.toString()
}

fun sendReceiptToWhatsApp(
    context: Context,
    transaction: Transaction,
    receiptText: String
) {
    try {
        var cleanPhone = transaction.customerPhone.replace("[^0-9]".toRegex(), "")
        if (cleanPhone.startsWith("0")) {
            cleanPhone = "62" + cleanPhone.substring(1)
        }

        val encodedMessage = URLEncoder.encode(receiptText, StandardCharsets.UTF_8.toString())
        val url = if (cleanPhone.isNotBlank()) {
            "https://wa.me/$cleanPhone?text=$encodedMessage"
        } else {
            "https://wa.me/?text=$encodedMessage"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback to general share intent if WhatsApp is not directly installed
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, receiptText)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(shareIntent, "Kirim Struk via..."))
    }
}
