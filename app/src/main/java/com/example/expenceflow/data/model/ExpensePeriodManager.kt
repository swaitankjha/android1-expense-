package com.example.expenceflow.data.model

import android.content.Context

object ExpensePeriodManager {
    private const val PREFS_NAME = "expense_period_prefs"
    private const val KEY_START_DATE = "active_start_date"
    private const val KEY_END_DATE = "active_end_date"
    private const val KEY_CYCLE_NAME = "active_cycle_name"

    fun getActivePeriod(context: Context): Pair<Long, Long>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val start = prefs.getLong(KEY_START_DATE, -1L)
        val end = prefs.getLong(KEY_END_DATE, -1L)
        if (start != -1L && end != -1L && end >= start) {
            return Pair(start, end)
        }
        return null
    }

    fun getActiveCycleName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CYCLE_NAME, "Custom Cycle") ?: "Custom Cycle"
    }

    fun setActivePeriod(context: Context, start: Long, end: Long, name: String = "Custom Cycle") {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putLong(KEY_START_DATE, start)
            .putLong(KEY_END_DATE, end)
            .putString(KEY_CYCLE_NAME, name)
            .apply()
    }

    fun clearActivePeriod(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(KEY_START_DATE)
            .remove(KEY_END_DATE)
            .remove(KEY_CYCLE_NAME)
            .apply()
    }
}
