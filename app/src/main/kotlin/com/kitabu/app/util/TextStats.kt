package com.kitabu.app.util

data class TextStats(
    val words: Int,
    val chars: Int,
    val charsNoSpaces: Int,
    val sentences: Int,
    val paragraphs: Int,
    val readingTimeMin: Int
) {
    val label: String get() = buildString {
        if (words == 0) return@buildString
        append("$words word${if (words != 1) "s" else ""}")
        append(" · $chars chars")
        if (readingTimeMin > 0) {
            append(" · ~${readingTimeMin} min read")
        }
    }
}

fun String.textStats(): TextStats {
    val trimmed = trim()
    if (trimmed.isEmpty()) return TextStats(0, 0, 0, 0, 0, 0)
    val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    val sentences = trimmed.split(Regex("[.!?]+\\s")).filter { it.isNotBlank() }.size
    val paragraphs = trimmed.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() }.size.coerceAtLeast(1)
    val readingTime = (words / 200.0).toInt().coerceAtLeast(1)
    return TextStats(
        words = words,
        chars = trimmed.length,
        charsNoSpaces = trimmed.replace("\\s".toRegex(), "").length,
        sentences = sentences,
        paragraphs = paragraphs,
        readingTimeMin = readingTime
    )
}
