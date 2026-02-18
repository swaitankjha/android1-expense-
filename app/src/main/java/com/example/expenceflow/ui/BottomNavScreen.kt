package com.example.expenceflow.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavScreen(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavScreen("home", "Home", Icons.Default.Home)
    object Graph : BottomNavScreen("graph", "Graph", Icons.Default.ShowChart)
    object Transaction : BottomNavScreen("transaction", "Transaction", Icons.Default.List)
    object Setting : BottomNavScreen("setting", "Setting", Icons.Default.Settings)
}
