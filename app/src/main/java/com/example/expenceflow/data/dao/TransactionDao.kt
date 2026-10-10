package com.example.expenceflow.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.expenceflow.data.db.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC, id DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>>

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId")
    suspend fun getTransactionCountForAccount(accountId: Long): Int

    @Query("UPDATE transactions SET accountId = :newAccountId WHERE accountId = :oldAccountId")
    suspend fun moveTransactionsToAccount(oldAccountId: Long, newAccountId: Long)

    @Query(
        "SELECT EXISTS(" +
                "SELECT 1 FROM transactions " +
                "WHERE date BETWEEN :start AND :end)"
    )
    suspend fun hasTransactionBetween(start: Long, end: Long): Boolean

    @Query(
        """
        SELECT SUM(amount)
        FROM transactions
        WHERE type = :type
        AND strftime('%Y-%m', date / 1000, 'unixepoch') = :month
        """
    )
    suspend fun getMonthlyTotal(
        type: String,
        month: String
    ): Double?

    @Query("""
        SELECT * FROM transactions 
        WHERE amount = :amount 
        AND type = :type
        AND (
            title = :merchant 
            OR title = 'Merchant' 
            OR :merchant = 'Merchant'
            OR title LIKE '%' || :merchant || '%' 
            OR :merchant LIKE '%' || title || '%'
        )
        AND abs(date - :date) <= :timeWindowMs
        LIMIT 1
    """)
    suspend fun findDuplicateTransaction(
        amount: Double,
        merchant: String,
        date: Long,
        type: String,
        timeWindowMs: Long = 5 * 60 * 1000
    ): Transaction?

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
}
