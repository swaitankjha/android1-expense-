package com.example.expenceflow.data.auto

import android.graphics.Bitmap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptScannerRepository @Inject constructor(
    private val scanner: ReceiptScanner,
    private val extractionEngine: ExtractionEngine
) {
    suspend fun scanAndProcess(bitmap: Bitmap): TransactionCandidate? {
        val candidate = scanner.scanReceipt(bitmap) ?: return null
        return extractionEngine.processCandidate(candidate)
    }
}
