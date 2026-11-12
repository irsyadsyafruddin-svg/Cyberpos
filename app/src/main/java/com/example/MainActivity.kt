package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Role
import com.example.data.model.Screen
import com.example.ui.CyberPosViewModel
import com.example.ui.components.CyberPosBottomBar
import com.example.ui.components.CyberPosTopBar
import com.example.ui.components.SwitchUserDialog
import com.example.ui.screens.*
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CyberPosApp()
            }
        }
    }
}

@Composable
fun CyberPosApp(
    viewModel: CyberPosViewModel = viewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val users by viewModel.users.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeReceiptTransaction by viewModel.activeReceiptTransaction.collectAsState()
    val receiptSettings by viewModel.receiptSettings.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    var showSwitchUserDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    // Handle back button: if not on Kasir, go back to Kasir
    BackHandler(enabled = currentScreen != Screen.KASIR) {
        viewModel.setScreen(Screen.KASIR)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 840.dp

        if (isWideScreen) {
            // Tablet Wide Navigation Rail Layout
            Row(modifier = Modifier.fillMaxSize()) {
                CyberPosNavigationRail(
                    currentScreen = currentScreen,
                    userRole = currentUser.role,
                    onScreenSelected = { viewModel.setScreen(it) }
                )

                Scaffold(
                    topBar = {
                        CyberPosTopBar(
                            currentUser = currentUser,
                            currentScreen = currentScreen,
                            onSwitchUserClick = { showSwitchUserDialog = true }
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    contentWindowInsets = WindowInsets.safeDrawing,
                    modifier = Modifier.weight(1f)
                ) { innerPadding ->
                    MainScreenContent(
                        screen = currentScreen,
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        } else {
            // Standard Mobile Scaffold with Bottom Navigation
            Scaffold(
                topBar = {
                    CyberPosTopBar(
                        currentUser = currentUser,
                        currentScreen = currentScreen,
                        onSwitchUserClick = { showSwitchUserDialog = true }
                    )
                },
                bottomBar = {
                    CyberPosBottomBar(
                        currentScreen = currentScreen,
                        userRole = currentUser.role,
                        onScreenSelected = { viewModel.setScreen(it) }
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.safeDrawing,
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                MainScreenContent(
                    screen = currentScreen,
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        // Switch User & PIN Modal
        if (showSwitchUserDialog) {
            SwitchUserDialog(
                users = users,
                currentUser = currentUser,
                onDismiss = { showSwitchUserDialog = false },
                onUserSwitched = { user, pin ->
                    val success = viewModel.switchUser(user, pin)
                    if (success) showSwitchUserDialog = false
                }
            )
        }

        // Active Receipt Thermal Dialog
        if (activeReceiptTransaction != null) {
            StrukDialog(
                transaction = activeReceiptTransaction!!,
                settings = receiptSettings,
                onDismiss = { viewModel.closeReceipt() }
            )
        }
    }
}

@Composable
fun MainScreenContent(
    screen: Screen,
    viewModel: CyberPosViewModel,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (screen) {
            Screen.KASIR -> KasirScreen(viewModel = viewModel)
            Screen.KATALOG -> KatalogScreen(viewModel = viewModel)
            Screen.KULAKAN, Screen.HUTANG -> KulakanScreen(viewModel = viewModel)
            Screen.PIUTANG -> PiutangScreen(viewModel = viewModel)
            Screen.PENGELUARAN -> PengeluaranScreen(viewModel = viewModel)
            Screen.LAPORAN -> LaporanScreen(viewModel = viewModel)
            Screen.PENGATURAN -> PengaturanScreen(viewModel = viewModel)
        }
    }
}

data class RailNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

@Composable
fun CyberPosNavigationRail(
    currentScreen: Screen,
    userRole: Role,
    onScreenSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember(userRole) {
        if (userRole == Role.KASIR) {
            listOf(
                RailNavItem(Screen.KASIR, Icons.Default.PointOfSale, "Kasir"),
                RailNavItem(Screen.PIUTANG, Icons.Default.ReceiptLong, "Piutang"),
                RailNavItem(Screen.PENGELUARAN, Icons.Default.AccountBalanceWallet, "Pengeluaran")
            )
        } else {
            listOf(
                RailNavItem(Screen.KASIR, Icons.Default.PointOfSale, "Kasir"),
                RailNavItem(Screen.KATALOG, Icons.Default.Inventory2, "Katalog"),
                RailNavItem(Screen.KULAKAN, Icons.Default.LocalShipping, "Kulakan"),
                RailNavItem(Screen.PIUTANG, Icons.Default.ReceiptLong, "Piutang"),
                RailNavItem(Screen.PENGELUARAN, Icons.Default.AccountBalanceWallet, "Arus Kas"),
                RailNavItem(Screen.LAPORAN, Icons.Default.BarChart, "Laporan"),
                RailNavItem(Screen.PENGATURAN, Icons.Default.Settings, "Pengaturan")
            )
        }
    }

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        items.forEach { item ->
            val selected = currentScreen == item.screen
            NavigationRailItem(
                selected = selected,
                onClick = { onScreenSelected(item.screen) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = CyberPrimary,
                    selectedTextColor = CyberPrimary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.testTag("rail_item_${item.screen.name.lowercase()}")
            )
        }
    }
}
