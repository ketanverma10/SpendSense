package data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey
    val smsId: Long,
    val amount: String, // Keep as String for now as per previous implementation, but consider Double for math
    val merchant: String,
    val type: String,
    val smsTimestamp: Long,
    val fullMessage: String,
    val isCategorized: Boolean = false
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
    val lastItemName: String,
    val lastCategory: String,
    val lastSubCategory: String,
    val lastUsedTimestamp: Long
)

object Categories {
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
