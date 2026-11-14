package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper
import java.util.UUID
import kotlin.math.abs

@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
class AdaKasirAuthAndMenuRuleTest : FirestoreEmulatorTestBase() {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    override fun setUpFirebase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        databaseId = context.getString(R.string.firestore_database_id)

        val existing = FirebaseApp.getApps(context).firstOrNull { it.name == TEST_APP_NAME }
        val app = existing ?: run {
            val created = FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.adakasir.kxpvqm")
                    .setProjectId("demo-no-project")
                    .setApiKey("fake-api-key-for-emulator")
                    .build(),
                TEST_APP_NAME
            )
            val newFirestore = FirebaseFirestore.getInstance(created, databaseId)
            newFirestore.useEmulator("127.0.0.1", 8085)
            newFirestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()

            val newAuth = FirebaseAuth.getInstance(created)
            newAuth.useEmulator("127.0.0.1", 9099)
            created
        }

        firestore = FirebaseFirestore.getInstance(app, databaseId)
        auth = FirebaseAuth.getInstance(app)
    }

    private fun signInBlockingWithPump(email: String) {
        var done = false
        var err: Throwable? = null
        CoroutineScope(Dispatchers.IO).launch {
            try {
                signInTestUser(email)
            } catch (t: Throwable) {
                err = t
            } finally {
                done = true
            }
        }
        composeRule.waitUntil(timeoutMillis = 15_000) {
            ShadowLooper.idleMainLooper()
            done
        }
        err?.let { throw it }
    }

    @Test
    fun appOpen_noSession_showsLogin_handlesLoadingAndError_thenEntersDashboardWithSixMenus() {
        auth.signOut()

        val triggerErrorNext = true
        var pendingOnSuccess: (() -> Unit)? = null
        var pendingOnError: ((String) -> Unit)? = null

        composeRule.setContent {
            AdaKasirRoot(
                firebaseAuth = auth,
                firestoreProvider = { firestore },
                splashDurationMs = 0L,
                onSignInClickOverride = { onSuccess, onError, _ ->
                    pendingOnSuccess = onSuccess
                    pendingOnError = onError
                },
                onSignOutOverride = { onComplete ->
                    auth.signOut()
                    onComplete()
                }
            )
        }

        composeRule.waitForIdle()

        // 1. Login screen always shown on startup; no auto-login
        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("dashboard_menu_grid").fetchSemanticsNodes().size)

        // 2. Click Masuk dengan Google -> loading indicator must appear
        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("auth_loading_indicator", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Memproses masuk...").assertIsDisplayed()

        // 3. Simulate sign-in error -> error banner must appear and stay on login screen
        if (triggerErrorNext) {
            composeRule.runOnUiThread {
                pendingOnError?.invoke("Koneksi gagal saat masuk")
            }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("auth_error_banner").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithText("Koneksi gagal saat masuk").assertIsDisplayed()
            assertEquals(0, composeRule.onAllNodesWithTag("dashboard_menu_grid").fetchSemanticsNodes().size)
        }

        // 4. Retry login and succeed -> lands directly on Dashboard with Today Summary + 6 Menu Grid + Chart
        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        signInBlockingWithPump("cuj_${UUID.randomUUID()}@test.com")
        composeRule.runOnUiThread {
            pendingOnSuccess?.invoke()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("blue_dashboard_header").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_today_sales_card").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_today_tx_card").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_supermarket_bg").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_menu_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("menu_cashier").assertIsDisplayed()
        composeRule.onNodeWithTag("menu_products").assertIsDisplayed()
        composeRule.onNodeWithTag("menu_sales_report").assertIsDisplayed()
        composeRule.onNodeWithTag("menu_expenses").assertIsDisplayed()
        composeRule.onNodeWithTag("menu_history").assertIsDisplayed()
        composeRule.onNodeWithTag("menu_settings").assertIsDisplayed()
    }

    @Test
    fun persistedSession_reopenShowsLogin_dashboardMenuNavigation_salesReport_expenses_settingsPrinter_andLogout() {
        val email = "persisted_${UUID.randomUUID()}@test.com"
        signInBlockingWithPump(email)

        var launchEpoch by mutableStateOf(1)

        composeRule.setContent {
            // Re-keying simulates reopening the app in a fresh session while Firebase Auth persists currentUser
            androidx.compose.runtime.key(launchEpoch) {
                AdaKasirRoot(
                    firebaseAuth = auth,
                    firestoreProvider = { firestore },
                    splashDurationMs = 0L,
                    onSignInClickOverride = { onSuccess, _, _ ->
                        onSuccess()
                    },
                    onSignOutOverride = { onComplete ->
                        auth.signOut()
                        onComplete()
                    }
                )
            }
        }

        composeRule.waitForIdle()

        // Even though auth.currentUser is ALREADY persisted, login page MUST be shown on open!
        composeRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
        composeRule.onNodeWithTag("saved_session_notice").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("dashboard_menu_grid").fetchSemanticsNodes().size)

        // User explicitly clicks Masuk dengan Google -> enters Dashboard directly
        composeRule.onNodeWithTag("google_sign_in_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("blue_dashboard_header").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_today_sales_card").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_supermarket_bg").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_menu_grid").assertIsDisplayed()

        // Verify the 6 menu tiles are neatly arranged (Row 1: Kasir, Produk, Laporan; Row 2: Pengeluaran, Riwayat, Pengaturan)
        val bCash = composeRule.onNodeWithTag("menu_cashier").getUnclippedBoundsInRoot()
        val bProd = composeRule.onNodeWithTag("menu_products").getUnclippedBoundsInRoot()
        val bRep = composeRule.onNodeWithTag("menu_sales_report").getUnclippedBoundsInRoot()
        val bExp = composeRule.onNodeWithTag("menu_expenses").getUnclippedBoundsInRoot()
        val bHist = composeRule.onNodeWithTag("menu_history").getUnclippedBoundsInRoot()
        val bSett = composeRule.onNodeWithTag("menu_settings").getUnclippedBoundsInRoot()
        assertTrue(abs(bCash.top.value - bProd.top.value) < 2f)
        assertTrue(abs(bProd.top.value - bRep.top.value) < 2f)
        assertTrue(bExp.top.value > bCash.bottom.value)
        assertTrue(abs(bExp.top.value - bHist.top.value) < 2f)
        assertTrue(abs(bHist.top.value - bSett.top.value) < 2f)

        // 1. Kasir & back to Dashboard
        composeRule.onNodeWithTag("menu_cashier").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("blue_subscreen_header").assertIsDisplayed()
        composeRule.onNodeWithTag("cashier_search_input").assertIsDisplayed()
        composeRule.onNodeWithTag("back_to_dashboard_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard_menu_grid").assertIsDisplayed()

        // 2. Produk (top full-width search + bottom-right non-overlapping box button) & back to Dashboard
        composeRule.onNodeWithTag("menu_products").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("blue_subscreen_header").assertIsDisplayed()
        composeRule.onNodeWithTag("products_search_input").assertIsDisplayed()
        composeRule.onNodeWithTag("products_add_button").assertIsDisplayed()
        val searchBounds = composeRule.onNodeWithTag("products_search_input").getUnclippedBoundsInRoot()
        val addProdBounds = composeRule.onNodeWithTag("products_add_button").getUnclippedBoundsInRoot()
        assertTrue(addProdBounds.top.value > searchBounds.bottom.value)
        assertTrue(addProdBounds.left.value > 150f)
        composeRule.onNodeWithTag("back_to_dashboard_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard_menu_grid").assertIsDisplayed()

        // 3. Laporan Penjualan (Tanggal/Bulan/Tahun + Omset + HPP + Laba Kotor) & back
        composeRule.onNodeWithTag("menu_sales_report").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("blue_subscreen_header").assertIsDisplayed()
        composeRule.onNodeWithTag("report_filter_date").assertIsDisplayed()
        composeRule.onNodeWithTag("report_filter_month").assertIsDisplayed()
        composeRule.onNodeWithTag("report_filter_year").assertIsDisplayed()
        composeRule.onNodeWithTag("report_total_sales_card").assertIsDisplayed()
        composeRule.onNodeWithTag("report_omset_card").assertIsDisplayed()
        composeRule.onNodeWithTag("report_hpp_card").assertIsDisplayed()
        composeRule.onNodeWithTag("report_gross_profit_card").assertIsDisplayed()
        composeRule.onNodeWithTag("report_filter_month").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("report_filter_year").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("back_to_dashboard_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard_menu_grid").assertIsDisplayed()

        // 4. Pengeluaran & back
        composeRule.onNodeWithTag("menu_expenses").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("blue_subscreen_header").assertIsDisplayed()
        composeRule.onNodeWithTag("expenses_add_button").assertIsDisplayed()
        composeRule.onNodeWithTag("back_to_dashboard_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard_menu_grid").assertIsDisplayed()

        // 5. Riwayat & back
        composeRule.onNodeWithTag("menu_history").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("blue_subscreen_header").assertIsDisplayed()
        composeRule.onNodeWithText("Cari nomor transaksi atau pelanggan...").assertIsDisplayed()
        composeRule.onNodeWithTag("back_to_dashboard_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard_menu_grid").assertIsDisplayed()

        // 6. Pengaturan (Printer, Kustomisasi Struk, Kelola Pengguna + (i), & Keluar Akun)
        composeRule.onNodeWithTag("menu_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("blue_subscreen_header").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_store_profile_item").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_appearance_item").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_user_management_item").assertIsDisplayed()
        composeRule.onNodeWithTag("user_mode_info_button").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_printer_item").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_receipt_customization_item").assertIsDisplayed()

        // Open (i) info dialog
        composeRule.onNodeWithTag("user_mode_info_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("user_mode_info_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Tutup").performClick()
        composeRule.waitForIdle()

        // Open Kelola Pengguna dialog
        composeRule.onNodeWithTag("settings_user_management_item").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("user_management_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("mode_chip_cashier").assertIsDisplayed()
        composeRule.onNodeWithTag("mode_chip_monitor").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tutup").performClick()
        composeRule.waitForIdle()

        // Open Kustomisasi Struk dialog
        composeRule.onNodeWithTag("settings_receipt_customization_item").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("receipt_customization_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("receipt_custom_preview").assertIsDisplayed()
        composeRule.onNodeWithTag("switch_receipt_logo").assertIsDisplayed()
        composeRule.onNodeWithTag("receipt_footer_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tutup").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Open Printer Kasir dialog
        composeRule.onNodeWithTag("settings_printer_item").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("printer_settings_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("refresh_printers_button").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tutup").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_lazy_column").performScrollToNode(hasTestTag("settings_sign_out_button"))
        composeRule.onNodeWithTag("settings_sign_out_button").performClick()
        composeRule.waitForIdle()

        assertNull(auth.currentUser)
        composeRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()

        // Sign in again, then simulate reopening app -> must show login page again
        signInBlockingWithPump(email)
        composeRule.runOnUiThread {
            launchEpoch += 1
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("dashboard_menu_grid").fetchSemanticsNodes().size)
    }

    @Test
    fun appOpen_showsSplashScreenWithLogoAndByRaflineDev_thenTransitionsToLogin() {
        auth.signOut()

        composeRule.setContent {
            AdaKasirRoot(
                firebaseAuth = auth,
                firestoreProvider = { firestore },
                splashDurationMs = 500L
            )
        }

        composeRule.onNodeWithTag("splash_screen").assertIsDisplayed()
        composeRule.onNodeWithText("ADA KASIR").assertIsDisplayed()
        composeRule.onNodeWithText("by Rafline Dev").assertIsDisplayed()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("google_sign_in_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("dashboard_menu_grid").fetchSemanticsNodes().size)
    }

    @Test
    fun adaKasirTheme_locksFontScaleToOneRegardlessOfSystemFontScale() {
        var measuredFontScale = -1f
        composeRule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                    density = 2.0f,
                    fontScale = 1.9f
                )
            ) {
                com.example.ui.theme.AdaKasirTheme {
                    measuredFontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
                }
            }
        }
        composeRule.waitForIdle()
        assertEquals(1.0f, measuredFontScale, 0.0001f)
    }

    @Test
    fun settingsScreen_dialogsOpenAndCloseCleanly() {
        composeRule.setContent {
            com.example.ui.theme.AdaKasirTheme {
                com.example.ui.screens.SettingsScreen(
                    store = com.example.data.model.StoreSettings(storeName = "Toko Uji"),
                    localPrefs = com.example.data.prefs.LocalAppPrefs(),
                    userEmail = "kasir@test.com",
                    isBackupBusy = false,
                    onBackToDashboard = {},
                    onSaveProfile = { _, _, _, _, _, _ -> },
                    onSetDarkTheme = {},
                    onSetLanguage = {},
                    onSelectPrinter = { _, _ -> },
                    onClearPrinter = {},
                    onSetPrinterPaperWidth = {},
                    onGenerateBackupJson = { Result.success("{}") },
                    onValidateBackupJson = { Result.success(com.example.data.firebase.BackupSummary("Toko Uji", 0, 0, 0, 0L)) },
                    onRestoreBackupJson = {},
                    onSignOut = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_printer_item").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("printer_settings_dialog").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tutup").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_receipt_customization_item").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("receipt_customization_dialog").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tutup").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("user_mode_info_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("user_mode_info_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Tutup").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_user_management_item").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("user_management_dialog").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tutup").performClick()
        composeRule.waitForIdle()
    }

    private companion object {
        const val TEST_APP_NAME = "test-emulator-app"
    }
}
