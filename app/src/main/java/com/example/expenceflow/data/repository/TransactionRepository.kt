package com.example.expenceflow.data.repository

import com.example.expenceflow.data.dao.TransactionDao
import com.example.expenceflow.data.db.Transaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {

    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions()
    }

    fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByAccount(accountId)
    }

    suspend fun getTransactionCountForAccount(accountId: Long): Int {
        return transactionDao.getTransactionCountForAccount(accountId)
    }

    suspend fun moveTransactionsToAccount(oldAccountId: Long, newAccountId: Long) {
        transactionDao.moveTransactionsToAccount(oldAccountId, newAccountId)
    }

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun hasTransactionBetween(start: Long, end: Long): Boolean {
        return transactionDao.hasTransactionBetween(start, end)
    }

    suspend fun findDuplicateTransaction(
        amount: Double,
        merchant: String,
        date: Long,
        type: String
    ): Transaction? {
        return transactionDao.findDuplicateTransaction(amount, merchant, date, type)
    }
}
