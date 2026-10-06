package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.theme.FinanceExpense
import com.example.ui.theme.FinanceIncome
import com.example.ui.theme.FinanceTransfer
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.DateUtils
import com.example.utils.MoneyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val transaction = remember(allTransactions, transactionId) {
        allTransactions.find { it.id == transactionId }
    }

    var isEditMode by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Edit fields
    var editAmountStr by remember { mutableStateOf("") }
    var editMerchant by remember { mutableStateOf("") }
    var editNote by remember { mutableStateOf("") }
    var editTags by remember { mutableStateOf("") }

    LaunchedEffect(transaction) {
        transaction?.let {
            editAmountStr = MoneyUtils.toDecimalString(it.amount)
            editMerchant = it.merchant
            editNote = it.note
            editTags = it.tags
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Transaction" else "Transaction Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (transaction != null) {
                        if (!isEditMode) {
                            IconButton(onClick = {
                                viewModel.duplicateTransaction(transaction) {
                                    onNavigateBack()
                                }
                            }) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Duplicate")
                            }
                            IconButton(onClick = { isEditMode = true }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = FinanceExpense)
                            }
                        } else {
                            IconButton(onClick = {
                                val minorUnits = MoneyUtils.parseToMinorUnits(editAmountStr)
                                if (minorUnits > 0L) {
                                    viewModel.updateTransaction(
                                        id = transaction.id,
                                        type = transaction.type,
                                        amount = minorUnits,
                                        categoryId = transaction.categoryId,
                                        accountId = transaction.accountId,
                                        toAccountId = transaction.toAccountId,
                                        dateTime = transaction.dateTime,
                                        note = editNote.trim(),
                                        merchant = editMerchant.trim(),
                                        location = transaction.location,
                                        receiptUri = transaction.receiptUri,
                                        tags = editTags.trim(),
                                        onSuccess = { isEditMode = false }
                                    )
                                }
                            }) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            )
        },
        modifier = modifier.testTag("transaction_detail_screen")
    ) { innerPadding ->
        if (transaction == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Transaction not found or deleted.", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        val typeColor = when (transaction.type) {
            TransactionType.EXPENSE -> FinanceExpense
            TransactionType.INCOME -> FinanceIncome
            TransactionType.TRANSFER -> FinanceTransfer
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Amount & Type Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = typeColor.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CategoryIconBadge(
                        iconName = transaction.categoryIcon,
                        colorHex = transaction.categoryColor,
                        size = 64.dp,
                        iconSize = 32.dp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isEditMode) {
                        OutlinedTextField(
                            value = editAmountStr,
                            onValueChange = { editAmountStr = it },
                            label = { Text("Amount") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.width(200.dp)
                        )
                    } else {
                        val formattedAmount = when (transaction.type) {
                            TransactionType.EXPENSE -> MoneyUtils.formatWithSign(transaction.amount, profile.currencySymbol, isIncome = false)
                            TransactionType.INCOME -> MoneyUtils.formatWithSign(transaction.amount, profile.currencySymbol, isIncome = true)
                            TransactionType.TRANSFER -> MoneyUtils.formatCurrency(transaction.amount, profile.currencySymbol)
                        }

                        Text(
                            text = formattedAmount,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = typeColor
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = transaction.type.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Details Table Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DetailRow("Category", transaction.categoryName)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    DetailRow("Account", transaction.accountName)
                    if (transaction.toAccountName != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                        DetailRow("To Account", transaction.toAccountName)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    DetailRow("Date", DateUtils.formatDate(transaction.dateTime))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    DetailRow("Time", DateUtils.formatTime(transaction.dateTime))

                    if (isEditMode) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                        OutlinedTextField(
                            value = editMerchant,
                            onValueChange = { editMerchant = it },
                            label = { Text("Merchant / Store") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editNote,
                            onValueChange = { editNote = it },
                            label = { Text("Note") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editTags,
                            onValueChange = { editTags = it },
                            label = { Text("Tags") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        if (transaction.merchant.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                            DetailRow("Merchant", transaction.merchant)
                        }

                        if (transaction.note.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                            DetailRow("Note", transaction.note)
                        }

                        if (transaction.tags.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                            DetailRow("Tags", transaction.tags)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isEditMode) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FinanceExpense)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Transaction")
                }
            } else {
                Button(
                    onClick = {
                        val minorUnits = MoneyUtils.parseToMinorUnits(editAmountStr)
                        if (minorUnits > 0L) {
                            viewModel.updateTransaction(
                                id = transaction.id,
                                type = transaction.type,
                                amount = minorUnits,
                                categoryId = transaction.categoryId,
                                accountId = transaction.accountId,
                                toAccountId = transaction.toAccountId,
                                dateTime = transaction.dateTime,
                                note = editNote.trim(),
                                merchant = editMerchant.trim(),
                                location = transaction.location,
                                receiptUri = transaction.receiptUri,
                                tags = editTags.trim(),
                                onSuccess = { isEditMode = false }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showDeleteDialog) {
            DeleteConfirmationDialog(
                title = "Delete Transaction?",
                message = "Are you sure you want to delete this ${transaction.categoryName} transaction of ${MoneyUtils.formatCurrency(transaction.amount, profile.currencySymbol)}? This will update your account balance and reports.",
                onConfirm = {
                    showDeleteDialog = false
                    viewModel.deleteTransaction(transaction.id) {
                        onNavigateBack()
                    }
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
