package com.kitabu.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.kitabu.app.databinding.ActivitySettingsBinding
import com.kitabu.app.util.ThemeManager

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

        val prefs = getSharedPreferences("kitabu_prefs", MODE_PRIVATE)

        // Load saved key
        binding.etApiKey.setText(prefs.getString("gemini_api_key", ""))

        // Save key
        binding.btnSaveApiKey.setOnClickListener {
            val key = binding.etApiKey.text.toString().trim()
            prefs.edit().putString("gemini_api_key", key).apply()
            Snackbar.make(binding.root, "API key saved", Snackbar.LENGTH_SHORT).show()
        }

        // Open Google AI Studio in browser
        binding.btnGetApiKey.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey")))
        }

        // Version info
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            binding.tvVersion.text = "Kitabu v${pInfo.versionName} (${pInfo.versionCode})"
        } catch (_: Exception) {
            binding.tvVersion.text = "Kitabu v2.1.0"
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == android.R.id.home) { finish(); true } else super.onOptionsItemSelected(item)
}
