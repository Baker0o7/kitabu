package com.kitabu.app.ui.ai

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kitabu.app.databinding.ActivityAiAssistantBinding
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import com.kitabu.app.util.ThemeManager

/**
 * AI Assistant powered by Google Gemini 2.0 Flash.
 *
 * FREE tier (no credit card): 15 requests/min, 1,500 requests/day, 1M tokens/day.
 * Get a key at https://aistudio.google.com — takes 30 seconds.
 */
class AiAssistantActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTE_CONTENT = "extra_note_content"
        private const val PREF_KEY    = "gemini_api_key"
        private const val MODEL       = "gemini-2.0-flash"
        private const val ENDPOINT    = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"
    }

    private lateinit var binding: ActivityAiAssistantBinding
    private val client = OkHttpClient()
    private var noteContent = ""

    // Gemini uses "user" / "model" (not "assistant")
    private val history = mutableListOf<Pair<String, String>>() // role to text

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.apply(this)
        binding = ActivityAiAssistantBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "✨ AI Assistant  •  Gemini Flash"

        noteContent = intent.getStringExtra(EXTRA_NOTE_CONTENT).orEmpty()

        mapOf(
            binding.btnSummarise  to "Summarise this note in 3 concise bullet points.",
            binding.btnExpand     to "Expand this note with more detail, context and examples.",
            binding.btnRewrite    to "Rewrite this note more clearly and concisely.",
            binding.btnFlashcards to "Generate 5 flashcard Q&A pairs from this note in Q: / A: format.",
            binding.btnTags       to "Suggest 3–5 relevant tags for this note. Use #hashtag format.",
            binding.btnOutline    to "Create a clean structured Markdown outline from this note."
        ).forEach { (btn, prompt) -> btn.setOnClickListener { ask(prompt) } }

        binding.btnSend.setOnClickListener {
            val msg = binding.etMessage.text.toString().trim()
            if (msg.isNotBlank()) { binding.etMessage.setText(""); ask(msg) }
        }
    }

    // ── Core ask ─────────────────────────────────────────────────────────────

    private fun ask(prompt: String) {
        val apiKey = getSharedPreferences("kitabu_prefs", MODE_PRIVATE)
            .getString(PREF_KEY, "").orEmpty().trim()

        appendBubble(sender = "You", text = prompt, isUser = true)
        binding.progressBar.visibility = View.VISIBLE

        if (apiKey.isBlank()) {
            appendBubble(
                sender = "Kitabu AI",
                text   = "⚙️  No API key set.\n\nGo to **☰ → Settings → AI Settings** and paste your free Google AI Studio key.\n\nGet one in 30 seconds at **aistudio.google.com** — no credit card needed.",
                isUser = false
            )
            binding.progressBar.visibility = View.GONE
            return
        }

        history.add("user" to prompt)
        val requestBody = buildGeminiBody()
        val request = Request.Builder()
            .url("$ENDPOINT?key=$apiKey")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        lifecycleScope.launch {
            try {
                val reply = withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { resp ->
                        val raw  = resp.body?.string() ?: "{}"
                        val json = JSONObject(raw)

                        // Surface API errors clearly
                        if (json.has("error")) {
                            val err = json.getJSONObject("error")
                            val code = err.optInt("code", 0)
                            val msg  = err.optString("message", "Unknown error")
                            return@use when (code) {
                                400 -> "❌ Bad request — check your API key format."
                                401, 403 -> "🔑 Invalid API key. Make sure you copied it correctly from aistudio.google.com."
                                429 -> "⏳ Rate limit hit (15 req/min on free tier). Wait a moment and try again."
                                else -> "⚠️ Gemini error $code: $msg"
                            }
                        }

                        json.optJSONArray("candidates")
                            ?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.optJSONObject(0)
                            ?.optString("text", "No response")
                            ?: "Empty response from Gemini."
                    }
                }
                history.add("model" to reply)
                appendBubble(sender = "Kitabu AI", text = reply, isUser = false)
            } catch (e: IOException) {
                appendBubble(sender = "Kitabu AI", text = "🌐 Network error: ${e.message}", isUser = false)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    // ── Gemini request body ───────────────────────────────────────────────────

    private fun buildGeminiBody(): String {
        // System instruction
        val systemParts = JSONArray().put(
            JSONObject().put("text", buildSystemPrompt())
        )
        val systemInstruction = JSONObject().put("parts", systemParts)

        // Conversation history → Gemini "contents" array
        val contents = JSONArray()
        history.forEach { (role, text) ->
            contents.put(
                JSONObject()
                    .put("role", role)
                    .put("parts", JSONArray().put(JSONObject().put("text", text)))
            )
        }

        val generationConfig = JSONObject()
            .put("maxOutputTokens", 1024)
            .put("temperature", 0.7)

        return JSONObject()
            .put("system_instruction", systemInstruction)
            .put("contents", contents)
            .put("generationConfig", generationConfig)
            .toString()
    }

    private fun buildSystemPrompt() = buildString {
        append("You are Kitabu AI, a smart assistant embedded in a Markdown notes app. ")
        append("Be helpful, concise, and format all responses in Markdown. ")
        append("Use **bold**, bullet lists, and code blocks where they add clarity. ")
        if (noteContent.isNotBlank()) {
            append("\n\nThe user is currently editing this note — use it as context:\n\n")
            append("---\n")
            append(noteContent.take(6000)) // stay within context limits
            append("\n---")
        }
    }

    // ── Chat bubbles ──────────────────────────────────────────────────────────

    private fun appendBubble(sender: String, text: String, isUser: Boolean) {
        val sep    = if (binding.tvChat.text.isBlank()) "" else "\n\n"
        val prefix = if (isUser) "→ You" else "✦ $sender"
        binding.tvChat.append("$sep$prefix\n$text")
        binding.scrollView.post { binding.scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == android.R.id.home) { finish(); true }
        else super.onOptionsItemSelected(item)
}
