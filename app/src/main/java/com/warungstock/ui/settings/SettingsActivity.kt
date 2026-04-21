package com.warungstock.ui.settings

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import com.warungstock.R

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchDark: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)

        switchDark = findViewById(R.id.switchDark)

        // load saved mode
        val isDark = prefs.getBoolean("dark_mode", false)
        switchDark.isChecked = isDark

        applyDarkMode(isDark)

        switchDark.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            applyDarkMode(isChecked)
        }

        loadSummary()
    }

    private fun applyDarkMode(isDark: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (isDark)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun loadSummary() {
        // sementara dummy (nanti kita sambung Room)
        findViewById<TextView>(R.id.tvTotalBarang).text = "12"
        findViewById<TextView>(R.id.tvTotalStok).text = "120"
        findViewById<TextView>(R.id.tvNilaiStok).text = "Rp 1.200.000"
    }
}