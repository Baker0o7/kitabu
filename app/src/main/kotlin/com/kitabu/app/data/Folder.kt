package com.kitabu.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    indices = [Index("parentFolderId")],
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["parentFolderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val parentFolderId: Int? = null,  // For nested folders
    val color: Int = 0xFF1E1E2E.toInt(),
    val icon: String = "📁",  // Emoji or material icon name
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

object FolderColor {
    const val DEFAULT  = 0xFF1E1E2E.toInt()
    const val BLUE     = 0xFF1E3A8A.toInt()
    const val GREEN    = 0xFF15803D.toInt()
    const val RED      = 0xFFB91C1C.toInt()
    const val PURPLE   = 0xFF6B21A8.toInt()
    const val ORANGE   = 0xFFEA580C.toInt()
    const val TEAL     = 0xFF0D9488.toInt()
    const val PINK     = 0xFFDB2777.toInt()
    const val YELLOW   = 0xFFCA8A04.toInt()
    const val GRAY     = 0xFF6B7280.toInt()
    const val INDIGO   = 0xFF4F46E5.toInt()
    val all = listOf(DEFAULT, BLUE, GREEN, RED, PURPLE, ORANGE, TEAL, PINK, YELLOW, GRAY, INDIGO)
}
