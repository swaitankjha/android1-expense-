package com.example.expenceflow.data.auto

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.expenceflow.data.dao.PendingTransactionDao
import com.example.expenceflow.data.db.PendingTransaction
import com.example.expenceflow.ui.notification.SmsNotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TransactionNotificationListener : NotificationListenerService() {
    private val TAG = "NotificationListener"

    @Inject
    lateinit var extractionEngine: ExtractionEngine

    @Inject
    lateinit var detectionRepository: DetectionRepository

    @Inject
    lateinit var pendingTransactionDao: PendingTransactionDao

    @Inject
    lateinit var smsParser: SmsParser

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification.extras

        // Use getCharSequence to safely extract SpannableString titles and text
        val title = extras.getCharSequence("android.title")?.toString()
            ?: extras.getCharSequence("android.title.big")?.toString()
            ?: ""
        val text = extras.getCharSequence("android.text")?.toString()
            ?: extras.getCharSequence("android.bigText")?.toString()
            ?: extras.getCharSequence("android.subText")?.toString()
            ?: ""

        val combinedText = "$title $text".trim()
        Log.d(TAG, "Notification received from $packageName: $combinedText")

        val candidate = parseNotification(title, text, packageName)
        if (candidate != null) {
            Log.d(TAG, "Parsed Notification Candidate: Amount=${candidate.amount}, Merchant=${candidate.merchant}")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val finalCandidate = extractionEngine.processCandidate(candidate)
                    if (finalCandidate != null) {
                        detectionRepository.addCandidate(finalCandidate)

                        val pending = PendingTransaction(
                            amount = finalCandidate.amount,
                            merchant = finalCandidate.merchant,
                            category = finalCandidate.category,
                            date = finalCandidate.date,
                            type = finalCandidate.type,
                            source = "Notification",
                            accountSuffix = packageName
                        )
                        val id = pendingTransactionDao.insertPendingTransaction(pending)
                        if (id > 0) {
                            Log.d(TAG, "Inserted Pending Notification ID $id")
                            SmsNotificationHelper.showDetectionNotification(this@TransactionNotificationListener, pending.copy(id = id))
                        }
                    } else {
                        Log.d(TAG, "ExtractionEngine filtered notification candidate")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing notification candidate", e)
                }
            }
        }
    }

    private fun parseNotification(title: String, text: String, packageName: String): TransactionCandidate? {
        val fullText = "$title $text"
        val smsCandidate = smsParser.parse(fullText, packageName, System.currentTimeMillis())
        if (smsCandidate != null) {
            return smsCandidate.copy(source = "Notification")
        }

        val amountRegex = Regex("(?:Rs|INR|\\u20B9|\\$)\\.?\\s*([\\d,]+\\.?\\d*)", RegexOption.IGNORE_CASE)
        val match = amountRegex.find(text) ?: amountRegex.find(title)

        if (match != null) {
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            if (amount <= 0.0) return null

            val isIncome = fullText.contains("received", true) || fullText.contains("credited", true)
            return TransactionCandidate(
                amount = amount,
                merchant = if (title.contains("Paid to", true)) title.replace("Paid to", "").trim() else "UPI Payment",
                category = "Other",
                date = System.currentTimeMillis(),
                account = "UPI",
                type = if (isIncome) "Income" else "Expense",
                confidence = 0.75f,
                source = "Notification"
            )
        }
        return null
    }
}
