package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.max
import kotlin.math.roundToLong

enum class ProductUnit(val label: String, val supportsDecimalPresets: Boolean = false) {
    PCS("Pcs"),
    KG("Kg", supportsDecimalPresets = true),
    GRAM("Gram"),
    LITER("Liter", supportsDecimalPresets = true),
    ML("Ml"),
    METER("Meter", supportsDecimalPresets = true),
    DUS("Dus"),
    PACK("Pack"),
    BOTOL("Botol"),
    KARUNG("Karung");

    companion object {
        fun fromLabel(label: String): ProductUnit {
            return entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: PCS
        }

        val allLabels: List<String> = entries.map { it.label }
        val decimalPresets: List<Double> = listOf(0.25, 0.50, 1.0, 2.0)
    }
}

enum class DiscountType(val code: String) {
    NONE("NONE"),
    PERCENT("PERCENT"),
    AMOUNT("AMOUNT");

    companion object {
        fun fromCode(code: String): DiscountType =
            entries.firstOrNull { it.code == code } ?: NONE
    }
}

enum class TaxType(val code: String) {
    NONE("NONE"),
    AUTO("AUTO"),
    MANUAL("MANUAL");

    companion object {
        fun fromCode(code: String): TaxType =
            entries.firstOrNull { it.code == code } ?: NONE
    }
}

enum class PaymentMethod(val code: String) {
    CASH("CASH"),
    BANK_TRANSFER("BANK_TRANSFER"),
    QRIS("QRIS"),
    OTHER("OTHER");

    companion object {
        fun fromCode(code: String): PaymentMethod =
            entries.firstOrNull { it.code == code } ?: CASH
    }
}

enum class TransactionStatus(val code: String) {
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED"),
    REFUNDED("REFUNDED");

    companion object {
        fun fromCode(code: String): TransactionStatus =
            entries.firstOrNull { it.code == code } ?: COMPLETED
    }
}

enum class DashboardPeriod {
    TODAY,
    SEVEN_DAYS,
    THIRTY_DAYS,
    THIS_MONTH
}

enum class AppAccessMode(val code: String) {
    CASHIER("CASHIER"),
    MONITOR("MONITOR");

    companion object {
        fun fromCode(code: String): AppAccessMode =
            entries.firstOrNull { it.code == code } ?: CASHIER
    }
}

@IgnoreExtraProperties
data class StoreSettings(
    @DocumentId val id: String = "app",
    val userId: String = "",
    val storeName: String = "Toko ADA KASIR",
    val ownerName: String = "",
    val phone: String = "",
    val address: String = "",
    val photoData: String = "",
    val storeIcon: String = "storefront",
    val storeIconBgColor: String = "#2563EB",
    val receiptPrefix: String = "AK",
    val darkTheme: Boolean = false,
    val language: String = "id",
    val defaultTaxPercent: Double = 11.0,
    val ownerEmail: String = "",
    val monitorEmails: List<String> = emptyList(),
    val showReceiptLogo: Boolean = true,
    val showReceiptCashierName: Boolean = true,
    val showReceiptStorePhone: Boolean = true,
    val showReceiptStoreAddress: Boolean = true,
    val showReceiptCustomerName: Boolean = true,
    val receiptFooterMessage: String = "",
    val lastBackupAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ProductCategory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val isDefault: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val sku: String = "",
    val category: String = "",
    val unit: String = ProductUnit.PCS.label,
    val costPrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val stock: Double = 0.0,
    val minStock: Double = 5.0,
    val description: String = "",
    val photoData: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isOutOfStock: Boolean get() = stock <= 0.0001
    val isLowStock: Boolean get() = !isOutOfStock && minStock > 0.0 && stock <= minStock
    val supportsDecimal: Boolean get() = ProductUnit.fromLabel(unit).supportsDecimalPresets
}

@IgnoreExtraProperties
data class TransactionItem(
    val productId: String = "",
    val productName: String = "",
    val unit: String = ProductUnit.PCS.label,
    val unitPrice: Double = 0.0,
    val costPrice: Double = 0.0,
    val quantity: Double = 0.0,
    val subtotal: Double = 0.0
)

@IgnoreExtraProperties
data class PosTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val transactionNumber: String = "",
    val customerName: String = "",
    val cashierName: String = "",
    val items: List<TransactionItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discountType: String = DiscountType.NONE.code,
    val discountValue: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxType: String = TaxType.NONE.code,
    val taxPercent: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentMethod: String = PaymentMethod.CASH.code,
    val amountPaid: Double = 0.0,
    val changeAmount: Double = 0.0,
    val status: String = TransactionStatus.COMPLETED.code,
    val statusReason: String = "",
    val stockDeducted: Boolean = true,
    val stockRestored: Boolean = false,
    val transactionMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CartLine(
    val product: Product,
    val quantity: Double
) {
    val subtotal: Double get() = PosCalculator.roundCurrency(product.sellPrice * quantity)
}

data class CheckoutTotals(
    val subtotal: Double,
    val discountAmount: Double,
    val taxableBase: Double,
    val taxAmount: Double,
    val grandTotal: Double,
    val changeAmount: Double
)

object PosCalculator {
    fun roundCurrency(value: Double): Double =
        (value.coerceAtLeast(0.0)).roundToLong().toDouble()

    fun roundQuantity(value: Double): Double =
        ((value.coerceAtLeast(0.0)) * 1000.0).roundToLong() / 1000.0

    /**
     * Single source of truth for POS checkout calculation.
     * - Discount is computed from subtotal and clamped to [0, subtotal].
     * - Tax base is consistently (subtotal - discountAmount).
     * - PPN is computed ONCE: 0 for NONE, taxableBase * taxPercent / 100 for AUTO, or manual amount for MANUAL.
     * - Change is max(0, amountPaid - grandTotal).
     */
    fun calculateTotals(
        subtotal: Double,
        discountType: DiscountType,
        discountValue: Double,
        taxType: TaxType,
        autoTaxPercent: Double,
        manualTaxAmount: Double,
        amountPaid: Double
    ): CheckoutTotals {
        val safeSubtotal = roundCurrency(subtotal)
        val rawDiscount = when (discountType) {
            DiscountType.NONE -> 0.0
            DiscountType.PERCENT -> safeSubtotal * (discountValue.coerceIn(0.0, 100.0) / 100.0)
            DiscountType.AMOUNT -> discountValue.coerceAtLeast(0.0)
        }
        val discountAmount = roundCurrency(rawDiscount).coerceIn(0.0, safeSubtotal)
        val taxableBase = max(0.0, safeSubtotal - discountAmount)
        val taxAmount = when (taxType) {
            TaxType.NONE -> 0.0
            TaxType.AUTO -> roundCurrency(taxableBase * (autoTaxPercent.coerceIn(0.0, 100.0) / 100.0))
            TaxType.MANUAL -> roundCurrency(manualTaxAmount.coerceAtLeast(0.0))
        }
        val grandTotal = roundCurrency(taxableBase + taxAmount)
        val change = roundCurrency(max(0.0, amountPaid - grandTotal))
        return CheckoutTotals(
            subtotal = safeSubtotal,
            discountAmount = discountAmount,
            taxableBase = taxableBase,
            taxAmount = taxAmount,
            grandTotal = grandTotal,
            changeAmount = change
        )
    }
}

enum class ReportFilterMode {
    DATE,
    MONTH,
    YEAR
}

enum class ExpenseCategory(val labelId: String, val labelEn: String) {
    OPERATIONAL("Operasional", "Operational"),
    STOCK_PURCHASE("Belanja Stok", "Stock Purchase"),
    UTILITIES("Listrik & Air", "Utilities"),
    SALARY("Gaji & Bonus", "Salary & Bonus"),
    TRANSPORT("Transportasi", "Transport"),
    OTHER("Lainnya", "Other");

    companion object {
        fun labelFor(codeOrName: String, language: String): String {
            val found = entries.firstOrNull {
                it.name.equals(codeOrName, ignoreCase = true) ||
                    it.labelId.equals(codeOrName, ignoreCase = true) ||
                    it.labelEn.equals(codeOrName, ignoreCase = true)
            }
            return if (found != null) {
                if (language == "en") found.labelEn else found.labelId
            } else {
                codeOrName.ifBlank { if (language == "en") OTHER.labelEn else OTHER.labelId }
            }
        }
    }
}

@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = ExpenseCategory.OPERATIONAL.labelId,
    val amount: Double = 0.0,
    val notes: String = "",
    val expenseMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StoreDirectoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val ownerEmail: String = "",
    val storeName: String = "",
    val monitorEmails: List<String> = emptyList(),
    @ServerTimestamp val updatedAt: Timestamp? = null
)

