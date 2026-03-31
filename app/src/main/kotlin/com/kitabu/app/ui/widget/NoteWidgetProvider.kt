package com.kitabu.app.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.kitabu.app.MainActivity
import com.kitabu.app.R
import com.kitabu.app.data.KitabuDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NoteWidgetProvider : AppWidgetProvider() {

    companion object {
        const val EXTRA_WIDGET_TYPE = "widget_type"
        const val WIDGET_QUICK_NOTE = "quick_note"
        const val WIDGET_RECENT = "recent"
        const val ACTION_REFRESH = "com.kitabu.app.ACTION_REFRESH_WIDGET"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, NoteWidgetProvider::class.java)
            )
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = KitabuDatabase.getDatabase(context)
            val recentNotes = db.noteDao().getAllNotesRaw().takeLast(5).reversed()

            val views = RemoteViews(context.packageName, R.layout.widget_note_list)

            // Set header
            views.setTextViewText(R.id.widget_title, "Recent Notes")
            views.setTextViewText(R.id.widget_subtitle, "${recentNotes.size} notes")

            // Set note list (simplified - shows last note title)
            if (recentNotes.isNotEmpty()) {
                views.setTextViewText(
                    R.id.widget_note_preview,
                    recentNotes.joinToString("\n") { "- ${it.title.ifBlank { "Untitled" }}" }
                )
            } else {
                views.setTextViewText(R.id.widget_note_preview, "No notes yet")
            }

            // Click to open app
            val openIntent = Intent(context, MainActivity::class.java)
            openIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getActivity(context, appWidgetId, openIntent, flags)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
