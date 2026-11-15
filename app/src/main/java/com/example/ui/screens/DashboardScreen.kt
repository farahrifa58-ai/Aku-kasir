package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DashboardPeriod
import com.example.data.model.PosTransaction
import com.example.data.model.Product
import com.example.data.model.StoreSettings
import com.example.data.model.TransactionStatus
import com.example.ui.components.ProductPhotoBox
import com.example.ui.components.StoreProfileAvatar
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueDark
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.AppMenu
import com.example.util.AnalyticsCalculator
import com.example.util.ChartBucket
import com.example.util.PosFormatters
import com.example.util.TopProductRank
import java.util.Calendar
import kotlin.math.abs

private data class DashboardMenuTile(
    val menu: AppMenu,
    val label: String,
    val icon: ImageVector,
    val tag: String,
    val startColor: Color,
    val endColor: Color
)

@Composable
fun DashboardScreen(
    store: StoreSettings,
    userDisplayName: String,
    products: List<Product>,
    transactions: List<PosTransaction>,
    selectedPeriod: DashboardPeriod,
    onSelectPeriod: (DashboardPeriod) -> Unit,
    onSelectMenu: (AppMenu) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onSelectTransaction: (PosTransaction) -> Unit,
    isMonitorMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val language = store.language
    var showAlertsDialog by remember { mutableStateOf(false) }

    val lowOrEmptyProducts = remember(products) {
        products.filter { it.isOutOfStock || it.isLowStock }
            .sortedBy { it.stock }
    }

    val todayStats = remember(transactions) {
        val todayBounds = AnalyticsCalculator.periodBounds(DashboardPeriod.TODAY).first
        val todayCompleted = transactions.filter {
            it.status == TransactionStatus.COMPLETED.code &&
                it.transactionMillis in todayBounds.first..todayBounds.second
        }
        todayCompleted.sumOf { it.totalAmount } to todayCompleted.size
    }

    val photoLookup = remember(products) {
        products.associate { it.id to it.photoData }
    }

    val analytics = remember(transactions, photoLookup, selectedPeriod) {
        AnalyticsCalculator.computePeriodAnalytics(
            transactions = transactions,
            photoLookup = photoLookup,
            period = selectedPeriod
        )
    }

    val recentTransactions = remember(transactions) {
        transactions.take(5)
    }

    val greeting = remember(strings) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        PosFormatters.greetingForHour(hour, strings)
    }
    val displayUser = store.ownerName.ifBlank { userDisplayName.ifBlank { "Kasir" } }

    Column(modifier = modifier.fillMaxSize()) {
        // Pure seamless blue top area (no elevated header card/box seam)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandBlue)
                .statusBarsPadding()
                .testTag("blue_dashboard_header"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 1040.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "$greeting,",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = displayUser,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = if (isMonitorMode) strings.userModeMonitorLabel else strings.userModeCashierLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { showAlertsDialog = true },
                    modifier = Modifier.testTag("dashboard_notifications_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (lowOrEmptyProducts.isNotEmpty()) {
                                Badge(containerColor = StatusError) {
                                    Text("${lowOrEmptyProducts.size}", color = Color.White)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = strings.stockAlertsTitle,
                            tint = Color.White
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 1040.dp)
                    .fillMaxSize()
                    .testTag("dashboard_lazy_column"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Monitor Mode Read-Only Banner (when owner is observing another cashier email)
                if (isMonitorMode) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("monitor_mode_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Visibility,
                                    contentDescription = null,
                                    tint = BrandBlue
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.monitorBannerTitle,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = store.storeName.ifBlank { "Toko ADA KASIR" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // A. STORE INFO CARD
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            StoreProfileAvatar(
                                photoData = store.photoData,
                                size = 56.dp,
                                onClick = { onSelectMenu(AppMenu.SETTINGS) }
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = store.storeName.ifBlank { "Toko ADA KASIR" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (store.address.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = store.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (store.phone.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Phone,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = store.phone,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // B. RINGKASAN HARI INI: Total Penjualan Hari Ini & Jumlah Transaksi Hari Ini
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryMetricCard(
                            title = strings.todaySales,
                            value = PosFormatters.formatRupiah(todayStats.first),
                            icon = Icons.AutoMirrored.Rounded.TrendingUp,
                            accentColor = BrandGreen,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_today_sales_card")
                        )
                        SummaryMetricCard(
                            title = strings.todayTransactions,
                            value = "${todayStats.second} Trx",
                            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                            accentColor = BrandBlue,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_today_tx_card")
                        )
                    }
                }

                // C. MENU UTAMA DENGAN BACKGROUND TOKO ANIMASI (6 Ikon Kotak Bulat Berwarna 3x2)
                item {
                    val menuTiles = listOf(
                        DashboardMenuTile(
                            menu = AppMenu.CASHIER,
                            label = strings.menuCashier,
                            icon = Icons.Outlined.ShoppingCart,
                            tag = "menu_cashier",
                            startColor = Color(0xFFEF4444),
                            endColor = Color(0xFFC62828)
                        ),
                        DashboardMenuTile(
                            menu = AppMenu.PRODUCTS,
                            label = strings.menuProducts,
                            icon = Icons.Outlined.Inventory2,
                            tag = "menu_products",
                            startColor = Color(0xFF3B82F6),
                            endColor = Color(0xFF1D4ED8)
                        ),
                        DashboardMenuTile(
                            menu = AppMenu.SALES_REPORT,
                            label = strings.menuSalesReport,
                            icon = Icons.Rounded.Assessment,
                            tag = "menu_sales_report",
                            startColor = Color(0xFFF59E0B),
                            endColor = Color(0xFFD97706)
                        ),
                        DashboardMenuTile(
                            menu = AppMenu.EXPENSES,
                            label = strings.menuExpenses,
                            icon = Icons.Rounded.AccountBalanceWallet,
                            tag = "menu_expenses",
                            startColor = Color(0xFF10B981),
                            endColor = Color(0xFF047857)
                        ),
                        DashboardMenuTile(
                            menu = AppMenu.HISTORY,
                            label = strings.menuHistory,
                            icon = Icons.Rounded.History,
                            tag = "menu_history",
                            startColor = Color(0xFF8B5CF6),
                            endColor = Color(0xFF6D28D9)
                        ),
                        DashboardMenuTile(
                            menu = AppMenu.SETTINGS,
                            label = strings.menuSettings,
                            icon = Icons.Outlined.Settings,
                            tag = "menu_settings",
                            startColor = Color(0xFF64748B),
                            endColor = Color(0xFF334155)
                        )
                    )

                    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                    val bgTransition = rememberInfiniteTransition(label = "supermarket_bg_anim")
                    val bgScale by bgTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.06f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 7000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "supermarket_bg_scale"
                    )

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_menu_grid")
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Image(
                                painter = painterResource(id = R.drawable.img_supermarket_bg),
                                contentDescription = "Background toko ADA KASIR",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .matchParentSize()
                                    .graphicsLayer {
                                        scaleX = bgScale
                                        scaleY = bgScale
                                    }
                                    .testTag("dashboard_supermarket_bg")
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        if (isDark) Color(0xFF0F172A).copy(alpha = 0.78f)
                                        else Color.White.copy(alpha = 0.74f)
                                    )
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = strings.menuTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                // Row 1: Kasir, Produk, Laporan Penjualan
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    menuTiles.take(3).forEach { tile ->
                                        DashboardMenuIconTile(
                                            tile = tile,
                                            onClick = { onSelectMenu(tile.menu) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                // Row 2: Pengeluaran, Riwayat, Pengaturan
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    menuTiles.drop(3).forEach { tile ->
                                        DashboardMenuIconTile(
                                            tile = tile,
                                            onClick = { onSelectMenu(tile.menu) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            // C. GRAFIK TREN PENJUALAN
            item {
                SalesTrendChartCard(
                    analytics = analytics,
                    selectedPeriod = selectedPeriod,
                    onSelectPeriod = onSelectPeriod
                )
            }

            // D. PRODUK TERLARIS (Top 3, directly below chart, same period filter)
            item {
                TopProductsCard(
                    topProducts = analytics.topProducts,
                    selectedPeriod = selectedPeriod,
                    onSelectPeriod = onSelectPeriod
                )
            }

            // E. TRANSAKSI TERBARU
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.recentTransactionsTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(
                                onClick = onNavigateToHistory,
                                modifier = Modifier.testTag("dashboard_view_all_history")
                            ) {
                                Text(strings.viewAllHistory)
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (recentTransactions.isEmpty()) {
                            Text(
                                text = strings.noTransactionsYet,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            recentTransactions.forEachIndexed { index, tx ->
                                RecentTransactionRow(
                                    transaction = tx,
                                    language = language,
                                    onClick = { onSelectTransaction(tx) }
                                )
                                if (index < recentTransactions.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    if (showAlertsDialog) {
        AlertDialog(
            onDismissRequest = { showAlertsDialog = false },
            title = { Text(strings.stockAlertsTitle) },
            text = {
                if (lowOrEmptyProducts.isEmpty()) {
                    Text(strings.allStockHealthy)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        lowOrEmptyProducts.take(10).forEach { prod ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WarningAmber,
                                        contentDescription = null,
                                        tint = if (prod.isOutOfStock) StatusError else StatusWarning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = prod.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = if (prod.isOutOfStock) strings.outOfStockBadge else PosFormatters.formatQuantity(prod.stock, prod.unit),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (prod.isOutOfStock) StatusError else StatusWarning
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (lowOrEmptyProducts.isNotEmpty()) {
                    TextButton(onClick = {
                        showAlertsDialog = false
                        onNavigateToProducts()
                    }) {
                        Text(strings.menuProducts)
                    }
                }
                TextButton(onClick = { showAlertsDialog = false }) {
                    Text(strings.close)
                }
            }
        )
    }
}

@Composable
private fun DashboardMenuIconTile(
    tile: DashboardMenuTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .testTag(tile.tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .shadow(5.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(tile.startColor, tile.endColor))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tile.icon,
                contentDescription = tile.label,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
        Text(
            text = tile.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun SummaryMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PeriodFilterRow(
    selectedPeriod: DashboardPeriod,
    onSelectPeriod: (DashboardPeriod) -> Unit
) {
    val strings = LocalAppStrings.current
    val options = listOf(
        DashboardPeriod.TODAY to strings.periodToday,
        DashboardPeriod.SEVEN_DAYS to strings.period7Days,
        DashboardPeriod.THIRTY_DAYS to strings.period30Days,
        DashboardPeriod.THIS_MONTH to strings.periodThisMonth
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (period, label) ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onSelectPeriod(period) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun SalesTrendChartCard(
    analytics: com.example.util.PeriodAnalytics,
    selectedPeriod: DashboardPeriod,
    onSelectPeriod: (DashboardPeriod) -> Unit
) {
    val strings = LocalAppStrings.current
    val pct = analytics.percentChange ?: 0.0
    val isUp = pct >= 0.0
    val badgeBg = if (isUp) StatusSuccess.copy(alpha = 0.14f) else StatusError.copy(alpha = 0.14f)
    val badgeColor = if (isUp) StatusSuccess else StatusError
    val pctText = String.format(java.util.Locale.US, "%s%.1f%%", if (isUp) "+" else "-", abs(pct))

    var selectedBucketIndex by remember(analytics.buckets) { mutableIntStateOf(-1) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.salesTrendTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = strings.salesTrendSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isUp) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = pctText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
            }

            PeriodFilterRow(
                selectedPeriod = selectedPeriod,
                onSelectPeriod = onSelectPeriod
            )

            // Selected point tooltip
            val inspected: ChartBucket? = analytics.buckets.getOrNull(selectedBucketIndex)
            if (inspected != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${inspected.label} (${inspected.transactionCount} trx)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = PosFormatters.formatRupiah(inspected.totalSales),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            SmoothSalesLineChart(
                buckets = analytics.buckets,
                selectedIndex = selectedBucketIndex,
                onSelectIndex = { selectedBucketIndex = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            )

            // Time labels: 08:00, 10:00, 12:00, 14:00, 16:00, 18:00, 20:00, 22:00
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                analytics.buckets.forEach { bucket ->
                    Text(
                        text = bucket.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun SmoothSalesLineChart(
    buckets: List<ChartBucket>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val maxVal = remember(buckets) {
        (buckets.maxOfOrNull { it.totalSales } ?: 0.0).coerceAtLeast(1.0)
    }

    BoxWithConstraints(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(buckets) {
                    detectTapGestures { offset ->
                        if (buckets.isNotEmpty()) {
                            val step = size.width.toFloat() / (buckets.size - 1).coerceAtLeast(1)
                            val idx = ((offset.x + step / 2f) / step).toInt().coerceIn(0, buckets.lastIndex)
                            onSelectIndex(idx)
                        }
                    }
                }
        ) {
            val topPad = 14.dp.toPx()
            val bottomPad = 12.dp.toPx()
            val sidePad = 10.dp.toPx()
            val chartW = (size.width - sidePad * 2).coerceAtLeast(1f)
            val chartH = (size.height - topPad - bottomPad).coerceAtLeast(1f)

            // Thin horizontal gridlines
            val gridSteps = 4
            for (i in 0..gridSteps) {
                val y = topPad + chartH * (i.toFloat() / gridSteps.toFloat())
                drawLine(
                    color = gridColor,
                    start = Offset(sidePad, y),
                    end = Offset(size.width - sidePad, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (buckets.isEmpty()) return@Canvas

            val points = buckets.mapIndexed { index, bucket ->
                val x = if (buckets.size == 1) {
                    sidePad + chartW / 2f
                } else {
                    sidePad + chartW * (index.toFloat() / (buckets.size - 1).toFloat())
                }
                val ratio = if (buckets.all { it.totalSales == 0.0 }) {
                    0f
                } else {
                    (bucket.totalSales / maxVal).toFloat().coerceIn(0f, 1f)
                }
                val y = topPad + chartH * (1f - ratio)
                Offset(x, y)
            }

            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.lastIndex) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val ctrlX = (p0.x + p1.x) / 2f
                    cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                }
            }

            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(points.last().x, topPad + chartH)
                lineTo(points.first().x, topPad + chartH)
                close()
            }

            // Transparent gradient fill under line
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BrandBlue.copy(alpha = 0.20f),
                        BrandGreen.copy(alpha = 0.03f)
                    ),
                    startY = topPad,
                    endY = topPad + chartH
                )
            )

            // Smooth curved line with Blue -> Green horizontal gradient
            drawPath(
                path = linePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(BrandBlue, BrandGreen)
                ),
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Circular point markers
            points.forEachIndexed { idx, pt ->
                val ratio = idx.toFloat() / (points.lastIndex.coerceAtLeast(1)).toFloat()
                val markerColor = lerpColor(BrandBlue, BrandGreen, ratio)
                val isSelected = idx == selectedIndex
                if (isSelected) {
                    drawCircle(
                        color = markerColor.copy(alpha = 0.24f),
                        radius = 10.dp.toPx(),
                        center = pt
                    )
                }
                drawCircle(
                    color = surfaceColor,
                    radius = if (isSelected) 6.dp.toPx() else 4.5.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = markerColor,
                    radius = if (isSelected) 4.dp.toPx() else 3.dp.toPx(),
                    center = pt
                )
            }
        }
    }
}

private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
}

@Composable
private fun TopProductsCard(
    topProducts: List<TopProductRank>,
    selectedPeriod: DashboardPeriod,
    onSelectPeriod: (DashboardPeriod) -> Unit
) {
    val strings = LocalAppStrings.current
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.topProductsTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            PeriodFilterRow(
                selectedPeriod = selectedPeriod,
                onSelectPeriod = onSelectPeriod
            )

            if (topProducts.isEmpty()) {
                Text(
                    text = strings.noSalesDataYet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            } else {
                topProducts.forEachIndexed { idx, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val rankColor = when (item.rank) {
                            1 -> BrandBlue
                            2 -> BrandGreen
                            else -> MaterialTheme.colorScheme.secondary
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(rankColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#${item.rank}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = rankColor
                            )
                        }
                        ProductPhotoBox(
                            photoData = item.photoData,
                            contentDescription = item.productName,
                            size = 48.dp,
                            cornerRadius = 12.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.productName,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${PosFormatters.formatQuantity(item.totalQty, item.unit)} ${strings.soldLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = PosFormatters.formatRupiah(item.totalValue),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (idx < topProducts.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionRow(
    transaction: PosTransaction,
    language: String,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val statusColor = when (TransactionStatus.fromCode(transaction.status)) {
        TransactionStatus.COMPLETED -> StatusSuccess
        TransactionStatus.CANCELLED -> StatusError
        TransactionStatus.REFUNDED -> StatusWarning
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = transaction.transactionNumber,
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${PosFormatters.formatDateTime(transaction.transactionMillis, language)} • ${PosFormatters.paymentMethodLabel(transaction.paymentMethod, strings)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = PosFormatters.formatRupiah(transaction.totalAmount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = statusColor.copy(alpha = 0.14f)
            ) {
                Text(
                    text = PosFormatters.statusLabel(transaction.status, strings),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
