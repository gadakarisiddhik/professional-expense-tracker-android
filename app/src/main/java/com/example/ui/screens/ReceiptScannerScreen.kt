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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.FinanceExpense
import com.example.ui.theme.FinanceIncome
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.DateUtils
import com.example.utils.MoneyUtils
import com.example.utils.ParsedReceipt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScannerScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val accounts by viewModel.activeAccounts.collectAsStateWithLifecycle()

    var rawReceiptText by remember {
        mutableStateOf(
            """
            DMart Supermarket
            Store #402, High Street
            Date: 05/10/2026
            Item 1: Organic Milk  - ₹65.00
            Item 2: Basmati Rice  - ₹450.00
            Item 3: Fresh Fruits  - ₹340.00
            Item 4: Cooking Oil   - ₹393.00
            GRAND TOTAL: ₹1248.00
            Payment: UPI / Card
            Thank you for shopping!
            """.trimIndent()
        )
    }

    var parsedResult by remember { mutableStateOf<ParsedReceipt?>(null) }
    var showReviewDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receipt Scanner (OCR)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier.testTag("receipt_scanner_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Receipt Intelligence Engine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Extracts merchant name, total amount, transaction date, and suggested category from receipts. You can paste receipt text or sample receipt logs below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text("Receipt Raw Text / OCR Input:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = rawReceiptText,
                    onValueChange = { rawReceiptText = it },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val parsed = viewModel.parseReceipt(rawReceiptText)
                            parsedResult = parsed
                            showReviewDialog = true
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Process Receipt")
                    }

                    OutlinedButton(
                        onClick = {
                            rawReceiptText = """
                            Starbucks Coffee
                            06 Oct 2026 09:30 AM
                            Caffe Latte - ₹350.00
                            Croissant - ₹220.00
                            Total Amount: ₹570.00
                            """.trimIndent()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text("Sample 2")
                    }
                }
            }
        }

        if (showReviewDialog && parsedResult != null) {
            val r = parsedResult!!
            ReviewReceiptDialog(
                receipt = r,
                currencySymbol = profile.currencySymbol,
                onDismiss = { showReviewDialog = false },
                onConfirmSave = { merchant, amount, categoryId, accountId ->
                    viewModel.addExpense(
                        amount = amount,
                        categoryId = categoryId,
                        accountId = accountId,
                        dateTime = r.dateTimestamp,
                        merchant = merchant,
                        note = "Scanned Receipt Purchase",
                        onSuccess = {
                            showReviewDialog = false
                            onNavigateBack()
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun ReviewReceiptDialog(
    receipt: ParsedReceipt,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirmSave: (String, Long, Long, Long) -> Unit
) {
    var merchant by remember { mutableStateOf(receipt.merchant) }
    var amountStr by remember { mutableStateOf(MoneyUtils.toDecimalString(receipt.amountMinorUnits)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Receipt Details", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Please verify the extracted information before saving:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Total Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Date: ${DateUtils.formatDate(receipt.dateTimestamp)}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Category Suggestion: ${receipt.suggestedCategory}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minor = MoneyUtils.parseToMinorUnits(amountStr)
                    onConfirmSave(merchant.trim(), minor, 1L, 1L)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FinanceExpense)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Confirm & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
