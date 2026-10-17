package com.simpledialer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvNumber: TextView
    private var currentNumber = StringBuilder()

    companion object {
        private const val CALL_PERMISSION_REQUEST = 1001
        private const val READ_CONTACTS_REQUEST = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvNumber = findViewById(R.id.tvNumber)

        val dialKeys = mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2",
            R.id.btn3 to "3", R.id.btn4 to "4", R.id.btn5 to "5",
            R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8",
            R.id.btn9 to "9", R.id.btnStar to "*", R.id.btnHash to "#"
        )

        dialKeys.forEach { (id, digit) ->
            findViewById<Button>(id).setOnClickListener { view ->
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                currentNumber.append(digit)
                tvNumber.text = formatNumber(currentNumber.toString())
            }
        }

        findViewById<Button>(R.id.btnBackspace).setOnClickListener {
            if (currentNumber.isNotEmpty()) {
                currentNumber.deleteCharAt(currentNumber.length - 1)
                tvNumber.text = formatNumber(currentNumber.toString())
            }
        }

        findViewById<Button>(R.id.btnBackspace).setOnLongClickListener {
            currentNumber.clear()
            tvNumber.text = ""
            true
        }

        findViewById<Button>(R.id.btnCall).setOnClickListener {
            val number = currentNumber.toString()
            if (number.isNotEmpty()) {
                makeCall(number)
            }
        }

        findViewById<ImageButton>(R.id.btnContacts).setOnClickListener {
            requestContactsPermission()
        }

        findViewById<ImageButton>(R.id.btnCallLog).setOnClickListener {
            startActivity(Intent(this, CallLogActivity::class.java))
        }

        // Handle tel: intent from other apps
        intent?.data?.let { uri ->
            if (uri.scheme == "tel") {
                val number = uri.schemeSpecificPart.replace("-", "").replace(" ", "")
                currentNumber.append(number)
                tvNumber.text = formatNumber(currentNumber.toString())
            }
        }
    }

    private fun formatNumber(raw: String): String {
        if (raw.isEmpty()) return ""
        return when {
            raw.startsWith("+") -> raw
            raw.length <= 5 -> raw
            raw.length <= 10 -> "${raw.substring(0, raw.length - 5)}-${raw.substring(raw.length - 5)}"
            else -> raw
        }
    }

    private fun makeCall(number: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED) {
            val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
            startActivity(callIntent)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                CALL_PERMISSION_REQUEST
            )
        }
    }

    private fun requestContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(this, ContactsActivity::class.java))
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                READ_CONTACTS_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CALL_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    makeCall(currentNumber.toString())
                } else {
                    Toast.makeText(this, "Call permission required", Toast.LENGTH_SHORT).show()
                }
            }
            READ_CONTACTS_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startActivity(Intent(this, ContactsActivity::class.java))
                }
            }
        }
    }
}
