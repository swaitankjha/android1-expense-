package com.example.expenceflow.data.auto

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expenceflow.data.db.Account
import com.example.expenceflow.data.db.Transaction
import com.example.expenceflow.data.repository.AccountRepository
import com.example.expenceflow.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val repository: ImportRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val smsInboxScanner: SmsInboxScanner
) : ViewModel() {
    private val TAG = "ImportViewModel"

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    private val _importSummary = MutableStateFlow<String?>(null)
    val importSummary = _importSummary.asStateFlow()

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun scanSmsInbox(context: Context) {
        viewModelScope.launch {
            Log.d(TAG, "Starting SMS Inbox scan")
            _isImporting.value = true
            _importSummary.value = null
            try {
                val addedCount = smsInboxScanner.scanInbox(context)
                if (addedCount > 0) {
                    _importSummary.value = "Successfully detected $addedCount new transaction(s) from your SMS Inbox!"
                } else {
                    _importSummary.value = "No new transaction SMS messages found in your inbox."
                }
            } catch (e: Exception) {
                Log.e(TAG, "SMS scan error: ${e.message}", e)
                _importSummary.value = "SMS scan failed: ${e.localizedMessage}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun createAccountAndGet(name: String, onCreated: (Account) -> Unit) {
        viewModelScope.launch {
            val accountName = name.trim().ifBlank { "Custom Account" }
            val newAccount = Account(name = accountName)
            val id = accountRepository.insertAccount(newAccount)
            onCreated(newAccount.copy(id = id))
        }
    }

    fun importFileToAccount(context: Context, uri: Uri, type: String, account: Account) {
        viewModelScope.launch {
            Log.d(TAG, "Importing file $uri of type $type directly to account ${account.name}")
            _isImporting.value = true
            _importSummary.value = null
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _importSummary.value = "Failed to open file."
                    return@launch
                }

                val candidates = when (type) {
                    "CSV" -> repository.importCsv(inputStream)
                    "EXCEL" -> repository.importExcel(inputStream)
                    else -> repository.importPdf(inputStream)
                }

                Log.d(TAG, "Extracted ${candidates.size} transactions from file")

                if (candidates.isEmpty()) {
                    _importSummary.value = "No transactions found in this file. Please check the file format."
                } else {
                    var successCount = 0
                    candidates.forEach { candidate ->
                        val transaction = Transaction(
                            title = candidate.merchant,
                            amount = candidate.amount,
                            date = candidate.date,
                            type = candidate.type,
                            category = candidate.category,
                            account = account.name,
                            accountId = account.id
                        )
                        transactionRepository.insertTransaction(transaction)
                        successCount++
                    }
                    _importSummary.value = "Successfully added $successCount transactions directly to '${account.name}'!"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Import error: ${e.message}", e)
                _importSummary.value = "Import failed: ${e.localizedMessage}"
            } finally {
                _isImporting.value = false
            }
        }
    }
}
