package com.kitabu.app.util

import android.content.Context

/**
 * Securely stores and retrieves sensitive data like API keys using EncryptedSharedPreferences.
 */
object SecurePrefsHelper {

    private const val KEY_AI_API_KEY = "ai_api_key"
    private const val KEY_AI_MODEL = "ai_model"
    private const val KEY_MASTER_PASSWORD_HASH = "master_password_hash"

    fun getAIApiKey(context: Context): String {
        return CryptoHelper.getEncryptedPrefs(context).getString(KEY_AI_API_KEY, "") ?: ""
    }

    fun setAIApiKey(context: Context, apiKey: String) {
        CryptoHelper.getEncryptedPrefs(context).edit().putString(KEY_AI_API_KEY, apiKey).apply()
    }

    fun getAIModel(context: Context): String {
        return CryptoHelper.getEncryptedPrefs(context).getString(KEY_AI_MODEL, "gemini-2.0-flash") ?: "gemini-2.0-flash"
    }

    fun setAIModel(context: Context, model: String) {
        CryptoHelper.getEncryptedPrefs(context).edit().putString(KEY_AI_MODEL, model).apply()
    }

    fun getMasterPasswordHash(context: Context): String {
        return CryptoHelper.getEncryptedPrefs(context).getString(KEY_MASTER_PASSWORD_HASH, "") ?: ""
    }

    fun setMasterPasswordHash(context: Context, hash: String) {
        CryptoHelper.getEncryptedPrefs(context).edit().putString(KEY_MASTER_PASSWORD_HASH, hash).apply()
    }

    fun hasApiKey(context: Context): Boolean {
        return getAIApiKey(context).isNotBlank()
    }

    fun clearAll(context: Context) {
        CryptoHelper.getEncryptedPrefs(context).edit().clear().apply()
    }
}
