package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import java.io.File
import java.io.FileOutputStream

object ExportHelper {

    /**
     * Exports transaction list to CSV format and opens Android Sharesheet.
     */
    fun exportToCsv(
        context: Context,
        transactions: List<TransactionItem>,
        currencySymbol: String = "₹"
    ): File {
        val fileName = "expense_tracker_export_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        val csvHeader = "ID,Date,Time,Type,Amount ($currencySymbol),Category,Account,To Account,Merchant,Note,Tags\n"
        val sb = StringBuilder(csvHeader)

        for (tx in transactions) {
            val dateStr = DateUtils.formatDate(tx.dateTime)
            val timeStr = DateUtils.formatTime(tx.dateTime)
            val typeStr = tx.type.name
            val amountStr = MoneyUtils.toDecimalString(tx.amount)
            val categoryStr = escapeCsv(tx.categoryName)
            val accountStr = escapeCsv(tx.accountName)
            val toAccountStr = escapeCsv(tx.toAccountName ?: "")
            val merchantStr = escapeCsv(tx.merchant)
            val noteStr = escapeCsv(tx.note)
            val tagsStr = escapeCsv(tx.tags)

            sb.append("${tx.id},\"$dateStr\",\"$timeStr\",$typeStr,$amountStr,\"$categoryStr\",\"$accountStr\",\"$toAccountStr\",\"$merchantStr\",\"$noteStr\",\"$tagsStr\"\n")
        }

        FileOutputStream(file).use { it.write(sb.toString().toByteArray()) }
        return file
    }

    /**
     * Generates a clean, professional PDF financial report and opens Android Sharesheet.
     */
    fun exportToPdf(
        context: Context,
        transactions: List<TransactionItem>,
        totalIncome: Long,
        totalExpense: Long,
        currencySymbol: String = "₹",
        reportTitle: String = "Financial Statement"
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 standard points
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42) // Slate Navy
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 10f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val regularPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 10f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        val headerBgPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
        }

        var y = 45f

        // Document Header
        canvas.drawText("PROFESSIONAL EXPENSE TRACKER", 40f, y, titlePaint)
        y += 16f
        canvas.drawText(reportTitle + " • Generated on " + DateUtils.formatDate(System.currentTimeMillis()), 40f, y, subPaint)
        y += 24f

        // Financial Summary Box
        val netSavings = (totalIncome - totalExpense).coerceAtLeast(0L)
        canvas.drawRect(40f, y, 555f, y + 54f, headerBgPaint)

        canvas.drawText("Total Income", 55f, y + 20f, subPaint)
        canvas.drawText(MoneyUtils.formatCurrency(totalIncome, currencySymbol), 55f, y + 40f, boldPaint)

        canvas.drawText("Total Expenses", 220f, y + 20f, subPaint)
        canvas.drawText(MoneyUtils.formatCurrency(totalExpense, currencySymbol), 220f, y + 40f, boldPaint)

        canvas.drawText("Net Savings", 390f, y + 20f, subPaint)
        canvas.drawText(MoneyUtils.formatCurrency(netSavings, currencySymbol), 390f, y + 40f, boldPaint)

        y += 74f

        // Transactions Table Header
        canvas.drawText("RECENT TRANSACTIONS", 40f, y, boldPaint)
        y += 14f

        canvas.drawRect(40f, y, 555f, y + 22f, headerBgPaint)
        canvas.drawText("Date", 50f, y + 15f, boldPaint)
        canvas.drawText("Category / Description", 130f, y + 15f, boldPaint)
        canvas.drawText("Account", 330f, y + 15f, boldPaint)
        canvas.drawText("Type", 430f, y + 15f, boldPaint)
        canvas.drawText("Amount", 490f, y + 15f, boldPaint)
        y += 25f

        // Table Rows (up to 30 rows on single A4 summary)
        val displayItems = transactions.take(30)
        for (item in displayItems) {
            val dateText = DateUtils.formatDate(item.dateTime)
            val desc = if (item.note.isNotBlank()) item.note else item.categoryName
            val displayDesc = if (desc.length > 25) desc.substring(0, 22) + "..." else desc
            val amountFormatted = MoneyUtils.formatCurrency(item.amount, currencySymbol)

            canvas.drawText(dateText, 50f, y + 12f, regularPaint)
            canvas.drawText(displayDesc, 130f, y + 12f, regularPaint)
            canvas.drawText(item.accountName, 330f, y + 12f, regularPaint)
            canvas.drawText(item.type.name, 430f, y + 12f, regularPaint)
            canvas.drawText(amountFormatted, 490f, y + 12f, boldPaint)

            canvas.drawLine(40f, y + 18f, 555f, y + 18f, linePaint)
            y += 20f
        }

        // Footer
        canvas.drawText("Confidential • Generated by Professional Expense Tracker Android App", 40f, 810f, subPaint)

        pdfDocument.finishPage(page)

        val fileName = "expense_tracker_report_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { pdfDocument.writeTo(it) }
        pdfDocument.close()
        return file
    }

    /**
     * Shares any file using the Android Sharesheet.
     */
    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String = "Share Financial Export") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
        } catch (_: Exception) {
            // Fallback without file provider if needed
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_TEXT, "Export file ready at ${file.name}")
            }
            context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
        }
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }
}
