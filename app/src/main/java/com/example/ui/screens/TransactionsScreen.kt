package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.TransactionType
import com.example.ui.components.EmptyState
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.FinanceExpense
import com.example.ui.theme.FinanceIncome
import com.example.ui.theme.FinanceTransfer
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.SortOrder
import com.example.ui.viewmodel.TimeFilter
import com.example.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: MainViewModel,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val selectedTime by viewModel.selectedTimeFilter.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSortOrder.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen")
    ) {
        TopAppBar(
            title = { Text(text = "Transactions", fontWeight = FontWeight.Bold) },
            actions = {
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Date (Newest First)") },
                            onClick = {
                                viewModel.selectedSortOrder.value = SortOrder.DATE_DESC
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Amount (High to Low)") },
                            onClick = {
                                viewModel.selectedSortOrder.value = SortOrder.AMOUNT_DESC
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Amount (Low to High)") },
                            onClick = {
                                viewModel.selectedSortOrder.value = SortOrder.AMOUNT_ASC
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Search by merchant, note, category...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("transaction_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Time Filters Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                Pair(TimeFilter.ALL, "All Time"),
                Pair(TimeFilter.TODAY, "Today"),
                Pair(TimeFilter.YESTERDAY, "Yesterday"),
                Pair(TimeFilter.THIS_WEEK, "This Week"),
                Pair(TimeFilter.THIS_MONTH, "This Month"),
                Pair(TimeFilter.LAST_MONTH, "Last Month")
            )

            items(filters) { (filter, label) ->
                val isSelected = selectedTime == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectedTimeFilter.value = filter },
                    label = { Text(text = label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Type Filters Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { viewModel.selectedTypeFilter.value = null },
                    label = { Text("All Types") }
                )
            }
            item {
                FilterChip(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = { viewModel.selectedTypeFilter.value = TransactionType.EXPENSE },
                    label = { Text("Expenses") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FinanceExpense.copy(alpha = 0.15f))
                )
            }
            item {
                FilterChip(
                    selected = selectedType == TransactionType.INCOME,
                    onClick = { viewModel.selectedTypeFilter.value = TransactionType.INCOME },
                    label = { Text("Income") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FinanceIncome.copy(alpha = 0.15f))
                )
            }
            item {
                FilterChip(
                    selected = selectedType == TransactionType.TRANSFER,
                    onClick = { viewModel.selectedTypeFilter.value = TransactionType.TRANSFER },
                    label = { Text("Transfers") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FinanceTransfer.copy(alpha = 0.15f))
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Transactions List Grouped by Date
        if (transactions.isEmpty()) {
            EmptyState(
                title = "No transactions found",
                description = "Try adjusting your search query or filter options.",
                modifier = Modifier.weight(1f)
            )
        } else {
            val grouped = remember(transactions) {
                transactions.groupBy { DateUtils.getDateGroupHeader(it.dateTime) }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("transactions_list"),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                grouped.forEach { (header, itemsInGroup) ->
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = header,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }

                    items(itemsInGroup, key = { it.id }) { tx ->
                        TransactionRowItem(
                            transaction = tx,
                            currencySymbol = profile.currencySymbol,
                            onClick = { onNavigateToDetail(tx.id) }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            modifier = Modifier.padding(start = 72.dp)
                        )
                    }
                }
            }
        }
    }
}
