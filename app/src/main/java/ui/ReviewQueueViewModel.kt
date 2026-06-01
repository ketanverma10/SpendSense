package ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import data.Transaction
import data.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReviewQueueViewModel(private val repository: TransactionRepository) : ViewModel() {

    val pendingTransactions: StateFlow<List<Transaction>> = repository.pendingReviewTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun confirm(transaction: Transaction) {
        viewModelScope.launch {
            repository.confirmSuggestion(transaction)
        }
    }

    class Factory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReviewQueueViewModel(repository) as T
        }
    }
}
