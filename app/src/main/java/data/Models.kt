package data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionStatus {
    PENDING_REVIEW,
    CONFIRMED,
    AUTO_CONFIRMED
}

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey
    val smsId: Long,
    val amount: Double,
    val merchant: String,
    val type: String,
    val smsTimestamp: Long,
    val fullMessage: String,
    val isCategorized: Boolean = false,
    val status: TransactionStatus = TransactionStatus.PENDING_REVIEW,
    val suggestedCategory: String? = null,
    val suggestedSubCategory: String? = null,
    val confidenceScore: Int = 0
)

@Entity(
    tableName = "expense_items",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["smsId"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("transactionId")]
)
data class ExpenseItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val itemName: String,
    val amount: Double,
    val category: String,
    val subCategory: String
)

@Entity(tableName = "merchant_profiles")
data class MerchantProfile(
    @PrimaryKey
    val merchantName: String,
    val transactionCount: Int = 0,
    val averageAmount: Double = 0.0,
    val lastItemName: String,
    val topCategory: String,
    val topSubCategory: String,
    val lastUsedTimestamp: Long
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val name: String,
    val color: Int = 0xFF6200EE.toInt()
)

@Entity(
    tableName = "sub_categories",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["name"],
            childColumns = ["categoryName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryName")]
)
data class SubCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryName: String,
    val name: String
)

object DefaultCategories {
    val data = mapOf(
        "Food" to listOf("Breakfast", "Lunch", "Dinner", "Snacks", "Tea/Coffee", "Cold Drink", "Groceries", "Fruits", "Vegetables", "Milk"),
        "Travel" to listOf("Fuel", "Metro", "Bus", "Taxi", "Uber", "Ola", "Parking", "Toll"),
        "Shopping" to listOf("Clothing", "Electronics", "Books", "Accessories", "Furniture"),
        "Bills" to listOf("Electricity", "Water", "Gas", "Internet", "Mobile Recharge", "Rent"),
        "Health" to listOf("Medicine", "Doctor", "Hospital", "Gym", "Supplements"),
        "Entertainment" to listOf("Movies", "OTT", "Games", "Music", "Events"),
        "Education" to listOf("Books", "Course", "Certification", "Coaching"),
        "Personal Care" to listOf("Haircut", "Skincare", "Cosmetics"),
        "Gifts" to listOf("Birthday", "Anniversary", "Festival"),
        "Investments" to listOf("Stocks", "Mutual Funds", "FD", "PPF", "Crypto"),
        "Others" to listOf("Miscellaneous")
    )
}
