package com.example.expenceflow.data.repository

import com.example.expenceflow.data.db.Transaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import com.example.expenceflow.data.dao.TransactionDao
@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {

    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions()
    }

    suspend fun insertTransaction(transaction: Transaction) {
        // This now matches the @Insert method in your DAO
        transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        // This now matches the @Delete method in your DAO
        transactionDao.deleteTransaction(transaction)
    }
    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }
    suspend fun hasTransactionBetween(start: Long, end: Long): Boolean {
        return transactionDao.hasTransactionBetween(start, end)
    }

}
