package parser

object SmsParser {

    // ✅ Detect debit messages only
    fun isDebitMessage(message: String): Boolean {

        return (
                message.contains("withdrawn", true) ||
                        message.contains("sent", true) ||
                        message.contains("debited", true) ||
                        message.contains("spent", true)
                )
    }

    // ✅ Extract amount
    fun extractAmount(message: String): String? {

        val regex =
            Regex("(₹|Rs\\.?|INR)\\s?\\d+(\\.\\d{1,2})?")

        return regex.find(message)?.value
    }

    // ✅ Extract merchant
    fun extractMerchant(message: String): String? {

        // UPI transactions
        val upiRegex =
            Regex("To\\s([A-Za-z ]+)", RegexOption.IGNORE_CASE)

        val upiMatch = upiRegex.find(message)

        if (upiMatch != null) {
            return upiMatch.groups[1]?.value?.trim()
        }

        // Card transactions
        val cardRegex =
            Regex("At\\s([+A-Za-z ]+)", RegexOption.IGNORE_CASE)

        val cardMatch = cardRegex.find(message)

        if (cardMatch != null) {
            return cardMatch.groups[1]?.value?.trim()
        }

        return "Unknown"
    }
}
