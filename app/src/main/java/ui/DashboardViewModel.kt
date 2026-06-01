package ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import data.CategorySum
import data.DailySum
import data.MerchantSum
import data.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.Calendar
import java.util.TimeZone

enum class TimeFilter {
    MONTH, WEEK, YEAR
}

class DashboardViewModel(private val repository: TransactionRepository) : ViewModel() {

    private val _timeFilter = MutableStateFlow(TimeFilter.MONTH)
    val timeFilter: StateFlow<TimeFilter> = _timeFilter

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    val selectedMonth: StateFlow<Int> = _selectedMonth

    private val _selectedWeek = MutableStateFlow<Int?>(null) 
    val selectedWeek: StateFlow<Int?> = _selectedWeek

    private val _drillDownCategory = MutableStateFlow<String?>(null)
    val drillDownCategory: StateFlow<String?> = _drillDownCategory

    @OptIn(ExperimentalCoroutinesApi::class)
    private val timeRange = combine(_timeFilter, _selectedYear, _selectedMonth, _selectedWeek) { filter, year, month, week ->
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+5:30"))
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val start: Long
        val end: Long

        when (filter) {
            TimeFilter.YEAR -> {
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                start = calendar.timeInMillis
                calendar.add(Calendar.YEAR, 1)
                end = calendar.timeInMillis
            }
            else -> {
                if (week == null) {
                    start = calendar.timeInMillis
                    calendar.add(Calendar.MONTH, 1)
                    end = calendar.timeInMillis
                } else {
                    val startDay = ((week - 1) * 7) + 1
                    calendar.set(Calendar.DAY_OF_MONTH, startDay)
                    start = calendar.timeInMillis
                    
                    val currentMonth = calendar.get(Calendar.MONTH)
                    calendar.add(Calendar.DAY_OF_MONTH, 7)
                    
                    if (calendar.get(Calendar.MONTH) != currentMonth) {
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                    }
                    end = calendar.timeInMillis
                }
            }
        }
        start to end
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalSpent = timeRange.flatMapLatest { (start, end) ->
        repository.getTotalSpent(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactionCount = timeRange.flatMapLatest { (start, end) ->
        repository.getTransactionCount(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoryBreakdown = combine(timeRange, _drillDownCategory) { range, drillDown ->
        range to drillDown
    }.flatMapLatest { (range, drillDown) ->
        val (start, end) = range
        if (drillDown == null) {
            repository.getCategoryBreakdown(start, end)
        } else {
            repository.getSubCategoryBreakdown(drillDown, start, end)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val topMerchant = timeRange.flatMapLatest { (start, end) ->
        repository.getTopMerchant(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val dailySpending = timeRange.flatMapLatest { (start, end) ->
        repository.getDailySpending(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTimeFilter(filter: TimeFilter) {
        _timeFilter.value = filter
        if (filter == TimeFilter.YEAR) {
            _selectedWeek.value = null
        }
    }

    fun setYear(year: Int) {
        _selectedYear.value = year
    }

    fun setMonth(month: Int) {
        _selectedMonth.value = month
    }

    fun setWeek(week: Int?) {
        _selectedWeek.value = week
        if (week != null) {
            _timeFilter.value = TimeFilter.WEEK
        } else if (_timeFilter.value == TimeFilter.WEEK) {
            _timeFilter.value = TimeFilter.MONTH
        }
    }

    fun drillDown(category: String?) {
        _drillDownCategory.value = category
    }

    class Factory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository) as T
        }
    }
}
