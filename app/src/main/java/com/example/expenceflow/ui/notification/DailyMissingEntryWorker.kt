package com.example.expenceflow.ui.notification

import android.app.PendingIntent
import android.content.Intent
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.expenceflow.R
import java.util.*
import android.annotation.SuppressLint
class DailyMissingEntryWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {

        val prefs =
            applicationContext.getSharedPreferences("daily", Context.MODE_PRIVATE)

        val lastEntry = prefs.getLong("last_entry_date", 0L)

        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        if (lastEntry < todayStart) {
            NotificationUtils.showNotification(
                applicationContext,
                "ExpenseFlow",
                "You haven’t logged any transaction today 💸"
            )
        }

        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun showNotification() {
            // ✅ Android 13 permission check
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val channelId = "expenseflow_daily"

            val manager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        channelId,
                        "Daily Reminder",
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                )
            }

            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("ExpenseFlow")
                .setContentText("You haven’t logged any transaction today 💸")
                .setAutoCancel(true)
                .build()
        if (manager.areNotificationsEnabled()) {
            manager.notify(2001, notification)
        }

    }
    }

