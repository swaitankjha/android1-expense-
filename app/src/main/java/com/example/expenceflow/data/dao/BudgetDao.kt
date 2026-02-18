package com.example.expenceflow.data.dao

import androidx.room.*
import com.example.expenceflow.data.db.BudgetGoal // ✅ Added this import

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: BudgetGoal)

    @Query("SELECT * FROM budget_goals WHERE month = :month LIMIT 1")
    suspend fun getGoalForMonth(month: String): BudgetGoal?
}
