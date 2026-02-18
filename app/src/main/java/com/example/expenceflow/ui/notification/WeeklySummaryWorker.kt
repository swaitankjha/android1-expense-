package com.example.expenceflow.ui.notification
import android.annotation.SuppressLint
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
import java.util.Calendar

class WeeklySummaryWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {

        // 🔥 READ WEEKLY DATA (SharedPreferences)
        val prefs =
            applicationContext.getSharedPreferences("weekly", Context.MODE_PRIVATE)

        val savedWeek = prefs.getInt("week", -1)
        val amount = prefs.getFloat("amount", 0f)

        val currentWeek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)

        if (savedWeek == currentWeek && amount > 0f) {
            showNotification(amount.toInt())
        }

        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(total: Int) {

        // ✅ Android 13+ permission check
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val channelId = "expenseflow_weekly"

        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Weekly Summary",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("ExpenseFlow")
            .setContentText("You spent ₹$total this week 📊")
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(3001, notification)
    }
}
