package ui

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import data.AppDatabase
import data.Transaction
import data.TransactionRepository
import sms.SmsReader
import com.example.spendsense.ui.theme.SpendSenseTheme
import utils.Constants

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                // Sync will be triggered by LaunchedEffect or manual call
            } else {
                Log.d("PERMISSION", "Permission Denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val smsReader = SmsReader(contentResolver)
        val repository = TransactionRepository(database.transactionDao(), smsReader)
        val viewModelFactory = TransactionViewModel.Factory(repository)
        val categorizationViewModelFactory = CategorizationViewModel.Factory(repository)

        setContent {
            SpendSenseTheme {
                val viewModel: TransactionViewModel = viewModel(factory = viewModelFactory)
                val categorizationViewModel: CategorizationViewModel = viewModel(factory = categorizationViewModelFactory)
                
                val transactions by viewModel.allTransactions.collectAsState()
                val selectedTransaction by categorizationViewModel.selectedTransaction

                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Constants.SMS_PERMISSION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        viewModel.sync()
                    } else {
                        requestPermissionLauncher.launch(Constants.SMS_PERMISSION)
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TransactionList(
                        transactions = transactions,
                        onTransactionClick = { transaction ->
                            if (!transaction.isCategorized) {
                                categorizationViewModel.selectTransaction(transaction)
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )

                    if (selectedTransaction != null) {
                        CategorizationScreen(
                            viewModel = categorizationViewModel,
                            onDismiss = { categorizationViewModel.selectTransaction(null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionList(
    transactions: List<Transaction>,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(transactions) { transaction ->
            ListItem(
                headlineContent = { Text(transaction.merchant) },
                supportingContent = { 
                    Column {
                        Text(transaction.fullMessage)
                        if (transaction.isCategorized) {
                            SuggestionChip(onClick = {}, label = { Text("Categorized") })
                        }
                    }
                },
                trailingContent = { Text(transaction.amount) },
                modifier = Modifier.clickable { onTransactionClick(transaction) }
            )
            HorizontalDivider()
        }
    }
}
