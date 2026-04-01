package com.kitabu.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.kitabu.app.databinding.ActivitySettingsBinding
import com.kitabu.app.util.SecurePrefsHelper
import com.kitabu.app.util.ThemeManager
import com.kitabu.app.R

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    private val aiModelOptions = arrayOf(
        "gemini-2.0-flash",
        "gemini-2.0-pro",
        "gemini-1.5-flash"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        // ── API Key ──
        binding.etApiKey.setText(SecurePrefsHelper.getAIApiKey(this))

        binding.btnSaveApiKey.setOnClickListener {
            val key = binding.etApiKey.text.toString().trim()
            SecurePrefsHelper.setAIApiKey(this, key)
            Snackbar.make(binding.root, "API key saved securely", Snackbar.LENGTH_SHORT).show()
        }

        binding.btnGetApiKey.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey")))
        }

        // ── AI Model Spinner ──
        val modelAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, aiModelOptions)
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAiModel.adapter = modelAdapter

        val currentModel = SecurePrefsHelper.getAIModel(this)
        val modelIndex = aiModelOptions.indexOf(currentModel).coerceAtLeast(0)
        binding.spinnerAiModel.setSelection(modelIndex)

        binding.spinnerAiModel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                SecurePrefsHelper.setAIModel(this@SettingsActivity, aiModelOptions[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        // ── Light/Dark Mode Toggle ──
        binding.switchLightMode.isChecked = ThemeManager.isLightMode(this)

        binding.switchLightMode.setOnCheckedChangeListener { _, isChecked ->
            ThemeManager.setLightMode(this, isChecked)
            recreate()
        }

        // ── Version Info ──
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
