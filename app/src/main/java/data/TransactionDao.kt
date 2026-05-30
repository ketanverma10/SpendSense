package data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction as RoomTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY smsTimestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isCategorized = 0 ORDER BY smsTimestamp DESC")
    fun getUncategorizedTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

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
        updateTransaction(transaction.copy(isCategorized = true))
        insertExpenseItems(items)
        insertMerchantProfile(profile)
    }
}
