package com.example.expenceflow.data.db // ✅ Changed from .data.dao to .data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_goals")
data class BudgetGoal(
    @PrimaryKey val month: String, // format: "2025-06"
    val amount: Int                // budget in ₹
)
