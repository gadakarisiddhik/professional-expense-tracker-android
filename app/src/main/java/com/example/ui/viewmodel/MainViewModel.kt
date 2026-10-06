package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ExpenseTrackerRepository
import com.example.domain.model.Account
import com.example.domain.model.BillReminderItem
import com.example.domain.model.BudgetWithStatus
import com.example.domain.model.Category
import com.example.domain.model.CategorySpend
import com.example.domain.model.CreditCardItem
import com.example.domain.model.DashboardSummary
import com.example.domain.model.RecurringTransactionItem
import com.example.domain.model.SavingsGoalItem
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import com.example.domain.model.UserProfile
import com.example.utils.BackupHelper
import com.example.utils.BackupValidationResult
import com.example.utils.DateUtils
import com.example.utils.ExportHelper
import com.example.utils.MoneyUtils
import com.example.utils.NotificationHelper
import com.example.utils.ParsedReceipt
import com.example.utils.QuickAddResult
import com.example.utils.ReceiptOcrHelper
import com.example.utils.SecurityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class TimeFilter {
    ALL, TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, LAST_MONTH
}

enum class SortOrder {
    DATE_DESC, AMOUNT_DESC, AMOUNT_ASC
}

data class AnalyticsState(
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val netSavings: Long = 0L,
    val averageDailySpending: Long = 0L,
    val highestExpense: TransactionItem? = null,
    val categoryBreakdown: List<CategorySpend> = emptyList(),
    val dailyTrend: List<Pair<String, Long>> = emptyList()
)

class MainViewModel(private val repository: ExpenseTrackerRepository) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = repository.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val activeAccounts: StateFlow<List<Account>> = repository.activeAccountsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAccounts: StateFlow<List<Account>> = repository.allAccountsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = repository.allCategoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionItem>> = repository.allTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<TransactionItem>> = repository.recentTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardSummary: StateFlow<DashboardSummary> = repository.dashboardSummaryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    val budgets: StateFlow<List<BudgetWithStatus>> = repository.budgetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsGoals: StateFlow<List<SavingsGoalItem>> = repository.savingsGoalsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val creditCards: StateFlow<List<CreditCardItem>> = repository.creditCardsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val billReminders: StateFlow<List<BillReminderItem>> = repository.billRemindersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringTransactions: StateFlow<List<RecurringTransactionItem>> = repository.recurringTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ----------------------------------------------------
    // Filter & Search State for Transaction History
    // ----------------------------------------------------
    val searchQuery = MutableStateFlow("")
    val selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    val selectedTimeFilter = MutableStateFlow(TimeFilter.ALL)
    val selectedCategoryFilter = MutableStateFlow<Long?>(null)
    val selectedAccountFilter = MutableStateFlow<Long?>(null)
    val selectedSortOrder = MutableStateFlow(SortOrder.DATE_DESC)

    data class TransactionFilters(
        val query: String = "",
        val type: TransactionType? = null,
        val timeFilter: TimeFilter = TimeFilter.ALL,
        val catId: Long? = null,
        val accId: Long? = null,
        val sort: SortOrder = SortOrder.DATE_DESC
    )

    private val filtersFlow = combine(
        combine(searchQuery, selectedTypeFilter, selectedTimeFilter) { q, t, tf -> Triple(q, t, tf) },
        combine(selectedCategoryFilter, selectedAccountFilter, selectedSortOrder) { cat, acc, sort -> Triple(cat, acc, sort) }
    ) { (q, t, tf), (cat, acc, sort) ->
        TransactionFilters(q, t, tf, cat, acc, sort)
    }

    val filteredTransactions: StateFlow<List<TransactionItem>> = combine(
        allTransactions,
        filtersFlow
    ) { txList, filters ->
        var list = txList

        // 1. Search Query
        if (filters.query.isNotBlank()) {
            val q = filters.query.trim().lowercase()
            list = list.filter {
                it.merchant.lowercase().contains(q) ||
                it.note.lowercase().contains(q) ||
                it.categoryName.lowercase().contains(q) ||
                it.accountName.lowercase().contains(q) ||
                it.tags.lowercase().contains(q) ||
                MoneyUtils.toDecimalString(it.amount).contains(q)
            }
        }

        // 2. Type Filter
        if (filters.type != null) {
            list = list.filter { it.type == filters.type }
        }

        // 3. Time Filter
        val now = System.currentTimeMillis()
        when (filters.timeFilter) {
            TimeFilter.ALL -> Unit
            TimeFilter.TODAY -> {
                val start = DateUtils.getStartOfDay(now)
                val end = DateUtils.getEndOfDay(now)
                list = list.filter { it.dateTime in start..end }
            }
            TimeFilter.YESTERDAY -> {
                val start = DateUtils.getStartOfDay(now - 86400000L)
                val end = DateUtils.getEndOfDay(now - 86400000L)
                list = list.filter { it.dateTime in start..end }
            }
            TimeFilter.THIS_WEEK -> {
                val start = DateUtils.getStartOfWeek(now)
                list = list.filter { it.dateTime >= start }
            }
            TimeFilter.THIS_MONTH -> {
                val start = DateUtils.getStartOfMonth(now)
                val end = DateUtils.getEndOfMonth(now)
                list = list.filter { it.dateTime in start..end }
            }
            TimeFilter.LAST_MONTH -> {
                val start = DateUtils.getStartOfLastMonth()
                val end = DateUtils.getEndOfLastMonth()
                list = list.filter { it.dateTime in start..end }
            }
        }

        // 4. Category Filter
        if (filters.catId != null) {
            list = list.filter { it.categoryId == filters.catId }
        }

        // 5. Account Filter
        if (filters.accId != null) {
            list = list.filter { it.accountId == filters.accId || it.toAccountId == filters.accId }
        }

        // 6. Sort
        when (filters.sort) {
            SortOrder.DATE_DESC -> list.sortedByDescending { it.dateTime }
            SortOrder.AMOUNT_DESC -> list.sortedByDescending { it.amount }
            SortOrder.AMOUNT_ASC -> list.sortedBy { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ----------------------------------------------------
    // Analytics State
    // ----------------------------------------------------
    val selectedAnalyticsMonth = MutableStateFlow(DateUtils.getStartOfMonth())

    val analyticsState: StateFlow<AnalyticsState> = combine(
        allTransactions,
        selectedAnalyticsMonth
    ) { transactions, monthStart ->
        val monthEnd = DateUtils.getEndOfMonth(monthStart)
        val monthTx = transactions.filter { it.dateTime in monthStart..monthEnd }

        val income = monthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = monthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val savings = (income - expense).coerceAtLeast(0L)

        val expenses = monthTx.filter { it.type == TransactionType.EXPENSE }
        val highest = expenses.maxByOrNull { it.amount }

        // Category breakdown
        val catGroups = expenses.groupBy { it.categoryName }
        val breakdown = catGroups.map { entry ->
            val sample = entry.value.first()
            val totalCat = entry.value.sumOf { it.amount }
            val pct = if (expense > 0) (totalCat.toFloat() / expense.toFloat()) * 100f else 0f
            CategorySpend(
                categoryId = sample.categoryId ?: 0L,
                categoryName = entry.key,
                categoryIcon = sample.categoryIcon,
                categoryColor = sample.categoryColor,
                amount = totalCat,
                percentage = pct
            )
        }.sortedByDescending { it.amount }

        // Daily trend (last 7 recorded days or days in month)
        val dailyMap = expenses.groupBy { DateUtils.formatDate(it.dateTime) }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
        val dailyTrend = dailyMap.entries.toList().takeLast(7).map { Pair(it.key.substring(0, 6), it.value) }

        val daysCount = (System.currentTimeMillis() - monthStart).coerceAtLeast(86400000L) / 86400000L
        val avgDaily = if (daysCount > 0) expense / daysCount else expense

        AnalyticsState(
            totalIncome = income,
            totalExpense = expense,
            netSavings = savings,
            averageDailySpending = avgDaily,
            highestExpense = highest,
            categoryBreakdown = breakdown,
            dailyTrend = dailyTrend
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsState())

    // ----------------------------------------------------
    // App Lock & Security State
    // ----------------------------------------------------
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = repository.getUserProfile()
            if (profile.appLockEnabled && profile.pinHash.isNotBlank()) {
                _isAppLocked.value = true
            }
        }
    }

    fun unlockWithPin(pin: String): Boolean {
        val currentProfile = userProfile.value
        val matches = SecurityUtils.verifyPin(pin, currentProfile.pinHash)
        if (matches) {
            _isAppLocked.value = false
        }
        return matches
    }

    fun unlockBiometric() {
        _isAppLocked.value = false
    }

    fun setAppPin(pin: String) {
        viewModelScope.launch {
            val current = userProfile.value
            val hash = SecurityUtils.hashPin(pin)
            repository.saveUserProfile(current.copy(pinHash = hash, appLockEnabled = true))
        }
    }

    fun toggleAppLock(enabled: Boolean) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.saveUserProfile(current.copy(appLockEnabled = enabled))
            if (!enabled) {
                _isAppLocked.value = false
            }
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.saveUserProfile(current.copy(biometricEnabled = enabled))
        }
    }

    // ----------------------------------------------------
    // User Profile Actions
    // ----------------------------------------------------
    fun updateProfile(name: String, currencySymbol: String, currencyCode: String, themeMode: String) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.saveUserProfile(
                current.copy(
                    name = name,
                    currencySymbol = currencySymbol,
                    currencyCode = currencyCode,
                    themeMode = themeMode,
                    isOnboarded = true
                )
            )
        }
    }

    fun completeOnboarding(
        name: String,
        currencySymbol: String,
        currencyCode: String,
        accountName: String,
        initialBalance: Long
    ) {
        viewModelScope.launch {
            // Save Account
            repository.saveAccount(
                Account(
                    name = accountName,
                    type = "BANK",
                    openingBalance = initialBalance,
                    currentBalance = initialBalance,
                    icon = "AccountBalance",
                    color = "#0284C7"
                )
            )
            // Save Profile
            repository.saveUserProfile(
                userProfile.value.copy(
                    name = name,
                    currencySymbol = currencySymbol,
                    currencyCode = currencyCode,
                    isOnboarded = true
                )
            )
        }
    }

    // ----------------------------------------------------
    // Transaction CRUD
    // ----------------------------------------------------
    fun addExpense(
        amount: Long,
        categoryId: Long,
        accountId: Long,
        dateTime: Long = System.currentTimeMillis(),
        merchant: String = "",
        note: String = "",
        location: String = "",
        tags: String = "",
        receiptUri: String? = null,
        context: Context? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.addTransaction(
                type = TransactionType.EXPENSE,
                amount = amount,
                categoryId = categoryId,
                accountId = accountId,
                dateTime = dateTime,
                merchant = merchant,
                note = note,
                location = location,
                tags = tags,
                receiptUri = receiptUri
            )

            // Check budget alert
            if (context != null) {
                checkBudgetAlerts(context, categoryId, amount)
            }
            onSuccess()
        }
    }

    fun addIncome(
        amount: Long,
        categoryId: Long,
        accountId: Long,
        dateTime: Long = System.currentTimeMillis(),
        source: String = "",
        note: String = "",
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.addTransaction(
                type = TransactionType.INCOME,
                amount = amount,
                categoryId = categoryId,
                accountId = accountId,
                dateTime = dateTime,
                merchant = source,
                note = note
            )
            onSuccess()
        }
    }

    fun addTransfer(
        amount: Long,
        fromAccountId: Long,
        toAccountId: Long,
        dateTime: Long = System.currentTimeMillis(),
        note: String = "",
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.addTransaction(
                type = TransactionType.TRANSFER,
                amount = amount,
                categoryId = null,
                accountId = fromAccountId,
                toAccountId = toAccountId,
                dateTime = dateTime,
                note = note
            )
            onSuccess()
        }
    }

    fun updateTransaction(
        id: Long,
        type: TransactionType,
        amount: Long,
        categoryId: Long?,
        accountId: Long,
        toAccountId: Long?,
        dateTime: Long,
        note: String,
        merchant: String,
        location: String,
        receiptUri: String?,
        tags: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.updateTransaction(
                existingId = id,
                newType = type,
                newAmount = amount,
                newCategoryId = categoryId,
                newAccountId = accountId,
                newToAccountId = toAccountId,
                newDateTime = dateTime,
                newNote = note,
                newMerchant = merchant,
                newLocation = location,
                newReceiptUri = receiptUri,
                newTags = tags
            )
            onSuccess()
        }
    }

    fun deleteTransaction(transactionId: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
            onComplete()
        }
    }

    fun duplicateTransaction(item: TransactionItem, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.addTransaction(
                type = item.type,
                amount = item.amount,
                categoryId = item.categoryId,
                accountId = item.accountId,
                toAccountId = item.toAccountId,
                dateTime = System.currentTimeMillis(),
                note = item.note,
                merchant = item.merchant,
                location = item.location,
                tags = item.tags
            )
            onSuccess()
        }
    }

    private suspend fun checkBudgetAlerts(context: Context, categoryId: Long, addedAmount: Long) {
        val currentBudgets = budgets.value
        val currencySym = userProfile.value.currencySymbol

        for (b in currentBudgets) {
            if (b.categoryId == null || b.categoryId == categoryId) {
                val newSpent = b.spentAmount + addedAmount
                val pct = MoneyUtils.calculatePercentage(newSpent, b.limitAmount)
                if (pct >= 100) {
                    NotificationHelper.sendBudgetAlert(
                        context,
                        b.name,
                        pct,
                        MoneyUtils.formatCurrency(newSpent, currencySym),
                        isExceeded = true
                    )
                } else if (pct >= 80) {
                    NotificationHelper.sendBudgetAlert(
                        context,
                        b.name,
                        pct,
                        MoneyUtils.formatCurrency(newSpent, currencySym),
                        isExceeded = false
                    )
                }
            }
        }
    }

    // ----------------------------------------------------
    // Accounts CRUD
    // ----------------------------------------------------
    fun saveAccount(account: Account, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveAccount(account)
            onSuccess()
        }
    }

    fun deleteAccount(account: Account, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAccount(account)
            onSuccess()
        }
    }

    // ----------------------------------------------------
    // Categories CRUD
    // ----------------------------------------------------
    fun saveCategory(category: Category, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveCategory(category)
            onSuccess()
        }
    }

    fun archiveCategory(category: Category, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            onSuccess()
        }
    }

    // ----------------------------------------------------
    // Budgets CRUD
    // ----------------------------------------------------
    fun saveBudget(id: Long, name: String, categoryId: Long?, limitAmount: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveBudget(id, name, categoryId, limitAmount)
            onSuccess()
        }
    }

    fun deleteBudget(id: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteBudget(id)
            onSuccess()
        }
    }

    // ----------------------------------------------------
    // Savings Goals CRUD
    // ----------------------------------------------------
    fun saveSavingsGoal(id: Long, name: String, targetAmount: Long, savedAmount: Long, targetDate: Long, notes: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveSavingsGoal(id, name, targetAmount, savedAmount, targetDate, notes = notes)
            onSuccess()
        }
    }

    fun addMoneyToGoal(goalId: Long, amount: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.addMoneyToGoal(goalId, amount)
            onSuccess()
        }
    }

    fun withdrawMoneyFromGoal(goalId: Long, amount: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.withdrawMoneyFromGoal(goalId, amount)
            onSuccess()
        }
    }

    fun deleteSavingsGoal(id: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(id)
            onSuccess()
        }
    }

    // ----------------------------------------------------
    // Credit Cards CRUD
    // ----------------------------------------------------
    fun saveCreditCard(
        id: Long,
        accountId: Long,
        cardName: String,
        bank: String,
        creditLimit: Long,
        statementDay: Int,
        dueDay: Int,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.saveCreditCard(id, accountId, cardName, bank, creditLimit, statementDay, dueDay)
            onSuccess()
        }
    }

    // ----------------------------------------------------
    // Bill Reminders & Recurring CRUD
    // ----------------------------------------------------
    fun saveBillReminder(id: Long, title: String, amount: Long, dueDate: Long, category: String, repeatRule: String, notes: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveBillReminder(id, title, amount, dueDate, category, repeatRule, notes)
            onSuccess()
        }
    }

    fun markBillPaid(id: Long, isPaid: Boolean) {
        viewModelScope.launch {
            repository.markBillPaid(id, isPaid)
        }
    }

    fun deleteBillReminder(id: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteBillReminder(id)
            onSuccess()
        }
    }

    fun saveRecurringTransaction(
        id: Long,
        name: String,
        amount: Long,
        type: TransactionType,
        categoryId: Long,
        accountId: Long,
        frequency: String,
        startDate: Long,
        nextDueDate: Long,
        note: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.saveRecurringTransaction(id, name, amount, type, categoryId, accountId, frequency, startDate, nextDueDate, note)
            onSuccess()
        }
    }

    fun deleteRecurringTransaction(id: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteRecurringTransaction(id)
            onSuccess()
        }
    }

    // ----------------------------------------------------
    // Smart Quick Add
    // ----------------------------------------------------
    fun parseQuickAdd(input: String): QuickAddResult {
        val cats = allCategories.value.map { it.name }
        val accs = activeAccounts.value.map { it.name }
        return ReceiptOcrHelper.parseQuickAdd(input, cats, accs)
    }

    // ----------------------------------------------------
    // Receipt Scanner OCR
    // ----------------------------------------------------
    fun parseReceipt(rawText: String): ParsedReceipt {
        return ReceiptOcrHelper.parseReceiptText(rawText)
    }

    // ----------------------------------------------------
    // Export & Backup
    // ----------------------------------------------------
    fun exportCsv(context: Context) {
        viewModelScope.launch {
            val list = allTransactions.value
            val sym = userProfile.value.currencySymbol
            val file = ExportHelper.exportToCsv(context, list, sym)
            ExportHelper.shareFile(context, file, "text/csv", "Share Expense Tracker CSV")
        }
    }

    fun exportPdf(context: Context) {
        viewModelScope.launch {
            val list = allTransactions.value
            val summary = dashboardSummary.value
            val sym = userProfile.value.currencySymbol
            val file = ExportHelper.exportToPdf(
                context,
                list,
                summary.totalIncome,
                summary.totalExpense,
                sym,
                "Monthly Financial Statement"
            )
            ExportHelper.shareFile(context, file, "application/pdf", "Share Financial Report PDF")
        }
    }

    fun createBackup(context: Context, database: com.example.data.local.AppDatabase, onFileReady: (File) -> Unit) {
        viewModelScope.launch {
            val file = BackupHelper.createBackup(context, database)
            onFileReady(file)
        }
    }

    fun validateBackup(json: String): BackupValidationResult {
        return BackupHelper.validateBackup(json)
    }

    fun restoreBackup(database: com.example.data.local.AppDatabase, json: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            BackupHelper.restoreBackup(database, json)
            onComplete()
        }
    }

    fun seedDemoData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.seedDemoData()
            onComplete()
        }
    }
}
