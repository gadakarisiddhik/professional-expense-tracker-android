package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.BillReminderDao
import com.example.data.local.dao.BudgetDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.CreditCardDao
import com.example.data.local.dao.RecurringTransactionDao
import com.example.data.local.dao.SavingsGoalDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.dao.UserProfileDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BillReminderEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CreditCardEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfileEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        SavingsGoalEntity::class,
        CreditCardEntity::class,
        BillReminderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun billReminderDao(): BillReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDefaultData(database)
                }
            }
        }

        private suspend fun populateDefaultData(database: AppDatabase) {
            // Default Profile
            database.userProfileDao().insertOrUpdate(
                UserProfileEntity(
                    id = 1L,
                    name = "User",
                    currencySymbol = "₹",
                    currencyCode = "INR",
                    isOnboarded = false,
                    appLockEnabled = false,
                    pinHash = "",
                    themeMode = "SYSTEM"
                )
            )

            // Default Accounts
            val defaultAccounts = listOf(
                AccountEntity(
                    id = 1L,
                    name = "Cash",
                    type = "CASH",
                    openingBalance = 500000L, // ₹5,000.00
                    currentBalance = 500000L,
                    icon = "Payments",
                    color = "#10B981",
                    sortOrder = 1
                ),
                AccountEntity(
                    id = 2L,
                    name = "SBI Bank",
                    type = "BANK",
                    openingBalance = 2500000L, // ₹25,000.00
                    currentBalance = 2500000L,
                    icon = "AccountBalance",
                    color = "#3B82F6",
                    sortOrder = 2
                ),
                AccountEntity(
                    id = 3L,
                    name = "UPI Wallet",
                    type = "UPI",
                    openingBalance = 200000L, // ₹2,000.00
                    currentBalance = 200000L,
                    icon = "QrCode",
                    color = "#8B5CF6",
                    sortOrder = 3
                ),
                AccountEntity(
                    id = 4L,
                    name = "Debit Card",
                    type = "DEBIT_CARD",
                    openingBalance = 0L,
                    currentBalance = 0L,
                    icon = "CreditCard",
                    color = "#0284C7",
                    sortOrder = 4
                ),
                AccountEntity(
                    id = 5L,
                    name = "Credit Card",
                    type = "CREDIT_CARD",
                    openingBalance = 0L,
                    currentBalance = 0L,
                    icon = "CreditScore",
                    color = "#F43F5E",
                    sortOrder = 5
                )
            )
            database.accountDao().insertAccounts(defaultAccounts)

            // Credit card details for account 5
            database.creditCardDao().insertCard(
                CreditCardEntity(
                    id = 1L,
                    accountId = 5L,
                    cardName = "HDFC Credit Card",
                    bank = "HDFC",
                    creditLimit = 5000000L, // ₹50,000 limit
                    statementDay = 15,
                    dueDay = 5,
                    minDuePercentage = 5.0
                )
            )

            // Default Expense Categories
            val defaultExpenseCategories = listOf(
                CategoryEntity(name = "Food", type = "EXPENSE", icon = "Restaurant", color = "#F97316", sortOrder = 1),
                CategoryEntity(name = "Transport", type = "EXPENSE", icon = "DirectionsCar", color = "#3B82F6", sortOrder = 2),
                CategoryEntity(name = "Shopping", type = "EXPENSE", icon = "ShoppingCart", color = "#EC4899", sortOrder = 3),
                CategoryEntity(name = "Bills", type = "EXPENSE", icon = "Receipt", color = "#EAB308", sortOrder = 4),
                CategoryEntity(name = "Rent", type = "EXPENSE", icon = "Home", color = "#6366F1", sortOrder = 5),
                CategoryEntity(name = "Entertainment", type = "EXPENSE", icon = "Movie", color = "#A855F7", sortOrder = 6),
                CategoryEntity(name = "Health", type = "EXPENSE", icon = "LocalHospital", color = "#EF4444", sortOrder = 7),
                CategoryEntity(name = "Education", type = "EXPENSE", icon = "School", color = "#14B8A6", sortOrder = 8),
                CategoryEntity(name = "Travel", type = "EXPENSE", icon = "Flight", color = "#06B6D4", sortOrder = 9),
                CategoryEntity(name = "Personal", type = "EXPENSE", icon = "Person", color = "#84CC16", sortOrder = 10),
                CategoryEntity(name = "Work", type = "EXPENSE", icon = "Work", color = "#64748B", sortOrder = 11),
                CategoryEntity(name = "Other", type = "EXPENSE", icon = "Category", color = "#94A3B8", sortOrder = 12)
            )
            database.categoryDao().insertCategories(defaultExpenseCategories)

            // Default Income Categories
            val defaultIncomeCategories = listOf(
                CategoryEntity(name = "Salary", type = "INCOME", icon = "Work", color = "#10B981", sortOrder = 1),
                CategoryEntity(name = "Freelance", type = "INCOME", icon = "Laptop", color = "#059669", sortOrder = 2),
                CategoryEntity(name = "Business", type = "INCOME", icon = "Store", color = "#047857", sortOrder = 3),
                CategoryEntity(name = "Bonus", type = "INCOME", icon = "CardGiftcard", color = "#0D9488", sortOrder = 4),
                CategoryEntity(name = "Investment", type = "INCOME", icon = "TrendingUp", color = "#065F46", sortOrder = 5),
                CategoryEntity(name = "Gift", type = "INCOME", icon = "Redeem", color = "#10B981", sortOrder = 6),
                CategoryEntity(name = "Refund", type = "INCOME", icon = "CurrencyExchange", color = "#34D399", sortOrder = 7),
                CategoryEntity(name = "Other", type = "INCOME", icon = "AttachMoney", color = "#6EE7B7", sortOrder = 8)
            )
            database.categoryDao().insertCategories(defaultIncomeCategories)

            // Default Budget
            database.budgetDao().insertBudget(
                BudgetEntity(
                    id = 1L,
                    name = "Monthly Overall Budget",
                    categoryId = null,
                    limitAmount = 3000000L, // ₹30,000.00
                    period = "MONTHLY",
                    startDate = 0L,
                    endDate = 0L
                )
            )
        }
    }
}
