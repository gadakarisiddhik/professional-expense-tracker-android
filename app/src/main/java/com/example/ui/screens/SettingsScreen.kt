package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.FinanceExpense
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.ExportHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    database: com.example.data.local.AppDatabase,
    onNavigateToAccounts: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToSavingsGoals: () -> Unit,
    onNavigateToCreditCards: () -> Unit,
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showDemoDataConfirm by remember { mutableStateOf(false) }
    var showBackupSuccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Data", fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier.testTag("settings_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Profile & Currency
            item {
                SettingsSectionHeader("PROFILE & CURRENCY")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SettingsRow(
                            icon = Icons.Default.Person,
                            title = "User Name",
                            subtitle = profile.name,
                            onClick = { showProfileDialog = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        SettingsRow(
                            icon = Icons.Default.AccountBalance,
                            title = "Currency",
                            subtitle = "${profile.currencyCode} (${profile.currencySymbol})",
                            onClick = { showProfileDialog = true }
                        )
                    }
                }
            }

            // Financial Modules Navigation
            item {
                SettingsSectionHeader("FINANCIAL MANAGEMENT")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SettingsNavRow("Accounts & Wallets", Icons.Default.AccountBalance, onNavigateToAccounts)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        SettingsNavRow("Categories & Tags", Icons.Default.Category, onNavigateToCategories)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        SettingsNavRow("Monthly Budgets", Icons.Default.PieChart, onNavigateToBudget)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        SettingsNavRow("Credit Cards", Icons.Default.CreditCard, onNavigateToCreditCards)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        SettingsNavRow("Recurring & Bills", Icons.Default.Repeat, onNavigateToRecurring)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        SettingsNavRow("Savings Goals", Icons.Default.Savings, onNavigateToSavingsGoals)
                    }
                }
            }

            // Security & App Lock
            item {
                SettingsSectionHeader("SECURITY & PRIVACY")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("App Lock (PIN)", fontWeight = FontWeight.SemiBold)
                                    Text("Require PIN to open app", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = profile.appLockEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && profile.pinHash.isBlank()) {
                                        showPinSetupDialog = true
                                    } else {
                                        viewModel.toggleAppLock(enabled)
                                    }
                                },
                                modifier = Modifier.testTag("app_lock_switch")
                            )
                        }

                        if (profile.appLockEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            SettingsRow(
                                icon = Icons.Default.Lock,
                                title = "Change PIN",
                                subtitle = "Update your 4-digit code",
                                onClick = { showPinSetupDialog = true }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Biometric Unlock", fontWeight = FontWeight.SemiBold)
                                        Text("Fingerprint or Face unlock", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(
                                    checked = profile.biometricEnabled,
                                    onCheckedChange = { viewModel.toggleBiometric(it) }
                                )
                            }
                        }
                    }
                }
            }

            // Data & Export
            item {
                SettingsSectionHeader("DATA, BACKUP & EXPORT")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SettingsRow(
                            icon = Icons.Default.TableChart,
                            title = "Reports & Export",
                            subtitle = "Download statements as PDF / CSV",
                            onClick = onNavigateToReports
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        SettingsRow(
                            icon = Icons.Default.Storage,
                            title = "Backup to File",
                            subtitle = "Create local JSON backup of all data",
                            onClick = {
                                viewModel.createBackup(context, database) { file ->
                                    ExportHelper.shareFile(context, file, "application/json", "Share Expense Tracker Backup")
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        SettingsRow(
                            icon = Icons.Default.Restore,
                            title = "Load Demo / Testing Data",
                            subtitle = "Populate realistic sample transactions",
                            onClick = { showDemoDataConfirm = true }
                        )
                    }
                }
            }

            // About & System Info
            item {
                SettingsSectionHeader("ABOUT")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Professional Expense Tracker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Version 1.0.0 (Production Release)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Offline-first architecture with deterministic Long-minor money calculations, Room database encryption compatibility, and zero analytics tracking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Profile Update Dialog
        if (showProfileDialog) {
            ProfileEditDialog(
                currentName = profile.name,
                currentSymbol = profile.currencySymbol,
                currentCode = profile.currencyCode,
                onDismiss = { showProfileDialog = false },
                onSave = { newName, newSymbol, newCode ->
                    viewModel.updateProfile(newName, newSymbol, newCode, profile.themeMode)
                    showProfileDialog = false
                }
            )
        }

        // PIN Setup Dialog
        if (showPinSetupDialog) {
            PinSetupDialog(
                onDismiss = { showPinSetupDialog = false },
                onSavePin = { pin ->
                    viewModel.setAppPin(pin)
                    showPinSetupDialog = false
                }
            )
        }

        // Demo Data Confirmation Dialog
        if (showDemoDataConfirm) {
            AlertDialog(
                onDismissRequest = { showDemoDataConfirm = false },
                title = { Text("Load Sample Data?", fontWeight = FontWeight.Bold) },
                text = { Text("This will add realistic sample transactions, bills, and goals for quick demonstration. Existing data will not be cleared.") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.seedDemoData {
                            showDemoDataConfirm = false
                        }
                    }) {
                        Text("Load Sample Data")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDemoDataConfirm = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsNavRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ProfileEditDialog(
    currentName: String,
    currentSymbol: String,
    currentCode: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var symbol by remember { mutableStateOf(currentSymbol) }
    var code by remember { mutableStateOf(currentCode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile & Currency", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("User Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("Currency Symbol (e.g. ₹, $, €)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Currency Code (e.g. INR, USD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name.trim(), symbol.trim(), code.trim()) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PinSetupDialog(
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set 4-Digit Security PIN", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it },
                    label = { Text("Enter 4-Digit PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4) confirmPin = it },
                    label = { Text("Confirm PIN") },
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
                if (pin.length != 4) {
                    error = "PIN must be exactly 4 digits."
                    return@Button
                }
                if (pin != confirmPin) {
                    error = "PINs do not match."
                    return@Button
                }
                onSavePin(pin)
            }) {
                Text("Enable PIN Lock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
