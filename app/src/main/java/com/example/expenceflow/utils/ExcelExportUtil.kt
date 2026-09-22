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

fun exportTransactionsToCsv(
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

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri == null) {
            Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show()
            return
        }

        resolver.openOutputStream(uri)?.use { outputStream ->
            val writer = OutputStreamWriter(outputStream)
            writer.append("Title,Amount,Type,Category,Date,Account\n")
            transactions.forEach { tx ->
                writer.append("${tx.title},${tx.amount},${tx.type},${tx.category},${sdf.format(Date(tx.date))},${tx.account}\n")
            }
            writer.flush()
            writer.close()
        }
        Toast.makeText(context, "CSV Saved to Downloads 📂", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

/**
 * Exports transactions using the XML Spreadsheet 2003 format.
 * This provides formatting (bold, colors, borders) without using Apache POI,
 * avoiding AWT crashes on Android.
 */
fun exportTransactionsToExcel(
    context: Context,
    transactions: List<Transaction>
) {
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
        val displaySdf = SimpleDateFormat("dd-MMM-yyyy HH:mm", Locale.getDefault())
        
        val totalIncome = transactions.filter { it.type.equals("Income", true) }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type.equals("Expense", true) }.sumOf { it.amount }

        val xml = StringBuilder()
        xml.append("<?xml version=\"1.0\"?>\n")
        xml.append("<?mso-application progid=\"Excel.Sheet\"?>\n")
        xml.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
        xml.append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n")
        xml.append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n")
        xml.append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
        xml.append(" xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n")

        // Styles
        xml.append(" <Styles>\n")
        xml.append("  <Style ss:ID=\"Default\" ss:Name=\"Normal\">\n")
        xml.append("   <Alignment ss:Vertical=\"Bottom\"/>\n")
        xml.append("   <Borders/>\n")
        xml.append("   <Font ss:FontName=\"Calibri\" x:Family=\"Swiss\" ss:Size=\"11\" ss:Color=\"#000000\"/>\n")
        xml.append("  </Style>\n")
        xml.append("  <Style ss:ID=\"Header\">\n")
        xml.append("   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>\n")
        xml.append("   <Borders>\n")
        xml.append("    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>\n")
        xml.append("   </Borders>\n")
        xml.append("   <Font ss:FontName=\"Calibri\" ss:Size=\"12\" ss:Color=\"#FFFFFF\" ss:Bold=\"1\"/>\n")
        xml.append("   <Interior ss:Color=\"#4472C4\" ss:Pattern=\"Solid\"/>\n")
        xml.append("  </Style>\n")
        xml.append("  <Style ss:ID=\"Income\">\n")
        xml.append("   <Font ss:Color=\"#008000\"/>\n")
        xml.append("  </Style>\n")
        xml.append("  <Style ss:ID=\"Expense\">\n")
        xml.append("   <Font ss:Color=\"#FF0000\"/>\n")
        xml.append("  </Style>\n")
        xml.append("  <Style ss:ID=\"Currency\">\n")
        xml.append("   <NumberFormat ss:Format=\"&quot;₹&quot;\\ #,##0.00\"/>\n")
        xml.append("  </Style>\n")
        xml.append("  <Style ss:ID=\"Summary\">\n")
        xml.append("   <Font ss:Bold=\"1\"/>\n")
        xml.append("   <Interior ss:Color=\"#F2F2F2\" ss:Pattern=\"Solid\"/>\n")
        xml.append("  </Style>\n")
        xml.append(" </Styles>\n")

        // Worksheet
        xml.append(" <Worksheet ss:Name=\"Financial Report\">\n")
        xml.append("  <Table>\n")
        xml.append("   <Column ss:Width=\"120\"/>\n") // Date
        xml.append("   <Column ss:Width=\"150\"/>\n") // Title
        xml.append("   <Column ss:Width=\"80\"/>\n")  // Category
        xml.append("   <Column ss:Width=\"80\"/>\n")  // Account
        xml.append("   <Column ss:Width=\"60\"/>\n")  // Type
        xml.append("   <Column ss:Width=\"100\"/>\n") // Amount

        // Header Row
        xml.append("   <Row ss:Height=\"20\">\n")
        listOf("Date", "Title", "Category", "Account", "Type", "Amount").forEach {
            xml.append("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">$it</Data></Cell>\n")
        }
        xml.append("   </Row>\n")

        // Data Rows
        transactions.forEach { tx ->
            val style = if (tx.type.equals("Income", true)) "Income" else "Expense"
            xml.append("   <Row>\n")
            xml.append("    <Cell><Data ss:Type=\"String\">${displaySdf.format(Date(tx.date))}</Data></Cell>\n")
            xml.append("    <Cell><Data ss:Type=\"String\">${escapeXml(tx.title)}</Data></Cell>\n")
            xml.append("    <Cell><Data ss:Type=\"String\">${escapeXml(tx.category)}</Data></Cell>\n")
            xml.append("    <Cell><Data ss:Type=\"String\">${escapeXml(tx.account)}</Data></Cell>\n")
            xml.append("    <Cell ss:StyleID=\"$style\"><Data ss:Type=\"String\">${tx.type}</Data></Cell>\n")
            xml.append("    <Cell ss:StyleID=\"Currency\"><Data ss:Type=\"Number\">${tx.amount}</Data></Cell>\n")
            xml.append("   </Row>\n")
        }

        // Summary Rows
        xml.append("   <Row/>\n") // Empty spacer
        xml.append("   <Row>\n")
        xml.append("    <Cell ss:Index=\"5\" ss:StyleID=\"Summary\"><Data ss:Type=\"String\">Total Income:</Data></Cell>\n")
        xml.append("    <Cell ss:StyleID=\"Currency\"><Data ss:Type=\"Number\">$totalIncome</Data></Cell>\n")
        xml.append("   </Row>\n")
        xml.append("   <Row>\n")
        xml.append("    <Cell ss:Index=\"5\" ss:StyleID=\"Summary\"><Data ss:Type=\"String\">Total Expense:</Data></Cell>\n")
        xml.append("    <Cell ss:StyleID=\"Currency\"><Data ss:Type=\"Number\">$totalExpense</Data></Cell>\n")
        xml.append("   </Row>\n")
        xml.append("   <Row>\n")
        xml.append("    <Cell ss:Index=\"5\" ss:StyleID=\"Summary\"><Data ss:Type=\"String\">Net Balance:</Data></Cell>\n")
        xml.append("    <Cell ss:StyleID=\"Currency\"><Data ss:Type=\"Number\">${totalIncome - totalExpense}</Data></Cell>\n")
        xml.append("   </Row>\n")

        xml.append("  </Table>\n")
        xml.append(" </Worksheet>\n")
        xml.append("</Workbook>")

        // Save file
        val fileName = "ExpenseFlow_Report_${System.currentTimeMillis()}.xls"
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.ms-excel")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri == null) {
            Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show()
            return
        }

        resolver.openOutputStream(uri)?.use { outputStream ->
            val writer = OutputStreamWriter(outputStream)
            writer.write(xml.toString())
            writer.flush()
            writer.close()
        }
        Toast.makeText(context, "Excel Report Saved! 📂", Toast.LENGTH_LONG).show()

    } catch (e: Exception) {
        Toast.makeText(context, "Excel Export failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun escapeXml(str: String): String {
    return str.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
