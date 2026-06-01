package utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    fun format(amount: Double): String {
        return format.format(amount).replace("INR", "₹").trim()
    }

    fun parseAmount(amountStr: String): Double {
        return amountStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
    }
}
