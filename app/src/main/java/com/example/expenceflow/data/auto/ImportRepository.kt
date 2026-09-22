package com.example.expenceflow.data.auto

import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportRepository @Inject constructor(
    private val csvParser: CsvStatementParser,
    private val excelParser: ExcelStatementParser,
    private val pdfParser: PdfStatementParser,
    private val extractionEngine: ExtractionEngine
) {
    suspend fun importCsv(inputStream: InputStream): List<TransactionCandidate> {
        val raw = csvParser.parse(inputStream)
        return raw.mapNotNull { extractionEngine.processCandidate(it) }
    }

    suspend fun importExcel(inputStream: InputStream): List<TransactionCandidate> {
        val raw = excelParser.parse(inputStream)
        return raw.mapNotNull { extractionEngine.processCandidate(it) }
    }

    suspend fun importPdf(inputStream: InputStream): List<TransactionCandidate> {
        val raw = pdfParser.extractTransactions(inputStream)
        return raw.mapNotNull { extractionEngine.processCandidate(it) }
    }
}
