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
    val repo = NoteRepository(db.noteDao(), db.tagDao(), db.noteVersionDao())
    val tagRepo      = TagRepository(db.tagDao())
    val templateRepo = TemplateRepository(db.templateDao())

    private val _searchQuery  = MutableStateFlow("")
    private val _sortOrder    = MutableStateFlow(SortOrder.UPDATED_DESC)
    private val _filterTagId  = MutableStateFlow<Int?>(null)
    private val _showDailyOnly = MutableStateFlow(false)
    private val _showArchivedOnly = MutableStateFlow(false)
    private val _showFavoritesOnly = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: LiveData<List<NoteWithTags>> = combine(
        _searchQuery.debounce(300), _sortOrder, _filterTagId, _showDailyOnly, _showArchivedOnly, _showFavoritesOnly
    ) { q, sort, tagId, dailyOnly, archivedOnly, favoritesOnly ->
        FilterState(q, sort, tagId, dailyOnly, archivedOnly, favoritesOnly)
    }
        .flatMapLatest { state ->
            val base: Flow<List<NoteWithTags>> = when {
                state.favoritesOnly  -> repo.favoriteNotes
                state.archivedOnly   -> repo.archivedNotes
                state.dailyOnly      -> repo.dailyNotes
                state.tagId != null  -> repo.getNotesByTag(state.tagId)
                state.query.isNotBlank() -> repo.searchNotes(state.query)
                else                 -> repo.allNotes
            }
            base.map { list -> sorted(list, state.sort) }
        }.asLiveData()

    val allTags: LiveData<List<Tag>> = tagRepo.allTags.asLiveData()

    private data class FilterState(
        val query: String,
        val sort: SortOrder,
        val tagId: Int?,
        val dailyOnly: Boolean,
        val archivedOnly: Boolean,
        val favoritesOnly: Boolean = false
    )

    private fun sorted(list: List<NoteWithTags>, sort: SortOrder): List<NoteWithTags> {
        val pinned   = list.filter { it.note.isPinned }
        val unpinned = list.filter { !it.note.isPinned }
        fun s(l: List<NoteWithTags>) = when (sort) {
            SortOrder.UPDATED_DESC -> l.sortedByDescending { it.note.updatedAt }
            SortOrder.CREATED_DESC -> l.sortedByDescending { it.note.createdAt }
            SortOrder.TITLE_ASC    -> l.sortedBy { it.note.title.lowercase() }
            SortOrder.TITLE_DESC   -> l.sortedByDescending { it.note.title.lowercase() }
            SortOrder.WORD_COUNT   -> l.sortedByDescending {
                com.kitabu.app.util.MarkdownHelper.wordCount(it.note.content)
            }
        }
        return s(pinned) + s(unpinned)
    }

    fun setSearchQuery(q: String)       { _searchQuery.value = q }
    fun setSortOrder(o: SortOrder)      { _sortOrder.value = o }
    fun filterByTag(tagId: Int?)        {
        _filterTagId.value = tagId
        _showDailyOnly.value = false
        _showArchivedOnly.value = false
        _showFavoritesOnly.value = false
    }
    fun showDailyNotes(v: Boolean)      {
        _showDailyOnly.value = v
        _filterTagId.value = null
        _showArchivedOnly.value = false
        _showFavoritesOnly.value = false
    }
    fun showArchivedNotes(v: Boolean)   {
        _showArchivedOnly.value = v
        _filterTagId.value = null
        _showDailyOnly.value = false
        _showFavoritesOnly.value = false
    }
    fun showFavoritesNotes(v: Boolean) {
        _showFavoritesOnly.value = v
        _filterTagId.value = null
        _showDailyOnly.value = false
        _showArchivedOnly.value = false
    }

    fun insert(note: Note)              = viewModelScope.launch { repo.insert(note) }
    fun update(note: Note)              = viewModelScope.launch { repo.update(note) }
    fun delete(note: Note)              = viewModelScope.launch { repo.delete(note) }

    fun togglePin(note: Note) = viewModelScope.launch {
        repo.update(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
    }
    fun toggleLock(note: Note) = viewModelScope.launch {
        repo.update(note.copy(isLocked = !note.isLocked))
    }
    fun toggleArchive(note: Note) = viewModelScope.launch {
        repo.toggleArchive(note)
    }
    fun toggleFavorite(note: Note) = viewModelScope.launch { repo.toggleFavorite(note) }
    fun trash(note: Note) = viewModelScope.launch { repo.toggleTrash(note) }
    fun emptyTrash() = viewModelScope.launch { repo.emptyTrash() }
    suspend fun getTrashedCount() = repo.getTrashedCount()

    suspend fun getNoteById(id: Int) = repo.getNoteById(id)
    suspend fun getNoteWithTagsById(id: Int) = repo.getNoteWithTagsById(id)
    suspend fun findNoteByTitle(title: String, excludeId: Int = -1) = repo.findNoteByTitle(title, excludeId)
    suspend fun searchTitlesByPrefix(prefix: String) = repo.searchTitlesByPrefix(prefix)
    suspend fun getOrCreateDailyNote() = repo.getOrCreateDailyNote()
    suspend fun getOrCreateTag(name: String) = repo.getOrCreateTag(name)
    suspend fun setTagsForNote(noteId: Int, tagIds: List<Int>) = repo.setTagsForNote(noteId, tagIds)
    suspend fun renameTag(tag: Tag, newName: String) = repo.renameTag(tag, newName)

    fun saveVersion(note: Note) = viewModelScope.launch { repo.saveVersion(note) }
    fun getVersions(noteId: Int) = repo.getVersionsForNote(noteId).asLiveData()
    suspend fun restoreVersion(v: NoteVersion) = repo.restoreVersion(v)
    fun getBacklinks(title: String, excludeId: Int = -1) = repo.getBacklinks(title, excludeId).asLiveData()
}
