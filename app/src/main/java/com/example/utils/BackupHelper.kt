package com.example.utils

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BillReminderEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CreditCardEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class BackupValidationResult(
    val isValid: Boolean,
    val transactionCount: Int = 0,
    val accountCount: Int = 0,
    val categoryCount: Int = 0,
    val backupDate: Long = 0L,
    val rawJson: String = "",
    val errorMessage: String? = null
)

object BackupHelper {

    /**
     * Creates a full JSON backup file containing all accounts, categories, transactions, budgets, goals, etc.
     */
    suspend fun createBackup(context: Context, database: AppDatabase): File = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        root.put("appName", "Professional Expense Tracker")

        // 1. Profile
        val profile = database.userProfileDao().getUserProfile()
        if (profile != null) {
            val pObj = JSONObject().apply {
                put("name", profile.name)
                put("currencySymbol", profile.currencySymbol)
                put("currencyCode", profile.currencyCode)
                put("themeMode", profile.themeMode)
            }
            root.put("profile", pObj)
        }

        // 2. Accounts
        val accounts = database.accountDao().getAllAccountsFlow().first()
        val accArray = JSONArray()
        for (a in accounts) {
            val o = JSONObject().apply {
                put("id", a.id)
                put("name", a.name)
                put("type", a.type)
                put("openingBalance", a.openingBalance)
                put("currentBalance", a.currentBalance)
                put("icon", a.icon)
                put("color", a.color)
                put("isActive", a.isActive)
                put("sortOrder", a.sortOrder)
            }
            accArray.put(o)
        }
        root.put("accounts", accArray)

        // 3. Categories
        val categories = database.categoryDao().getAllCategoriesFlow().first()
        val catArray = JSONArray()
        for (c in categories) {
            val o = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("type", c.type)
                put("icon", c.icon)
                put("color", c.color)
                put("isActive", c.isActive)
                put("sortOrder", c.sortOrder)
            }
            catArray.put(o)
        }
        root.put("categories", catArray)

        // 4. Transactions
        val transactions = database.transactionDao().getAllTransactionsFlow().first()
        val txArray = JSONArray()
        for (t in transactions) {
            val o = JSONObject().apply {
                put("id", t.id)
                put("type", t.type)
                put("amount", t.amount)
                put("categoryId", t.categoryId ?: JSONObject.NULL)
                put("accountId", t.accountId)
                put("toAccountId", t.toAccountId ?: JSONObject.NULL)
                put("dateTime", t.dateTime)
                put("note", t.note)
                put("merchant", t.merchant)
                put("location", t.location)
                put("receiptUri", t.receiptUri ?: JSONObject.NULL)
                put("tags", t.tags)
            }
            txArray.put(o)
        }
        root.put("transactions", txArray)

        // 5. Budgets
        val budgets = database.budgetDao().getAllBudgetsFlow().first()
        val bArray = JSONArray()
        for (b in budgets) {
            val o = JSONObject().apply {
                put("id", b.id)
                put("name", b.name)
                put("categoryId", b.categoryId ?: JSONObject.NULL)
                put("limitAmount", b.limitAmount)
                put("period", b.period)
            }
            bArray.put(o)
        }
        root.put("budgets", bArray)

        // 6. Savings Goals
        val goals = database.savingsGoalDao().getAllGoalsFlow().first()
        val gArray = JSONArray()
        for (g in goals) {
            val o = JSONObject().apply {
                put("id", g.id)
                put("name", g.name)
                put("targetAmount", g.targetAmount)
                put("savedAmount", g.savedAmount)
                put("targetDate", g.targetDate)
                put("icon", g.icon)
                put("color", g.color)
                put("notes", g.notes)
            }
            gArray.put(o)
        }
        root.put("goals", gArray)

        // 7. Bill Reminders
        val reminders = database.billReminderDao().getAllRemindersFlow().first()
        val rArray = JSONArray()
        for (r in reminders) {
            val o = JSONObject().apply {
                put("id", r.id)
                put("title", r.title)
                put("amount", r.amount)
                put("dueDate", r.dueDate)
                put("category", r.category)
                put("isPaid", r.isPaid)
                put("notes", r.notes)
            }
            rArray.put(o)
        }
        root.put("reminders", rArray)

        val file = File(context.cacheDir, "expense_tracker_backup_${System.currentTimeMillis()}.json")
        FileOutputStream(file).use { it.write(root.toString(2).toByteArray()) }
        file
    }

    /**
     * Inspects and validates a backup JSON content before applying restore.
     */
    fun validateBackup(jsonString: String): BackupValidationResult {
        return try {
            val obj = JSONObject(jsonString)
            val txCount = obj.optJSONArray("transactions")?.length() ?: 0
            val accCount = obj.optJSONArray("accounts")?.length() ?: 0
            val catCount = obj.optJSONArray("categories")?.length() ?: 0
            val timestamp = obj.optLong("timestamp", System.currentTimeMillis())

            if (accCount == 0 && catCount == 0 && txCount == 0) {
                BackupValidationResult(
                    isValid = false,
                    errorMessage = "Backup file does not contain any valid financial records."
                )
            } else {
                BackupValidationResult(
                    isValid = true,
                    transactionCount = txCount,
                    accountCount = accCount,
                    categoryCount = catCount,
                    backupDate = timestamp,
                    rawJson = jsonString
                )
            }
        } catch (e: Exception) {
            BackupValidationResult(
                isValid = false,
                errorMessage = "Invalid backup format: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Restores data from validated JSON into Room database.
     */
    suspend fun restoreBackup(database: AppDatabase, jsonString: String) = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonString)

        // 1. Profile
        if (root.has("profile")) {
            val p = root.getJSONObject("profile")
            val current = database.userProfileDao().getUserProfile() ?: UserProfileEntity()
            database.userProfileDao().insertOrUpdate(
                current.copy(
                    name = p.optString("name", current.name),
                    currencySymbol = p.optString("currencySymbol", current.currencySymbol),
                    currencyCode = p.optString("currencyCode", current.currencyCode),
                    themeMode = p.optString("themeMode", current.themeMode)
                )
            )
        }

        // 2. Accounts
        root.optJSONArray("accounts")?.let { arr ->
            val list = mutableListOf<AccountEntity>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    AccountEntity(
                        id = o.optLong("id", 0L),
                        name = o.getString("name"),
                        type = o.optString("type", "BANK"),
                        openingBalance = o.optLong("openingBalance", 0L),
                        currentBalance = o.optLong("currentBalance", 0L),
                        icon = o.optString("icon", "AccountBalance"),
                        color = o.optString("color", "#0284C7"),
                        isActive = o.optBoolean("isActive", true),
                        sortOrder = o.optInt("sortOrder", i)
                    )
                )
            }
            if (list.isNotEmpty()) {
                database.accountDao().insertAccounts(list)
            }
        }

        // 3. Categories
        root.optJSONArray("categories")?.let { arr ->
            val list = mutableListOf<CategoryEntity>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    CategoryEntity(
                        id = o.optLong("id", 0L),
                        name = o.getString("name"),
                        type = o.optString("type", "EXPENSE"),
                        icon = o.optString("icon", "Category"),
                        color = o.optString("color", "#10B981"),
                        isActive = o.optBoolean("isActive", true),
                        sortOrder = o.optInt("sortOrder", i)
                    )
                )
            }
            if (list.isNotEmpty()) {
                database.categoryDao().insertCategories(list)
            }
        }

        // 4. Transactions
        root.optJSONArray("transactions")?.let { arr ->
            val list = mutableListOf<TransactionEntity>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    TransactionEntity(
                        id = o.optLong("id", 0L),
                        type = o.getString("type"),
                        amount = o.getLong("amount"),
                        categoryId = if (o.isNull("categoryId")) null else o.getLong("categoryId"),
                        accountId = o.getLong("accountId"),
                        toAccountId = if (o.isNull("toAccountId")) null else o.getLong("toAccountId"),
                        dateTime = o.optLong("dateTime", System.currentTimeMillis()),
                        note = o.optString("note", ""),
                        merchant = o.optString("merchant", ""),
                        location = o.optString("location", ""),
                        receiptUri = if (o.isNull("receiptUri")) null else o.getString("receiptUri"),
                        tags = o.optString("tags", "")
                    )
                )
            }
            if (list.isNotEmpty()) {
                database.transactionDao().insertTransactions(list)
            }
        }

        // 5. Budgets
        root.optJSONArray("budgets")?.let { arr ->
            val list = mutableListOf<BudgetEntity>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    BudgetEntity(
                        id = o.optLong("id", 0L),
                        name = o.getString("name"),
                        categoryId = if (o.isNull("categoryId")) null else o.getLong("categoryId"),
                        limitAmount = o.getLong("limitAmount"),
                        period = o.optString("period", "MONTHLY")
                    )
                )
            }
            if (list.isNotEmpty()) {
                database.budgetDao().insertBudgets(list)
            }
        }

        // 6. Savings Goals
        root.optJSONArray("goals")?.let { arr ->
            val list = mutableListOf<SavingsGoalEntity>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    SavingsGoalEntity(
                        id = o.optLong("id", 0L),
                        name = o.getString("name"),
                        targetAmount = o.getLong("targetAmount"),
                        savedAmount = o.optLong("savedAmount", 0L),
                        targetDate = o.optLong("targetDate", 0L),
                        icon = o.optString("icon", "Savings"),
                        color = o.optString("color", "#8B5CF6"),
                        notes = o.optString("notes", "")
                    )
                )
            }
            if (list.isNotEmpty()) {
                database.savingsGoalDao().insertGoals(list)
            }
        }

        // 7. Bill Reminders
        root.optJSONArray("reminders")?.let { arr ->
            val list = mutableListOf<BillReminderEntity>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    BillReminderEntity(
                        id = o.optLong("id", 0L),
                        title = o.getString("title"),
                        amount = o.getLong("amount"),
                        dueDate = o.getLong("dueDate"),
                        category = o.optString("category", "Bills"),
                        isPaid = o.optBoolean("isPaid", false),
                        notes = o.optString("notes", "")
                    )
                )
            }
            if (list.isNotEmpty()) {
                database.billReminderDao().insertReminders(list)
            }
        }
    }
}
