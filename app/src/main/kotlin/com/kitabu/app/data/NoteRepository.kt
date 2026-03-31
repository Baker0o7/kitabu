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
    val trashedNotes: Flow<List<NoteWithTags>> = noteDao.getTrashedNotesWithTags()
    val favoriteNotes: Flow<List<NoteWithTags>> = noteDao.getFavoriteNotesWithTags()

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
        val notes = noteDao.getAllNotesRaw()
        val tags = noteDao.getAllTagsRaw()
        val noteTags = noteDao.getAllNoteTagsRaw()
        val versions = noteDao.getAllVersionsRaw()
        val templates = noteDao.getAllTemplatesRaw()

        val json = org.json.JSONObject()
        json.put("version", 4)
        json.put("exportedAt", System.currentTimeMillis())
        json.put("notes", org.json.JSONArray().apply {
            notes.forEach { n ->
                put(org.json.JSONObject().apply {
                    put("id", n.id); put("title", n.title); put("content", n.content)
                    put("color", n.color); put("isPinned", n.isPinned); put("isLocked", n.isLocked)
                    put("isArchived", n.isArchived); put("isDaily", n.isDaily); put("dailyDate", n.dailyDate)
                    put("templateId", n.templateId); put("reminderTime", n.reminderTime)
                    put("isTrashed", n.isTrashed); put("trashedAt", n.trashedAt)
                    put("isFavorite", n.isFavorite)
                    put("createdAt", n.createdAt); put("updatedAt", n.updatedAt)
                })
            }
        })
        json.put("tags", org.json.JSONArray().apply {
            tags.forEach { t ->
                put(org.json.JSONObject().apply {
                    put("id", t.id); put("name", t.name); put("color", t.color)
                    put("createdAt", t.createdAt)
                })
            }
        })
        json.put("noteTags", org.json.JSONArray().apply {
            noteTags.forEach { nt ->
                put(org.json.JSONObject().apply {
                    put("noteId", nt.noteId); put("tagId", nt.tagId)
                })
            }
        })
        json.put("versions", org.json.JSONArray().apply {
            versions.forEach { v ->
                put(org.json.JSONObject().apply {
                    put("id", v.id); put("noteId", v.noteId); put("title", v.title)
                    put("content", v.content); put("savedAt", v.savedAt)
                })
            }
        })
        json.put("templates", org.json.JSONArray().apply {
            templates.forEach { t ->
                put(org.json.JSONObject().apply {
                    put("id", t.id); put("name", t.name); put("content", t.content)
                    put("icon", t.icon); put("isBuiltIn", t.isBuiltIn); put("createdAt", t.createdAt)
                })
            }
        })
        return json.toString(2)
    }
}
