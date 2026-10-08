package com.example.expenceflow.data.model

import com.example.expenceflow.data.db.Transaction
import java.util.Calendar

object ExpensePeriodFilterUtil {

    fun filterTransactions(
        transactions: List<Transaction>,
        filter: ExpensePeriodFilter,
        activeStart: Long? = null,
        activeEnd: Long? = null
    ): List<Transaction> {
        return when (filter) {
            is ExpensePeriodFilter.AllTime -> transactions
            is ExpensePeriodFilter.CurrentPeriod -> {
                if (activeStart != null && activeEnd != null) {
                    val startOfDay = getStartOfDay(minOf(activeStart, activeEnd))
                    val endOfDay = getEndOfDay(maxOf(activeStart, activeEnd))
                    transactions.filter { it.date in startOfDay..endOfDay }
                } else {
                    filterThisMonth(transactions)
                }
            }
            is ExpensePeriodFilter.ThisMonth -> {
                filterThisMonth(transactions)
            }
            is ExpensePeriodFilter.SpecificMonth -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, filter.year)
                    set(Calendar.MONTH, filter.month - 1)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val end = cal.timeInMillis
                transactions.filter { it.date in start..end }
            }
            is ExpensePeriodFilter.CustomRange -> {
                val startOfDay = getStartOfDay(minOf(filter.startDate, filter.endDate))
                val endOfDay = getEndOfDay(maxOf(filter.startDate, filter.endDate))
                transactions.filter { it.date in startOfDay..endOfDay }
            }
        }
    }

    private fun filterThisMonth(transactions: List<Transaction>): List<Transaction> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val start = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val end = cal.timeInMillis
        return transactions.filter { it.date in start..end }
    }

    fun getStartOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getEndOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }
}
