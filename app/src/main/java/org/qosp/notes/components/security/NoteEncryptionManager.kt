package org.qosp.notes.components.security

import android.util.Base64
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.qosp.notes.data.model.Note
import org.qosp.notes.data.model.NoteTask
import org.qosp.notes.preferences.PreferenceRepository
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class NoteEncryptionManager(
    private val preferenceRepository: PreferenceRepository,
) {

    suspend fun decryptIfNeeded(note: Note): DecryptionResult {
        if (!isEncryptedContent(note.content)) return DecryptionResult(note = note, isEncrypted = false)

        return runCatching {
            val encrypted = note.content.removePrefix(CONTENT_PREFIX)
            val split = encrypted.split(":", limit = 2)
            require(split.size == 2)

            val iv = Base64.decode(split[0], Base64.NO_WRAP)
            val ciphertext = Base64.decode(split[1], Base64.NO_WRAP)
            val payloadJson = decrypt(ciphertext, iv)
            val payload = Json.decodeFromString(EncryptedNotePayload.serializer(), payloadJson)

            DecryptionResult(
                note = note.copy(
                    title = payload.title,
                    content = payload.content,
                    isList = payload.isList,
                    taskList = payload.taskList,
                    isMarkdownEnabled = payload.isMarkdownEnabled,
                ),
                isEncrypted = true,
            )
        }.getOrElse {
            DecryptionResult(
                note = note.copy(
                    title = ENCRYPTED_TITLE_PLACEHOLDER,
                    content = "",
                    isList = false,
                    taskList = emptyList(),
                ),
                isEncrypted = true,
                hasError = true,
            )
        }
    }

    suspend fun encryptNote(note: Note): Note {
        if (isEncryptedContent(note.content)) return note

        val payload = EncryptedNotePayload(
            title = note.title,
            content = note.content,
            isList = note.isList,
            taskList = note.taskList,
            isMarkdownEnabled = note.isMarkdownEnabled,
        )

        val payloadJson = Json.encodeToString(EncryptedNotePayload.serializer(), payload)
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val cipherText = encrypt(payloadJson, iv)
        val encodedIv = Base64.encodeToString(iv, Base64.NO_WRAP)
        val encodedCipher = Base64.encodeToString(cipherText, Base64.NO_WRAP)

        return note.copy(
            title = ENCRYPTED_TITLE_PLACEHOLDER,
            content = "$CONTENT_PREFIX$encodedIv:$encodedCipher",
            isList = false,
            taskList = emptyList(),
        )
    }

    fun isEncryptedContent(content: String): Boolean {
        return content.startsWith(CONTENT_PREFIX)
    }

    private suspend fun getOrCreateKey(): SecretKeySpec {
        val existing = preferenceRepository.getEncryptedString(PreferenceRepository.NOTE_ENCRYPTION_KEY).first()
        val keyBytes = if (existing.isBlank()) {
            ByteArray(32).also(SecureRandom()::nextBytes).also {
                val encoded = Base64.encodeToString(it, Base64.NO_WRAP)
                preferenceRepository.putEncryptedStrings(PreferenceRepository.NOTE_ENCRYPTION_KEY to encoded)
            }
        } else {
            Base64.decode(existing, Base64.NO_WRAP)
        }

        return SecretKeySpec(keyBytes, ALGORITHM)
    }

    private suspend fun encrypt(text: String, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(text.toByteArray(Charsets.UTF_8))
    }

    private suspend fun decrypt(bytes: ByteArray, iv: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(bytes), Charsets.UTF_8)
    }

    @Serializable
    data class EncryptedNotePayload(
        val title: String,
        val content: String,
        val isList: Boolean,
        val taskList: List<NoteTask>,
        val isMarkdownEnabled: Boolean,
    )

    data class DecryptionResult(
        val note: Note,
        val isEncrypted: Boolean,
        val hasError: Boolean = false,
    )

    companion object {
        private const val CONTENT_PREFIX = "qenc1:"
        private const val ENCRYPTED_TITLE_PLACEHOLDER = "Encrypted note"
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
    }
}
