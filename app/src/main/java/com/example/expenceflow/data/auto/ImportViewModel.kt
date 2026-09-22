package com.example.expenceflow.data.auto

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expenceflow.data.dao.PendingTransactionDao
import com.example.expenceflow.data.db.PendingTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val repository: ImportRepository,
    private val pendingTransactionDao: PendingTransactionDao,
    private val smsInboxScanner: SmsInboxScanner
) : ViewModel() {
    private val TAG = "ImportViewModel"

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    private val _importSummary = MutableStateFlow<String?>(null)
    val importSummary = _importSummary.asStateFlow()

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

    fun importFile(context: Context, uri: Uri, type: String) {
        viewModelScope.launch {
            Log.d(TAG, "Starting import: $uri (type: $type)")
            _isImporting.value = true
            _importSummary.value = null
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Log.e(TAG, "Could not open input stream for URI: $uri")
                    _importSummary.value = "Failed to open file."
                    return@launch
                }

                val candidates = when (type) {
                    "CSV" -> repository.importCsv(inputStream)
                    "EXCEL" -> repository.importExcel(inputStream)
                    else -> repository.importPdf(inputStream)
                }

                Log.d(TAG, "Imported ${candidates.size} candidates")

                if (candidates.isEmpty()) {
                    _importSummary.value = "No transactions found in this file. Please check the format."
                } else {
                    var successCount = 0
                    candidates.forEach { candidate ->
                        val pending = PendingTransaction(
                            amount = candidate.amount,
                            merchant = candidate.merchant,
                            category = candidate.category,
                            date = candidate.date,
                            type = candidate.type,
                            source = candidate.source
                        )
                        val id = pendingTransactionDao.insertPendingTransaction(pending)
                        if (id > 0) successCount++
                    }
                    _importSummary.value = "Successfully imported $successCount transactions for review."
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
