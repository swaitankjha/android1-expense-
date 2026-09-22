package com.example.expenceflow.data.auto

import android.util.Log
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsParser @Inject constructor() {
    private val TAG = "SmsParser"

    private val transactionKeywords = listOf(
        "debited", "credited", "spent", "paid", "sent", "received", "added",
        "txn", "transaction", "transfer", "trf", "withdrawn", "deposited", "refund", "purchase"
    )

    private val incomeKeywords = listOf(
        "credited", "received", "added", "deposited", "refund", "cashback", "salary", "recd"
    )

    private val amountPattern = Pattern.compile(
        "(?i)(?:Rs|INR|\\u20B9|Amt|Amount|USD|\\$)\\.?\\s*([\\d,]+(?:\\.\\d{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    private val fallbackAmountPattern = Pattern.compile(
        "(?i)(?:debited|credited|spent|paid|received|sent|withdrawn)\\s+(?:by|for|with|of)?\\s*(?:Rs|INR|\\u20B9|Amt)?\\.?\\s*([\\d,]+(?:\\.\\d{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    private val merchantPrefixes = listOf(
        "(?i)(?:by transfer from|transfer from|from)\\s+([A-Za-z0-9\\s&.'@_-]{2,30})",
        "(?i)(?:at|trf to|towards|in favour of|vpa)\\s+([A-Za-z0-9\\s&.'@_-]{2,30})",
        "(?i)(?:to)\\s+([A-Za-z0-9\\s&.'@_-]{2,30})"
    )

    fun parse(smsBody: String, sender: String = "", timestamp: Long = System.currentTimeMillis()): TransactionCandidate? {
        Log.d(TAG, "Parsing SMS from $sender: $smsBody")

        val lowerBody = smsBody.lowercase()

        if (lowerBody.contains("otp") || lowerBody.contains("verification code") || lowerBody.contains("secret code")) {
            if (!transactionKeywords.any { lowerBody.contains(it) }) {
                Log.d(TAG, "SMS ignored: OTP message without transaction indicator")
                return null
            }
        }

        val hasFinancialKeyword = transactionKeywords.any { lowerBody.contains(it) }
        val hasCurrencySymbol = lowerBody.contains("rs") || lowerBody.contains("inr") ||
                                lowerBody.contains("₹") || lowerBody.contains("amt") ||
                                lowerBody.contains("$")

        if (!hasFinancialKeyword && !hasCurrencySymbol) {
            Log.d(TAG, "SMS ignored: No financial keyword or currency symbol found")
            return null
        }

        var amount: Double? = null
        val amountMatcher = amountPattern.matcher(smsBody)
        if (amountMatcher.find()) {
            val amountStr = amountMatcher.group(1)?.replace(",", "")
            amount = amountStr?.toDoubleOrNull()
        }

        if (amount == null) {
            val fallbackMatcher = fallbackAmountPattern.matcher(smsBody)
            if (fallbackMatcher.find()) {
                val amountStr = fallbackMatcher.group(1)?.replace(",", "")
                amount = amountStr?.toDoubleOrNull()
            }
        }

        if (amount == null || amount <= 0.0) {
            Log.d(TAG, "Could not extract valid transaction amount from SMS")
            return null
        }

        val isIncome = incomeKeywords.any { lowerBody.contains(it) } && !lowerBody.contains("debited")
        val type = if (isIncome) "Income" else "Expense"

        val merchant = extractMerchant(smsBody, sender)

        Log.d(TAG, "Parsed Candidate: Amount=$amount, Merchant=$merchant, Type=$type")

        return TransactionCandidate(
            amount = amount,
            merchant = merchant,
            category = "Other",
            date = timestamp,
            account = detectBank(smsBody, sender),
            type = type,
            confidence = 0.85f,
            source = "SMS"
        )
    }

    private fun extractMerchant(smsBody: String, sender: String): String {
        for (prefixRegex in merchantPrefixes) {
            val pattern = Pattern.compile(prefixRegex)
            val matcher = pattern.matcher(smsBody)
            while (matcher.find()) {
                val rawMerchant = matcher.group(1)?.trim() ?: continue
                val cleaned = cleanMerchantName(rawMerchant)
                if (cleaned.isNotBlank() && cleaned.length >= 2 && !cleaned.contains("Bank", true) && !cleaned.contains("A/c", true)) {
                    return cleaned
                }
            }
        }

        val cleanedSender = sender.replace(Regex("[^A-Za-z0-9]"), "").takeLast(6)
        return if (cleanedSender.isNotBlank()) "Txn ($cleanedSender)" else "Merchant"
    }

    private fun cleanMerchantName(raw: String): String {
        val stopWords = listOf(
            "on", "ref", "ref no", "bal", "avail", "balance", "card",
            "using", "via", "upi", "dated", "val", "lim", "is", "dt", "ref:"
        )
        val skipPrefixes = listOf("your", "my", "a/c", "ac", "account", "vpa")

        val parts = raw.split(Regex("\\s+"))
        val resultParts = mutableListOf<String>()

        for (part in parts) {
            val cleanPart = part.lowercase().trim('.', ',', ':', ';')
            if (resultParts.isEmpty() && skipPrefixes.contains(cleanPart)) continue
            if (stopWords.contains(cleanPart) || cleanPart.startsWith("ref") || cleanPart.startsWith("bal")) {
                break
            }
            if (part.matches(Regex("\\d{2}[/-]\\d{2}[/-]\\d{2,4}"))) {
                break
            }
            resultParts.add(part)
        }

        val name = resultParts.joinToString(" ").trim('.', ',', ':', ';', ' ')
        return when {
            name.contains("@") -> name.split("@").firstOrNull()?.capitalizeWords() ?: name
            name.isBlank() -> "Merchant"
            else -> name.capitalizeWords()
        }
    }

    private fun String.capitalizeWords(): String {
        return this.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    private fun detectBank(smsBody: String, sender: String): String {
        val combined = (smsBody + sender).uppercase()
        return when {
            combined.contains("SBI") -> "Bank (SBI)"
            combined.contains("HDFC") -> "Bank (HDFC)"
            combined.contains("ICICI") -> "Bank (ICICI)"
            combined.contains("AXIS") -> "Bank (AXIS)"
            combined.contains("KOTAK") -> "Bank (KOTAK)"
            combined.contains("PNB") -> "Bank (PNB)"
            combined.contains("BOB") || combined.contains("BARODA") -> "Bank (BOB)"
            combined.contains("PAYTM") -> "Wallet (Paytm)"
            else -> "Bank"
        }
    }
}
