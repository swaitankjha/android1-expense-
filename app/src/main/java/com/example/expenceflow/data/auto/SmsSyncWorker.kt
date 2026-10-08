package com.example.expenceflow.data.auto

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SmsSyncWorkerEntryPoint {
    fun smsInboxScanner(): SmsInboxScanner
}

class SmsSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val TAG = "SmsSyncWorker"

    override suspend fun doWork(): Result {
        Log.d(TAG, "Executing periodic SMS sync worker")

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "READ_SMS permission not granted. Skipping worker execution.")
            return Result.success()
        }

        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                SmsSyncWorkerEntryPoint::class.java
            )
            val smsInboxScanner = entryPoint.smsInboxScanner()
            val addedCount = smsInboxScanner.scanInbox(applicationContext, limit = 50)
            Log.d(TAG, "SmsSyncWorker finished successfully. Added $addedCount transactions.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error running SmsSyncWorker", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "sms_periodic_sync_worker"

        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<SmsSyncWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
