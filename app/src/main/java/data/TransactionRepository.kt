package data

import kotlinx.coroutines.flow.Flow
import sms.SmsReader
import java.util.Calendar

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val smsReader: SmsReader
) {

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val uncategorizedTransactions: Flow<List<Transaction>> = transactionDao.getUncategorizedTransactions()

    suspend fun syncTransactions() {
        val hasTxns = transactionDao.getTransactionCount() > 0
        val sinceTimestamp = if (hasTxns) {
            transactionDao.getLatestTimestamp() ?: 0L
        } else {
            getStartOfYearTimestamp()
        }

        val newTransactions = smsReader.readTransactions(sinceTimestamp)
        if (newTransactions.isNotEmpty()) {
            transactionDao.insertTransactions(newTransactions)
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
        transactionDao.categorizeTransaction(transaction, items, profile)
    }

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
