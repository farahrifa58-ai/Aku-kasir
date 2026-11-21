package com.example.data.prefs

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.adaKasirDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ada_kasir_prefs"
)

data class LocalAppPrefs(
    val isLicensed: Boolean = false,
    val licenseCode: String = "",
    val deviceId: String = "",
    val businessName: String = "",
    val activatedAtMillis: Long = 0L,
    val darkTheme: Boolean = false,
    val language: String = "id",
    val defaultPrinterName: String = "",
    val defaultPrinterAddress: String = "",
    val printerPaperWidthMm: Int = 58,
    val lastBackupAt: Long = 0L,
    val hasNewDataSinceBackup: Boolean = false
)

class LocalPreferencesManager(private val context: Context) {
    private object Keys {
        val IS_LICENSED = booleanPreferencesKey("is_licensed")
        val LICENSE_CODE = stringPreferencesKey("license_code")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val BUSINESS_NAME = stringPreferencesKey("business_name")
        val ACTIVATED_AT_MILLIS = longPreferencesKey("activated_at_millis")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val LANGUAGE = stringPreferencesKey("language")
        val PRINTER_NAME = stringPreferencesKey("printer_name")
        val PRINTER_ADDRESS = stringPreferencesKey("printer_address")
        val PRINTER_PAPER_MM = intPreferencesKey("printer_paper_mm")
        val SAMPLE_SEEDED = booleanPreferencesKey("sample_products_seeded_local_v1")
        val SAMPLE_KG_PACK_SEEDED = booleanPreferencesKey("sample_kg_pack_seeded_local_v2")
        val LAST_BACKUP_AT = longPreferencesKey("lastBackupAt")
        val HAS_NEW_DATA_SINCE_BACKUP = booleanPreferencesKey("hasNewDataSinceBackup")
    }

    val prefsFlow: Flow<LocalAppPrefs> = context.adaKasirDataStore.data.map { prefs ->
        LocalAppPrefs(
            isLicensed = prefs[Keys.IS_LICENSED] ?: false,
            licenseCode = prefs[Keys.LICENSE_CODE] ?: "",
            deviceId = prefs[Keys.DEVICE_ID] ?: "",
            businessName = prefs[Keys.BUSINESS_NAME] ?: "",
            activatedAtMillis = prefs[Keys.ACTIVATED_AT_MILLIS] ?: 0L,
            darkTheme = prefs[Keys.DARK_THEME] ?: false,
            language = prefs[Keys.LANGUAGE] ?: "id",
            defaultPrinterName = prefs[Keys.PRINTER_NAME] ?: "",
            defaultPrinterAddress = prefs[Keys.PRINTER_ADDRESS] ?: "",
            printerPaperWidthMm = if ((prefs[Keys.PRINTER_PAPER_MM] ?: 58) == 80) 80 else 58,
            lastBackupAt = prefs[Keys.LAST_BACKUP_AT] ?: 0L,
            hasNewDataSinceBackup = prefs[Keys.HAS_NEW_DATA_SINCE_BACKUP] ?: false
        )
    }

    /**
     * Returns a permanent unique device ID (using Android Secure ANDROID_ID when available,
     * falling back to a generated UUID persisted permanently in DataStore).
     */
    @SuppressLint("HardwareIds")
    suspend fun getOrCreatePersistentDeviceId(): String {
        val existing = context.adaKasirDataStore.data.first()[Keys.DEVICE_ID]
        if (!existing.isNullOrBlank() && existing.length >= 4) {
            return existing
        }
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (_: Exception) {
            null
        }
        val resolved = if (
            !androidId.isNullOrBlank() &&
            androidId != "9774d56d682e549c" &&
            androidId.length >= 4
        ) {
            "AND-${androidId.uppercase()}"
        } else {
            "DEV-${UUID.randomUUID().toString().uppercase()}"
        }
        context.adaKasirDataStore.edit { prefs ->
            prefs[Keys.DEVICE_ID] = resolved
        }
        return resolved
    }

    /**
     * Permanently saves license activation state locally so daily usage works 100% offline.
     */
    suspend fun saveLicenseActivation(
        licenseCode: String,
        deviceId: String,
        businessName: String,
        activatedAtMillis: Long = System.currentTimeMillis()
    ) {
        context.adaKasirDataStore.edit { prefs ->
            prefs[Keys.IS_LICENSED] = true
            prefs[Keys.LICENSE_CODE] = licenseCode.trim().uppercase()
            prefs[Keys.DEVICE_ID] = deviceId.trim()
            prefs[Keys.BUSINESS_NAME] = businessName.trim()
            prefs[Keys.ACTIVATED_AT_MILLIS] = activatedAtMillis
        }
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

    suspend fun hasSeededSampleProducts(): Boolean {
        return context.adaKasirDataStore.data.first()[Keys.SAMPLE_SEEDED] ?: false
    }

    suspend fun markSampleProductsSeeded() {
        context.adaKasirDataStore.edit { prefs ->
            prefs[Keys.SAMPLE_SEEDED] = true
        }
    }

    suspend fun hasSeededKgPackSamples(): Boolean {
        return context.adaKasirDataStore.data.first()[Keys.SAMPLE_KG_PACK_SEEDED] ?: false
    }

    suspend fun markKgPackSamplesSeeded() {
        context.adaKasirDataStore.edit { prefs ->
            prefs[Keys.SAMPLE_KG_PACK_SEEDED] = true
        }
    }

    suspend fun markNewDataSinceBackup() {
        context.adaKasirDataStore.edit { prefs ->
            prefs[Keys.HAS_NEW_DATA_SINCE_BACKUP] = true
        }
    }

    suspend fun markBackupCompleted(timestampMillis: Long = System.currentTimeMillis()) {
        context.adaKasirDataStore.edit { prefs ->
            prefs[Keys.LAST_BACKUP_AT] = timestampMillis
            prefs[Keys.HAS_NEW_DATA_SINCE_BACKUP] = false
        }
    }
}
