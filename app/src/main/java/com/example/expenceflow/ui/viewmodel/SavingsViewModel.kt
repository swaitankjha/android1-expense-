package com.example.expenceflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expenceflow.data.dao.BudgetDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import com.example.expenceflow.data.dao.TransactionDao
@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao
) : ViewModel() {

    // Pair of (Total Expenses, Budget Goal)
    private val _progressData = MutableStateFlow<Pair<Int, Int>?>(null)
    val progressData: StateFlow<Pair<Int, Int>?> = _progressData

    init {
        loadProgress()
    }

    fun loadProgress() {
        viewModelScope.launch {
            // Get current month string, e.g., "2025-06"
            val month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

            val budgetGoal = budgetDao.getGoalForMonth(month)?.amount ?: 0

            // getMonthlyTotal returns Double?, so we cast to Int for the Pair
            val expensesDouble = transactionDao.getMonthlyTotal("Expense", month) ?: 0.0
            val expenses = expensesDouble.toInt()

            _progressData.value = Pair(expenses, budgetGoal)
        }
    }
}
