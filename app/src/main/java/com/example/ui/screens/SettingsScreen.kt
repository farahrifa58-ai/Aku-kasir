package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.BackupSummary
import com.example.data.model.PaymentMethod
import com.example.data.model.PosTransaction
import com.example.data.model.StoreSettings
import com.example.data.model.TransactionItem
import com.example.data.prefs.LocalAppPrefs
import com.example.ui.activation.openAdminPurchaseEmail
import com.example.ui.components.PhotoPickerSheetDialog
import com.example.ui.components.StoreBrandingPresets
import com.example.ui.components.StoreProfileAvatar
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.util.BackupStatusHelper
import com.example.util.ImageUtils
import com.example.util.PairedPrinter
import com.example.util.PaperWidth
import com.example.util.PosFormatters
import com.example.util.ReceiptHelper
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    store: StoreSettings,
    localPrefs: LocalAppPrefs,
    isBackupBusy: Boolean,
    onBackToDashboard: () -> Unit,
    onSaveProfile: (String, String, String, String, String, String, String, String, Double) -> Unit,
    onSaveReceiptCustomization: (Boolean, Boolean, Boolean, Boolean, Boolean, String) -> Unit = { _, _, _, _, _, _ -> },
    onSetDarkTheme: (Boolean) -> Unit,
    onSetLanguage: (String) -> Unit,
    onSelectPrinter: (String, String) -> Unit,
    onClearPrinter: () -> Unit,
    onSetPrinterPaperWidth: (Int) -> Unit,
    onGenerateBackupJson: suspend () -> Result<String>,
    onValidateBackupJson: (String) -> Result<BackupSummary>,
    onRestoreBackupJson: (String) -> Unit,
    openBackupDialogInitially: Boolean = false,
    onConsumeOpenBackupDialog: () -> Unit = {},
    onBackupExportSucceeded: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()

    var showProfileDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showPrinterDialog by remember { mutableStateOf(false) }
    var showReceiptCustomDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPhotoPicker by remember { mutableStateOf(false) }

    LaunchedEffect(openBackupDialogInitially) {
        if (openBackupDialogInitially) {
            showBackupDialog = true
            onConsumeOpenBackupDialog()
        }
    }

    var storeName by remember(store.storeName) { mutableStateOf(store.storeName) }
    var ownerName by remember(store.ownerName) { mutableStateOf(store.ownerName) }
    var phone by remember(store.phone) { mutableStateOf(store.phone) }
    var address by remember(store.address) { mutableStateOf(store.address) }
    var photoData by remember(store.photoData) { mutableStateOf(store.photoData) }
    var storeIcon by remember(store.storeIcon) { mutableStateOf(store.storeIcon) }
    var storeIconBgColor by remember(store.storeIconBgColor) { mutableStateOf(store.storeIconBgColor) }
    var receiptPrefix by remember(store.receiptPrefix) { mutableStateOf(store.receiptPrefix) }
    var taxPercentInput by remember(store.defaultTaxPercent) {
        mutableStateOf(PosFormatters.formatQuantity(store.defaultTaxPercent))
    }

    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var pendingRestoreSummary by remember { mutableStateOf<BackupSummary?>(null) }

    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val res = onGenerateBackupJson()
                res.onSuccess { json ->
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        }
                        onBackupExportSucceeded()
                        Toast.makeText(context, strings.backupSuccessMsg, Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, e.localizedMessage ?: "Export error", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { err ->
                    Toast.makeText(context, err.localizedMessage ?: "Backup failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val restoreImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                val validation = onValidateBackupJson(json)
                validation.onSuccess { summary ->
                    pendingRestoreJson = json
                    pendingRestoreSummary = summary
                }.onFailure {
                    Toast.makeText(context, strings.restoreInvalidMsg, Toast.LENGTH_LONG).show()
                }
            } catch (_: Exception) {
                Toast.makeText(context, strings.restoreInvalidMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    val effectiveLastBackupAt = BackupStatusHelper.resolveEffectiveLastBackupAt(
        prefsLastBackupAt = localPrefs.lastBackupAt,
        storeLastBackupAtMillis = store.lastBackupAt?.toDate()?.time
    )
    val lastBackupStr = if (effectiveLastBackupAt > 0L) {
        PosFormatters.formatDateTime(effectiveLastBackupAt, store.language)
    } else {
        strings.neverBackedUp
    }

    val printerSummarySubtitle = if (localPrefs.defaultPrinterName.isNotBlank()) {
        "${localPrefs.defaultPrinterName} • ${localPrefs.printerPaperWidthMm} mm"
    } else {
        "${strings.printerSettingsSubtitle} (${localPrefs.printerPaperWidthMm} mm)"
    }

    Column(modifier = modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = strings.menuSettings,
            onBackClick = onBackToDashboard,
            centeredTitle = true
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 820.dp)
                    .fillMaxSize()
                    .testTag("settings_lazy_column"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // SECTION 1: Konfigurasi Toko & POS
                item {
                    SettingsSectionHeader(strings.settingsConfigSection)
                }
                item {
                    SettingsMenuCard(
                        icon = StoreBrandingPresets.resolveIcon(store.storeIcon),
                        iconTint = StoreBrandingPresets.parseHexColor(store.storeIconBgColor, BrandBlue),
                        iconBg = StoreBrandingPresets.parseHexColor(store.storeIconBgColor, BrandBlue).copy(alpha = 0.14f),
                        title = strings.storeProfileSection,
                        subtitle = "${store.storeName.ifBlank { strings.storeProfileSubtitle }} • Nota: ${PosFormatters.sanitizeReceiptPrefix(store.receiptPrefix)}",
                        onClick = { showProfileDialog = true },
                        modifier = Modifier.testTag("settings_store_profile_item")
                    )
                }
                item {
                    val themeLabel = if (store.darkTheme) strings.darkTheme else strings.lightTheme
                    val langLabel = if (store.language == "en") strings.languageEnglish else strings.languageIndonesian
                    SettingsMenuCard(
                        icon = Icons.Rounded.Palette,
                        iconTint = Color(0xFF059669),
                        iconBg = Color(0xFFD1FAE5),
                        title = strings.appearanceLanguageTitle,
                        subtitle = "$themeLabel • $langLabel",
                        onClick = { showAppearanceDialog = true },
                        modifier = Modifier.testTag("settings_appearance_item")
                    )
                }

                // SECTION 2: Hardware & Struk
                item {
                    Spacer(Modifier.height(4.dp))
                    SettingsSectionHeader(strings.settingsHardwareSection)
                }
                item {
                    SettingsMenuCard(
                        icon = Icons.Rounded.Print,
                        iconTint = Color(0xFF4F46E5),
                        iconBg = Color(0xFFE0E7FF),
                        title = strings.printerSettingsTitle,
                        subtitle = printerSummarySubtitle,
                        onClick = { showPrinterDialog = true },
                        modifier = Modifier.testTag("settings_printer_item")
                    )
                }
                item {
                    SettingsMenuCard(
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                        iconTint = Color(0xFFEA580C),
                        iconBg = Color(0xFFFFEDD5),
                        title = strings.receiptCustomTitle,
                        subtitle = strings.receiptCustomSubtitle,
                        onClick = { showReceiptCustomDialog = true },
                        modifier = Modifier.testTag("settings_receipt_customization_item")
                    )
                }

                // SECTION 3: Data & Cadangan
                item {
                    Spacer(Modifier.height(4.dp))
                    SettingsSectionHeader(strings.settingsDataSection)
                }
                item {
                    SettingsMenuCard(
                        icon = Icons.Rounded.Backup,
                        iconTint = Color(0xFFD97706),
                        iconBg = Color(0xFFFEF3C7),
                        title = strings.backupRestoreSection,
                        subtitle = "${strings.lastBackupLabel}: $lastBackupStr",
                        onClick = { showBackupDialog = true },
                        modifier = Modifier.testTag("settings_backup_item")
                    )
                }

                // SECTION 4: Lisensi Perangkat & Aplikasi (100% Offline, Tanpa Logout)
                item {
                    Spacer(Modifier.height(4.dp))
                    SettingsSectionHeader(strings.licenseStatusSection)
                }
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_license_status_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BrandGreen.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = strings.licenseStatusTitle,
                                        tint = BrandGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.licenseStatusTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = BrandGreen.copy(alpha = 0.14f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            text = strings.licenseActiveBadge,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.licenseCodeLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = localPrefs.licenseCode.ifBlank { "-" },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.licenseDeviceIdLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = localPrefs.deviceId.ifBlank { "-" },
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (localPrefs.deviceId.isNotBlank()) {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            clipboard?.setPrimaryClip(
                                                ClipData.newPlainText("Device ID", localPrefs.deviceId)
                                            )
                                            Toast.makeText(context, strings.licenseCopiedMsg, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.testTag("settings_copy_device_id_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = strings.licenseDeviceIdLabel,
                                        tint = BrandBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Text(
                                text = strings.licenseOfflineNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedButton(
                                onClick = {
                                    openAdminPurchaseEmail(
                                        context = context,
                                        businessName = store.storeName.ifBlank { localPrefs.businessName },
                                        deviceId = localPrefs.deviceId,
                                        fallbackCopiedMsg = strings.activationEmailCopiedMsg
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_contact_admin_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Email,
                                    contentDescription = null,
                                    tint = BrandBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${strings.contactAdminButton} (${strings.contactAdminSubtitle})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                item {
                    SettingsMenuCard(
                        icon = Icons.Rounded.Info,
                        iconTint = Color(0xFF7C3AED),
                        iconBg = Color(0xFFEDE9FE),
                        title = strings.aboutSection,
                        subtitle = "${strings.offlineStorageBadge} • v1.0.0",
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.testTag("settings_about_item")
                    )
                }
            }
        }
    }

    // --- Dialog 1: Profil Toko / Bisnis (Icon Usaha, Warna Background, & Prefix Nota) ---
    if (showProfileDialog) {
        val selectedBgColorObj = StoreBrandingPresets.parseHexColor(storeIconBgColor, BrandBlue)
        val previewTxNumber = remember(receiptPrefix) {
            PosFormatters.generateTransactionNumber(prefix = receiptPrefix)
        }
        Dialog(
            onDismissRequest = { showProfileDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .safeDrawingPadding()
                    .imePadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .widthIn(max = 520.dp)
                    .heightIn(max = 720.dp)
                    .testTag("store_profile_dialog")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Pinned Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.storeProfileSection,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showProfileDialog = false }) {
                            Icon(Icons.Rounded.Close, contentDescription = strings.close)
                        }
                    }
                    HorizontalDivider()

                    // Scrollable Form Content
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Store Avatar / Icon Preview Card
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = selectedBgColorObj.copy(alpha = 0.08f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, selectedBgColorObj.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StoreProfileAvatar(
                                    photoData = photoData,
                                    storeIcon = storeIcon,
                                    storeIconBgColor = storeIconBgColor,
                                    size = 84.dp,
                                    onClick = { showPhotoPicker = true },
                                    modifier = Modifier.testTag("settings_store_avatar")
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledTonalButton(
                                        onClick = { showPhotoPicker = true },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.heightIn(min = 38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PhotoCamera,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = strings.useCustomPhotoButton,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                    if (photoData.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = { photoData = "" },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .heightIn(min = 38.dp)
                                                .testTag("settings_use_icon_badge_button")
                                        ) {
                                            Text(
                                                text = strings.useIconBadgeButton,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Pilihan Icon Usaha (2 rows x 5 columns)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = strings.storeIconLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            StoreBrandingPresets.iconOptions.chunked(5).forEach { rowIcons ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowIcons.forEach { option ->
                                        val isSelected = storeIcon == option.key && photoData.isBlank()
                                        val isSelectedIcon = storeIcon == option.key
                                        val iconLabel = if (store.language == "en") option.labelEn else option.labelId
                                        Surface(
                                            onClick = {
                                                storeIcon = option.key
                                                photoData = ""
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isSelectedIcon) {
                                                selectedBgColorObj.copy(alpha = 0.14f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelectedIcon) selectedBgColorObj else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .testTag("store_icon_option_${option.key}")
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = option.icon,
                                                    contentDescription = iconLabel,
                                                    tint = if (isSelectedIcon) selectedBgColorObj else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Text(
                                                    text = iconLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelectedIcon) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelectedIcon) selectedBgColorObj else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Pilihan Warna Background Icon (2 rows x 5 circular swatches)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = strings.storeBgColorLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            StoreBrandingPresets.bgColorHexOptions.chunked(5).forEach { rowColors ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    rowColors.forEach { hex ->
                                        val swatchColor = StoreBrandingPresets.parseHexColor(hex, BrandBlue)
                                        val isSelected = storeIconBgColor.equals(hex, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(swatchColor)
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.35f),
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    storeIconBgColor = hex
                                                }
                                                .testTag("store_color_option_${hex.removePrefix("#")}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = hex,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        OutlinedTextField(
                            value = storeName,
                            onValueChange = { storeName = it },
                            label = { Text(strings.storeNameLabel) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text(strings.ownerNameLabel) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text(strings.phoneLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text(strings.addressLabel) },
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Prefix Nomor Nota / Struk + Live Preview
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = receiptPrefix,
                                onValueChange = { raw ->
                                    receiptPrefix = raw.uppercase()
                                        .filter { it.isLetterOrDigit() }
                                        .take(10)
                                },
                                label = { Text(strings.receiptPrefixLabel) },
                                placeholder = { Text(strings.receiptPrefixHint) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Tag,
                                        contentDescription = null,
                                        tint = BrandBlue
                                    )
                                },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_receipt_prefix_input")
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = strings.receiptPrefixPreview,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = previewTxNumber,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.testTag("settings_receipt_prefix_preview")
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = taxPercentInput,
                            onValueChange = { taxPercentInput = it },
                            label = { Text(strings.defaultTaxPercentLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Pinned Save Button
                    HorizontalDivider()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Button(
                            onClick = {
                                val taxPct = taxPercentInput.replace(",", ".").toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 11.0
                                val cleanPrefix = PosFormatters.sanitizeReceiptPrefix(receiptPrefix)
                                receiptPrefix = cleanPrefix
                                onSaveProfile(
                                    storeName,
                                    ownerName,
                                    phone,
                                    address,
                                    photoData,
                                    storeIcon,
                                    storeIconBgColor,
                                    cleanPrefix,
                                    taxPct
                                )
                                showProfileDialog = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("settings_save_profile_button")
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.saveChanges, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (showPhotoPicker) {
        PhotoPickerSheetDialog(
            onDismiss = { showPhotoPicker = false },
            onPhotoCompressed = { newPhoto ->
                photoData = newPhoto
                val taxPct = taxPercentInput.replace(",", ".").toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 11.0
                onSaveProfile(
                    storeName,
                    ownerName,
                    phone,
                    address,
                    newPhoto,
                    storeIcon,
                    storeIconBgColor,
                    PosFormatters.sanitizeReceiptPrefix(receiptPrefix),
                    taxPct
                )
            }
        )
    }

    // --- Dialog 2: Tampilan & Bahasa ---
    if (showAppearanceDialog) {
        AlertDialog(
            onDismissRequest = { showAppearanceDialog = false },
            title = { Text(strings.appearanceLanguageTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(strings.themeSection, style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = !store.darkTheme,
                            onClick = { onSetDarkTheme(false) },
                            leadingIcon = { Icon(Icons.Rounded.LightMode, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            label = { Text(strings.lightTheme) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = store.darkTheme,
                            onClick = { onSetDarkTheme(true) },
                            leadingIcon = { Icon(Icons.Rounded.DarkMode, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            label = { Text(strings.darkTheme) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Language, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                        Text(strings.languageSection, style = MaterialTheme.typography.titleSmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = store.language != "en",
                            onClick = { onSetLanguage("id") },
                            label = { Text(strings.languageIndonesian) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = store.language == "en",
                            onClick = { onSetLanguage("en") },
                            label = { Text(strings.languageEnglish) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppearanceDialog = false }) {
                    Text(strings.close)
                }
            }
        )
    }

    // --- Dialog 3: Printer Kasir & Daftar Printer Tersambung ---
    if (showPrinterDialog) {
        PrinterSettingsDialog(
            store = store,
            localPrefs = localPrefs,
            onDismiss = { showPrinterDialog = false },
            onSelectPrinter = onSelectPrinter,
            onClearPrinter = onClearPrinter,
            onSetPaperWidth = onSetPrinterPaperWidth
        )
    }

    // --- Dialog 4: Backup & Restore ---
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { if (!isBackupBusy) showBackupDialog = false },
            title = { Text(strings.backupRestoreSection) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${strings.lastBackupLabel}: $lastBackupStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isBackupBusy) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = strings.backupInProgress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            backupExportLauncher.launch("ADA_KASIR_Backup_${System.currentTimeMillis()}.json")
                        },
                        enabled = !isBackupBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.manualBackupButton)
                    }
                    OutlinedButton(
                        onClick = {
                            restoreImportLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        enabled = !isBackupBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.restoreBackupButton)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupDialog = false }, enabled = !isBackupBusy) {
                    Text(strings.close)
                }
            }
        )
    }

    // --- Dialog 5: Tentang Aplikasi & Penyimpanan Lokal ---
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(strings.aboutSection) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.appName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Developer: ${strings.developerBy}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text("${strings.versionLabel}: 1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(strings.aboutDescription, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(strings.copyrightText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(strings.googleAccountSection, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = strings.syncedWithGoogle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(strings.close)
                }
            }
        )
    }

    val summary = pendingRestoreSummary
    val jsonToRestore = pendingRestoreJson
    if (summary != null && jsonToRestore != null) {
        AlertDialog(
            onDismissRequest = {
                pendingRestoreJson = null
                pendingRestoreSummary = null
            },
            title = { Text(strings.restoreConfirmTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.restoreConfirmBody)
                    HorizontalDivider()
                    Text("${strings.storeNameLabel}: ${summary.storeName}", style = MaterialTheme.typography.bodySmall)
                    Text("${strings.menuProducts}: ${summary.productCount}", style = MaterialTheme.typography.bodySmall)
                    Text("${strings.menuHistory}: ${summary.transactionCount}", style = MaterialTheme.typography.bodySmall)
                    Text("${strings.menuExpenses}: ${summary.expenseCount}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRestoreJson = null
                        pendingRestoreSummary = null
                        showBackupDialog = false
                        onRestoreBackupJson(jsonToRestore)
                    }
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingRestoreJson = null
                        pendingRestoreSummary = null
                    }
                ) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // --- Dialog 6: Kustomisasi Struk ---
    if (showReceiptCustomDialog) {
        ReceiptCustomizationDialog(
            store = store,
            onDismiss = { showReceiptCustomDialog = false },
            onSave = { showLogo, showCashier, showPhone, showAddress, showCustomer, footer ->
                onSaveReceiptCustomization(showLogo, showCashier, showPhone, showAddress, showCustomer, footer)
                showReceiptCustomDialog = false
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun SettingsMenuCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingAccessory: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val containerTint = if (isDark) iconTint.copy(alpha = 0.22f) else iconBg
    val tintColor = if (isDark) iconTint.copy(alpha = 0.9f) else iconTint

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(containerTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tintColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailingAccessory != null) {
                trailingAccessory()
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrinterSettingsDialog(
    store: StoreSettings,
    localPrefs: LocalAppPrefs,
    onDismiss: () -> Unit,
    onSelectPrinter: (String, String) -> Unit,
    onClearPrinter: () -> Unit,
    onSetPaperWidth: (Int) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()

    var printers by remember { mutableStateOf<List<PairedPrinter>>(emptyList()) }
    var isTestingPrint by remember { mutableStateOf(false) }
    var hasBtPermission by remember { mutableStateOf(true) }

    fun loadPrinters() {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        hasBtPermission = granted
        printers = if (granted) ReceiptHelper.getPairedBluetoothPrinters(context) else emptyList()
    }

    val btPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasBtPermission = grants.values.all { it }
        if (hasBtPermission) {
            printers = ReceiptHelper.getPairedBluetoothPrinters(context)
        } else {
            Toast.makeText(context, strings.bluetoothPermissionNeeded, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        loadPrinters()
    }

    val paperWidth = if (localPrefs.printerPaperWidthMm == 80) PaperWidth.MM_80 else PaperWidth.MM_58

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .testTag("printer_settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.printerSettingsTitle, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.close)
                    }
                }

                // Default paper width
                Text(strings.defaultPaperWidthLabel, style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = localPrefs.printerPaperWidthMm != 80,
                        onClick = { onSetPaperWidth(58) },
                        label = { Text(strings.printerWidth58) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = localPrefs.printerPaperWidthMm == 80,
                        onClick = { onSetPaperWidth(80) },
                        label = { Text(strings.printerWidth80) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Active / Selected Printer Summary
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = strings.selectedPrinterLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        if (localPrefs.defaultPrinterAddress.isBlank()) {
                            Text(
                                text = strings.noPrinterSelected,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "${localPrefs.defaultPrinterName} (${localPrefs.defaultPrinterAddress})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        isTestingPrint = true
                                        scope.launch {
                                            val res = ReceiptHelper.printTestReceipt(
                                                context = context,
                                                printerAddress = localPrefs.defaultPrinterAddress,
                                                store = store,
                                                paperWidth = paperWidth
                                            )
                                            isTestingPrint = false
                                            val msg = if (res.isSuccess) strings.printSuccessMsg else "${strings.printFailedMsg}: ${res.exceptionOrNull()?.localizedMessage}"
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    enabled = !isTestingPrint,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(strings.testPrintButton)
                                }
                                OutlinedButton(
                                    onClick = onClearPrinter,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(strings.disconnectPrinterButton)
                                }
                            }
                        }
                    }
                }

                // Connected / Paired Printers List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.connectedPrintersTitle, style = MaterialTheme.typography.titleSmall)
                    TextButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val needConnect = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                                val needScan = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                                if (needConnect || needScan) {
                                    btPermLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.BLUETOOTH_CONNECT,
                                            Manifest.permission.BLUETOOTH_SCAN
                                        )
                                    )
                                    return@TextButton
                                }
                            }
                            loadPrinters()
                        },
                        modifier = Modifier.testTag("refresh_printers_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.refreshPrintersButton)
                    }
                }

                if (!hasBtPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    OutlinedButton(
                        onClick = {
                            btPermLauncher.launch(
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.BLUETOOTH_SCAN
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Bluetooth, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.bluetoothPermissionNeeded)
                    }
                } else if (printers.isEmpty()) {
                    Text(
                        text = strings.noPairedPrinters,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        printers.forEach { printer ->
                            val isSelected = printer.address == localPrefs.defaultPrinterAddress
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bluetooth,
                                        contentDescription = null,
                                        tint = if (printer.isConnected || isSelected) StatusSuccess else BrandBlue
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = printer.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = printer.address,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = if (printer.isConnected) StatusSuccess.copy(alpha = 0.14f) else BrandBlue.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = if (printer.isConnected) strings.printerConnectedBadge else strings.printerPairedBadge,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (printer.isConnected) StatusSuccess else BrandBlue,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            if (isSelected) {
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = BrandGreen.copy(alpha = 0.16f)
                                                ) {
                                                    Text(
                                                        text = strings.printerSelectedBadge,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = BrandGreen,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    FilledTonalButton(
                                        onClick = { onSelectPrinter(printer.name, printer.address) }
                                    ) {
                                        Text(if (isSelected) strings.printerSelectedBadge else strings.selectPrinterButton)
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.openBluetoothSettingsButton)
                }
            }
        }
    }
}

@Composable
private fun ReceiptCustomizationDialog(
    store: StoreSettings,
    onDismiss: () -> Unit,
    onSave: (Boolean, Boolean, Boolean, Boolean, Boolean, String) -> Unit
) {
    val strings = LocalAppStrings.current
    var showLogo by remember(store.showReceiptLogo) { mutableStateOf(store.showReceiptLogo) }
    var showCashierName by remember(store.showReceiptCashierName) { mutableStateOf(store.showReceiptCashierName) }
    var showStorePhone by remember(store.showReceiptStorePhone) { mutableStateOf(store.showReceiptStorePhone) }
    var showStoreAddress by remember(store.showReceiptStoreAddress) { mutableStateOf(store.showReceiptStoreAddress) }
    var showCustomerName by remember(store.showReceiptCustomerName) { mutableStateOf(store.showReceiptCustomerName) }
    var footerMessage by remember(store.receiptFooterMessage) { mutableStateOf(store.receiptFooterMessage) }

    val previewStore = remember(
        store,
        showLogo,
        showCashierName,
        showStorePhone,
        showStoreAddress,
        showCustomerName,
        footerMessage
    ) {
        store.copy(
            storeName = store.storeName.ifBlank { "Toko ADA KASIR" },
            ownerName = store.ownerName.ifBlank { "Kasir Toko" },
            phone = store.phone.ifBlank { "0812-3456-7890" },
            address = store.address.ifBlank { "Jl. Raya Utama No. 12" },
            showReceiptLogo = showLogo,
            showReceiptCashierName = showCashierName,
            showReceiptStorePhone = showStorePhone,
            showReceiptStoreAddress = showStoreAddress,
            showReceiptCustomerName = showCustomerName,
            receiptFooterMessage = footerMessage
        )
    }

    val sampleTx = remember(previewStore.ownerName, previewStore.receiptPrefix) {
        PosTransaction(
            id = "preview",
            transactionNumber = PosFormatters.generateTransactionNumber(prefix = previewStore.receiptPrefix),
            customerName = "Budi Santoso",
            cashierName = previewStore.ownerName.ifBlank { "Admin Kasir" },
            items = listOf(
                TransactionItem(
                    productId = "p1",
                    productName = "Beras Premium 5 Kg",
                    unit = "Pcs",
                    unitPrice = 75000.0,
                    subtotal = 75000.0,
                    quantity = 1.0
                ),
                TransactionItem(
                    productId = "p2",
                    productName = "Air Mineral",
                    unit = "Botol",
                    unitPrice = 4000.0,
                    subtotal = 8000.0,
                    quantity = 2.0
                )
            ),
            subtotal = 83000.0,
            totalAmount = 83000.0,
            paymentMethod = PaymentMethod.CASH.code,
            amountPaid = 100000.0,
            changeAmount = 17000.0,
            transactionMillis = System.currentTimeMillis()
        )
    }

    val previewText = remember(previewStore, sampleTx, strings) {
        ReceiptHelper.buildPlainTextReceipt(
            transaction = sampleTx,
            store = previewStore,
            strings = strings,
            language = store.language,
            charsPerLine = 32
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .widthIn(max = 500.dp)
                .heightIn(max = 680.dp)
                .testTag("receipt_customization_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Pinned Dialog Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.receiptCustomTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.close)
                    }
                }
                HorizontalDivider()

                // Scrollable Receipt Preview, Toggles, and Footer Message
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Live Receipt Image Preview at the top
                    Text(strings.receiptPreviewLabel, style = MaterialTheme.typography.labelLarge)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFAFAFA),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .testTag("receipt_custom_preview")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.width(IntrinsicSize.Max),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (showLogo) {
                                    StoreProfileAvatar(
                                        photoData = store.photoData,
                                        storeIcon = store.storeIcon,
                                        storeIconBgColor = store.storeIconBgColor,
                                        size = 48.dp
                                    )
                                }
                                Text(
                                    text = previewText,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    softWrap = false,
                                    color = Color(0xFF111827)
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                    // 2-6. Toggle Switches
                    ReceiptToggleRow(
                        label = strings.receiptShowLogoLabel,
                        checked = showLogo,
                        onCheckedChange = { showLogo = it },
                        modifier = Modifier.testTag("switch_receipt_logo")
                    )
                    ReceiptToggleRow(
                        label = strings.receiptShowCashierLabel,
                        checked = showCashierName,
                        onCheckedChange = { showCashierName = it },
                        modifier = Modifier.testTag("switch_receipt_cashier")
                    )
                    ReceiptToggleRow(
                        label = strings.receiptShowPhoneLabel,
                        checked = showStorePhone,
                        onCheckedChange = { showStorePhone = it },
                        modifier = Modifier.testTag("switch_receipt_phone")
                    )
                    ReceiptToggleRow(
                        label = strings.receiptShowAddressLabel,
                        checked = showStoreAddress,
                        onCheckedChange = { showStoreAddress = it },
                        modifier = Modifier.testTag("switch_receipt_address")
                    )
                    ReceiptToggleRow(
                        label = strings.receiptShowCustomerLabel,
                        checked = showCustomerName,
                        onCheckedChange = { showCustomerName = it },
                        modifier = Modifier.testTag("switch_receipt_customer")
                    )

                    // 7. Editable Footer Message
                    OutlinedTextField(
                        value = footerMessage,
                        onValueChange = { footerMessage = it.take(200) },
                        label = { Text(strings.receiptFooterMessageLabel) },
                        placeholder = { Text(strings.receiptFooterMessageHint) },
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("receipt_footer_input")
                    )
                }

                // Pinned Bottom Save Bar — always crisp and clear of navigation bars
                HorizontalDivider()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = {
                            onSave(
                                showLogo,
                                showCashierName,
                                showStorePhone,
                                showStoreAddress,
                                showCustomerName,
                                footerMessage
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("save_receipt_customization_button")
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.saveChanges,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
