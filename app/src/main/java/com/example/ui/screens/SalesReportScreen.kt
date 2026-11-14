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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Expense
import com.example.data.model.PosTransaction
import com.example.data.model.Product
import com.example.data.model.ReportFilterMode
import com.example.data.model.StoreSettings
import com.example.ui.components.ProductPhotoBox
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusWarning
import com.example.util.ChartBucket
import com.example.util.PosFormatters
import com.example.util.SalesReportCalculator
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesReportScreen(
    store: StoreSettings,
    products: List<Product>,
    transactions: List<PosTransaction>,
    expenses: List<Expense>,
    onBackToDashboard: () -> Unit,
    onSelectTransaction: (PosTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val language = store.language

    val todayCal = remember { Calendar.getInstance() }
    var filterMode by remember { mutableStateOf(ReportFilterMode.DATE) }
    var selectedYear by remember { mutableIntStateOf(todayCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(todayCal.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(todayCal.get(Calendar.DAY_OF_MONTH)) }
    var showDatePicker by remember { mutableStateOf(false) }

    val photoLookup = remember(products) { products.associate { it.id to it.photoData } }

    val report = remember(
        transactions,
        expenses,
        photoLookup,
        filterMode,
        selectedYear,
        selectedMonth,
        selectedDay
    ) {
        SalesReportCalculator.computeReport(
            transactions = transactions,
            expenses = expenses,
            photoLookup = photoLookup,
            mode = filterMode,
            year = selectedYear,
            monthZeroBased = selectedMonth,
            dayOfMonth = selectedDay
        )
    }

    val activePeriodLabel = remember(filterMode, selectedYear, selectedMonth, selectedDay, language) {
        when (filterMode) {
            ReportFilterMode.DATE -> {
                val cal = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay, 12, 0, 0)
                }
                PosFormatters.formatDateOnly(cal.timeInMillis, language)
            }
            ReportFilterMode.MONTH -> PosFormatters.formatMonthYear(selectedYear, selectedMonth, language)
            ReportFilterMode.YEAR -> selectedYear.toString()
        }
    }

    fun stepPeriod(delta: Int) {
        val cal = Calendar.getInstance().apply {
            clear()
            set(selectedYear, selectedMonth, selectedDay)
        }
        when (filterMode) {
            ReportFilterMode.DATE -> cal.add(Calendar.DAY_OF_MONTH, delta)
            ReportFilterMode.MONTH -> cal.add(Calendar.MONTH, delta)
            ReportFilterMode.YEAR -> cal.add(Calendar.YEAR, delta)
        }
        selectedYear = cal.get(Calendar.YEAR)
        selectedMonth = cal.get(Calendar.MONTH)
        selectedDay = cal.get(Calendar.DAY_OF_MONTH)
    }

    Column(modifier = modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = strings.menuSalesReport,
            onBackClick = onBackToDashboard
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 1040.dp)
                    .fillMaxSize()
                    .testTag("sales_report_lazy_column"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Period Mode Filter: Tanggal, Bulan, Tahun + Navigator
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = filterMode == ReportFilterMode.DATE,
                                    onClick = { filterMode = ReportFilterMode.DATE },
                                    label = { Text(strings.reportFilterDate) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("report_filter_date")
                                )
                                FilterChip(
                                    selected = filterMode == ReportFilterMode.MONTH,
                                    onClick = { filterMode = ReportFilterMode.MONTH },
                                    label = { Text(strings.reportFilterMonth) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("report_filter_month")
                                )
                                FilterChip(
                                    selected = filterMode == ReportFilterMode.YEAR,
                                    onClick = { filterMode = ReportFilterMode.YEAR },
                                    label = { Text(strings.reportFilterYear) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("report_filter_year")
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(
                                        onClick = { stepPeriod(-1) },
                                        modifier = Modifier.testTag("report_prev_period_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                            contentDescription = "Previous"
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable(enabled = filterMode == ReportFilterMode.DATE) {
                                                showDatePicker = true
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CalendarMonth,
                                            contentDescription = strings.reportSelectDate,
                                            tint = BrandBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = activePeriodLabel,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    IconButton(
                                        onClick = { stepPeriod(1) },
                                        modifier = Modifier.testTag("report_next_period_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                            contentDescription = "Next"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. 4 Core Report Metrics: Total Penjualan, Omset, HPP/Modal, Laba Kotor
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ReportMetricCard(
                                title = strings.reportTotalSales,
                                value = PosFormatters.formatRupiah(report.totalSalesNet),
                                subtitle = "${PosFormatters.formatQuantity(report.totalItemsSold)} item • ${report.transactionCount} Trx",
                                icon = Icons.Rounded.ShoppingBag,
                                accentColor = BrandBlue,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("report_total_sales_card")
                            )
                            ReportMetricCard(
                                title = strings.reportOmset,
                                value = PosFormatters.formatRupiah(report.totalOmset),
                                subtitle = if (report.totalTax > 0.0) {
                                    "Termasuk PPN ${PosFormatters.formatRupiah(report.totalTax)}"
                                } else {
                                    "${report.transactionCount} Transaksi"
                                },
                                icon = Icons.Rounded.Payments,
                                accentColor = BrandGreen,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("report_omset_card")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ReportMetricCard(
                                title = strings.reportHpp,
                                value = PosFormatters.formatRupiah(report.totalHpp),
                                subtitle = strings.productCostPriceLabel.replace(" (Rp)", ""),
                                icon = Icons.Rounded.Inventory,
                                accentColor = StatusWarning,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("report_hpp_card")
                            )
                            ReportMetricCard(
                                title = strings.reportGrossProfit,
                                value = PosFormatters.formatRupiah(report.grossProfit),
                                subtitle = if (report.totalSalesNet > 0.0) {
                                    val margin = (report.grossProfit / report.totalSalesNet) * 100.0
                                    String.format(java.util.Locale.US, "Margin %.1f%%", margin)
                                } else {
                                    "Penjualan - HPP"
                                },
                                icon = Icons.AutoMirrored.Rounded.TrendingUp,
                                accentColor = if (report.grossProfit >= 0.0) BrandGreen else StatusError,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("report_gross_profit_card")
                            )
                        }
                    }
                }

                // 3. Pengeluaran & Laba Bersih Estimasi
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
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
                                Column {
                                    Text(
                                        text = strings.reportExpensesSummary,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = PosFormatters.formatRupiah(report.totalExpenses),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusError
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = strings.reportNetProfit,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = PosFormatters.formatRupiah(report.netProfit),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (report.netProfit >= 0.0) BrandGreen else StatusError
                                )
                            }
                        }
                    }
                }

                // 4. Grafik Tren Penjualan Periode Dipilih
                item {
                    var selectedIdx by remember(report.buckets) { mutableIntStateOf(-1) }
                    val inspected: ChartBucket? = report.buckets.getOrNull(selectedIdx)
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = strings.salesTrendTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = activePeriodLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (inspected != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${inspected.label} (${inspected.transactionCount} trx)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = PosFormatters.formatRupiah(inspected.totalSales),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            SmoothSalesLineChart(
                                buckets = report.buckets,
                                selectedIndex = selectedIdx,
                                onSelectIndex = { selectedIdx = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(175.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                report.buckets.forEach { b ->
                                    Text(
                                        text = b.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Payment Method Breakdown
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = strings.reportPaymentBreakdown,
                                style = MaterialTheme.typography.titleMedium
                            )
                            report.paymentBreakdown.forEachIndexed { idx, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${PosFormatters.paymentMethodLabel(item.methodCode, strings)} (${item.count}x)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = PosFormatters.formatRupiah(item.totalAmount),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (idx < report.paymentBreakdown.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }

                // 6. Top Products in Selected Period
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = strings.topProductsTitle,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (report.topProducts.isEmpty()) {
                                Text(
                                    text = strings.noSalesDataYet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                report.topProducts.forEachIndexed { idx, top ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "#${top.rank}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandBlue
                                        )
                                        ProductPhotoBox(
                                            photoData = top.photoData,
                                            contentDescription = top.productName,
                                            size = 42.dp,
                                            cornerRadius = 10.dp
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = top.productName,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${PosFormatters.formatQuantity(top.totalQty, top.unit)} ${strings.soldLabel}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = PosFormatters.formatRupiah(top.totalValue),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (idx < report.topProducts.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // 7. Transactions in Selected Period
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                    contentDescription = null,
                                    tint = BrandBlue
                                )
                                Text(
                                    text = "${strings.recentTransactionsTitle} (${report.transactions.size})",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            if (report.transactions.isEmpty()) {
                                Text(
                                    text = strings.noTransactionsYet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                report.transactions.take(15).forEachIndexed { idx, tx ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSelectTransaction(tx) }
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tx.transactionNumber,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                            Text(
                                                text = PosFormatters.formatDateTime(tx.transactionMillis, language),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = PosFormatters.formatRupiah(tx.totalAmount),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandGreen
                                        )
                                    }
                                    if (idx < minOf(report.transactions.size, 15) - 1) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initUtc = remember(selectedYear, selectedMonth, selectedDay) {
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(selectedYear, selectedMonth, selectedDay, 12, 0, 0)
            }.timeInMillis
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initUtc)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = millis
                            }
                            selectedYear = utcCal.get(Calendar.YEAR)
                            selectedMonth = utcCal.get(Calendar.MONTH)
                            selectedDay = utcCal.get(Calendar.DAY_OF_MONTH)
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
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ReportMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
