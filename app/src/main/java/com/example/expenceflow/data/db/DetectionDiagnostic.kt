package com.example.expenceflow.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detection_diagnostics")
data class DetectionDiagnostic(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val stage: String, // SMS_RECEIVED, PARSER_CANDIDATE, PARSER_REJECTED, DUPLICATE_FILTERED, DB_INSERTED, NOTIFICATION_POSTED, NOTIFICATION_BLOCKED, WORKER_SYNC, ERROR
    val source: String, // SMS, Notification, SMS Inbox, Worker
    val summary: String, // Privacy-safe non-sensitive summary
    val isSuccess: Boolean,
    val errorDetails: String? = null
)
