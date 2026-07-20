package com.example.expenceflow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.expenceflow.ui.LiveRatesScreen
import com.example.expenceflow.ui.TransactionScreen
import com.example.expenceflow.ui.alltransactions.AllTransactionsScreen
import com.example.expenceflow.ui.graph.GraphScreen
import com.example.expenceflow.ui.home.DashboardScreen
import com.example.expenceflow.ui.setting.*
import com.example.expenceflow.ui.theme.ExpenceFlowTheme
import com.example.expenceflow.ui.transaction.TransactionViewModel
import com.example.expenceflow.ui.BottomNavScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val transactionViewModel: TransactionViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val context = LocalContext.current
            LaunchedEffect(Unit) { settingsViewModel.loadTheme(context) }
            val isDarkModeEnabled by settingsViewModel.isDarkModeEnabled.collectAsStateWithLifecycle()

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

@Composable
fun BottomNavApp(
    transactionViewModel: TransactionViewModel,
    settingsViewModel: SettingsViewModel,
    startScreen: String?
) {
    val navController = rememberNavController()
    val startDestination = if (startScreen == "transaction") BottomNavScreen.Transaction.route else BottomNavScreen.Home.route

    Scaffold(
        bottomBar = { CreativeBottomBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable(BottomNavScreen.Home.route) {
                DashboardScreen(
                    viewModel = transactionViewModel,
                    onViewAllClick = { navController.navigate("all_transactions") },
                    onExportExcel = {}
                )
            }
            composable("all_transactions") {
                AllTransactionsScreen(viewModel = transactionViewModel, onBack = { navController.popBackStack() })
            }
            composable(BottomNavScreen.Graph.route) { GraphScreen(transactionViewModel) }
            composable(BottomNavScreen.Transaction.route) { TransactionScreen(viewModel = transactionViewModel) }
            composable(BottomNavScreen.Setting.route) {
                SettingScreen(settingsViewModel = settingsViewModel, navController = navController)
            }
            composable("settings/improvement") { ImprovementScreen(onBack = { navController.popBackStack() }) }
            composable(BottomNavScreen.LiveRates.route) { LiveRatesScreen() }
            composable("settings/about") { AboutScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

@Composable
fun CreativeBottomBar(navController: NavHostController) {
    val items = listOf(
        BottomNavScreen.Home,
        BottomNavScreen.Graph,
        BottomNavScreen.Transaction,
        BottomNavScreen.LiveRates,
        BottomNavScreen.Setting
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 12.dp,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { screen ->
                val selected = currentRoute == screen.route
                val scale by animateFloatAsState(if (selected) 1.2f else 1f)
                val iconColor by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                            tint = iconColor,
                            modifier = Modifier.scale(scale).size(24.dp)
                        )
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
    }
}
