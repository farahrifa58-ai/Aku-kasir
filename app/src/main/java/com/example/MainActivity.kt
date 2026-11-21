package com.example

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.LicenseActivationRepository
import com.example.data.firebase.LicenseActivationResult
import com.example.data.local.LocalPosDatabase
import com.example.data.local.LocalPosRepository
import com.example.data.model.PosTransaction
import com.example.data.prefs.LocalAppPrefs
import com.example.data.prefs.LocalPreferencesManager
import com.example.ui.activation.ActivationScreen
import com.example.ui.components.QuickBackupStatusDialog
import com.example.ui.components.ReceiptDialog
import com.example.ui.screens.CashierScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.SalesReportScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AdaKasirTheme
import com.example.ui.theme.BrandBlueDark
import com.example.ui.theme.EnglishStrings
import com.example.ui.theme.IndonesianStrings
import com.example.ui.theme.LocalAppStrings
import com.example.ui.viewmodel.AppMenu
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.UiNotice
import com.example.util.BackupStatusHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val fixedConfig = Configuration(newBase.resources.configuration).apply {
            fontScale = 1.0f
        }
        super.attachBaseContext(newBase.createConfigurationContext(fixedConfig))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            AdaKasirRoot()
        }
    }
}

@Composable
fun AdaKasirRoot(
    firestoreProvider: (() -> FirebaseFirestore)? = null,
    localDatabaseProvider: (() -> LocalPosDatabase)? = null,
    splashDurationMs: Long = 1200L,
    onActivateOverride: (suspend (businessName: String, activationCode: String, deviceId: String) -> Result<LicenseActivationResult>)? = null
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val prefsManager = remember(appContext) { LocalPreferencesManager(appContext) }
    val loadedPrefs by prefsManager.prefsFlow.collectAsStateWithLifecycle(initialValue = null)

    var deviceId by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(prefsManager) {
        deviceId = prefsManager.getOrCreatePersistentDeviceId()
    }

    var showSplash by rememberSaveable { mutableStateOf(splashDurationMs > 0L) }
    LaunchedEffect(splashDurationMs) {
        if (splashDurationMs > 0L && showSplash) {
            delay(splashDurationMs)
            showSplash = false
        }
    }

    val effectivePrefs = loadedPrefs ?: LocalAppPrefs()
    val strings = if (effectivePrefs.language == "en") EnglishStrings else IndonesianStrings

    CompositionLocalProvider(LocalAppStrings provides strings) {
        AdaKasirTheme(darkTheme = effectivePrefs.darkTheme) {
            // Wait for DataStore to load license status so already-activated devices
            // jump straight to Dashboard offline with zero flash of ActivationScreen.
            if (showSplash || loadedPrefs == null) {
                AdaKasirSplashScreen()
            } else if (!effectivePrefs.isLicensed) {
                val licenseRepo = remember(appContext, firestoreProvider) {
                    val db = firestoreProvider?.invoke() ?: FirebaseFirestore.getInstance(
                        appContext.getString(R.string.firestore_database_id)
                    )
                    LicenseActivationRepository(db)
                }

                ActivationScreen(
                    deviceId = deviceId,
                    onActivate = { businessName, code ->
                        val resolvedDeviceId = deviceId.ifBlank {
                            prefsManager.getOrCreatePersistentDeviceId().also { deviceId = it }
                        }
                        val activationResult = if (onActivateOverride != null) {
                            onActivateOverride(businessName, code, resolvedDeviceId)
                        } else {
                            licenseRepo.activateLicense(
                                businessName = businessName,
                                rawCode = code,
                                deviceId = resolvedDeviceId
                            )
                        }
                        activationResult.onSuccess { activated ->
                            prefsManager.saveLicenseActivation(
                                licenseCode = activated.licenseCode,
                                deviceId = activated.deviceId,
                                businessName = activated.businessName,
                                activatedAtMillis = activated.activatedAtMillis
                            )
                        }
                    },
                    onActivationSuccess = {
                        // DataStore prefsFlow automatically updates effectivePrefs.isLicensed = true
                    }
                )
            } else {
                // Licensed device: 100% offline-first local database, no login, no logout
                val viewModel: PosViewModel = viewModel(
                    key = "local_pos_${effectivePrefs.licenseCode}",
                    factory = viewModelFactory {
                        initializer {
                            val localDb = localDatabaseProvider?.invoke()
                                ?: LocalPosDatabase.getInstance(appContext)
                            PosViewModel(
                                repository = LocalPosRepository(localDb),
                                prefsManager = prefsManager,
                                currentUserId = LocalPosRepository.LOCAL_USER_ID,
                                initialBusinessName = effectivePrefs.businessName
                            )
                        }
                    }
                )
                AdaKasirAppShell(
                    viewModel = viewModel,
                    businessDisplayName = effectivePrefs.businessName
                )
            }
        }
    }
}

@Composable
internal fun AdaKasirSplashScreen(modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF93C5FD),
                        Color(0xFFDBEAFE),
                        Color(0xFFFFFFFF)
                    )
                )
            )
            .safeDrawingPadding()
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ada_kasir_logo),
                    contentDescription = strings.appName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(132.dp)
                        .clip(RoundedCornerShape(32.dp))
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = strings.appName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = BrandBlueDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.splashByDeveloper,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E40AF),
                modifier = Modifier.testTag("splash_brand_footer")
            )
        }
    }
}

@Composable
internal fun AdaKasirAppShell(
    viewModel: PosViewModel,
    businessDisplayName: String = ""
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val localPrefs by viewModel.localPrefs.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val currentMenu by viewModel.currentMenu.collectAsStateWithLifecycle()
    val dashboardPeriod by viewModel.dashboardPeriod.collectAsStateWithLifecycle()
    val cashierState by viewModel.cashierState.collectAsStateWithLifecycle()
    val totals by viewModel.checkoutTotals.collectAsStateWithLifecycle()
    val completedReceipt by viewModel.completedReceipt.collectAsStateWithLifecycle()
    val isBackupBusy by viewModel.isBackupBusy.collectAsStateWithLifecycle()
    val openBackupDialogOnSettings by viewModel.openBackupDialogOnSettings.collectAsStateWithLifecycle()

    val strings = if (settings.language == "en") EnglishStrings else IndonesianStrings
    val snackbarHostState = remember { SnackbarHostState() }
    var historyQuickSelectedTx by remember { mutableStateOf<PosTransaction?>(null) }
    var showQuickBackupDialog by remember { mutableStateOf(false) }

    val effectiveLastBackupAt = BackupStatusHelper.resolveEffectiveLastBackupAt(
        prefsLastBackupAt = localPrefs.lastBackupAt,
        storeLastBackupAtMillis = settings.lastBackupAt?.toDate()?.time
    )
    val backupStatus = BackupStatusHelper.evaluateStatus(
        lastBackupAtMillis = effectiveLastBackupAt,
        hasNewDataSinceBackup = localPrefs.hasNewDataSinceBackup
    )

    LaunchedEffect(Unit) {
        viewModel.notices.collect { notice ->
            val msg = when (notice) {
                is UiNotice.Info -> notice.message
                is UiNotice.Error -> notice.message
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Pressing system Back inside any sub-screen returns to Dashboard
    BackHandler(enabled = currentMenu != AppMenu.DASHBOARD) {
        viewModel.selectMenu(AppMenu.DASHBOARD)
    }

    CompositionLocalProvider(LocalAppStrings provides strings) {
        AdaKasirTheme(darkTheme = settings.darkTheme) {
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier.fillMaxSize()
            ) { _ ->
                Box(modifier = Modifier.fillMaxSize()) {
                    Crossfade(
                        targetState = currentMenu,
                        animationSpec = tween(durationMillis = 200),
                        label = "dashboard_menu_crossfade"
                    ) { menu ->
                        when (menu) {
                            AppMenu.DASHBOARD -> DashboardScreen(
                                store = settings,
                                userDisplayName = settings.ownerName.ifBlank { businessDisplayName },
                                products = products,
                                transactions = transactions,
                                selectedPeriod = dashboardPeriod,
                                onSelectPeriod = viewModel::selectDashboardPeriod,
                                onSelectMenu = viewModel::selectMenu,
                                onNavigateToHistory = { viewModel.selectMenu(AppMenu.HISTORY) },
                                onNavigateToProducts = { viewModel.selectMenu(AppMenu.PRODUCTS) },
                                onSelectTransaction = { tx ->
                                    historyQuickSelectedTx = tx
                                    viewModel.selectMenu(AppMenu.HISTORY)
                                },
                                backupStatus = backupStatus,
                                onBackupStatusClick = { showQuickBackupDialog = true },
                                isMonitorMode = false
                            )

                            AppMenu.CASHIER -> CashierScreen(
                                products = products,
                                categories = categories,
                                store = settings,
                                cashierState = cashierState,
                                totals = totals,
                                onAddProduct = { prod, qty ->
                                    viewModel.addProductToCart(prod, qty, strings.insufficientStockError)
                                },
                                onSetCartQuantity = { prodId, qty ->
                                    viewModel.setCartItemQuantity(prodId, qty, strings.insufficientStockError)
                                },
                                onRemoveCartItem = viewModel::removeFromCart,
                                onCustomerNameChange = viewModel::updateCustomerName,
                                onDiscountChange = viewModel::updateDiscount,
                                onTaxChange = viewModel::updateTax,
                                onPaymentMethodChange = viewModel::updatePaymentMethod,
                                onAmountPaidChange = viewModel::updateAmountPaidInput,
                                onFillExactCash = viewModel::fillExactAmountPaid,
                                onCompleteCheckout = {
                                    viewModel.completeCheckout(
                                        insufficientPaymentMsg = strings.insufficientPaymentError,
                                        insufficientStockMsg = strings.insufficientStockError,
                                        successMsg = strings.transactionSuccessMsg
                                    )
                                },
                                onClearCart = viewModel::clearCart,
                                onBackToDashboard = { viewModel.selectMenu(AppMenu.DASHBOARD) },
                                backupStatus = backupStatus,
                                onBackupStatusClick = { showQuickBackupDialog = true }
                            )

                            AppMenu.PRODUCTS -> ProductsScreen(
                                userId = viewModel.currentUserId,
                                products = products,
                                categories = categories,
                                onSaveProduct = viewModel::saveProduct,
                                onDeleteProduct = viewModel::deleteProduct,
                                onAddCategory = viewModel::addCategory,
                                onUpdateCategory = viewModel::updateCategory,
                                onDeleteCategory = viewModel::deleteCategory,
                                onBackToDashboard = { viewModel.selectMenu(AppMenu.DASHBOARD) }
                            )

                            AppMenu.SALES_REPORT -> SalesReportScreen(
                                store = settings,
                                products = products,
                                transactions = transactions,
                                expenses = expenses,
                                onBackToDashboard = { viewModel.selectMenu(AppMenu.DASHBOARD) },
                                onSelectTransaction = { tx ->
                                    historyQuickSelectedTx = tx
                                    viewModel.selectMenu(AppMenu.HISTORY)
                                }
                            )

                            AppMenu.EXPENSES -> ExpensesScreen(
                                userId = viewModel.currentUserId,
                                language = settings.language,
                                expenses = expenses,
                                onBackToDashboard = { viewModel.selectMenu(AppMenu.DASHBOARD) },
                                onSaveExpense = { expense, onSaved ->
                                    viewModel.saveExpense(expense, strings.expenseSavedMsg, onSaved)
                                },
                                onDeleteExpense = viewModel::deleteExpense
                            )

                            AppMenu.HISTORY -> HistoryScreen(
                                transactions = transactions,
                                store = settings,
                                initialSelectedTransaction = historyQuickSelectedTx,
                                defaultPaperWidthMm = localPrefs.printerPaperWidthMm,
                                defaultPrinterAddress = localPrefs.defaultPrinterAddress,
                                onClearInitialSelection = { historyQuickSelectedTx = null },
                                onCancelOrRefund = viewModel::cancelOrRefundTransaction,
                                onBackToDashboard = { viewModel.selectMenu(AppMenu.DASHBOARD) }
                            )

                            AppMenu.SETTINGS -> SettingsScreen(
                                store = settings,
                                localPrefs = localPrefs,
                                isBackupBusy = isBackupBusy,
                                onBackToDashboard = { viewModel.selectMenu(AppMenu.DASHBOARD) },
                                onSaveProfile = { storeName, ownerName, phone, address, photoData, storeIcon, storeIconBgColor, receiptPrefix, taxPct ->
                                    viewModel.saveStoreProfile(
                                        storeName = storeName,
                                        ownerName = ownerName,
                                        phone = phone,
                                        address = address,
                                        photoData = photoData,
                                        storeIcon = storeIcon,
                                        storeIconBgColor = storeIconBgColor,
                                        receiptPrefix = receiptPrefix,
                                        defaultTaxPercent = taxPct,
                                        successMsg = strings.profileSavedMsg
                                    )
                                },
                                onSaveReceiptCustomization = { showLogo, showCashier, showPhone, showAddress, showCustomer, footer ->
                                    viewModel.saveReceiptCustomization(
                                        showLogo = showLogo,
                                        showCashierName = showCashier,
                                        showStorePhone = showPhone,
                                        showStoreAddress = showAddress,
                                        showCustomerName = showCustomer,
                                        footerMessage = footer,
                                        successMsg = strings.receiptCustomSavedMsg
                                    )
                                },
                                onSetDarkTheme = viewModel::setDarkTheme,
                                onSetLanguage = viewModel::setLanguage,
                                onSelectPrinter = viewModel::selectDefaultPrinter,
                                onClearPrinter = viewModel::clearDefaultPrinter,
                                onSetPrinterPaperWidth = viewModel::setPrinterPaperWidth,
                                onGenerateBackupJson = viewModel::generateBackupJson,
                                onValidateBackupJson = viewModel::validateBackupJson,
                                onRestoreBackupJson = { json ->
                                    viewModel.restoreBackupJson(
                                        jsonString = json,
                                        successMsg = strings.restoreSuccessMsg,
                                        invalidMsg = strings.restoreInvalidMsg
                                    )
                                },
                                openBackupDialogInitially = openBackupDialogOnSettings,
                                onConsumeOpenBackupDialog = viewModel::consumeOpenBackupDialogRequest,
                                onBackupExportSucceeded = viewModel::markBackupExportCompleted
                            )
                        }
                    }
                }
            }

            val receiptTx = completedReceipt
            if (receiptTx != null) {
                ReceiptDialog(
                    transaction = receiptTx,
                    store = settings,
                    language = settings.language,
                    defaultPaperWidthMm = localPrefs.printerPaperWidthMm,
                    defaultPrinterAddress = localPrefs.defaultPrinterAddress,
                    onDismiss = viewModel::dismissCompletedReceipt
                )
            }

            if (showQuickBackupDialog) {
                QuickBackupStatusDialog(
                    status = backupStatus,
                    lastBackupAtMillis = effectiveLastBackupAt,
                    hasNewDataSinceBackup = localPrefs.hasNewDataSinceBackup,
                    language = settings.language,
                    isBackupBusy = isBackupBusy,
                    onOpenBackupSettings = {
                        showQuickBackupDialog = false
                        viewModel.openBackupSettings()
                    },
                    onDismiss = { showQuickBackupDialog = false }
                )
            }
        }
    }
}
