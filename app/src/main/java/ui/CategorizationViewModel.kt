package ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import data.CategoryEntity
import data.ExpenseItem
import data.MerchantProfile
import data.SubCategoryEntity
import data.Transaction
import data.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import utils.CurrencyFormatter
import kotlin.math.abs

data class ExpenseItemState(
    val id: Long = System.currentTimeMillis(), // Unique ID for key in LazyColumn
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

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _subCategories = MutableStateFlow<List<SubCategoryEntity>>(emptyList())
    val subCategories: StateFlow<List<SubCategoryEntity>> = _subCategories

    fun loadSubCategories(categoryName: String) {
        viewModelScope.launch {
            repository.getSubCategories(categoryName).collect {
                _subCategories.value = it
            }
        }
    }

    fun selectTransaction(transaction: Transaction?) {
        _selectedTransaction.value = transaction
        items.clear()
        _isMultipleItems.value = false
        _errorMessage.value = null
        
        if (transaction != null) {
            viewModelScope.launch {
                val profile = repository.getMerchantProfile(transaction.merchant)
                
                if (profile != null) {
                    items.add(
                        ExpenseItemState(
                            itemName = profile.lastItemName,
                            amount = transaction.amount.toString(),
                            category = profile.topCategory,
                            subCategory = profile.topSubCategory
                        )
                    )
                } else {
                    items.add(ExpenseItemState(amount = transaction.amount.toString()))
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
        
        val totalAmount = items.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        
        if (abs(totalAmount - transaction.amount) > 0.01) {
            _errorMessage.value = "Total amount (${CurrencyFormatter.format(totalAmount)}) does not match transaction amount (${CurrencyFormatter.format(transaction.amount)})"
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

            val firstItem = expenseItems.first()
            val profile = MerchantProfile(
                merchantName = transaction.merchant,
                lastItemName = firstItem.itemName,
                topCategory = firstItem.category,
                topSubCategory = firstItem.subCategory,
                lastUsedTimestamp = System.currentTimeMillis(),
                transactionCount = 1,
                averageAmount = firstItem.amount
            )

            repository.categorize(transaction, expenseItems, profile)
            _selectedTransaction.value = null
        }
    }

    fun addNewCategory(name: String) {
        viewModelScope.launch {
            repository.addCategory(name)
        }
    }

    fun addNewSubCategory(categoryName: String, name: String) {
        viewModelScope.launch {
            repository.addSubCategory(categoryName, name)
        }
    }

    class Factory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CategorizationViewModel(repository) as T
        }
    }
}
