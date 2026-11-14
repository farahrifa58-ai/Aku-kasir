package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.PosTransaction
import com.example.data.model.StoreSettings
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueDark
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.MenuCashierBgDark
import com.example.ui.theme.MenuCashierBgLight
import com.example.ui.theme.MenuCashierFgDark
import com.example.ui.theme.MenuCashierFgLight
import com.example.ui.theme.MenuDashboardBgDark
import com.example.ui.theme.MenuDashboardBgLight
import com.example.ui.theme.MenuDashboardFgDark
import com.example.ui.theme.MenuDashboardFgLight
import com.example.ui.theme.MenuHistoryBgDark
import com.example.ui.theme.MenuHistoryBgLight
import com.example.ui.theme.MenuHistoryFgDark
import com.example.ui.theme.MenuHistoryFgLight
import com.example.ui.theme.MenuProductsBgDark
import com.example.ui.theme.MenuProductsBgLight
import com.example.ui.theme.MenuProductsFgDark
import com.example.ui.theme.MenuProductsFgLight
import com.example.ui.theme.MenuSettingsBgDark
import com.example.ui.theme.MenuSettingsBgLight
import com.example.ui.theme.MenuSettingsFgDark
import com.example.ui.theme.MenuSettingsFgLight
import com.example.ui.viewmodel.AppMenu
import com.example.util.ImageUtils
import com.example.util.PairedPrinter
import com.example.util.PaperWidth
import com.example.util.ReceiptHelper
import kotlinx.coroutines.launch

@Composable
fun StoreProfileAvatar(
    photoData: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val bitmap = remember(photoData) { ImageUtils.decodeDataUriToBitmap(photoData) }
    val baseModifier = modifier
        .size(size)
        .clip(CircleShape)
        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Store profile photo",
            contentScale = ContentScale.Crop,
            modifier = baseModifier
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.img_store_default),
            contentDescription = "Default store photo",
            contentScale = ContentScale.Crop,
            modifier = baseModifier
        )
    }
}

@Composable
fun ProductPhotoBox(
    photoData: String,
    contentDescription: String,
    size: Dp = 56.dp,
    cornerRadius: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(photoData) { ImageUtils.decodeDataUriToBitmap(photoData) }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Storefront,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                modifier = Modifier.size(size * 0.48f)
            )
        }
    }
}

@Composable
fun SubScreenTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    centeredTitle: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    Surface(
        color = Color.Transparent,
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(BrandBlueDark, BrandBlue)
                )
            )
            .statusBarsPadding()
            .testTag("blue_subscreen_header")
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .minimumInteractiveComponentSize()
                        .testTag("back_to_dashboard_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = strings.menuDashboard,
                        tint = Color.White
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (centeredTitle) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier
                        .align(if (centeredTitle) Alignment.Center else Alignment.CenterStart)
                        .padding(start = if (centeredTitle) 48.dp else 52.dp, end = 48.dp)
                )
                if (trailingContent != null) {
                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        trailingContent()
                    }
                }
            }
        }
    }
}

/**
 * Center Grid Menu Dock — sleek bottom return-to-Dashboard control on sub-screens.
 */
@Composable
fun CenterGridNavigationDock(
    currentMenu: AppMenu,
    isPanelOpen: Boolean,
    onToggleMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val currentTitle = when (currentMenu) {
        AppMenu.DASHBOARD -> strings.menuDashboard
        AppMenu.CASHIER -> strings.menuCashier
        AppMenu.PRODUCTS -> strings.menuProducts
        AppMenu.SALES_REPORT -> strings.menuSalesReport
        AppMenu.EXPENSES -> strings.menuExpenses
        AppMenu.HISTORY -> strings.menuHistory
        AppMenu.SETTINGS -> strings.menuSettings
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .widthIn(min = 210.dp, max = 340.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        RoundedCornerShape(32.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = currentTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp, end = 12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(BrandBlue, BrandGreen)
                                )
                            )
                            .clickable(onClick = onToggleMenu)
                            .minimumInteractiveComponentSize()
                            .testTag("center_grid_menu_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPanelOpen) Icons.Rounded.Close else Icons.Rounded.GridView,
                            contentDescription = strings.menuTitle,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Text(
                        text = "ADA KASIR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                    )
                }
            }
        }
    }
}

private data class MenuGridItem(
    val menu: AppMenu,
    val label: String,
    val icon: ImageVector,
    val tag: String,
    val containerColor: Color,
    val contentColor: Color
)

/**
 * Floating Center 5-Menu Grid Panel triggered by the center navigation button.
 * Centered horizontally and vertically over a translucent dark scrim with muted pastel cards:
 * Row 1: Dashboard (blue), Kasir (soft orange), Produk (sage green)
 * Row 2: Riwayat (purple), Pengaturan (dusty red)
 */
@Composable
fun FiveMenuGridOverlay(
    visible: Boolean,
    currentMenu: AppMenu,
    onSelectMenu: (AppMenu) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val items = listOf(
        MenuGridItem(
            menu = AppMenu.DASHBOARD,
            label = strings.menuDashboard,
            icon = Icons.Outlined.GridView,
            tag = "menu_dashboard",
            containerColor = if (isDark) MenuDashboardBgDark else MenuDashboardBgLight,
            contentColor = if (isDark) MenuDashboardFgDark else MenuDashboardFgLight
        ),
        MenuGridItem(
            menu = AppMenu.CASHIER,
            label = strings.menuCashier,
            icon = Icons.Outlined.ShoppingCart,
            tag = "menu_cashier",
            containerColor = if (isDark) MenuCashierBgDark else MenuCashierBgLight,
            contentColor = if (isDark) MenuCashierFgDark else MenuCashierFgLight
        ),
        MenuGridItem(
            menu = AppMenu.PRODUCTS,
            label = strings.menuProducts,
            icon = Icons.Outlined.Inventory2,
            tag = "menu_products",
            containerColor = if (isDark) MenuProductsBgDark else MenuProductsBgLight,
            contentColor = if (isDark) MenuProductsFgDark else MenuProductsFgLight
        ),
        MenuGridItem(
            menu = AppMenu.HISTORY,
            label = strings.menuHistory,
            icon = Icons.Rounded.History,
            tag = "menu_history",
            containerColor = if (isDark) MenuHistoryBgDark else MenuHistoryBgLight,
            contentColor = if (isDark) MenuHistoryFgDark else MenuHistoryFgLight
        ),
        MenuGridItem(
            menu = AppMenu.SETTINGS,
            label = strings.menuSettings,
            icon = Icons.Outlined.Settings,
            tag = "menu_settings",
            containerColor = if (isDark) MenuSettingsBgDark else MenuSettingsBgLight,
            contentColor = if (isDark) MenuSettingsFgDark else MenuSettingsFgLight
        )
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)),
        exit = fadeOut(spring(stiffness = Spring.StiffnessMedium))
    ) {
        val scrimInteraction = remember { MutableInteractionSource() }
        val cardInteraction = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.56f))
                .clickable(
                    interactionSource = scrimInteraction,
                    indication = null,
                    onClick = onDismiss
                )
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .testTag("floating_menu_overlay"),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.90f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(),
                exit = scaleOut(targetScale = 0.92f) + fadeOut()
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 18.dp),
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = cardInteraction,
                            indication = null,
                            onClick = {}
                        )
                        .testTag("floating_center_menu")
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = strings.menuTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ADA KASIR • Rafline Dev",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("menu_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = strings.close,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Row 1: 3 cards (Dashboard, Kasir, Produk)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items.take(3).forEach { item ->
                                MenuGridTile(
                                    item = item,
                                    selected = item.menu == currentMenu,
                                    onClick = { onSelectMenu(item.menu) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Row 2: 2 cards (Riwayat, Pengaturan)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items.drop(3).forEach { item ->
                                MenuGridTile(
                                    item = item,
                                    selected = item.menu == currentMenu,
                                    onClick = { onSelectMenu(item.menu) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuGridTile(
    item: MenuGridItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = item.containerColor,
        tonalElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier
            .height(98.dp)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) item.contentColor.copy(alpha = 0.8f) else item.contentColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag(item.tag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = item.contentColor,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = item.contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PhotoPickerSheetDialog(
    onDismiss: () -> Unit,
    onPhotoCompressed: (String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            ImageUtils.compressUriToDataUri(context, uri)?.let(onPhotoCompressed)
        }
        onDismiss()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraUri
        if (success && uri != null) {
            ImageUtils.compressUriToDataUri(context, uri)?.let(onPhotoCompressed)
        }
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.productPhotoLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(strings.pickFromGallery)
                }
                OutlinedButton(
                    onClick = {
                        val outUri = ImageUtils.createCameraOutputUri(context)
                        cameraUri = outUri
                        cameraLauncher.launch(outUri)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(strings.takePhoto)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close)
            }
        }
    )
}

@Composable
fun ReceiptDialog(
    transaction: PosTransaction,
    store: StoreSettings,
    language: String,
    defaultPaperWidthMm: Int = 58,
    defaultPrinterAddress: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    var paperWidth by remember(defaultPaperWidthMm) {
        mutableStateOf(if (defaultPaperWidthMm == 80) PaperWidth.MM_80 else PaperWidth.MM_58)
    }
    var showPrinterPicker by remember { mutableStateOf(false) }
    var pairedPrinters by remember { mutableStateOf<List<PairedPrinter>>(emptyList()) }
    var isPrinting by remember { mutableStateOf(false) }

    val receiptText = remember(transaction, store, paperWidth, language) {
        ReceiptHelper.buildPlainTextReceipt(
            transaction = transaction,
            store = store,
            strings = strings,
            language = language,
            charsPerLine = paperWidth.charsPerLine
        )
    }

    val pdfSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                val tmpPdf = ReceiptHelper.generateReceiptPdf(
                    context, transaction, store, strings, language, paperWidth
                )
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    tmpPdf.inputStream().use { input -> input.copyTo(out) }
                }
                Toast.makeText(context, strings.pdfSavedMsg, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, e.localizedMessage ?: "PDF error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants.values.all { it }
        if (granted) {
            pairedPrinters = ReceiptHelper.getPairedBluetoothPrinters(context)
            showPrinterPicker = true
        } else {
            Toast.makeText(context, strings.bluetoothPermissionNeeded, Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
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
                    Text(
                        text = strings.receiptTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.close)
                    }
                }

                // 58mm vs 80mm selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = paperWidth == PaperWidth.MM_58,
                        onClick = { paperWidth = PaperWidth.MM_58 },
                        label = { Text(strings.printerWidth58) }
                    )
                    FilterChip(
                        selected = paperWidth == PaperWidth.MM_80,
                        onClick = { paperWidth = PaperWidth.MM_80 },
                        label = { Text(strings.printerWidth80) }
                    )
                }

                // Thermal paper preview
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFAFAFA),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (store.showReceiptLogo) {
                            val storeLogoBitmap = remember(store.photoData) {
                                ImageUtils.decodeDataUriToBitmap(store.photoData)
                            }
                            if (storeLogoBitmap != null) {
                                Image(
                                    bitmap = storeLogoBitmap.asImageBitmap(),
                                    contentDescription = store.storeName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ada_kasir_logo),
                                    contentDescription = "ADA KASIR",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            }
                        }
                        Text(
                            text = receiptText,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = if (paperWidth == PaperWidth.MM_58) 11.sp else 10.sp,
                            lineHeight = 15.sp,
                            color = Color(0xFF111827),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                HorizontalDivider()

                // Print Receipt (Bluetooth 58/80mm)
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val needConnect = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                            val needScan = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.BLUETOOTH_SCAN
                            ) != PackageManager.PERMISSION_GRANTED
                            if (needConnect || needScan) {
                                btPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.BLUETOOTH_CONNECT,
                                        Manifest.permission.BLUETOOTH_SCAN
                                    )
                                )
                                return@Button
                            }
                        }
                        pairedPrinters = ReceiptHelper.getPairedBluetoothPrinters(context)
                        showPrinterPicker = true
                    },
                    enabled = !isPrinting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Rounded.Print, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.printReceipt)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            pdfSaverLauncher.launch("Struk_${transaction.transactionNumber}.pdf")
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = strings.savePdf,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Button(
                        onClick = {
                            ReceiptHelper.shareReceiptWhatsApp(context, transaction, store, strings, language)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = strings.shareWhatsApp,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }

    if (showPrinterPicker) {
        AlertDialog(
            onDismissRequest = { showPrinterPicker = false },
            title = { Text("${strings.selectPrinterTitle} (${paperWidth.mm}mm)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pairedPrinters.isEmpty()) {
                        Text(
                            text = strings.noPairedPrinters,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        pairedPrinters.forEach { printer ->
                            val isSavedDefault = printer.address == defaultPrinterAddress
                            FilledTonalButton(
                                onClick = {
                                    showPrinterPicker = false
                                    isPrinting = true
                                    scope.launch {
                                        val res = ReceiptHelper.printBluetoothEscPos(
                                            context = context,
                                            printerAddress = printer.address,
                                            transaction = transaction,
                                            store = store,
                                            strings = strings,
                                            language = language,
                                            paperWidth = paperWidth
                                        )
                                        isPrinting = false
                                        val msg = if (res.isSuccess) strings.printSuccessMsg else "${strings.printFailedMsg}: ${res.exceptionOrNull()?.localizedMessage}"
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (isSavedDefault) Icons.Rounded.CheckCircle else Icons.Rounded.Bluetooth,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = printer.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = buildString {
                                            append(printer.address)
                                            if (printer.isConnected) append(" • ${strings.printerConnectedBadge}")
                                            if (isSavedDefault) append(" • ${strings.printerSelectedBadge}")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            showPrinterPicker = false
                            val pdf = ReceiptHelper.generateReceiptPdf(
                                context, transaction, store, strings, language, paperWidth
                            )
                            ReceiptHelper.printViaAndroidSystem(context, pdf, "Struk_${transaction.transactionNumber}")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.systemPrintFallback)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrinterPicker = false }) {
                    Text(strings.close)
                }
            }
        )
    }
}
