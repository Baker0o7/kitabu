package com.kitabu.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,               // e.g. "work/meetings"
    val color: Int = 0xFF6C63FF.toInt(),
    val createdAt: Long = System.currentTimeMillis()
) {
    /** Top-level name segment, e.g. "work" from "work/meetings" */
    val topLevel: String get() = name.substringBefore("/")

    /** Leaf segment, e.g. "meetings" from "work/meetings" */
    val leaf: String get() = name.substringAfterLast("/")

    /** Full display with # prefix */
    val display: String get() = "#$name"
}
