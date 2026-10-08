package com.example.expenceflow.data.auto

import com.example.expenceflow.data.db.PendingTransaction
import com.example.expenceflow.data.db.Transaction
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class DuplicateDetectionEngine @Inject constructor() {

    private val TIME_WINDOW_MS = 5 * 60 * 1000 // 5 minutes
    private val AMOUNT_THRESHOLD = 0.01

    fun isDuplicate(candidate: TransactionCandidate, existingTransactions: List<Transaction>): Boolean {
        return existingTransactions.any { existing ->
            isMatch(
                candidateAmount = candidate.amount,
                candidateDate = candidate.date,
                candidateType = candidate.type,
                candidateMerchant = candidate.merchant,
                existing = existing
            )
        }
    }

    fun findMatchingPending(
        amount: Double,
        date: Long,
        type: String,
        merchant: String,
        pendingTransactions: List<PendingTransaction>
    ): PendingTransaction? {
        return pendingTransactions.find { pending ->
            val timeDiff = abs(date - pending.date)
            val amountDiff = abs(amount - pending.amount)

            val nameMatch = merchant.isBlank() ||
                            pending.merchant.isBlank() ||
                            merchant.equals("Merchant", true) ||
                            pending.merchant.equals("Merchant", true) ||
                            merchant.contains(pending.merchant, true) ||
                            pending.merchant.contains(merchant, true)

            timeDiff <= TIME_WINDOW_MS &&
            amountDiff <= AMOUNT_THRESHOLD &&
            type.equals(pending.type, ignoreCase = true) &&
            nameMatch
        }
    }

    private fun isMatch(
        candidateAmount: Double,
        candidateDate: Long,
        candidateType: String,
        candidateMerchant: String,
        existing: Transaction
    ): Boolean {
        val timeDiff = abs(candidateDate - existing.date)
        val amountDiff = abs(candidateAmount - existing.amount)

        val nameMatch = candidateMerchant.contains("Unknown", true) ||
                        candidateMerchant.contains("Merchant", true) ||
                        existing.title.contains("Unknown", true) ||
                        existing.title.contains("Merchant", true) ||
                        existing.title.contains(candidateMerchant, ignoreCase = true) ||
                        candidateMerchant.contains(existing.title, ignoreCase = true)

        return timeDiff <= TIME_WINDOW_MS &&
               amountDiff <= AMOUNT_THRESHOLD &&
               candidateType.equals(existing.type, ignoreCase = true) &&
               nameMatch
    }
}
