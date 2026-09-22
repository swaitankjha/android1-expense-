package com.example.expenceflow.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_transactions",
    indices = [Index(value = ["amount", "merchant", "date"], unique = true)]
)
data class PendingTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val category: String,
    val date: Long,
    val type: String, // "Income" or "Expense"
    val referenceId: String? = null,
    val accountSuffix: String? = null,
    val source: String = "SMS",
    val status: String = "PENDING", // PENDING, ADDED, DISMISSED
    val detectedAt: Long = System.currentTimeMillis()
)
