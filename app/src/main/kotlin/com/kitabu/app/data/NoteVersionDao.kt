package com.kitabu.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteVersionDao {

    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY savedAt DESC")
    fun getVersionsForNote(noteId: Int): Flow<List<NoteVersion>>

    @Insert
    suspend fun insertVersion(version: NoteVersion)

    @Delete
    suspend fun deleteVersion(version: NoteVersion)

    // Keep only the last N versions per note
    @Query("""
        DELETE FROM note_versions WHERE noteId = :noteId
        AND id NOT IN (
            SELECT id FROM note_versions WHERE noteId = :noteId
            ORDER BY savedAt DESC LIMIT :keep
        )
    """)
    suspend fun pruneVersions(noteId: Int, keep: Int = 30)
}
