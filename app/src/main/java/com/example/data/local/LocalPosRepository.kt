package com.example.data.local

import androidx.room.withTransaction
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.PosCalculator
import com.example.data.model.PosTransaction
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ProductUnit
import com.example.data.model.StoreSettings
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionStatus
import com.example.util.PosFormatters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class BackupSummary(
    val storeName: String,
    val categoryCount: Int,
    val productCount: Int,
    val transactionCount: Int,
    val expenseCount: Int,
    val exportedAtMillis: Long
)

class LocalPosRepository(private val database: LocalPosDatabase) {

    private val dao: LocalPosDao = database.posDao()

    fun observeSettings(): Flow<StoreSettings> =
        dao.observeSettings().map { entity ->
            entity?.toDomain() ?: StoreSettings(userId = LOCAL_USER_ID)
        }

    fun observeCategories(): Flow<List<ProductCategory>> =
        dao.observeCategories().map { list -> list.map { it.toDomain() } }

    fun observeProducts(): Flow<List<Product>> =
        dao.observeProducts().map { list -> list.map { it.toDomain() } }

    fun observeTransactions(): Flow<List<PosTransaction>> =
        dao.observeTransactions().map { list -> list.map { it.toDomain() } }

    fun observeExpenses(): Flow<List<Expense>> =
        dao.observeExpenses().map { list -> list.map { it.toDomain() } }

    suspend fun ensureInitialLocalSetup(
        businessName: String,
        defaultDarkTheme: Boolean,
        defaultLanguage: String,
        seedSampleProducts: Boolean,
        seedKgPackSamples: Boolean = seedSampleProducts
    ) {
        val now = System.currentTimeMillis()
        val resolvedName = businessName.trim().ifBlank { "Toko ADA KASIR" }.take(80)
        database.withTransaction {
            val existingSettings = dao.getSettings()
            if (existingSettings == null) {
                dao.upsertSettings(
                    StoreSettingsEntity(
                        id = "app",
                        storeName = resolvedName,
                        ownerName = "",
                        phone = "",
                        address = "",
                        photoData = "",
                        storeIcon = "storefront",
                        storeIconBgColor = "#2563EB",
                        receiptPrefix = "AK",
                        darkTheme = defaultDarkTheme,
                        language = if (defaultLanguage == "en") "en" else "id",
                        defaultTaxPercent = 11.0,
                        showReceiptLogo = true,
                        showReceiptCashierName = true,
                        showReceiptStorePhone = true,
                        showReceiptStoreAddress = true,
                        showReceiptCustomerName = true,
                        receiptFooterMessage = "",
                        lastBackupMillis = null,
                        createdAtMillis = now,
                        updatedAtMillis = now
                    )
                )
            } else if (existingSettings.storeName == "Toko ADA KASIR" && resolvedName != "Toko ADA KASIR") {
                dao.upsertSettings(
                    existingSettings.copy(
                        storeName = resolvedName,
                        updatedAtMillis = now
                    )
                )
            }

            if (dao.countCategories() == 0) {
                val defaults = listOf(
                    "cat_makanan" to "Makanan",
                    "cat_minuman" to "Minuman",
                    "cat_sembako" to "Sembako",
                    "cat_sayuran" to "Sayuran",
                    "cat_buah" to "Buah-buahan",
                    "cat_lainnya" to "Lainnya"
                ).map { (id, name) ->
                    CategoryEntity(
                        id = id,
                        name = name,
                        isDefault = true,
                        createdAtMillis = now,
                        updatedAtMillis = now
                    )
                }
                dao.upsertCategories(defaults)
            }

            // Backfill bundled product images on previously seeded sample products if they have no custom photo
            val sampleImageBackfill = mapOf(
                "prod_sample_kopi_aren" to "drawable:img_prod_kopi_aren",
                "prod_sample_beras_5kg" to "drawable:img_prod_beras_5kg",
                "prod_sample_gula_kg" to "drawable:img_prod_gula_kg",
                "prod_sample_mie_pack" to "drawable:img_prod_mie_pack"
            )
            sampleImageBackfill.forEach { (prodId, drawableRef) ->
                val existingProd = dao.getProductById(prodId)
                if (existingProd != null && existingProd.photoData.isBlank()) {
                    dao.upsertProduct(existingProd.copy(photoData = drawableRef, updatedAtMillis = now))
                }
            }

            if (seedSampleProducts) {
                val baseSamples = listOf(
                    ProductEntity(
                        id = "prod_sample_kopi_aren",
                        name = "Kopi Susu Gula Aren",
                        sku = "KSA-001",
                        category = "Minuman",
                        unit = "Botol",
                        costPrice = 10000.0,
                        sellPrice = 18000.0,
                        stock = 24.0,
                        minStock = 5.0,
                        description = "Contoh produk minuman kopi susu kemasan 250 ml",
                        photoData = "drawable:img_prod_kopi_aren",
                        createdAtMillis = now,
                        updatedAtMillis = now
                    ),
                    ProductEntity(
                        id = "prod_sample_beras_5kg",
                        name = "Beras Premium 5 Kg",
                        sku = "BRP-005",
                        category = "Sembako",
                        unit = "Pcs",
                        costPrice = 62000.0,
                        sellPrice = 75000.0,
                        stock = 15.0,
                        minStock = 3.0,
                        description = "Contoh produk sembako beras pulen kemasan 5 kg",
                        photoData = "drawable:img_prod_beras_5kg",
                        createdAtMillis = now,
                        updatedAtMillis = now
                    )
                )
                baseSamples.forEach { sample ->
                    if (dao.getProductById(sample.id) == null) {
                        dao.upsertProduct(sample)
                    }
                }
            }

            if (seedKgPackSamples) {
                val unitSamples = listOf(
                    ProductEntity(
                        id = "prod_sample_gula_kg",
                        name = "Gula Pasir Kristal",
                        sku = "GLP-001",
                        category = "Sembako",
                        unit = "Kg",
                        costPrice = 14500.0,
                        sellPrice = 17500.0,
                        stock = 25.0,
                        minStock = 5.0,
                        description = "Contoh produk curah satuan Kg (mendukung jumlah desimal seperti 0.5 Kg)",
                        photoData = "drawable:img_prod_gula_kg",
                        createdAtMillis = now,
                        updatedAtMillis = now
                    ),
                    ProductEntity(
                        id = "prod_sample_mie_pack",
                        name = "Mie Instan Goreng Pack",
                        sku = "MIE-005",
                        category = "Makanan",
                        unit = "Pack",
                        costPrice = 13500.0,
                        sellPrice = 16500.0,
                        stock = 30.0,
                        minStock = 5.0,
                        description = "Contoh produk makanan kemasan isi 5 bungkus per pack",
                        photoData = "drawable:img_prod_mie_pack",
                        createdAtMillis = now,
                        updatedAtMillis = now
                    )
                )
                unitSamples.forEach { sample ->
                    if (dao.getProductById(sample.id) == null) {
                        dao.upsertProduct(sample)
                    }
                }
            }
        }
    }

    suspend fun saveStoreProfile(
        storeName: String,
        ownerName: String,
        phone: String,
        address: String,
        photoData: String,
        storeIcon: String = "storefront",
        storeIconBgColor: String = "#2563EB",
        receiptPrefix: String = "AK",
        defaultTaxPercent: Double
    ): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val current = dao.getSettings() ?: StoreSettingsEntity(createdAtMillis = now)
        val cleanIcon = storeIcon.trim().ifBlank { "storefront" }.take(32)
        val cleanBg = storeIconBgColor.trim().takeIf { it.startsWith("#") && (it.length == 7 || it.length == 9) } ?: "#2563EB"
        val cleanPrefix = PosFormatters.sanitizeReceiptPrefix(receiptPrefix)
        dao.upsertSettings(
            current.copy(
                storeName = storeName.trim().ifBlank { "Toko ADA KASIR" }.take(80),
                ownerName = ownerName.trim().take(80),
                phone = phone.trim().take(30),
                address = address.trim().take(250),
                photoData = photoData.take(250000),
                storeIcon = cleanIcon,
                storeIconBgColor = cleanBg,
                receiptPrefix = cleanPrefix,
                defaultTaxPercent = defaultTaxPercent.coerceIn(0.0, 100.0),
                updatedAtMillis = now
            )
        )
    }.onFailure { if (it is CancellationException) throw it }

    suspend fun saveReceiptCustomization(
        showLogo: Boolean,
        showCashierName: Boolean,
        showStorePhone: Boolean,
        showStoreAddress: Boolean,
        showCustomerName: Boolean,
        footerMessage: String
    ): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val current = dao.getSettings() ?: StoreSettingsEntity(createdAtMillis = now)
        dao.upsertSettings(
            current.copy(
                showReceiptLogo = showLogo,
                showReceiptCashierName = showCashierName,
                showReceiptStorePhone = showStorePhone,
                showReceiptStoreAddress = showStoreAddress,
                showReceiptCustomerName = showCustomerName,
                receiptFooterMessage = footerMessage.trim().take(200),
                updatedAtMillis = now
            )
        )
    }.onFailure { if (it is CancellationException) throw it }

    suspend fun updateTheme(darkTheme: Boolean): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val current = dao.getSettings() ?: StoreSettingsEntity(createdAtMillis = now)
        dao.upsertSettings(current.copy(darkTheme = darkTheme, updatedAtMillis = now))
    }.onFailure { if (it is CancellationException) throw it }

    suspend fun updateLanguage(language: String): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val cleanLang = if (language == "en") "en" else "id"
        val current = dao.getSettings() ?: StoreSettingsEntity(createdAtMillis = now)
        dao.upsertSettings(current.copy(language = cleanLang, updatedAtMillis = now))
    }.onFailure { if (it is CancellationException) throw it }

    suspend fun addCategory(name: String): Result<String> = runCatching {
        val cleanName = name.trim().take(60)
        require(cleanName.isNotBlank()) { "Category name cannot be empty" }
        val now = System.currentTimeMillis()
        val id = "cat_" + UUID.randomUUID().toString().replace("-", "").take(12)
        dao.upsertCategory(
            CategoryEntity(
                id = id,
                name = cleanName,
                isDefault = false,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
        id
    }.onFailure { if (it is CancellationException) throw it }

    suspend fun updateCategory(categoryId: String, newName: String): Result<Unit> = runCatching {
        val cleanName = newName.trim().take(60)
        require(categoryId.isNotBlank() && cleanName.isNotBlank())
        val existing = dao.getCategoryById(categoryId) ?: throw IllegalStateException("Category not found")
        dao.upsertCategory(
            existing.copy(
                name = cleanName,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }.onFailure { if (it is CancellationException) throw it }

    suspend fun deleteCategory(categoryId: String): Result<Unit> = runCatching {
        dao.deleteCategoryById(categoryId)
    }.onFailure { if (it is CancellationException) throw it }

    suspend fun saveProduct(product: Product): Result<String> = runCatching {
        val cleanName = product.name.trim().take(120)
        require(cleanName.isNotBlank()) { "Product name is required" }
        val validUnit = ProductUnit.fromLabel(product.unit).label
        val cleanCategory = product.category.trim().ifBlank { "Lainnya" }.take(60)
        val now = System.currentTimeMillis()
        val id = product.id.ifBlank {
            "prod_" + UUID.randomUUID().toString().replace("-", "").take(16)
        }
        val existing = dao.getProductById(id)
        dao.upsertProduct(
            ProductEntity(
                id = id,
                name = cleanName,
                sku = product.sku.trim().take(60),
                category = cleanCategory,
                unit = validUnit,
                costPrice = product.costPrice.coerceAtLeast(0.0),
                sellPrice = product.sellPrice.coerceAtLeast(0.0),
                stock = PosCalculator.roundQuantity(product.stock.coerceAtLeast(0.0)),
                minStock = PosCalculator.roundQuantity(product.minStock.coerceAtLeast(0.0)),
                description = product.description.trim().take(500),
                photoData = product.photoData.take(250000),
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now
            )
        )
        id
    }.onFailure { if (it is CancellationException) throw it }

    suspend fun deleteProduct(productId: String): Result<Unit> = runCatching {
        dao.deleteProductById(productId)
    }.onFailure { if (it is CancellationException) throw it }

    suspend fun saveExpense(expense: Expense): Result<String> = runCatching {
        val cleanTitle = expense.title.trim().take(120)
        require(cleanTitle.isNotBlank()) { "Expense title is required" }
        val cleanCategory = expense.category.trim().ifBlank { ExpenseCategory.OPERATIONAL.labelId }.take(60)
        val now = System.currentTimeMillis()
        val safeMillis = if (expense.expenseMillis > 0L) expense.expenseMillis else now
        val safeAmount = PosCalculator.roundCurrency(expense.amount.coerceAtLeast(0.0))
        val id = expense.id.ifBlank {
            "exp_" + UUID.randomUUID().toString().replace("-", "").take(16)
        }
        val existing = dao.getExpenseById(id)
        dao.upsertExpense(
            ExpenseEntity(
                id = id,
                title = cleanTitle,
                category = cleanCategory,
                amount = safeAmount,
                notes = expense.notes.trim().take(400),
                expenseMillis = safeMillis,
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now
            )
        )
        id
    }.onFailure { if (it is CancellationException) throw it }

    suspend fun deleteExpense(expenseId: String): Result<Unit> = runCatching {
        dao.deleteExpenseById(expenseId)
    }.onFailure { if (it is CancellationException) throw it }

    /**
     * Atomically records a POS transaction and deducts local SQLite product stock.
     */
    suspend fun completeTransaction(transaction: PosTransaction): Result<PosTransaction> = runCatching {
        require(transaction.items.isNotEmpty()) { "Cart cannot be empty" }
        val txId = transaction.id.ifBlank {
            "tx_" + UUID.randomUUID().toString().replace("-", "").take(16)
        }
        val now = System.currentTimeMillis()
        val txMillis = if (transaction.transactionMillis > 0L) transaction.transactionMillis else now

        database.withTransaction {
            val existing = dao.getTransactionById(txId)
            if (existing != null) {
                return@withTransaction existing.toDomain()
            }

            // Validate & compute stock updates
            val stockUpdates = mutableListOf<Pair<String, Double>>()
            for (item in transaction.items) {
                val prod = dao.getProductById(item.productId)
                    ?: throw IllegalStateException("Product ${item.productName} not found")
                if (prod.stock + 0.0001 < item.quantity) {
                    throw IllegalStateException("Insufficient stock for ${item.productName}")
                }
                val newStock = PosCalculator.roundQuantity((prod.stock - item.quantity).coerceAtLeast(0.0))
                stockUpdates += prod.id to newStock
            }

            stockUpdates.forEach { (prodId, newStock) ->
                dao.updateProductStock(prodId, newStock, now)
            }

            val sanitizedItems = transaction.items.map { item ->
                item.copy(
                    productName = item.productName.take(120),
                    quantity = PosCalculator.roundQuantity(item.quantity)
                )
            }

            val entity = TransactionEntity(
                id = txId,
                transactionNumber = transaction.transactionNumber.take(60),
                customerName = transaction.customerName.trim().take(100),
                cashierName = transaction.cashierName.trim().take(80),
                itemsJson = serializeTransactionItems(sanitizedItems),
                subtotal = transaction.subtotal,
                discountType = transaction.discountType,
                discountValue = transaction.discountValue,
                discountAmount = transaction.discountAmount,
                taxType = transaction.taxType,
                taxPercent = transaction.taxPercent,
                taxAmount = transaction.taxAmount,
                totalAmount = transaction.totalAmount,
                paymentMethod = transaction.paymentMethod,
                amountPaid = transaction.amountPaid,
                changeAmount = transaction.changeAmount,
                status = TransactionStatus.COMPLETED.code,
                statusReason = "",
                stockDeducted = true,
                stockRestored = false,
                transactionMillis = txMillis,
                createdAtMillis = now,
                updatedAtMillis = now
            )
            dao.upsertTransaction(entity)
            entity.toDomain()
        }
    }.onFailure { if (it is CancellationException) throw it }

    /**
     * Atomically cancels or refunds a COMPLETED transaction and restores product stock once.
     */
    suspend fun cancelOrRefundTransaction(
        transactionId: String,
        targetStatus: TransactionStatus,
        reason: String
    ): Result<Unit> = runCatching {
        require(targetStatus == TransactionStatus.CANCELLED || targetStatus == TransactionStatus.REFUNDED)
        val now = System.currentTimeMillis()
        database.withTransaction {
            val tx = dao.getTransactionById(transactionId)
                ?: throw IllegalStateException("Transaction not found")
            if (tx.status != TransactionStatus.COMPLETED.code || tx.stockRestored) {
                throw IllegalStateException("Transaction already cancelled or refunded")
            }

            val items = deserializeTransactionItems(tx.itemsJson)
            items.forEach { item ->
                if (item.quantity > 0.0) {
                    val prod = dao.getProductById(item.productId)
                    if (prod != null) {
                        val restored = PosCalculator.roundQuantity(prod.stock + item.quantity)
                        dao.updateProductStock(prod.id, restored, now)
                    }
                }
            }

            dao.markTransactionCancelledOrRefunded(
                id = transactionId,
                status = targetStatus.code,
                reason = reason.trim().take(250),
                updatedAt = now
            )
        }
    }.onFailure { if (it is CancellationException) throw it }

    /**
     * Exports all 100% local Room data (store profile, categories, products, transactions, expenses)
     * as a formatted JSON backup file and updates local lastBackupMillis.
     */
    suspend fun exportBackupJson(): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        val settings = dao.getSettings() ?: StoreSettingsEntity(createdAtMillis = now, updatedAtMillis = now)
        val categories = dao.getAllCategories()
        val products = dao.getAllProducts()
        val transactions = dao.getAllTransactions()
        val expenses = dao.getAllExpenses()

        val root = JSONObject().apply {
            put("app", "ADA KASIR")
            put("developer", "Rafline Dev")
            put("version", 2)
            put("storageMode", "offline_local")
            put("exportedAtMillis", now)
            put("settings", JSONObject().apply {
                put("storeName", settings.storeName)
                put("ownerName", settings.ownerName)
                put("phone", settings.phone)
                put("address", settings.address)
                put("photoData", settings.photoData)
                put("storeIcon", settings.storeIcon)
                put("storeIconBgColor", settings.storeIconBgColor)
                put("receiptPrefix", settings.receiptPrefix)
                put("darkTheme", settings.darkTheme)
                put("language", settings.language)
                put("defaultTaxPercent", settings.defaultTaxPercent)
                put("showReceiptLogo", settings.showReceiptLogo)
                put("showReceiptCashierName", settings.showReceiptCashierName)
                put("showReceiptStorePhone", settings.showReceiptStorePhone)
                put("showReceiptStoreAddress", settings.showReceiptStoreAddress)
                put("showReceiptCustomerName", settings.showReceiptCustomerName)
                put("receiptFooterMessage", settings.receiptFooterMessage)
            })
            put("categories", JSONArray().apply {
                categories.forEach { cat ->
                    put(JSONObject().apply {
                        put("id", cat.id)
                        put("name", cat.name)
                        put("isDefault", cat.isDefault)
                    })
                }
            })
            put("products", JSONArray().apply {
                products.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("sku", p.sku)
                        put("category", p.category)
                        put("unit", p.unit)
                        put("costPrice", p.costPrice)
                        put("sellPrice", p.sellPrice)
                        put("stock", p.stock)
                        put("minStock", p.minStock)
                        put("description", p.description)
                        put("photoData", p.photoData)
                    })
                }
            })
            put("transactions", JSONArray().apply {
                transactions.forEach { t ->
                    val items = deserializeTransactionItems(t.itemsJson)
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("transactionNumber", t.transactionNumber)
                        put("customerName", t.customerName)
                        put("cashierName", t.cashierName)
                        put("subtotal", t.subtotal)
                        put("discountType", t.discountType)
                        put("discountValue", t.discountValue)
                        put("discountAmount", t.discountAmount)
                        put("taxType", t.taxType)
                        put("taxPercent", t.taxPercent)
                        put("taxAmount", t.taxAmount)
                        put("totalAmount", t.totalAmount)
                        put("paymentMethod", t.paymentMethod)
                        put("amountPaid", t.amountPaid)
                        put("changeAmount", t.changeAmount)
                        put("status", t.status)
                        put("statusReason", t.statusReason)
                        put("stockDeducted", t.stockDeducted)
                        put("stockRestored", t.stockRestored)
                        put("transactionMillis", t.transactionMillis)
                        put("items", JSONArray().apply {
                            items.forEach { item ->
                                put(JSONObject().apply {
                                    put("productId", item.productId)
                                    put("productName", item.productName)
                                    put("unit", item.unit)
                                    put("unitPrice", item.unitPrice)
                                    put("costPrice", item.costPrice)
                                    put("quantity", item.quantity)
                                    put("subtotal", item.subtotal)
                                })
                            }
                        })
                    })
                }
            })
            put("expenses", JSONArray().apply {
                expenses.forEach { exp ->
                    put(JSONObject().apply {
                        put("id", exp.id)
                        put("title", exp.title)
                        put("category", exp.category)
                        put("amount", exp.amount)
                        put("notes", exp.notes)
                        put("expenseMillis", exp.expenseMillis)
                    })
                }
            })
        }

        dao.upsertSettings(settings.copy(lastBackupMillis = now, updatedAtMillis = now))
        root.toString(2)
    }.onFailure { if (it is CancellationException) throw it }

    suspend fun recordBackupCompleted(timestampMillis: Long = System.currentTimeMillis()): Result<Unit> = runCatching {
        val current = dao.getSettings() ?: StoreSettingsEntity(createdAtMillis = timestampMillis)
        dao.upsertSettings(current.copy(lastBackupMillis = timestampMillis, updatedAtMillis = timestampMillis))
    }.onFailure { if (it is CancellationException) throw it }

    fun validateBackupJson(jsonString: String): Result<BackupSummary> = runCatching {
        val root = JSONObject(jsonString)
        require(root.optString("app") == "ADA KASIR") { "Invalid ADA KASIR backup file" }
        val settingsObj = root.getJSONObject("settings")
        val categoriesArr = root.getJSONArray("categories")
        val productsArr = root.getJSONArray("products")
        val transactionsArr = root.getJSONArray("transactions")
        val expensesArr = root.optJSONArray("expenses")
        BackupSummary(
            storeName = settingsObj.optString("storeName", "Toko ADA KASIR"),
            categoryCount = categoriesArr.length(),
            productCount = productsArr.length(),
            transactionCount = transactionsArr.length(),
            expenseCount = expensesArr?.length() ?: 0,
            exportedAtMillis = root.optLong("exportedAtMillis", System.currentTimeMillis())
        )
    }

    /**
     * Atomically restores all operational data from a validated JSON backup file into local Room DB.
     */
    suspend fun restoreBackupJson(jsonString: String): Result<BackupSummary> = runCatching {
        val summary = validateBackupJson(jsonString).getOrThrow()
        val root = JSONObject(jsonString)
        val settingsObj = root.getJSONObject("settings")
        val categoriesArr = root.getJSONArray("categories")
        val productsArr = root.getJSONArray("products")
        val transactionsArr = root.getJSONArray("transactions")
        val expensesArr = root.optJSONArray("expenses")
        val now = System.currentTimeMillis()

        database.withTransaction {
            val existingSettings = dao.getSettings()
            dao.upsertSettings(
                StoreSettingsEntity(
                    id = "app",
                    storeName = settingsObj.optString("storeName", "Toko ADA KASIR").trim().ifBlank { "Toko ADA KASIR" }.take(80),
                    ownerName = settingsObj.optString("ownerName", "").take(80),
                    phone = settingsObj.optString("phone", "").take(30),
                    address = settingsObj.optString("address", "").take(250),
                    photoData = settingsObj.optString("photoData", "").take(250000),
                    storeIcon = settingsObj.optString("storeIcon", "storefront").ifBlank { "storefront" }.take(32),
                    storeIconBgColor = settingsObj.optString("storeIconBgColor", "#2563EB")
                        .takeIf { it.startsWith("#") && (it.length == 7 || it.length == 9) } ?: "#2563EB",
                    receiptPrefix = PosFormatters.sanitizeReceiptPrefix(settingsObj.optString("receiptPrefix", "AK")),
                    darkTheme = settingsObj.optBoolean("darkTheme", false),
                    language = if (settingsObj.optString("language") == "en") "en" else "id",
                    defaultTaxPercent = settingsObj.optDouble("defaultTaxPercent", 11.0).coerceIn(0.0, 100.0),
                    showReceiptLogo = settingsObj.optBoolean("showReceiptLogo", true),
                    showReceiptCashierName = settingsObj.optBoolean("showReceiptCashierName", true),
                    showReceiptStorePhone = settingsObj.optBoolean("showReceiptStorePhone", true),
                    showReceiptStoreAddress = settingsObj.optBoolean("showReceiptStoreAddress", true),
                    showReceiptCustomerName = settingsObj.optBoolean("showReceiptCustomerName", true),
                    receiptFooterMessage = settingsObj.optString("receiptFooterMessage", "").take(200),
                    lastBackupMillis = existingSettings?.lastBackupMillis,
                    createdAtMillis = existingSettings?.createdAtMillis ?: now,
                    updatedAtMillis = now
                )
            )

            val catEntities = mutableListOf<CategoryEntity>()
            for (i in 0 until categoriesArr.length()) {
                val obj = categoriesArr.getJSONObject(i)
                val id = obj.optString("id").takeIf { it.isNotBlank() }
                    ?: ("cat_" + UUID.randomUUID().toString().replace("-", "").take(12))
                val name = obj.optString("name").trim().take(60)
                if (name.isBlank()) continue
                catEntities += CategoryEntity(
                    id = id,
                    name = name,
                    isDefault = obj.optBoolean("isDefault", false),
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            }
            if (catEntities.isNotEmpty()) dao.upsertCategories(catEntities)

            val prodEntities = mutableListOf<ProductEntity>()
            for (i in 0 until productsArr.length()) {
                val obj = productsArr.getJSONObject(i)
                val id = obj.optString("id").takeIf { it.isNotBlank() }
                    ?: ("prod_" + UUID.randomUUID().toString().replace("-", "").take(16))
                val name = obj.optString("name").trim().take(120)
                if (name.isBlank()) continue
                prodEntities += ProductEntity(
                    id = id,
                    name = name,
                    sku = obj.optString("sku", "").take(60),
                    category = obj.optString("category", "Lainnya").trim().ifBlank { "Lainnya" }.take(60),
                    unit = ProductUnit.fromLabel(obj.optString("unit", "Pcs")).label,
                    costPrice = obj.optDouble("costPrice", 0.0).coerceAtLeast(0.0),
                    sellPrice = obj.optDouble("sellPrice", 0.0).coerceAtLeast(0.0),
                    stock = PosCalculator.roundQuantity(obj.optDouble("stock", 0.0).coerceAtLeast(0.0)),
                    minStock = PosCalculator.roundQuantity(obj.optDouble("minStock", 0.0).coerceAtLeast(0.0)),
                    description = obj.optString("description", "").take(500),
                    photoData = obj.optString("photoData", "").take(250000),
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            }
            if (prodEntities.isNotEmpty()) dao.upsertProducts(prodEntities)

            val txEntities = mutableListOf<TransactionEntity>()
            for (i in 0 until transactionsArr.length()) {
                val obj = transactionsArr.getJSONObject(i)
                val id = obj.optString("id").takeIf { it.isNotBlank() }
                    ?: ("tx_" + UUID.randomUUID().toString().replace("-", "").take(16))
                val itemsArr = obj.optJSONArray("items") ?: continue
                if (itemsArr.length() == 0) continue
                val items = mutableListOf<TransactionItem>()
                for (j in 0 until minOf(itemsArr.length(), 100)) {
                    val itObj = itemsArr.getJSONObject(j)
                    val qty = itObj.optDouble("quantity", 0.0)
                    val pId = itObj.optString("productId", "")
                    val pName = itObj.optString("productName", "")
                    if (pId.isBlank() || pName.isBlank() || qty <= 0.0) continue
                    items += TransactionItem(
                        productId = pId,
                        productName = pName.take(120),
                        unit = itObj.optString("unit", "Pcs"),
                        unitPrice = itObj.optDouble("unitPrice", 0.0).coerceAtLeast(0.0),
                        costPrice = itObj.optDouble("costPrice", 0.0).coerceAtLeast(0.0),
                        quantity = PosCalculator.roundQuantity(qty),
                        subtotal = itObj.optDouble("subtotal", 0.0).coerceAtLeast(0.0)
                    )
                }
                if (items.isEmpty()) continue
                txEntities += TransactionEntity(
                    id = id,
                    transactionNumber = obj.optString("transactionNumber", id).take(60),
                    customerName = obj.optString("customerName", "").take(100),
                    cashierName = obj.optString("cashierName", "").take(80),
                    itemsJson = serializeTransactionItems(items),
                    subtotal = obj.optDouble("subtotal", 0.0).coerceAtLeast(0.0),
                    discountType = obj.optString("discountType", "NONE"),
                    discountValue = obj.optDouble("discountValue", 0.0).coerceAtLeast(0.0),
                    discountAmount = obj.optDouble("discountAmount", 0.0).coerceAtLeast(0.0),
                    taxType = obj.optString("taxType", "NONE"),
                    taxPercent = obj.optDouble("taxPercent", 0.0).coerceIn(0.0, 100.0),
                    taxAmount = obj.optDouble("taxAmount", 0.0).coerceAtLeast(0.0),
                    totalAmount = obj.optDouble("totalAmount", 0.0).coerceAtLeast(0.0),
                    paymentMethod = obj.optString("paymentMethod", "CASH"),
                    amountPaid = obj.optDouble("amountPaid", 0.0).coerceAtLeast(0.0),
                    changeAmount = obj.optDouble("changeAmount", 0.0).coerceAtLeast(0.0),
                    status = obj.optString("status", "COMPLETED"),
                    statusReason = obj.optString("statusReason", "").take(250),
                    stockDeducted = obj.optBoolean("stockDeducted", true),
                    stockRestored = obj.optBoolean("stockRestored", false),
                    transactionMillis = obj.optLong("transactionMillis", now).coerceAtLeast(1L),
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            }
            if (txEntities.isNotEmpty()) dao.upsertTransactions(txEntities)

            if (expensesArr != null) {
                val expEntities = mutableListOf<ExpenseEntity>()
                for (i in 0 until expensesArr.length()) {
                    val obj = expensesArr.getJSONObject(i)
                    val id = obj.optString("id").takeIf { it.isNotBlank() }
                        ?: ("exp_" + UUID.randomUUID().toString().replace("-", "").take(16))
                    val title = obj.optString("title").trim().take(120)
                    if (title.isBlank()) continue
                    expEntities += ExpenseEntity(
                        id = id,
                        title = title,
                        category = obj.optString("category", ExpenseCategory.OPERATIONAL.labelId)
                            .trim().ifBlank { ExpenseCategory.OPERATIONAL.labelId }.take(60),
                        amount = PosCalculator.roundCurrency(obj.optDouble("amount", 0.0).coerceAtLeast(0.0)),
                        notes = obj.optString("notes", "").take(400),
                        expenseMillis = obj.optLong("expenseMillis", now).coerceAtLeast(1L),
                        createdAtMillis = now,
                        updatedAtMillis = now
                    )
                }
                if (expEntities.isNotEmpty()) dao.upsertExpenses(expEntities)
            }
        }

        summary
    }.onFailure { if (it is CancellationException) throw it }

    companion object {
        const val LOCAL_USER_ID = "local_device"
    }
}
