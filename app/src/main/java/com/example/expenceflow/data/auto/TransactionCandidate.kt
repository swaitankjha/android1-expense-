package com.example.expenceflow.data.auto

data class TransactionCandidate(
    val amount: Double,
    val merchant: String,
    val category: String,
    val date: Long,
    val account: String,
    val type: String, // "Income" or "Expense"
    val isTypeConfident: Boolean = true,
    val notes: String = "",
    val confidence: Float,
    val source: String // "SMS", "Notification", "OCR", "Voice", "CSV", "PDF"
)
