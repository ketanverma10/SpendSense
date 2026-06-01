package utils

import data.MerchantProfile
import data.Transaction

object ConfidenceEngine {

    data class Prediction(
        val category: String?,
        val subCategory: String?,
        val confidence: Int
    )

    fun calculateConfidence(
        merchantProfile: MerchantProfile?,
        transaction: Transaction
    ): Prediction {
        if (merchantProfile == null) {
            return Prediction(null, null, 0)
        }

        // Logic based on requirements:
        // High confidence if it's a very frequent merchant with consistent behavior.
        // For now, we use a simple heuristic based on transaction count.
        // In a real app, we'd look at category distribution.
        
        val confidence = when {
            merchantProfile.transactionCount > 50 -> 95
            merchantProfile.transactionCount > 20 -> 80
            merchantProfile.transactionCount > 10 -> 60
            merchantProfile.transactionCount > 0 -> 40
            else -> 0
        }

        return Prediction(
            category = merchantProfile.topCategory,
            subCategory = merchantProfile.topSubCategory,
            confidence = confidence
        )
    }
}
