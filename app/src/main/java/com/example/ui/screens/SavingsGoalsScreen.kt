package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.SavingsGoalItem
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.EmptyState
import com.example.ui.theme.FinanceExpense
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.DateUtils
import com.example.utils.MoneyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val goals by viewModel.savingsGoals.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedGoalForMoneyAction by remember { mutableStateOf<SavingsGoalItem?>(null) }
    var isDepositAction by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Savings Goals", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_savings_goal_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Goal")
            }
        },
        modifier = modifier.testTag("savings_goals_screen")
    ) { innerPadding ->
        if (goals.isEmpty()) {
            EmptyState(
                title = "No savings goals",
                description = "Set goals for Emergency Fund, New Gadgets, Vacation, or Vehicles.",
                icon = Icons.Default.Savings,
                actionText = "+ Create Savings Goal",
                onActionClick = { showAddDialog = true },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(goals, key = { it.id }) { goal ->
                    GoalCardItem(
                        goal = goal,
                        currencySymbol = profile.currencySymbol,
                        onAddMoney = {
                            selectedGoalForMoneyAction = goal
                            isDepositAction = true
                        },
                        onWithdrawMoney = {
                            selectedGoalForMoneyAction = goal
                            isDepositAction = false
                        },
                        onDelete = { viewModel.deleteSavingsGoal(goal.id) {} }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddGoalDialog(
                currencySymbol = profile.currencySymbol,
                onDismiss = { showAddDialog = false },
                onSave = { name, targetAmount, notes ->
                    viewModel.saveSavingsGoal(
                        id = 0L,
                        name = name,
                        targetAmount = targetAmount,
                        savedAmount = 0L,
                        targetDate = System.currentTimeMillis() + 86400000L * 180,
                        notes = notes,
                        onSuccess = { showAddDialog = false }
                    )
                }
            )
        }

        selectedGoalForMoneyAction?.let { goal ->
            MoneyAdjustmentDialog(
                goalName = goal.name,
                isDeposit = isDepositAction,
                currencySymbol = profile.currencySymbol,
                onDismiss = { selectedGoalForMoneyAction = null },
                onConfirm = { amount ->
                    if (isDepositAction) {
                        viewModel.addMoneyToGoal(goal.id, amount) { selectedGoalForMoneyAction = null }
                    } else {
                        viewModel.withdrawMoneyFromGoal(goal.id, amount) { selectedGoalForMoneyAction = null }
                    }
                }
            )
        }
    }
}

@Composable
private fun GoalCardItem(
    goal: SavingsGoalItem,
    currencySymbol: String,
    onAddMoney: () -> Unit,
    onWithdrawMoney: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("savings_goal_${goal.name}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIconBadge(
                        iconName = goal.icon,
                        colorHex = goal.color,
                        size = 40.dp,
                        iconSize = 22.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = goal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (goal.targetDate > 0L) {
                            Text(
                                text = "Target: ${DateUtils.formatDate(goal.targetDate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${goal.progressPercentage}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = FinanceExpense, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val progressFraction = if (goal.targetAmount > 0) (goal.savedAmount.toFloat() / goal.targetAmount.toFloat()).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Saved: ${MoneyUtils.formatCurrency(goal.savedAmount, currencySymbol)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Remaining: ${MoneyUtils.formatCurrency(goal.remainingAmount, currencySymbol)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAddMoney,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Money")
                }

                OutlinedButton(
                    onClick = onWithdrawMoney,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Withdraw")
                }
            }
        }
    }
}

@Composable
private fun AddGoalDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (String, Long, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Savings Goal", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name") },
                    placeholder = { Text("e.g. New iPhone, Emergency Fund") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { targetStr = it },
                    label = { Text("Target Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
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
                val minor = MoneyUtils.parseToMinorUnits(targetStr)
                if (name.isBlank() || minor <= 0L) {
                    error = "Name and target amount must be valid."
                    return@Button
                }
                onSave(name.trim(), minor, notes.trim())
            }) {
                Text("Create Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun MoneyAdjustmentDialog(
    goalName: String,
    isDeposit: Boolean,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isDeposit) "Add Money to $goalName" else "Withdraw from $goalName", fontWeight = FontWeight.Bold) },
        text = {
            Column {
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
                if (minor <= 0L) {
                    error = "Amount must be greater than zero."
                    return@Button
                }
                onConfirm(minor)
            }) {
                Text(if (isDeposit) "Deposit" else "Withdraw")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
