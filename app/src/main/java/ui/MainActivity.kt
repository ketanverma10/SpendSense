package ui

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import data.AppDatabase
import data.Transaction
import data.TransactionRepository
import sms.SmsReader
import com.example.spendsense.ui.theme.SpendSenseTheme
import utils.Constants
import utils.CurrencyFormatter

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
        val reviewQueueViewModelFactory = ReviewQueueViewModel.Factory(repository)
        val dashboardViewModelFactory = DashboardViewModel.Factory(repository)

        setContent {
            SpendSenseTheme {
                val viewModel: TransactionViewModel = viewModel(factory = viewModelFactory)
                val categorizationViewModel: CategorizationViewModel = viewModel(factory = categorizationViewModelFactory)
                val reviewQueueViewModel: ReviewQueueViewModel = viewModel(factory = reviewQueueViewModelFactory)
                val dashboardViewModel: DashboardViewModel = viewModel(factory = dashboardViewModelFactory)
                
                val transactions by viewModel.allTransactions.collectAsState()
                val selectedTransaction by categorizationViewModel.selectedTransaction
                
                var selectedTab by remember { mutableIntStateOf(0) }

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

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                label = { Text("Dashboard") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.History, contentDescription = null) },
                                label = { Text("History") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.RateReview, contentDescription = null) },
                                label = { Text("Review") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (selectedTab) {
                            0 -> DashboardScreen(viewModel = dashboardViewModel)
                            1 -> TransactionList(
                                transactions = transactions,
                                onTransactionClick = { transaction ->
                                    if (!transaction.isCategorized) {
                                        categorizationViewModel.selectTransaction(transaction)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            2 -> ReviewQueueScreen(
                                viewModel = reviewQueueViewModel,
                                onEdit = { transaction ->
                                    categorizationViewModel.selectTransaction(transaction)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

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
                trailingContent = { Text(CurrencyFormatter.format(transaction.amount)) },
                modifier = Modifier.clickable { onTransactionClick(transaction) }
            )
            HorizontalDivider()
        }
    }
}
