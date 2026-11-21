package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartLine
import com.example.data.model.CheckoutTotals
import com.example.data.model.DiscountType
import com.example.data.model.PaymentMethod
import com.example.data.model.PosCalculator
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ProductUnit
import com.example.data.model.StoreSettings
import com.example.data.model.TaxType
import com.example.ui.components.BackupStatusHeaderPill
import com.example.ui.components.ProductPhotoBox
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.CashierUiState
import com.example.util.BackupHealthStatus
import com.example.util.PosFormatters
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierScreen(
    products: List<Product>,
    categories: List<ProductCategory>,
    store: StoreSettings,
    cashierState: CashierUiState,
    totals: CheckoutTotals,
    onAddProduct: (Product, Double) -> Unit,
    onSetCartQuantity: (String, Double) -> Unit,
    onRemoveCartItem: (String) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onDiscountChange: (DiscountType, String) -> Unit,
    onTaxChange: (TaxType, String) -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onAmountPaidChange: (String) -> Unit,
    onFillExactCash: () -> Unit,
    onCompleteCheckout: () -> Unit,
    onClearCart: () -> Unit = {},
    onBackToDashboard: () -> Unit = {},
    backupStatus: BackupHealthStatus = BackupHealthStatus.NEVER_OR_OVERDUE,
    onBackupStatusClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var decimalPickerProduct by remember { mutableStateOf<Product?>(null) }
    var showCartSheet by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { prod ->
            val matchesSearch = searchQuery.isBlank() ||
                prod.name.contains(searchQuery, ignoreCase = true) ||
                prod.sku.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == null || prod.category.equals(selectedCategory, ignoreCase = true)
            matchesSearch && matchesCat
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = strings.menuCashier,
            onBackClick = onBackToDashboard,
            centeredTitle = false,
            trailingContent = {
                BackupStatusHeaderPill(
                    status = backupStatus,
                    onClick = onBackupStatusClick,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        )

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val isWideScreen = maxWidth >= 760.dp

            if (isWideScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // Left: Product catalog
                ProductCatalogPane(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it },
                    products = filteredProducts,
                    onProductTap = { prod ->
                        if (prod.supportsDecimal) {
                            decimalPickerProduct = prod
                        } else {
                            onAddProduct(prod, 1.0)
                        }
                    },
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight()
                )

                // Right: Cart & Payment panel
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    CartAndCheckoutContent(
                        store = store,
                        cashierState = cashierState,
                        totals = totals,
                        onSetCartQuantity = onSetCartQuantity,
                        onRemoveCartItem = onRemoveCartItem,
                        onClearCart = onClearCart,
                        onEditDecimalLine = { decimalPickerProduct = it },
                        onCustomerNameChange = onCustomerNameChange,
                        onDiscountChange = onDiscountChange,
                        onTaxChange = onTaxChange,
                        onPaymentMethodChange = onPaymentMethodChange,
                        onAmountPaidChange = onAmountPaidChange,
                        onFillExactCash = onFillExactCash,
                        onCompleteCheckout = onCompleteCheckout,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // Mobile / Compact layout
            Box(modifier = Modifier.fillMaxSize()) {
                ProductCatalogPane(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it },
                    products = filteredProducts,
                    onProductTap = { prod ->
                        if (prod.supportsDecimal) {
                            decimalPickerProduct = prod
                        } else {
                            onAddProduct(prod, 1.0)
                        }
                    },
                    bottomContentPadding = if (cashierState.cart.isNotEmpty()) 96.dp else 20.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )

                if (cashierState.cart.isNotEmpty()) {
                    Surface(
                        onClick = { showCartSheet = true },
                        shape = RoundedCornerShape(20.dp),
                        color = BrandBlue,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            .fillMaxWidth()
                            .testTag("cashier_cart_bar")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = BrandGreen) {
                                            Text("${cashierState.cart.size}", color = Color.White)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ShoppingCart,
                                        contentDescription = strings.cartTitle,
                                        tint = Color.White
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${cashierState.cart.size} ${strings.cartTitle}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                    Text(
                                        text = PosFormatters.formatRupiah(totals.grandTotal),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Text(
                                text = strings.viewCartButton,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            if (showCartSheet) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { showCartSheet = false },
                    sheetState = sheetState,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    CartAndCheckoutContent(
                        store = store,
                        cashierState = cashierState,
                        totals = totals,
                        onSetCartQuantity = onSetCartQuantity,
                        onRemoveCartItem = onRemoveCartItem,
                        onClearCart = {
                            onClearCart()
                            showCartSheet = false
                        },
                        onEditDecimalLine = { decimalPickerProduct = it },
                        onCustomerNameChange = onCustomerNameChange,
                        onDiscountChange = onDiscountChange,
                        onTaxChange = onTaxChange,
                        onPaymentMethodChange = onPaymentMethodChange,
                        onAmountPaidChange = onAmountPaidChange,
                        onFillExactCash = onFillExactCash,
                        onCompleteCheckout = {
                            onCompleteCheckout()
                            showCartSheet = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        }
    }

    val pickerProd = decimalPickerProduct
    if (pickerProd != null) {
        DecimalQuantityDialog(
            product = pickerProd,
            onDismiss = { decimalPickerProduct = null },
            onConfirm = { qty ->
                onAddProduct(pickerProd, qty)
                decimalPickerProduct = null
            }
        )
    }
}

@Composable
private fun ProductCatalogPane(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    categories: List<ProductCategory>,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    products: List<Product>,
    onProductTap: (Product) -> Unit,
    bottomContentPadding: androidx.compose.ui.unit.Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text(strings.searchProductHint) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("cashier_search_input")
        )

        // Category chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onSelectCategory(null) },
                label = { Text(strings.allProducts) }
            )
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat.name,
                    onClick = { onSelectCategory(cat.name) },
                    label = { Text(cat.name) }
                )
            }
        }

        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.noProductsFound,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 152.dp),
                contentPadding = PaddingValues(bottom = bottomContentPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(products, key = { it.id }) { prod ->
                    CashierProductCard(
                        product = prod,
                        onClick = { onProductTap(prod) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CashierProductCard(
    product: Product,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val enabled = !product.isOutOfStock
    Card(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 2.dp else 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                ProductPhotoBox(
                    photoData = product.photoData,
                    contentDescription = product.name,
                    size = 54.dp,
                    cornerRadius = 12.dp
                )
                val badgeColor = when {
                    product.isOutOfStock -> StatusError
                    product.isLowStock -> StatusWarning
                    else -> BrandGreen
                }
                val badgeText = when {
                    product.isOutOfStock -> strings.outOfStockBadge
                    else -> PosFormatters.formatQuantity(product.stock, product.unit)
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${PosFormatters.formatRupiah(product.sellPrice)} / ${product.unit}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DecimalQuantityDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val strings = LocalAppStrings.current
    var qtyInput by remember { mutableStateOf("1.0") }
    val parsedQty = qtyInput.replace(",", ".").toDoubleOrNull() ?: 0.0
    val estimated = PosCalculator.roundCurrency(product.sellPrice * parsedQty.coerceAtLeast(0.0))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${strings.selectQuantityTitle} (${product.unit})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${product.name} • ${PosFormatters.formatRupiah(product.sellPrice)}/${product.unit}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = strings.quickAmountLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProductUnit.decimalPresets.forEach { preset ->
                        val selected = kotlin.math.abs(parsedQty - preset) < 0.001
                        FilterChip(
                            selected = selected,
                            onClick = { qtyInput = preset.toString() },
                            label = { Text(preset.toString()) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                OutlinedTextField(
                    value = qtyInput,
                    onValueChange = { qtyInput = it },
                    label = { Text(strings.manualDecimalLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.estimatedSubtotal, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = PosFormatters.formatRupiah(estimated),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (parsedQty > 0.0) onConfirm(parsedQty) },
                enabled = parsedQty > 0.0
            ) {
                Text(strings.addToCart)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
private fun CartAndCheckoutContent(
    store: StoreSettings,
    cashierState: CashierUiState,
    totals: CheckoutTotals,
    onSetCartQuantity: (String, Double) -> Unit,
    onRemoveCartItem: (String) -> Unit,
    onClearCart: () -> Unit,
    onEditDecimalLine: (Product) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onDiscountChange: (DiscountType, String) -> Unit,
    onTaxChange: (TaxType, String) -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onAmountPaidChange: (String) -> Unit,
    onFillExactCash: () -> Unit,
    onCompleteCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val parsedAmountPaid = cashierState.amountPaidInput.replace(",", ".").toDoubleOrNull() ?: 0.0
    val shortfall = (totals.grandTotal - parsedAmountPaid).coerceAtLeast(0.0)
    val quickCashOptions = remember(totals.grandTotal) {
        buildQuickCashDenominations(totals.grandTotal)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        // 1. Sleek Header with Cart Icon Badge, Item Count Pill & Clear Cart Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(BrandBlue, Color(0xFF1D4ED8))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ShoppingCart,
                        contentDescription = strings.cartTitle,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = strings.cartTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (cashierState.cart.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = BrandBlue.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "${cashierState.cart.size} item",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = store.storeName.ifBlank { "ADA KASIR" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (cashierState.cart.isNotEmpty()) {
                TextButton(
                    onClick = onClearCart,
                    modifier = Modifier.testTag("checkout_clear_cart_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = StatusError,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = strings.clearCartButton,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = StatusError
                    )
                }
            }
        }

        if (cashierState.cart.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = strings.emptyCartHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // 2. Scrollable Modern Card Sections
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // SECTION A: Cart Items Card (with Product Thumbnails & Modern Pill Steppers)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        cashierState.cart.forEachIndexed { index, line ->
                            CartLineRow(
                                line = line,
                                onIncrement = {
                                    val step = if (line.product.supportsDecimal) 0.25 else 1.0
                                    onSetCartQuantity(line.product.id, line.quantity + step)
                                },
                                onDecrement = {
                                    val step = if (line.product.supportsDecimal) 0.25 else 1.0
                                    onSetCartQuantity(line.product.id, line.quantity - step)
                                },
                                onTapQty = {
                                    if (line.product.supportsDecimal) onEditDecimalLine(line.product)
                                },
                                onRemove = { onRemoveCartItem(line.product.id) }
                            )
                            if (index < cashierState.cart.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }
                }

                // SECTION B: Pelanggan, Diskon & PPN Card (Full-width segmented pills — zero cut-off!)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Customer Name Input
                        OutlinedTextField(
                            value = cashierState.customerName,
                            onValueChange = onCustomerNameChange,
                            label = { Text(strings.customerNameLabel) },
                            placeholder = { Text(strings.customerNameHint) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.PersonOutline,
                                    contentDescription = null,
                                    tint = BrandBlue
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Discount Segmented Pill Bar
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalOffer,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = strings.discountLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            CheckoutSegmentedBar(
                                options = listOf(
                                    DiscountType.NONE to (if (store.language == "en") "None" else "Tanpa"),
                                    DiscountType.PERCENT to "% Persen",
                                    DiscountType.AMOUNT to "Rp Nominal"
                                ),
                                selected = cashierState.discountType,
                                activeColor = BrandGreen,
                                onSelect = { type ->
                                    val nextInput = if (type == DiscountType.NONE) "" else cashierState.discountInput
                                    onDiscountChange(type, nextInput)
                                }
                            )
                            if (cashierState.discountType != DiscountType.NONE) {
                                OutlinedTextField(
                                    value = cashierState.discountInput,
                                    onValueChange = { onDiscountChange(cashierState.discountType, it) },
                                    label = {
                                        Text(
                                            if (cashierState.discountType == DiscountType.PERCENT) strings.discountPercent
                                            else strings.discountAmount
                                        )
                                    },
                                    prefix = if (cashierState.discountType == DiscountType.AMOUNT) {
                                        { Text("Rp ", fontWeight = FontWeight.SemiBold) }
                                    } else null,
                                    suffix = if (cashierState.discountType == DiscountType.PERCENT) {
                                        { Text("%", fontWeight = FontWeight.SemiBold) }
                                    } else null,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Tax (PPN) Segmented Pill Bar — Never clips!
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ReceiptLong,
                                    contentDescription = null,
                                    tint = BrandBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = strings.taxLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            val autoTaxLabel = "Auto ${PosFormatters.formatQuantity(store.defaultTaxPercent)}%"
                            CheckoutSegmentedBar(
                                options = listOf(
                                    TaxType.NONE to (if (store.language == "en") "None" else "Tanpa"),
                                    TaxType.AUTO to autoTaxLabel,
                                    TaxType.MANUAL to "Manual Rp"
                                ),
                                selected = cashierState.taxType,
                                activeColor = BrandBlue,
                                onSelect = { type ->
                                    val nextInput = if (type == TaxType.MANUAL) cashierState.manualTaxInput else ""
                                    onTaxChange(type, nextInput)
                                }
                            )
                            if (cashierState.taxType == TaxType.MANUAL) {
                                OutlinedTextField(
                                    value = cashierState.manualTaxInput,
                                    onValueChange = { onTaxChange(TaxType.MANUAL, it) },
                                    label = { Text(strings.taxManual) },
                                    prefix = { Text("Rp ", fontWeight = FontWeight.SemiBold) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // SECTION C: Metode Pembayaran 2x2 Modern Card Grid (Never clips!)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                            RoundedCornerShape(20.dp)
                        )
                        .testTag("checkout_payment_grid")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.paymentMethodLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        val methods = listOf(
                            Triple(PaymentMethod.CASH, strings.methodCash, Icons.Rounded.Payments),
                            Triple(PaymentMethod.BANK_TRANSFER, strings.methodBankTransfer, Icons.Rounded.AccountBalance),
                            Triple(PaymentMethod.QRIS, strings.methodQris, Icons.Rounded.QrCode2),
                            Triple(PaymentMethod.OTHER, strings.methodOther, Icons.Rounded.CreditCard)
                        )
                        methods.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { (method, label, icon) ->
                                    PaymentMethodCardOption(
                                        label = label,
                                        icon = icon,
                                        selected = cashierState.paymentMethod == method,
                                        onClick = { onPaymentMethodChange(method) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("payment_method_${method.code.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION D: Ringkasan Pembayaran (Elevated Highlight Card)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            BrandBlue.copy(alpha = 0.22f),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = strings.paymentSummaryTitle,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue
                        )
                        SummaryLine(
                            label = strings.subtotalLabel,
                            value = PosFormatters.formatRupiah(totals.subtotal)
                        )
                        if (totals.discountAmount > 0.0) {
                            SummaryLine(
                                label = strings.discountLabel,
                                value = "-${PosFormatters.formatRupiah(totals.discountAmount)}",
                                valueColor = BrandGreen
                            )
                        }
                        if (totals.taxAmount > 0.0) {
                            SummaryLine(
                                label = strings.taxLabel,
                                value = "+${PosFormatters.formatRupiah(totals.taxAmount)}"
                            )
                        }
                        HorizontalDivider(color = BrandBlue.copy(alpha = 0.18f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.grandTotalLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = PosFormatters.formatRupiah(totals.grandTotal),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = BrandBlue
                            )
                        }
                    }
                }

                // SECTION E: Pembayaran Tunai + Nominal Cepat OR Info Non-Tunai
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (cashierState.paymentMethod == PaymentMethod.CASH) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = cashierState.amountPaidInput,
                                    onValueChange = onAmountPaidChange,
                                    label = { Text(strings.amountReceivedLabel) },
                                    prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                                    textStyle = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("cashier_amount_paid_input")
                                )
                                FilledTonalButton(
                                    onClick = onFillExactCash,
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    modifier = Modifier.height(54.dp)
                                ) {
                                    Text(
                                        text = strings.exactCashButton,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Quick Cash Denomination Chips
                            if (quickCashOptions.isNotEmpty()) {
                                Text(
                                    text = strings.quickCashLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .testTag("checkout_quick_cash_row"),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    quickCashOptions.forEach { amount ->
                                        val isActive = kotlin.math.abs(parsedAmountPaid - amount.toDouble()) < 0.5
                                        Surface(
                                            onClick = { onAmountPaidChange(amount.toString()) },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isActive) BrandBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.border(
                                                width = 1.dp,
                                                color = if (isActive) BrandBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        ) {
                                            Text(
                                                text = PosFormatters.formatRupiah(amount.toDouble()),
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Change or Shortfall Highlight Banner
                            val isShort = parsedAmountPaid > 0.0 && shortfall > 0.01
                            val bannerColor = if (isShort) StatusWarning else BrandGreen
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = bannerColor.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, bannerColor.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isShort) Icons.Rounded.WarningAmber else Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = bannerColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (isShort) strings.cashShortfallLabel else strings.changeLabel,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = PosFormatters.formatRupiah(if (isShort) shortfall else totals.changeAmount),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = bannerColor
                                    )
                                }
                            }
                        } else {
                            // Non-Cash auto-exact payment banner
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = BrandBlue.copy(alpha = 0.10f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = BrandBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = strings.nonCashAutoExactNote,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
            }

            // 3. Sticky Pinned Checkout Action Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 10.dp,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = onCompleteCheckout,
                        enabled = !cashierState.isSubmitting && cashierState.cart.isNotEmpty(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("cashier_complete_tx_button")
                    ) {
                        if (cashierState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${strings.completeTransactionButton} • ${PosFormatters.formatRupiah(totals.grandTotal)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildQuickCashDenominations(grandTotal: Double): List<Long> {
    if (grandTotal <= 0.0) return emptyList()
    val totalLong = ceil(grandTotal).toLong()
    val candidates = linkedSetOf<Long>()
    val standardBills = listOf(10_000L, 20_000L, 50_000L, 100_000L, 150_000L, 200_000L, 500_000L)
    val roundUp5k = ((totalLong + 4_999L) / 5_000L) * 5_000L
    val roundUp10k = ((totalLong + 9_999L) / 10_000L) * 10_000L
    val roundUp50k = ((totalLong + 49_999L) / 50_000L) * 50_000L
    if (roundUp5k > totalLong) candidates += roundUp5k
    if (roundUp10k > totalLong) candidates += roundUp10k
    if (roundUp50k > totalLong) candidates += roundUp50k
    standardBills.filter { it > totalLong }.forEach { candidates += it }
    return candidates.sorted().take(4)
}

@Composable
private fun <T> CheckoutSegmentedBar(
    options: List<Pair<T, String>>,
    selected: T,
    activeColor: Color,
    onSelect: (T) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { (value, label) ->
                val isSelected = selected == value
                Surface(
                    onClick = { onSelect(value) },
                    shape = RoundedCornerShape(11.dp),
                    color = if (isSelected) activeColor else Color.Transparent,
                    shadowElevation = if (isSelected) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodCardOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) BrandBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier
            .height(52.dp)
            .border(
                width = if (selected) 1.8.dp else 1.dp,
                color = if (selected) BrandBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) BrandBlue else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CartLineRow(
    line: CartLine,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onTapQty: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProductPhotoBox(
            photoData = line.product.photoData,
            contentDescription = line.product.name,
            size = 52.dp,
            cornerRadius = 14.dp
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = line.product.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${PosFormatters.formatRupiah(line.product.sellPrice)} / ${line.product.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = PosFormatters.formatRupiah(line.subtotal),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = BrandBlue
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Modern Pill Stepper
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                        RoundedCornerShape(50)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable(onClick = onDecrement),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = "Decrease",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = PosFormatters.formatQuantity(line.quantity, line.product.unit),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clickable(enabled = line.product.supportsDecimal, onClick = onTapQty)
                                .padding(horizontal = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(BrandBlue)
                                .clickable(onClick = onIncrement),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Increase",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Remove",
                        tint = StatusError,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = valueColor
        )
    }
}
