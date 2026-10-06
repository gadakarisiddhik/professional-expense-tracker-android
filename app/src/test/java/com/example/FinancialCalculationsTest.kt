package com.example

import com.example.utils.MoneyUtils
import com.example.utils.ReceiptOcrHelper
import com.example.utils.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialCalculationsTest {

    @Test
    fun testMoneyMinorUnitsConversion() {
        // ₹250.50 -> 25050L minor units
        val minor = MoneyUtils.parseToMinorUnits("250.50")
        assertEquals(25050L, minor)

        // ₹250 -> 25000L
        assertEquals(25000L, MoneyUtils.parseToMinorUnits("250"))

        // ₹1,248.00 -> 124800L
        assertEquals(124800L, MoneyUtils.parseToMinorUnits("1,248.00"))

        // Round-trip back to decimal string
        assertEquals("250.50", MoneyUtils.toDecimalString(25050L))
        assertEquals("250", MoneyUtils.toDecimalString(25000L))
    }

    @Test
    fun testCurrencyFormatting() {
        val formatted = MoneyUtils.formatCurrency(2458000L, "₹")
        assertEquals("₹24,580", formatted)

        val formattedDecimals = MoneyUtils.formatCurrency(25050L, "₹")
        assertEquals("₹250.50", formattedDecimals)

        val negative = MoneyUtils.formatCurrency(-35000L, "₹")
        assertEquals("-₹350", negative)
    }

    @Test
    fun testBudgetPercentageCalculation() {
        // Spent 21,400 out of 30,000 -> 71%
        val spent = 2140000L
        val limit = 3000000L
        val pct = MoneyUtils.calculatePercentage(spent, limit)
        assertEquals(71, pct)

        // 80% threshold
        val warningSpent = 2400000L
        assertEquals(80, MoneyUtils.calculatePercentage(warningSpent, limit))

        // 100% threshold
        val exceededSpent = 3100000L
        assertTrue(MoneyUtils.calculatePercentage(exceededSpent, limit) > 100)
    }

    @Test
    fun testSavingsCalculation() {
        // Income 45,000, Expense 20,420 -> Savings 24,580
        val income = 4500000L
        val expense = 2042000L
        val savings = (income - expense).coerceAtLeast(0L)
        assertEquals(2458000L, savings)
    }

    @Test
    fun testTransferBalanceCalculation() {
        var sbiBalance = 2500000L // ₹25,000
        var cashBalance = 500000L // ₹5,000
        val transferAmount = 500000L // ₹5,000

        // Execute Transfer
        sbiBalance -= transferAmount
        cashBalance += transferAmount

        assertEquals(2000000L, sbiBalance) // ₹20,000
        assertEquals(1000000L, cashBalance) // ₹10,000
    }

    @Test
    fun testEditTransactionBalanceReversal() {
        var accountBalance = 1000000L // ₹10,000
        val originalExpense = 50000L  // Food ₹500
        accountBalance -= originalExpense
        assertEquals(950000L, accountBalance)

        // User edits transaction from ₹500 to ₹300
        val newExpense = 30000L
        // 1. Reverse old
        accountBalance += originalExpense
        // 2. Apply new
        accountBalance -= newExpense
        assertEquals(970000L, accountBalance) // Balance difference is exactly ₹200
    }

    @Test
    fun testQuickAddParser() {
        val categories = listOf("Food", "Transport", "Shopping", "Bills")
        val accounts = listOf("Cash", "SBI Bank", "UPI")

        val result = ReceiptOcrHelper.parseQuickAdd("250 Food UPI", categories, accounts)
        assertEquals(25000L, result.amount)
        assertEquals("Food", result.categoryName)
        assertEquals("UPI", result.accountName)
        assertFalse(result.isAmbiguous)
    }

    @Test
    fun testReceiptOcrHeuristics() {
        val sampleReceipt = """
            DMart
            Date: 05/10/2026
            Item 1: Milk 50
            Total: ₹1,248.00
        """.trimIndent()

        val parsed = ReceiptOcrHelper.parseReceiptText(sampleReceipt)
        assertEquals("DMart", parsed.merchant)
        assertEquals(124800L, parsed.amountMinorUnits)
        assertEquals("Food", parsed.suggestedCategory)
    }

    @Test
    fun testSecurityPinHashing() {
        val pin = "1234"
        val hash = SecurityUtils.hashPin(pin)
        assertTrue(SecurityUtils.verifyPin(pin, hash))
        assertFalse(SecurityUtils.verifyPin("9999", hash))
    }
}
