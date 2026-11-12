package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatRupiah(amount: Long): String {
    val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
        groupingSeparator = '.'
        monetaryDecimalSeparator = ','
    }
    val formatter = DecimalFormat("#,###", symbols)
    return "Rp " + formatter.format(amount)
}

fun formatTanggal(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    return sdf.format(Date(timestamp))
}

fun formatTanggalPendek(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
    return sdf.format(Date(timestamp))
}

@Composable
fun BarcodeView(
    code: String,
    modifier: Modifier = Modifier,
    barColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    Column(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            val totalWidth = size.width
            val height = size.height

            // Generate deterministic bar widths based on string characters
            val hashPattern = buildString {
                for (char in code) {
                    val v = char.code % 16
                    append(v.toString(2).padStart(4, '0'))
                }
                if (isEmpty()) append("1010101010101010")
            }

            val barCount = hashPattern.length.coerceAtLeast(30)
            val barWidth = totalWidth / barCount

            // Draw guard bars at left
            drawRect(barColor, Offset(0f, 0f), Size(barWidth * 1.5f, height))
            drawRect(barColor, Offset(barWidth * 2.5f, 0f), Size(barWidth * 1.5f, height))

            var x = barWidth * 5f
            for (i in hashPattern.indices) {
                if (hashPattern[i] == '1') {
                    val w = if (i % 3 == 0) barWidth * 1.8f else barWidth * 0.9f
                    drawRect(barColor, Offset(x, 0f), Size(w, height * 0.92f))
                    x += w + barWidth * 0.5f
                } else {
                    x += barWidth * 1.2f
                }
                if (x >= totalWidth - (barWidth * 6f)) break
            }

            // Draw guard bars at right
            drawRect(barColor, Offset(totalWidth - barWidth * 4f, 0f), Size(barWidth * 1.5f, height))
            drawRect(barColor, Offset(totalWidth - barWidth * 1.5f, 0f), Size(barWidth * 1.5f, height))
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = barColor,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun StatusBadge(
    label: String,
    backgroundColor: Color,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(backgroundColor.copy(alpha = 0.15f), RoundedCornerShape(100.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = backgroundColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}
