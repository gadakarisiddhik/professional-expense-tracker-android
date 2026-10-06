package com.example.data.repository

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
import com.example.domain.model.Account
import com.example.domain.model.BillReminderItem
import com.example.domain.model.BudgetWithStatus
import com.example.domain.model.Category
import com.example.domain.model.CategorySpend
import com.example.domain.model.CreditCardItem
import com.example.domain.model.DashboardSummary
import com.example.domain.model.FinancialInsight
import com.example.domain.model.InsightType
import com.example.domain.model.RecurringTransactionItem
import com.example.domain.model.SavingsGoalItem
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import com.example.domain.model.UserProfile
import com.example.utils.DateUtils
import com.example.utils.MoneyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ExpenseTrackerRepository(private val database: AppDatabase) {

    private val userProfileDao = database.userProfileDao()
    private val accountDao = database.accountDao()
    private val categoryDao = database.categoryDao()
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val recurringDao = database.recurringTransactionDao()
    private val savingsGoalDao = database.savingsGoalDao()
    private val creditCardDao = database.creditCardDao()
    private val billReminderDao = database.billReminderDao()

    // ----------------------------------------------------
    // User Profile
    // ----------------------------------------------------
    val userProfileFlow: Flow<UserProfile> = userProfileDao.getUserProfileFlow().map { entity ->
        entity?.toDomain() ?: UserProfile()
    }

    suspend fun getUserProfile(): UserProfile {
        return userProfileDao.getUserProfile()?.toDomain() ?: UserProfile()
    }

    suspend fun saveUserProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        userProfileDao.insertOrUpdate(profile.toEntity())
    }

    // ----------------------------------------------------
    // Accounts
    // ----------------------------------------------------
    val activeAccountsFlow: Flow<List<Account>> = accountDao.getAllActiveAccountsFlow().map { list ->
        list.map { it.toDomain() }
    }

    val allAccountsFlow: Flow<List<Account>> = accountDao.getAllAccountsFlow().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getAccountById(id: Long): Account? = withContext(Dispatchers.IO) {
        accountDao.getAccountById(id)?.toDomain()
    }

    suspend fun saveAccount(account: Account): Long = withContext(Dispatchers.IO) {
        if (account.id == 0L) {
            val entity = account.toEntity().copy(currentBalance = account.openingBalance)
            accountDao.insertAccount(entity)
        } else {
            accountDao.updateAccount(account.toEntity())
            account.id
        }
    }

    suspend fun deleteAccount(account: Account) = withContext(Dispatchers.IO) {
        accountDao.deleteAccount(account.toEntity())
    }

    // ----------------------------------------------------
    // Categories
    // ----------------------------------------------------
    val allCategoriesFlow: Flow<List<Category>> = categoryDao.getAllCategoriesFlow().map { list ->
        list.map { it.toDomain() }
    }

    val activeCategoriesFlow: Flow<List<Category>> = categoryDao.getAllActiveCategoriesFlow().map { list ->
        list.map { it.toDomain() }
    }

    fun getCategoriesByTypeFlow(type: String): Flow<List<Category>> =
        categoryDao.getCategoriesByTypeFlow(type).map { list -> list.map { it.toDomain() } }

    suspend fun saveCategory(category: Category): Long = withContext(Dispatchers.IO) {
        if (category.id == 0L) {
            categoryDao.insertCategory(category.toEntity())
        } else {
            categoryDao.updateCategory(category.toEntity())
            category.id
        }
    }

    suspend fun deleteCategory(category: Category) = withContext(Dispatchers.IO) {
        // Soft delete / archive to never destroy historical transactions
        categoryDao.updateCategory(category.toEntity().copy(isActive = false))
    }

    // ----------------------------------------------------
    // Transactions
    // ----------------------------------------------------
    val allTransactionsFlow: Flow<List<TransactionItem>> = combine(
        transactionDao.getAllTransactionsFlow(),
        categoryDao.getAllCategoriesFlow(),
        accountDao.getAllAccountsFlow()
    ) { transactions, categories, accounts ->
        val categoryMap = categories.associateBy { it.id }
        val accountMap = accounts.associateBy { it.id }
        transactions.map { it.toDomain(categoryMap, accountMap) }
    }.flowOn(Dispatchers.IO)

    val recentTransactionsFlow: Flow<List<TransactionItem>> = combine(
        transactionDao.getRecentTransactionsFlow(10),
        categoryDao.getAllCategoriesFlow(),
        accountDao.getAllAccountsFlow()
    ) { transactions, categories, accounts ->
        val categoryMap = categories.associateBy { it.id }
        val accountMap = accounts.associateBy { it.id }
        transactions.map { it.toDomain(categoryMap, accountMap) }
    }.flowOn(Dispatchers.IO)

    suspend fun getTransactionById(id: Long): TransactionItem? = withContext(Dispatchers.IO) {
        val entity = transactionDao.getTransactionById(id) ?: return@withContext null
        val categories = categoryDao.getAllCategoriesFlow().first().associateBy { it.id }
        val accounts = accountDao.getAllAccountsFlow().first().associateBy { it.id }
        entity.toDomain(categories, accounts)
    }

    /**
     * Adds an expense, income, or transfer transaction and atomically adjusts balances.
     */
    suspend fun addTransaction(
        type: TransactionType,
        amount: Long,
        categoryId: Long?,
        accountId: Long,
        toAccountId: Long? = null,
        dateTime: Long = System.currentTimeMillis(),
        note: String = "",
        merchant: String = "",
        location: String = "",
        receiptUri: String? = null,
        tags: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val entity = TransactionEntity(
            type = type.name,
            amount = amount,
            categoryId = categoryId,
            accountId = accountId,
            toAccountId = toAccountId,
            dateTime = dateTime,
            note = note,
            merchant = merchant,
            location = location,
            receiptUri = receiptUri,
            tags = tags
        )
        val transactionId = transactionDao.insertTransaction(entity)

        // Apply balance changes
        applyTransactionBalanceEffect(type, amount, accountId, toAccountId)

        transactionId
    }

    /**
     * Edits an existing transaction: reverses the old effect, then applies the new effect.
     */
    suspend fun updateTransaction(
        existingId: Long,
        newType: TransactionType,
        newAmount: Long,
        newCategoryId: Long?,
        newAccountId: Long,
        newToAccountId: Long?,
        newDateTime: Long,
        newNote: String,
        newMerchant: String,
        newLocation: String,
        newReceiptUri: String?,
        newTags: String
    ) = withContext(Dispatchers.IO) {
        val old = transactionDao.getTransactionById(existingId) ?: return@withContext

        // 1. Reverse old transaction effect
        val oldType = TransactionType.valueOf(old.type)
        reverseTransactionBalanceEffect(oldType, old.amount, old.accountId, old.toAccountId)

        // 2. Update transaction entity
        val updatedEntity = old.copy(
            type = newType.name,
            amount = newAmount,
            categoryId = newCategoryId,
            accountId = newAccountId,
            toAccountId = newToAccountId,
            dateTime = newDateTime,
            note = newNote,
            merchant = newMerchant,
            location = newLocation,
            receiptUri = newReceiptUri,
            tags = newTags,
            updatedAt = System.currentTimeMillis()
        )
        transactionDao.updateTransaction(updatedEntity)

        // 3. Apply new transaction effect
        applyTransactionBalanceEffect(newType, newAmount, newAccountId, newToAccountId)
    }

    /**
     * Deletes a transaction: reverses its balance effect first, then deletes it.
     */
    suspend fun deleteTransaction(transactionId: Long) = withContext(Dispatchers.IO) {
        val old = transactionDao.getTransactionById(transactionId) ?: return@withContext
        val oldType = TransactionType.valueOf(old.type)
        reverseTransactionBalanceEffect(oldType, old.amount, old.accountId, old.toAccountId)
        transactionDao.deleteTransactionById(transactionId)
    }

    private suspend fun applyTransactionBalanceEffect(
        type: TransactionType,
        amount: Long,
        accountId: Long,
        toAccountId: Long?
    ) {
        val sourceAccount = accountDao.getAccountById(accountId)
        when (type) {
            TransactionType.EXPENSE -> {
                sourceAccount?.let {
                    accountDao.updateBalance(accountId, it.currentBalance - amount)
                }
            }
            TransactionType.INCOME -> {
                sourceAccount?.let {
                    accountDao.updateBalance(accountId, it.currentBalance + amount)
                }
            }
            TransactionType.TRANSFER -> {
                sourceAccount?.let {
                    accountDao.updateBalance(accountId, it.currentBalance - amount)
                }
                if (toAccountId != null && toAccountId != accountId) {
                    val destAccount = accountDao.getAccountById(toAccountId)
                    destAccount?.let {
                        accountDao.updateBalance(toAccountId, it.currentBalance + amount)
                    }
                }
            }
        }
    }

    private suspend fun reverseTransactionBalanceEffect(
        type: TransactionType,
        amount: Long,
        accountId: Long,
        toAccountId: Long?
    ) {
        val sourceAccount = accountDao.getAccountById(accountId)
        when (type) {
            TransactionType.EXPENSE -> {
                sourceAccount?.let {
                    accountDao.updateBalance(accountId, it.currentBalance + amount)
                }
            }
            TransactionType.INCOME -> {
                sourceAccount?.let {
                    accountDao.updateBalance(accountId, it.currentBalance - amount)
                }
            }
            TransactionType.TRANSFER -> {
                sourceAccount?.let {
                    accountDao.updateBalance(accountId, it.currentBalance + amount)
                }
                if (toAccountId != null && toAccountId != accountId) {
                    val destAccount = accountDao.getAccountById(toAccountId)
                    destAccount?.let {
                        accountDao.updateBalance(toAccountId, it.currentBalance - amount)
                    }
                }
            }
        }
    }

    // ----------------------------------------------------
    // Budgets
    // ----------------------------------------------------
    val budgetsFlow: Flow<List<BudgetWithStatus>> = combine(
        budgetDao.getAllBudgetsFlow(),
        categoryDao.getAllCategoriesFlow(),
        transactionDao.getAllTransactionsFlow()
    ) { budgets, categories, transactions ->
        val categoryMap = categories.associateBy { it.id }
        val startOfMonth = DateUtils.getStartOfMonth()
        val endOfMonth = DateUtils.getEndOfMonth()

        val monthlyExpenses = transactions.filter {
            it.type == TransactionType.EXPENSE.name && it.dateTime in startOfMonth..endOfMonth
        }

        budgets.map { budget ->
            val spent = if (budget.categoryId == null) {
                // Overall monthly budget
                monthlyExpenses.sumOf { it.amount }
            } else {
                monthlyExpenses.filter { it.categoryId == budget.categoryId }.sumOf { it.amount }
            }
            val cat = budget.categoryId?.let { categoryMap[it] }
            val remaining = (budget.limitAmount - spent).coerceAtLeast(0L)
            val percentage = MoneyUtils.calculatePercentage(spent, budget.limitAmount)

            BudgetWithStatus(
                id = budget.id,
                name = budget.name,
                categoryId = budget.categoryId,
                categoryName = cat?.name,
                categoryIcon = cat?.icon,
                categoryColor = cat?.color,
                limitAmount = budget.limitAmount,
                spentAmount = spent,
                remainingAmount = remaining,
                percentage = percentage,
                period = budget.period
            )
        }
    }.flowOn(Dispatchers.IO)

    suspend fun saveBudget(
        id: Long,
        name: String,
        categoryId: Long?,
        limitAmount: Long,
        period: String = "MONTHLY"
    ) = withContext(Dispatchers.IO) {
        val entity = BudgetEntity(
            id = id,
            name = name,
            categoryId = categoryId,
            limitAmount = limitAmount,
            period = period
        )
        if (id == 0L) {
            budgetDao.insertBudget(entity)
        } else {
            budgetDao.updateBudget(entity)
        }
    }

    suspend fun deleteBudget(id: Long) = withContext(Dispatchers.IO) {
        val budget = budgetDao.getBudgetById(id) ?: return@withContext
        budgetDao.deleteBudget(budget)
    }

    // ----------------------------------------------------
    // Dashboard Summary Flow
    // ----------------------------------------------------
    // Secondary bundle for goals, reminders, and recurring
    private val secondaryFlow = combine(
        savingsGoalDao.getActiveGoalsFlow(),
        billReminderDao.getUpcomingRemindersFlow(),
        recurringDao.getActiveRecurringFlow()
    ) { activeGoals, upcomingBills, activeRecurring ->
        Triple(activeGoals, upcomingBills, activeRecurring)
    }

    val dashboardSummaryFlow: Flow<DashboardSummary> = combine(
        accountDao.getAllActiveAccountsFlow(),
        transactionDao.getAllTransactionsFlow(),
        categoryDao.getAllCategoriesFlow(),
        budgetsFlow,
        secondaryFlow
    ) { accounts, rawTransactions, categories, budgets, (activeGoals, upcomingBills, activeRecurring) ->
        val categoryMap = categories.associateBy { it.id }
        val accountMap = accounts.associateBy { it.id }
        val transactions = rawTransactions.map { it.toDomain(categoryMap, accountMap) }

        val startOfMonth = DateUtils.getStartOfMonth()
        val endOfMonth = DateUtils.getEndOfMonth()
        val startOfToday = DateUtils.getStartOfDay()
        val endOfToday = DateUtils.getEndOfDay()

        // Available balance: sum of active accounts (excluding credit cards)
        val availableBalance = accounts
            .filter { it.type != "CREDIT_CARD" }
            .sumOf { it.currentBalance }

        val monthTransactions = transactions.filter { it.dateTime in startOfMonth..endOfMonth }
        val totalIncome = monthTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val totalExpense = monthTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val estimatedSavings = (totalIncome - totalExpense).coerceAtLeast(0L)

        val todayTransactions = transactions.filter { it.dateTime in startOfToday..endOfToday }
        val todaySpending = todayTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val overallBudget = budgets.find { it.categoryId == null } ?: budgets.firstOrNull()

        // Generate Insights
        val insights = generateInsights(transactions, totalIncome, totalExpense, categories)

        DashboardSummary(
            availableBalance = availableBalance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            estimatedSavings = estimatedSavings,
            todaySpending = todaySpending,
            overallBudget = overallBudget,
            recentTransactions = transactions.take(6),
            todayTransactions = todayTransactions,
            activeGoal = activeGoals.firstOrNull()?.toDomain(),
            upcomingBills = upcomingBills.take(3).map { it.toDomain() },
            upcomingRecurring = activeRecurring.take(3).map { it.toDomain(categoryMap, accountMap) },
            spendingInsights = insights
        )
    }.flowOn(Dispatchers.IO)

    private fun generateInsights(
        allTransactions: List<TransactionItem>,
        monthIncome: Long,
        monthExpense: Long,
        categories: List<CategoryEntity>
    ): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()
        val startOfMonth = DateUtils.getStartOfMonth()
        val endOfMonth = DateUtils.getEndOfMonth()
        val currentMonthExpenses = allTransactions.filter {
            it.type == TransactionType.EXPENSE && it.dateTime in startOfMonth..endOfMonth
        }

        if (currentMonthExpenses.isEmpty()) {
            insights.add(
                FinancialInsight(
                    title = "Start Tracking",
                    message = "Add your daily expenses to unlock personalized spending insights and budget alerts.",
                    type = InsightType.INFO
                )
            )
            return insights
        }

        // 1. Savings rate insight
        if (monthIncome > 0) {
            val savingsRate = (((monthIncome - monthExpense).toDouble() / monthIncome.toDouble()) * 100).toInt()
            if (savingsRate > 20) {
                insights.add(
                    FinancialInsight(
                        title = "Strong Savings Rate",
                        message = "You saved $savingsRate% of your income this month. Great financial discipline!",
                        type = InsightType.SUCCESS
                    )
                )
            } else if (savingsRate < 0) {
                insights.add(
                    FinancialInsight(
                        title = "Expenses Exceed Income",
                        message = "You've spent more than your recorded income this month. Consider reviewing non-essential spending.",
                        type = InsightType.WARNING
                    )
                )
            }
        }

        // 2. Highest Category Insight
        val categorySpends = currentMonthExpenses.groupBy { it.categoryName }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        val highest = categorySpends.maxByOrNull { it.value }
        if (highest != null && monthExpense > 0) {
            val pct = ((highest.value.toDouble() / monthExpense.toDouble()) * 100).toInt()
            insights.add(
                FinancialInsight(
                    title = "Top Spending Category",
                    message = "${highest.key} is your highest spending category this month, accounting for $pct% of expenses.",
                    type = InsightType.INFO
                )
            )
        }

        // 3. Month over Month comparison
        val startOfLastMonth = DateUtils.getStartOfLastMonth()
        val endOfLastMonth = DateUtils.getEndOfLastMonth()
        val lastMonthExpense = allTransactions.filter {
            it.type == TransactionType.EXPENSE && it.dateTime in startOfLastMonth..endOfLastMonth
        }.sumOf { it.amount }

        if (lastMonthExpense > 0) {
            val diff = monthExpense - lastMonthExpense
            if (diff > 0) {
                val pctIncrease = ((diff.toDouble() / lastMonthExpense.toDouble()) * 100).toInt()
                insights.add(
                    FinancialInsight(
                        title = "Spending Trend",
                        message = "Your spending increased by $pctIncrease% compared with last month.",
                        type = InsightType.WARNING
                    )
                )
            } else {
                val pctDecrease = ((-diff.toDouble() / lastMonthExpense.toDouble()) * 100).toInt()
                insights.add(
                    FinancialInsight(
                        title = "Spending Reduced",
                        message = "Your spending is $pctDecrease% lower than last month. Keep up the good momentum!",
                        type = InsightType.SUCCESS
                    )
                )
            }
        }

        return insights
    }

    // ----------------------------------------------------
    // Recurring Transactions
    // ----------------------------------------------------
    val recurringTransactionsFlow: Flow<List<RecurringTransactionItem>> = combine(
        recurringDao.getAllRecurringFlow(),
        categoryDao.getAllCategoriesFlow(),
        accountDao.getAllAccountsFlow()
    ) { recurringList, categories, accounts ->
        val categoryMap = categories.associateBy { it.id }
        val accountMap = accounts.associateBy { it.id }
        recurringList.map { it.toDomain(categoryMap, accountMap) }
    }.flowOn(Dispatchers.IO)

    suspend fun saveRecurringTransaction(
        id: Long,
        name: String,
        amount: Long,
        type: TransactionType,
        categoryId: Long,
        accountId: Long,
        frequency: String,
        startDate: Long,
        nextDueDate: Long,
        note: String
    ) = withContext(Dispatchers.IO) {
        val entity = RecurringTransactionEntity(
            id = id,
            name = name,
            amount = amount,
            type = type.name,
            categoryId = categoryId,
            accountId = accountId,
            frequency = frequency,
            startDate = startDate,
            nextDueDate = nextDueDate,
            note = note
        )
        if (id == 0L) {
            recurringDao.insertRecurring(entity)
        } else {
            recurringDao.updateRecurring(entity)
        }
    }

    suspend fun deleteRecurringTransaction(id: Long) = withContext(Dispatchers.IO) {
        val entity = recurringDao.getRecurringById(id) ?: return@withContext
        recurringDao.deleteRecurring(entity)
    }

    // ----------------------------------------------------
    // Savings Goals
    // ----------------------------------------------------
    val savingsGoalsFlow: Flow<List<SavingsGoalItem>> = savingsGoalDao.getAllGoalsFlow().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun saveSavingsGoal(
        id: Long,
        name: String,
        targetAmount: Long,
        savedAmount: Long,
        targetDate: Long,
        icon: String = "Savings",
        color: String = "#8B5CF6",
        notes: String = ""
    ) = withContext(Dispatchers.IO) {
        val entity = SavingsGoalEntity(
            id = id,
            name = name,
            targetAmount = targetAmount,
            savedAmount = savedAmount,
            targetDate = targetDate,
            icon = icon,
            color = color,
            isCompleted = savedAmount >= targetAmount && targetAmount > 0,
            notes = notes
        )
        if (id == 0L) {
            savingsGoalDao.insertGoal(entity)
        } else {
            savingsGoalDao.updateGoal(entity)
        }
    }

    suspend fun addMoneyToGoal(goalId: Long, amount: Long) = withContext(Dispatchers.IO) {
        val goal = savingsGoalDao.getGoalById(goalId) ?: return@withContext
        val newSaved = goal.savedAmount + amount
        savingsGoalDao.updateGoal(
            goal.copy(
                savedAmount = newSaved,
                isCompleted = newSaved >= goal.targetAmount
            )
        )
    }

    suspend fun withdrawMoneyFromGoal(goalId: Long, amount: Long) = withContext(Dispatchers.IO) {
        val goal = savingsGoalDao.getGoalById(goalId) ?: return@withContext
        val newSaved = (goal.savedAmount - amount).coerceAtLeast(0L)
        savingsGoalDao.updateGoal(
            goal.copy(
                savedAmount = newSaved,
                isCompleted = newSaved >= goal.targetAmount
            )
        )
    }

    suspend fun deleteSavingsGoal(id: Long) = withContext(Dispatchers.IO) {
        val goal = savingsGoalDao.getGoalById(id) ?: return@withContext
        savingsGoalDao.deleteGoal(goal)
    }

    // ----------------------------------------------------
    // Credit Cards
    // ----------------------------------------------------
    val creditCardsFlow: Flow<List<CreditCardItem>> = combine(
        creditCardDao.getAllCardsFlow(),
        accountDao.getAllAccountsFlow()
    ) { cards, accounts ->
        val accountMap = accounts.associateBy { it.id }
        cards.map { card ->
            val acc = accountMap[card.accountId]
            val balance = acc?.currentBalance ?: 0L
            val outstanding = if (balance < 0) -balance else 0L
            val available = (card.creditLimit - outstanding).coerceAtLeast(0L)
            CreditCardItem(
                id = card.id,
                accountId = card.accountId,
                cardName = card.cardName,
                bank = card.bank,
                creditLimit = card.creditLimit,
                currentOutstanding = outstanding,
                availableLimit = available,
                statementDay = card.statementDay,
                dueDay = card.dueDay,
                minDuePercentage = card.minDuePercentage
            )
        }
    }.flowOn(Dispatchers.IO)

    suspend fun saveCreditCard(
        id: Long,
        accountId: Long,
        cardName: String,
        bank: String,
        creditLimit: Long,
        statementDay: Int = 15,
        dueDay: Int = 5,
        minDuePercentage: Double = 5.0
    ) = withContext(Dispatchers.IO) {
        val entity = CreditCardEntity(
            id = id,
            accountId = accountId,
            cardName = cardName,
            bank = bank,
            creditLimit = creditLimit,
            statementDay = statementDay,
            dueDay = dueDay,
            minDuePercentage = minDuePercentage
        )
        if (id == 0L) {
            creditCardDao.insertCard(entity)
        } else {
            creditCardDao.updateCard(entity)
        }
    }

    // ----------------------------------------------------
    // Bill Reminders
    // ----------------------------------------------------
    val billRemindersFlow: Flow<List<BillReminderItem>> = billReminderDao.getAllRemindersFlow().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun saveBillReminder(
        id: Long,
        title: String,
        amount: Long,
        dueDate: Long,
        category: String = "Bills",
        repeatRule: String = "MONTHLY",
        notes: String = ""
    ) = withContext(Dispatchers.IO) {
        val entity = BillReminderEntity(
            id = id,
            title = title,
            amount = amount,
            dueDate = dueDate,
            category = category,
            repeatRule = repeatRule,
            notes = notes
        )
        if (id == 0L) {
            billReminderDao.insertReminder(entity)
        } else {
            billReminderDao.updateReminder(entity)
        }
    }

    suspend fun markBillPaid(id: Long, isPaid: Boolean) = withContext(Dispatchers.IO) {
        val reminder = billReminderDao.getReminderById(id) ?: return@withContext
        billReminderDao.updateReminder(reminder.copy(isPaid = isPaid))
    }

    suspend fun deleteBillReminder(id: Long) = withContext(Dispatchers.IO) {
        val reminder = billReminderDao.getReminderById(id) ?: return@withContext
        billReminderDao.deleteReminder(reminder)
    }

    // ----------------------------------------------------
    // Seed Sample / Demo Data for Testing
    // ----------------------------------------------------
    suspend fun seedDemoData() = withContext(Dispatchers.IO) {
        // Creates a realistic spread of past transactions for beautiful charts & testing
        val now = System.currentTimeMillis()
        val oneDay = 86400000L

        val demoTx = listOf(
            TransactionEntity(type = "INCOME", amount = 4500000L, categoryId = 13L, accountId = 2L, dateTime = now - oneDay * 5, note = "Monthly Salary", merchant = "Company Corp"),
            TransactionEntity(type = "EXPENSE", amount = 35000L, categoryId = 1L, accountId = 3L, dateTime = now, note = "Dinner with team", merchant = "Biryani House"),
            TransactionEntity(type = "EXPENSE", amount = 12000L, categoryId = 2L, accountId = 1L, dateTime = now, note = "Auto fare", merchant = "City Auto"),
            TransactionEntity(type = "EXPENSE", amount = 249900L, categoryId = 3L, accountId = 2L, dateTime = now - oneDay * 1, note = "Running shoes", merchant = "Nike Store"),
            TransactionEntity(type = "EXPENSE", amount = 85000L, categoryId = 4L, accountId = 3L, dateTime = now - oneDay * 2, note = "Electricity Bill", merchant = "State Electricity"),
            TransactionEntity(type = "EXPENSE", amount = 1500000L, categoryId = 5L, accountId = 2L, dateTime = now - oneDay * 4, note = "Apartment Rent", merchant = "Landlord"),
            TransactionEntity(type = "EXPENSE", amount = 64900L, categoryId = 6L, accountId = 3L, dateTime = now - oneDay * 3, note = "Netflix Subscription", merchant = "Netflix"),
            TransactionEntity(type = "INCOME", amount = 1500000L, categoryId = 14L, accountId = 2L, dateTime = now - oneDay * 10, note = "Mobile App Design", merchant = "Freelance Client"),
            TransactionEntity(type = "EXPENSE", amount = 120000L, categoryId = 7L, accountId = 1L, dateTime = now - oneDay * 8, note = "Medicines & Checkup", merchant = "Apollo Pharmacy"),
            TransactionEntity(type = "TRANSFER", amount = 500000L, accountId = 2L, toAccountId = 1L, dateTime = now - oneDay * 6, note = "ATM Cash Withdrawal", merchant = "SBI ATM")
        )
        transactionDao.insertTransactions(demoTx)

        // Add a demo goal
        savingsGoalDao.insertGoal(
            SavingsGoalEntity(
                name = "New Phone",
                targetAmount = 4000000L,
                savedAmount = 1850000L,
                targetDate = now + oneDay * 60,
                icon = "Smartphone",
                color = "#3B82F6",
                notes = "Flagship upgrade"
            )
        )

        // Add bill reminders
        billReminderDao.insertReminder(
            BillReminderEntity(
                title = "Netflix Subscription",
                amount = 64900L,
                dueDate = now + oneDay * 1,
                category = "Entertainment",
                notes = "Auto-debit"
            )
        )
        billReminderDao.insertReminder(
            BillReminderEntity(
                title = "Internet Broadband",
                amount = 99900L,
                dueDate = now + oneDay * 5,
                category = "Bills",
                notes = "Airtel Xstream"
            )
        )
    }

    // ----------------------------------------------------
    // Mapping Extensions
    // ----------------------------------------------------
    private fun UserProfileEntity.toDomain() = UserProfile(
        id = id,
        name = name,
        currencySymbol = currencySymbol,
        currencyCode = currencyCode,
        isOnboarded = isOnboarded,
        appLockEnabled = appLockEnabled,
        pinHash = pinHash,
        biometricEnabled = biometricEnabled,
        themeMode = themeMode,
        budgetAlertsEnabled = budgetAlertsEnabled,
        billRemindersEnabled = billRemindersEnabled
    )

    private fun UserProfile.toEntity() = UserProfileEntity(
        id = id,
        name = name,
        currencySymbol = currencySymbol,
        currencyCode = currencyCode,
        isOnboarded = isOnboarded,
        appLockEnabled = appLockEnabled,
        pinHash = pinHash,
        biometricEnabled = biometricEnabled,
        themeMode = themeMode,
        budgetAlertsEnabled = budgetAlertsEnabled,
        billRemindersEnabled = billRemindersEnabled
    )

    private fun AccountEntity.toDomain() = Account(
        id = id,
        name = name,
        type = type,
        openingBalance = openingBalance,
        currentBalance = currentBalance,
        icon = icon,
        color = color,
        isActive = isActive,
        sortOrder = sortOrder
    )

    private fun Account.toEntity() = AccountEntity(
        id = id,
        name = name,
        type = type,
        openingBalance = openingBalance,
        currentBalance = currentBalance,
        icon = icon,
        color = color,
        isActive = isActive,
        sortOrder = sortOrder
    )

    private fun CategoryEntity.toDomain() = Category(
        id = id,
        name = name,
        type = type,
        icon = icon,
        color = color,
        isActive = isActive,
        sortOrder = sortOrder
    )

    private fun Category.toEntity() = CategoryEntity(
        id = id,
        name = name,
        type = type,
        icon = icon,
        color = color,
        isActive = isActive,
        sortOrder = sortOrder
    )

    private fun TransactionEntity.toDomain(
        categoryMap: Map<Long, CategoryEntity>,
        accountMap: Map<Long, AccountEntity>
    ): TransactionItem {
        val cat = categoryId?.let { categoryMap[it] }
        val acc = accountMap[accountId]
        val toAcc = toAccountId?.let { accountMap[it] }

        return TransactionItem(
            id = id,
            type = TransactionType.valueOf(type),
            amount = amount,
            categoryId = categoryId,
            categoryName = cat?.name ?: if (type == "TRANSFER") "Transfer" else "General",
            categoryIcon = cat?.icon ?: if (type == "TRANSFER") "SwapHoriz" else "Category",
            categoryColor = cat?.color ?: if (type == "TRANSFER") "#3B82F6" else "#64748B",
            accountId = accountId,
            accountName = acc?.name ?: "Account",
            accountIcon = acc?.icon ?: "AccountBalance",
            toAccountId = toAccountId,
            toAccountName = toAcc?.name,
            dateTime = dateTime,
            note = note,
            merchant = merchant,
            location = location,
            receiptUri = receiptUri,
            tags = tags,
            createdAt = createdAt
        )
    }

    private fun SavingsGoalEntity.toDomain() = SavingsGoalItem(
        id = id,
        name = name,
        targetAmount = targetAmount,
        savedAmount = savedAmount,
        targetDate = targetDate,
        icon = icon,
        color = color,
        isCompleted = isCompleted,
        notes = notes
    )

    private fun BillReminderEntity.toDomain() = BillReminderItem(
        id = id,
        title = title,
        amount = amount,
        dueDate = dueDate,
        category = category,
        repeatRule = repeatRule,
        isPaid = isPaid,
        isEnabled = isEnabled,
        notes = notes
    )

    private fun RecurringTransactionEntity.toDomain(
        categoryMap: Map<Long, CategoryEntity>,
        accountMap: Map<Long, AccountEntity>
    ) = RecurringTransactionItem(
        id = id,
        name = name,
        amount = amount,
        type = TransactionType.valueOf(type),
        categoryId = categoryId,
        categoryName = categoryMap[categoryId]?.name ?: "General",
        accountId = accountId,
        accountName = accountMap[accountId]?.name ?: "Account",
        frequency = frequency,
        startDate = startDate,
        nextDueDate = nextDueDate,
        lastProcessedDate = lastProcessedDate,
        isActive = isActive,
        note = note
    )
}
