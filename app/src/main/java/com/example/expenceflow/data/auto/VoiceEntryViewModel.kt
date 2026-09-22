package com.example.expenceflow.data.auto

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expenceflow.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceEntryViewModel @Inject constructor(
    private val voiceManager: VoiceRecognitionManager,
    private val voiceParser: VoiceParser,
    private val extractionEngine: ExtractionEngine,
    private val repository: TransactionRepository,
    private val detectionRepository: DetectionRepository
) : ViewModel() {
    private val TAG = "VoiceEntryViewModel"

    val isListening = voiceManager.isListening
    val results = voiceManager.results
    val error = voiceManager.error

    private val _parsedCandidate = MutableStateFlow<TransactionCandidate?>(null)
    val parsedCandidate = _parsedCandidate.asStateFlow()

    init {
        viewModelScope.launch {
            results.collectLatest { text ->
                if (!text.isNullOrBlank()) {
                    Log.d(TAG, "Speech results: $text")
                    val rawCandidate = voiceParser.parseVoiceCommand(text)
                    if (rawCandidate != null) {
                        Log.d(TAG, "Parsed raw candidate: $rawCandidate")
                        _parsedCandidate.value = extractionEngine.processCandidate(rawCandidate)
                        Log.d(TAG, "Processed candidate: ${_parsedCandidate.value}")
                    } else {
                        Log.d(TAG, "VoiceParser failed to parse: $text")
                    }
                }
            }
        }
    }

    fun confirmTransaction() {
        viewModelScope.launch {
            val candidate = _parsedCandidate.value ?: run {
                Log.d(TAG, "confirmTransaction: No candidate to confirm")
                return@launch
            }
            Log.d(TAG, "confirmTransaction: Confirming $candidate")
            val transaction = com.example.expenceflow.data.db.Transaction(
                title = candidate.merchant,
                amount = candidate.amount,
                date = candidate.date,
                type = candidate.type,
                category = candidate.category,
                account = candidate.account
            )
            repository.insertTransaction(transaction)
            Log.d(TAG, "confirmTransaction: Transaction inserted successfully")
            _parsedCandidate.value = null
        }
    }

    fun updateCandidateType(type: String) {
        _parsedCandidate.value = _parsedCandidate.value?.copy(type = type, isTypeConfident = true)
    }

    fun updateCandidateMerchant(merchant: String) {
        _parsedCandidate.value = _parsedCandidate.value?.copy(merchant = merchant)
    }

    fun updateCandidateAmount(amount: Double) {
        _parsedCandidate.value = _parsedCandidate.value?.copy(amount = amount)
    }

    fun startListening() {
        voiceManager.startListening()
    }

    fun stopListening() {
        voiceManager.stopListening()
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
