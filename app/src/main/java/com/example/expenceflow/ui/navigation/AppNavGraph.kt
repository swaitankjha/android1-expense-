package com.example.expenceflow.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.expenceflow.ui.alltransactions.AllTransactionsScreen
import com.example.expenceflow.ui.home.DashboardScreen
import com.example.expenceflow.ui.transaction.TransactionViewModel
import com.example.expenceflow.ui.setting.SettingScreen
@Composable
fun AppNavGraph(viewModel: TransactionViewModel) {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD
    ) {

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                viewModel = viewModel,
                onViewAllClick = {
                    navController.navigate(Routes.ALL_TRANSACTIONS)
                },
                onExportExcel = {
                    // TODO: Excel export logic
                }
            )
        }

        composable(Routes.ALL_TRANSACTIONS) {
            AllTransactionsScreen(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

    }
}
