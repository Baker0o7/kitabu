package com.kitabu.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    // --- Notes with Tags ---

    @Transaction
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotesWithTags(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("""
        SELECT DISTINCT notes.* FROM notes
        LEFT JOIN note_tags ON notes.id = note_tags.noteId
        WHERE (notes.title LIKE '%' || :q || '%' OR notes.content LIKE '%' || :q || '%')
        ORDER BY notes.isPinned DESC, notes.updatedAt DESC
    """)
    fun searchNotesWithTags(q: String): Flow<List<NoteWithTags>>

    @Transaction
    @Query("""
        SELECT DISTINCT notes.* FROM notes
        INNER JOIN note_tags ON notes.id = note_tags.noteId
        WHERE note_tags.tagId = :tagId
        ORDER BY notes.isPinned DESC, notes.updatedAt DESC
    """)
    fun getNotesByTagWithTags(tagId: Int): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isDaily = 1 ORDER BY dailyDate DESC")
    fun getDailyNotesWithTags(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteWithTagsById(id: Int): NoteWithTags?

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): Note?

    @Query("SELECT * FROM notes WHERE isDaily = 1 AND dailyDate = :date LIMIT 1")
    suspend fun getDailyNote(date: String): Note?

    // Wikilink: find notes whose title matches
    @Query("SELECT * FROM notes WHERE LOWER(title) = LOWER(:title) LIMIT 1")
    suspend fun findNoteByTitle(title: String): Note?

    // Backlinks: find notes whose content contains [[title]]
    @Query("SELECT * FROM notes WHERE content LIKE '%[[' || :title || ']]%'")
    fun getBacklinks(title: String): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun getCount(): Int

    // --- Note Tags ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTag(noteTag: NoteTag)

    @Delete
    suspend fun deleteNoteTag(noteTag: NoteTag)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun clearNoteTags(noteId: Int)
}
