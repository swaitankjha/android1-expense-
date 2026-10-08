package com.example.expenceflow.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class ExpensePeriodFilter {
    object CurrentPeriod : ExpensePeriodFilter()
    object ThisMonth : ExpensePeriodFilter()
    object AllTime : ExpensePeriodFilter()
    data class SpecificMonth(val year: Int, val month: Int) : ExpensePeriodFilter() // month: 1-12
    data class CustomRange(val startDate: Long, val endDate: Long) : ExpensePeriodFilter()

    fun getLabel(): String {
        return when (this) {
            is CurrentPeriod -> "Current Cycle"
            is ThisMonth -> "This Month"
            is AllTime -> "All Time"
            is SpecificMonth -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month - 1)
                }
                SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(cal.time)
            }
            is CustomRange -> {
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                "${sdf.format(Date(startDate))} - ${sdf.format(Date(endDate))}"
            }
        }
    }
}
