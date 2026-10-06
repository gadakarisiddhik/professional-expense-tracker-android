package com.example.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Money utilities using Long minor units (e.g., 25050L for 250.50).
 * Completely immune to floating point drift and strictly deterministic.
 */
object MoneyUtils {

    /**
     * Converts a string user input (e.g. "250", "250.50", "1,200.75") to Long minor units.
     * Returns 0L if invalid or empty.
     */
    fun parseToMinorUnits(input: String): Long {
        val clean = input.trim().replace(",", "").replace(" ", "").replace("₹", "").replace("$", "").replace("€", "").replace("£", "")
        if (clean.isBlank()) return 0L
        return try {
            val bd = BigDecimal(clean)
            bd.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Converts minor units to a plain decimal string for text input fields (e.g. 25050L -> "250.50").
     */
    fun toDecimalString(minorUnits: Long): String {
        val absVal = kotlin.math.abs(minorUnits)
        val whole = absVal / 100
        val fraction = absVal % 100
        return if (fraction == 0L) {
            "$whole"
        } else {
            String.format(Locale.US, "%d.%02d", whole, fraction)
        }
    }

    /**
     * Formats minor units with currency symbol (e.g. ₹24,580.00 or ₹24,580).
     */
    fun formatCurrency(
        minorUnits: Long,
        currencySymbol: String = "₹",
        includeDecimals: Boolean = true
    ): String {
        val isNegative = minorUnits < 0
        val absVal = kotlin.math.abs(minorUnits)
        val whole = absVal / 100
        val fraction = absVal % 100

        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        val formatter = DecimalFormat("#,##0", symbols)
        val formattedWhole = formatter.format(whole)

        val text = if (includeDecimals && fraction > 0) {
            String.format(Locale.US, "%s%s.%02d", currencySymbol, formattedWhole, fraction)
        } else {
            "$currencySymbol$formattedWhole"
        }

        return if (isNegative) "-$text" else text
    }

    /**
     * Formats with explicit sign (+₹50,000 or -₹350)
     */
    fun formatWithSign(
        minorUnits: Long,
        currencySymbol: String = "₹",
        isIncome: Boolean
    ): String {
        val formatted = formatCurrency(kotlin.math.abs(minorUnits), currencySymbol, includeDecimals = true)
        return if (isIncome) "+$formatted" else "-$formatted"
    }

    /**
     * Calculates percentage utilized: (spent / total) * 100 safely.
     */
    fun calculatePercentage(spent: Long, total: Long): Int {
        if (total <= 0L) return 0
        val pct = (spent.toDouble() / total.toDouble()) * 100.0
        return pct.toInt().coerceAtLeast(0)
    }
}
