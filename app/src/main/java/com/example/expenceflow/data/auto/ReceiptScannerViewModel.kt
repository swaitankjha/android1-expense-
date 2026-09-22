package com.example.expenceflow.data.auto

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.expenceflow.data.dao.PendingTransactionDao
import com.example.expenceflow.data.db.PendingTransaction

@HiltViewModel
class ReceiptScannerViewModel @Inject constructor(
    private val repository: ReceiptScannerRepository,
    private val pendingTransactionDao: PendingTransactionDao
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun scanReceipt(bitmap: Bitmap) {
        viewModelScope.launch {
            _isScanning.value = true
            _error.value = null
            try {
                val candidate = repository.scanAndProcess(bitmap)
                if (candidate != null) {
                    val pending = PendingTransaction(
                        amount = candidate.amount,
                        merchant = candidate.merchant,
                        category = candidate.category,
                        date = candidate.date,
                        type = candidate.type,
                        source = "OCR"
                    )
                    pendingTransactionDao.insertPendingTransaction(pending)
                } else {
                    _error.value = "Could not extract data from receipt. Try a clearer photo."
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
            }
        }
    }
}
