package com.kitabu.app.ui.notes

import android.app.Application
import androidx.lifecycle.*
import com.kitabu.app.data.*
import com.kitabu.app.util.SortOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val db   = KitabuDatabase.getDatabase(application)
    private val repo = NoteRepository(db.noteDao(), db.tagDao(), db.noteVersionDao())
    val tagRepo      = TagRepository(db.tagDao())
    val templateRepo = TemplateRepository(db.templateDao())

    private val _searchQuery  = MutableStateFlow("")
    private val _sortOrder    = MutableStateFlow(SortOrder.UPDATED_DESC)
    private val _filterTagId  = MutableStateFlow<Int?>(null)
    private val _showDailyOnly = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: LiveData<List<NoteWithTags>> = combine(
        _searchQuery.debounce(300), _sortOrder, _filterTagId, _showDailyOnly
    ) { q, sort, tagId, dailyOnly -> Triple(q, sort, tagId to dailyOnly) }
        .flatMapLatest { (query, sort, filter) ->
            val (tagId, dailyOnly) = filter
            val base: Flow<List<NoteWithTags>> = when {
                dailyOnly        -> repo.dailyNotes
                tagId != null    -> repo.getNotesByTag(tagId)
                query.isNotBlank() -> repo.searchNotes(query)
                else             -> repo.allNotes
            }
            base.map { list -> sorted(list, sort) }
        }.asLiveData()

    val allTags: LiveData<List<Tag>> = tagRepo.allTags.asLiveData()

    private fun sorted(list: List<NoteWithTags>, sort: SortOrder): List<NoteWithTags> {
        val pinned   = list.filter { it.note.isPinned }
        val unpinned = list.filter { !it.note.isPinned }
        fun s(l: List<NoteWithTags>) = when (sort) {
            SortOrder.UPDATED_DESC -> l.sortedByDescending { it.note.updatedAt }
            SortOrder.CREATED_DESC -> l.sortedByDescending { it.note.createdAt }
            SortOrder.TITLE_ASC    -> l.sortedBy { it.note.title.lowercase() }
        }
        return s(pinned) + s(unpinned)
    }

    fun setSearchQuery(q: String)       { _searchQuery.value = q }
    fun setSortOrder(o: SortOrder)      { _sortOrder.value = o }
    fun filterByTag(tagId: Int?)        { _filterTagId.value = tagId; _showDailyOnly.value = false }
    fun showDailyNotes(v: Boolean)      { _showDailyOnly.value = v; _filterTagId.value = null }

    fun insert(note: Note)              = viewModelScope.launch { repo.insert(note) }
    fun update(note: Note)              = viewModelScope.launch { repo.update(note) }
    fun delete(note: Note)              = viewModelScope.launch { repo.delete(note) }

    fun togglePin(note: Note) = viewModelScope.launch {
        repo.update(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
    }
    fun toggleLock(note: Note) = viewModelScope.launch {
        repo.update(note.copy(isLocked = !note.isLocked))
    }

    suspend fun getNoteById(id: Int) = repo.getNoteById(id)
    suspend fun getNoteWithTagsById(id: Int) = repo.getNoteWithTagsById(id)
    suspend fun findNoteByTitle(title: String) = repo.findNoteByTitle(title)
    suspend fun getOrCreateDailyNote() = repo.getOrCreateDailyNote()
    suspend fun getOrCreateTag(name: String) = repo.getOrCreateTag(name)
    suspend fun setTagsForNote(noteId: Int, tagIds: List<Int>) = repo.setTagsForNote(noteId, tagIds)

    fun saveVersion(note: Note) = viewModelScope.launch { repo.saveVersion(note) }
    fun getVersions(noteId: Int) = repo.getVersionsForNote(noteId).asLiveData()
    suspend fun restoreVersion(v: NoteVersion) = repo.restoreVersion(v)
    fun getBacklinks(title: String) = repo.getBacklinks(title).asLiveData()
}
