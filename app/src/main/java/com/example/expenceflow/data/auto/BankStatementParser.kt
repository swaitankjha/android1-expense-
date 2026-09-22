package com.example.expenceflow.data.auto

import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

interface StatementParser {
    suspend fun parse(inputStream: InputStream): List<TransactionCandidate>
}

@Singleton
class ExcelStatementParser @Inject constructor() : StatementParser {
    private val TAG = "ExcelStatementParser"

    override suspend fun parse(inputStream: InputStream): List<TransactionCandidate> {
        System.setProperty("org.apache.poi.ss.ignoreMissingFontMetrics", "true")
        val transactions = mutableListOf<TransactionCandidate>()
        val formatter = DataFormatter()
        try {
            // Using XSSFWorkbook directly to bypass some WorkbookFactory AWT checks
            val workbook = XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0)
            
            // Find the header row (first non-empty row containing "amount" or "date")
            var headerRowIndex = -1
            for (i in 0..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                val rowText = row.map { it?.toString() ?: "" }.joinToString(" ")
                if (rowText.contains("amount", true) || rowText.contains("date", true) || rowText.contains("title", true)) {
                    headerRowIndex = i
                    break
                }
            }

            if (headerRowIndex == -1) {
                Log.e(TAG, "Could not find header row in Excel")
                workbook.close()
                return emptyList()
            }

            val headerRow = sheet.getRow(headerRowIndex)
            val headers = mutableListOf<String>()
            for (i in 0 until headerRow.lastCellNum) {
                headers.add(headerRow.getCell(i)?.toString()?.lowercase()?.trim() ?: "")
            }

            Log.d(TAG, "Excel Headers: $headers")

            for (rowIndex in (headerRowIndex + 1)..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue
                val data = mutableMapOf<String, String>()
                
                headers.forEachIndexed { index, header ->
                    if (header.isNotEmpty()) {
                        val cell = row.getCell(index)
                        data[header] = formatter.formatCellValue(cell)
                    }
                }

                if (data.values.all { it.isEmpty() }) continue

                val candidate = parseRowMap(data)
                if (candidate != null) {
                    transactions.add(candidate)
                }
            }
            workbook.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Excel: ${e.message}")
        }
        return transactions
    }

    private fun parseRowMap(data: Map<String, String>): TransactionCandidate? {
        val amountKey = data.keys.find { it.contains("amount") || it.contains("debit") || it.contains("credit") || it.contains("value") || it.contains("amt") } ?: return null
        val merchantKey = data.keys.find { it.contains("title") || it.contains("description") || it.contains("merchant") || it.contains("particulars") || it.contains("narration") } ?: return null
        val dateKey = data.keys.find { it.contains("date") || it.contains("time") }
        val typeKey = data.keys.find { it.contains("type") }

        val amountStr = data[amountKey]?.replace(",", "")?.replace("₹", "")?.trim() ?: return null
        val amount = amountStr.toDoubleOrNull() ?: 0.0
        if (amount == 0.0) return null

        val merchant = data[merchantKey]?.trim() ?: "Unknown"
        if (merchant.isEmpty()) return null

        val type = data[typeKey]?.trim() ?: if (amountKey.contains("credit") || amount > 0) "Income" else "Expense"
        
        var date = System.currentTimeMillis()
        dateKey?.let { key ->
            data[key]?.let { dateStr ->
                date = parseDate(dateStr) ?: date
            }
        }

        return TransactionCandidate(
            amount = Math.abs(amount),
            merchant = merchant,
            category = "Other",
            date = date,
            account = "Bank",
            type = if (type.contains("income", true) || type.contains("credit", true)) "Income" else "Expense",
            confidence = 1.0f,
            source = "EXCEL"
        )
    }

    private fun parseDate(dateStr: String): Long? {
        val formats = listOf(
            "dd-MMM-yyyy HH:mm",
            "dd-MM-yyyy HH:mm",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "yyyy-MM-dd",
            "MM/dd/yyyy",
            "dd MMM yyyy",
            "dd-MMM-yyyy"
        )
        for (format in formats) {
            try {
                return SimpleDateFormat(format, Locale.getDefault()).parse(dateStr)?.time
            } catch (e: Exception) {}
        }
        return null
    }
}

@Singleton
class CsvStatementParser @Inject constructor() : StatementParser {
    private val TAG = "CsvStatementParser"

    override suspend fun parse(inputStream: InputStream): List<TransactionCandidate> {
        val transactions = mutableListOf<TransactionCandidate>()
        val reader = inputStream.bufferedReader()
        var headers: List<String>? = null
        
        try {
            reader.forEachLine { line ->
                if (line.isBlank()) return@forEachLine
                
                // Robust CSV splitting (handles quotes)
                val parts = line.split(Regex(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))
                    .map { it.trim().replace("\"", "") }
                
                Log.d(TAG, "Processing line: $line")

                if (headers == null) {
                    // Try to detect if this line is a header (contains "amount" or "date")
                    if (line.contains("amount", true) || line.contains("date", true) || line.contains("debit", true)) {
                        headers = parts
                        Log.d(TAG, "Headers detected: $headers")
                    }
                } else {
                    val candidate = parseRow(headers!!, parts)
                    if (candidate != null) {
                        transactions.add(candidate)
                        Log.d(TAG, "Transaction added: ${candidate.merchant} - ${candidate.amount}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing CSV: ${e.message}")
        }
        return transactions
    }

    private fun parseRow(headers: List<String>, parts: List<String>): TransactionCandidate? {
        if (parts.size < headers.size) return null
        
        val data = headers.zip(parts).toMap()
        
        val merchantKey = data.keys.find { 
            it.contains("description", true) || it.contains("narration", true) || it.contains("merchant", true) || it.contains("particulars", true) || it.contains("title", true)
        } ?: return null
        
        val dateKey = data.keys.find { it.contains("date", true) }

        // Handle separate Debit/Credit columns or a single Amount column
        val debitKey = data.keys.find { it.contains("debit", true) || it.contains("withdrawal", true) }
        val creditKey = data.keys.find { it.contains("credit", true) || it.contains("deposit", true) }
        val genericAmountKey = data.keys.find { it.contains("amount", true) || it.contains("value", true) }

        var amount: Double
        var type: String

        if (debitKey != null && data[debitKey]?.replace(",", "")?.toDoubleOrNull()?.let { it != 0.0 } == true) {
            amount = Math.abs(data[debitKey]!!.replace(",", "").toDoubleOrNull() ?: 0.0)
            type = "Expense"
        } else if (creditKey != null && data[creditKey]?.replace(",", "")?.toDoubleOrNull()?.let { it != 0.0 } == true) {
            amount = Math.abs(data[creditKey]!!.replace(",", "").toDoubleOrNull() ?: 0.0)
            type = "Income"
        } else if (genericAmountKey != null) {
            val rawAmount = genericAmountAmount(data[genericAmountKey]!!)
            amount = Math.abs(rawAmount)
            type = if (rawAmount > 0) "Income" else "Expense"
        } else {
            return null
        }

        if (amount == 0.0) return null

        val merchant = data[merchantKey] ?: "Unknown"
        
        var date = System.currentTimeMillis()
        dateKey?.let { key ->
            data[key]?.let { dateStr ->
                date = parseDate(dateStr) ?: date
            }
        }

        return TransactionCandidate(
            amount = amount,
            merchant = merchant,
            category = "Other",
            date = date,
            account = "Bank",
            type = type,
            confidence = 1.0f,
            source = "CSV"
        )
    }

    private fun genericAmountAmount(str: String): Double {
        val clean = str.replace(",", "")
        return clean.toDoubleOrNull() ?: 0.0
    }

    private fun parseDate(dateStr: String): Long? {
        val formats = listOf(
            "dd-MMM-yyyy HH:mm",
            "dd-MM-yyyy HH:mm",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "yyyy-MM-dd",
            "MM/dd/yyyy",
            "dd MMM yyyy",
            "dd-MMM-yyyy"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                return sdf.parse(dateStr)?.time
            } catch (e: Exception) {}
        }
        return null
    }
}

@Singleton
class PdfStatementParser @Inject constructor() {
    private val TAG = "PdfStatementParser"
    
    fun extractTransactions(inputStream: InputStream): List<TransactionCandidate> {
        return try {
            val document = PDDocument.load(inputStream)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            document.close()
            Log.d(TAG, "Extracted PDF text length: ${text.length}")
            parsePdfText(text)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing PDF: ${e.message}")
            emptyList()
        }
    }

    private fun parsePdfText(text: String): List<TransactionCandidate> {
        val transactions = mutableListOf<TransactionCandidate>()
        
        // 1. Standard format: Date Description Ref Amount Dr/Cr
        val regex1 = Regex("(\\d{2}[-/]\\d{2}[-/]\\d{2,4})\\s+(.*?)\\s+([\\d,]+\\.\\d{2})\\s*(DR|CR)?")
        
        // 2. Generic format: Keyword (Paid/Sent/Received/Transfer) Merchant Amount
        val regex2 = Regex("(?i)(?:Paid|Sent|Received|Transfer|UPI).*?\\s+([\\d,]+\\.\\d{2})")

        regex1.findAll(text).forEach { match ->
            val merchant = match.groupValues[2].trim()
            val amountStr = match.groupValues[3].replace(",", "")
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            val drCr = match.groupValues[4]
            
            Log.d(TAG, "Found PDF line (Standard): ${match.value}")

            transactions.add(
                TransactionCandidate(
                    amount = amount,
                    merchant = merchant,
                    category = "Other",
                    date = System.currentTimeMillis(),
                    account = "Bank",
                    type = if (drCr == "CR") "Income" else "Expense",
                    confidence = 0.9f,
                    source = "PDF"
                )
            )
        }
        
        if (transactions.isEmpty()) {
            // Try secondary parsing if standard fails
            regex2.findAll(text).forEach { match ->
                val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                     transactions.add(
                        TransactionCandidate(
                            amount = amount,
                            merchant = "Bank Transaction",
                            category = "Other",
                            date = System.currentTimeMillis(),
                            account = "Bank",
                            type = if (match.value.contains("Received", true)) "Income" else "Expense",
                            confidence = 0.6f,
                            source = "PDF"
                        )
                    )
                }
            }
        }

        return transactions
    }
}
