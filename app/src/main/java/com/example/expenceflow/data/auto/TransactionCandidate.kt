package com.example.expenceflow.data.auto

data class TransactionCandidate(
    val amount: Double,
    val merchant: String,
    val category: String,
    val date: Long,
    val account: String,
    val type: String,
    val notes: String = "",
    val confidence: Float = 1.0f,
    val source: String,
    val paymentMode: String = "UPI" // "Cash", "UPI", "Bank Transfer", "Card"
)
