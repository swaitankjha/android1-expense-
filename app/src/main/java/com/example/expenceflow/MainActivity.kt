package com.example.expenceflow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.expenceflow.ui.TransactionScreen
import com.example.expenceflow.ui.alltransactions.AllTransactionsScreen
import com.example.expenceflow.ui.graph.GraphScreen
import com.example.expenceflow.ui.home.DashboardScreen
import com.example.expenceflow.ui.setting.*
import com.example.expenceflow.ui.theme.ExpenceFlowTheme
import com.example.expenceflow.ui.transaction.TransactionViewModel
import dagger.hilt.android.AndroidEntryPoint

/* ---------------- BOTTOM NAV ---------------- */

sealed class BottomNavScreen(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Home : BottomNavScreen("home", "Home", Icons.Default.Home)
    object Graph : BottomNavScreen("graph", "Graph", Icons.Default.ShowChart)
    object Transaction : BottomNavScreen("transaction", "Transaction", Icons.Default.List)
    object Setting : BottomNavScreen("setting", "Setting", Icons.Default.Settings)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val transactionViewModel: TransactionViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔔 Android 13+ notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val context = LocalContext.current

            // 🔥 Load saved dark mode once
            LaunchedEffect(Unit) {
                settingsViewModel.loadTheme(context)
            }

            val isDarkModeEnabled by settingsViewModel.isDarkModeEnabled.collectAsState()

            ExpenceFlowTheme(darkTheme = isDarkModeEnabled) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BottomNavApp(
                        transactionViewModel = transactionViewModel,
                        settingsViewModel = settingsViewModel,
                        startScreen = intent?.getStringExtra("open_screen")
                    )
                }
            }
        }
    }
}

/* ---------------- NAV HOST ---------------- */

@Composable
fun BottomNavApp(
    transactionViewModel: TransactionViewModel,
    settingsViewModel: SettingsViewModel,
    startScreen: String?
) {
    val navController = rememberNavController()

    // ✅ THIS IS THE MAGIC
    val startDestination =
        if (startScreen == "transaction")
            BottomNavScreen.Transaction.route
        else
            BottomNavScreen.Home.route

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { padding ->

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {

            // 🏠 DASHBOARD
            composable(BottomNavScreen.Home.route) {
                DashboardScreen(
                    viewModel = transactionViewModel,
                    onViewAllClick = {
                        navController.navigate("all_transactions")
                    },
                    onExportExcel = {}
                )
            }

            // 👀 ALL TRANSACTIONS (hidden)
            composable("all_transactions") {
                AllTransactionsScreen(
                    viewModel = transactionViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // 📊 GRAPH
            composable(BottomNavScreen.Graph.route) {
                GraphScreen(transactionViewModel)
            }

            // 🧾 TRANSACTION
            composable(BottomNavScreen.Transaction.route) {
                TransactionScreen(viewModel = transactionViewModel)
            }

            // ⚙️ SETTINGS
            composable(BottomNavScreen.Setting.route) {
                SettingScreen(
                    settingsViewModel = settingsViewModel,
                    navController = navController
                )
            }

            // 🚀 IMPROVEMENT
            composable("settings/improvement") {
                ImprovementScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // ℹ️ ABOUT
            composable("settings/about") {
                AboutScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/* ---------------- BOTTOM BAR ---------------- */

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavScreen.Home,
        BottomNavScreen.Graph,
        BottomNavScreen.Transaction,
        BottomNavScreen.Setting
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
