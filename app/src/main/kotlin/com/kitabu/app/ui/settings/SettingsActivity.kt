package com.kitabu.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.kitabu.app.databinding.ActivitySettingsBinding
import com.kitabu.app.util.CryptoHelper
import com.kitabu.app.util.ThemeManager
import com.kitabu.app.R

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.apply(this)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        // Use encrypted preferences
        try {
            val prefs = CryptoHelper.getEncryptedPrefs(this)
            binding.etApiKey.setText(prefs.getString("gemini_api_key", "") ?: "")

            binding.btnSaveApiKey.setOnClickListener {
                val key = binding.etApiKey.text.toString().trim()
                prefs.edit().putString("gemini_api_key", key).apply()
                Snackbar.make(binding.root, "API key saved securely", Snackbar.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            // Fallback to regular prefs if encryption fails
            val prefs = getSharedPreferences("kitabu_prefs", MODE_PRIVATE)
            binding.etApiKey.setText(prefs.getString("gemini_api_key", ""))

            binding.btnSaveApiKey.setOnClickListener {
                val key = binding.etApiKey.text.toString().trim()
                prefs.edit().putString("gemini_api_key", key).apply()
                Snackbar.make(binding.root, "API key saved", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.btnGetApiKey.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey")))
        }

        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            binding.tvVersion.text = "Kitabu v${pInfo.versionName} (${pInfo.versionCode})"
        } catch (_: Exception) {
            binding.tvVersion.text = "Kitabu v3.0.0"
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == android.R.id.home) { finish(); true } else super.onOptionsItemSelected(item)
}
