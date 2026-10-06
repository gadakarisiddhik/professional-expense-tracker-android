package com.example.domain.model

data class UserProfile(
    val id: Long = 1L,
    val name: String = "User",
    val currencySymbol: String = "₹",
    val currencyCode: String = "INR",
    val isOnboarded: Boolean = false,
    val appLockEnabled: Boolean = false,
    val pinHash: String = "",
    val biometricEnabled: Boolean = false,
    val themeMode: String = "SYSTEM",
    val budgetAlertsEnabled: Boolean = true,
    val billRemindersEnabled: Boolean = true
)

enum class TransactionType {
    EXPENSE, INCOME, TRANSFER
}

data class Account(
    val id: Long = 0L,
    val name: String,
    val type: String, // CASH, BANK, UPI, DEBIT_CARD, CREDIT_CARD, SAVINGS
    val openingBalance: Long = 0L,
    val currentBalance: Long = 0L,
    val icon: String = "AccountBalance",
    val color: String = "#0284C7",
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

data class Category(
    val id: Long = 0L,
    val name: String,
    val type: String, // EXPENSE, INCOME
    val icon: String = "Category",
    val color: String = "#10B981",
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

data class TransactionItem(
    val id: Long = 0L,
    val type: TransactionType,
    val amount: Long, // Minor units
    val categoryId: Long? = null,
    val categoryName: String = "General",
    val categoryIcon: String = "Category",
    val categoryColor: String = "#64748B",
    val accountId: Long,
    val accountName: String = "Account",
    val accountIcon: String = "AccountBalance",
    val toAccountId: Long? = null,
    val toAccountName: String? = null,
    val dateTime: Long = System.currentTimeMillis(),
    val note: String = "",
    val merchant: String = "",
    val location: String = "",
    val receiptUri: String? = null,
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class BudgetWithStatus(
    val id: Long = 0L,
    val name: String,
    val categoryId: Long? = null,
    val categoryName: String? = null,
    val categoryIcon: String? = null,
    val categoryColor: String? = null,
    val limitAmount: Long, // Minor units
    val spentAmount: Long, // Minor units
    val remainingAmount: Long,
    val percentage: Int, // 0..100+
    val period: String = "MONTHLY",
    val isWarning: Boolean = percentage >= 80,
    val isExceeded: Boolean = percentage >= 100
)

data class RecurringTransactionItem(
    val id: Long = 0L,
    val name: String,
    val amount: Long,
    val type: TransactionType,
    val categoryId: Long,
    val categoryName: String = "General",
    val accountId: Long,
    val accountName: String = "Account",
    val frequency: String = "MONTHLY",
    val startDate: Long,
    val nextDueDate: Long,
    val lastProcessedDate: Long? = null,
    val isActive: Boolean = true,
    val note: String = ""
)

data class SavingsGoalItem(
    val id: Long = 0L,
    val name: String,
    val targetAmount: Long,
    val savedAmount: Long,
    val targetDate: Long,
    val icon: String = "Savings",
    val color: String = "#8B5CF6",
    val isCompleted: Boolean = false,
    val notes: String = "",
    val progressPercentage: Int = if (targetAmount > 0) ((savedAmount.toDouble() / targetAmount.toDouble()) * 100).toInt().coerceIn(0, 100) else 0,
    val remainingAmount: Long = (targetAmount - savedAmount).coerceAtLeast(0L)
)

data class CreditCardItem(
    val id: Long = 0L,
    val accountId: Long,
    val cardName: String,
    val bank: String,
    val creditLimit: Long,
    val currentOutstanding: Long, // e.g. abs(balance) if negative or positive used
    val availableLimit: Long,
    val statementDay: Int,
    val dueDay: Int,
    val minDuePercentage: Double = 5.0,
    val minimumDue: Long = (currentOutstanding * minDuePercentage / 100.0).toLong()
)

data class BillReminderItem(
    val id: Long = 0L,
    val title: String,
    val amount: Long,
    val dueDate: Long,
    val category: String = "Bills",
    val repeatRule: String = "MONTHLY",
    val isPaid: Boolean = false,
    val isEnabled: Boolean = true,
    val notes: String = ""
)

data class DashboardSummary(
    val availableBalance: Long = 0L,
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val estimatedSavings: Long = 0L,
    val todaySpending: Long = 0L,
    val overallBudget: BudgetWithStatus? = null,
    val recentTransactions: List<TransactionItem> = emptyList(),
    val todayTransactions: List<TransactionItem> = emptyList(),
    val activeGoal: SavingsGoalItem? = null,
    val upcomingBills: List<BillReminderItem> = emptyList(),
    val upcomingRecurring: List<RecurringTransactionItem> = emptyList(),
    val spendingInsights: List<FinancialInsight> = emptyList()
)

data class FinancialInsight(
    val title: String,
    val message: String,
    val type: InsightType,
    val icon: String = "Insights"
)

enum class InsightType {
    INFO, WARNING, SUCCESS, TIP
}

data class CategorySpend(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val amount: Long,
    val percentage: Float
)
