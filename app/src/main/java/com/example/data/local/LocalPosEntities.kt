package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Expense
import com.example.data.model.PosTransaction
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ProductUnit
import com.example.data.model.StoreSettings
import com.example.data.model.TransactionItem
import com.google.firebase.Timestamp
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

@Entity(tableName = "store_settings")
data class StoreSettingsEntity(
    @PrimaryKey val id: String = "app",
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
    val showReceiptLogo: Boolean = true,
    val showReceiptCashierName: Boolean = true,
    val showReceiptStorePhone: Boolean = true,
    val showReceiptStoreAddress: Boolean = true,
    val showReceiptCustomerName: Boolean = true,
    val receiptFooterMessage: String = "",
    val lastBackupMillis: Long? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isDefault: Boolean = false,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sku: String = "",
    val category: String = "Lainnya",
    val unit: String = ProductUnit.PCS.label,
    val costPrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val stock: Double = 0.0,
    val minStock: Double = 5.0,
    val description: String = "",
    val photoData: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val transactionNumber: String,
    val customerName: String = "",
    val cashierName: String = "",
    val itemsJson: String = "[]",
    val subtotal: Double = 0.0,
    val discountType: String = "NONE",
    val discountValue: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxType: String = "NONE",
    val taxPercent: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "CASH",
    val amountPaid: Double = 0.0,
    val changeAmount: Double = 0.0,
    val status: String = "COMPLETED",
    val statusReason: String = "",
    val stockDeducted: Boolean = true,
    val stockRestored: Boolean = false,
    val transactionMillis: Long = 0L,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val amount: Double,
    val notes: String = "",
    val expenseMillis: Long,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

private fun Long?.toTimestampOrNull(): Timestamp? =
    if (this != null && this > 0L) Timestamp(Date(this)) else null

fun StoreSettingsEntity.toDomain(localUserId: String = "local_device"): StoreSettings =
    StoreSettings(
        id = id,
        userId = localUserId,
        storeName = storeName,
        ownerName = ownerName,
        phone = phone,
        address = address,
        photoData = photoData,
        storeIcon = storeIcon,
        storeIconBgColor = storeIconBgColor,
        receiptPrefix = receiptPrefix,
        darkTheme = darkTheme,
        language = language,
        defaultTaxPercent = defaultTaxPercent,
        ownerEmail = "",
        monitorEmails = emptyList(),
        showReceiptLogo = showReceiptLogo,
        showReceiptCashierName = showReceiptCashierName,
        showReceiptStorePhone = showReceiptStorePhone,
        showReceiptStoreAddress = showReceiptStoreAddress,
        showReceiptCustomerName = showReceiptCustomerName,
        receiptFooterMessage = receiptFooterMessage,
        lastBackupAt = lastBackupMillis.toTimestampOrNull(),
        createdAt = createdAtMillis.toTimestampOrNull(),
        updatedAt = updatedAtMillis.toTimestampOrNull()
    )

fun CategoryEntity.toDomain(localUserId: String = "local_device"): ProductCategory =
    ProductCategory(
        id = id,
        userId = localUserId,
        name = name,
        isDefault = isDefault,
        createdAt = createdAtMillis.toTimestampOrNull(),
        updatedAt = updatedAtMillis.toTimestampOrNull()
    )

fun ProductEntity.toDomain(localUserId: String = "local_device"): Product =
    Product(
        id = id,
        userId = localUserId,
        name = name,
        sku = sku,
        category = category,
        unit = unit,
        costPrice = costPrice,
        sellPrice = sellPrice,
        stock = stock,
        minStock = minStock,
        description = description,
        photoData = photoData,
        createdAt = createdAtMillis.toTimestampOrNull(),
        updatedAt = updatedAtMillis.toTimestampOrNull()
    )

fun ExpenseEntity.toDomain(localUserId: String = "local_device"): Expense =
    Expense(
        id = id,
        userId = localUserId,
        title = title,
        category = category,
        amount = amount,
        notes = notes,
        expenseMillis = expenseMillis,
        createdAt = createdAtMillis.toTimestampOrNull(),
        updatedAt = updatedAtMillis.toTimestampOrNull()
    )

fun serializeTransactionItems(items: List<TransactionItem>): String {
    val arr = JSONArray()
    items.forEach { item ->
        arr.put(JSONObject().apply {
            put("productId", item.productId)
            put("productName", item.productName)
            put("unit", item.unit)
            put("unitPrice", item.unitPrice)
            put("costPrice", item.costPrice)
            put("quantity", item.quantity)
            put("subtotal", item.subtotal)
        })
    }
    return arr.toString()
}

fun deserializeTransactionItems(json: String): List<TransactionItem> {
    if (json.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                add(
                    TransactionItem(
                        productId = obj.optString("productId", ""),
                        productName = obj.optString("productName", ""),
                        unit = obj.optString("unit", ProductUnit.PCS.label),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        costPrice = obj.optDouble("costPrice", 0.0),
                        quantity = obj.optDouble("quantity", 0.0),
                        subtotal = obj.optDouble("subtotal", 0.0)
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

fun TransactionEntity.toDomain(localUserId: String = "local_device"): PosTransaction =
    PosTransaction(
        id = id,
        userId = localUserId,
        transactionNumber = transactionNumber,
        customerName = customerName,
        cashierName = cashierName,
        items = deserializeTransactionItems(itemsJson),
        subtotal = subtotal,
        discountType = discountType,
        discountValue = discountValue,
        discountAmount = discountAmount,
        taxType = taxType,
        taxPercent = taxPercent,
        taxAmount = taxAmount,
        totalAmount = totalAmount,
        paymentMethod = paymentMethod,
        amountPaid = amountPaid,
        changeAmount = changeAmount,
        status = status,
        statusReason = statusReason,
        stockDeducted = stockDeducted,
        stockRestored = stockRestored,
        transactionMillis = transactionMillis,
        createdAt = createdAtMillis.toTimestampOrNull(),
        updatedAt = updatedAtMillis.toTimestampOrNull()
    )
