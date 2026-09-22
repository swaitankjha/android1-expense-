package com.example.expenceflow.data.auto

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceParser @Inject constructor(
    private val categorizationEngine: CategorizationEngine
) {
    private val TAG = "VoiceParser"

    fun parseVoiceCommand(text: String): TransactionCandidate? {
        Log.d(TAG, "Parsing text: $text")
        
        val amountRegex = "([\\d,]+\\.?\\d*)"
        val merchantRegex = "(.*)"

        // 1. Explicit Expense: Spent/Paid/Gave [Amount] on/to [Merchant]
        val expenseRegex1 = Regex("(?i)(?:Spent|Paid|Gave|Bought|Purchased)\\s+$amountRegex\\s+(?:on|for|to|at)\\s+$merchantRegex")
        // 2. Explicit Expense: Spent/Paid [Merchant] [Amount]
        val expenseRegex2 = Regex("(?i)(?:Spent|Paid|Gave|Bought|Purchased)\\s+$merchantRegex\\s+$amountRegex")
        // 3. Explicit Income: Received/Got/Salary [Amount] from [Source]
        val incomeRegex1 = Regex("(?i)(?:Received|Got|Salary|Earned|Credit|Income)\\s+$amountRegex\\s*(?:from|for)?\\s*$merchantRegex")
        // 4. Explicit Income: Received [Source] [Amount]
        val incomeRegex2 = Regex("(?i)(?:Received|Got|Salary|Earned|Credit|Income)\\s+$merchantRegex\\s+$amountRegex")
        
        // 5. Generic fallback with prepositions: "[Amount] for [Merchant]" (Often expense)
        val genericPrepRegex = Regex("(?i)$amountRegex\\s+(?:for|on|to|at)\\s+$merchantRegex")
        // 6. Generic pattern: "[Merchant] [Amount]"
        val genericPatternRegex = Regex("(?i)^\\s*(.*?)\\s+$amountRegex\\s*$")

        val e1 = expenseRegex1.find(text)
        if (e1 != null) {
            return createCandidate(e1.groupValues[1], e1.groupValues[2], "Expense", true)
        }

        val e2 = expenseRegex2.find(text)
        if (e2 != null) {
            return createCandidate(e2.groupValues[2], e2.groupValues[1], "Expense", true)
        }

        val i1 = incomeRegex1.find(text)
        if (i1 != null) {
            return createCandidate(i1.groupValues[1], i1.groupValues[2].ifEmpty { "Income" }, "Income", true)
        }

        val i2 = incomeRegex2.find(text)
        if (i2 != null) {
            return createCandidate(i2.groupValues[2], i2.groupValues[1], "Income", true)
        }

        val gp = genericPrepRegex.find(text)
        if (gp != null) {
            // Prepositions usually imply expense, but let's mark as not confident
            return createCandidate(gp.groupValues[1], gp.groupValues[2], "Expense", false)
        }
        
        val up = genericPatternRegex.find(text)
        if (up != null) {
            val merchant = up.groupValues[1].trim()
            val amount = up.groupValues[2]
            if (merchant.isNotEmpty() && !merchant.all { it.isDigit() || it == '.' || it == ',' }) {
                // If just name and amount, we don't know if it's income or expense
                return createCandidate(amount, merchant, "Expense", false)
            }
        }

        Log.d(TAG, "No match found for: $text")
        return null
    }

    private fun createCandidate(amountStr: String, merchant: String, type: String, isConfident: Boolean): TransactionCandidate {
        val amount = amountStr.replace(",", "").toDoubleOrNull() ?: 0.0
        val cleanMerchant = merchant.trim()
        Log.d(TAG, "Creating candidate: Amt=$amount, Merchant=$cleanMerchant, Type=$type, Confident=$isConfident")
        return TransactionCandidate(
            amount = amount,
            merchant = cleanMerchant,
            category = categorizationEngine.categorize(cleanMerchant),
            date = System.currentTimeMillis(),
            account = "Cash",
            type = type,
            isTypeConfident = isConfident,
            confidence = 0.9f,
            source = "Voice"
        )
    }
}
