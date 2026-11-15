package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.BackupSummary
import com.example.data.firebase.PosRepository
import com.example.data.model.AppAccessMode
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
import com.example.data.model.StoreDirectoryEntry
import com.example.data.model.StoreSettings
import com.example.data.model.TaxType
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionStatus
import com.example.data.prefs.LocalAppPrefs
import com.example.data.prefs.LocalPreferencesManager
import com.example.util.PosFormatters
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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

@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModel(
    private val repository: PosRepository,
    private val prefsManager: LocalPreferencesManager,
    val currentUserId: String,
    private val userDisplayName: String,
    val userEmail: String
) : ViewModel() {

    private val _isHomeMenuVisible = MutableStateFlow(true)
    val isHomeMenuVisible: StateFlow<Boolean> = _isHomeMenuVisible.asStateFlow()

    private val _currentMenu = MutableStateFlow(AppMenu.DASHBOARD)
    val currentMenu: StateFlow<AppMenu> = _currentMenu.asStateFlow()

    private val _isMenuPanelOpen = MutableStateFlow(false)
    val isMenuPanelOpen: StateFlow<Boolean> = _isMenuPanelOpen.asStateFlow()

    private val _dashboardPeriod = MutableStateFlow(DashboardPeriod.TODAY)
    val dashboardPeriod: StateFlow<DashboardPeriod> = _dashboardPeriod.asStateFlow()

    private val _cashierState = MutableStateFlow(CashierUiState())
    val cashierState: StateFlow<CashierUiState> = _cashierState.asStateFlow()

    private val _completedReceipt = MutableStateFlow<PosTransaction?>(null)
    val completedReceipt: StateFlow<PosTransaction?> = _completedReceipt.asStateFlow()

    private val _isBackupBusy = MutableStateFlow(false)
    val isBackupBusy: StateFlow<Boolean> = _isBackupBusy.asStateFlow()

    private val _streamError = MutableStateFlow<String?>(null)
    val streamError: StateFlow<String?> = _streamError.asStateFlow()

    private val _notices = MutableSharedFlow<UiNotice>(extraBufferCapacity = 8)
    val notices = _notices.asSharedFlow()

    val localPrefs: StateFlow<LocalAppPrefs> = prefsManager.prefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalAppPrefs())

    val isMonitorMode: StateFlow<Boolean> = localPrefs
        .map { it.accessMode == AppAccessMode.MONITOR.code && it.monitorTargetUserId.isNotBlank() && it.monitorTargetUserId != currentUserId }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    private val activeStoreUserId: StateFlow<String> = localPrefs
        .map { prefs ->
            if (prefs.accessMode == AppAccessMode.MONITOR.code && prefs.monitorTargetUserId.isNotBlank()) {
                prefs.monitorTargetUserId
            } else {
                currentUserId
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), currentUserId)

    val authorizedMonitorStores: StateFlow<List<StoreDirectoryEntry>> =
        repository.observeAuthorizedStoresForMonitor(userEmail)
            .map { stores -> stores.filter { it.userId != currentUserId } }
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val settings: StateFlow<StoreSettings> = activeStoreUserId
        .flatMapLatest { targetUid ->
            repository.observeSettings(targetUid)
                .catch { error ->
                    handleStreamError(error)
                    emit(StoreSettings(userId = targetUid))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), StoreSettings(userId = currentUserId))

    val categories: StateFlow<List<ProductCategory>> = activeStoreUserId
        .flatMapLatest { targetUid ->
            repository.observeCategories(targetUid)
                .catch { error ->
                    handleStreamError(error)
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val products: StateFlow<List<Product>> = activeStoreUserId
        .flatMapLatest { targetUid ->
            repository.observeProducts(targetUid)
                .catch { error ->
                    handleStreamError(error)
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val transactions: StateFlow<List<PosTransaction>> = activeStoreUserId
        .flatMapLatest { targetUid ->
            repository.observeTransactions(targetUid)
                .catch { error ->
                    handleStreamError(error)
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val expenses: StateFlow<List<Expense>> = activeStoreUserId
        .flatMapLatest { targetUid ->
            repository.observeExpenses(targetUid)
                .catch { error ->
                    handleStreamError(error)
                    emit(emptyList())
                }
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
            val shouldSeedSamples = !prefsManager.hasSeededSampleProducts(currentUserId)
            repository.ensureInitialUserSetup(
                userId = currentUserId,
                ownerDisplayName = userDisplayName,
                ownerEmail = userEmail,
                defaultDarkTheme = local.darkTheme,
                defaultLanguage = local.language,
                seedSampleProducts = shouldSeedSamples
            )
            if (shouldSeedSamples) {
                prefsManager.markSampleProductsSeeded(currentUserId)
            }
        }
        // Sync remote theme & language changes when operating own store
        viewModelScope.launch {
            combine(settings, isMonitorMode) { s, monitoring -> s to monitoring }
                .collect { (s, monitoring) ->
                    if (!monitoring && (s.createdAt != null || s.updatedAt != null)) {
                        prefsManager.setDarkTheme(s.darkTheme)
                        prefsManager.setLanguage(s.language)
                    }
                }
        }
    }

    private fun handleStreamError(error: Throwable) {
        val fsError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (fsError == null && error is CancellationException) throw error
        _streamError.value = (fsError ?: error).localizedMessage
    }

    private fun blockIfMonitorMode(): Boolean {
        if (isMonitorMode.value) {
            val msg = if (localPrefs.value.language == "en") {
                "Monitor Mode is read-only. Switch to Cashier Mode to edit data."
            } else {
                "Mode Pemantau bersifat hanya-baca. Ubah ke Mode Kasir untuk mengedit data."
            }
            _notices.tryEmit(UiNotice.Error(msg))
            return true
        }
        return false
    }

    fun openHomeMenu() {
        _isHomeMenuVisible.value = true
        _isMenuPanelOpen.value = false
    }

    fun selectMenu(menu: AppMenu) {
        _currentMenu.value = menu
        _isHomeMenuVisible.value = false
        _isMenuPanelOpen.value = false
    }

    fun toggleMenuPanel(open: Boolean? = null) {
        _isMenuPanelOpen.value = open ?: !_isMenuPanelOpen.value
    }

    fun selectDashboardPeriod(period: DashboardPeriod) {
        _dashboardPeriod.value = period
    }

    // --- Cashier Cart & Checkout ---
    fun addProductToCart(product: Product, quantity: Double, stockErrorMsg: String) {
        if (blockIfMonitorMode()) return
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
        if (blockIfMonitorMode()) return
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
        if (blockIfMonitorMode()) return
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
        val txNumber = PosFormatters.generateTransactionNumber(nowMillis)
        val resolvedCashierName = settings.value.ownerName
            .ifBlank { userDisplayName }
            .ifBlank { userEmail.substringBefore("@") }
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
            val result = repository.completeTransaction(currentUserId, tx)
            _cashierState.update { it.copy(isSubmitting = false) }
            result.onSuccess { savedTx ->
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

    // --- Products & Categories ---
    fun saveProduct(product: Product, onSaved: () -> Unit = {}) {
        if (blockIfMonitorMode()) return
        viewModelScope.launch {
            repository.saveProduct(currentUserId, product)
                .onSuccess { onSaved() }
                .onFailure { _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Failed to save product")) }
        }
    }

    fun deleteProduct(productId: String) {
        if (blockIfMonitorMode()) return
        viewModelScope.launch {
            repository.deleteProduct(currentUserId, productId)
                .onFailure { _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Failed to delete product")) }
        }
    }

    fun addCategory(name: String) {
        if (blockIfMonitorMode()) return
        viewModelScope.launch {
            repository.addCategory(currentUserId, name)
                .onFailure { _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Failed to add category")) }
        }
    }

    fun updateCategory(categoryId: String, newName: String) {
        if (blockIfMonitorMode()) return
        viewModelScope.launch {
            repository.updateCategory(currentUserId, categoryId, newName)
                .onFailure { _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Failed to update category")) }
        }
    }

    fun deleteCategory(categoryId: String) {
        if (blockIfMonitorMode()) return
        viewModelScope.launch {
            repository.deleteCategory(currentUserId, categoryId)
                .onFailure { _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Failed to delete category")) }
        }
    }

    // --- Expenses ---
    fun saveExpense(expense: Expense, successMsg: String, onSaved: () -> Unit = {}) {
        if (blockIfMonitorMode()) return
        viewModelScope.launch {
            repository.saveExpense(currentUserId, expense)
                .onSuccess {
                    _notices.tryEmit(UiNotice.Info(successMsg))
                    onSaved()
                }
                .onFailure { _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Failed to save expense")) }
        }
    }

    fun deleteExpense(expenseId: String) {
        if (blockIfMonitorMode()) return
        viewModelScope.launch {
            repository.deleteExpense(currentUserId, expenseId)
                .onFailure { _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Failed to delete expense")) }
        }
    }

    // --- History Cancel / Refund ---
    fun cancelOrRefundTransaction(
        transactionId: String,
        targetStatus: TransactionStatus,
        reason: String,
        onComplete: () -> Unit = {}
    ) {
        if (blockIfMonitorMode()) return
        viewModelScope.launch {
            repository.cancelOrRefundTransaction(currentUserId, transactionId, targetStatus, reason)
                .onSuccess { onComplete() }
                .onFailure { _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Operation failed")) }
        }
    }

    // --- Settings, Theme, Language, Receipt Customization, User Role/Monitor, Backup ---
    fun saveStoreProfile(
        storeName: String,
        ownerName: String,
        phone: String,
        address: String,
        photoData: String,
        defaultTaxPercent: Double,
        successMsg: String
    ) {
        if (blockIfMonitorMode()) return
        viewModelScope.launch {
            repository.saveStoreProfile(
                userId = currentUserId,
                storeName = storeName,
                ownerName = ownerName,
                phone = phone,
                address = address,
                photoData = photoData,
                defaultTaxPercent = defaultTaxPercent,
                ownerEmail = userEmail
            ).onSuccess {
                _notices.tryEmit(UiNotice.Info(successMsg))
            }.onFailure {
                _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Failed to save profile"))
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
        if (blockIfMonitorMode()) return
        viewModelScope.launch {
            repository.saveReceiptCustomization(
                userId = currentUserId,
                showLogo = showLogo,
                showCashierName = showCashierName,
                showStorePhone = showStorePhone,
                showStoreAddress = showStoreAddress,
                showCustomerName = showCustomerName,
                footerMessage = footerMessage
            ).onSuccess {
                _notices.tryEmit(UiNotice.Info(successMsg))
            }.onFailure {
                _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Failed to save receipt settings"))
            }
        }
    }

    fun addMonitorEmail(email: String) {
        if (blockIfMonitorMode()) return
        val clean = email.trim().lowercase()
        if (!clean.contains("@") || clean.startsWith("@") || clean.endsWith("@")) return
        val currentList = settings.value.monitorEmails
        if (clean in currentList) return
        val updated = (currentList + clean).distinct().take(10)
        viewModelScope.launch {
            repository.saveMonitorEmails(
                userId = currentUserId,
                ownerEmail = userEmail,
                storeName = settings.value.storeName,
                emails = updated
            ).onFailure {
                _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Failed to add monitor email"))
            }
        }
    }

    fun removeMonitorEmail(email: String) {
        if (blockIfMonitorMode()) return
        val clean = email.trim().lowercase()
        val updated = settings.value.monitorEmails.filterNot { it.equals(clean, ignoreCase = true) }
        viewModelScope.launch {
            repository.saveMonitorEmails(
                userId = currentUserId,
                ownerEmail = userEmail,
                storeName = settings.value.storeName,
                emails = updated
            ).onFailure {
                _notices.tryEmit(UiNotice.Error(it.localizedMessage ?: "Failed to remove monitor email"))
            }
        }
    }

    fun setAccessMode(mode: AppAccessMode) {
        viewModelScope.launch {
            prefsManager.setAccessMode(mode.code)
            if (mode == AppAccessMode.CASHIER) {
                prefsManager.clearMonitorTarget()
            } else if (localPrefs.value.monitorTargetUserId.isBlank()) {
                authorizedMonitorStores.value.firstOrNull()?.let { firstStore ->
                    prefsManager.setMonitorTarget(
                        userId = firstStore.userId,
                        email = firstStore.ownerEmail,
                        storeName = firstStore.storeName
                    )
                }
            }
        }
    }

    fun connectMonitorStore(entry: StoreDirectoryEntry) {
        viewModelScope.launch {
            prefsManager.setMonitorTarget(
                userId = entry.userId,
                email = entry.ownerEmail,
                storeName = entry.storeName
            )
            _currentMenu.value = AppMenu.DASHBOARD
        }
    }

    fun disconnectMonitorStore() {
        viewModelScope.launch {
            prefsManager.clearMonitorTarget()
            prefsManager.setAccessMode(AppAccessMode.CASHIER.code)
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            prefsManager.setDarkTheme(enabled)
            if (!isMonitorMode.value) {
                repository.updateTheme(currentUserId, enabled)
            }
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            prefsManager.setLanguage(language)
            if (!isMonitorMode.value) {
                repository.updateLanguage(currentUserId, language)
            }
        }
    }

    suspend fun generateBackupJson(): Result<String> {
        if (blockIfMonitorMode()) {
            return Result.failure(IllegalStateException("Monitor mode is read-only"))
        }
        _isBackupBusy.value = true
        return try {
            repository.exportBackupJson(currentUserId)
        } finally {
            _isBackupBusy.value = false
        }
    }

    fun validateBackupJson(jsonString: String): Result<BackupSummary> =
        repository.validateBackupJson(jsonString)

    fun restoreBackupJson(jsonString: String, successMsg: String, invalidMsg: String) {
        if (blockIfMonitorMode()) return
        viewModelScope.launch {
            _isBackupBusy.value = true
            try {
                repository.restoreBackupJson(currentUserId, jsonString)
                    .onSuccess { _notices.tryEmit(UiNotice.Info(successMsg)) }
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
