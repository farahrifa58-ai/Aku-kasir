package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ProductUnit
import com.example.ui.components.PhotoPickerSheetDialog
import com.example.ui.components.ProductPhotoBox
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusWarning
import com.example.util.PosFormatters

@Composable
fun ProductsScreen(
    userId: String,
    products: List<Product>,
    categories: List<ProductCategory>,
    onSaveProduct: (Product, () -> Unit) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onAddCategory: (String) -> Unit,
    onUpdateCategory: (String, String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onBackToDashboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showProductForm by remember { mutableStateOf(false) }
    var deletingProduct by remember { mutableStateOf<Product?>(null) }
    var showCategoryManager by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { prod ->
            val matchQuery = searchQuery.isBlank() ||
                prod.name.contains(searchQuery, ignoreCase = true) ||
                prod.sku.contains(searchQuery, ignoreCase = true)
            val matchCat = selectedCategory == null || prod.category.equals(selectedCategory, ignoreCase = true)
            matchQuery && matchCat
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SubScreenTopBar(
            title = strings.menuProducts,
            onBackClick = onBackToDashboard
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 1040.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Full-width Search Bar at the very top
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchProductHint) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("products_search_input")
                )

                // 2. Dropdown Category Filter + Manage Categories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { categoryDropdownExpanded = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedCategory ?: strings.allProducts,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.allProducts) },
                            onClick = {
                                selectedCategory = null
                                categoryDropdownExpanded = false
                            }
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategory = cat.name
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                FilledTonalButton(
                    onClick = { showCategoryManager = true },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Category, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.manageCategories)
                }
            }

            if (filteredProducts.isEmpty()) {
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
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { prod ->
                        ProductInventoryRowCard(
                            product = prod,
                            onEdit = {
                                editingProduct = prod
                                showProductForm = true
                            },
                            onDelete = { deletingProduct = prod }
                        )
                    }
                }
            }
        }
        }

        // Non-overlapping Bottom-Right Box Button Bar for Tambah Produk
        Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 1040.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            editingProduct = null
                            showProductForm = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("products_add_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.addProduct, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }

    if (showProductForm) {
        ProductFormDialog(
            userId = userId,
            initialProduct = editingProduct,
            categories = categories,
            onDismiss = { showProductForm = false },
            onAddCategory = onAddCategory,
            onSave = { product ->
                onSaveProduct(product) {
                    showProductForm = false
                }
            }
        )
    }

    val toDelete = deletingProduct
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { deletingProduct = null },
            title = { Text(strings.delete) },
            text = { Text("${strings.deleteProductConfirm} (${toDelete.name})") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProduct(toDelete.id)
                        deletingProduct = null
                    }
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingProduct = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showCategoryManager) {
        CategoryManagerDialog(
            categories = categories,
            onDismiss = { showCategoryManager = false },
            onAddCategory = onAddCategory,
            onUpdateCategory = onUpdateCategory,
            onDeleteCategory = onDeleteCategory
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductInventoryRowCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    Card(
        onClick = onEdit,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProductPhotoBox(
                photoData = product.photoData,
                contentDescription = product.name,
                size = 62.dp,
                cornerRadius = 14.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        product.category.takeIf { it.isNotBlank() },
                        product.sku.takeIf { it.isNotBlank() }
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${PosFormatters.formatRupiah(product.sellPrice)}/${product.unit}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )

                    val stockColor = when {
                        product.isOutOfStock -> StatusError
                        product.isLowStock -> StatusWarning
                        else -> BrandGreen
                    }
                    val stockLabel = when {
                        product.isOutOfStock -> strings.outOfStockBadge
                        product.isLowStock -> "${strings.lowStockBadge}: ${PosFormatters.formatQuantity(product.stock, product.unit)}"
                        else -> PosFormatters.formatQuantity(product.stock, product.unit)
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = stockColor.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (product.isLowStock || product.isOutOfStock) {
                                Icon(
                                    imageVector = Icons.Rounded.WarningAmber,
                                    contentDescription = null,
                                    tint = stockColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = stockLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = stockColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = strings.edit, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.delete, tint = StatusError, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ProductFormDialog(
    userId: String,
    initialProduct: Product?,
    categories: List<ProductCategory>,
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit,
    onSave: (Product) -> Unit
) {
    val strings = LocalAppStrings.current
    val isEdit = initialProduct != null

    var photoData by remember { mutableStateOf(initialProduct?.photoData ?: "") }
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var sku by remember { mutableStateOf(initialProduct?.sku ?: "") }
    var category by remember {
        mutableStateOf(initialProduct?.category?.ifBlank { null } ?: categories.firstOrNull()?.name ?: "Lainnya")
    }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: ProductUnit.PCS.label) }
    var costPrice by remember {
        mutableStateOf(if (initialProduct != null && initialProduct.costPrice > 0) initialProduct.costPrice.toLong().toString() else "")
    }
    var sellPrice by remember {
        mutableStateOf(if (initialProduct != null && initialProduct.sellPrice > 0) initialProduct.sellPrice.toLong().toString() else "")
    }
    var stock by remember {
        mutableStateOf(if (initialProduct != null) PosFormatters.formatQuantity(initialProduct.stock) else "")
    }
    var minStock by remember {
        mutableStateOf(if (initialProduct != null) PosFormatters.formatQuantity(initialProduct.minStock) else "5")
    }
    var description by remember { mutableStateOf(initialProduct?.description ?: "") }

    var catMenuExpanded by remember { mutableStateOf(false) }
    var unitMenuExpanded by remember { mutableStateOf(false) }
    var showPhotoPicker by remember { mutableStateOf(false) }
    var newCategoryPrompt by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isEdit) strings.editProduct else strings.addProduct,
                    style = MaterialTheme.typography.titleLarge
                )

                // 1. Product Photo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPhotoPicker = true }
                ) {
                    ProductPhotoBox(
                        photoData = photoData,
                        contentDescription = strings.productPhotoLabel,
                        size = 76.dp,
                        cornerRadius = 16.dp
                    )
                    Column {
                        Text(strings.productPhotoLabel, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${strings.pickFromGallery} / ${strings.takePhoto}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 2. Product Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.productNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. SKU
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text(strings.productSkuLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 4 & 5. Category & Unit Dropdowns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { catMenuExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = category,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = catMenuExpanded,
                            onDismissRequest = { catMenuExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        category = cat.name
                                        catMenuExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("+ ${strings.addCategory}") },
                                onClick = {
                                    catMenuExpanded = false
                                    newCategoryPrompt = true
                                }
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { unitMenuExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = unit,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = unitMenuExpanded,
                            onDismissRequest = { unitMenuExpanded = false }
                        ) {
                            ProductUnit.allLabels.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unit = u
                                        unitMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 6 & 7. Cost Price & Sell Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = { Text(strings.productCostPriceLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellPrice,
                        onValueChange = { sellPrice = it },
                        label = { Text(strings.productSellPriceLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 8 & 9. Stock & Min Stock
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it },
                        label = { Text(strings.productStockLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minStock,
                        onValueChange = { minStock = it },
                        label = { Text(strings.productMinStockLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 10. Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings.productDescLabel) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val prod = (initialProduct ?: Product(userId = userId)).copy(
                                userId = userId,
                                name = name.trim(),
                                sku = sku.trim(),
                                category = category,
                                unit = unit,
                                costPrice = costPrice.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                                sellPrice = sellPrice.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                                stock = stock.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                                minStock = minStock.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                                description = description.trim(),
                                photoData = photoData
                            )
                            onSave(prod)
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text(strings.save)
                    }
                }
            }
        }
    }

    if (showPhotoPicker) {
        PhotoPickerSheetDialog(
            onDismiss = { showPhotoPicker = false },
            onPhotoCompressed = { photoData = it }
        )
    }

    if (newCategoryPrompt) {
        var newCatName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { newCategoryPrompt = false },
            title = { Text(strings.addCategory) },
            text = {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text(strings.categoryNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newCatName.trim()
                        if (trimmed.isNotBlank()) {
                            onAddCategory(trimmed)
                            category = trimmed
                            newCategoryPrompt = false
                        }
                    },
                    enabled = newCatName.isNotBlank()
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { newCategoryPrompt = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun CategoryManagerDialog(
    categories: List<ProductCategory>,
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit,
    onUpdateCategory: (String, String) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    var newName by remember { mutableStateOf("") }
    var editingCat by remember { mutableStateOf<ProductCategory?>(null) }
    var editName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.manageCategories) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text(strings.categoryNameLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onAddCategory(newName.trim())
                                newName = ""
                            }
                        },
                        enabled = newName.isNotBlank()
                    ) {
                        Text(strings.addCategory)
                    }
                }

                HorizontalDivider()

                categories.forEach { cat ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            editingCat = cat
                            editName = cat.name
                        }) {
                            Icon(Icons.Rounded.Edit, contentDescription = strings.edit)
                        }
                        if (!cat.isDefault) {
                            IconButton(onClick = { onDeleteCategory(cat.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.delete, tint = StatusError)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close)
            }
        }
    )

    val catToEdit = editingCat
    if (catToEdit != null) {
        AlertDialog(
            onDismissRequest = { editingCat = null },
            title = { Text(strings.editCategory) },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text(strings.categoryNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            onUpdateCategory(catToEdit.id, editName.trim())
                            editingCat = null
                        }
                    },
                    enabled = editName.isNotBlank()
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCat = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
