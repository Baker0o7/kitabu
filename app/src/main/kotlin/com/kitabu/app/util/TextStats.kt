package com.kitabu.app.util

data class TextStats(val words: Int, val chars: Int)

fun String.textStats(): TextStats {
    val trimmed = trim()
    val words = if (trimmed.isEmpty()) 0 else trimmed.split(Regex("\\s+")).size
    return TextStats(words = words, chars = trimmed.length)
}

fun TextStats.label(): String = when {
    words == 0 -> ""
    words == 1 -> "1 word"
    else       -> "$words words · $chars chars"
}
