package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BackupSummary
import com.example.data.local.LocalPosRepository
import com.example.data.model.CartLine
import com.example.data.model.CheckoutTotals
import com.example.data.model.DashboardPeriod
import com.example.data.model.DiscountType
import com.example.data.model.Expense
import com.example.data.model.PaymentMethod
import com.example.data.model.PosCalculator
import com.example.data.model.PosTransaction
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.StoreSettings
import com.example.data.model.TaxType
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionStatus
import com.example.data.prefs.LocalAppPrefs
import com.example.data.prefs.LocalPreferencesManager
import com.example.util.PosFormatters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppMenu {
    DASHBOARD,
    CASHIER,
    PRODUCTS,
    SALES_REPORT,
    EXPENSES,
    HISTORY,
    SETTINGS
}

data class CashierUiState(
    val cart: List<CartLine> = emptyList(),
    val customerName: String = "",
    val discountType: DiscountType = DiscountType.NONE,
    val discountInput: String = "",
    val taxType: TaxType = TaxType.NONE,
    val manualTaxInput: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val amountPaidInput: String = "",
    val isSubmitting: Boolean = false
)

sealed interface UiNotice {
    data class Info(val message: String) : UiNotice
    data class Error(val message: String) : UiNotice
}

class PosViewModel(
    private val repository: LocalPosRepository,
    private val prefsManager: LocalPreferencesManager,
    val currentUserId: String = LocalPosRepository.LOCAL_USER_ID,
    private val initialBusinessName: String = ""
) : ViewModel() {

    private val _currentMenu = MutableStateFlow(AppMenu.DASHBOARD)
    val currentMenu: StateFlow<AppMenu> = _currentMenu.asStateFlow()

    private val _dashboardPeriod = MutableStateFlow(DashboardPeriod.TODAY)
    val dashboardPeriod: StateFlow<DashboardPeriod> = _dashboardPeriod.asStateFlow()

    private val _cashierState = MutableStateFlow(CashierUiState())
    val cashierState: StateFlow<CashierUiState> = _cashierState.asStateFlow()

    private val _completedReceipt = MutableStateFlow<PosTransaction?>(null)
    val completedReceipt: StateFlow<PosTransaction?> = _completedReceipt.asStateFlow()

    private val _isBackupBusy = MutableStateFlow(false)
    val isBackupBusy: StateFlow<Boolean> = _isBackupBusy.asStateFlow()

    private val _openBackupDialogOnSettings = MutableStateFlow(false)
    val openBackupDialogOnSettings: StateFlow<Boolean> = _openBackupDialogOnSettings.asStateFlow()

    private val _notices = MutableSharedFlow<UiNotice>(extraBufferCapacity = 8)
    val notices = _notices.asSharedFlow()

    val localPrefs: StateFlow<LocalAppPrefs> = prefsManager.prefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalAppPrefs())

    val settings: StateFlow<StoreSettings> = repository.observeSettings()
        .catch { error ->
            if (error is CancellationException) throw error
            emit(StoreSettings(userId = currentUserId, storeName = initialBusinessName.ifBlank { "Toko ADA KASIR" }))
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            StoreSettings(userId = currentUserId, storeName = initialBusinessName.ifBlank { "Toko ADA KASIR" })
        )

    val categories: StateFlow<List<ProductCategory>> = repository.observeCategories()
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val products: StateFlow<List<Product>> = repository.observeProducts()
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val transactions: StateFlow<List<PosTransaction>> = repository.observeTransactions()
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val expenses: StateFlow<List<Expense>> = repository.observeExpenses()
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val checkoutTotals: StateFlow<CheckoutTotals> = combine(
        _cashierState,
        settings
    ) { cashier, storeSettings ->
        val subtotal = cashier.cart.sumOf { it.subtotal }
        val discountVal = cashier.discountInput.replace(",", ".").toDoubleOrNull() ?: 0.0
        val manualTaxVal = cashier.manualTaxInput.replace(",", ".").toDoubleOrNull() ?: 0.0
        val amountPaidVal = cashier.amountPaidInput.replace(",", ".").toDoubleOrNull() ?: 0.0
        PosCalculator.calculateTotals(
            subtotal = subtotal,
            discountType = cashier.discountType,
            discountValue = discountVal,
            taxType = cashier.taxType,
            autoTaxPercent = storeSettings.defaultTaxPercent,
            manualTaxAmount = manualTaxVal,
            amountPaid = amountPaidVal
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        CheckoutTotals(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    )

    init {
        viewModelScope.launch {
            val local = prefsManager.prefsFlow.first()
            val shouldSeedSamples = !prefsManager.hasSeededSampleProducts()
            val shouldSeedKgPack = !prefsManager.hasSeededKgPackSamples()
            val resolvedName = initialBusinessName.ifBlank { local.businessName }
            repository.ensureInitialLocalSetup(
                businessName = resolvedName,
                defaultDarkTheme = local.darkTheme,
                defaultLanguage = local.language,
                seedSampleProducts = shouldSeedSamples,
                seedKgPackSamples = shouldSeedSamples || shouldSeedKgPack
            )
            if (shouldSeedSamples) {
                prefsManager.markSampleProductsSeeded()
            }
            if (shouldSeedSamples || shouldSeedKgPack) {
                prefsManager.markKgPackSamplesSeeded()
            }
        }
    }

    fun selectMenu(menu: AppMenu) {
        _currentMenu.value = menu
    }

    fun openBackupSettings() {
        _openBackupDialogOnSettings.value = true
        _currentMenu.value = AppMenu.SETTINGS
    }

    fun consumeOpenBackupDialogRequest() {
        _openBackupDialogOnSettings.value = false
    }

    fun selectDashboardPeriod(period: DashboardPeriod) {
        _dashboardPeriod.value = period
    }

    // --- Cashier Cart & Checkout (100% Offline) ---
    fun addProductToCart(product: Product, quantity: Double, stockErrorMsg: String) {
        if (product.isOutOfStock) {
            _notices.tryEmit(UiNotice.Error(stockErrorMsg))
            return
        }
        val roundedQty = PosCalculator.roundQuantity(quantity)
        if (roundedQty <= 0.0) return

        _cashierState.update { state ->
            val existingIdx = state.cart.indexOfFirst { it.product.id == product.id }
            val currentQty = if (existingIdx >= 0) state.cart[existingIdx].quantity else 0.0
            val newQty = PosCalculator.roundQuantity(currentQty + roundedQty)
            if (newQty > product.stock + 0.0001) {
                _notices.tryEmit(UiNotice.Error(stockErrorMsg))
                return@update state
            }
            val updatedCart = if (existingIdx >= 0) {
                state.cart.toMutableList().apply {
                    this[existingIdx] = this[existingIdx].copy(product = product, quantity = newQty)
                }
            } else {
                state.cart + CartLine(product = product, quantity = newQty)
            }
            state.copy(cart = updatedCart)
        }
    }

    fun setCartItemQuantity(productId: String, newQty: Double, stockErrorMsg: String) {
        val rounded = PosCalculator.roundQuantity(newQty)
        if (rounded <= 0.0) {
            removeFromCart(productId)
            return
        }
        _cashierState.update { state ->
            val line = state.cart.firstOrNull { it.product.id == productId } ?: return@update state
            val liveProd = products.value.firstOrNull { it.id == productId } ?: line.product
            if (rounded > liveProd.stock + 0.0001) {
                _notices.tryEmit(UiNotice.Error(stockErrorMsg))
                return@update state
            }
            state.copy(
                cart = state.cart.map {
                    if (it.product.id == productId) it.copy(product = liveProd, quantity = rounded) else it
                }
            )
        }
    }

    fun removeFromCart(productId: String) {
        _cashierState.update { state ->
            state.copy(cart = state.cart.filterNot { it.product.id == productId })
        }
    }

    fun clearCart() {
        _cashierState.value = CashierUiState()
    }

    fun updateCustomerName(name: String) {
        _cashierState.update { it.copy(customerName = name) }
    }

    fun updateDiscount(type: DiscountType, input: String) {
        _cashierState.update { it.copy(discountType = type, discountInput = input) }
    }

    fun updateTax(type: TaxType, manualInput: String) {
        _cashierState.update { it.copy(taxType = type, manualTaxInput = manualInput) }
    }

    fun updatePaymentMethod(method: PaymentMethod) {
        _cashierState.update { it.copy(paymentMethod = method) }
    }

    fun updateAmountPaidInput(input: String) {
        _cashierState.update { it.copy(amountPaidInput = input) }
    }

    fun fillExactAmountPaid() {
        val total = checkoutTotals.value.grandTotal.toLong().toString()
        _cashierState.update { it.copy(amountPaidInput = total) }
    }

    fun completeCheckout(
        insufficientPaymentMsg: String,
        insufficientStockMsg: String,
        successMsg: String
    ) {
        val state = _cashierState.value
        if (state.isSubmitting || state.cart.isEmpty()) return

        val totals = checkoutTotals.value
        val amountPaid = if (state.paymentMethod == PaymentMethod.CASH) {
            state.amountPaidInput.replace(",", ".").toDoubleOrNull() ?: 0.0
        } else {
            val entered = state.amountPaidInput.replace(",", ".").toDoubleOrNull()
            if (entered != null && entered > 0.0) entered else totals.grandTotal
        }

        if (amountPaid + 0.01 < totals.grandTotal) {
            _notices.tryEmit(UiNotice.Error(insufficientPaymentMsg))
            return
        }

        val liveProductMap = products.value.associateBy { it.id }
        for (line in state.cart) {
            val liveStock = liveProductMap[line.product.id]?.stock ?: line.product.stock
            if (line.quantity > liveStock + 0.0001) {
                _notices.tryEmit(UiNotice.Error(insufficientStockMsg))
                return
            }
        }

        val storeTaxPct = if (state.taxType == TaxType.AUTO) settings.value.defaultTaxPercent else 0.0
        val discountVal = state.discountInput.replace(",", ".").toDoubleOrNull() ?: 0.0
        val nowMillis = System.currentTimeMillis()
        val txId = "tx_" + UUID.randomUUID().toString().replace("-", "").take(16)
        val txNumber = PosFormatters.generateTransactionNumber(nowMillis, settings.value.receiptPrefix)
        val resolvedCashierName = settings.value.ownerName
            .ifBlank { settings.value.storeName }
            .ifBlank { "Kasir" }

        val tx = PosTransaction(
            id = txId,
            userId = currentUserId,
            transactionNumber = txNumber,
            customerName = state.customerName.trim(),
            cashierName = resolvedCashierName.take(80),
            items = state.cart.map { line ->
                TransactionItem(
                    productId = line.product.id,
                    productName = line.product.name,
                    unit = line.product.unit,
                    unitPrice = line.product.sellPrice,
                    costPrice = line.product.costPrice,
                    quantity = PosCalculator.roundQuantity(line.quantity),
                    subtotal = line.subtotal
                )
            },
            subtotal = totals.subtotal,
            discountType = state.discountType.code,
            discountValue = discountVal,
            discountAmount = totals.discountAmount,
            taxType = state.taxType.code,
            taxPercent = storeTaxPct,
            taxAmount = totals.taxAmount,
            totalAmount = totals.grandTotal,
            paymentMethod = state.paymentMethod.code,
            amountPaid = PosCalculator.roundCurrency(amountPaid),
            changeAmount = PosCalculator.roundCurrency((amountPaid - totals.grandTotal).coerceAtLeast(0.0)),
            status = TransactionStatus.COMPLETED.code,
            statusReason = "",
            stockDeducted = true,
            stockRestored = false,
            transactionMillis = nowMillis
        )

        viewModelScope.launch {
            _cashierState.update { it.copy(isSubmitting = true) }
            val result = repository.completeTransaction(tx)
            _cashierState.update { it.copy(isSubmitting = false) }
            result.onSuccess { savedTx ->
                prefsManager.markNewDataSinceBackup()
                _cashierState.value = CashierUiState()
                _completedReceipt.value = savedTx
                _notices.tryEmit(UiNotice.Info(successMsg))
            }.onFailure { err ->
                _notices.tryEmit(UiNotice.Error(err.localizedMessage ?: insufficientStockMsg))
            }
        }
    }

    fun dismissCompletedReceipt() {
        _completedReceipt.value = null
    }

    // --- Products & Categories (100% Local) ---
    fun saveProduct(product: Product, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveProduct(product)
                .onSuccess {
                    prefsManager.markNewDataSinceBackup()
                    onSaved()
                }
                .onFailure { _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Gagal menyimpan produk")) }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
                .onSuccess { prefsManager.markNewDataSinceBackup() }
                .onFailure { _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Gagal menghapus produk")) }
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            repository.addCategory(name)
                .onSuccess { prefsManager.markNewDataSinceBackup() }
                .onFailure { _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Gagal menambah kategori")) }
        }
    }

    fun updateCategory(categoryId: String, newName: String) {
        viewModelScope.launch {
            repository.updateCategory(categoryId, newName)
                .onSuccess { prefsManager.markNewDataSinceBackup() }
                .onFailure { _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Gagal memperbarui kategori")) }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
                .onSuccess { prefsManager.markNewDataSinceBackup() }
                .onFailure { _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Gagal menghapus kategori")) }
        }
    }

    // --- Expenses (100% Local) ---
    fun saveExpense(expense: Expense, successMsg: String, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveExpense(expense)
                .onSuccess {
                    prefsManager.markNewDataSinceBackup()
                    _notices.tryEmit(UiNotice.Info(successMsg))
                    onSaved()
                }
                .onFailure { _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Gagal menyimpan pengeluaran")) }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
                .onSuccess { prefsManager.markNewDataSinceBackup() }
                .onFailure { _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Gagal menghapus pengeluaran")) }
        }
    }

    // --- History Cancel / Refund (100% Local) ---
    fun cancelOrRefundTransaction(
        transactionId: String,
        targetStatus: TransactionStatus,
        reason: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.cancelOrRefundTransaction(transactionId, targetStatus, reason)
                .onSuccess {
                    prefsManager.markNewDataSinceBackup()
                    onComplete()
                }
                .onFailure { _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Operasi gagal")) }
        }
    }

    // --- Settings, Theme, Language, Receipt Customization, Offline Backup & Restore ---
    fun saveStoreProfile(
        storeName: String,
        ownerName: String,
        phone: String,
        address: String,
        photoData: String,
        storeIcon: String = "storefront",
        storeIconBgColor: String = "#2563EB",
        receiptPrefix: String = "AK",
        defaultTaxPercent: Double,
        successMsg: String
    ) {
        viewModelScope.launch {
            repository.saveStoreProfile(
                storeName = storeName,
                ownerName = ownerName,
                phone = phone,
                address = address,
                photoData = photoData,
                storeIcon = storeIcon,
                storeIconBgColor = storeIconBgColor,
                receiptPrefix = receiptPrefix,
                defaultTaxPercent = defaultTaxPercent
            ).onSuccess {
                prefsManager.markNewDataSinceBackup()
                _notices.tryEmit(UiNotice.Info(successMsg))
            }.onFailure {
                _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Gagal menyimpan profil"))
            }
        }
    }

    fun saveReceiptCustomization(
        showLogo: Boolean,
        showCashierName: Boolean,
        showStorePhone: Boolean,
        showStoreAddress: Boolean,
        showCustomerName: Boolean,
        footerMessage: String,
        successMsg: String
    ) {
        viewModelScope.launch {
            repository.saveReceiptCustomization(
                showLogo = showLogo,
                showCashierName = showCashierName,
                showStorePhone = showStorePhone,
                showStoreAddress = showStoreAddress,
                showCustomerName = showCustomerName,
                footerMessage = footerMessage
            ).onSuccess {
                prefsManager.markNewDataSinceBackup()
                _notices.tryEmit(UiNotice.Info(successMsg))
            }.onFailure {
                _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Gagal menyimpan struk"))
            }
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            prefsManager.setDarkTheme(enabled)
            repository.updateTheme(enabled)
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            prefsManager.setLanguage(language)
            repository.updateLanguage(language)
        }
    }

    suspend fun generateBackupJson(): Result<String> {
        _isBackupBusy.value = true
        return try {
            repository.exportBackupJson()
        } finally {
            _isBackupBusy.value = false
        }
    }

    fun markBackupExportCompleted() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            prefsManager.markBackupCompleted(now)
            repository.recordBackupCompleted(now)
        }
    }

    fun validateBackupJson(jsonString: String): Result<BackupSummary> =
        repository.validateBackupJson(jsonString)

    fun restoreBackupJson(jsonString: String, successMsg: String, invalidMsg: String) {
        viewModelScope.launch {
            _isBackupBusy.value = true
            try {
                repository.restoreBackupJson(jsonString)
                    .onSuccess {
                        val updated = settings.value
                        prefsManager.setDarkTheme(updated.darkTheme)
                        prefsManager.setLanguage(updated.language)
                        _notices.tryEmit(UiNotice.Info(successMsg))
                    }
                    .onFailure { _notices.tryEmit(UiNotice.Error(invalidMsg)) }
            } finally {
                _isBackupBusy.value = false
            }
        }
    }

    fun setPrinterPaperWidth(mm: Int) {
        viewModelScope.launch {
            prefsManager.setPrinterPaperWidth(mm)
        }
    }

    fun selectDefaultPrinter(name: String, address: String) {
        viewModelScope.launch {
            prefsManager.setDefaultPrinter(name, address)
        }
    }

    fun clearDefaultPrinter() {
        viewModelScope.launch {
            prefsManager.clearDefaultPrinter()
        }
    }

    fun emitNotice(notice: UiNotice) {
        _notices.tryEmit(notice)
    }
}
