package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.DashboardPeriod
import com.example.data.model.PaymentMethod
import com.example.data.model.PosTransaction
import com.example.data.model.StoreSettings
import com.example.data.model.TransactionStatus
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.util.AnalyticsCalculator
import com.example.util.PosFormatters
import com.example.util.ReceiptHelper

@Composable
fun HistoryScreen(
    transactions: List<PosTransaction>,
    store: StoreSettings,
    initialSelectedTransaction: PosTransaction? = null,
    defaultPaperWidthMm: Int = 58,
    defaultPrinterAddress: String = "",
    onClearInitialSelection: () -> Unit = {},
    onCancelOrRefund: (String, TransactionStatus, String, () -> Unit) -> Unit,
    onBackToDashboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val language = store.language
    var searchQuery by remember { mutableStateOf("") }
    var dateFilter by remember { mutableStateOf<DashboardPeriod?>(null) }
    var methodFilter by remember { mutableStateOf<PaymentMethod?>(null) }

    var detailTransaction by remember(initialSelectedTransaction) {
        mutableStateOf(initialSelectedTransaction)
    }
    var receiptTransaction by remember { mutableStateOf<PosTransaction?>(null) }

    val filteredTransactions = remember(transactions, searchQuery, dateFilter, methodFilter) {
        val bounds = dateFilter?.let { AnalyticsCalculator.periodBounds(it).first }
        transactions.filter { tx ->
            val matchQuery = searchQuery.isBlank() ||
                tx.transactionNumber.contains(searchQuery, ignoreCase = true) ||
                tx.customerName.contains(searchQuery, ignoreCase = true)
            val matchDate = bounds == null || tx.transactionMillis in bounds.first..bounds.second
            val matchMethod = methodFilter == null || tx.paymentMethod == methodFilter!!.code
            matchQuery && matchDate && matchMethod
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = strings.menuHistory,
            onBackClick = onBackToDashboard
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 1040.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchTrxHint) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Date Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = dateFilter == null,
                    onClick = { dateFilter = null },
                    label = { Text(strings.all) }
                )
                listOf(
                    DashboardPeriod.TODAY to strings.periodToday,
                    DashboardPeriod.SEVEN_DAYS to strings.period7Days,
                    DashboardPeriod.THIRTY_DAYS to strings.period30Days,
                    DashboardPeriod.THIS_MONTH to strings.periodThisMonth
                ).forEach { (period, label) ->
                    FilterChip(
                        selected = dateFilter == period,
                        onClick = { dateFilter = period },
                        label = { Text(label) }
                    )
                }
            }

            // Payment Method Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = methodFilter == null,
                    onClick = { methodFilter = null },
                    label = { Text(strings.all) }
                )
                listOf(
                    PaymentMethod.CASH to strings.methodCash,
                    PaymentMethod.BANK_TRANSFER to strings.methodBankTransfer,
                    PaymentMethod.QRIS to strings.methodQris,
                    PaymentMethod.OTHER to strings.methodOther
                ).forEach { (method, label) ->
                    FilterChip(
                        selected = methodFilter == method,
                        onClick = { methodFilter = method },
                        label = { Text(label) }
                    )
                }
            }

            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.noTransactionsYet,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        HistoryTransactionCard(
                            transaction = tx,
                            language = language,
                            onClick = { detailTransaction = tx }
                        )
                    }
                }
            }
        }
    }
    }

    val selectedTx = detailTransaction
    if (selectedTx != null) {
        TransactionDetailDialog(
            transaction = selectedTx,
            store = store,
            onDismiss = {
                detailTransaction = null
                onClearInitialSelection()
            },
            onOpenReceipt = {
                receiptTransaction = selectedTx
            },
            onCancelOrRefund = { targetStatus, reason ->
                onCancelOrRefund(selectedTx.id, targetStatus, reason) {
                    detailTransaction = null
                    onClearInitialSelection()
                }
            }
        )
    }

    val rxTx = receiptTransaction
    if (rxTx != null) {
        ReceiptDialog(
            transaction = rxTx,
            store = store,
            language = language,
            defaultPaperWidthMm = defaultPaperWidthMm,
            defaultPrinterAddress = defaultPrinterAddress,
            onDismiss = { receiptTransaction = null }
        )
    }
}

@Composable
private fun HistoryTransactionCard(
    transaction: PosTransaction,
    language: String,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val statusColor = when (TransactionStatus.fromCode(transaction.status)) {
        TransactionStatus.COMPLETED -> StatusSuccess
        TransactionStatus.CANCELLED -> StatusError
        TransactionStatus.REFUNDED -> StatusWarning
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = transaction.transactionNumber,
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = PosFormatters.formatDateTime(transaction.transactionMillis, language),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val subtitle = buildString {
                    append(PosFormatters.paymentMethodLabel(transaction.paymentMethod, strings))
                    if (transaction.customerName.isNotBlank()) {
                        append(" • ")
                        append(transaction.customerName)
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = PosFormatters.formatRupiah(transaction.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = PosFormatters.statusLabel(transaction.status, strings),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionDetailDialog(
    transaction: PosTransaction,
    store: StoreSettings,
    onDismiss: () -> Unit,
    onOpenReceipt: () -> Unit,
    onCancelOrRefund: (TransactionStatus, String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val language = store.language
    var confirmAction by remember { mutableStateOf<TransactionStatus?>(null) }
    var reasonInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.trxDetailTitle, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.close)
                    }
                }

                Text(
                    text = transaction.transactionNumber,
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${strings.receiptDate}: ${PosFormatters.formatDateTime(transaction.transactionMillis, language)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (transaction.customerName.isNotBlank()) {
                    Text(
                        text = "${strings.receiptCustomer}: ${transaction.customerName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "${strings.paymentMethodLabel}: ${PosFormatters.paymentMethodLabel(transaction.paymentMethod, strings)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (transaction.statusReason.isNotBlank()) {
                    Text(
                        text = "${strings.reasonOptionalLabel}: ${transaction.statusReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusError
                    )
                }

                HorizontalDivider()
                Text(strings.purchasedItemsLabel, style = MaterialTheme.typography.titleSmall)

                transaction.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.productName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${PosFormatters.formatQuantity(item.quantity, item.unit)} x ${PosFormatters.formatRupiah(item.unitPrice)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = PosFormatters.formatRupiah(item.subtotal),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider()

                DetailAmountRow(strings.subtotalLabel, PosFormatters.formatRupiah(transaction.subtotal))
                if (transaction.discountAmount > 0.0) {
                    DetailAmountRow(strings.discountLabel, "-${PosFormatters.formatRupiah(transaction.discountAmount)}")
                }
                if (transaction.taxAmount > 0.0) {
                    DetailAmountRow(strings.taxLabel, PosFormatters.formatRupiah(transaction.taxAmount))
                }
                DetailAmountRow(strings.grandTotalLabel, PosFormatters.formatRupiah(transaction.totalAmount), bold = true)
                DetailAmountRow(strings.amountReceivedLabel, PosFormatters.formatRupiah(transaction.amountPaid))
                DetailAmountRow(strings.changeLabel, PosFormatters.formatRupiah(transaction.changeAmount))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onOpenReceipt,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.reprintReceipt, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Button(
                        onClick = {
                            ReceiptHelper.shareReceiptWhatsApp(context, transaction, store, strings, language)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.shareReceipt, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                if (transaction.status == TransactionStatus.COMPLETED.code && !transaction.stockRestored) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { confirmAction = TransactionStatus.CANCELLED },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.cancelTransaction, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = { confirmAction = TransactionStatus.REFUNDED },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusWarning),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.refundTransaction, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }

    val pendingAction = confirmAction
    if (pendingAction != null) {
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = {
                Text(
                    if (pendingAction == TransactionStatus.CANCELLED) strings.cancelConfirmTitle
                    else strings.refundConfirmTitle
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.cancelRefundWarning, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        label = { Text(strings.reasonOptionalLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmAction = null
                        onCancelOrRefund(pendingAction, reasonInput)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun DetailAmountRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = if (bold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
