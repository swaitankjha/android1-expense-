package com.example.expenceflow.ui.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.expenceflow.R
import com.example.expenceflow.data.db.PendingTransaction
import com.example.expenceflow.ui.transaction.QuickConfirmActivity

object SmsNotificationHelper {
    private const val CHANNEL_ID = "transaction_detections"
    private const val CHANNEL_NAME = "Transaction Detections"

    fun showDetectionNotification(context: Context, pendingTx: PendingTransaction) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        // Action: Add (Opens Dialog Activity with fallback extras)
        val addIntent = Intent(context, QuickConfirmActivity::class.java).apply {
            putExtra("pending_id", pendingTx.id)
            putExtra("amount", pendingTx.amount)
            putExtra("merchant", pendingTx.merchant)
            putExtra("type", pendingTx.type)
            putExtra("category", pendingTx.category)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val addPendingIntent = PendingIntent.getActivity(
            context,
            pendingTx.id.toInt(),
            addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Dismiss (Triggers Broadcast to mark as DISMISSED)
        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_DISMISS"
            putExtra("pending_id", pendingTx.id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            pendingTx.id.toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val icon = if (pendingTx.type == "Income") "💰" else "💸"
        val title = "$icon ${if (pendingTx.type == "Income") "Received" else "Spent"} ₹${pendingTx.amount}"
        val text = "At ${pendingTx.merchant}. Tap to review."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(addPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Add", addPendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .setGroup("transaction_group")
            .build()

        manager.notify(pendingTx.id.toInt(), notification)
    }
}
