package com.example.expenceflow.ui.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.expenceflow.data.dao.PendingTransactionDao
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
interface NotificationActionReceiverEntryPoint {
    fun pendingTransactionDao(): PendingTransactionDao
}

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {
    private val TAG = "NotificationActionReceiver"

    @Inject
    lateinit var pendingTransactionDao: PendingTransactionDao

    override fun onReceive(context: Context, intent: Intent?) {
        if (!::pendingTransactionDao.isInitialized) {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    NotificationActionReceiverEntryPoint::class.java
                )
                pendingTransactionDao = entryPoint.pendingTransactionDao()
            } catch (e: Exception) {
                Log.e(TAG, "Error performing EntryPoint injection in NotificationActionReceiver", e)
            }
        }

        val pendingId = intent?.getLongExtra("pending_id", -1L) ?: -1L
        if (pendingId == -1L) return

        when (intent?.action) {
            "ACTION_DISMISS" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    pendingTransactionDao.updateStatus(pendingId, "DISMISSED")
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(pendingId.toInt())
            }
        }
    }
}
