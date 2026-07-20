package com.example.expenceflow.ui.transaction

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expenceflow.data.db.Transaction
import com.example.expenceflow.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val allTransactions: StateFlow<List<Transaction>> =
        transactionRepository.getAllTransactions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    private val _selectedAccount = MutableStateFlow("All")
    val selectedAccount = _selectedAccount.asStateFlow()

    val filteredTransactions = combine(allTransactions, _selectedAccount) { txs, account ->
        if (account == "All") txs else txs.filter { it.account == account }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectAccount(account: String) {
        _selectedAccount.value = account
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: String,
        category: String,
        date: Long,
        account: String,
        context: Context
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                title = title,
                amount = amount,
                date = date,
                type = type,
                category = category,
                account = account
            )
            transactionRepository.insertTransaction(transaction)

            // Mark today as done for reminders
            val dailyPrefs = context.getSharedPreferences("daily", Context.MODE_PRIVATE)
            dailyPrefs.edit().putLong("last_entry", System.currentTimeMillis()).apply()
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(transaction)
        }
    }
}
