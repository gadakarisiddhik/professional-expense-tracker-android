package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.BillReminderItem
import com.example.domain.model.RecurringTransactionItem
import com.example.domain.model.TransactionType
import com.example.ui.components.EmptyState
import com.example.ui.theme.FinanceExpense
import com.example.ui.theme.FinanceIncome
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.DateUtils
import com.example.utils.MoneyUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val recurringList by viewModel.recurringTransactions.collectAsStateWithLifecycle()
    val reminders by viewModel.billReminders.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val accounts by viewModel.activeAccounts.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Recurring, 1: Bill Reminders
    var showAddRecurringDialog by remember { mutableStateOf(false) }
    var showAddReminderDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring & Reminders", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showAddRecurringDialog = true else showAddReminderDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_recurring_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item")
            }
        },
        modifier = modifier.testTag("recurring_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Recurring (${recurringList.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Bill Reminders (${reminders.size})", fontWeight = FontWeight.SemiBold) }
                )
            }

            if (selectedTab == 0) {
                if (recurringList.isEmpty()) {
                    EmptyState(
                        title = "No recurring transactions",
                        description = "Add recurring payments like Rent, Subscriptions, EMI, Gym, and Internet.",
                        icon = Icons.Default.Repeat,
                        actionText = "+ Add Recurring",
                        onActionClick = { showAddRecurringDialog = true }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recurringList, key = { it.id }) { item ->
                            RecurringCard(
                                item = item,
                                currencySymbol = profile.currencySymbol,
                                onDelete = { viewModel.deleteRecurringTransaction(item.id) {} }
                            )
                        }
                    }
                }
            } else {
                if (reminders.isEmpty()) {
                    EmptyState(
                        title = "No bill reminders",
                        description = "Set reminders for credit card bills, utility payments, and subscriptions.",
                        icon = Icons.Default.Notifications,
                        actionText = "+ Add Reminder",
                        onActionClick = { showAddReminderDialog = true }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(reminders, key = { it.id }) { reminder ->
                            BillReminderCard(
                                reminder = reminder,
                                currencySymbol = profile.currencySymbol,
                                onTogglePaid = { viewModel.markBillPaid(reminder.id, !reminder.isPaid) },
                                onDelete = { viewModel.deleteBillReminder(reminder.id) {} }
                            )
                        }
                    }
                }
            }
        }

        if (showAddRecurringDialog) {
            AddRecurringDialog(
                categories = categories.filter { it.type == "EXPENSE" },
                accounts = accounts,
                currencySymbol = profile.currencySymbol,
                onDismiss = { showAddRecurringDialog = false },
                onSave = { name, amount, catId, accId, freq, note ->
                    viewModel.saveRecurringTransaction(
                        id = 0L,
                        name = name,
                        amount = amount,
                        type = TransactionType.EXPENSE,
                        categoryId = catId,
                        accountId = accId,
                        frequency = freq,
                        startDate = System.currentTimeMillis(),
                        nextDueDate = System.currentTimeMillis() + 86400000L * 30,
                        note = note,
                        onSuccess = { showAddRecurringDialog = false }
                    )
                }
            )
        }

        if (showAddReminderDialog) {
            AddReminderDialog(
                currencySymbol = profile.currencySymbol,
                onDismiss = { showAddReminderDialog = false },
                onSave = { title, amount, dueDate, category ->
                    viewModel.saveBillReminder(
                        id = 0L,
                        title = title,
                        amount = amount,
                        dueDate = dueDate,
                        category = category,
                        repeatRule = "MONTHLY",
                        notes = "",
                        onSuccess = { showAddReminderDialog = false }
                    )
                }
            )
        }
    }
}

@Composable
private fun RecurringCard(
    item: RecurringTransactionItem,
    currencySymbol: String,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("recurring_item_${item.name}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.frequency} • ${item.categoryName} • ${item.accountName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Next Due: ${DateUtils.formatDate(item.nextDueDate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = MoneyUtils.formatCurrency(item.amount, currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FinanceExpense
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = FinanceExpense, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun BillReminderCard(
    reminder: BillReminderItem,
    currencySymbol: String,
    onTogglePaid: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isPaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("reminder_item_${reminder.title}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = reminder.isPaid, onCheckedChange = { onTogglePaid() })

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Due: ${DateUtils.formatDate(reminder.dueDate)} • ${reminder.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = MoneyUtils.formatCurrency(reminder.amount, currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (reminder.isPaid) FinanceIncome else FinanceExpense
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = FinanceExpense, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AddRecurringDialog(
    categories: List<com.example.domain.model.Category>,
    accounts: List<com.example.domain.model.Account>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (String, Long, Long, Long, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 1L) }
    var selectedAccId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 1L) }
    var frequency by remember { mutableStateOf("MONTHLY") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Recurring Payment", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Payment Name") },
                    placeholder = { Text("e.g. Netflix, Gym, Rent") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error!!, color = FinanceExpense, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val minor = MoneyUtils.parseToMinorUnits(amountStr)
                if (name.isBlank() || minor <= 0L) {
                    error = "Valid name and amount required."
                    return@Button
                }
                onSave(name.trim(), minor, selectedCatId, selectedAccId, frequency, note.trim())
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddReminderDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (String, Long, Long, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bill Reminder", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Bill Title") },
                    placeholder = { Text("e.g. Electricity, Wifi, Credit Card") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error!!, color = FinanceExpense, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val minor = MoneyUtils.parseToMinorUnits(amountStr)
                if (title.isBlank() || minor <= 0L) {
                    error = "Valid title and amount required."
                    return@Button
                }
                onSave(title.trim(), minor, System.currentTimeMillis() + 86400000L * 7, "Bills")
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
