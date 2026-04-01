package com.kitabu.app.util

import android.content.Context
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.linkify.LinkifyPlugin

object MarkdownHelper {

    fun buildMarkwon(context: Context): Markwon = Markwon.builder(context)
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(context))
        .usePlugin(TaskListPlugin.create(context))
        .usePlugin(HtmlPlugin.create())
        .usePlugin(LinkifyPlugin.create())
        .build()

    /** Resolve template variables: {{date}}, {{time}}, {{day}} */
    fun resolveTemplateVars(content: String): String {
        val today = java.time.LocalDate.now()
        val now = java.time.LocalTime.now()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
        return content
            .replace("{{date}}", today.toString())
            .replace("{{time}}", now.format(formatter))
            .replace("{{day}}", today.dayOfWeek.toString().lowercase().replaceFirstChar { it.uppercase() })
    }

    /** Extract all [[wikilinks]] from content */
    fun extractWikiLinks(content: String): List<String> {
        val regex = Regex("""\[\[([^\]]+)]]""")
        return regex.findAll(content).map { it.groupValues[1].trim() }.distinct().toList()
    }

    /** Replace [[Title]] with clickable markdown links */
    fun renderWikiLinks(content: String): String {
        return content.replace(Regex("""\[\[([^\]]+)]]""")) { mr ->
            "[${mr.groupValues[1]}](#wiki-${mr.groupValues[1].lowercase().replace(" ", "-")})"
        }
    }

    /** Count words in markdown source (strips syntax) */
    fun wordCount(markdown: String): Int {
        val stripped = markdown
            .replace(Regex("#+\\s"), "")
            .replace(Regex("[*_`~>\\[\\]()#-]"), " ")
            .trim()
        return if (stripped.isEmpty()) 0 else stripped.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }

    /** Extract first heading (H1 or first non-empty line) for preview */
    fun extractTitle(content: String): String {
        val h1Match = Regex("^#\\s+(.+)$", RegexOption.MULTILINE).find(content)
        if (h1Match != null) return h1Match.groupValues[1].trim()
        val lines = content.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        return lines.firstOrNull()?.trim()?.take(80) ?: ""
    }

    /** Check if content is mostly empty (no real text) */
    fun isContentEmpty(content: String): Boolean {
        val stripped = content.replace(Regex("[#*_`~>\\[\\]()!\\s-]"), "")
        return stripped.isBlank()
    }
}
