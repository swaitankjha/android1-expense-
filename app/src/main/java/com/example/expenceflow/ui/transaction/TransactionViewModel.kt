package com.example.expenceflow.ui.transaction

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expenceflow.data.auto.DuplicateDetectionEngine
import com.example.expenceflow.data.auto.ExtractionEngine
import com.example.expenceflow.data.auto.SmsInboxScanner
import com.example.expenceflow.data.dao.PendingTransactionDao
import com.example.expenceflow.data.db.Account
import com.example.expenceflow.data.db.PendingTransaction
import com.example.expenceflow.data.db.Transaction
import com.example.expenceflow.data.repository.AccountRepository
import com.example.expenceflow.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val pendingTransactionDao: PendingTransactionDao,
    private val duplicateDetectionEngine: DuplicateDetectionEngine,
    private val smsInboxScanner: SmsInboxScanner
) : ViewModel() {

    private val _isScanningSms = MutableStateFlow(false)
    val isScanningSms = _isScanningSms.asStateFlow()

    fun scanSmsInbox(context: Context, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            _isScanningSms.value = true
            val count = smsInboxScanner.scanInbox(context)
            _isScanningSms.value = false
            onComplete(count)
        }
    }

    fun findMatchingPending(amount: Double, type: String): PendingTransaction? {
        val currentPending = pendingTransactions.value
        return duplicateDetectionEngine.findMatchingPending(
            amount = amount,
            date = System.currentTimeMillis(),
            type = type,
            pendingTransactions = currentPending
        )
    }

    val pendingTransactions: StateFlow<List<PendingTransaction>> =
        pendingTransactionDao.getPendingTransactions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<Account>> =
        accountRepository.getAllAccounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentAccountId = MutableStateFlow<Long?>(null) // null means All Accounts
    val currentAccountId = _currentAccountId.asStateFlow()

    val allTransactions: StateFlow<List<Transaction>> =
        _currentAccountId.flatMapLatest { id ->
            if (id == null) transactionRepository.getAllTransactions()
            else transactionRepository.getTransactionsByAccount(id)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    private val _selectedAccount = MutableStateFlow("All")
    val selectedAccount = _selectedAccount.asStateFlow()

    fun selectAccountId(id: Long?) {
        _currentAccountId.value = id
    }

    fun addAccount(name: String, moveExisting: Boolean = false) {
        viewModelScope.launch {
            val newId = accountRepository.insertAccount(Account(name = name))
            if (moveExisting) {
                transactionRepository.moveTransactionsToAccount(1L, newId)
            }
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            accountRepository.updateAccount(account)
        }
    }

    fun deleteAccount(account: Account, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val count = transactionRepository.getTransactionCountForAccount(account.id)
            if (count > 0) {
                onError("Cannot delete account with existing transactions. Move or delete them first.")
            } else if (account.id == 1L) {
                onError("Cannot delete the default Personal account.")
            } else {
                accountRepository.deleteAccount(account)
                onSuccess()
            }
        }
    }

    fun dismissPendingTransaction(id: Long) {
        viewModelScope.launch {
            pendingTransactionDao.updateStatus(id, "DISMISSED")
        }
    }

    fun confirmPendingTransaction(pending: PendingTransaction, title: String, category: String, accountId: Long) {
        viewModelScope.launch {
            val account = accountRepository.getAllAccounts().first().find { it.id == accountId }?.name ?: "Personal"
            val transaction = Transaction(
                title = title,
                amount = pending.amount,
                date = pending.date,
                type = pending.type,
                category = category,
                account = account,
                accountId = accountId
            )
            transactionRepository.insertTransaction(transaction)
            pendingTransactionDao.updateStatus(pending.id, "ADDED")
        }
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: String,
        category: String,
        date: Long,
        account: String,
        accountId: Long = 1,
        context: Context
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                title = title,
                amount = amount,
                date = date,
                type = type,
                category = category,
                account = account,
                accountId = accountId
            )
            transactionRepository.insertTransaction(transaction)

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
