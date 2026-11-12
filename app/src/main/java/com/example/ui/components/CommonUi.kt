package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Role
import com.example.data.model.Screen
import com.example.data.model.User
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberPosTopBar(
    currentUser: User,
    currentScreen: Screen,
    onSwitchUserClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PointOfSale,
                        contentDescription = "CyberPos Logo",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CyberPos",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(CyberAccent.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "POS",
                                color = CyberPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = currentScreen.title,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            // User Switcher Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (currentUser.role == Role.OWNER) CyberNavyCard else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .testTag("user_switcher_button")
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSwitchUserClick() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                if (currentUser.role == Role.OWNER) StatusWarning else CyberPrimary,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (currentUser.role == Role.OWNER) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                            contentDescription = "Role",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Column {
                        Text(
                            text = currentUser.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentUser.role == Role.OWNER) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = currentUser.role.label,
                            fontSize = 9.sp,
                            color = if (currentUser.role == Role.OWNER) StatusWarning else CyberPrimary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Ganti Pengguna",
                        tint = if (currentUser.role == Role.OWNER) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    )
}

@Composable
fun SwitchUserDialog(
    users: List<User>,
    currentUser: User,
    onDismiss: () -> Unit,
    onUserSwitched: (User, String) -> Unit
) {
    var selectedUser by remember { mutableStateOf(currentUser) }
    var enteredPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = CyberPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ganti Pengguna & Masukkan PIN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Pilih akun pengguna yang ingin digunakan:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // User selection list
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    users.forEach { user ->
                        val isSelected = user.id == selectedUser.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, CyberPrimary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedUser = user
                                    enteredPin = ""
                                    errorText = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                if (user.role == Role.OWNER) StatusWarning else CyberPrimary,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (user.role == Role.OWNER) Icons.Default.Shield else Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = user.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Peran: ${user.role.label} ${if (user.role == Role.OWNER) "(Akses Penuh)" else "(Kasir, Piutang, Pengeluaran)"}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CyberPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // PIN Input
                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            enteredPin = it
                            errorText = null
                        }
                    },
                    label = { Text("PIN Keamanan (Default: Owner=1234, Kasir=0000)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_input"),
                    isError = errorText != null,
                    supportingText = {
                        if (errorText != null) {
                            Text(text = errorText ?: "", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(text = "Masukkan PIN 4 angka akun ${selectedUser.name}")
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (enteredPin.isBlank()) {
                        errorText = "PIN tidak boleh kosong!"
                    } else if (enteredPin != selectedUser.pin) {
                        errorText = "PIN salah! Periksa kembali PIN Anda."
                    } else {
                        onUserSwitched(selectedUser, enteredPin)
                    }
                },
                modifier = Modifier.testTag("submit_pin_button")
            ) {
                Text("Masuk / Beralih")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

data class NavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

@Composable
fun CyberPosBottomBar(
    currentScreen: Screen,
    userRole: Role,
    onScreenSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember(userRole) {
        if (userRole == Role.KASIR) {
            listOf(
                NavItem(Screen.KASIR, Icons.Default.PointOfSale, "Kasir"),
                NavItem(Screen.PIUTANG, Icons.Default.ReceiptLong, "Piutang"),
                NavItem(Screen.PENGELUARAN, Icons.Default.AccountBalanceWallet, "Pengeluaran")
            )
        } else {
            listOf(
                NavItem(Screen.KASIR, Icons.Default.PointOfSale, "Kasir"),
                NavItem(Screen.KATALOG, Icons.Default.Inventory2, "Katalog"),
                NavItem(Screen.KULAKAN, Icons.Default.LocalShipping, "Kulakan"),
                NavItem(Screen.PIUTANG, Icons.Default.ReceiptLong, "Piutang"),
                NavItem(Screen.PENGELUARAN, Icons.Default.AccountBalanceWallet, "Arus Kas"),
                NavItem(Screen.LAPORAN, Icons.Default.BarChart, "Laporan"),
                NavItem(Screen.PENGATURAN, Icons.Default.Settings, "Pengaturan")
            )
        }
    }

    NavigationBar(
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        items.forEach { item ->
            val selected = currentScreen == item.screen
            NavigationBarItem(
                selected = selected,
                onClick = { onScreenSelected(item.screen) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyberPrimary,
                    selectedTextColor = CyberPrimary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.testTag("nav_item_${item.screen.name.lowercase()}")
            )
        }
    }
}
