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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BakeryDining
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.VerifiedUser
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.util.BackupHealthStatus
import com.example.util.ImageUtils
import com.example.util.PairedPrinter
import com.example.util.PaperWidth
import com.example.util.PosFormatters
import com.example.util.ReceiptHelper
import kotlinx.coroutines.launch

data class StoreIconOption(
    val key: String,
    val labelId: String,
    val labelEn: String,
    val icon: ImageVector
)

object StoreBrandingPresets {
    val iconOptions: List<StoreIconOption> = listOf(
        StoreIconOption("storefront", "Toko", "Shop", Icons.Rounded.Storefront),
        StoreIconOption("coffee", "Kafe", "Cafe", Icons.Rounded.LocalCafe),
        StoreIconOption("restaurant", "Kuliner", "Food", Icons.Rounded.Restaurant),
        StoreIconOption("shopping_cart", "Mart", "Mart", Icons.Rounded.ShoppingCart),
        StoreIconOption("bakery", "Roti", "Bakery", Icons.Rounded.BakeryDining),
        StoreIconOption("pharmacy", "Apotek", "Health", Icons.Rounded.LocalPharmacy),
        StoreIconOption("checkroom", "Fashion", "Apparel", Icons.Rounded.Checkroom),
        StoreIconOption("devices", "Gadget", "Tech", Icons.Rounded.Devices),
        StoreIconOption("spa", "Salon", "Beauty", Icons.Rounded.Spa),
        StoreIconOption("build", "Servis", "Service", Icons.Rounded.Build)
    )

    val bgColorHexOptions: List<String> = listOf(
        "#2563EB", // Royal Blue
        "#059669", // Emerald
        "#0D9488", // Teal
        "#7C3AED", // Violet
        "#DB2777", // Rose Pink
        "#EA580C", // Sunset Orange
        "#D97706", // Amber Gold
        "#DC2626", // Crimson Red
        "#4F46E5", // Indigo
        "#1E293B"  // Slate Navy
    )

    fun resolveIcon(key: String): ImageVector {
        return iconOptions.firstOrNull { it.key == key }?.icon ?: Icons.Rounded.Storefront
    }

    fun parseHexColor(hex: String, fallback: Color = BrandBlue): Color {
        return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
    }
}

fun resolveBundledProductDrawable(photoData: String): Int? {
    if (!photoData.startsWith("drawable:")) return null
    return when (photoData.removePrefix("drawable:").trim()) {
        "img_prod_kopi_aren" -> R.drawable.img_prod_kopi_aren
        "img_prod_beras_5kg" -> R.drawable.img_prod_beras_5kg
        "img_prod_gula_kg" -> R.drawable.img_prod_gula_kg
        "img_prod_mie_pack" -> R.drawable.img_prod_mie_pack
        else -> null
    }
}

@Composable
fun StoreProfileAvatar(
    photoData: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    storeIcon: String = "storefront",
    storeIconBgColor: String = "#2563EB",
    onClick: (() -> Unit)? = null
) {
    val bitmap = remember(photoData) { ImageUtils.decodeDataUriToBitmap(photoData) }
    val bgColor = remember(storeIconBgColor) {
        StoreBrandingPresets.parseHexColor(storeIconBgColor, BrandBlue)
    }
    val darkerBgColor = remember(bgColor) {
        Color(
            red = (bgColor.red * 0.78f).coerceIn(0f, 1f),
            green = (bgColor.green * 0.78f).coerceIn(0f, 1f),
            blue = (bgColor.blue * 0.85f).coerceIn(0f, 1f),
            alpha = 1f
        )
    }
    val iconVector = remember(storeIcon) { StoreBrandingPresets.resolveIcon(storeIcon) }

    val baseModifier = modifier
        .size(size)
        .clip(CircleShape)
        .border(2.dp, bgColor.copy(alpha = 0.30f), CircleShape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Store profile photo",
            contentScale = ContentScale.Crop,
            modifier = baseModifier
        )
    } else {
        Box(
            modifier = baseModifier.background(
                Brush.linearGradient(listOf(bgColor, darkerBgColor))
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = "Store icon",
                tint = Color.White,
                modifier = Modifier.size(size * 0.50f)
            )
        }
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
    val bundledResId = remember(photoData) { resolveBundledProductDrawable(photoData) }
    val bitmap = remember(photoData, bundledResId) {
        if (bundledResId == null) ImageUtils.decodeDataUriToBitmap(photoData) else null
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (bundledResId != null) {
            Image(
                painter = painterResource(id = bundledResId),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (bitmap != null) {
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(BrandBlue)
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

                // Thermal paper preview (logo & monospace text aligned to exact same center axis)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFAFAFA),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
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
                            if (store.showReceiptLogo) {
                                StoreProfileAvatar(
                                    photoData = store.photoData,
                                    storeIcon = store.storeIcon,
                                    storeIconBgColor = store.storeIconBgColor,
                                    size = 48.dp
                                )
                            }
                            Text(
                                text = receiptText,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = if (paperWidth == PaperWidth.MM_58) 10.sp else 9.sp,
                                lineHeight = 14.sp,
                                softWrap = false,
                                color = Color(0xFF111827)
                            )
                        }
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

/**
 * Compact icon-only Shield button placed in the app header alongside the notification bell.
 * - Green (VerifiedUser / ShieldCheck): backed up today & no new data
 * - Yellow (GppMaybe / ShieldAlert): >24h or new unsaved changes + pulsing amber dot
 * - Red (GppBad / ShieldX): never backed up or >3 days + pulsing red dot
 */
@Composable
fun BackupStatusHeaderPill(
    status: BackupHealthStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val (accessibilityLabel, icon, bgColor, borderColor, iconTint, dotColor) = when (status) {
        BackupHealthStatus.SAFE -> Sextuple(
            strings.backupStatusSafe,
            Icons.Rounded.VerifiedUser,
            Color(0xFFECFDF5),
            Color(0xFF6EE7B7),
            Color(0xFF15803D),
            null
        )
        BackupHealthStatus.NEEDS_BACKUP -> Sextuple(
            strings.backupStatusNeedsBackup,
            Icons.Rounded.GppMaybe,
            Color(0xFFFFFBEB),
            Color(0xFFFCD34D),
            Color(0xFFD97706),
            Color(0xFFF59E0B)
        )
        BackupHealthStatus.NEVER_OR_OVERDUE -> Sextuple(
            strings.backupStatusNever,
            Icons.Rounded.GppBad,
            Color(0xFFFEF2F2),
            Color(0xFFFCA5A5),
            Color(0xFFDC2626),
            Color(0xFFEF4444)
        )
    }

    val showAlertDot = dotColor != null

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("header_backup_status_badge"),
        contentAlignment = Alignment.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = bgColor,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, borderColor, RoundedCornerShape(11.dp))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = accessibilityLabel,
                        tint = iconTint,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            if (showAlertDot && dotColor != null) {
                val pulseTransition = rememberInfiniteTransition(label = "backup_shield_dot_pulse")
                val pulseScale by pulseTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.9f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "pulse_scale"
                )
                val pulseAlpha by pulseTransition.animateFloat(
                    initialValue = 0.65f,
                    targetValue = 0.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "pulse_alpha"
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (-3).dp)
                        .testTag("header_backup_status_alert_dot"),
                    contentAlignment = Alignment.Center
                ) {
                    // Subtle pulsing halo ring
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                                alpha = pulseAlpha
                            }
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    // Solid notification dot with crisp white outline
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }
            }
        }
    }
}

private data class Sextuple<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F
)

/**
 * Clean centered modal showing detailed backup status when the header Shield icon is tapped.
 * Never shown automatically — only opens on user tap.
 */
@Composable
fun QuickBackupStatusDialog(
    status: BackupHealthStatus,
    lastBackupAtMillis: Long,
    hasNewDataSinceBackup: Boolean = false,
    language: String,
    isBackupBusy: Boolean = false,
    onOpenBackupSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val (statusTitle, statusDesc, icon, accentColor, ctaColor) = when (status) {
        BackupHealthStatus.SAFE -> Quintuple(
            strings.backupModalTitleSafe,
            strings.backupQuickSafeDesc,
            Icons.Rounded.VerifiedUser,
            Color(0xFF16A34A),
            BrandGreen
        )
        BackupHealthStatus.NEEDS_BACKUP -> Quintuple(
            if (hasNewDataSinceBackup) strings.backupModalTitleNewData else strings.backupModalTitleOverdue24h,
            strings.backupQuickNeedsDesc,
            Icons.Rounded.GppMaybe,
            Color(0xFFD97706),
            BrandBlue
        )
        BackupHealthStatus.NEVER_OR_OVERDUE -> Quintuple(
            strings.backupModalTitleNeverOrDanger,
            strings.backupQuickDangerDesc,
            Icons.Rounded.GppBad,
            Color(0xFFDC2626),
            BrandBlue
        )
    }

    val historyLine = if (lastBackupAtMillis > 0L) {
        val formattedDate = PosFormatters.formatDateTime(lastBackupAtMillis, language)
        val relative = com.example.util.BackupStatusHelper.formatRelativeElapsed(
            lastBackupAtMillis = lastBackupAtMillis,
            language = language
        )
        "${strings.backupModalLastPrefix} $formattedDate ($relative)"
    } else {
        strings.backupModalNeverDetail
    }

    AlertDialog(
        onDismissRequest = { if (!isBackupBusy) onDismiss() },
        modifier = Modifier.testTag("quick_backup_status_dialog"),
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = statusTitle,
                    tint = accentColor,
                    modifier = Modifier.size(36.dp)
                )
            }
        },
        title = {
            Text(
                text = statusTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("quick_backup_status_title")
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = accentColor.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, accentColor.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                ) {
                    Text(
                        text = historyLine,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("quick_backup_last_time_text")
                    )
                }
                Text(
                    text = statusDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(2.dp))
                Button(
                    onClick = onOpenBackupSettings,
                    enabled = !isBackupBusy,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ctaColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("quick_open_backup_settings_button")
                ) {
                    Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = strings.backupModalGoToMenuButton,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isBackupBusy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("quick_backup_close_button")
                ) {
                    Text(
                        text = strings.close,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {}
    )
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
