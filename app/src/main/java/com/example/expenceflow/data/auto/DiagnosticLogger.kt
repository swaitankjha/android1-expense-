package com.example.expenceflow.data.auto

import android.util.Log
import com.example.expenceflow.data.dao.DetectionDiagnosticDao
import com.example.expenceflow.data.db.DetectionDiagnostic
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticLogger @Inject constructor(
    private val diagnosticDao: DetectionDiagnosticDao
) {
    private val TAG = "DiagnosticLogger"

    suspend fun log(
        stage: String,
        source: String,
        summary: String,
        isSuccess: Boolean,
        errorDetails: String? = null
    ) {
        try {
            val diagnostic = DetectionDiagnostic(
                stage = stage,
                source = source,
                summary = summary,
                isSuccess = isSuccess,
                errorDetails = errorDetails
            )
            Log.d(TAG, "[$stage][$source] $summary ${errorDetails ?: ""}")
            diagnosticDao.insertDiagnostic(diagnostic)
            diagnosticDao.purgeOldDiagnostics()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist diagnostic log", e)
        }
    }
}
