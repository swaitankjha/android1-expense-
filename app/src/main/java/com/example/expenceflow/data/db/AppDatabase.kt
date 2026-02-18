package com.example.expenceflow.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.expenceflow.data.dao.BudgetDao
import com.example.expenceflow.data.dao.TransactionDao
@Database(
    entities = [Transaction::class, BudgetGoal::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao

    // The companion object is deleted because Hilt now creates the database.
}
