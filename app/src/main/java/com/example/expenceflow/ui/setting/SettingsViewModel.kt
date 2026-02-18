package com.example.expenceflow.ui.setting

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import com.example.expenceflow.ui.notification.DailyMissingEntryWorker

class SettingsViewModel : ViewModel() {

    // 🌙 THEME
    private val _isDarkModeEnabled = MutableStateFlow(false)
    val isDarkModeEnabled: StateFlow<Boolean> = _isDarkModeEnabled

    // 🔔 NOTIFICATIONS
    private val _isNotificationEnabled = MutableStateFlow(false)
    val isNotificationEnabled: StateFlow<Boolean> = _isNotificationEnabled

    /* ---------------- THEME ---------------- */

    fun loadTheme(context: Context) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        _isDarkModeEnabled.value = prefs.getBoolean("dark_mode", false)
        _isNotificationEnabled.value = prefs.getBoolean("notifications", false)
    }

    fun toggleDarkMode(context: Context) {
        viewModelScope.launch {
            val newValue = !_isDarkModeEnabled.value
            _isDarkModeEnabled.value = newValue

            context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("dark_mode", newValue)
                .apply()
        }
    }

    /* ---------------- NOTIFICATIONS ---------------- */

    fun toggleNotifications(context: Context) {
        viewModelScope.launch {
            val newValue = !_isNotificationEnabled.value
            _isNotificationEnabled.value = newValue

            context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("notifications", newValue)
                .apply()

            if (newValue) {
                scheduleDailyMissingEntry(context)
            } else {
                cancelAllReminders(context)
            }
        }
    }

    /* ---------------- WORK MANAGER ---------------- */

    private fun scheduleDailyMissingEntry(context: Context) {

        val workRequest =
            PeriodicWorkRequestBuilder<DailyMissingEntryWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(
                    calculateInitialDelay(),
                    TimeUnit.MILLISECONDS
                )
                .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "daily_missing_entry",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
    }

    private fun cancelAllReminders(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("daily_missing_entry")
    }

    /* ---------------- TIME (9 PM) ---------------- */

    private fun calculateInitialDelay(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21) // 9 PM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }
}
