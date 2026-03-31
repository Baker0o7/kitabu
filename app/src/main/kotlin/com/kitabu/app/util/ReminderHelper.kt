package com.kitabu.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.kitabu.app.data.Note
import com.kitabu.app.ui.reminder.ReminderReceiver
import java.util.Calendar

/**
 * Manages scheduled reminders for notes using AlarmManager.
 */
object ReminderHelper {

    private const val REMINDER_REQUEST_BASE = 1000

    /**
     * Schedule a reminder notification for a note at its reminderTime.
     */
    fun scheduleReminder(context: Context, note: Note) {
        val reminderTime = note.reminderTime ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_NOTE_ID, note.id)
            putExtra(ReminderReceiver.EXTRA_NOTE_TITLE, note.title)
            putExtra(ReminderReceiver.EXTRA_NOTE_CONTENT, note.content.take(200))
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context, REMINDER_REQUEST_BASE + note.id, intent, flags
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent
        )
    }

    /**
     * Cancel a previously scheduled reminder for a note.
     */
    fun cancelReminder(context: Context, noteId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context, REMINDER_REQUEST_BASE + noteId, intent, flags
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
