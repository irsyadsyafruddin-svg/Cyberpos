package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReceiptSettings
import com.example.data.model.Role
import com.example.data.model.User
import com.example.ui.CyberPosViewModel
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengaturanScreen(
    viewModel: CyberPosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val receiptSettings by viewModel.receiptSettings.collectAsState()
    val users by viewModel.users.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var activeTab by remember { mutableStateOf("Struk") } // "Struk", "Pengguna", "Backup"

    var showAddUserDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<User?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Struk Form states
    var storeName by remember(receiptSettings) { mutableStateOf(receiptSettings.storeName) }
    var address by remember(receiptSettings) { mutableStateOf(receiptSettings.address) }
    var phone by remember(receiptSettings) { mutableStateOf(receiptSettings.phoneNumber) }
    var openingMsg by remember(receiptSettings) { mutableStateOf(receiptSettings.openingMessage) }
    var closingMsg by remember(receiptSettings) { mutableStateOf(receiptSettings.closingMessage) }
    var showAddress by remember(receiptSettings) { mutableStateOf(receiptSettings.showAddress) }
    var showPhone by remember(receiptSettings) { mutableStateOf(receiptSettings.showPhone) }
    var showCashier by remember(receiptSettings) { mutableStateOf(receiptSettings.showCashier) }
    var showNote by remember(receiptSettings) { mutableStateOf(receiptSettings.showNote) }
    var showRemaining by remember(receiptSettings) { mutableStateOf(receiptSettings.showRemainingBalance) }
    var paperWidth by remember(receiptSettings) { mutableStateOf(receiptSettings.paperWidthMm) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tab Selector: Struk, Pengguna & PIN, Backup & Restore
        TabRow(
            selectedTabIndex = when (activeTab) {
                "Struk" -> 0
                "Pengguna" -> 1
                else -> 2
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = activeTab == "Struk",
                onClick = { activeTab = "Struk" },
                text = { Text("Struk Toko", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = activeTab == "Pengguna",
                onClick = { activeTab = "Pengguna" },
                text = { Text("Pengguna & PIN", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = activeTab == "Backup",
                onClick = { activeTab = "Backup" },
                text = { Text("Backup Data", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        when (activeTab) {
            "Struk" -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = storeName,
                            onValueChange = { storeName = it },
                            label = { Text("Nama Toko / Usaha") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("store_name_input")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Alamat Toko") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Nomor Telepon / WhatsApp Toko") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = openingMsg,
                            onValueChange = { openingMsg = it },
                            label = { Text("Teks Pembuka Struk") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = closingMsg,
                            onValueChange = { closingMsg = it },
                            label = { Text("Teks Penutup / Kebijakan Toko") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Text("Pengaturan Elemen Struk:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    item {
                        SettingToggleRow("Tampilkan Alamat Toko", showAddress) { showAddress = it }
                    }
                    item {
                        SettingToggleRow("Tampilkan Nomor Telepon Toko", showPhone) { showPhone = it }
                    }
                    item {
                        SettingToggleRow("Tampilkan Nama Kasir", showCashier) { showCashier = it }
                    }
                    item {
                        SettingToggleRow("Tampilkan Catatan Transaksi", showNote) { showNote = it }
                    }
                    item {
                        SettingToggleRow("Tampilkan Sisa Piutang (Jika Ada)", showRemaining) { showRemaining = it }
                    }

                    item {
                        Text("Lebar Kertas Default:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = paperWidth == 58,
                                onClick = { paperWidth = 58 },
                                label = { Text("58 mm (Mini Thermal)") }
                            )
                            FilterChip(
                                selected = paperWidth == 80,
                                onClick = { paperWidth = 80 },
                                label = { Text("80 mm (Thermal Lebar)") }
                            )
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                val updated = receiptSettings.copy(
                                    storeName = storeName.trim().ifEmpty { "CyberPos" },
                                    address = address.trim(),
                                    phoneNumber = phone.trim(),
                                    openingMessage = openingMsg.trim(),
                                    closingMessage = closingMsg.trim(),
                                    showAddress = showAddress,
                                    showPhone = showPhone,
                                    showCashier = showCashier,
                                    showNote = showNote,
                                    showRemainingBalance = showRemaining,
                                    paperWidthMm = paperWidth
                                )
                                viewModel.updateReceiptSettings(updated)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                            modifier = Modifier.fillMaxWidth().testTag("save_receipt_settings_button")
                        ) {
                            Text("Simpan Pengaturan Struk")
                        }
                    }
                }
            }

            "Pengguna" -> {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Daftar Pengguna & Hak Akses:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Button(
                            onClick = {
                                userToEdit = null
                                showAddUserDialog = true
                            }
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tambah Pengguna", fontSize = 12.sp)
                        }
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(users, key = { it.id }) { u ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(u.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            if (u.id == currentUser.id) {
                                                Text("(Sedang Aktif)", fontSize = 10.sp, color = CyberPrimary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text("Peran: ${u.role.label} • PIN: **** (${u.pin})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            if (u.role == Role.OWNER) "Hak akses: Semua menu & laporan" else "Hak akses: Kasir, Piutang, Pengeluaran",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row {
                                        IconButton(onClick = {
                                            userToEdit = u
                                            showAddUserDialog = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                                        }
                                        if (users.size > 1 && u.id != currentUser.id) {
                                            IconButton(onClick = { viewModel.deleteUser(u.id) }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "Backup" -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = CyberPrimary)
                                    Text("Cadangkan Data (Backup JSON)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Text(
                                    "Simpan seluruh data CyberPos (produk, stok, transaksi, piutang, kulakan, pengeluaran) ke dalam format JSON. Aman dan siap disimpan ke Google Drive, WhatsApp, atau perangkat Anda.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val json = viewModel.exportBackupJson()
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("CyberPos Backup", json)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Data JSON berhasil disalin ke Clipboard!", Toast.LENGTH_LONG).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Salin Data JSON")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val json = viewModel.exportBackupJson()
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/json"
                                                putExtra(Intent.EXTRA_SUBJECT, "CyberPos_Backup_${System.currentTimeMillis()}.json")
                                                putExtra(Intent.EXTRA_TEXT, json)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Bagikan / Ekspor Backup JSON"))
                                        }
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Bagikan File")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = StatusSuccess)
                                    Text("Pulihkan Data (Restore JSON)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Text(
                                    "Pulihkan data dari teks atau file JSON cadangan sebelumnya. Semua data akan diperbarui sesuai isi cadangan.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = { showImportDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                                ) {
                                    Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Impor / Tempel JSON")
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError)
                                    Text("Reset ke Data Awal Pabrik", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = StatusError)
                                }
                                Text(
                                    "Kembalikan data ke bibit awal default (V 2K, V 5K, Member 50K dengan stok 100, Owner PIN 1234, Kasir PIN 0000). Semua transaksi akan direset.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )

                                Button(
                                    onClick = { showResetDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                                ) {
                                    Text("Reset Data")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add or Edit User Dialog
    if (showAddUserDialog) {
        AddEditUserDialog(
            initialUser = userToEdit,
            onDismiss = { showAddUserDialog = false },
            onSave = { user ->
                viewModel.saveUser(user)
                showAddUserDialog = false
            }
        )
    }

    // Import JSON Dialog
    if (showImportDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Tempel Teks JSON Backup", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tempel teks JSON cadangan CyberPos Anda di bawah:", fontSize = 12.sp)
                    OutlinedTextField(
                        value = jsonInput,
                        onValueChange = { jsonInput = it },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        placeholder = { Text("{ \"appName\": \"CyberPos\", ... }") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = viewModel.importBackupJson(jsonInput)
                        if (success) showImportDialog = false
                    }
                ) {
                    Text("Pulihkan Sekarang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        var ownerPinConfirm by remember { mutableStateOf("") }
        var errorPin by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Konfirmasi Reset Data", fontWeight = FontWeight.Bold, color = StatusError) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tindakan ini akan menghapus transaksi dan mengembalikan produk awal ke V 2K, V 5K, dan Member 50K.")
                    OutlinedTextField(
                        value = ownerPinConfirm,
                        onValueChange = {
                            ownerPinConfirm = it
                            errorPin = null
                        },
                        label = { Text("Masukkan PIN Owner (1234)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = errorPin != null,
                        supportingText = { if (errorPin != null) Text(errorPin!!, color = MaterialTheme.colorScheme.error) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (viewModel.verifyOwnerPin(ownerPinConfirm)) {
                            viewModel.resetToDefaults()
                            showResetDialog = false
                        } else {
                            errorPin = "PIN Owner salah!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Ya, Reset Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditUserDialog(
    initialUser: User?,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit
) {
    var name by remember { mutableStateOf(initialUser?.name ?: "") }
    var role by remember { mutableStateOf(initialUser?.role ?: Role.KASIR) }
    var pin by remember { mutableStateOf(initialUser?.pin ?: "0000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialUser != null) "Edit Pengguna" else "Tambah Pengguna Baru", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Pengguna") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Peran Akses:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = role == Role.OWNER,
                        onClick = { role = Role.OWNER },
                        label = { Text("Owner (Semua Akses)") }
                    )
                    FilterChip(
                        selected = role == Role.KASIR,
                        onClick = { role = Role.KASIR },
                        label = { Text("Kasir (Terbatas)") }
                    )
                }

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("PIN Keamanan (4-6 angka)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && pin.length >= 4) {
                        onSave(
                            User(
                                id = initialUser?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                role = role,
                                pin = pin
                            )
                        )
                    }
                }
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
