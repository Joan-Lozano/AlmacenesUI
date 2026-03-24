package com.campestre.almacenesui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var urlInput: TextInputEditText
    private lateinit var keyboardSwitch: MaterialSwitch
    private lateinit var goButton: MaterialButton
    private lateinit var exitButton: MaterialButton
    private lateinit var sharedPreferences: SharedPreferences

    private val PREFS_NAME = "AlmacenesUIPrefs"
    private val KEY_URL = "saved_url"
    private val KEY_KEYBOARD = "enable_keyboard"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupFullScreen()

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        urlInput = findViewById(R.id.urlInput)
        keyboardSwitch = findViewById(R.id.keyboardSwitch)
        goButton = findViewById(R.id.goButton)
        exitButton = findViewById(R.id.exitButton)

        // Cargar URL y estado del teclado guardados
        val savedUrl = sharedPreferences.getString(KEY_URL, "")
        val savedKeyboard = sharedPreferences.getBoolean(KEY_KEYBOARD, true)
        
        urlInput.setText(savedUrl)
        keyboardSwitch.isChecked = savedKeyboard
        
        // Aplicar estado inicial del teclado
        updateKeyboardVisibility(savedKeyboard)

        keyboardSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(KEY_KEYBOARD, isChecked).apply()
            updateKeyboardVisibility(isChecked)
        }

        goButton.setOnClickListener {
            val url = urlInput.text.toString().trim()

            if (url.isEmpty()) {
                showError("Ingresa una URL o IP")
                return@setOnClickListener
            }

            sharedPreferences.edit().putString(KEY_URL, url).apply()

            val fixedUrl = normalizeUrl(url)

            val intent = Intent(this, WebViewActivity::class.java)
            intent.putExtra("URL", fixedUrl)
            intent.putExtra("ENABLE_KEYBOARD", keyboardSwitch.isChecked)
            startActivity(intent)
        }

        exitButton.setOnClickListener {
            finishAffinity()
        }
    }

    private fun updateKeyboardVisibility(enable: Boolean) {
        if (!enable) {
            // Este flag bloquea la aparición del teclado virtual en esta ventana
            window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            // Forzar ocultamiento si ya está fuera
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }
    }

    private fun setupFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

    private fun normalizeUrl(url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url
        }
        val ipPattern = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(:[0-9]+)?$".toRegex()
        return if (url.contains("localhost") || ipPattern.matches(url.split("/")[0])) {
            "http://$url"
        } else {
            "https://$url"
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setupFullScreen()
        }
    }
}