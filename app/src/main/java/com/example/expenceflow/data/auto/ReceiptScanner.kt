package com.example.expenceflow.data.auto

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptScanner @Inject constructor() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun scanReceipt(bitmap: Bitmap): TransactionCandidate? {
        val image = InputImage.fromBitmap(bitmap, 0)
        return try {
            val result = recognizer.process(image).await()
            parseText(result.text)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseText(text: String): TransactionCandidate? {
        // Look for common keywords like "Total", "Grand Total", "Amount Paid"
        val lines = text.lines()
        var amount: Double? = null
        var merchant: String = "Unknown Merchant"

        // Simple merchant extraction: Usually the first line
        if (lines.isNotEmpty()) {
            merchant = lines[0].trim()
        }

        val totalRegex = Regex("(?i)(?:Total|Amount|Sum|Grand Total)[:\\s]*[\\u20B9]?[:\\s]*([\\d,]+\\.?\\d*)")
        
        for (line in lines) {
            val match = totalRegex.find(line)
            if (match != null) {
                amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
                break
            }
        }

        if (amount != null) {
            return TransactionCandidate(
                amount = amount,
                merchant = merchant,
                category = "Other",
                date = System.currentTimeMillis(),
                account = "Cash",
                type = "Expense",
                confidence = 0.6f,
                source = "OCR"
            )
        }
        return null
    }
}
