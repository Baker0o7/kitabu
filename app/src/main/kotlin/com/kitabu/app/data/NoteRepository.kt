package com.kitabu.app.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class NoteRepository(
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val versionDao: NoteVersionDao
) {

    // --- Notes ---

    val allNotes: Flow<List<NoteWithTags>> = noteDao.getAllNotesWithTags()
    val dailyNotes: Flow<List<NoteWithTags>> = noteDao.getDailyNotesWithTags()
    val archivedNotes: Flow<List<NoteWithTags>> = noteDao.getArchivedNotesWithTags()
    val notesWithReminders: Flow<List<Note>> = noteDao.getNotesWithReminders()

    fun searchNotes(q: String): Flow<List<NoteWithTags>> = noteDao.searchNotesWithTags(q)
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

    suspend fun getOrCreateDailyNote(): Note {
        val today = LocalDate.now().toString()
        return noteDao.getDailyNote(today) ?: run {
            val newNote = Note(
                title = "📅 ${LocalDate.now()}",
                content = "## Daily Note — $today\n\n",
                isDaily = true,
                dailyDate = today,
                color = NoteColor.DEFAULT
            )
            val id = noteDao.insertNote(newNote)
            newNote.copy(id = id.toInt())
        }
    }

    // --- Tags ---

    suspend fun setTagsForNote(noteId: Int, tagIds: List<Int>) {
        noteDao.clearNoteTags(noteId)
        tagIds.forEach { noteDao.insertNoteTag(NoteTag(noteId, it)) }
    }

    suspend fun getOrCreateTag(name: String): Tag {
        return tagDao.findTagByName(name) ?: run {
            val tag = Tag(name = name.lowercase().trim())
            val id = tagDao.insertTag(tag)
            tag.copy(id = id.toInt())
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

    // --- Export / Import ---

    suspend fun exportAllAsJson(): String {
        val notes = mutableListOf<Note>()
        var offset = 0
        val pageSize = 100
        while (true) {
            // Room Flow collect is not available here, so we use individual queries
            // In practice you'd add a non-flow query to NoteDao for bulk export
            break // simplified
        }
        return "[]"
    }
}
