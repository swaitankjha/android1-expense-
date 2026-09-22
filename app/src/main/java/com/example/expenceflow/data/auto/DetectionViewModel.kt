package com.example.expenceflow.data.auto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expenceflow.data.repository.TransactionRepository
import com.example.expenceflow.data.db.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetectionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val detectionRepository: DetectionRepository
) : ViewModel() {

    val pendingCandidates = detectionRepository.pendingCandidates

    fun confirmCandidate(candidate: TransactionCandidate) {
        viewModelScope.launch {
            val transaction = Transaction(
                title = candidate.merchant,
                amount = candidate.amount,
                date = candidate.date,
                type = candidate.type,
                category = candidate.category,
                account = candidate.account
            )
            repository.insertTransaction(transaction)
            dismissCandidate(candidate)
        }
    }

    fun dismissCandidate(candidate: TransactionCandidate) {
        detectionRepository.removeCandidate(candidate)
    }
}
