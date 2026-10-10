package com.example.expenceflow.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.expenceflow.data.dao.AccountDao
import com.example.expenceflow.data.dao.BudgetDao
import com.example.expenceflow.data.dao.DetectionDiagnosticDao
import com.example.expenceflow.data.dao.PendingTransactionDao
import com.example.expenceflow.data.dao.TransactionDao

@Database(
    entities = [Transaction::class, BudgetGoal::class, Account::class, PendingTransaction::class, DetectionDiagnostic::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun accountDao(): AccountDao
    abstract fun pendingTransactionDao(): PendingTransactionDao
    abstract fun detectionDiagnosticDao(): DetectionDiagnosticDao
}
