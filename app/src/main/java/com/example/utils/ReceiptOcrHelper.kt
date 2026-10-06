package com.example.utils

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

data class ParsedReceipt(
    val merchant: String,
    val amountMinorUnits: Long,
    val dateTimestamp: Long,
    val suggestedCategory: String,
    val rawText: String = ""
)

object ReceiptOcrHelper {

    private val amountPatterns = listOf(
        Pattern.compile("""(?i)(?:total|grand\s*total|net\s*amount|amount\s*paid|subtotal|due|bal)\s*[:=-]?\s*(?:rs\.?|inr|₹|\$)?\s*([0-9,]+(?:\.[0-9]{2})?)"""),
        Pattern.compile("""(?:rs\.?|inr|₹|\$)\s*([0-9,]+(?:\.[0-9]{2})?)"""),
        Pattern.compile("""\b([0-9,]+\.[0-9]{2})\b""")
    )

    private val datePatterns = listOf(
        SimpleDateFormat("dd/MM/yyyy", Locale.US),
        SimpleDateFormat("dd-MM-yyyy", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("dd MMM yyyy", Locale.US),
        SimpleDateFormat("MM/dd/yyyy", Locale.US)
    )

    private val merchantKeywords = mapOf(
        "supermarket" to "Food",
        "grocery" to "Food",
        "dmart" to "Food",
        "mart" to "Shopping",
        "restaurant" to "Food",
        "cafe" to "Food",
        "coffee" to "Food",
        "uber" to "Transport",
        "ola" to "Transport",
        "petrol" to "Transport",
        "fuel" to "Transport",
        "pharmacy" to "Health",
        "hospital" to "Health",
        "apollo" to "Health",
        "cinema" to "Entertainment",
        "movie" to "Entertainment",
        "netflix" to "Entertainment",
        "amazon" to "Shopping",
        "flipkart" to "Shopping",
        "zara" to "Shopping",
        "h&m" to "Shopping",
        "electricity" to "Bills",
        "broadband" to "Bills",
        "airtel" to "Bills",
        "jio" to "Bills"
    )

    /**
     * Parses unstructured OCR text from receipt into merchant, amount, date, and suggested category.
     */
    fun parseReceiptText(text: String): ParsedReceipt {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        var merchant = ""
        var maxAmount = 0L
        var date = System.currentTimeMillis()
        var suggestedCategory = "Shopping"

        // 1. Identify Merchant (usually the first 1-3 prominent non-number lines)
        for (line in lines.take(5)) {
            val hasLetters = line.any { it.isLetter() }
            val hasFewDigits = line.count { it.isDigit() } < line.length / 2
            if (hasLetters && hasFewDigits && !line.contains("tax", ignoreCase = true) && !line.contains("receipt", ignoreCase = true)) {
                merchant = line
                break
            }
        }
        if (merchant.isBlank() && lines.isNotEmpty()) {
            merchant = lines.first()
        }

        // 2. Identify Amount
        for (line in lines) {
            for (pattern in amountPatterns) {
                val matcher = pattern.matcher(line)
                while (matcher.find()) {
                    val matchStr = matcher.group(1) ?: continue
                    val minorUnits = MoneyUtils.parseToMinorUnits(matchStr)
                    if (minorUnits > maxAmount) {
                        maxAmount = minorUnits
                    }
                }
            }
        }

        // 3. Identify Date
        val dateRegex = Pattern.compile("""(\d{1,4}[-/\.]\d{1,2}[-/\.]\d{1,4}|\d{1,2}\s+[A-Za-z]{3}\s+\d{2,4})""")
        for (line in lines) {
            val matcher = dateRegex.matcher(line)
            if (matcher.find()) {
                val matchStr = matcher.group(1) ?: continue
                for (fmt in datePatterns) {
                    try {
                        val parsed = fmt.parse(matchStr)
                        if (parsed != null) {
                            date = parsed.time
                            break
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // 4. Suggest Category based on merchant or text content
        val fullLower = text.lowercase(Locale.getDefault())
        for ((keyword, cat) in merchantKeywords) {
            if (fullLower.contains(keyword)) {
                suggestedCategory = cat
                break
            }
        }

        return ParsedReceipt(
            merchant = if (merchant.isNotBlank()) merchant else "Receipt Purchase",
            amountMinorUnits = maxAmount,
            dateTimestamp = date,
            suggestedCategory = suggestedCategory,
            rawText = text
        )
    }

    /**
     * Smart quick add natural-language parser.
     * Examples:
     * "250 food UPI" -> amount = 25000L, category = "Food", account = "UPI"
     * "450 dinner cash" -> amount = 45000L, category = "Food", account = "Cash"
     * "₹1,200 shopping sbi" -> amount = 120000L, category = "Shopping", account = "SBI Bank"
     */
    fun parseQuickAdd(
        input: String,
        availableCategories: List<String>,
        availableAccounts: List<String>
    ): QuickAddResult {
        val tokens = input.trim().split("\\s+".toRegex())
        var parsedAmount: Long? = null
        var matchedCategory: String? = null
        var matchedAccount: String? = null
        val remainingWords = mutableListOf<String>()

        for (token in tokens) {
            val cleanToken = token.replace("₹", "").replace("$", "").replace(",", "")
            if (parsedAmount == null && cleanToken.matches("\\d+(\\.\\d+)?".toRegex())) {
                parsedAmount = MoneyUtils.parseToMinorUnits(cleanToken)
                continue
            }

            val catMatch = availableCategories.firstOrNull { it.equals(token, ignoreCase = true) || it.contains(token, ignoreCase = true) }
            if (catMatch != null && matchedCategory == null) {
                matchedCategory = catMatch
                continue
            }

            val accMatch = availableAccounts.firstOrNull { it.equals(token, ignoreCase = true) || it.contains(token, ignoreCase = true) }
            if (accMatch != null && matchedAccount == null) {
                matchedAccount = accMatch
                continue
            }

            remainingWords.add(token)
        }

        return QuickAddResult(
            amount = parsedAmount ?: 0L,
            categoryName = matchedCategory,
            accountName = matchedAccount,
            note = remainingWords.joinToString(" "),
            isAmbiguous = parsedAmount == null || matchedCategory == null || matchedAccount == null
        )
    }
}

data class QuickAddResult(
    val amount: Long,
    val categoryName: String?,
    val accountName: String?,
    val note: String,
    val isAmbiguous: Boolean
)
