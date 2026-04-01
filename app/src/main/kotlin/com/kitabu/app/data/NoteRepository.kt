package com.kitabu.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class NoteRepository(
    private val database: KitabuDatabase,
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val versionDao: NoteVersionDao
) {

    // --- Notes ---

    val allNotes: Flow<List<NoteWithTags>> = noteDao.getAllNotesWithTags()
    val dailyNotes: Flow<List<NoteWithTags>> = noteDao.getDailyNotesWithTags()
    val archivedNotes: Flow<List<NoteWithTags>> = noteDao.getArchivedNotesWithTags()
    val notesWithReminders: Flow<List<Note>> = noteDao.getNotesWithReminders()
    val trashedNotes: Flow<List<NoteWithTags>> = noteDao.getTrashedNotesWithTags()
    val favoriteNotes: Flow<List<NoteWithTags>> = noteDao.getFavoriteNotesWithTags()

    fun searchNotes(q: String): Flow<List<NoteWithTags>> = noteDao.searchNotesWithTagsFts(sanitizeFtsQuery(q))
    fun getNotesByTag(tagId: Int): Flow<List<NoteWithTags>> = noteDao.getNotesByTagWithTags(tagId)
    fun getBacklinks(title: String, excludeId: Int = -1): Flow<List<Note>> = noteDao.getBacklinks(title, excludeId)

    suspend fun getNoteById(id: Int): Note? = noteDao.getNoteById(id)
    suspend fun getNoteWithTagsById(id: Int): NoteWithTags? = noteDao.getNoteWithTagsById(id)
    suspend fun findNoteByTitle(title: String, excludeId: Int = -1): Note? = noteDao.findNoteByTitle(title, excludeId)
    suspend fun searchTitlesByPrefix(prefix: String): List<Note> = noteDao.searchTitlesByPrefix(prefix)

    suspend fun insert(note: Note): Long = noteDao.insertNote(note)
    suspend fun update(note: Note) = noteDao.updateNote(note)
    suspend fun delete(note: Note) = noteDao.deleteNote(note)
    suspend fun deleteById(id: Int) = noteDao.deleteNoteById(id)

    suspend fun archive(note: Note) = noteDao.updateNote(note.copy(isArchived = true, isPinned = false))
    suspend fun unarchive(note: Note) = noteDao.updateNote(note.copy(isArchived = false))
    suspend fun toggleArchive(note: Note) {
        if (note.isArchived) unarchive(note) else archive(note)
    }

    // --- Trash ---

    suspend fun trash(note: Note) = noteDao.updateNote(note.copy(isTrashed = true, isPinned = false, trashedAt = System.currentTimeMillis()))
    suspend fun restoreFromTrash(note: Note) = noteDao.updateNote(note.copy(isTrashed = false, trashedAt = null))
    suspend fun toggleTrash(note: Note) {
        if (note.isTrashed) restoreFromTrash(note) else trash(note)
    }
    suspend fun emptyTrash(cutoffTime: Long = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000) {
        noteDao.purgeExpiredTrash(cutoffTime)
    }
    suspend fun getTrashedCount() = noteDao.getTrashedCount()

    // --- Favorites ---

    suspend fun toggleFavorite(note: Note) = noteDao.updateNote(note.copy(isFavorite = !note.isFavorite))

    suspend fun getOrCreateDailyNote(): Note {
        val today = LocalDate.now().toString()
        return database.withTransaction {
            noteDao.getDailyNote(today) ?: run {
                val newNote = Note(
                    title = "📅 ${LocalDate.now()}",
                    content = "## Daily Note — $today\n\n",
                    isDaily = true,
                    dailyDate = today,
                    color = NoteColor.DEFAULT
                )
                val id = noteDao.insertNote(newNote)
                // Re-query to handle race condition
                noteDao.getDailyNote(today) ?: newNote.copy(id = id.toInt())
            }
        }
    }

    // --- Tags ---

    suspend fun setTagsForNote(noteId: Int, tagIds: List<Int>) {
        database.withTransaction {
            noteDao.clearNoteTags(noteId)
            tagIds.forEach { noteDao.insertNoteTag(NoteTag(noteId, it)) }
        }
    }

    suspend fun getOrCreateTag(name: String): Tag {
        val normalizedName = name.lowercase().trim()
        return database.withTransaction {
            tagDao.findTagByName(normalizedName) ?: run {
                val tag = Tag(name = normalizedName)
                val id = tagDao.insertTag(tag)
                if (id == -1L) {
                    // Insert was ignored (race condition) — re-fetch the existing tag
                    tagDao.findTagByName(normalizedName)!!
                } else {
                    tag.copy(id = id.toInt())
                }
            }
        }
    }

    suspend fun renameTag(tag: Tag, newName: String) {
        val updated = tag.copy(name = newName.lowercase().trim())
        tagDao.updateTag(updated)
    }

    // --- Version History ---

    fun getVersionsForNote(noteId: Int) = versionDao.getVersionsForNote(noteId)

    suspend fun saveVersion(note: Note) {
        if (note.id == 0) return
        versionDao.insertVersion(
            NoteVersion(noteId = note.id, title = note.title, content = note.content)
        )
        versionDao.pruneVersions(note.id, keep = 30)
    }

    suspend fun restoreVersion(version: NoteVersion): Note? {
        val note = noteDao.getNoteById(version.noteId) ?: return null
        val restored = note.copy(
            title = version.title,
            content = version.content,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.updateNote(restored)
        return restored
    }

    // --- FTS Query Sanitization ---

    private fun sanitizeFtsQuery(query: String): String {
        // FTS4 treats special chars as operators; wrap terms in double quotes for safety
        // Escape any double-quote characters by doubling them
        return query.trim().split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .joinToString(" ") { """"${it.replace("\"", "\"\"")}"""" }
    }
}
