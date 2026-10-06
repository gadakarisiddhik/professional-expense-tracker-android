package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.AppDatabase
import com.example.data.repository.ExpenseTrackerRepository
import com.example.ui.components.QuickAddDialog
import com.example.ui.navigation.Screen
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.AddExpenseScreen
import com.example.ui.screens.AddIncomeScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.CreditCardsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ReceiptScannerScreen
import com.example.ui.screens.RecurringScreen
import com.example.ui.screens.ReportsExportScreen
import com.example.ui.screens.SavingsGoalsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionDetailScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.screens.TransferScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ViewModelFactory
import com.example.utils.NotificationHelper

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.initNotificationChannels(this)

        val database = AppDatabase.getInstance(this)
        val repository = ExpenseTrackerRepository(database)
        val factory = ViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()

            // Request Notification Permission on Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = {}
            )

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            val darkTheme = when (userProfile.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                when {
                    !userProfile.isOnboarded -> {
                        OnboardingScreen(
                            viewModel = viewModel,
                            onComplete = { /* Onboarding updates profile flow and auto switches */ }
                        )
                    }
                    isAppLocked -> {
                        AppLockScreen(
                            viewModel = viewModel,
                            onUnlocked = { /* ViewModel state updates to false and unlocks */ }
                        )
                    }
                    else -> {
                        ExpenseTrackerApp(viewModel = viewModel, database = database)
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseTrackerApp(
    viewModel: MainViewModel,
    database: AppDatabase
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showQuickAddDialog by remember { mutableStateOf(false) }
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    val bottomBarRoutes = listOf(
        Screen.Home.route,
        Screen.Analytics.route,
        Screen.Transactions.route,
        Screen.Settings.route
    )

    val shouldShowBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Home.route,
                        onClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        modifier = Modifier.testTag("nav_home")
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.Analytics.route,
                        onClick = {
                            navController.navigate(Screen.Analytics.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.PieChart, contentDescription = "Analytics") },
                        label = { Text("Analytics") },
                        modifier = Modifier.testTag("nav_analytics")
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.Transactions.route,
                        onClick = {
                            navController.navigate(Screen.Transactions.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Transactions") },
                        label = { Text("Transactions") },
                        modifier = Modifier.testTag("nav_transactions")
                    )

                    NavigationBarItem(
                        selected = currentRoute == Screen.Settings.route,
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        modifier = Modifier.testTag("nav_settings")
                    )
                }
            }
        },
        floatingActionButton = {
            if (shouldShowBottomBar) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddExpense.route) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("main_add_expense_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToAddExpense = { navController.navigate(Screen.AddExpense.route) },
                        onNavigateToAddIncome = { navController.navigate(Screen.AddIncome.route) },
                        onNavigateToTransfer = { navController.navigate(Screen.Transfer.route) },
                        onNavigateToBudget = { navController.navigate(Screen.Budget.route) },
                        onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                        onNavigateToTransactionDetail = { id ->
                            navController.navigate(Screen.TransactionDetail.createRoute(id))
                        },
                        onNavigateToSavingsGoals = { navController.navigate(Screen.SavingsGoals.route) },
                        onNavigateToReceiptScanner = { navController.navigate(Screen.ReceiptScanner.route) },
                        onOpenQuickAdd = { showQuickAddDialog = true }
                    )
                }

                composable(Screen.Analytics.route) {
                    AnalyticsScreen(viewModel = viewModel)
                }

                composable(Screen.Transactions.route) {
                    TransactionsScreen(
                        viewModel = viewModel,
                        onNavigateToDetail = { id ->
                            navController.navigate(Screen.TransactionDetail.createRoute(id))
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        database = database,
                        onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
                        onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                        onNavigateToBudget = { navController.navigate(Screen.Budget.route) },
                        onNavigateToRecurring = { navController.navigate(Screen.Recurring.route) },
                        onNavigateToSavingsGoals = { navController.navigate(Screen.SavingsGoals.route) },
                        onNavigateToCreditCards = { navController.navigate(Screen.CreditCards.route) },
                        onNavigateToReports = { navController.navigate(Screen.ReportsExport.route) }
                    )
                }

                composable(Screen.AddExpense.route) {
                    AddExpenseScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.AddIncome.route) {
                    AddIncomeScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Transfer.route) {
                    TransferScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.TransactionDetail.route,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getLong("id") ?: 0L
                    TransactionDetailScreen(
                        transactionId = id,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Budget.route) {
                    BudgetScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Accounts.route) {
                    AccountsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToTransfer = { navController.navigate(Screen.Transfer.route) }
                    )
                }

                composable(Screen.Categories.route) {
                    CategoriesScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Recurring.route) {
                    RecurringScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.SavingsGoals.route) {
                    SavingsGoalsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.CreditCards.route) {
                    CreditCardsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToTransfer = { navController.navigate(Screen.Transfer.route) }
                    )
                }

                composable(Screen.ReportsExport.route) {
                    ReportsExportScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.ReceiptScanner.route) {
                    ReceiptScannerScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            if (showQuickAddDialog) {
                QuickAddDialog(
                    viewModel = viewModel,
                    currencySymbol = profile.currencySymbol,
                    onDismiss = { showQuickAddDialog = false },
                    onSuccess = { showQuickAddDialog = false }
                )
            }
        }
    }
}
