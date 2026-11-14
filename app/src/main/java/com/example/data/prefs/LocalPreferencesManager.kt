package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.adaKasirDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ada_kasir_prefs"
)

data class LocalAppPrefs(
    val darkTheme: Boolean = false,
    val language: String = "id",
    val defaultPrinterName: String = "",
    val defaultPrinterAddress: String = "",
    val printerPaperWidthMm: Int = 58,
    val accessMode: String = "CASHIER",
    val monitorTargetUserId: String = "",
    val monitorTargetEmail: String = "",
    val monitorTargetStoreName: String = ""
)

class LocalPreferencesManager(private val context: Context) {
    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val LANGUAGE = stringPreferencesKey("language")
        val PRINTER_NAME = stringPreferencesKey("printer_name")
        val PRINTER_ADDRESS = stringPreferencesKey("printer_address")
        val PRINTER_PAPER_MM = intPreferencesKey("printer_paper_mm")
        val ACCESS_MODE = stringPreferencesKey("access_mode")
        val MONITOR_TARGET_UID = stringPreferencesKey("monitor_target_uid")
        val MONITOR_TARGET_EMAIL = stringPreferencesKey("monitor_target_email")
        val MONITOR_TARGET_STORE = stringPreferencesKey("monitor_target_store")
    }

    val prefsFlow: Flow<LocalAppPrefs> = context.adaKasirDataStore.data.map { prefs ->
        LocalAppPrefs(
            darkTheme = prefs[Keys.DARK_THEME] ?: false,
            language = prefs[Keys.LANGUAGE] ?: "id",
            defaultPrinterName = prefs[Keys.PRINTER_NAME] ?: "",
            defaultPrinterAddress = prefs[Keys.PRINTER_ADDRESS] ?: "",
            printerPaperWidthMm = if ((prefs[Keys.PRINTER_PAPER_MM] ?: 58) == 80) 80 else 58,
            accessMode = if (prefs[Keys.ACCESS_MODE] == "MONITOR") "MONITOR" else "CASHIER",
            monitorTargetUserId = prefs[Keys.MONITOR_TARGET_UID] ?: "",
            monitorTargetEmail = prefs[Keys.MONITOR_TARGET_EMAIL] ?: "",
            monitorTargetStoreName = prefs[Keys.MONITOR_TARGET_STORE] ?: ""
        )
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.adaKasirDataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = enabled
        }
    }

    suspend fun setLanguage(language: String) {
        context.adaKasirDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = if (language == "en") "en" else "id"
        }
    }

    suspend fun setDefaultPrinter(name: String, address: String) {
        context.adaKasirDataStore.edit { prefs ->
            prefs[Keys.PRINTER_NAME] = name.trim()
            prefs[Keys.PRINTER_ADDRESS] = address.trim()
        }
    }

    suspend fun clearDefaultPrinter() {
        context.adaKasirDataStore.edit { prefs ->
            prefs.remove(Keys.PRINTER_NAME)
            prefs.remove(Keys.PRINTER_ADDRESS)
        }
    }

    suspend fun setPrinterPaperWidth(mm: Int) {
        context.adaKasirDataStore.edit { prefs ->
            prefs[Keys.PRINTER_PAPER_MM] = if (mm == 80) 80 else 58
        }
    }

    suspend fun setAccessMode(mode: String) {
        context.adaKasirDataStore.edit { prefs ->
            prefs[Keys.ACCESS_MODE] = if (mode == "MONITOR") "MONITOR" else "CASHIER"
        }
    }

    suspend fun setMonitorTarget(userId: String, email: String, storeName: String) {
        context.adaKasirDataStore.edit { prefs ->
            prefs[Keys.ACCESS_MODE] = "MONITOR"
            prefs[Keys.MONITOR_TARGET_UID] = userId.trim()
            prefs[Keys.MONITOR_TARGET_EMAIL] = email.trim().lowercase()
            prefs[Keys.MONITOR_TARGET_STORE] = storeName.trim()
        }
    }

    suspend fun clearMonitorTarget() {
        context.adaKasirDataStore.edit { prefs ->
            prefs.remove(Keys.MONITOR_TARGET_UID)
            prefs.remove(Keys.MONITOR_TARGET_EMAIL)
            prefs.remove(Keys.MONITOR_TARGET_STORE)
        }
    }
}
