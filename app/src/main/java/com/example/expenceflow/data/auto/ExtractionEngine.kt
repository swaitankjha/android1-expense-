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
        // 1. Categorize if needed
        val finalCategory = if (candidate.category == "Other" || candidate.category.isEmpty()) {
            categorizationEngine.categorize(candidate.merchant)
        } else {
            candidate.category
        }

        val enrichedCandidate = candidate.copy(category = finalCategory)

        // 2. Check for duplicates (Only for SMS and Notifications which are truly automatic)
        if (enrichedCandidate.source == "SMS" || enrichedCandidate.source == "Notification") {
            val existing = repository.getAllTransactions().first()
            if (duplicateDetectionEngine.isDuplicate(enrichedCandidate, existing)) {
                Log.d(TAG, "Duplicate detected in main history for: ${enrichedCandidate.merchant} ₹${enrichedCandidate.amount}")
                return null
            }
            
            val pendingList = pendingTransactionDao.getPendingTransactions().first()
            val matchingPending = duplicateDetectionEngine.findMatchingPending(
                enrichedCandidate.amount, enrichedCandidate.date, enrichedCandidate.type, pendingList
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
