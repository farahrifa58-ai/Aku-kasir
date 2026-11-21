package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PosTransaction
import com.example.data.model.Product
import com.example.data.model.StoreSettings
import com.example.data.model.TransactionStatus
import com.example.ui.components.StoreProfileAvatar
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.AppMenu
import com.example.util.PosFormatters
import java.util.Calendar

private data class LauncherMenuTile(
    val menu: AppMenu,
    val label: String,
    val icon: ImageVector,
    val tag: String,
    val startColor: Color,
    val endColor: Color
)

@Composable
fun HomeMenuScreen(
    store: StoreSettings,
    userDisplayName: String,
    products: List<Product>,
    transactions: List<PosTransaction>,
    onSelectMenu: (AppMenu) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Subtle animated supermarket backdrop motion for cartoon/animated liveliness
    val infiniteTransition = rememberInfiniteTransition(label = "supermarket_bg_motion")
    val bgScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_scale"
    )

    var animateTilesIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateTilesIn = true
    }

    val startOfDayMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayCompleted = remember(transactions, startOfDayMillis) {
        transactions.filter {
            TransactionStatus.fromCode(it.status) == TransactionStatus.COMPLETED &&
                it.transactionMillis >= startOfDayMillis
        }
    }
    val todaySales = remember(todayCompleted) { todayCompleted.sumOf { it.totalAmount } }
    val lowStockCount = remember(products) { products.count { it.isLowStock || it.isOutOfStock } }

    val menuTiles = listOf(
        LauncherMenuTile(
            menu = AppMenu.DASHBOARD,
            label = strings.menuDashboard,
            icon = Icons.Outlined.GridView,
            tag = "menu_dashboard",
            startColor = Color(0xFF4F8FF7),
            endColor = Color(0xFF1D4ED8)
        ),
        LauncherMenuTile(
            menu = AppMenu.CASHIER,
            label = strings.menuCashier,
            icon = Icons.Outlined.ShoppingCart,
            tag = "menu_cashier",
            startColor = Color(0xFFFBBF24),
            endColor = Color(0xFFEA580C)
        ),
        LauncherMenuTile(
            menu = AppMenu.PRODUCTS,
            label = strings.menuProducts,
            icon = Icons.Outlined.Inventory2,
            tag = "menu_products",
            startColor = Color(0xFF34D399),
            endColor = Color(0xFF059669)
        ),
        LauncherMenuTile(
            menu = AppMenu.HISTORY,
            label = strings.menuHistory,
            icon = Icons.Rounded.History,
            tag = "menu_history",
            startColor = Color(0xFFA78BFA),
            endColor = Color(0xFF6D28D9)
        ),
        LauncherMenuTile(
            menu = AppMenu.SETTINGS,
            label = strings.menuSettings,
            icon = Icons.Outlined.Settings,
            tag = "menu_settings",
            startColor = Color(0xFFFB7185),
            endColor = Color(0xFFBE123C)
        )
    )

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Full-screen Cartoon / Animated Supermarket Background
        Image(
            painter = painterResource(id = R.drawable.img_supermarket_bg),
            contentDescription = "Supermarket cartoon illustration background",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = bgScale
                    scaleY = bgScale
                }
                .testTag("home_supermarket_bg")
        )

        // Balanced gradient scrim for crisp foreground legibility in Light & Dark themes
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0xFF0B1120).copy(alpha = 0.72f),
                                Color(0xFF0B1120).copy(alpha = 0.56f),
                                Color(0xFF0B1120).copy(alpha = 0.78f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.42f),
                                Color(0xFFF8FAFC).copy(alpha = 0.24f),
                                Color.White.copy(alpha = 0.48f)
                            )
                        }
                    )
                )
        )

        // 2. Foreground Content: Top Store Bar + Centered Phone-Style 5-Menu Grid + Brand Footer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Frosted Store Summary Pill
            val greeting = PosFormatters.greetingForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY), strings)
            val displayOwner = store.ownerName.ifBlank { userDisplayName }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                tonalElevation = 4.dp,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.30f),
                        RoundedCornerShape(22.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StoreProfileAvatar(
                        photoData = store.photoData,
                        storeIcon = store.storeIcon,
                        storeIconBgColor = store.storeIconBgColor,
                        size = 48.dp,
                        onClick = { onSelectMenu(AppMenu.SETTINGS) }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = store.storeName.ifBlank { "Toko ADA KASIR" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (displayOwner.isNotBlank()) "$greeting, $displayOwner" else greeting,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = BrandGreen.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "${PosFormatters.formatRupiah(todaySales)} • ${todayCompleted.size}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            if (lowStockCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = StatusWarning.copy(alpha = 0.18f),
                                    modifier = Modifier.clickable { onSelectMenu(AppMenu.PRODUCTS) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.NotificationsActive,
                                            contentDescription = strings.stockAlertsTitle,
                                            tint = StatusWarning,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "$lowStockCount",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Center Area: 5 Colorful Phone-Launcher Menu Squares Directly Outside in the Center
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val tileWidth = minOf(112.dp, (maxWidth - 36.dp) / 3)
                val iconBoxSize = (tileWidth * 0.78f).coerceIn(66.dp, 86.dp)
                val tileScale by animateFloatAsState(
                    targetValue = if (animateTilesIn) 1f else 0.88f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "home_menu_tiles_scale"
                )

                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.78f else 0.78f),
                    tonalElevation = 4.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .widthIn(max = 460.dp)
                        .graphicsLayer {
                            scaleX = tileScale
                            scaleY = tileScale
                        }
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = if (isDark) 0.14f else 0.65f),
                            shape = RoundedCornerShape(30.dp)
                        )
                        .testTag("home_center_menu_grid")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Row 1: 3 colorful launcher boxes (Dashboard, Kasir, Produk)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.Top
                        ) {
                            menuTiles.take(3).forEach { tile ->
                                PhoneStyleMenuTile(
                                    tile = tile,
                                    tileWidth = tileWidth,
                                    iconBoxSize = iconBoxSize,
                                    onClick = { onSelectMenu(tile.menu) }
                                )
                            }
                        }

                        // Row 2: 2 colorful launcher boxes centered neatly (Riwayat, Pengaturan)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.Top
                        ) {
                            menuTiles.drop(3).forEach { tile ->
                                PhoneStyleMenuTile(
                                    tile = tile,
                                    tileWidth = tileWidth,
                                    iconBoxSize = iconBoxSize,
                                    onClick = { onSelectMenu(tile.menu) }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom subtle brand pill
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                tonalElevation = 2.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    RoundedCornerShape(50)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(BrandBlue, BrandGreen))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PointOfSale,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = "${strings.appName} • ${strings.developerBy}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneStyleMenuTile(
    tile: LauncherMenuTile,
    tileWidth: Dp,
    iconBoxSize: Dp,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(tileWidth)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(vertical = 4.dp)
            .testTag(tile.tag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(iconBoxSize)
                .shadow(8.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(tile.startColor, tile.endColor)
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = Color.White.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(22.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tile.icon,
                contentDescription = tile.label,
                tint = Color.White,
                modifier = Modifier.size(iconBoxSize * 0.46f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = tile.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
