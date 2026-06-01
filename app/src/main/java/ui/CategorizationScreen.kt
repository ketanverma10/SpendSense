package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import data.CategoryEntity
import data.SubCategoryEntity
import data.Transaction
import utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorizationScreen(
    viewModel: CategorizationViewModel,
    onDismiss: () -> Unit,
) {
    val transaction = viewModel.selectedTransaction.value ?: return
    val items = viewModel.items
    val isMultiple = viewModel.isMultipleItems.value
    val errorMessage = viewModel.errorMessage.value
    val categories by viewModel.categories.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorize", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding().imePadding()
            ) {
                Button(
                    onClick = { viewModel.saveCategorization() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Save Categorization", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TransactionHeader(transaction)
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text("Split into multiple items?", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(
                        checked = isMultiple,
                        onCheckedChange = { viewModel.setMultipleItems(it) }
                    )
                }
            }

            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                ExpenseItemForm(
                    item = item,
                    index = index,
                    showRemove = (isMultiple && items.size > 1),
                    categories = categories,
                    onUpdate = { viewModel.updateItem(index, it) },
                    onRemove = { viewModel.removeItem(index) },
                    viewModel = viewModel
                )
            }

            if (isMultiple) {
                item {
                    OutlinedButton(
                        onClick = { viewModel.addItem() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Another Item")
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
            
            item {
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun TransactionHeader(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(transaction.merchant, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(4.dp))
                Text(
                    CurrencyFormatter.format(transaction.amount), 
                    style = MaterialTheme.typography.headlineLarge, 
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(8.dp))
                Text(transaction.fullMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun ExpenseItemForm(
    item: ExpenseItemState,
    index: Int,
    showRemove: Boolean,
    categories: List<CategoryEntity>,
    onUpdate: (ExpenseItemState) -> Unit,
    onRemove: () -> Unit,
    viewModel: CategorizationViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Item ${index + 1}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (showRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = item.itemName,
                onValueChange = { onUpdate(item.copy(itemName = it)) },
                label = { Text("Item Name") },
                placeholder = { Text("e.g. Milk, Petrol") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = item.amount,
                onValueChange = { onUpdate(item.copy(amount = it)) },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                prefix = { Text("₹") }
            )

            Spacer(Modifier.height(12.dp))

            DynamicDropdown(
                label = "Category",
                selected = item.category,
                options = categories.map { it.name },
                onSelect = { 
                    onUpdate(item.copy(category = it, subCategory = ""))
                },
                onAddNew = { viewModel.addNewCategory(it) }
            )

            if (item.category.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                val subCategories by viewModel.subCategories.collectAsState()
                LaunchedEffect(item.category) {
                    viewModel.loadSubCategories(item.category)
                }

                DynamicDropdown(
                    label = "Sub-Category",
                    selected = item.subCategory,
                    options = subCategories.map { it.name },
                    onSelect = { onUpdate(item.copy(subCategory = it)) },
                    onAddNew = { viewModel.addNewSubCategory(item.category, it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    onAddNew: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newOptionName by remember { mutableStateOf("") }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("+ Add New", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                onClick = {
                    showAddDialog = true
                    expanded = false
                }
            )
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New $label") },
            text = {
                OutlinedTextField(
                    value = newOptionName,
                    onValueChange = { newOptionName = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newOptionName.isNotBlank()) {
                        onAddNew(newOptionName)
                        onSelect(newOptionName)
                        newOptionName = ""
                        showAddDialog = false
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
