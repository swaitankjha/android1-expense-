package com.example.expenceflow.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val date: Long, // Changed from Date to Long (timestamp) to simplify Room storage
    val type: String, // e.g., "Income", "Expense"
    val category: String // e.g., "Food", "Salary"
)
