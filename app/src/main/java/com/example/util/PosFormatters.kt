package com.example.util

import com.example.data.model.DashboardPeriod
import com.example.data.model.Expense
import com.example.data.model.PaymentMethod
import com.example.data.model.PosTransaction
import com.example.data.model.ReportFilterMode
import com.example.data.model.TransactionStatus
import com.example.ui.theme.AppStrings
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object PosFormatters {
    private val idLocale: Locale = Locale.forLanguageTag("id-ID")
    private val idSymbols = DecimalFormatSymbols(idLocale).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    private val currencyFormat = DecimalFormat("#,##0", idSymbols)
    private val qtyDecimalFormat = DecimalFormat("#,##0.##", idSymbols)

    fun formatRupiah(amount: Double): String {
        val rounded = amount.roundToLong()
        return "Rp ${currencyFormat.format(rounded)}"
    }

    fun formatQuantity(qty: Double, unit: String = ""): String {
        val isWhole = abs(qty - qty.roundToLong()) < 0.0001
        val numStr = if (isWhole) {
            qty.roundToLong().toString()
        } else {
            String.format(Locale.US, "%.2f", qty).trimEnd('0').trimEnd('.')
        }
        return if (unit.isBlank()) numStr else "$numStr $unit"
    }

    fun formatDateTime(millis: Long, language: String = "id"): String {
        val locale = if (language == "en") Locale.ENGLISH else idLocale
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", locale)
        return sdf.format(Date(millis))
    }

    fun formatTimeOnly(millis: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatDateOnly(millis: Long, language: String = "id"): String {
        val locale = if (language == "en") Locale.ENGLISH else idLocale
        val sdf = SimpleDateFormat("dd MMMM yyyy", locale)
        return sdf.format(Date(millis))
    }

    fun formatMonthYear(year: Int, monthZeroBased: Int, language: String = "id"): String {
        val locale = if (language == "en") Locale.ENGLISH else idLocale
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val sdf = SimpleDateFormat("MMMM yyyy", locale)
        return sdf.format(cal.time)
    }

    fun sanitizeReceiptPrefix(rawPrefix: String): String {
        val cleaned = rawPrefix.trim().uppercase().filter { ch ->
            ch in 'A'..'Z' || ch in '0'..'9'
        }.take(10)
        return cleaned.ifBlank { "AK" }
    }

    fun generateTransactionNumber(
        nowMillis: Long = System.currentTimeMillis(),
        prefix: String = "AK"
    ): String {
        val sdf = SimpleDateFormat("yyMMdd-HHmmss", Locale.US)
        val suffix = (100..999).random()
        val cleanPrefix = sanitizeReceiptPrefix(prefix)
        return "$cleanPrefix-${sdf.format(Date(nowMillis))}-$suffix"
    }

    fun paymentMethodLabel(code: String, strings: AppStrings): String =
        when (PaymentMethod.fromCode(code)) {
            PaymentMethod.CASH -> strings.methodCash
            PaymentMethod.BANK_TRANSFER -> strings.methodBankTransfer
            PaymentMethod.QRIS -> strings.methodQris
            PaymentMethod.OTHER -> strings.methodOther
        }

    fun statusLabel(code: String, strings: AppStrings): String =
        when (TransactionStatus.fromCode(code)) {
            TransactionStatus.COMPLETED -> strings.statusCompleted
            TransactionStatus.CANCELLED -> strings.statusCancelled
            TransactionStatus.REFUNDED -> strings.statusRefunded
        }

    fun greetingForHour(hour: Int, strings: AppStrings): String = when (hour) {
        in 4..10 -> strings.greetingMorning
        in 11..14 -> strings.greetingAfternoon
        in 15..18 -> strings.greetingEvening
        else -> strings.greetingNight
    }
}

data class ChartBucket(
    val label: String,
    val startHour: Int,
    val endHour: Int,
    val totalSales: Double,
    val transactionCount: Int
)

data class TopProductRank(
    val rank: Int,
    val productId: String,
    val productName: String,
    val unit: String,
    val photoData: String,
    val totalQty: Double,
    val totalValue: Double
)

data class PeriodAnalytics(
    val currentTotalSales: Double,
    val previousTotalSales: Double,
    val percentChange: Double?,
    val buckets: List<ChartBucket>,
    val topProducts: List<TopProductRank>
)

object AnalyticsCalculator {
    private val timeSlots = listOf(
        8 to "08:00",
        10 to "10:00",
        12 to "12:00",
        14 to "14:00",
        16 to "16:00",
        18 to "18:00",
        20 to "20:00",
        22 to "22:00"
    )

    fun periodBounds(period: DashboardPeriod, nowMillis: Long = System.currentTimeMillis()): Pair<Pair<Long, Long>, Pair<Long, Long>> {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        val dayMs = 86_400_000L

        return when (period) {
            DashboardPeriod.TODAY -> {
                val cur = todayStart to nowMillis
                val prev = (todayStart - dayMs) to todayStart
                cur to prev
            }
            DashboardPeriod.SEVEN_DAYS -> {
                val curStart = todayStart - 6 * dayMs
                val cur = curStart to nowMillis
                val prev = (curStart - 7 * dayMs) to curStart
                cur to prev
            }
            DashboardPeriod.THIRTY_DAYS -> {
                val curStart = todayStart - 29 * dayMs
                val cur = curStart to nowMillis
                val prev = (curStart - 30 * dayMs) to curStart
                cur to prev
            }
            DashboardPeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val monthStart = cal.timeInMillis
                val prevCal = Calendar.getInstance().apply {
                    timeInMillis = monthStart
                    add(Calendar.MONTH, -1)
                }
                val prevMonthStart = prevCal.timeInMillis
                (monthStart to nowMillis) to (prevMonthStart to monthStart)
            }
        }
    }

    fun computePeriodAnalytics(
        transactions: List<PosTransaction>,
        photoLookup: Map<String, String>,
        period: DashboardPeriod,
        nowMillis: Long = System.currentTimeMillis()
    ): PeriodAnalytics {
        val (curRange, prevRange) = periodBounds(period, nowMillis)
        val completed = transactions.filter { it.status == TransactionStatus.COMPLETED.code }

        val curTx = completed.filter { it.transactionMillis in curRange.first..curRange.second }
        val prevTx = completed.filter { it.transactionMillis in prevRange.first until prevRange.second }

        val curTotal = curTx.sumOf { it.totalAmount }
        val prevTotal = prevTx.sumOf { it.totalAmount }
        val pctChange = when {
            prevTotal > 0.0 -> ((curTotal - prevTotal) / prevTotal) * 100.0
            curTotal > 0.0 -> 100.0
            else -> 0.0
        }

        // Bucket current period transactions into 08:00 .. 22:00 (2-hour windows around each label)
        val cal = Calendar.getInstance()
        val bucketSales = DoubleArray(timeSlots.size)
        val bucketCounts = IntArray(timeSlots.size)

        curTx.forEach { tx ->
            cal.timeInMillis = tx.transactionMillis
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val idx = when {
                hour < 9 -> 0       // <= 08:59 -> 08:00
                hour < 11 -> 1      // 09:00..10:59 -> 10:00
                hour < 13 -> 2      // 11:00..12:59 -> 12:00
                hour < 15 -> 3      // 13:00..14:59 -> 14:00
                hour < 17 -> 4      // 15:00..16:59 -> 16:00
                hour < 19 -> 5      // 17:00..18:59 -> 18:00
                hour < 21 -> 6      // 19:00..20:59 -> 20:00
                else -> 7           // >= 21:00 -> 22:00
            }
            bucketSales[idx] += tx.totalAmount
            bucketCounts[idx] += 1
        }

        val buckets = timeSlots.mapIndexed { index, (hour, label) ->
            ChartBucket(
                label = label,
                startHour = hour,
                endHour = hour + 2,
                totalSales = bucketSales[index],
                transactionCount = bucketCounts[index]
            )
        }

        // Aggregate Top 3 products sorted by quantity sold (respecting units)
        data class Acc(
            val productId: String,
            var name: String,
            var unit: String,
            var qty: Double,
            var value: Double
        )
        val prodMap = linkedMapOf<String, Acc>()
        curTx.forEach { tx ->
            tx.items.forEach { item ->
                val key = "${item.productId}|${item.unit}"
                val acc = prodMap.getOrPut(key) {
                    Acc(
                        productId = item.productId,
                        name = item.productName,
                        unit = item.unit,
                        qty = 0.0,
                        value = 0.0
                    )
                }
                acc.qty += item.quantity
                acc.value += item.subtotal
            }
        }

        val top3 = prodMap.values
            .sortedWith(compareByDescending<Acc> { it.qty }.thenByDescending { it.value })
            .take(3)
            .mapIndexed { idx, acc ->
                TopProductRank(
                    rank = idx + 1,
                    productId = acc.productId,
                    productName = acc.name,
                    unit = acc.unit,
                    photoData = photoLookup[acc.productId].orEmpty(),
                    totalQty = acc.qty,
                    totalValue = acc.value
                )
            }

        return PeriodAnalytics(
            currentTotalSales = curTotal,
            previousTotalSales = prevTotal,
            percentChange = pctChange,
            buckets = buckets,
            topProducts = top3
        )
    }
}

data class PaymentBreakdownItem(
    val methodCode: String,
    val count: Int,
    val totalAmount: Double
)

data class SalesReportSummary(
    val rangeStartMillis: Long,
    val rangeEndMillis: Long,
    val transactionCount: Int,
    val totalItemsSold: Double,
    val totalSalesNet: Double,
    val totalOmset: Double,
    val totalHpp: Double,
    val grossProfit: Double,
    val totalDiscount: Double,
    val totalTax: Double,
    val totalExpenses: Double,
    val netProfit: Double,
    val buckets: List<ChartBucket>,
    val topProducts: List<TopProductRank>,
    val paymentBreakdown: List<PaymentBreakdownItem>,
    val transactions: List<PosTransaction>,
    val expenses: List<Expense>
)

object SalesReportCalculator {
    fun dateRangeFor(
        mode: ReportFilterMode,
        year: Int,
        monthZeroBased: Int,
        dayOfMonth: Int
    ): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            when (mode) {
                ReportFilterMode.DATE -> {
                    set(Calendar.MONTH, monthZeroBased)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                ReportFilterMode.MONTH -> {
                    set(Calendar.MONTH, monthZeroBased)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                ReportFilterMode.YEAR -> {
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
            }
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = Calendar.getInstance().apply {
            timeInMillis = start.timeInMillis
            when (mode) {
                ReportFilterMode.DATE -> add(Calendar.DAY_OF_MONTH, 1)
                ReportFilterMode.MONTH -> add(Calendar.MONTH, 1)
                ReportFilterMode.YEAR -> add(Calendar.YEAR, 1)
            }
            add(Calendar.MILLISECOND, -1)
        }
        return start.timeInMillis to end.timeInMillis
    }

    fun computeReport(
        transactions: List<PosTransaction>,
        expenses: List<Expense>,
        photoLookup: Map<String, String>,
        mode: ReportFilterMode,
        year: Int,
        monthZeroBased: Int,
        dayOfMonth: Int
    ): SalesReportSummary {
        val (startMs, endMs) = dateRangeFor(mode, year, monthZeroBased, dayOfMonth)
        val completedTx = transactions
            .filter {
                it.status == TransactionStatus.COMPLETED.code &&
                    it.transactionMillis in startMs..endMs
            }
            .sortedByDescending { it.transactionMillis }

        val periodExpenses = expenses
            .filter { it.expenseMillis in startMs..endMs }
            .sortedByDescending { it.expenseMillis }

        val totalOmset = completedTx.sumOf { it.totalAmount }
        val totalDiscount = completedTx.sumOf { it.discountAmount }
        val totalTax = completedTx.sumOf { it.taxAmount }
        val totalSalesNet = completedTx.sumOf { (it.subtotal - it.discountAmount).coerceAtLeast(0.0) }
        val totalHpp = completedTx.sumOf { tx ->
            tx.items.sumOf { item -> item.costPrice * item.quantity }
        }
        val grossProfit = totalSalesNet - totalHpp
        val totalExpenses = periodExpenses.sumOf { it.amount }
        val netProfit = grossProfit - totalExpenses
        val totalItemsSold = completedTx.sumOf { tx -> tx.items.sumOf { it.quantity } }

        // Time-series buckets matching the selected mode
        val cal = Calendar.getInstance()
        val buckets: List<ChartBucket> = when (mode) {
            ReportFilterMode.DATE -> {
                val slots = listOf(
                    8 to "08:00", 10 to "10:00", 12 to "12:00", 14 to "14:00",
                    16 to "16:00", 18 to "18:00", 20 to "20:00", 22 to "22:00"
                )
                val sales = DoubleArray(slots.size)
                val counts = IntArray(slots.size)
                completedTx.forEach { tx ->
                    cal.timeInMillis = tx.transactionMillis
                    val h = cal.get(Calendar.HOUR_OF_DAY)
                    val idx = when {
                        h < 9 -> 0; h < 11 -> 1; h < 13 -> 2; h < 15 -> 3
                        h < 17 -> 4; h < 19 -> 5; h < 21 -> 6; else -> 7
                    }
                    sales[idx] += tx.totalAmount
                    counts[idx] += 1
                }
                slots.mapIndexed { i, (h, label) ->
                    ChartBucket(label, h, h + 2, sales[i], counts[i])
                }
            }
            ReportFilterMode.MONTH -> {
                val maxDay = Calendar.getInstance().apply {
                    clear()
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, monthZeroBased)
                    set(Calendar.DAY_OF_MONTH, 1)
                }.getActualMaximum(Calendar.DAY_OF_MONTH)
                // Group into 6 readable intervals across the month
                val ranges = listOf(1..5, 6..10, 11..15, 16..20, 21..25, 26..maxDay)
                val sales = DoubleArray(ranges.size)
                val counts = IntArray(ranges.size)
                completedTx.forEach { tx ->
                    cal.timeInMillis = tx.transactionMillis
                    val d = cal.get(Calendar.DAY_OF_MONTH)
                    val idx = ranges.indexOfFirst { d in it }.coerceAtLeast(0)
                    sales[idx] += tx.totalAmount
                    counts[idx] += 1
                }
                ranges.mapIndexed { i, r ->
                    ChartBucket("${r.first}-${r.last}", r.first, r.last, sales[i], counts[i])
                }
            }
            ReportFilterMode.YEAR -> {
                val monthLabels = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
                val sales = DoubleArray(12)
                val counts = IntArray(12)
                completedTx.forEach { tx ->
                    cal.timeInMillis = tx.transactionMillis
                    val m = cal.get(Calendar.MONTH).coerceIn(0, 11)
                    sales[m] += tx.totalAmount
                    counts[m] += 1
                }
                monthLabels.mapIndexed { idx, label ->
                    ChartBucket(label, idx, idx + 1, sales[idx], counts[idx])
                }
            }
        }

        // Top products in period
        data class Acc(val productId: String, val name: String, val unit: String, var qty: Double, var value: Double)
        val prodMap = linkedMapOf<String, Acc>()
        completedTx.forEach { tx ->
            tx.items.forEach { item ->
                val key = "${item.productId}|${item.unit}"
                val acc = prodMap.getOrPut(key) {
                    Acc(item.productId, item.productName, item.unit, 0.0, 0.0)
                }
                acc.qty += item.quantity
                acc.value += item.subtotal
            }
        }
        val topProducts = prodMap.values
            .sortedWith(compareByDescending<Acc> { it.qty }.thenByDescending { it.value })
            .take(5)
            .mapIndexed { idx, acc ->
                TopProductRank(
                    rank = idx + 1,
                    productId = acc.productId,
                    productName = acc.name,
                    unit = acc.unit,
                    photoData = photoLookup[acc.productId].orEmpty(),
                    totalQty = acc.qty,
                    totalValue = acc.value
                )
            }

        val paymentBreakdown = PaymentMethod.entries.map { method ->
            val matching = completedTx.filter { it.paymentMethod == method.code }
            PaymentBreakdownItem(
                methodCode = method.code,
                count = matching.size,
                totalAmount = matching.sumOf { it.totalAmount }
            )
        }

        return SalesReportSummary(
            rangeStartMillis = startMs,
            rangeEndMillis = endMs,
            transactionCount = completedTx.size,
            totalItemsSold = totalItemsSold,
            totalSalesNet = totalSalesNet,
            totalOmset = totalOmset,
            totalHpp = totalHpp,
            grossProfit = grossProfit,
            totalDiscount = totalDiscount,
            totalTax = totalTax,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            buckets = buckets,
            topProducts = topProducts,
            paymentBreakdown = paymentBreakdown,
            transactions = completedTx,
            expenses = periodExpenses
        )
    }
}

