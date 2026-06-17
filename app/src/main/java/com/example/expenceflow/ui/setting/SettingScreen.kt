package com.example.expenceflow.ui.setting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.expenceflow.ui.notification.DailyMissingEntryWorker

@Composable
fun SettingScreen(
    settingsViewModel: SettingsViewModel,
    navController: NavHostController
) {
    val isDarkModeEnabled by settingsViewModel.isDarkModeEnabled.collectAsState()
    val isNotificationEnabled by settingsViewModel.isNotificationEnabled.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Settings",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(28.dp))

        // 🔔 NOTIFICATIONS
        SettingsRow(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            trailing = {
                Switch(
                    checked = isNotificationEnabled,
                    onCheckedChange = {
                        settingsViewModel.toggleNotifications(context)
                    }
                )
            }
        )

        // 🌙 DARK MODE
        SettingsRow(
            icon = Icons.Default.DarkMode,
            title = "Dark Mode",
            trailing = {
                Switch(
                    checked = isDarkModeEnabled,
                    onCheckedChange = {
                        settingsViewModel.toggleDarkMode(context)
                    }
                )
            }
        )

        Spacer(Modifier.height(8.dp))


        SettingsRow(
            icon = Icons.Default.Build,
            title = "Help improve ExpenseFlow",
            onClick = { navController.navigate("settings/improvement") }
        )


        SettingsRow(
            icon = Icons.Default.Info,
            title = "About ExpenseFlow",
            onClick = { navController.navigate("settings/about") }
        )

        Spacer(modifier = Modifier.weight(1f))






        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(26.dp)
                .padding(end = 16.dp)
        )

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )

        trailing?.invoke() ?: Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        )
    }
}
