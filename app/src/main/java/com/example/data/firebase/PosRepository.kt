package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.PosCalculator
import com.example.data.model.PosTransaction
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ProductUnit
import com.example.data.model.StoreDirectoryEntry
import com.example.data.model.StoreSettings
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class BackupSummary(
    val storeName: String,
    val categoryCount: Int,
    val productCount: Int,
    val transactionCount: Int,
    val exportedAtMillis: Long
)

class PosRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun userDoc(userId: String) = db.collection("users").document(userId)
    private fun settingsDoc(userId: String) = userDoc(userId).collection("settings").document("app")
    private fun categoriesCol(userId: String) = userDoc(userId).collection("categories")
    private fun productsCol(userId: String) = userDoc(userId).collection("products")
    private fun transactionsCol(userId: String) = userDoc(userId).collection("transactions")
    private fun expensesCol(userId: String) = userDoc(userId).collection("expenses")
    private fun storeDirectoryDoc(userId: String) = db.collection("storeDirectory").document(userId)

    private val firebaseAuth: FirebaseAuth
        get() = FirebaseAuth.getInstance(db.app)

    private fun requireActiveUid(expectedUserId: String): String {
        val currentUid = firebaseAuth.currentUser?.uid
        if (currentUid != null && currentUid != expectedUserId) {
            throw IllegalStateException("Authenticated user mismatch")
        }
        return expectedUserId
    }

    fun observeSettings(userId: String): Flow<StoreSettings> =
        settingsDoc(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        StoreSettings::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: StoreSettings(userId = userId)
                } else {
                    StoreSettings(userId = userId)
                }
            }
            .catch { error ->
                logIfNotCancellation(error, OperationType.GET, settingsDoc(userId).path, userId)
                throw error
            }

    fun observeCategories(userId: String): Flow<List<ProductCategory>> =
        categoriesCol(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ProductCategory::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(compareByDescending<ProductCategory> { it.isDefault }.thenBy { it.name.lowercase() })
            }
            .catch { error ->
                logIfNotCancellation(error, OperationType.LIST, categoriesCol(userId).path, userId)
                throw error
            }

    fun observeProducts(userId: String): Flow<List<Product>> =
        productsCol(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Product::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.name.lowercase() }
            }
            .catch { error ->
                logIfNotCancellation(error, OperationType.LIST, productsCol(userId).path, userId)
                throw error
            }

    fun observeTransactions(userId: String): Flow<List<PosTransaction>> =
        transactionsCol(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PosTransaction::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.transactionMillis }
            }
            .catch { error ->
                logIfNotCancellation(error, OperationType.LIST, transactionsCol(userId).path, userId)
                throw error
            }

    fun observeExpenses(userId: String): Flow<List<Expense>> =
        expensesCol(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Expense::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.expenseMillis }
            }
            .catch { error ->
                logIfNotCancellation(error, OperationType.LIST, expensesCol(userId).path, userId)
                throw error
            }

    fun observeAuthorizedStoresForMonitor(monitorEmail: String): Flow<List<StoreDirectoryEntry>> {
        val cleanEmail = monitorEmail.trim().lowercase()
        if (cleanEmail.isBlank()) return flowOf(emptyList())
        return db.collection("storeDirectory")
            .whereArrayContains("monitorEmails", cleanEmail)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    StoreDirectoryEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.storeName.lowercase() }
            }
            .catch { error ->
                logIfNotCancellation(error, OperationType.LIST, "storeDirectory", monitorEmail)
                emit(emptyList())
            }
    }

    private suspend fun syncStoreDirectoryEntry(
        uid: String,
        ownerEmail: String,
        storeName: String,
        monitorEmails: List<String>
    ) {
        val cleanEmails = monitorEmails
            .map { it.trim().lowercase() }
            .filter { it.contains("@") }
            .distinct()
            .take(10)
        val payload = mapOf(
            "userId" to uid,
            "ownerEmail" to ownerEmail.trim().lowercase().take(120),
            "storeName" to storeName.trim().ifBlank { "Toko ADA KASIR" }.take(80),
            "monitorEmails" to cleanEmails,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        storeDirectoryDoc(uid).set(payload).await()
    }

    suspend fun ensureInitialUserSetup(
        userId: String,
        ownerDisplayName: String,
        ownerEmail: String = "",
        defaultDarkTheme: Boolean,
        defaultLanguage: String
    ) {
        val uid = requireActiveUid(userId)
        val settingsRef = settingsDoc(uid)
        val cleanEmail = ownerEmail.trim().lowercase().take(120)
        try {
            val existingSettings = settingsRef.get().await()
            if (!existingSettings.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "storeName" to "Toko ADA KASIR",
                    "ownerName" to ownerDisplayName.take(80),
                    "phone" to "",
                    "address" to "",
                    "photoData" to "",
                    "darkTheme" to defaultDarkTheme,
                    "language" to if (defaultLanguage == "en") "en" else "id",
                    "defaultTaxPercent" to 11.0,
                    "ownerEmail" to cleanEmail,
                    "monitorEmails" to emptyList<String>(),
                    "showReceiptLogo" to true,
                    "showReceiptCashierName" to true,
                    "showReceiptStorePhone" to true,
                    "showReceiptStoreAddress" to true,
                    "showReceiptCustomerName" to true,
                    "receiptFooterMessage" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                settingsRef.set(payload).await()
                syncStoreDirectoryEntry(uid, cleanEmail, "Toko ADA KASIR", emptyList())
            } else {
                val existingStoreName = existingSettings.getString("storeName") ?: "Toko ADA KASIR"
                @Suppress("UNCHECKED_CAST")
                val existingMonitors = (existingSettings.get("monitorEmails") as? List<String>).orEmpty()
                val existingOwnerEmail = existingSettings.getString("ownerEmail") ?: ""
                if (cleanEmail.isNotBlank() && existingOwnerEmail.isBlank()) {
                    settingsRef.update(
                        mapOf(
                            "ownerEmail" to cleanEmail,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                val dirSnap = storeDirectoryDoc(uid).get().await()
                if (!dirSnap.exists()) {
                    syncStoreDirectoryEntry(
                        uid = uid,
                        ownerEmail = existingOwnerEmail.ifBlank { cleanEmail },
                        storeName = existingStoreName,
                        monitorEmails = existingMonitors
                    )
                }
            }

            val existingCategories = categoriesCol(uid).limit(1).get().await()
            if (existingCategories.isEmpty) {
                val defaults = listOf(
                    "cat_makanan" to "Makanan",
                    "cat_minuman" to "Minuman",
                    "cat_sembako" to "Sembako",
                    "cat_sayuran" to "Sayuran",
                    "cat_buah" to "Buah-buahan",
                    "cat_lainnya" to "Lainnya"
                )
                val batch = db.batch()
                defaults.forEach { (id, name) ->
                    val ref = categoriesCol(uid).document(id)
                    batch.set(
                        ref,
                        mapOf(
                            "userId" to uid,
                            "name" to name,
                            "isDefault" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, settingsRef.path)
        }
    }

    suspend fun saveStoreProfile(
        userId: String,
        storeName: String,
        ownerName: String,
        phone: String,
        address: String,
        photoData: String,
        defaultTaxPercent: Double,
        ownerEmail: String = ""
    ): Result<Unit> = runCatching {
        val uid = requireActiveUid(userId)
        val ref = settingsDoc(uid)
        val cleanName = storeName.trim().ifBlank { "Toko ADA KASIR" }.take(80)
        val snapshot = ref.get().await()
        if (snapshot.exists()) {
            ref.update(
                mapOf(
                    "storeName" to cleanName,
                    "ownerName" to ownerName.trim().take(80),
                    "phone" to phone.trim().take(30),
                    "address" to address.trim().take(250),
                    "photoData" to photoData.take(250000),
                    "defaultTaxPercent" to defaultTaxPercent.coerceIn(0.0, 100.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            @Suppress("UNCHECKED_CAST")
            val existingMonitors = (snapshot.get("monitorEmails") as? List<String>).orEmpty()
            val resolvedEmail = (snapshot.getString("ownerEmail") ?: "").ifBlank { ownerEmail }
            syncStoreDirectoryEntry(uid, resolvedEmail, cleanName, existingMonitors)
        } else {
            val cleanEmail = ownerEmail.trim().lowercase().take(120)
            ref.set(
                mapOf(
                    "userId" to uid,
                    "storeName" to cleanName,
                    "ownerName" to ownerName.trim().take(80),
                    "phone" to phone.trim().take(30),
                    "address" to address.trim().take(250),
                    "photoData" to photoData.take(250000),
                    "darkTheme" to false,
                    "language" to "id",
                    "defaultTaxPercent" to defaultTaxPercent.coerceIn(0.0, 100.0),
                    "ownerEmail" to cleanEmail,
                    "monitorEmails" to emptyList<String>(),
                    "showReceiptLogo" to true,
                    "showReceiptCashierName" to true,
                    "showReceiptStorePhone" to true,
                    "showReceiptStoreAddress" to true,
                    "showReceiptCustomerName" to true,
                    "receiptFooterMessage" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            syncStoreDirectoryEntry(uid, cleanEmail, cleanName, emptyList())
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, settingsDoc(userId).path)
    }

    suspend fun saveReceiptCustomization(
        userId: String,
        showLogo: Boolean,
        showCashierName: Boolean,
        showStorePhone: Boolean,
        showStoreAddress: Boolean,
        showCustomerName: Boolean,
        footerMessage: String
    ): Result<Unit> = runCatching {
        val uid = requireActiveUid(userId)
        val ref = settingsDoc(uid)
        val snapshot = ref.get().await()
        val fields = mapOf(
            "showReceiptLogo" to showLogo,
            "showReceiptCashierName" to showCashierName,
            "showReceiptStorePhone" to showStorePhone,
            "showReceiptStoreAddress" to showStoreAddress,
            "showReceiptCustomerName" to showCustomerName,
            "receiptFooterMessage" to footerMessage.trim().take(200),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (snapshot.exists()) {
            ref.update(fields).await()
        } else {
            ref.set(
                fields + mapOf(
                    "userId" to uid,
                    "storeName" to "Toko ADA KASIR",
                    "ownerName" to "",
                    "phone" to "",
                    "address" to "",
                    "photoData" to "",
                    "darkTheme" to false,
                    "language" to "id",
                    "defaultTaxPercent" to 11.0,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, settingsDoc(userId).path)
    }

    suspend fun saveMonitorEmails(
        userId: String,
        ownerEmail: String,
        storeName: String,
        emails: List<String>
    ): Result<Unit> = runCatching {
        val uid = requireActiveUid(userId)
        val ref = settingsDoc(uid)
        val cleanEmails = emails
            .map { it.trim().lowercase() }
            .filter { it.contains("@") && it.length <= 120 }
            .distinct()
            .take(10)
        val cleanOwnerEmail = ownerEmail.trim().lowercase().take(120)
        val cleanStoreName = storeName.trim().ifBlank { "Toko ADA KASIR" }.take(80)

        val snapshot = ref.get().await()
        if (snapshot.exists()) {
            ref.update(
                mapOf(
                    "ownerEmail" to cleanOwnerEmail,
                    "monitorEmails" to cleanEmails,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            ref.set(
                mapOf(
                    "userId" to uid,
                    "storeName" to cleanStoreName,
                    "ownerName" to "",
                    "phone" to "",
                    "address" to "",
                    "photoData" to "",
                    "darkTheme" to false,
                    "language" to "id",
                    "defaultTaxPercent" to 11.0,
                    "ownerEmail" to cleanOwnerEmail,
                    "monitorEmails" to cleanEmails,
                    "showReceiptLogo" to true,
                    "showReceiptCashierName" to true,
                    "showReceiptStorePhone" to true,
                    "showReceiptStoreAddress" to true,
                    "showReceiptCustomerName" to true,
                    "receiptFooterMessage" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        syncStoreDirectoryEntry(uid, cleanOwnerEmail, cleanStoreName, cleanEmails)
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, settingsDoc(userId).path)
    }

    suspend fun updateTheme(userId: String, darkTheme: Boolean): Result<Unit> = runCatching {
        val uid = requireActiveUid(userId)
        val ref = settingsDoc(uid)
        val snapshot = ref.get().await()
        if (snapshot.exists()) {
            ref.update(
                mapOf(
                    "darkTheme" to darkTheme,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            ref.set(
                mapOf(
                    "userId" to uid,
                    "storeName" to "Toko ADA KASIR",
                    "ownerName" to "",
                    "phone" to "",
                    "address" to "",
                    "photoData" to "",
                    "darkTheme" to darkTheme,
                    "language" to "id",
                    "defaultTaxPercent" to 11.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, settingsDoc(userId).path)
    }

    suspend fun updateLanguage(userId: String, language: String): Result<Unit> = runCatching {
        val uid = requireActiveUid(userId)
        val cleanLang = if (language == "en") "en" else "id"
        val ref = settingsDoc(uid)
        val snapshot = ref.get().await()
        if (snapshot.exists()) {
            ref.update(
                mapOf(
                    "language" to cleanLang,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            ref.set(
                mapOf(
                    "userId" to uid,
                    "storeName" to "Toko ADA KASIR",
                    "ownerName" to "",
                    "phone" to "",
                    "address" to "",
                    "photoData" to "",
                    "darkTheme" to false,
                    "language" to cleanLang,
                    "defaultTaxPercent" to 11.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, settingsDoc(userId).path)
    }

    suspend fun addCategory(userId: String, name: String): Result<String> = runCatching {
        val uid = requireActiveUid(userId)
        val cleanName = name.trim().take(60)
        require(cleanName.isNotBlank()) { "Category name cannot be empty" }
        val docId = "cat_" + UUID.randomUUID().toString().replace("-", "").take(12)
        val ref = categoriesCol(uid).document(docId)
        ref.set(
            mapOf(
                "userId" to uid,
                "name" to cleanName,
                "isDefault" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, categoriesCol(userId).path)
    }

    suspend fun updateCategory(userId: String, categoryId: String, newName: String): Result<Unit> = runCatching {
        val uid = requireActiveUid(userId)
        val cleanName = newName.trim().take(60)
        require(categoryId.isNotBlank() && cleanName.isNotBlank())
        val ref = categoriesCol(uid).document(categoryId)
        ref.update(
            mapOf(
                "name" to cleanName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, categoriesCol(userId).document(categoryId).path)
    }

    suspend fun deleteCategory(userId: String, categoryId: String): Result<Unit> = runCatching {
        val uid = requireActiveUid(userId)
        val ref = categoriesCol(uid).document(categoryId)
        ref.delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, categoriesCol(userId).document(categoryId).path)
    }

    suspend fun saveProduct(userId: String, product: Product): Result<String> = runCatching {
        val uid = requireActiveUid(userId)
        val cleanName = product.name.trim().take(120)
        require(cleanName.isNotBlank()) { "Product name is required" }
        val validUnit = ProductUnit.fromLabel(product.unit).label
        val cleanCategory = product.category.trim().ifBlank { "Lainnya" }.take(60)

        if (product.id.isBlank()) {
            val newId = "prod_" + UUID.randomUUID().toString().replace("-", "").take(16)
            val ref = productsCol(uid).document(newId)
            ref.set(
                mapOf(
                    "userId" to uid,
                    "name" to cleanName,
                    "sku" to product.sku.trim().take(60),
                    "category" to cleanCategory,
                    "unit" to validUnit,
                    "costPrice" to product.costPrice.coerceAtLeast(0.0),
                    "sellPrice" to product.sellPrice.coerceAtLeast(0.0),
                    "stock" to PosCalculator.roundQuantity(product.stock.coerceAtLeast(0.0)),
                    "minStock" to PosCalculator.roundQuantity(product.minStock.coerceAtLeast(0.0)),
                    "description" to product.description.trim().take(500),
                    "photoData" to product.photoData.take(250000),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            newId
        } else {
            val ref = productsCol(uid).document(product.id)
            ref.update(
                mapOf(
                    "name" to cleanName,
                    "sku" to product.sku.trim().take(60),
                    "category" to cleanCategory,
                    "unit" to validUnit,
                    "costPrice" to product.costPrice.coerceAtLeast(0.0),
                    "sellPrice" to product.sellPrice.coerceAtLeast(0.0),
                    "stock" to PosCalculator.roundQuantity(product.stock.coerceAtLeast(0.0)),
                    "minStock" to PosCalculator.roundQuantity(product.minStock.coerceAtLeast(0.0)),
                    "description" to product.description.trim().take(500),
                    "photoData" to product.photoData.take(250000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            product.id
        }
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, productsCol(userId).path)
    }

    suspend fun deleteProduct(userId: String, productId: String): Result<Unit> = runCatching {
        val uid = requireActiveUid(userId)
        productsCol(uid).document(productId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, productsCol(userId).document(productId).path)
    }

    suspend fun saveExpense(userId: String, expense: Expense): Result<String> = runCatching {
        val uid = requireActiveUid(userId)
        val cleanTitle = expense.title.trim().take(120)
        require(cleanTitle.isNotBlank()) { "Expense title is required" }
        val cleanCategory = expense.category.trim().ifBlank { ExpenseCategory.OPERATIONAL.labelId }.take(60)
        val safeMillis = if (expense.expenseMillis > 0L) expense.expenseMillis else System.currentTimeMillis()
        val safeAmount = PosCalculator.roundCurrency(expense.amount.coerceAtLeast(0.0))

        if (expense.id.isBlank()) {
            val newId = "exp_" + UUID.randomUUID().toString().replace("-", "").take(16)
            val ref = expensesCol(uid).document(newId)
            ref.set(
                mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "category" to cleanCategory,
                    "amount" to safeAmount,
                    "notes" to expense.notes.trim().take(400),
                    "expenseMillis" to safeMillis,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            newId
        } else {
            val ref = expensesCol(uid).document(expense.id)
            ref.update(
                mapOf(
                    "title" to cleanTitle,
                    "category" to cleanCategory,
                    "amount" to safeAmount,
                    "notes" to expense.notes.trim().take(400),
                    "expenseMillis" to safeMillis,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            expense.id
        }
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, expensesCol(userId).path)
    }

    suspend fun deleteExpense(userId: String, expenseId: String): Result<Unit> = runCatching {
        val uid = requireActiveUid(userId)
        expensesCol(uid).document(expenseId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, expensesCol(userId).document(expenseId).path)
    }

    /**
     * Atomically records a transaction and deducts product stock using Firestore Transaction.
     * Uses deterministic/idempotent transactionId so duplicate submissions cannot double-record
     * or double-deduct stock.
     */
    suspend fun completeTransaction(
        userId: String,
        transaction: PosTransaction
    ): Result<PosTransaction> = runCatching {
        val uid = requireActiveUid(userId)
        require(transaction.items.isNotEmpty()) { "Cart cannot be empty" }
        val txId = transaction.id.ifBlank {
            "tx_" + UUID.randomUUID().toString().replace("-", "").take(16)
        }
        val txRef = transactionsCol(uid).document(txId)
        val nowMillis = if (transaction.transactionMillis > 0L) transaction.transactionMillis else System.currentTimeMillis()

        db.runTransaction { firestoreTx ->
            val existingTx = firestoreTx.get(txRef)
            if (existingTx.exists()) {
                // Idempotent return: already committed, never deduct stock twice
                return@runTransaction existingTx.toObject(
                    PosTransaction::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: transaction.copy(id = txId, userId = uid)
            }

            // Read all product snapshots first (Firestore transaction invariant)
            val productSnapshots = transaction.items.associate { item ->
                val prodRef = productsCol(uid).document(item.productId)
                item.productId to firestoreTx.get(prodRef)
            }

            // Validate & compute new stock
            val stockUpdates = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Double>>()
            for (item in transaction.items) {
                val snap = productSnapshots[item.productId]
                    ?: throw IllegalStateException("Product ${item.productName} not found")
                if (!snap.exists()) {
                    throw IllegalStateException("Product ${item.productName} no longer exists")
                }
                val currentStock = snap.getDouble("stock") ?: 0.0
                if (currentStock + 0.0001 < item.quantity) {
                    throw IllegalStateException("Insufficient stock for ${item.productName}")
                }
                val newStock = PosCalculator.roundQuantity((currentStock - item.quantity).coerceAtLeast(0.0))
                stockUpdates += snap.reference to newStock
            }

            // Perform writes
            stockUpdates.forEach { (prodRef, newStock) ->
                firestoreTx.update(
                    prodRef,
                    mapOf(
                        "stock" to newStock,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val itemsPayload = transaction.items.map { item ->
                mapOf(
                    "productId" to item.productId,
                    "productName" to item.productName.take(120),
                    "unit" to item.unit,
                    "unitPrice" to item.unitPrice,
                    "costPrice" to item.costPrice,
                    "quantity" to PosCalculator.roundQuantity(item.quantity),
                    "subtotal" to item.subtotal
                )
            }

            val txPayload = mapOf(
                "userId" to uid,
                "transactionNumber" to transaction.transactionNumber.take(60),
                "customerName" to transaction.customerName.trim().take(100),
                "cashierName" to transaction.cashierName.trim().take(80),
                "items" to itemsPayload,
                "subtotal" to transaction.subtotal,
                "discountType" to transaction.discountType,
                "discountValue" to transaction.discountValue,
                "discountAmount" to transaction.discountAmount,
                "taxType" to transaction.taxType,
                "taxPercent" to transaction.taxPercent,
                "taxAmount" to transaction.taxAmount,
                "totalAmount" to transaction.totalAmount,
                "paymentMethod" to transaction.paymentMethod,
                "amountPaid" to transaction.amountPaid,
                "changeAmount" to transaction.changeAmount,
                "status" to TransactionStatus.COMPLETED.code,
                "statusReason" to "",
                "stockDeducted" to true,
                "stockRestored" to false,
                "transactionMillis" to nowMillis,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestoreTx.set(txRef, txPayload)

            transaction.copy(
                id = txId,
                userId = uid,
                status = TransactionStatus.COMPLETED.code,
                stockDeducted = true,
                stockRestored = false,
                transactionMillis = nowMillis
            )
        }.await()
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, transactionsCol(userId).path)
    }

    /**
     * Atomically cancels or refunds a COMPLETED transaction and restores product stock exactly once.
     */
    suspend fun cancelOrRefundTransaction(
        userId: String,
        transactionId: String,
        targetStatus: TransactionStatus,
        reason: String
    ): Result<Unit> = runCatching {
        val uid = requireActiveUid(userId)
        require(targetStatus == TransactionStatus.CANCELLED || targetStatus == TransactionStatus.REFUNDED)
        val txRef = transactionsCol(uid).document(transactionId)

        db.runTransaction { firestoreTx ->
            val txSnap = firestoreTx.get(txRef)
            if (!txSnap.exists()) {
                throw IllegalStateException("Transaction not found")
            }
            val currentStatus = txSnap.getString("status") ?: TransactionStatus.COMPLETED.code
            val alreadyRestored = txSnap.getBoolean("stockRestored") ?: false
            if (currentStatus != TransactionStatus.COMPLETED.code || alreadyRestored) {
                throw IllegalStateException("Transaction already cancelled or refunded")
            }

            @Suppress("UNCHECKED_CAST")
            val rawItems = txSnap.get("items") as? List<Map<String, Any?>> ?: emptyList()
            val productSnaps = rawItems.mapNotNull { itemMap ->
                val prodId = itemMap["productId"] as? String ?: return@mapNotNull null
                val qty = (itemMap["quantity"] as? Number)?.toDouble() ?: 0.0
                if (qty <= 0.0) return@mapNotNull null
                val snap = firestoreTx.get(productsCol(uid).document(prodId))
                Triple(snap, prodId, qty)
            }

            productSnaps.forEach { (prodSnap, _, qty) ->
                if (prodSnap.exists()) {
                    val currentStock = prodSnap.getDouble("stock") ?: 0.0
                    val restoredStock = PosCalculator.roundQuantity(currentStock + qty)
                    firestoreTx.update(
                        prodSnap.reference,
                        mapOf(
                            "stock" to restoredStock,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            firestoreTx.update(
                txRef,
                mapOf(
                    "status" to targetStatus.code,
                    "statusReason" to reason.trim().take(250),
                    "stockRestored" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, transactionsCol(userId).document(transactionId).path)
    }

    suspend fun exportBackupJson(userId: String): Result<String> = runCatching {
        val uid = requireActiveUid(userId)
        val settingsSnap = settingsDoc(uid).get().await()
        val settings = settingsSnap.toObject(
            StoreSettings::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ) ?: StoreSettings(userId = uid)
        val categories = categoriesCol(uid).get().await().toObjects(
            ProductCategory::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
        val products = productsCol(uid).get().await().toObjects(
            Product::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
        val transactions = transactionsCol(uid).get().await().toObjects(
            PosTransaction::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
        val expenses = expensesCol(uid).get().await().toObjects(
            Expense::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )

        val root = JSONObject().apply {
            put("app", "ADA KASIR")
            put("developer", "Rafline Dev")
            put("version", 1)
            put("exportedAtMillis", System.currentTimeMillis())
            put("settings", JSONObject().apply {
                put("storeName", settings.storeName)
                put("ownerName", settings.ownerName)
                put("phone", settings.phone)
                put("address", settings.address)
                put("photoData", settings.photoData)
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
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("transactionNumber", t.transactionNumber)
                        put("customerName", t.customerName)
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
                            t.items.forEach { item ->
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

        // Update lastBackupAt in Firestore
        if (settingsSnap.exists()) {
            settingsDoc(uid).update(
                mapOf(
                    "lastBackupAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        root.toString(2)
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.GET, userDoc(userId).path)
    }

    fun validateBackupJson(jsonString: String): Result<BackupSummary> = runCatching {
        val root = JSONObject(jsonString)
        require(root.optString("app") == "ADA KASIR") { "Invalid backup header" }
        val settingsObj = root.getJSONObject("settings")
        val categoriesArr = root.getJSONArray("categories")
        val productsArr = root.getJSONArray("products")
        val transactionsArr = root.getJSONArray("transactions")

        val storeName = settingsObj.optString("storeName", "Toko ADA KASIR")
        require(storeName.isNotBlank()) { "Missing storeName in backup" }

        BackupSummary(
            storeName = storeName,
            categoryCount = categoriesArr.length(),
            productCount = productsArr.length(),
            transactionCount = transactionsArr.length(),
            exportedAtMillis = root.optLong("exportedAtMillis", 0L)
        )
    }

    suspend fun restoreBackupJson(userId: String, jsonString: String): Result<BackupSummary> = runCatching {
        val uid = requireActiveUid(userId)
        val summary = validateBackupJson(jsonString).getOrThrow()
        val root = JSONObject(jsonString)
        val settingsObj = root.getJSONObject("settings")
        val categoriesArr = root.getJSONArray("categories")
        val productsArr = root.getJSONArray("products")
        val transactionsArr = root.getJSONArray("transactions")

        // 1. Restore settings
        val settingsSnap = settingsDoc(uid).get().await()
        val settingsFields = mapOf(
            "storeName" to settingsObj.optString("storeName", "Toko ADA KASIR").ifBlank { "Toko ADA KASIR" }.take(80),
            "ownerName" to settingsObj.optString("ownerName", "").take(80),
            "phone" to settingsObj.optString("phone", "").take(30),
            "address" to settingsObj.optString("address", "").take(250),
            "photoData" to settingsObj.optString("photoData", "").take(250000),
            "darkTheme" to settingsObj.optBoolean("darkTheme", false),
            "language" to if (settingsObj.optString("language") == "en") "en" else "id",
            "defaultTaxPercent" to settingsObj.optDouble("defaultTaxPercent", 11.0).coerceIn(0.0, 100.0),
            "showReceiptLogo" to settingsObj.optBoolean("showReceiptLogo", true),
            "showReceiptCashierName" to settingsObj.optBoolean("showReceiptCashierName", true),
            "showReceiptStorePhone" to settingsObj.optBoolean("showReceiptStorePhone", true),
            "showReceiptStoreAddress" to settingsObj.optBoolean("showReceiptStoreAddress", true),
            "showReceiptCustomerName" to settingsObj.optBoolean("showReceiptCustomerName", true),
            "receiptFooterMessage" to settingsObj.optString("receiptFooterMessage", "").take(200),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (settingsSnap.exists()) {
            settingsDoc(uid).update(settingsFields).await()
        } else {
            settingsDoc(uid).set(
                settingsFields + mapOf(
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        // Fetch existing doc IDs so we use update() for existing and set() for new (preserves createdAt!)
        val existingCatIds = categoriesCol(uid).get().await().documents.map { it.id }.toSet()
        val existingProdIds = productsCol(uid).get().await().documents.map { it.id }.toSet()
        val existingTxIds = transactionsCol(uid).get().await().documents.map { it.id }.toSet()
        val existingExpIds = expensesCol(uid).get().await().documents.map { it.id }.toSet()

        val operations = mutableListOf<suspend () -> Unit>()

        // Collect batched writes
        val writeOps = mutableListOf<(com.google.firebase.firestore.WriteBatch) -> Unit>()

        for (i in 0 until categoriesArr.length()) {
            val obj = categoriesArr.getJSONObject(i)
            val id = obj.optString("id").takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
                ?: ("cat_" + UUID.randomUUID().toString().replace("-", "").take(12))
            val name = obj.optString("name").trim().take(60)
            if (name.isBlank()) continue
            val isDefault = obj.optBoolean("isDefault", false)
            val ref = categoriesCol(uid).document(id)
            if (id in existingCatIds) {
                writeOps += { batch ->
                    batch.update(
                        ref,
                        mapOf(
                            "name" to name,
                            "isDefault" to isDefault,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            } else {
                writeOps += { batch ->
                    batch.set(
                        ref,
                        mapOf(
                            "userId" to uid,
                            "name" to name,
                            "isDefault" to isDefault,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
        }

        for (i in 0 until productsArr.length()) {
            val obj = productsArr.getJSONObject(i)
            val id = obj.optString("id").takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
                ?: ("prod_" + UUID.randomUUID().toString().replace("-", "").take(16))
            val name = obj.optString("name").trim().take(120)
            if (name.isBlank()) continue
            val unit = ProductUnit.fromLabel(obj.optString("unit", "Pcs")).label
            val category = obj.optString("category", "Lainnya").trim().ifBlank { "Lainnya" }.take(60)
            val ref = productsCol(uid).document(id)
            val common = mapOf(
                "name" to name,
                "sku" to obj.optString("sku", "").take(60),
                "category" to category,
                "unit" to unit,
                "costPrice" to obj.optDouble("costPrice", 0.0).coerceAtLeast(0.0),
                "sellPrice" to obj.optDouble("sellPrice", 0.0).coerceAtLeast(0.0),
                "stock" to PosCalculator.roundQuantity(obj.optDouble("stock", 0.0).coerceAtLeast(0.0)),
                "minStock" to PosCalculator.roundQuantity(obj.optDouble("minStock", 0.0).coerceAtLeast(0.0)),
                "description" to obj.optString("description", "").take(500),
                "photoData" to obj.optString("photoData", "").take(250000),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (id in existingProdIds) {
                writeOps += { batch -> batch.update(ref, common) }
            } else {
                writeOps += { batch ->
                    batch.set(
                        ref,
                        common + mapOf(
                            "userId" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
        }

        for (i in 0 until transactionsArr.length()) {
            val obj = transactionsArr.getJSONObject(i)
            val id = obj.optString("id").takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
                ?: ("tx_" + UUID.randomUUID().toString().replace("-", "").take(16))
            if (id in existingTxIds) continue // Never overwrite existing immutable transaction records
            val itemsArr = obj.optJSONArray("items") ?: continue
            if (itemsArr.length() == 0) continue
            val itemsList = mutableListOf<Map<String, Any>>()
            for (j in 0 until minOf(itemsArr.length(), 100)) {
                val itObj = itemsArr.getJSONObject(j)
                val qty = itObj.optDouble("quantity", 0.0)
                val pId = itObj.optString("productId", "")
                val pName = itObj.optString("productName", "")
                if (pId.isBlank() || pName.isBlank() || qty <= 0.0) continue
                itemsList += mapOf(
                    "productId" to pId,
                    "productName" to pName.take(120),
                    "unit" to itObj.optString("unit", "Pcs"),
                    "unitPrice" to itObj.optDouble("unitPrice", 0.0).coerceAtLeast(0.0),
                    "costPrice" to itObj.optDouble("costPrice", 0.0).coerceAtLeast(0.0),
                    "quantity" to PosCalculator.roundQuantity(qty),
                    "subtotal" to itObj.optDouble("subtotal", 0.0).coerceAtLeast(0.0)
                )
            }
            if (itemsList.isEmpty()) continue
            val ref = transactionsCol(uid).document(id)
            val txPayload = mapOf(
                "userId" to uid,
                "transactionNumber" to obj.optString("transactionNumber", id).take(60),
                "customerName" to obj.optString("customerName", "").take(100),
                "items" to itemsList,
                "subtotal" to obj.optDouble("subtotal", 0.0).coerceAtLeast(0.0),
                "discountType" to obj.optString("discountType", "NONE"),
                "discountValue" to obj.optDouble("discountValue", 0.0).coerceAtLeast(0.0),
                "discountAmount" to obj.optDouble("discountAmount", 0.0).coerceAtLeast(0.0),
                "taxType" to obj.optString("taxType", "NONE"),
                "taxPercent" to obj.optDouble("taxPercent", 0.0).coerceIn(0.0, 100.0),
                "taxAmount" to obj.optDouble("taxAmount", 0.0).coerceAtLeast(0.0),
                "totalAmount" to obj.optDouble("totalAmount", 0.0).coerceAtLeast(0.0),
                "paymentMethod" to obj.optString("paymentMethod", "CASH"),
                "amountPaid" to obj.optDouble("amountPaid", 0.0).coerceAtLeast(0.0),
                "changeAmount" to obj.optDouble("changeAmount", 0.0).coerceAtLeast(0.0),
                "status" to obj.optString("status", "COMPLETED"),
                "statusReason" to obj.optString("statusReason", "").take(250),
                "stockDeducted" to obj.optBoolean("stockDeducted", true),
                "stockRestored" to obj.optBoolean("stockRestored", false),
                "transactionMillis" to obj.optLong("transactionMillis", System.currentTimeMillis()).coerceAtLeast(1L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            writeOps += { batch -> batch.set(ref, txPayload) }
        }

        val expensesArr = root.optJSONArray("expenses")
        if (expensesArr != null) {
            for (i in 0 until expensesArr.length()) {
                val obj = expensesArr.getJSONObject(i)
                val id = obj.optString("id").takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
                    ?: ("exp_" + UUID.randomUUID().toString().replace("-", "").take(16))
                val title = obj.optString("title").trim().take(120)
                if (title.isBlank()) continue
                val ref = expensesCol(uid).document(id)
                val common = mapOf(
                    "title" to title,
                    "category" to obj.optString("category", ExpenseCategory.OPERATIONAL.labelId).trim().ifBlank { ExpenseCategory.OPERATIONAL.labelId }.take(60),
                    "amount" to PosCalculator.roundCurrency(obj.optDouble("amount", 0.0).coerceAtLeast(0.0)),
                    "notes" to obj.optString("notes", "").take(400),
                    "expenseMillis" to obj.optLong("expenseMillis", System.currentTimeMillis()).coerceAtLeast(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (id in existingExpIds) {
                    writeOps += { batch -> batch.update(ref, common) }
                } else {
                    writeOps += { batch ->
                        batch.set(
                            ref,
                            common + mapOf(
                                "userId" to uid,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }
        }

        // Commit in chunks of 400 to stay well under Firestore 500-op batch limit
        writeOps.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { op -> op(batch) }
            batch.commit().await()
        }

        summary
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, userDoc(userId).path)
    }

    private fun logIfNotCancellation(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<com.google.firebase.firestore.FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: error
        if (firebaseAuth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
