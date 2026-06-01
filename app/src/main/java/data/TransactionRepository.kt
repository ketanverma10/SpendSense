package data

import kotlinx.coroutines.flow.Flow
import sms.SmsReader
import utils.ConfidenceEngine
import utils.CurrencyFormatter
import java.util.Calendar

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val smsReader: SmsReader
) {

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val uncategorizedTransactions: Flow<List<Transaction>> = transactionDao.getUncategorizedTransactions()
    val pendingReviewTransactions: Flow<List<Transaction>> = 
        transactionDao.getTransactionsByStatus(TransactionStatus.PENDING_REVIEW)

    suspend fun syncTransactions() {
        val hasTxns = transactionDao.getTransactionCount() > 0
        val sinceTimestamp = if (hasTxns) {
            transactionDao.getLatestTimestamp() ?: 0L
        } else {
            getStartOfYearTimestamp()
        }

        val newTransactions = smsReader.readTransactions(sinceTimestamp)
        
        val processedTransactions = newTransactions.map { txn ->
            val profile = transactionDao.getMerchantProfile(txn.merchant)
            val prediction = ConfidenceEngine.calculateConfidence(profile, txn)
            
            val status = when {
                prediction.confidence >= 90 -> TransactionStatus.AUTO_CONFIRMED
                else -> TransactionStatus.PENDING_REVIEW
            }

            txn.copy(
                status = status,
                suggestedCategory = prediction.category,
                suggestedSubCategory = prediction.subCategory,
                confidenceScore = prediction.confidence
            )
        }

        if (processedTransactions.isNotEmpty()) {
            transactionDao.insertTransactions(processedTransactions)
            
            processedTransactions.filter { it.status == TransactionStatus.AUTO_CONFIRMED }.forEach { txn ->
                val profile = transactionDao.getMerchantProfile(txn.merchant)!!
                val item = ExpenseItem(
                    transactionId = txn.smsId,
                    itemName = profile.lastItemName,
                    amount = txn.amount,
                    category = txn.suggestedCategory!!,
                    subCategory = txn.suggestedSubCategory!!
                )
                transactionDao.confirmAutoCategorization(txn, listOf(item))
            }
        }
    }

    suspend fun getMerchantProfile(merchantName: String): MerchantProfile? {
        return transactionDao.getMerchantProfile(merchantName)
    }

    suspend fun categorize(
        transaction: Transaction,
        items: List<ExpenseItem>,
        profile: MerchantProfile
    ) {
        val existingProfile = transactionDao.getMerchantProfile(profile.merchantName)
        val updatedProfile = if (existingProfile != null) {
            val newCount = existingProfile.transactionCount + 1
            val currentTxnAmount = items.sumOf { it.amount }
            val newAverage = (existingProfile.averageAmount * existingProfile.transactionCount + currentTxnAmount) / newCount
            
            existingProfile.copy(
                transactionCount = newCount,
                averageAmount = newAverage,
                lastItemName = profile.lastItemName,
                topCategory = profile.topCategory,
                topSubCategory = profile.topSubCategory,
                lastUsedTimestamp = System.currentTimeMillis()
            )
        } else {
            profile.copy(transactionCount = 1, averageAmount = items.sumOf { it.amount })
        }

        transactionDao.categorizeTransaction(transaction, items, updatedProfile)
    }

    suspend fun confirmSuggestion(transaction: Transaction) {
        val profile = transactionDao.getMerchantProfile(transaction.merchant) ?: return
        val item = ExpenseItem(
            transactionId = transaction.smsId,
            itemName = profile.lastItemName,
            amount = transaction.amount,
            category = transaction.suggestedCategory ?: "Other",
            subCategory = transaction.suggestedSubCategory ?: "Other"
        )
        transactionDao.confirmAutoCategorization(transaction, listOf(item))
    }

    // Dynamic Categories
    fun getAllCategories() = transactionDao.getAllCategories()
    fun getSubCategories(categoryName: String) = transactionDao.getSubCategories(categoryName)
    suspend fun addCategory(name: String) = transactionDao.insertCategory(CategoryEntity(name))
    suspend fun addSubCategory(categoryName: String, name: String) = 
        transactionDao.insertSubCategory(SubCategoryEntity(categoryName = categoryName, name = name))

    // Analytics
    fun getTotalSpent(start: Long, end: Long) = transactionDao.getTotalSpent(start, end)
    fun getTransactionCount(start: Long, end: Long) = transactionDao.getTransactionCountRange(start, end)
    fun getCategoryBreakdown(start: Long, end: Long) = transactionDao.getCategoryBreakdown(start, end)
    fun getSubCategoryBreakdown(category: String, start: Long, end: Long) = 
        transactionDao.getSubCategoryBreakdown(category, start, end)
    fun getTopMerchant(start: Long, end: Long) = transactionDao.getTopMerchant(start, end)
    fun getDailySpending(start: Long, end: Long) = transactionDao.getDailySpending(start, end)

    private fun getStartOfYearTimestamp(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
