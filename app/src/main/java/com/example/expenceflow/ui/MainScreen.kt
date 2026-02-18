package com.example.expenceflow.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.expenceflow.ui.alltransactions.AllTransactionsScreen
import com.example.expenceflow.ui.graph.GraphScreen
import com.example.expenceflow.ui.home.DashboardScreen
import com.example.expenceflow.ui.setting.SettingScreen
import com.example.expenceflow.ui.TransactionScreen
import com.example.expenceflow.ui.transaction.TransactionViewModel

@Composable
fun MainScreen() {

    // ✅ single ViewModel instance
    val transactionViewModel: TransactionViewModel = hiltViewModel()
    val settingsViewModel = hiltViewModel<com.example.expenceflow.ui.setting.SettingsViewModel>()

    val screens = listOf(
        BottomNavScreen.Home,
        BottomNavScreen.Graph,
        BottomNavScreen.Transaction,
        BottomNavScreen.Setting
    )

    var selectedScreen by remember { mutableStateOf<BottomNavScreen>(BottomNavScreen.Home) }

    // 🔒 hidden screen flag
    var showAllTransactions by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = selectedScreen == screen,
                        onClick = {
                            selectedScreen = screen
                            showAllTransactions = false // 👈 reset
                        }
                    )
                }
            }
        }
    ) { paddingValues ->

        Box(modifier = Modifier.padding(paddingValues)) {

            // 🔥 HIDDEN SCREEN (View All)
            if (showAllTransactions) {
                AllTransactionsScreen(
                    viewModel = transactionViewModel,
                    onBack = { showAllTransactions = false }
                )
            } else {
                when (selectedScreen) {

                    BottomNavScreen.Home -> {
                        DashboardScreen(
                            viewModel = transactionViewModel,
                            onViewAllClick = {
                                showAllTransactions = true
                            },
                            onExportExcel = {
                                // TODO: Excel export logic
                            }
                        )
                    }

                    BottomNavScreen.Graph -> {
                        GraphScreen(transactionViewModel)
                    }

                    BottomNavScreen.Transaction -> {
                        TransactionScreen(viewModel = transactionViewModel)
                    }



                    BottomNavScreen.Setting -> {
                        SettingScreen(
                            settingsViewModel = settingsViewModel,
                            navController = rememberNavController()
                        )
                    }
                }
            }
        }
    }
}
