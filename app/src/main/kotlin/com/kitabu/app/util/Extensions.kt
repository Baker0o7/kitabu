package com.kitabu.app.util

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

fun View.show()      { visibility = View.VISIBLE }
fun View.hide()      { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

fun View.hideKeyboard() {
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
        .hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard() {
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
        .showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun Context.toast(msg: String, long: Boolean = false) =
    Toast.makeText(this, msg, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()

private val fmtShort = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
private val fmtFull  = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
private val fmtTime  = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

fun Long.toNoteDate(): String {
    val diff = System.currentTimeMillis() - this
    val days = diff / 86_400_000L
    return when {
        diff  < 60_000L    -> "Just now"
        diff  < 3_600_000L -> "${diff / 60_000}m ago"
        days == 0L         -> {
            val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
            ldt.format(fmtTime)
        }
        days == 1L         -> "Yesterday"
        days  < 7          -> "${days}d ago"
        sameYear(this)     -> {
            val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
            ldt.format(fmtShort)
        }
        else               -> {
            val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
            ldt.format(fmtFull)
        }
    }
}

fun Long.toFullDate(): String {
    val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
    return ldt.format(fmtFull)
}

private fun sameYear(ts: Long): Boolean {
    val a = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault()).year
    val b = LocalDateTime.now().year
    return a == b
}

/** Truncate text to maxLen chars, adding ellipsis if needed */
fun String.truncate(maxLen: Int): String {
    return if (length <= maxLen) this else take(maxLen) + "…"
}

/** Strip markdown formatting for plain text preview */
fun String.stripMarkdown(): String {
    return this
        .replace(Regex("^#{1,6}\\s+"), "")
        .replace(Regex("\\*{1,2}([^*]+)\\*{1,2}"), "$1")
        .replace(Regex("_{1,2}([^_]+)_{1,2}"), "$1")
        .replace(Regex("~~([^~]+)~~"), "$1")
        .replace(Regex("`([^`]+)`"), "$1")
        .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
        .replace(Regex("^[-*+]\\s", RegexOption.MULTILINE), "• ")
        .replace(Regex("^\\d+\\.\\s", RegexOption.MULTILINE), "")
        .replace(Regex("\\[([ xX])\\]\\s*"), "☐ ")
        .trim()
}
