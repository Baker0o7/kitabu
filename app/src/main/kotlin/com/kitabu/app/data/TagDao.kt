package com.kitabu.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTagById(id: Int): Tag?

    @Query("SELECT * FROM tags WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findTagByName(name: String): Tag?

    @Query("""
        SELECT tags.* FROM tags
        INNER JOIN note_tags ON tags.id = note_tags.tagId
        WHERE note_tags.noteId = :noteId
        ORDER BY tags.name ASC
    """)
    suspend fun getTagsForNote(noteId: Int): List<Tag>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: Tag): Long

    @Update
    suspend fun updateTag(tag: Tag)

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTagById(id: Int)

    @Query("SELECT COUNT(DISTINCT noteId) FROM note_tags WHERE tagId = :tagId")
    suspend fun getNoteCountForTag(tagId: Int): Int

    @Query("SELECT * FROM tags ORDER BY id")
    suspend fun getAllTagsRaw(): List<Tag>
}
