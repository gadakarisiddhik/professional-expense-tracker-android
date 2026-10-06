package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FinanceExpense
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.MoneyUtils
import com.example.utils.QuickAddResult

@Composable
fun QuickAddDialog(
    viewModel: MainViewModel,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var parsedResult by remember { mutableStateOf<QuickAddResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Smart Quick Add", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = "Type natural input like \"₹250 Food UPI\" or \"450 dinner cash\":",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = {
                        textInput = it
                        errorMessage = null
                        if (it.isNotBlank()) {
                            parsedResult = viewModel.parseQuickAdd(it)
                        } else {
                            parsedResult = null
                        }
                    },
                    placeholder = { Text("e.g. 250 Food UPI") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quick_add_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                parsedResult?.let { result ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Parsed Interpretation:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Text("• Amount: ${MoneyUtils.formatCurrency(result.amount, currencySymbol)}")
                            Text("• Category: ${result.categoryName ?: "Not detected"}")
                            Text("• Account: ${result.accountName ?: "Not detected"}")
                            if (result.note.isNotBlank()) {
                                Text("• Note: ${result.note}")
                            }

                            if (result.isAmbiguous) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⚠️ Some fields are unclear. Please review before saving.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FinanceExpense
                                )
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = FinanceExpense,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = parsedResult
                    if (result == null || result.amount <= 0L) {
                        errorMessage = "Please enter a valid amount."
                        return@Button
                    }

                    val cat = viewModel.allCategories.value.firstOrNull {
                        it.name.equals(result.categoryName, ignoreCase = true)
                    } ?: viewModel.allCategories.value.firstOrNull { it.type == "EXPENSE" }

                    val acc = viewModel.activeAccounts.value.firstOrNull {
                        it.name.contains(result.accountName ?: "", ignoreCase = true)
                    } ?: viewModel.activeAccounts.value.firstOrNull()

                    if (cat == null || acc == null) {
                        errorMessage = "Could not match category or account."
                        return@Button
                    }

                    viewModel.addExpense(
                        amount = result.amount,
                        categoryId = cat.id,
                        accountId = acc.id,
                        note = result.note.ifBlank { "Quick Add: ${cat.name}" },
                        onSuccess = {
                            onDismiss()
                            onSuccess()
                        }
                    )
                },
                modifier = Modifier.testTag("quick_add_confirm_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Confirm & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
