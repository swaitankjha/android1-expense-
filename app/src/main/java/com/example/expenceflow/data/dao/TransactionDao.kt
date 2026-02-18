package com.example.expenceflow.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.expenceflow.data.db.Transaction
import kotlinx.coroutines.flow.Flow
import androidx.room.Update
@Dao
interface TransactionDao {

    // ✅ Renamed from 'insert' to 'insertTransaction' to match Repository
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>
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
    @Update
    suspend fun updateTransaction(transaction: Transaction)
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
}
