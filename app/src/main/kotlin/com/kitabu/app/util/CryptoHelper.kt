package com.kitabu.app.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

/**
 * Provides encryption utilities for Kitabu:
 * 1. EncryptedSharedPreferences for API keys and sensitive settings
 * 2. AES-256-GCM encryption for locked note content
 */
object CryptoHelper {

    private const val KEYSTORE_ALIAS = "kitabu_encryption_key"
    private const val PREFS_FILE = "kitabu_encrypted_prefs"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128

    // ── Encrypted SharedPreferences ──

    /**
     * Get encrypted SharedPreferences instance.
     * Use this instead of getSharedPreferences() for sensitive data.
     */
    fun getEncryptedPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ── Note Content Encryption ──

    /**
     * Get or create the AES-256 encryption key from Android Keystore.
     */
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            val entry = keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }

    /**
     * Encrypt plaintext string using AES-256-GCM.
     * Returns Base64-encoded string: IV + ciphertext.
     */
    fun encrypt(plaintext: String): String {
        if (plaintext.isBlank()) return ""
        val key = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypt Base64-encoded ciphertext (IV + ciphertext) back to plaintext.
     */
    fun decrypt(ciphertext: String): String {
        if (ciphertext.isBlank()) return ""
        try {
            val key = getOrCreateSecretKey()
            val combined = Base64.decode(ciphertext, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, 12)
            val encrypted = combined.copyOfRange(12, combined.size)
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            return String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            // If decryption fails, return as-is (backward compat with unencrypted notes)
            return ciphertext
        }
    }

    /**
     * Check if a string appears to be encrypted (Base64 encoded, starts with known pattern)
     */
    fun isEncrypted(content: String): Boolean {
        if (content.isBlank() || content.length < 20) return false
        return try {
            val decoded = Base64.decode(content, Base64.NO_WRAP)
            decoded.size >= 12 // Must have at least 12-byte IV
        } catch (e: Exception) {
            false
        }
    }
}
