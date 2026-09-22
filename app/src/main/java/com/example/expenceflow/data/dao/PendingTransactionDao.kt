package com.example.expenceflow.data.dao

import androidx.room.*
import com.example.expenceflow.data.db.PendingTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTransactionDao {
    @Query("SELECT * FROM pending_transactions WHERE status = 'PENDING' ORDER BY detectedAt DESC")
    fun getPendingTransactions(): Flow<List<PendingTransaction>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPendingTransaction(transaction: PendingTransaction): Long

    @Update
    suspend fun updatePendingTransaction(transaction: PendingTransaction)

    @Delete
    suspend fun deletePendingTransaction(transaction: PendingTransaction)

    @Query("UPDATE pending_transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("SELECT * FROM pending_transactions WHERE amount = :amount AND merchant = :merchant AND date = :date LIMIT 1")
    suspend fun getExistingPending(amount: Double, merchant: String, date: Long): PendingTransaction?
}
