package ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import data.Categories
import data.Transaction

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorize Transaction") },
                actions = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = { 
                    viewModel.saveCategorization()
                    // onDismiss will be handled by observing selectedTransaction becoming null in parent
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Save Categorization")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TransactionSummary(transaction)
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Single Item", modifier = Modifier.weight(1f))
                    Switch(
                        checked = isMultiple,
                        onCheckedChange = { viewModel.setMultipleItems(it) }
                    )
                    Text("Multiple Items", modifier = Modifier.padding(start = 8.dp))
                }
            }

            itemsIndexed(items) { index, item ->
                ExpenseItemForm(
                    item = item,
                    index = index,
                    showRemove = (isMultiple && items.size > 1),
                    onUpdate = { viewModel.updateItem(index, it) },
                    onRemove = { viewModel.removeItem(index) }
                )
            }

            if (isMultiple) {
                item {
                    OutlinedButton(
                        onClick = { viewModel.addItem() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Another Item")
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun TransactionSummary(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(transaction.merchant, style = MaterialTheme.typography.titleLarge)
            Text(transaction.amount, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(transaction.fullMessage, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ExpenseItemForm(
    item: ExpenseItemState,
    index: Int,
    showRemove: Boolean,
    onUpdate: (ExpenseItemState) -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Item ${index + 1}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (showRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                    }
                }
            }
            
            OutlinedTextField(
                value = item.itemName,
                onValueChange = { onUpdate(item.copy(itemName = it)) },
                label = { Text("Item Name (e.g. Milk, Petrol)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = item.amount,
                onValueChange = { onUpdate(item.copy(amount = it)) },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            CategoryDropdown(
                selectedCategory = item.category,
                onCategorySelected = { onUpdate(item.copy(category = it, subCategory = "")) }
            )

            if (item.category.isNotEmpty()) {
                SubCategoryDropdown(
                    category = item.category,
                    selectedSubCategory = item.subCategory,
                    onSubCategorySelected = { onUpdate(item.copy(subCategory = it)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val categories = Categories.data.keys.toList()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
            )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubCategoryDropdown(
    category: String,
    selectedSubCategory: String,
    onSubCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val subCategories = Categories.data[category] ?: emptyList()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
            OutlinedTextField(
                value = selectedSubCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Sub-Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
            )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            subCategories.forEach { sub ->
                DropdownMenuItem(
                    text = { Text(sub) },
                    onClick = {
                        onSubCategorySelected(sub)
                        expanded = false
                    }
                )
            }
        }
    }
}
