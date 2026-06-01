package data

import androidx.room.*
import androidx.room.Transaction as RoomTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY smsTimestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY smsTimestamp DESC")
    fun getTransactionsByStatus(status: TransactionStatus): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isCategorized = 0 ORDER BY smsTimestamp DESC")
    fun getUncategorizedTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("UPDATE transactions SET status = :status WHERE smsId = :transactionId")
    suspend fun updateTransactionStatus(transactionId: Long, status: TransactionStatus)

    @Query("SELECT MAX(smsTimestamp) FROM transactions")
    suspend fun getLatestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    // Expense Items
    @Insert
    suspend fun insertExpenseItems(items: List<ExpenseItem>)

    @Query("SELECT * FROM expense_items WHERE transactionId = :transactionId")
    suspend fun getItemsForTransaction(transactionId: Long): List<ExpenseItem>

    // Merchant Profile
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchantProfile(profile: MerchantProfile)

    @Query("SELECT * FROM merchant_profiles WHERE merchantName = :merchantName")
    suspend fun getMerchantProfile(merchantName: String): MerchantProfile?

    @RoomTransaction
    suspend fun categorizeTransaction(
        transaction: Transaction,
        items: List<ExpenseItem>,
        profile: MerchantProfile
    ) {
        updateTransaction(transaction.copy(isCategorized = true, status = TransactionStatus.CONFIRMED))
        insertExpenseItems(items)
        insertMerchantProfile(profile)
    }

    @RoomTransaction
    suspend fun confirmAutoCategorization(
        transaction: Transaction,
        items: List<ExpenseItem>
    ) {
        updateTransaction(transaction.copy(isCategorized = true, status = TransactionStatus.CONFIRMED))
        insertExpenseItems(items)
    }

    // Categories
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubCategory(subCategory: SubCategoryEntity)

    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM sub_categories WHERE categoryName = :categoryName")
    fun getSubCategories(categoryName: String): Flow<List<SubCategoryEntity>>

    // Analytics
    @Query("SELECT SUM(amount) FROM transactions WHERE smsTimestamp >= :startTime AND smsTimestamp < :endTime")
    fun getTotalSpent(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM transactions WHERE smsTimestamp >= :startTime AND smsTimestamp < :endTime")
    fun getTransactionCountRange(startTime: Long, endTime: Long): Flow<Int>

    @Query("SELECT category, SUM(expense_items.amount) as total FROM expense_items " +
           "INNER JOIN transactions ON expense_items.transactionId = transactions.smsId " +
           "WHERE transactions.smsTimestamp >= :startTime AND transactions.smsTimestamp < :endTime " +
           "GROUP BY category ORDER BY total DESC")
    fun getCategoryBreakdown(startTime: Long, endTime: Long): Flow<List<CategorySum>>

    @Query("SELECT subCategory as category, SUM(expense_items.amount) as total FROM expense_items " +
           "INNER JOIN transactions ON expense_items.transactionId = transactions.smsId " +
           "WHERE transactions.smsTimestamp >= :startTime AND transactions.smsTimestamp < :endTime " +
           "AND expense_items.category = :category " +
           "GROUP BY subCategory ORDER BY total DESC")
    fun getSubCategoryBreakdown(category: String, startTime: Long, endTime: Long): Flow<List<CategorySum>>

    @Query("SELECT merchant, SUM(amount) as total FROM transactions " +
           "WHERE smsTimestamp >= :startTime AND smsTimestamp < :endTime " +
           "GROUP BY merchant ORDER BY total DESC LIMIT 1")
    fun getTopMerchant(startTime: Long, endTime: Long): Flow<MerchantSum?>

    @Query("SELECT CAST((smsTimestamp + 19800000) / 86400000 AS INTEGER) * 86400000 - 19800000 as day, SUM(amount) as total FROM transactions " +
           "WHERE smsTimestamp >= :startTime AND smsTimestamp < :endTime " +
           "GROUP BY day ORDER BY day ASC")
    fun getDailySpending(startTime: Long, endTime: Long): Flow<List<DailySum>>
}

data class CategorySum(val category: String, val total: Double)
data class MerchantSum(val merchant: String, val total: Double)
data class DailySum(val day: Long, val total: Double)
