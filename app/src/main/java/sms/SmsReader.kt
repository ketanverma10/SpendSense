package sms

import android.content.ContentResolver
import android.database.Cursor
import androidx.core.net.toUri
import parser.SmsParser
import data.Transaction

class SmsReader(
    private val contentResolver: ContentResolver
) {

    fun readTransactions(sinceTimestamp: Long): List<Transaction> {

        val transactions = mutableListOf<Transaction>()

        val uri = "content://sms/inbox".toUri()

        val cursor: Cursor? =
            contentResolver.query(
                uri,
                null,
                "date > ?",
                arrayOf(sinceTimestamp.toString()),
                "date DESC"
            )

        cursor?.use {

            while (it.moveToNext()) {

                val smsId =
                    it.getLong(it.getColumnIndexOrThrow("_id"))

                val smsTimestamp =
                    it.getLong(it.getColumnIndexOrThrow("date"))

                val message =
                    it.getString(it.getColumnIndexOrThrow("body"))

                if (!SmsParser.isDebitMessage(message)) {
                    continue
                }

                val amount =
                    SmsParser.extractAmount(message)

                if (amount == null) {
                    continue
                }

                val merchant =
                    SmsParser.extractMerchant(message)

                transactions.add(
                    Transaction(
                        smsId = smsId,
                        amount = amount,
                        merchant = merchant ?: "Unknown",
                        type = "DEBIT",
                        smsTimestamp = smsTimestamp,
                        fullMessage = message
                    )
                )
            }
        }

        return transactions
    }
}
