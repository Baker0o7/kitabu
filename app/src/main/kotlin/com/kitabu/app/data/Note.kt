package com.kitabu.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val color: Int = NoteColor.DEFAULT,
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val isDaily: Boolean = false,
    val dailyDate: String? = null,   // "2026-03-18"
    val templateId: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

object NoteColor {
    const val DEFAULT  = 0xFF1E1E2E.toInt()
    const val ROSE     = 0xFF2D1B2E.toInt()
    const val OCEAN    = 0xFF0D2137.toInt()
    const val FOREST   = 0xFF0D2818.toInt()
    const val AMBER    = 0xFF2D1F00.toInt()
    const val LAVENDER = 0xFF1A1A2E.toInt()
    val all = listOf(DEFAULT, ROSE, OCEAN, FOREST, AMBER, LAVENDER)
}
