package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.CreditCardItem
import com.example.ui.components.EmptyState
import com.example.ui.theme.FinanceExpense
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.MoneyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val cards by viewModel.creditCards.collectAsStateWithLifecycle()
    val accounts by viewModel.activeAccounts.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Credit Card Tracker", fontWeight = FontWeight.Bold) },
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
                modifier = Modifier.testTag("add_credit_card_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Card")
            }
        },
        modifier = modifier.testTag("credit_cards_screen")
    ) { innerPadding ->
        if (cards.isEmpty()) {
            EmptyState(
                title = "No Credit Cards Linked",
                description = "Track card limits, statement dates, due dates, and minimum due amounts.",
                icon = Icons.Default.CreditCard,
                actionText = "+ Add Credit Card",
                onActionClick = { showAddDialog = true },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(cards, key = { it.id }) { card ->
                    CreditCardItemView(
                        card = card,
                        currencySymbol = profile.currencySymbol,
                        onPayBill = onNavigateToTransfer
                    )
                }
            }
        }

        if (showAddDialog) {
            AddCreditCardDialog(
                accounts = accounts,
                currencySymbol = profile.currencySymbol,
                onDismiss = { showAddDialog = false },
                onSave = { cardName, bank, limit, statementDay, dueDay ->
                    val cardAcc = accounts.firstOrNull { it.type == "CREDIT_CARD" }?.id ?: accounts.firstOrNull()?.id ?: 1L
                    viewModel.saveCreditCard(
                        id = 0L,
                        accountId = cardAcc,
                        cardName = cardName,
                        bank = bank,
                        creditLimit = limit,
                        statementDay = statementDay,
                        dueDay = dueDay,
                        onSuccess = { showAddDialog = false }
                    )
                }
            )
        }
    }
}

@Composable
private fun CreditCardItemView(
    card: CreditCardItem,
    currencySymbol: String,
    onPayBill: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("credit_card_${card.cardName}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Card Visual Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF334155))
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = card.bank, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Icon(imageVector = Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(24.dp))
                    }

                    Text(
                        text = card.cardName,
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "•••• •••• •••• 4289", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                        Text(text = "Due Day: ${card.dueDay}th", color = Color(0xFFFBBF24), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Limits & Outstanding
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Credit Limit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = MoneyUtils.formatCurrency(card.creditLimit, currencySymbol), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Outstanding", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = MoneyUtils.formatCurrency(card.currentOutstanding, currencySymbol),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (card.currentOutstanding > 0) FinanceExpense else MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Available Limit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = MoneyUtils.formatCurrency(card.availableLimit, currencySymbol),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val usedFraction = if (card.creditLimit > 0) (card.currentOutstanding.toFloat() / card.creditLimit.toFloat()).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { usedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (usedFraction > 0.5f) FinanceExpense else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "Minimum Due (5%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = MoneyUtils.formatCurrency(card.minimumDue, currencySymbol), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onPayBill,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pay Bill")
                }
            }
        }
    }
}

@Composable
private fun AddCreditCardDialog(
    accounts: List<com.example.domain.model.Account>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, Int, Int) -> Unit
) {
    var cardName by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var limitStr by remember { mutableStateOf("") }
    var dueDayStr by remember { mutableStateOf("5") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Credit Card", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = cardName,
                    onValueChange = { cardName = it },
                    label = { Text("Card Name") },
                    placeholder = { Text("e.g. Millennia, Regalia, Amazon Pay") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = bank,
                    onValueChange = { bank = it },
                    label = { Text("Bank") },
                    placeholder = { Text("e.g. HDFC, ICICI, SBI") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = { Text("Credit Limit ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dueDayStr,
                    onValueChange = { dueDayStr = it },
                    label = { Text("Due Day of Month (1-31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                val minor = MoneyUtils.parseToMinorUnits(limitStr)
                val day = dueDayStr.toIntOrNull() ?: 5
                if (cardName.isBlank() || minor <= 0L) {
                    error = "Card name and limit must be valid."
                    return@Button
                }
                onSave(cardName.trim(), bank.trim(), minor, 15, day)
            }) {
                Text("Add Card")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
