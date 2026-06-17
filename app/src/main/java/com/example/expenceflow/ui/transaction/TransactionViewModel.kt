package com.example.expenceflow.ui.transaction

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expenceflow.data.db.Transaction
import com.example.expenceflow.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    /* -------------------------------------------------- */
    /* ---------------- SINGLE SOURCE ------------------- */
    /* -------------------------------------------------- */

    val allTransactions: StateFlow<List<Transaction>> =
        transactionRepository.getAllTransactions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /* -------------------------------------------------- */
    /* ---------------- ADD ----------------------------- */
    /* -------------------------------------------------- */

    /**
     * context REQUIRED:
     * - daily reminder tracking
     * - weekly summary tracking
     */
    fun addTransaction(
        title: String,
        amount: Double,
        type: String,
        category: String,
        date: Long,
        context: Context
    ) {
        viewModelScope.launch {

            // 1️⃣ SAVE TO DATABASE
            val transaction = Transaction(
                title = title,
                amount = amount,
                date = date,
                type = type,
                category = category
            )
            transactionRepository.insertTransaction(transaction)

            // 2️⃣ MARK TODAY AS DONE (for daily reminder)
            val dailyPrefs =
                context.getSharedPreferences("daily", Context.MODE_PRIVATE)

            dailyPrefs.edit()
                .putLong("last_entry_date", System.currentTimeMillis())
                .apply()

            // 3️⃣ UPDATE WEEKLY TOTAL (for weekly summary)
            val weeklyPrefs =
                context.getSharedPreferences("weekly", Context.MODE_PRIVATE)

            val calendar = Calendar.getInstance()
            val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)

            val savedWeek = weeklyPrefs.getInt("week", -1)
            val savedAmount = weeklyPrefs.getFloat("amount", 0f)

            val newTotal =
                if (savedWeek == currentWeek)
                    savedAmount + amount.toFloat()
                else
                    amount.toFloat()

            weeklyPrefs.edit()
                .putInt("week", currentWeek)
                .putFloat("amount", newTotal)
                .apply()
        }
    }

    /* -------------------------------------------------- */
    /* ---------------- DELETE -------------------------- */
    /* -------------------------------------------------- */

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }

    /* -------------------------------------------------- */
    /* ---------------- UPDATE -------------------------- */
    /* -------------------------------------------------- */

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(transaction)
        }
    }
}
