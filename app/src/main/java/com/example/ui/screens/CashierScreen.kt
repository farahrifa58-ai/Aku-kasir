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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.example.ui.components.ProductPhotoBox
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.CashierUiState
import com.example.util.PosFormatters

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
    onBackToDashboard: () -> Unit = {},
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
            onBackClick = onBackToDashboard
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
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        onEditDecimalLine = { decimalPickerProduct = it },
                        onCustomerNameChange = onCustomerNameChange,
                        onDiscountChange = onDiscountChange,
                        onTaxChange = onTaxChange,
                        onPaymentMethodChange = onPaymentMethodChange,
                        onAmountPaidChange = onAmountPaidChange,
                        onFillExactCash = onFillExactCash,
                        onCompleteCheckout = onCompleteCheckout
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
                    sheetState = sheetState
                ) {
                    CartAndCheckoutContent(
                        store = store,
                        cashierState = cashierState,
                        totals = totals,
                        onSetCartQuantity = onSetCartQuantity,
                        onRemoveCartItem = onRemoveCartItem,
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

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "${strings.cartTitle} (${cashierState.cart.size})",
            style = MaterialTheme.typography.titleLarge
        )

        if (cashierState.cart.isEmpty()) {
            Text(
                text = strings.emptyCartHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            cashierState.cart.forEach { line ->
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
            }

            HorizontalDivider()

            // A. Optional Customer Name
            OutlinedTextField(
                value = cashierState.customerName,
                onValueChange = onCustomerNameChange,
                label = { Text(strings.customerNameLabel) },
                placeholder = { Text(strings.customerNameHint) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // B. Optional Discount (None / % / Rp)
            Text(strings.discountLabel, style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = cashierState.discountType == DiscountType.NONE,
                    onClick = { onDiscountChange(DiscountType.NONE, "") },
                    label = { Text(strings.discountNone) }
                )
                FilterChip(
                    selected = cashierState.discountType == DiscountType.PERCENT,
                    onClick = { onDiscountChange(DiscountType.PERCENT, cashierState.discountInput) },
                    label = { Text("%") }
                )
                FilterChip(
                    selected = cashierState.discountType == DiscountType.AMOUNT,
                    onClick = { onDiscountChange(DiscountType.AMOUNT, cashierState.discountInput) },
                    label = { Text("Rp") }
                )
            }
            if (cashierState.discountType != DiscountType.NONE) {
                OutlinedTextField(
                    value = cashierState.discountInput,
                    onValueChange = { onDiscountChange(cashierState.discountType, it) },
                    label = {
                        Text(
                            if (cashierState.discountType == DiscountType.PERCENT) strings.discountPercent else strings.discountAmount
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // C. PPN (None / Auto % / Manual Rp)
            Text(strings.taxLabel, style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = cashierState.taxType == TaxType.NONE,
                    onClick = { onTaxChange(TaxType.NONE, "") },
                    label = { Text(strings.taxNone) }
                )
                FilterChip(
                    selected = cashierState.taxType == TaxType.AUTO,
                    onClick = { onTaxChange(TaxType.AUTO, "") },
                    label = { Text("${strings.taxAuto} (${PosFormatters.formatQuantity(store.defaultTaxPercent)}%)") }
                )
                FilterChip(
                    selected = cashierState.taxType == TaxType.MANUAL,
                    onClick = { onTaxChange(TaxType.MANUAL, cashierState.manualTaxInput) },
                    label = { Text(strings.taxManual) }
                )
            }
            if (cashierState.taxType == TaxType.MANUAL) {
                OutlinedTextField(
                    value = cashierState.manualTaxInput,
                    onValueChange = { onTaxChange(TaxType.MANUAL, it) },
                    label = { Text(strings.taxManual) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // D. Payment Method
            Text(strings.paymentMethodLabel, style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    PaymentMethod.CASH to strings.methodCash,
                    PaymentMethod.BANK_TRANSFER to strings.methodBankTransfer,
                    PaymentMethod.QRIS to strings.methodQris,
                    PaymentMethod.OTHER to strings.methodOther
                ).forEach { (method, label) ->
                    FilterChip(
                        selected = cashierState.paymentMethod == method,
                        onClick = { onPaymentMethodChange(method) },
                        label = { Text(label) }
                    )
                }
            }

            // E. Totals Summary Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SummaryLine(strings.subtotalLabel, PosFormatters.formatRupiah(totals.subtotal))
                    if (totals.discountAmount > 0.0) {
                        SummaryLine(strings.discountLabel, "-${PosFormatters.formatRupiah(totals.discountAmount)}")
                    }
                    if (totals.taxAmount > 0.0) {
                        SummaryLine(strings.taxLabel, PosFormatters.formatRupiah(totals.taxAmount))
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.grandTotalLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = PosFormatters.formatRupiah(totals.grandTotal),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Payment Input & Change
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = cashierState.amountPaidInput,
                    onValueChange = onAmountPaidChange,
                    label = { Text(strings.amountReceivedLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cashier_amount_paid_input")
                )
                FilledTonalButton(
                    onClick = onFillExactCash,
                    modifier = Modifier.height(54.dp)
                ) {
                    Text(strings.exactCashButton)
                }
            }

            if (cashierState.paymentMethod == PaymentMethod.CASH) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.changeLabel, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = PosFormatters.formatRupiah(totals.changeAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreen
                    )
                }
            }

            Button(
                onClick = onCompleteCheckout,
                enabled = !cashierState.isSubmitting && cashierState.cart.isNotEmpty(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
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
                        text = strings.completeTransactionButton,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
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
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = line.product.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${PosFormatters.formatRupiah(line.product.sellPrice)}/${line.product.unit} • ${PosFormatters.formatRupiah(line.subtotal)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrement, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Remove, contentDescription = "Decrease")
            }
            Text(
                text = PosFormatters.formatQuantity(line.quantity, line.product.unit),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clickable(enabled = line.product.supportsDecimal, onClick = onTapQty)
                    .padding(horizontal = 6.dp)
            )
            IconButton(onClick = onIncrement, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = "Increase")
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove", tint = StatusError)
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
