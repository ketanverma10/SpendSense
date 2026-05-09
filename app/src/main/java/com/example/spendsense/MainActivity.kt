package com.example.spendsense

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.spendsense.ui.theme.SpendSenseTheme

class MainActivity : ComponentActivity() {

    // ✅ Permission launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->

            if (isGranted) {
                Log.d("PERMISSION", "SMS Permission Granted")
                readSms()
            } else {
                Log.d("PERMISSION", "SMS Permission Denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            SpendSenseTheme {

                // ✅ Run once when app starts
                LaunchedEffect(Unit) {
                    checkSmsPermission()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "SpendSense",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    // ✅ Check SMS permission
    fun checkSmsPermission() {

        when {

            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED -> {

                Log.d("PERMISSION", "Already Granted")
                readSms()
            }

            else -> {

                Log.d("PERMISSION", "Requesting Permission")
                requestPermissionLauncher.launch(Manifest.permission.READ_SMS)
            }
        }
    }

    // ✅ Read SMS
    fun readSms() {

        Log.d("SMS", "readSms function called")

        val uri = "content://sms/inbox".toUri()

        val cursor: Cursor? =
            contentResolver.query(uri, null, null, null, null)

        cursor?.use {

            while (it.moveToNext()) {

                val message =
                    it.getString(it.getColumnIndexOrThrow("body"))

                // 🔍 OPTIONAL: View all SMS
                // Log.d("SMS_ALL", message)

                // ✅ ONLY TRACK MONEY OUT (DEBIT)
                if (
                    message.contains("withdrawn", true) ||
                    message.contains("sent", true) ||
                    message.contains("debited", true) ||
                    message.contains("spent", true)
                ) {

                    val amount = extractAmount(message)
                    val merchant = extractMerchant(message)

                    if (amount != null) {

                        Log.d("TXN", "====================")
                        Log.d("TXN", "DEBIT TRANSACTION")
                        Log.d("TXN", "Amount: $amount")
                        Log.d(
                            "TXN",
                            "Merchant: ${merchant ?: "Unknown"}"
                        )
                        Log.d("TXN", "Full SMS: $message")
                    }
                }
            }
        }
    }

    // ✅ Extract amount
    fun extractAmount(message: String): String? {

        val regex =
            Regex("(₹|Rs\\.?|INR)\\s?\\d+(\\.\\d{1,2})?")

        return regex.find(message)?.value
    }

    // ✅ Extract merchant name
    fun extractMerchant(message: String): String? {

        // ✅ UPI transactions
        val upiRegex =
            Regex("To\\s([A-Za-z ]+)", RegexOption.IGNORE_CASE)

        val upiMatch = upiRegex.find(message)

        if (upiMatch != null) {
            return upiMatch.groups[1]?.value?.trim()
        }

        // ✅ Card swipe transactions
        val cardRegex =
            Regex("At\\s([+A-Za-z ]+)", RegexOption.IGNORE_CASE)

        val cardMatch = cardRegex.find(message)

        if (cardMatch != null) {
            return cardMatch.groups[1]?.value?.trim()
        }

        return null
    }
}


// ---------------- UI ----------------

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {

    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {

    SpendSenseTheme {
        Greeting("SpendSense")
    }
}