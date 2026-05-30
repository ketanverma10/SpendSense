package ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import data.ExpenseItem
import data.MerchantProfile
import data.Transaction
import data.TransactionRepository
import kotlinx.coroutines.launch

data class ExpenseItemState(
    val itemName: String = "",
    val amount: String = "",
    val category: String = "",
    val subCategory: String = ""
)

class CategorizationViewModel(private val repository: TransactionRepository) : ViewModel() {

    private val _selectedTransaction = mutableStateOf<Transaction?>(null)
    val selectedTransaction: State<Transaction?> = _selectedTransaction

    val items = mutableStateListOf<ExpenseItemState>()

    private val _isMultipleItems = mutableStateOf(false)
    val isMultipleItems: State<Boolean> = _isMultipleItems

    private val _merchantProfile = mutableStateOf<MerchantProfile?>(null)
    val merchantProfile: State<MerchantProfile?> = _merchantProfile

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    fun selectTransaction(transaction: Transaction?) {
        _selectedTransaction.value = transaction
        items.clear()
        _isMultipleItems.value = false
        _errorMessage.value = null
        
        if (transaction != null) {
            viewModelScope.launch {
                val profile = repository.getMerchantProfile(transaction.merchant)
                _merchantProfile.value = profile
                
                // Auto-suggestion logic
                if (profile != null) {
                    items.add(
                        ExpenseItemState(
                            itemName = profile.lastItemName,
                            amount = transaction.amount.replace(Regex("[^0-9.]"), ""),
                            category = profile.lastCategory,
                            subCategory = profile.lastSubCategory
                        )
                    )
                } else {
                    items.add(ExpenseItemState(amount = transaction.amount.replace(Regex("[^0-9.]"), "")))
                }
            }
        }
    }

    fun setMultipleItems(isMultiple: Boolean) {
        _isMultipleItems.value = isMultiple
        if (!isMultiple && items.size > 1) {
            val firstItem = items[0]
            items.clear()
            items.add(firstItem)
        }
    }

    fun addItem() {
        items.add(ExpenseItemState())
    }

    fun removeItem(index: Int) {
        if (items.size > 1) {
            items.removeAt(index)
        }
    }

    fun updateItem(index: Int, newState: ExpenseItemState) {
        items[index] = newState
    }

    fun saveCategorization() {
        val transaction = _selectedTransaction.value ?: return
        
        // Validation
        val totalAmount = items.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        val transactionAmount = transaction.amount.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        
        if (Math.abs(totalAmount - transactionAmount) > 0.01) {
            _errorMessage.value = "Total amount (₹$totalAmount) does not match transaction amount (₹$transactionAmount)"
            return
        }

        if (items.any { it.itemName.isBlank() || it.category.isBlank() || it.subCategory.isBlank() }) {
            _errorMessage.value = "Please fill all fields for all items"
            return
        }

        viewModelScope.launch {
            val expenseItems = items.map {
                ExpenseItem(
                    transactionId = transaction.smsId,
                    itemName = it.itemName,
                    amount = it.amount.toDouble(),
                    category = it.category,
                    subCategory = it.subCategory
                )
            }

            // Update merchant profile based on the first item (or could be more sophisticated)
            val firstItem = expenseItems.first()
            val profile = MerchantProfile(
                merchantName = transaction.merchant,
                lastItemName = firstItem.itemName,
                lastCategory = firstItem.category,
                lastSubCategory = firstItem.subCategory,
                lastUsedTimestamp = System.currentTimeMillis()
            )

            repository.categorize(transaction, expenseItems, profile)
            _selectedTransaction.value = null // Clear selection after save
        }
    }

    class Factory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CategorizationViewModel(repository) as T
        }
    }
}
