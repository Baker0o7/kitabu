package com.kitabu.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    // --- Notes with Tags ---

    @Transaction
    @Query("SELECT * FROM notes WHERE isArchived = 0 AND isTrashed = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotesWithTags(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("""
        SELECT DISTINCT notes.* FROM notes
        LEFT JOIN note_tags ON notes.id = note_tags.noteId
        WHERE (notes.title LIKE '%' || :q || '%' OR notes.content LIKE '%' || :q || '%')
        AND notes.isArchived = 0 AND notes.isTrashed = 0
        ORDER BY notes.isPinned DESC, notes.updatedAt DESC
    """)
    fun searchNotesWithTags(q: String): Flow<List<NoteWithTags>>

    // --- FTS4 Full-Text Search ---
    // Note: @SkipQueryVerification is needed because notes_fts is a virtual
    // FTS4 table created via migration, not a Room-managed entity.

    @SkipQueryVerification
    @Query("SELECT * FROM notes WHERE id IN (SELECT rowid FROM notes_fts WHERE notes_fts MATCH :query) AND isArchived = 0 AND isTrashed = 0")
    fun searchNotesFts(query: String): Flow<List<Note>>

    @SkipQueryVerification
    @Transaction
    @Query("SELECT * FROM notes WHERE id IN (SELECT rowid FROM notes_fts WHERE notes_fts MATCH :query) AND isArchived = 0 AND isTrashed = 0")
    fun searchNotesWithTagsFts(query: String): Flow<List<NoteWithTags>>

    @Transaction
    @Query("""
        SELECT DISTINCT notes.* FROM notes
        INNER JOIN note_tags ON notes.id = note_tags.noteId
        WHERE note_tags.tagId = :tagId
        AND notes.isArchived = 0 AND notes.isTrashed = 0
        ORDER BY notes.isPinned DESC, notes.updatedAt DESC
    """)
    fun getNotesByTagWithTags(tagId: Int): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isDaily = 1 AND isArchived = 0 AND isTrashed = 0 ORDER BY dailyDate DESC")
    fun getDailyNotesWithTags(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isArchived = 1 AND isTrashed = 0 ORDER BY updatedAt DESC")
    fun getArchivedNotesWithTags(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE folderId IS NULL AND isArchived = 0 AND isTrashed = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getRootNotesWithTags(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE folderId = :folderId AND isArchived = 0 AND isTrashed = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesInFolderWithTags(folderId: Int?): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteWithTagsById(id: Int): NoteWithTags?

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): Note?

    @Query("SELECT * FROM notes WHERE isDaily = 1 AND dailyDate = :date LIMIT 1")
    suspend fun getDailyNote(date: String): Note?

    @Query("SELECT * FROM notes WHERE reminderTime IS NOT NULL AND reminderTime > :now AND isArchived = 0 AND isTrashed = 0 ORDER BY reminderTime ASC")
    fun getNotesWithReminders(now: Long = System.currentTimeMillis()): Flow<List<Note>>

    // Wikilink: find notes whose title matches
    @Query("SELECT * FROM notes WHERE LOWER(title) = LOWER(:title) AND id != :excludeId LIMIT 1")
    suspend fun findNoteByTitle(title: String, excludeId: Int = -1): Note?

    // Autocomplete: find notes whose title starts with prefix
    @Query("SELECT * FROM notes WHERE title LIKE :prefix || '%' AND isArchived = 0 AND isTrashed = 0 ORDER BY updatedAt DESC LIMIT 10")
    suspend fun searchTitlesByPrefix(prefix: String): List<Note>

    // Backlinks: find notes whose content contains [[title]]
    @Query("SELECT * FROM notes WHERE content LIKE '%[[' || :title || ']]%' AND id != :excludeId AND isTrashed = 0")
    fun getBacklinks(title: String, excludeId: Int = -1): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)

    @Query("SELECT COUNT(*) FROM notes WHERE isArchived = 0 AND isTrashed = 0")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM notes WHERE isArchived = 1")
    suspend fun getArchivedCount(): Int

    @Query("SELECT COUNT(*) FROM notes WHERE folderId IS NULL AND isArchived = 0 AND isTrashed = 0")
    suspend fun getRootNoteCount(): Int

    @Query("SELECT COUNT(*) FROM notes WHERE folderId = :folderId AND isArchived = 0 AND isTrashed = 0")
    suspend fun getNoteCountInFolder(folderId: Int?): Int

    // --- Note Tags ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTag(noteTag: NoteTag)

    @Delete
    suspend fun deleteNoteTag(noteTag: NoteTag)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun clearNoteTags(noteId: Int)

    // --- Trash ---

    @Transaction
    @Query("SELECT * FROM notes WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    fun getTrashedNotesWithTags(): Flow<List<NoteWithTags>>

    @Query("SELECT COUNT(*) FROM notes WHERE isTrashed = 1")
    suspend fun getTrashedCount(): Int

    @Query("DELETE FROM notes WHERE isTrashed = 1 AND trashedAt < :cutoffTime")
    suspend fun purgeExpiredTrash(cutoffTime: Long)

    // --- Favorites ---

    @Transaction
    @Query("SELECT * FROM notes WHERE isFavorite = 1 AND isArchived = 0 AND isTrashed = 0 ORDER BY updatedAt DESC")
    fun getFavoriteNotesWithTags(): Flow<List<NoteWithTags>>

    // --- Non-flow versions for export ---

    @Query("SELECT * FROM notes ORDER BY id")
    suspend fun getAllNotesRaw(): List<Note>

    @Query("SELECT * FROM note_tags ORDER BY noteId, tagId")
    suspend fun getAllNoteTagsRaw(): List<NoteTag>

    @Query("SELECT * FROM note_versions ORDER BY noteId, savedAt DESC")
    suspend fun getAllVersionsRaw(): List<NoteVersion>
}
