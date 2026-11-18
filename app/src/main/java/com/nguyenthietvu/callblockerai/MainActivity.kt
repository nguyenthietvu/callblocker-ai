package com.nguyenthietvu.callblockerai

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val preferences by lazy {
        getSharedPreferences(CallScreeningServiceImpl.PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
    private lateinit var statusText: TextView
    private lateinit var numbersContainer: LinearLayout
    private lateinit var numberInput: EditText

    private val roleRequest = registerForActivityResult(
        android.app.ActivityResultContracts.StartActivityForResult()
    ) { refreshScreen() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshScreen()
    }

    override fun onResume() {
        super.onResume()
        if (::statusText.isInitialized) refreshScreen()
    }

    private fun refreshScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(24))
            setBackgroundColor(0xFFF7F8FC.toInt())
        }
        root.addView(TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 26f
            setTextColor(0xFF17233C.toInt())
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = getString(R.string.intro)
            textSize = 15f
            setTextColor(0xFF4D596D.toInt())
            setPadding(0, dp(8), 0, dp(16))
        })

        statusText = TextView(this).apply {
            textSize = 15f
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(0xFFE7ECF5.toInt())
        }
        root.addView(statusText, matchWidth())
        root.addView(Button(this).apply {
            text = getString(R.string.enable_protection)
            setOnClickListener { requestScreeningRole() }
        }, matchWidth())

        root.addView(TextView(this).apply {
            text = getString(R.string.blocklist_title)
            textSize = 19f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFF17233C.toInt())
            setPadding(0, dp(22), 0, dp(8))
        })
        numberInput = EditText(this).apply {
            hint = getString(R.string.phone_hint)
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            singleLine = true
        }
        root.addView(numberInput, matchWidth())
        root.addView(Button(this).apply {
            text = getString(R.string.add_number)
            setOnClickListener { addNumber() }
        }, matchWidth())

        numbersContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = android.widget.ScrollView(this).apply { addView(numbersContainer) }
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ).apply { topMargin = dp(8) })

        root.addView(TextView(this).apply {
            text = getString(R.string.disclaimer)
            textSize = 12f
            setTextColor(0xFF697386.toInt())
            setPadding(0, dp(12), 0, 0)
        })
        setContentView(root)
        updateStatus()
        renderNumbers()
    }

    private fun requestScreeningRole() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(this, R.string.role_unavailable, Toast.LENGTH_LONG).show()
            return
        }
        if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(this, R.string.protection_enabled, Toast.LENGTH_SHORT).show()
            return
        }
        roleRequest.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
    }

    private fun addNumber() {
        val entered = numberInput.text.toString().trim()
        val normalized = normalize(entered)
        if (normalized.length < 3) {
            numberInput.error = getString(R.string.invalid_number)
            return
        }
        val numbers = preferences.getStringSet(CallScreeningServiceImpl.BLOCKED_NUMBERS, emptySet())
            .orEmpty().toMutableSet()
        if (!numbers.add(normalized)) {
            Toast.makeText(this, R.string.number_exists, Toast.LENGTH_SHORT).show()
            return
        }
        preferences.edit().putStringSet(CallScreeningServiceImpl.BLOCKED_NUMBERS, numbers).apply()
        numberInput.text.clear()
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(numberInput.windowToken, 0)
        renderNumbers()
    }

    private fun renderNumbers() {
        if (!::numbersContainer.isInitialized) return
        numbersContainer.removeAllViews()
        val numbers = preferences.getStringSet(CallScreeningServiceImpl.BLOCKED_NUMBERS, emptySet())
            .orEmpty().toList().sorted()
        if (numbers.isEmpty()) {
            numbersContainer.addView(TextView(this).apply {
                text = getString(R.string.empty_list)
                setTextColor(0xFF697386.toInt())
                setPadding(0, dp(12), 0, dp(12))
            })
            return
        }
        numbers.forEach { phone ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(2), dp(4), dp(2))
                setBackgroundColor(0xFFFFFFFF.toInt())
            }
            row.addView(TextView(this).apply {
                text = phone
                textSize = 16f
                setTextColor(0xFF17233C.toInt())
            }, LinearLayout.LayoutParams(0, dp(52), 1f))
            row.addView(Button(this).apply {
                text = getString(R.string.remove)
                setOnClickListener { removeNumber(phone) }
            })
            numbersContainer.addView(row, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(6) })
        }
    }

    private fun removeNumber(phone: String) {
        val numbers = preferences.getStringSet(CallScreeningServiceImpl.BLOCKED_NUMBERS, emptySet())
            .orEmpty().toMutableSet()
        numbers.remove(phone)
        preferences.edit().putStringSet(CallScreeningServiceImpl.BLOCKED_NUMBERS, numbers).apply()
        renderNumbers()
    }

    private fun updateStatus() {
        val roleManager = getSystemService(RoleManager::class.java)
        val enabled = roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        statusText.text = getString(if (enabled) R.string.status_on else R.string.status_off)
        statusText.setTextColor(if (enabled) 0xFF176B43.toInt() else 0xFF8A4B08.toInt())
    }

    private fun normalize(value: String): String = value.filter { it.isDigit() }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )
}
