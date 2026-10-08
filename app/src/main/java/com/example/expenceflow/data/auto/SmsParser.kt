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
        "txn", "transaction", "transfer", "trf", "withdrawn", "deposited", "refund", "purchase", "payment"
    )

    private val incomeKeywords = listOf(
        "credited", "received", "added", "deposited", "refund", "cashback", "salary", "recd"
    )

    // Pattern 1: Currency before amount (e.g. Rs. 500, Rs 500.00, INR 1,250.50, ₹450, Amt 1000)
    private val currencyBeforeAmountPattern = Pattern.compile(
        "(?i)(?:Rs|INR|\\u20B9|Amt|Amount|USD|\\$)\\.?\\s*([\\d,]+(?:\\.\\d{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    // Pattern 2: Amount before currency (e.g. 500.00 Rs, 1250 INR, 450 ₹)
    private val amountBeforeCurrencyPattern = Pattern.compile(
        "(?i)([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:Rs|INR|\\u20B9|USD|\\$)",
        Pattern.CASE_INSENSITIVE
    )

    // Pattern 3: Financial verb/noun before amount (e.g. debited by 500.00, txn of 250, payment of 100)
    private val verbBeforeAmountPattern = Pattern.compile(
        "(?i)(?:debited|credited|spent|paid|received|sent|withdrawn|txn|transaction|transfer|trf|payment|purchase)\\s+(?:by|for|with|of|is)?\\s*(?:Rs|INR|\\u20B9|Amt)?\\.?\\s*([\\d,]+(?:\\.\\d{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    private val merchantPrefixes = listOf(
        "(?i)(?:by transfer from|transfer from|from)\\s+([A-Za-z0-9\\s&.'@_/-]{2,35})",
        "(?i)(?:at|trf to|towards|in favour of|vpa|info:|info|merchant:|vendor:|store:)\\s+([A-Za-z0-9\\s&.'@_/-]{2,35})",
        "(?i)(?:spent on|paid to|sent to|paid at|to|on|for)\\s+([A-Za-z0-9\\s&.'@_/-]{2,35})"
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

        // Try Pattern 1 (Currency before amount)
        val matcher1 = currencyBeforeAmountPattern.matcher(smsBody)
        if (matcher1.find()) {
            val amountStr = matcher1.group(1)?.replace(",", "")
            amount = amountStr?.toDoubleOrNull()
        }

        // Try Pattern 2 (Amount before currency)
        if (amount == null) {
            val matcher2 = amountBeforeCurrencyPattern.matcher(smsBody)
            if (matcher2.find()) {
                val amountStr = matcher2.group(1)?.replace(",", "")
                amount = amountStr?.toDoubleOrNull()
            }
        }

        // Try Pattern 3 (Verb before amount)
        if (amount == null) {
            val matcher3 = verbBeforeAmountPattern.matcher(smsBody)
            if (matcher3.find()) {
                val amountStr = matcher3.group(1)?.replace(",", "")
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
        var processed = raw

        // Handle raw UPI patterns like UPI/30123456/Zomato or INF*Zomato*123
        if (processed.contains("/") || processed.contains("*")) {
            val segments = processed.split(Regex("[/*]"))
            val bestSegment = segments.firstOrNull { seg ->
                val cleanSeg = seg.trim().lowercase()
                cleanSeg.length >= 3 &&
                !cleanSeg.matches(Regex("\\d+")) &&
                !listOf("upi", "inf", "vpa", "payment", "txn", "ref", "p2m", "p2p").contains(cleanSeg)
            }
            if (bestSegment != null) {
                processed = bestSegment.trim()
            }
        }

        val stopWords = listOf(
            "on", "ref", "ref no", "bal", "avail", "balance", "card",
            "using", "via", "upi", "dated", "val", "lim", "is", "dt", "ref:"
        )
        val skipPrefixes = listOf("your", "my", "a/c", "ac", "account", "vpa")

        val parts = processed.split(Regex("\\s+"))
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
