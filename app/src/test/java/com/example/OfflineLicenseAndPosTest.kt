package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.firebase.LicenseActivationResult
import com.example.data.local.BackupSummary
import com.example.data.local.LocalPosDatabase
import com.example.data.local.LocalPosRepository
import com.example.data.model.Expense
import com.example.data.model.PaymentMethod
import com.example.data.model.PosTransaction
import com.example.data.model.Product
import com.example.data.model.StoreSettings
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionStatus
import com.example.data.prefs.LocalAppPrefs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.activation.ActivationScreen
import com.example.ui.components.BackupStatusHeaderPill
import com.example.ui.components.QuickBackupStatusDialog
import com.example.ui.components.resolveBundledProductDrawable
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AdaKasirTheme
import com.example.util.BackupHealthStatus
import com.example.util.BackupStatusHelper
import com.example.util.PosFormatters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class OfflineLicenseAndPosTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: LocalPosDatabase
    private lateinit var repository: LocalPosRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, LocalPosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LocalPosRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun localRepository_checkout_cancel_and_backupRestore_roundTrip() = runBlocking {
        repository.ensureInitialLocalSetup(
            businessName = "Warung Berkah Mandiri",
            defaultDarkTheme = false,
            defaultLanguage = "id",
            seedSampleProducts = false
        )

        val initialSettings = repository.observeSettings().first()
        assertEquals("Warung Berkah Mandiri", initialSettings.storeName)

        // Add product
        val productId = repository.saveProduct(
            Product(
                name = "Gula Pasir 1 Kg",
                sku = "GL-001",
                category = "Sembako",
                unit = "Kg",
                costPrice = 14000.0,
                sellPrice = 17500.0,
                stock = 10.0,
                minStock = 2.0
            )
        ).getOrThrow()

        // Complete transaction for 2.5 Kg
        val tx = PosTransaction(
            transactionNumber = "TRX-OFFLINE-001",
            customerName = "Budi",
            cashierName = "Owner",
            items = listOf(
                TransactionItem(
                    productId = productId,
                    productName = "Gula Pasir 1 Kg",
                    unit = "Kg",
                    unitPrice = 17500.0,
                    costPrice = 14000.0,
                    quantity = 2.5,
                    subtotal = 43750.0
                )
            ),
            subtotal = 43750.0,
            totalAmount = 43750.0,
            paymentMethod = PaymentMethod.CASH.code,
            amountPaid = 50000.0,
            changeAmount = 6250.0
        )
        val completed = repository.completeTransaction(tx).getOrThrow()
        assertEquals(TransactionStatus.COMPLETED.code, completed.status)

        val afterCheckout = repository.observeProducts().first().first { it.id == productId }
        assertEquals(7.5, afterCheckout.stock, 0.001)

        // Add an expense
        repository.saveExpense(
            Expense(
                title = "Beli Plastik Kresek",
                category = "Operasional",
                amount = 15000.0,
                notes = "2 pack"
            )
        ).getOrThrow()

        // Export Backup JSON
        val backupJson = repository.exportBackupJson().getOrThrow()
        val summary = repository.validateBackupJson(backupJson).getOrThrow()
        assertEquals("Warung Berkah Mandiri", summary.storeName)
        assertEquals(1, summary.productCount)
        assertEquals(1, summary.transactionCount)
        assertEquals(1, summary.expenseCount)

        // Cancel transaction & verify stock restored
        repository.cancelOrRefundTransaction(
            transactionId = completed.id,
            targetStatus = TransactionStatus.CANCELLED,
            reason = "Salah input"
        ).getOrThrow()
        val afterCancel = repository.observeProducts().first().first { it.id == productId }
        assertEquals(10.0, afterCancel.stock, 0.001)

        // Restore into a fresh in-memory database and verify full data fidelity
        val freshDb = Room.inMemoryDatabaseBuilder(context, LocalPosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val freshRepo = LocalPosRepository(freshDb)
            val restoredSummary = freshRepo.restoreBackupJson(backupJson).getOrThrow()
            assertEquals("Warung Berkah Mandiri", restoredSummary.storeName)
            assertEquals(1, freshRepo.observeProducts().first().size)
            assertEquals(1, freshRepo.observeTransactions().first().size)
            assertEquals(1, freshRepo.observeExpenses().first().size)
        } finally {
            freshDb.close()
        }
    }

    @Test
    fun activationScreen_autoUppercasesCode_and_validatesInputs() {
        var capturedBusiness = ""
        var capturedCode = ""
        var succeeded = false

        composeRule.setContent {
            AdaKasirTheme(darkTheme = false) {
                ActivationScreen(
                    deviceId = "DEV-TEST-001",
                    onActivate = { business, code ->
                        capturedBusiness = business
                        capturedCode = code
                        Result.success(
                            LicenseActivationResult(
                                licenseCode = code,
                                businessName = business,
                                deviceId = "DEV-TEST-001",
                                activatedAtMillis = System.currentTimeMillis()
                            )
                        )
                    },
                    onActivationSuccess = { succeeded = true }
                )
            }
        }

        // Empty submit shows error banner
        composeRule.onNodeWithTag("activate_now_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("activation_error_banner").assertIsDisplayed()

        // Fill business name and lowercase code -> auto-uppercased
        composeRule.onNodeWithTag("activation_business_name_input").performScrollTo().performTextInput("Toko Maju")
        composeRule.onNodeWithTag("activation_code_input").performScrollTo().performTextInput("adakasir-2026-xyz")
        composeRule.onNodeWithTag("activation_code_input").assertTextContains("ADAKASIR-2026-XYZ")

        // Verify contact admin button exists
        composeRule.onNodeWithTag("contact_admin_button").performScrollTo().assertIsDisplayed()

        // Submit activation
        composeRule.onNodeWithTag("activate_now_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertTrue(succeeded)
        assertEquals("Toko Maju", capturedBusiness)
        assertEquals("ADAKASIR-2026-XYZ", capturedCode)
    }

    @Test
    fun settingsScreen_showsLicenseStatus_and_hasNoLogoutButton() {
        composeRule.setContent {
            AdaKasirTheme(darkTheme = false) {
                SettingsScreen(
                    store = StoreSettings(storeName = "Warung Offline"),
                    localPrefs = LocalAppPrefs(
                        isLicensed = true,
                        licenseCode = "ADAKASIR-2026-001",
                        deviceId = "DEV-99887766",
                        businessName = "Warung Offline"
                    ),
                    isBackupBusy = false,
                    onBackToDashboard = {},
                    onSaveProfile = { _, _, _, _, _, _, _, _, _ -> },
                    onSetDarkTheme = {},
                    onSetLanguage = {},
                    onSelectPrinter = { _, _ -> },
                    onClearPrinter = {},
                    onSetPrinterPaperWidth = {},
                    onGenerateBackupJson = { Result.success("{}") },
                    onValidateBackupJson = {
                        Result.success(BackupSummary("Warung Offline", 0, 0, 0, 0, 0L))
                    },
                    onRestoreBackupJson = {}
                )
            }
        }

        // Scroll to Backup & Restore and Device License Status cards
        composeRule.onNodeWithTag("settings_lazy_column")
            .performScrollToNode(hasTestTag("settings_backup_item"))
        composeRule.onNodeWithTag("settings_backup_item").assertIsDisplayed()

        composeRule.onNodeWithTag("settings_lazy_column")
            .performScrollToNode(hasTestTag("settings_license_status_card"))
        composeRule.onNodeWithTag("settings_license_status_card").assertIsDisplayed()

        composeRule.onNodeWithTag("settings_lazy_column")
            .performScrollToNode(hasTestTag("settings_about_item"))
        composeRule.onNodeWithTag("settings_about_item").assertIsDisplayed()

        // Verify no Logout button exists anywhere in the hierarchy
        composeRule.onNodeWithTag("settings_sign_out_button").assertDoesNotExist()
    }

    @Test
    fun sampleProducts_includeImages_and_kgPackUnits_and_storeBrandingPersists() = runBlocking {
        repository.ensureInitialLocalSetup(
            businessName = "Kopi & Sembako Jaya",
            defaultDarkTheme = false,
            defaultLanguage = "id",
            seedSampleProducts = true,
            seedKgPackSamples = true
        )

        val products = repository.observeProducts().first()
        assertEquals(4, products.size)
        val units = products.map { it.unit }.toSet()
        assertTrue(units.contains("Botol"))
        assertTrue(units.contains("Pcs"))
        assertTrue(units.contains("Kg"))
        assertTrue(units.contains("Pack"))
        products.forEach { prod ->
            assertTrue("Expected drawable photo for ${prod.name}", prod.photoData.startsWith("drawable:"))
            assertTrue("Expected drawable res for ${prod.name}", resolveBundledProductDrawable(prod.photoData) != null)
        }

        // Save custom store icon, background color, and receipt prefix
        repository.saveStoreProfile(
            storeName = "Kopi & Sembako Jaya",
            ownerName = "Rendy",
            phone = "08123456789",
            address = "Jl. Merdeka No. 1",
            photoData = "",
            storeIcon = "coffee",
            storeIconBgColor = "#059669",
            receiptPrefix = "wrng-01!",
            defaultTaxPercent = 11.0
        ).getOrThrow()

        val savedSettings = repository.observeSettings().first()
        assertEquals("coffee", savedSettings.storeIcon)
        assertEquals("#059669", savedSettings.storeIconBgColor)
        assertEquals("WRNG01", savedSettings.receiptPrefix)

        val generatedTx = PosFormatters.generateTransactionNumber(prefix = savedSettings.receiptPrefix)
        assertTrue("Expected tx number to start with WRNG01-, got $generatedTx", generatedTx.startsWith("WRNG01-"))

        // Verify backup & restore preserves storeIcon, storeIconBgColor, and receiptPrefix
        val json = repository.exportBackupJson().getOrThrow()
        val freshDb = Room.inMemoryDatabaseBuilder(context, LocalPosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val freshRepo = LocalPosRepository(freshDb)
            freshRepo.restoreBackupJson(json).getOrThrow()
            val restored = freshRepo.observeSettings().first()
            assertEquals("coffee", restored.storeIcon)
            assertEquals("#059669", restored.storeIconBgColor)
            assertEquals("WRNG01", restored.receiptPrefix)
        } finally {
            freshDb.close()
        }
    }

    @Test
    fun backupStatusHelper_evaluatesGreenYellowRedStatesCorrectly() {
        val cal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 21, 14, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis
        val oneHourAgo = now - (60L * 60L * 1000L)
        val twentySixHoursAgo = now - (26L * 60L * 60L * 1000L)
        val fourDaysAgo = now - (4L * 24L * 60L * 60L * 1000L)

        // Never backed up -> Red
        assertEquals(
            BackupHealthStatus.NEVER_OR_OVERDUE,
            BackupStatusHelper.evaluateStatus(0L, hasNewDataSinceBackup = false, nowMillis = now)
        )
        // More than 3 days ago -> Red
        assertEquals(
            BackupHealthStatus.NEVER_OR_OVERDUE,
            BackupStatusHelper.evaluateStatus(fourDaysAgo, hasNewDataSinceBackup = false, nowMillis = now)
        )
        // Over 24h (<= 3 days) -> Yellow
        assertEquals(
            BackupHealthStatus.NEEDS_BACKUP,
            BackupStatusHelper.evaluateStatus(twentySixHoursAgo, hasNewDataSinceBackup = false, nowMillis = now)
        )
        // Backed up today, but new data/transactions added -> Yellow
        assertEquals(
            BackupHealthStatus.NEEDS_BACKUP,
            BackupStatusHelper.evaluateStatus(oneHourAgo, hasNewDataSinceBackup = true, nowMillis = now)
        )
        // Backed up today and no new data -> Green
        assertEquals(
            BackupHealthStatus.SAFE,
            BackupStatusHelper.evaluateStatus(oneHourAgo, hasNewDataSinceBackup = false, nowMillis = now)
        )

        // Relative elapsed time strings
        assertEquals(
            "baru saja",
            BackupStatusHelper.formatRelativeElapsed(now - 20_000L, language = "id", nowMillis = now)
        )
        assertEquals(
            "15 menit yang lalu",
            BackupStatusHelper.formatRelativeElapsed(now - 15L * 60_000L, language = "id", nowMillis = now)
        )
        assertEquals(
            "3 jam yang lalu",
            BackupStatusHelper.formatRelativeElapsed(now - 3L * 3_600_000L, language = "id", nowMillis = now)
        )
        assertEquals(
            "4 hari yang lalu",
            BackupStatusHelper.formatRelativeElapsed(fourDaysAgo, language = "id", nowMillis = now)
        )
    }

    @Test
    fun headerBackupStatusShield_rendersIconWithPulseDot_and_opensStatusModalOnClick() {
        var openSettingsClicked = false

        composeRule.setContent {
            AdaKasirTheme(darkTheme = false) {
                var showDialog by remember { mutableStateOf(false) }
                BackupStatusHeaderPill(
                    status = BackupHealthStatus.NEEDS_BACKUP,
                    onClick = { showDialog = true }
                )
                if (showDialog) {
                    QuickBackupStatusDialog(
                        status = BackupHealthStatus.NEEDS_BACKUP,
                        lastBackupAtMillis = System.currentTimeMillis() - (2L * 3_600_000L),
                        hasNewDataSinceBackup = true,
                        language = "id",
                        isBackupBusy = false,
                        onOpenBackupSettings = { openSettingsClicked = true },
                        onDismiss = { showDialog = false }
                    )
                }
            }
        }

        // Compact icon-only shield shows pulsing alert dot for yellow status
        composeRule.onNodeWithTag("header_backup_status_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("header_backup_status_alert_dot", useUnmergedTree = true).assertIsDisplayed()

        // Tap shield -> clean center modal appears with title, relative timestamp, and 2 actions
        composeRule.onNodeWithTag("header_backup_status_badge").performClick()

        composeRule.onNodeWithTag("quick_backup_status_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_backup_status_title")
            .assertTextContains("Ada Data Baru Belum Di-backup")
        composeRule.onNodeWithTag("quick_backup_last_time_text")
            .assertTextContains("Terakhir backup:", substring = true)
            .assertTextContains("jam yang lalu", substring = true)
        composeRule.onNodeWithTag("quick_backup_close_button").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_open_backup_settings_button")
            .assertIsDisplayed()
            .assertTextContains("Ke Menu Backup Sekarang")
            .performClick()
        assertTrue(openSettingsClicked)
    }

    @Test
    fun headerBackupStatusShield_safeHasNoPulseDot_and_neverBackedUpShowsClearMessage() {
        composeRule.setContent {
            AdaKasirTheme(darkTheme = false) {
                BackupStatusHeaderPill(
                    status = BackupHealthStatus.SAFE,
                    onClick = {}
                )
                QuickBackupStatusDialog(
                    status = BackupHealthStatus.NEVER_OR_OVERDUE,
                    lastBackupAtMillis = 0L,
                    hasNewDataSinceBackup = false,
                    language = "id",
                    onOpenBackupSettings = {},
                    onDismiss = {}
                )
            }
        }

        // Safe green shield has no alert dot
        composeRule.onNodeWithTag("header_backup_status_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("header_backup_status_alert_dot", useUnmergedTree = true).assertDoesNotExist()

        // Never-backed-up modal displays exact clear guidance
        composeRule.onNodeWithTag("quick_backup_status_title")
            .assertTextContains("Data Belum Di-backup")
        composeRule.onNodeWithTag("quick_backup_last_time_text")
            .assertTextContains("Anda belum pernah melakukan backup data sejak aplikasi ini digunakan.")
    }
}
