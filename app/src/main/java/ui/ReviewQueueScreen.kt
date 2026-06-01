package ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.Transaction
import utils.CurrencyFormatter

@Composable
fun ReviewQueueScreen(
    viewModel: ReviewQueueViewModel,
    onEdit: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val pendingTransactions by viewModel.pendingTransactions.collectAsState()

    if (pendingTransactions.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No transactions need review.", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Review Queue",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(pendingTransactions) { transaction ->
                ReviewCard(
                    transaction = transaction,
                    onConfirm = { viewModel.confirm(transaction) },
                    onEdit = { onEdit(transaction) }
                )
            }
        }
    }
}

@Composable
fun ReviewCard(
    transaction: Transaction,
    onConfirm: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(transaction.merchant, style = MaterialTheme.typography.titleLarge)
                Text(
                    CurrencyFormatter.format(transaction.amount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            if (transaction.suggestedCategory != null) {
                SuggestionBadge(
                    category = transaction.suggestedCategory,
                    subCategory = transaction.suggestedSubCategory ?: "",
                    confidence = transaction.confidenceScore
                )
            } else {
                Text("No suggestion available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }

            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onEdit) {
                    Text(if (transaction.suggestedCategory != null) "Edit" else "Categorize")
                }
                if (transaction.suggestedCategory != null) {
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onConfirm) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionBadge(
    category: String,
    subCategory: String,
    confidence: Int
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Suggested: $category > $subCategory",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Confidence: $confidence%",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
