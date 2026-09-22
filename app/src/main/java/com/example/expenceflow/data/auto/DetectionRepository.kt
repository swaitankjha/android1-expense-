package com.example.expenceflow.data.auto

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DetectionRepository @Inject constructor() {
    private val _pendingCandidates = MutableStateFlow<List<TransactionCandidate>>(emptyList())
    val pendingCandidates = _pendingCandidates.asStateFlow()

    fun addCandidate(candidate: TransactionCandidate) {
        _pendingCandidates.value = _pendingCandidates.value + candidate
    }

    fun removeCandidate(candidate: TransactionCandidate) {
        _pendingCandidates.value = _pendingCandidates.value.filter { it != candidate }
    }
}
