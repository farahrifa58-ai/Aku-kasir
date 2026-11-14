package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.DashboardPeriod
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.ReportFilterMode
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusWarning
import com.example.util.AnalyticsCalculator
import com.example.util.PosFormatters
import com.example.util.SalesReportCalculator
import java.util.Calendar
import java.util.TimeZone

@Composable
fun ExpensesScreen(
    userId: String,
    language: String,
    expenses: List<Expense>,
    onBackToDashboard: () -> Unit,
    onSaveExpense: (Expense, () -> Unit) -> Unit,
    onDeleteExpense: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var showFormDialog by remember { mutableStateOf(false) }
    var deletingExpense by remember { mutableStateOf<Expense?>(null) }

    val todaySum = remember(expenses) {
        val bounds = AnalyticsCalculator.periodBounds(DashboardPeriod.TODAY).first
        expenses.filter { it.expenseMillis in bounds.first..bounds.second }.sumOf { it.amount }
    }
    val monthSum = remember(expenses) {
        val cal = Calendar.getInstance()
        val bounds = SalesReportCalculator.dateRangeFor(
            ReportFilterMode.MONTH,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        expenses.filter { it.expenseMillis in bounds.first..bounds.second }.sumOf { it.amount }
    }

    val filteredExpenses = remember(expenses, searchQuery, selectedCategory) {
        val q = searchQuery.trim().lowercase()
        expenses.filter { exp ->
            val matchCat = selectedCategory == null || exp.category.equals(selectedCategory, ignoreCase = true)
            val matchQuery = q.isEmpty() ||
                exp.title.lowercase().contains(q) ||
                exp.category.lowercase().contains(q) ||
                exp.notes.lowercase().contains(q)
            matchCat && matchQuery
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SubScreenTopBar(
                title = strings.menuExpenses,
                onBackClick = onBackToDashboard
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 960.dp)
                        .fillMaxSize()
                        .testTag("expenses_lazy_column"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Summary Cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ExpenseSummaryCard(
                                title = strings.todayExpensesLabel,
                                value = PosFormatters.formatRupiah(todaySum),
                                accent = StatusError,
                                modifier = Modifier.weight(1f)
                            )
                            ExpenseSummaryCard(
                                title = strings.monthExpensesLabel,
                                value = PosFormatters.formatRupiah(monthSum),
                                accent = StatusWarning,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Search & Category Chips
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(strings.searchPlaceholder) },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Rounded.Close, contentDescription = strings.close)
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedCategory == null,
                                    onClick = { selectedCategory = null },
                                    label = { Text(strings.all) }
                                )
                                ExpenseCategory.entries.forEach { cat ->
                                    val label = if (language == "en") cat.labelEn else cat.labelId
                                    FilterChip(
                                        selected = selectedCategory == cat.labelId,
                                        onClick = {
                                            selectedCategory = if (selectedCategory == cat.labelId) null else cat.labelId
                                        },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }

                    if (filteredExpenses.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = strings.noExpensesYet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(24.dp)
                                )
                            }
                        }
                    } else {
                        items(filteredExpenses, key = { it.id }) { exp ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(StatusError.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = StatusError,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = exp.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = ExpenseCategory.labelFor(exp.category, language),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = PosFormatters.formatDateOnly(exp.expenseMillis, language),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (exp.notes.isNotBlank()) {
                                            Text(
                                                text = exp.notes,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = PosFormatters.formatRupiah(exp.amount),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusError
                                        )
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    editingExpense = exp
                                                    showFormDialog = true
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Edit,
                                                    contentDescription = strings.edit,
                                                    tint = BrandBlue,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            IconButton(onClick = { deletingExpense = exp }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Delete,
                                                    contentDescription = strings.delete,
                                                    tint = StatusError,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingExpense = null
                showFormDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("expenses_add_button")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = strings.addExpense)
        }
    }

    if (showFormDialog) {
        ExpenseFormDialog(
            userId = userId,
            language = language,
            initial = editingExpense,
            onDismiss = { showFormDialog = false },
            onSave = { expense ->
                onSaveExpense(expense) { showFormDialog = false }
            }
        )
    }

    val pendingDelete = deletingExpense
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { deletingExpense = null },
            title = { Text(strings.delete) },
            text = { Text("${strings.deleteExpenseConfirm}\n\n${pendingDelete.title}") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExpense(pendingDelete.id)
                        deletingExpense = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingExpense = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun ExpenseSummaryCard(
    title: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseFormDialog(
    userId: String,
    language: String,
    initial: Expense?,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit
) {
    val strings = LocalAppStrings.current
    var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
    var category by remember(initial) { mutableStateOf(initial?.category ?: ExpenseCategory.OPERATIONAL.labelId) }
    var amountInput by remember(initial) {
        mutableStateOf(if (initial != null && initial.amount > 0.0) initial.amount.toLong().toString() else "")
    }
    var notes by remember(initial) { mutableStateOf(initial?.notes ?: "") }
    var expenseMillis by remember(initial) { mutableLongStateOf(initial?.expenseMillis ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initial == null) strings.addExpense else strings.editExpense,
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.close)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.expenseTitleLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input")
                )

                Text(strings.expenseCategoryLabel, style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExpenseCategory.entries.forEach { cat ->
                        val label = if (language == "en") cat.labelEn else cat.labelId
                        FilterChip(
                            selected = category == cat.labelId,
                            onClick = { category = cat.labelId },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text(strings.expenseAmountLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = BrandBlue)
                            Text(strings.expenseDateLabel, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            text = PosFormatters.formatDateOnly(expenseMillis, language),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.expenseNotesLabel) },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                val parsedAmount = amountInput.replace(",", ".").toDoubleOrNull() ?: 0.0
                val isValid = title.trim().isNotBlank() && parsedAmount > 0.0

                Button(
                    onClick = {
                        onSave(
                            Expense(
                                id = initial?.id ?: "",
                                userId = userId,
                                title = title.trim(),
                                category = category,
                                amount = parsedAmount,
                                notes = notes.trim(),
                                expenseMillis = expenseMillis
                            )
                        )
                    },
                    enabled = isValid,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("expense_save_button")
                ) {
                    Text(strings.save)
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = expenseMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { utcMillis ->
                            val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val local = Calendar.getInstance().apply {
                                timeInMillis = expenseMillis
                                set(Calendar.YEAR, utc.get(Calendar.YEAR))
                                set(Calendar.MONTH, utc.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH))
                            }
                            expenseMillis = local.timeInMillis
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(strings.cancel)
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
