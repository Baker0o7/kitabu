package com.kitabu.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "note_tags",
    primaryKeys = ["noteId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = Note::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Tag::class,  parentColumns = ["id"], childColumns = ["tagId"],  onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("noteId"), Index("tagId")]
)
data class NoteTag(
    val noteId: Int,
    val tagId: Int
)
