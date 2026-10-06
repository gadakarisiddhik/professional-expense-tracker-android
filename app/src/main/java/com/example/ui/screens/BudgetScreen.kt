package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.PieChart
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.domain.model.BudgetWithStatus
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.EmptyState
import com.example.ui.theme.FinanceExpense
import com.example.ui.theme.FinanceWarning
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.MoneyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()

    val expenseCategories = remember(allCategories) {
        allCategories.filter { it.type == "EXPENSE" && it.isActive }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetWithStatus?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Management", fontWeight = FontWeight.Bold) },
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
                modifier = Modifier.testTag("add_budget_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Budget")
            }
        },
        modifier = modifier.testTag("budget_screen")
    ) { innerPadding ->
        if (budgets.isEmpty()) {
            EmptyState(
                title = "No Budgets Set",
                description = "Set overall or category-level monthly budgets to stay in control of your spending.",
                icon = Icons.Default.PieChart,
                actionText = "+ Create Budget",
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
                items(budgets, key = { it.id }) { budget ->
                    BudgetCardItem(
                        budget = budget,
                        currencySymbol = profile.currencySymbol,
                        onEdit = { editingBudget = budget },
                        onDelete = { viewModel.deleteBudget(budget.id) {} }
                    )
                }
            }
        }

        if (showAddDialog || editingBudget != null) {
            val budgetToEdit = editingBudget
            BudgetDialog(
                initialName = budgetToEdit?.name ?: "",
                initialLimitStr = budgetToEdit?.let { MoneyUtils.toDecimalString(it.limitAmount) } ?: "",
                initialCategoryId = budgetToEdit?.categoryId,
                categories = expenseCategories,
                currencySymbol = profile.currencySymbol,
                onDismiss = {
                    showAddDialog = false
                    editingBudget = null
                },
                onSave = { name, limitAmount, categoryId ->
                    viewModel.saveBudget(
                        id = budgetToEdit?.id ?: 0L,
                        name = name,
                        categoryId = categoryId,
                        limitAmount = limitAmount,
                        onSuccess = {
                            showAddDialog = false
                            editingBudget = null
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun BudgetCardItem(
    budget: BudgetWithStatus,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progressFraction = (budget.spentAmount.toFloat() / budget.limitAmount.toFloat()).coerceIn(0f, 1f)
    val statusColor = if (budget.isExceeded) FinanceExpense else if (budget.isWarning) FinanceWarning else MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("budget_card_${budget.name}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (budget.categoryIcon != null && budget.categoryColor != null) {
                        CategoryIconBadge(
                            iconName = budget.categoryIcon,
                            colorHex = budget.categoryColor,
                            size = 38.dp,
                            iconSize = 20.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Column {
                        Text(text = budget.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "${budget.period.lowercase().replaceFirstChar { it.uppercase() }} Budget", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = FinanceExpense, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = MoneyUtils.formatCurrency(budget.spentAmount, currencySymbol),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Utilization", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${budget.percentage}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = MoneyUtils.formatCurrency(budget.remainingAmount, currencySymbol),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (budget.remainingAmount > 0) MaterialTheme.colorScheme.primary else FinanceExpense
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetDialog(
    initialName: String,
    initialLimitStr: String,
    initialCategoryId: Long?,
    categories: List<com.example.domain.model.Category>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (String, Long, Long?) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var limitStr by remember { mutableStateOf(initialLimitStr) }
    var selectedCategoryId by remember { mutableStateOf(initialCategoryId) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialName.isBlank()) "Create Budget" else "Edit Budget", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Budget Name") },
                    placeholder = { Text("e.g. Monthly Budget or Food Budget") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = { Text("Limit Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Budget Scope:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    val isOverall = selectedCategoryId == null
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedCategoryId = null },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isOverall) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Overall Monthly",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isOverall) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error!!, color = FinanceExpense, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val minor = MoneyUtils.parseToMinorUnits(limitStr)
                if (name.isBlank()) {
                    error = "Name cannot be empty."
                    return@Button
                }
                if (minor <= 0L) {
                    error = "Limit must be greater than zero."
                    return@Button
                }
                onSave(name.trim(), minor, selectedCategoryId)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
