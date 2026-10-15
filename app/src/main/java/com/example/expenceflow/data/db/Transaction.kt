package com.example.expenceflow.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val date: Long,
    val type: String, // e.g., "Income", "Expense"
    val category: String, // e.g., "Food", "Salary"
    val account: String = "Personal", // Money Space / Account name
    val accountId: Long = 1, // Link to Account entity
    val paymentMode: String = "UPI" // "Cash", "UPI", "Bank Transfer", "Card"
)
