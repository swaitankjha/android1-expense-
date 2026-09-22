package com.example.expenceflow.data.auto

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.expenceflow.data.dao.PendingTransactionDao
import com.example.expenceflow.data.db.PendingTransaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsInboxScanner @Inject constructor(
    private val smsParser: SmsParser,
    private val extractionEngine: ExtractionEngine,
    private val pendingTransactionDao: PendingTransactionDao
) {
    private val TAG = "SmsInboxScanner"

    suspend fun scanInbox(context: Context, limit: Int = 100): Int {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "READ_SMS permission not granted!")
            throw SecurityException("SMS Permission is not granted. Please allow SMS permission in settings.")
        }

        var addedCount = 0
        try {
            val uri = Uri.parse("content://sms/inbox")
            val projection = arrayOf("_id", "address", "body", "date")
            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "date DESC LIMIT $limit"
            )

            cursor?.use { c ->
                val bodyIdx = c.getColumnIndex("body")
                val addressIdx = c.getColumnIndex("address")
                val dateIdx = c.getColumnIndex("date")

                while (c.moveToNext()) {
                    val body = if (bodyIdx != -1) c.getString(bodyIdx) ?: "" else ""
                    val address = if (addressIdx != -1) c.getString(addressIdx) ?: "Unknown" else "Unknown"
                    val date = if (dateIdx != -1) c.getLong(dateIdx) else System.currentTimeMillis()

                    val candidate = smsParser.parse(body, address, date)
                    if (candidate != null) {
                        val existing = pendingTransactionDao.getExistingPending(
                            candidate.amount, candidate.merchant, candidate.date
                        )
                        if (existing == null) {
                            val finalCandidate = extractionEngine.processCandidate(candidate)
                            if (finalCandidate != null) {
                                val pending = PendingTransaction(
                                    amount = finalCandidate.amount,
                                    merchant = finalCandidate.merchant,
                                    category = finalCandidate.category,
                                    date = finalCandidate.date,
                                    type = finalCandidate.type,
                                    source = "SMS Inbox",
                                    accountSuffix = address
                                )
                                val id = pendingTransactionDao.insertPendingTransaction(pending)
                                if (id > 0) {
                                    addedCount++
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning SMS inbox", e)
            throw e
        }
        Log.d(TAG, "SMS Inbox Scan completed. Added $addedCount transactions.")
        return addedCount
    }
}
