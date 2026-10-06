package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Long = 1L,
    val name: String = "User",
    val currencySymbol: String = "₹",
    val currencyCode: String = "INR",
    val isOnboarded: Boolean = false,
    val appLockEnabled: Boolean = false,
    val pinHash: String = "",
    val biometricEnabled: Boolean = false,
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val budgetAlertsEnabled: Boolean = true,
    val billRemindersEnabled: Boolean = true
)

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["name"])]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: String, // CASH, BANK, UPI, DEBIT_CARD, CREDIT_CARD, SAVINGS
    val openingBalance: Long = 0L,
    val currentBalance: Long = 0L,
    val icon: String = "AccountBalance",
    val color: String = "#0284C7",
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name", "type"])]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: String, // EXPENSE, INCOME
    val icon: String = "Category",
    val color: String = "#10B981",
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["dateTime"]),
        Index(value = ["type"]),
        Index(value = ["categoryId"]),
        Index(value = ["accountId"]),
        Index(value = ["toAccountId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: String, // EXPENSE, INCOME, TRANSFER
    val amount: Long, // Minor units (e.g., 25050L for 250.50)
    val categoryId: Long? = null,
    val accountId: Long,
    val toAccountId: Long? = null, // Used only when type is TRANSFER
    val dateTime: Long = System.currentTimeMillis(),
    val note: String = "",
    val merchant: String = "",
    val location: String = "",
    val receiptUri: String? = null,
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["categoryId"])]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val categoryId: Long? = null, // null indicates overall monthly budget
    val limitAmount: Long, // Minor units
    val period: String = "MONTHLY", // MONTHLY, WEEKLY, YEARLY
    val startDate: Long = 0L,
    val endDate: Long = 0L
)

@Entity(
    tableName = "recurring_transactions",
    indices = [Index(value = ["nextDueDate"]), Index(value = ["isActive"])]
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val amount: Long,
    val type: String = "EXPENSE", // EXPENSE, INCOME
    val categoryId: Long,
    val accountId: Long,
    val frequency: String = "MONTHLY", // DAILY, WEEKLY, MONTHLY, YEARLY
    val startDate: Long = System.currentTimeMillis(),
    val nextDueDate: Long = System.currentTimeMillis(),
    val lastProcessedDate: Long? = null,
    val isActive: Boolean = true,
    val note: String = ""
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val targetAmount: Long,
    val savedAmount: Long = 0L,
    val targetDate: Long = 0L,
    val icon: String = "Savings",
    val color: String = "#8B5CF6",
    val isCompleted: Boolean = false,
    val notes: String = ""
)

@Entity(
    tableName = "credit_cards",
    indices = [Index(value = ["accountId"], unique = true)]
)
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val accountId: Long,
    val cardName: String,
    val bank: String,
    val creditLimit: Long,
    val statementDay: Int = 15,
    val dueDay: Int = 5,
    val minDuePercentage: Double = 5.0
)

@Entity(
    tableName = "bill_reminders",
    indices = [Index(value = ["dueDate"]), Index(value = ["isPaid"])]
)
data class BillReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val amount: Long,
    val dueDate: Long,
    val category: String = "Bills",
    val repeatRule: String = "MONTHLY", // NONE, WEEKLY, MONTHLY, YEARLY
    val isPaid: Boolean = false,
    val isEnabled: Boolean = true,
    val notes: String = ""
)
