package com.kitabu.app.util

import android.content.Context
import android.graphics.Color
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
        .usePlugin(TaskListPlugin.create(Color.parseColor("#C8A2FF"), Color.parseColor("#C8A2FF"), Color.WHITE))
        .usePlugin(HtmlPlugin.create())
        .usePlugin(LinkifyPlugin.create())
        .build()

    /** Resolve template variables: {{date}} → today */
    fun resolveTemplateVars(content: String): String {
        val today = java.time.LocalDate.now().toString()
        return content.replace("{{date}}", today)
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
        return if (stripped.isEmpty()) 0 else stripped.split(Regex("\\s+")).size
    }
}
