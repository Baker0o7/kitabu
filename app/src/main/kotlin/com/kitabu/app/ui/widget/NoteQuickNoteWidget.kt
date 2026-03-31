package com.kitabu.app.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent as AndroidIntent
import android.widget.RemoteViews
import com.kitabu.app.R

class NoteQuickNoteWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_note)
            views.setTextViewText(R.id.widget_qn_title, "Quick Note")

            val openIntent = AndroidIntent(context, Class.forName("com.kitabu.app.ui.editor.EditorActivity"))
            openIntent.flags = AndroidIntent.FLAG_ACTIVITY_NEW_TASK or AndroidIntent.FLAG_ACTIVITY_CLEAR_TOP
            val pFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getActivity(context, appWidgetId, openIntent, pFlags)
            views.setOnClickPendingIntent(R.id.widget_qn_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}
