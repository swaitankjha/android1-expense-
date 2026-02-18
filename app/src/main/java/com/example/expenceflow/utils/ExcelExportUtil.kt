package com.example.expenceflow.utils

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.expenceflow.data.db.Transaction
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

fun exportTransactionsToExcel(
    context: Context,
    transactions: List<Transaction>
) {
    try {
        val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        val fileName = "ExpenseFlow_${System.currentTimeMillis()}.csv"

        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            contentValues
        )

        if (uri == null) {
            Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show()
            return
        }

        resolver.openOutputStream(uri)?.use { outputStream ->
            val writer = OutputStreamWriter(outputStream)

            // 🔹 Header
            writer.append("Title,Amount,Type,Category,Date\n")

            // 🔹 Rows
            transactions.forEach { tx ->
                writer.append(
                    "${tx.title}," +
                            "${tx.amount}," +
                            "${tx.type}," +
                            "${tx.category}," +
                            "${sdf.format(Date(tx.date))}\n"
                )
            }

            writer.flush()
            writer.close()
        }

        Toast.makeText(
            context,
            "Saved to Downloads 📂",
            Toast.LENGTH_LONG
        ).show()

    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Export failed: ${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }
}
