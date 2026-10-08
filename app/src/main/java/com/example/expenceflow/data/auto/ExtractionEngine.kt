package com.example.expenceflow.data.auto

import android.util.Log
import com.example.expenceflow.data.dao.PendingTransactionDao
import com.example.expenceflow.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtractionEngine @Inject constructor(
    private val categorizationEngine: CategorizationEngine,
    private val duplicateDetectionEngine: DuplicateDetectionEngine,
    private val repository: TransactionRepository,
    private val pendingTransactionDao: PendingTransactionDao
) {
    private val TAG = "ExtractionEngine"

    suspend fun processCandidate(candidate: TransactionCandidate): TransactionCandidate? {
        Log.d(TAG, "Processing candidate from ${candidate.source}: ${candidate.merchant} ₹${candidate.amount}")

        val finalCategory = if (candidate.category == "Other" || candidate.category.isEmpty()) {
            categorizationEngine.categorize(candidate.merchant)
        } else {
            candidate.category
        }

        val enrichedCandidate = candidate.copy(category = finalCategory)

        if (enrichedCandidate.source == "SMS" || enrichedCandidate.source == "Notification") {
            val existing = repository.getAllTransactions().first()
            if (duplicateDetectionEngine.isDuplicate(enrichedCandidate, existing)) {
                Log.d(TAG, "Duplicate detected in main history for: ${enrichedCandidate.merchant} ₹${enrichedCandidate.amount}")
                return null
            }

            val pendingList = pendingTransactionDao.getPendingTransactions().first()
            val matchingPending = duplicateDetectionEngine.findMatchingPending(
                amount = enrichedCandidate.amount,
                date = enrichedCandidate.date,
                type = enrichedCandidate.type,
                merchant = enrichedCandidate.merchant,
                pendingTransactions = pendingList
            )
            if (matchingPending != null) {
                Log.d(TAG, "Duplicate detected in pending list for: ${enrichedCandidate.merchant} ₹${enrichedCandidate.amount}")
                return null
            }
        }

        Log.d(TAG, "Candidate enriched and ready: $enrichedCandidate")
        return enrichedCandidate
    }
}
