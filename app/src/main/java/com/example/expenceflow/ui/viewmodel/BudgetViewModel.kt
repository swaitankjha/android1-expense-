package com.example.expenceflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expenceflow.data.dao.BudgetDao
import com.example.expenceflow.data.db.BudgetGoal // ✅ Corrected Import (was data.dao)
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel // ✅ Required for Hilt Injection
class BudgetViewModel @Inject constructor( // ✅ Required for Hilt Injection
    private val budgetDao: BudgetDao
) : ViewModel() {

    private val _monthlyGoal = MutableStateFlow<BudgetGoal?>(null)
    val monthlyGoal: StateFlow<BudgetGoal?> = _monthlyGoal

    init {
        loadCurrentMonthGoal()
    }

    private fun currentMonthKey(): String =
        YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

    fun loadCurrentMonthGoal() {
        viewModelScope.launch {
            val goal = budgetDao.getGoalForMonth(currentMonthKey())
            _monthlyGoal.value = goal
        }
    }

    fun saveBudgetGoal(amount: Int) {
        viewModelScope.launch {
            val key = currentMonthKey()
            // Ensure we are using the correct Entity constructor
            val goal = BudgetGoal(month = key, amount = amount)
            budgetDao.upsertGoal(goal)
            _monthlyGoal.value = goal
        }
    }
}
