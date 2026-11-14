package com.example.data.model

import com.example.util.AnalyticsCalculator
import com.example.util.SalesReportCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class PosCalculatorAndAnalyticsTest {

    @Test
    fun calculateTotals_decimalUnitAndAutoTax_computedOnceAccurately() {
        // Rp20.000/kg * 0.50 kg = Rp10.000
        val lineSubtotal = PosCalculator.roundCurrency(20000.0 * 0.50)
        assertEquals(10000.0, lineSubtotal, 0.001)

        // 10% discount = 1000 -> taxable base = 9000 -> 11% PPN = 990 -> grandTotal = 9990
        val totals = PosCalculator.calculateTotals(
            subtotal = lineSubtotal,
            discountType = DiscountType.PERCENT,
            discountValue = 10.0,
            taxType = TaxType.AUTO,
            autoTaxPercent = 11.0,
            manualTaxAmount = 5000.0, // ignored when AUTO
            amountPaid = 10000.0
        )

        assertEquals(10000.0, totals.subtotal, 0.001)
        assertEquals(1000.0, totals.discountAmount, 0.001)
        assertEquals(9000.0, totals.taxableBase, 0.001)
        assertEquals(990.0, totals.taxAmount, 0.001)
        assertEquals(9990.0, totals.grandTotal, 0.001)
        assertEquals(10.0, totals.changeAmount, 0.001)
    }

    @Test
    fun calculateTotals_manualTaxAndNoTax_accurate() {
        val noTax = PosCalculator.calculateTotals(
            subtotal = 50000.0,
            discountType = DiscountType.AMOUNT,
            discountValue = 5000.0,
            taxType = TaxType.NONE,
            autoTaxPercent = 11.0,
            manualTaxAmount = 0.0,
            amountPaid = 50000.0
        )
        assertEquals(0.0, noTax.taxAmount, 0.001)
        assertEquals(45000.0, noTax.grandTotal, 0.001)
        assertEquals(5000.0, noTax.changeAmount, 0.001)

        val manualTax = PosCalculator.calculateTotals(
            subtotal = 50000.0,
            discountType = DiscountType.NONE,
            discountValue = 0.0,
            taxType = TaxType.MANUAL,
            autoTaxPercent = 11.0,
            manualTaxAmount = 2500.0,
            amountPaid = 52500.0
        )
        assertEquals(2500.0, manualTax.taxAmount, 0.001)
        assertEquals(52500.0, manualTax.grandTotal, 0.001)
        assertEquals(0.0, manualTax.changeAmount, 0.001)
    }

    @Test
    fun analytics_computesBucketsPercentageAndTopProducts_excludingCancelled() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 15)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today10am = cal.timeInMillis
        val yesterday10am = today10am - 86_400_000L
        val now = today10am + 3_600_000L

        val txToday = PosTransaction(
            id = "t1",
            totalAmount = 30000.0,
            status = TransactionStatus.COMPLETED.code,
            transactionMillis = today10am,
            items = listOf(
                TransactionItem("p1", "Beras", "Kg", 20000.0, 15000.0, 1.5, 30000.0)
            )
        )
        val txCancelled = PosTransaction(
            id = "t2",
            totalAmount = 99999.0,
            status = TransactionStatus.CANCELLED.code,
            transactionMillis = today10am,
            items = listOf(
                TransactionItem("p2", "Gula", "Kg", 15000.0, 12000.0, 5.0, 75000.0)
            )
        )
        val txYesterday = PosTransaction(
            id = "t3",
            totalAmount = 20000.0,
            status = TransactionStatus.COMPLETED.code,
            transactionMillis = yesterday10am
        )

        val result = AnalyticsCalculator.computePeriodAnalytics(
            transactions = listOf(txToday, txCancelled, txYesterday),
            photoLookup = emptyMap(),
            period = DashboardPeriod.TODAY,
            nowMillis = now
        )

        assertEquals(30000.0, result.currentTotalSales, 0.001)
        assertEquals(20000.0, result.previousTotalSales, 0.001)
        assertEquals(50.0, result.percentChange ?: 0.0, 0.001) // +50%
        assertEquals(8, result.buckets.size)
        assertEquals(30000.0, result.buckets[1].totalSales, 0.001) // 10:00 bucket
        assertEquals(1, result.topProducts.size)
        assertEquals("Beras", result.topProducts.first().productName)
    }

    @Test
    fun salesReportCalculator_computesDateMonthYear_withHppGrossProfitOmsetAndExpenses() {
        fun millisAt(year: Int, monthZero: Int, day: Int, hour: Int): Long =
            Calendar.getInstance().apply {
                set(year, monthZero, day, hour, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

        val mar10Millis = millisAt(2026, Calendar.MARCH, 10, 10)
        val mar18Millis = millisAt(2026, Calendar.MARCH, 18, 14)
        val feb05Millis = millisAt(2026, Calendar.FEBRUARY, 5, 11)

        // Day 1 (Mar 10): subtotal 100_000, discount 10_000, tax 9_900 -> net sales 90_000, omset 99_900, hpp 60_000, gross 30_000
        val txMar10 = PosTransaction(
            id = "r1",
            subtotal = 100000.0,
            discountAmount = 10000.0,
            taxAmount = 9900.0,
            totalAmount = 99900.0,
            paymentMethod = PaymentMethod.CASH.code,
            status = TransactionStatus.COMPLETED.code,
            transactionMillis = mar10Millis,
            items = listOf(
                TransactionItem("p1", "Kopi", "Pcs", 25000.0, 15000.0, 4.0, 100000.0)
            )
        )
        // Cancelled tx on Mar 10 must be excluded
        val txMar10Cancelled = PosTransaction(
            id = "r2",
            subtotal = 50000.0,
            totalAmount = 50000.0,
            status = TransactionStatus.CANCELLED.code,
            transactionMillis = mar10Millis,
            items = listOf(
                TransactionItem("p2", "Teh", "Pcs", 10000.0, 5000.0, 5.0, 50000.0)
            )
        )
        // Day 2 in March (Mar 18): net sales 40_000, omset 40_000, hpp 24_000, gross 16_000
        val txMar18 = PosTransaction(
            id = "r3",
            subtotal = 40000.0,
            discountAmount = 0.0,
            taxAmount = 0.0,
            totalAmount = 40000.0,
            paymentMethod = PaymentMethod.QRIS.code,
            status = TransactionStatus.COMPLETED.code,
            transactionMillis = mar18Millis,
            items = listOf(
                TransactionItem("p3", "Roti", "Pcs", 20000.0, 12000.0, 2.0, 40000.0)
            )
        )
        // February tx (same year 2026): net sales 50_000, omset 50_000, hpp 30_000, gross 20_000
        val txFeb = PosTransaction(
            id = "r4",
            subtotal = 50000.0,
            totalAmount = 50000.0,
            paymentMethod = PaymentMethod.BANK_TRANSFER.code,
            status = TransactionStatus.COMPLETED.code,
            transactionMillis = feb05Millis,
            items = listOf(
                TransactionItem("p1", "Kopi", "Pcs", 25000.0, 15000.0, 2.0, 50000.0)
            )
        )

        val expenses = listOf(
            Expense(id = "e1", title = "Plastik", amount = 5000.0, expenseMillis = mar10Millis),
            Expense(id = "e2", title = "Listrik", amount = 10000.0, expenseMillis = mar18Millis),
            Expense(id = "e3", title = "Air", amount = 8000.0, expenseMillis = feb05Millis)
        )
        val allTx = listOf(txMar10, txMar10Cancelled, txMar18, txFeb)

        // 1. DATE filter (2026-03-10)
        val dateReport = SalesReportCalculator.computeReport(
            transactions = allTx,
            expenses = expenses,
            photoLookup = emptyMap(),
            mode = ReportFilterMode.DATE,
            year = 2026,
            monthZeroBased = Calendar.MARCH,
            dayOfMonth = 10
        )
        assertEquals(1, dateReport.transactionCount)
        assertEquals(90000.0, dateReport.totalSalesNet, 0.001)
        assertEquals(99900.0, dateReport.totalOmset, 0.001)
        assertEquals(60000.0, dateReport.totalHpp, 0.001)
        assertEquals(30000.0, dateReport.grossProfit, 0.001)
        assertEquals(5000.0, dateReport.totalExpenses, 0.001)
        assertEquals(25000.0, dateReport.netProfit, 0.001)

        // 2. MONTH filter (March 2026)
        val monthReport = SalesReportCalculator.computeReport(
            transactions = allTx,
            expenses = expenses,
            photoLookup = emptyMap(),
            mode = ReportFilterMode.MONTH,
            year = 2026,
            monthZeroBased = Calendar.MARCH,
            dayOfMonth = 10
        )
        assertEquals(2, monthReport.transactionCount)
        assertEquals(130000.0, monthReport.totalSalesNet, 0.001)
        assertEquals(139900.0, monthReport.totalOmset, 0.001)
        assertEquals(84000.0, monthReport.totalHpp, 0.001)
        assertEquals(46000.0, monthReport.grossProfit, 0.001)
        assertEquals(15000.0, monthReport.totalExpenses, 0.001)
        assertEquals(31000.0, monthReport.netProfit, 0.001)

        // 3. YEAR filter (2026)
        val yearReport = SalesReportCalculator.computeReport(
            transactions = allTx,
            expenses = expenses,
            photoLookup = emptyMap(),
            mode = ReportFilterMode.YEAR,
            year = 2026,
            monthZeroBased = Calendar.MARCH,
            dayOfMonth = 10
        )
        assertEquals(3, yearReport.transactionCount)
        assertEquals(180000.0, yearReport.totalSalesNet, 0.001)
        assertEquals(189900.0, yearReport.totalOmset, 0.001)
        assertEquals(114000.0, yearReport.totalHpp, 0.001)
        assertEquals(66000.0, yearReport.grossProfit, 0.001)
        assertEquals(23000.0, yearReport.totalExpenses, 0.001)
        assertEquals(43000.0, yearReport.netProfit, 0.001)
    }

    @Test
    fun receiptHelper_honorsCustomizationTogglesAndFooterMessage() {
        val tx = PosTransaction(
            id = "tx_rcpt",
            transactionNumber = "AK-TEST-100",
            customerName = "Ibu Sari",
            cashierName = "Kasir Andi",
            items = listOf(
                TransactionItem("p1", "Gula Pasir", "Kg", 16000.0, 12000.0, 1.0, 16000.0)
            ),
            subtotal = 16000.0,
            totalAmount = 16000.0,
            paymentMethod = PaymentMethod.CASH.code,
            amountPaid = 20000.0,
            changeAmount = 4000.0,
            transactionMillis = System.currentTimeMillis()
        )
        val strings = com.example.ui.theme.IndonesianStrings

        val fullStore = StoreSettings(
            storeName = "Toko Berkah",
            phone = "08111222333",
            address = "Jl. Merdeka 10",
            showReceiptLogo = true,
            showReceiptCashierName = true,
            showReceiptStorePhone = true,
            showReceiptStoreAddress = true,
            showReceiptCustomerName = true,
            receiptFooterMessage = "Barang sudah dibeli tidak dapat ditukar"
        )
        val fullReceipt = com.example.util.ReceiptHelper.buildPlainTextReceipt(
            transaction = tx,
            store = fullStore,
            strings = strings,
            language = "id",
            charsPerLine = 48
        )
        org.junit.Assert.assertTrue(fullReceipt.contains("Jl. Merdeka 10"))
        org.junit.Assert.assertTrue(fullReceipt.contains("08111222333"))
        org.junit.Assert.assertTrue(fullReceipt.contains("Kasir Andi"))
        org.junit.Assert.assertTrue(fullReceipt.contains("Ibu Sari"))
        org.junit.Assert.assertTrue(fullReceipt.contains("Barang sudah dibeli"))

        val hiddenStore = fullStore.copy(
            showReceiptCashierName = false,
            showReceiptStorePhone = false,
            showReceiptStoreAddress = false,
            showReceiptCustomerName = false,
            receiptFooterMessage = "Terima kasih kakak"
        )
        val minimalReceipt = com.example.util.ReceiptHelper.buildPlainTextReceipt(
            transaction = tx,
            store = hiddenStore,
            strings = strings,
            language = "id",
            charsPerLine = 48
        )
        org.junit.Assert.assertFalse(minimalReceipt.contains("Jl. Merdeka 10"))
        org.junit.Assert.assertFalse(minimalReceipt.contains("08111222333"))
        org.junit.Assert.assertFalse(minimalReceipt.contains("Kasir Andi"))
        org.junit.Assert.assertFalse(minimalReceipt.contains("Ibu Sari"))
        org.junit.Assert.assertTrue(minimalReceipt.contains("Terima kasih kakak"))
    }
}
