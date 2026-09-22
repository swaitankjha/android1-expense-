package com.example.expenceflow.data.auto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.example.expenceflow.data.dao.PendingTransactionDao
import com.example.expenceflow.data.db.PendingTransaction
import com.example.expenceflow.ui.notification.SmsNotificationHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SmsReceiverEntryPoint {
    fun smsParser(): SmsParser
    fun extractionEngine(): ExtractionEngine
    fun pendingTransactionDao(): PendingTransactionDao
}

@AndroidEntryPoint
class SmsReceiver @Inject constructor() : BroadcastReceiver() {
    private val TAG = "SmsReceiver"

    @Inject
    lateinit var smsParser: SmsParser

    @Inject
    lateinit var extractionEngine: ExtractionEngine

    @Inject
    lateinit var pendingTransactionDao: PendingTransactionDao

    override fun onReceive(context: Context, intent: Intent) {
        if (!::smsParser.isInitialized) {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    SmsReceiverEntryPoint::class.java
                )
                smsParser = entryPoint.smsParser()
                extractionEngine = entryPoint.extractionEngine()
                pendingTransactionDao = entryPoint.pendingTransactionDao()
            } catch (e: Exception) {
                Log.e(TAG, "Error performing EntryPoint injection in SmsReceiver", e)
            }
        }

        val pendingResult = goAsync()

        Log.d(TAG, "!!! SMS RECEIVER TRIGGERED !!! Action: ${intent.action}")

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION ||
            intent.action == "android.provider.Telephony.SMS_RECEIVED") {

            val messages = getMessages(intent)
            if (messages.isEmpty()) {
                pendingResult.finish()
                return
            }

            val fullBody = messages.joinToString("") { it?.displayMessageBody ?: "" }
            val sender = messages.firstOrNull()?.displayOriginatingAddress ?: "Unknown"
            val timestamp = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

            Log.d(TAG, "SMS FROM: $sender, Body: $fullBody")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val candidate = smsParser.parse(fullBody, sender, timestamp)
                    if (candidate != null) {
                        val existing = pendingTransactionDao.getExistingPending(
                            candidate.amount, candidate.merchant, candidate.date
                        )

                        if (existing != null) {
                            Log.d(TAG, "Duplicate pending transaction detected, skipping.")
                        } else {
                            val finalCandidate = extractionEngine.processCandidate(candidate)
                            if (finalCandidate != null) {
                                val pending = PendingTransaction(
                                    amount = finalCandidate.amount,
                                    merchant = finalCandidate.merchant,
                                    category = finalCandidate.category,
                                    date = finalCandidate.date,
                                    type = finalCandidate.type,
                                    source = "SMS",
                                    accountSuffix = sender
                                )
                                val id = pendingTransactionDao.insertPendingTransaction(pending)
                                if (id > 0) {
                                    Log.d(TAG, "SUCCESS: Inserted Pending ID $id")
                                    SmsNotificationHelper.showDetectionNotification(context, pending.copy(id = id))
                                }
                            } else {
                                Log.d(TAG, "ExtractionEngine filtered candidate")
                            }
                        }
                    } else {
                        Log.d(TAG, "SmsParser returned null for message")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "CRITICAL ERROR in SmsReceiver pipeline", e)
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            pendingResult.finish()
        }
    }

    private fun getMessages(intent: Intent): Array<SmsMessage?> {
        return try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (e: Exception) {
            val bundle = intent.extras
            val pdus = bundle?.get("pdus") as? Array<*>
            val format = bundle?.getString("format")
            if (pdus != null) {
                pdus.map {
                    @Suppress("DEPRECATION")
                    SmsMessage.createFromPdu(it as ByteArray, format)
                }.toTypedArray()
            } else {
                emptyArray()
            }
        }
    }
}
