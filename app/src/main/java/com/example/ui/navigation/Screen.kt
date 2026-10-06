package com.example.ui.navigation

sealed class Screen(val route: String) {
    // Bottom Bar tabs
    object Home : Screen("home")
    object Analytics : Screen("analytics")
    object Transactions : Screen("transactions")
    object Settings : Screen("settings")

    // Primary action flows
    object AddExpense : Screen("add_expense")
    object AddIncome : Screen("add_income")
    object Transfer : Screen("transfer")
    object TransactionDetail : Screen("transaction_detail/{id}") {
        fun createRoute(id: Long) = "transaction_detail/$id"
    }

    // Secondary feature screens
    object Budget : Screen("budget")
    object Accounts : Screen("accounts")
    object Categories : Screen("categories")
    object Recurring : Screen("recurring")
    object SavingsGoals : Screen("savings_goals")
    object CreditCards : Screen("credit_cards")
    object ReportsExport : Screen("reports_export")
    object ReceiptScanner : Screen("receipt_scanner")

    // Security & Onboarding
    object Onboarding : Screen("onboarding")
    object AppLock : Screen("app_lock")
}
