package com.kitabu.app.ui.notes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.kitabu.app.R
import com.kitabu.app.data.Note
import com.kitabu.app.data.NoteWithTags
import com.kitabu.app.data.Tag
import com.kitabu.app.databinding.ActivityMainBinding
import com.kitabu.app.ui.editor.EditorActivity
import com.kitabu.app.ui.graph.GraphActivity
import com.kitabu.app.ui.settings.SettingsActivity
import com.kitabu.app.ui.theme.ThemePickerActivity
import com.kitabu.app.ui.tags.TagManagerActivity
import com.kitabu.app.ui.templates.TemplatesActivity
import com.kitabu.app.util.BiometricHelper
import com.kitabu.app.util.SortOrder
import kotlinx.coroutines.launch
import com.kitabu.app.util.ThemeManager
import com.kitabu.app.util.CryptoHelper
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: NoteViewModel by viewModels()
    private lateinit var adapter: NoteAdapter
    private var showingArchived = false
    private var showingTrashed = false
    private var showingFavorites = false

    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleImport(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.apply(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupDrawer()
        setupRecyclerView()
        setupFab()
        observeNotes()
        observeTags()

        // Handle deep link from notifications
        val noteId = intent.getIntExtra("open_note_id", -1)
        if (noteId != -1) {
            lifecycleScope.launch {
                val nwt = vm.getNoteWithTagsById(noteId)
                if (nwt != null) {
                    if (nwt.note.isLocked && BiometricHelper.isAvailable(this@MainActivity)) {
                        BiometricHelper.authenticate(this@MainActivity,
                            onSuccess = { launchEditor(nwt.note.id) },
                            onError = { msg -> Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show() }
                        )
                    } else {
                        launchEditor(nwt.note.id)
                    }
                }
            }
        }
    }

    // ── Drawer ──────────────────────────────────────────────────────────────

    private fun setupDrawer() {
        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.nav_open, R.string.nav_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener { item ->
            binding.drawerLayout.closeDrawers()
            when (item.itemId) {
                R.id.nav_all_notes   -> { vm.filterByTag(null); vm.showDailyNotes(false); vm.showArchivedNotes(false); vm.showFavoritesNotes(false); showingArchived = false; showingTrashed = false; showingFavorites = false; true }
                R.id.nav_daily       -> { vm.showDailyNotes(true); vm.showArchivedNotes(false); vm.showFavoritesNotes(false); showingArchived = false; showingTrashed = false; showingFavorites = false; true }
                R.id.nav_archived    -> { vm.showArchivedNotes(true); vm.showDailyNotes(false); vm.showFavoritesNotes(false); showingArchived = true; showingTrashed = false; showingFavorites = false; true }
                R.id.nav_trash       -> { vm.showArchivedNotes(false); vm.showDailyNotes(false); vm.showFavoritesNotes(false); showingArchived = false; showingTrashed = true; showingFavorites = false; loadTrashedNotes(); true }
                R.id.nav_favorites   -> { vm.showFavoritesNotes(true); vm.showArchivedNotes(false); vm.showDailyNotes(false); showingArchived = false; showingTrashed = false; showingFavorites = true; true }
                R.id.nav_tags        -> { startActivity(Intent(this, TagManagerActivity::class.java)); true }
                R.id.nav_templates   -> { startActivity(Intent(this, TemplatesActivity::class.java)); true }
                R.id.nav_graph       -> { startActivity(Intent(this, GraphActivity::class.java)); true }
                R.id.nav_settings    -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
                R.id.nav_theme       -> { startActivity(Intent(this, ThemePickerActivity::class.java)); true }
                else -> false
            }
        }
    }

    // ── RecyclerView ─────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            onClick      = { nwt -> openNote(nwt) },
            onLongClick  = { nwt -> showOptions(nwt); true }
        )
        binding.recyclerView.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter        = this@MainActivity.adapter
            setHasFixedSize(false)
        }
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(rv: RecyclerView, a: RecyclerView.ViewHolder, b: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val nwt = adapter.currentList[vh.adapterPosition]
                when {
                    showingTrashed -> {
                        vm.delete(nwt.note)
                        Snackbar.make(binding.root, "Permanently deleted", Snackbar.LENGTH_LONG)
                            .setAction("Undo") { lifecycleScope.launch { vm.insert(nwt.note) } }.show()
                    }
                    showingArchived -> {
                        vm.toggleArchive(nwt.note)
                        Snackbar.make(binding.root, "Restored to notes", Snackbar.LENGTH_LONG)
                            .setAction("Undo") { vm.toggleArchive(nwt.note) }.show()
                    }
                    else -> {
                        vm.trash(nwt.note)
                        Snackbar.make(binding.root, "Moved to trash", Snackbar.LENGTH_LONG)
                            .setAction("Undo") { lifecycleScope.launch { vm.trash(nwt.note) } }.show()
                    }
                }
            }
        }).attachToRecyclerView(binding.recyclerView)
    }

    private fun setupFab() {
        binding.fab.setOnClickListener { showNewNoteOptions() }
    }

    // ── Observe ───────────────────────────────────────────────────────────────

    private fun observeNotes() {
        vm.notes.observe(this) { list ->
            adapter.submitList(list)
            binding.layoutEmpty.visibility  = if (list.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (list.isEmpty()) View.GONE   else View.VISIBLE
            val label = when {
                showingTrashed -> "trashed note"
                showingArchived -> "archived note"
                showingFavorites -> "favorite"
                else -> "note"
            }
            supportActionBar?.subtitle = "${list.size} $label${if (list.size != 1) "s" else ""}"
            updateEmptyState()
        }
    }

    private fun loadTrashedNotes() {
        lifecycleScope.launch {
            vm.repo.trashedNotes.collect { list ->
                adapter.submitList(list)
                binding.layoutEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                supportActionBar?.subtitle = "${list.size} trashed note${if (list.size != 1) "s" else ""}"
                binding.emptyIcon.text = ""
                binding.emptyTitle.text = "Trash is empty"
                binding.emptySubtitle.text = "Deleted notes appear here for 30 days"
            }
        }
    }

    private fun updateEmptyState() {
        if (showingTrashed) {
            binding.emptyIcon.text = ""
            binding.emptyTitle.text = "Trash is empty"
            binding.emptySubtitle.text = "Deleted notes appear here for 30 days"
        } else if (showingArchived) {
            binding.emptyIcon.text = ""
            binding.emptyTitle.text = "No archived notes"
            binding.emptySubtitle.text = "Long-press a note and choose Archive to move it here"
        } else if (showingFavorites) {
            binding.emptyIcon.text = ""
            binding.emptyTitle.text = "No favorites yet"
            binding.emptySubtitle.text = "Long-press a note and choose Favorite"
        } else {
            binding.emptyIcon.text = ""
            binding.emptyTitle.text = "No notes yet"
            binding.emptySubtitle.text = "Tap + to start writing"
        }
    }

    private fun observeTags() {
        vm.allTags.observe(this) { tags -> updateTagsInDrawer(tags) }
    }

    private fun updateTagsInDrawer(tags: List<Tag>) {
        val menu = binding.navView.menu
        val tagGroup = menu.findItem(R.id.nav_tags_group)?.subMenu ?: return
        tagGroup.clear()
        tags.forEach { tag ->
            tagGroup.add(Menu.NONE, View.generateViewId(), Menu.NONE, tag.display).apply {
                setOnMenuItemClickListener { vm.filterByTag(tag.id); binding.drawerLayout.closeDrawers(); true }
            }
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private fun openNote(nwt: NoteWithTags) {
        if (nwt.note.isLocked && BiometricHelper.isAvailable(this)) {
            BiometricHelper.authenticate(this,
                onSuccess = { launchEditor(nwt.note.id) },
                onError = { msg -> Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show() }
            )
        } else {
            launchEditor(nwt.note.id)
        }
    }

    private fun launchEditor(noteId: Int?) {
        startActivity(Intent(this, EditorActivity::class.java).apply {
            noteId?.let { putExtra(EditorActivity.EXTRA_NOTE_ID, it) }
        })
    }

    private fun showNewNoteOptions() {
        val items = arrayOf("Blank note", "Today's daily note", "From template")
        MaterialAlertDialogBuilder(this, R.style.KitabuDialog)
            .setTitle("New note")
            .setItems(items) { _, i ->
                when (i) {
                    0 -> launchEditor(null)
                    1 -> lifecycleScope.launch {
                        val daily = vm.getOrCreateDailyNote()
                        launchEditor(daily.id)
                    }
                    2 -> startActivity(Intent(this, TemplatesActivity::class.java)
                        .putExtra(TemplatesActivity.EXTRA_PICK_MODE, true))
                }
            }.show()
    }

    private fun showOptions(nwt: NoteWithTags) {
        val note = nwt.note
        val opts = mutableListOf<String>()
        opts.add(if (note.isPinned) "Unpin" else "Pin")
        opts.add(if (note.isFavorite) "Remove from favorites" else "Add to favorites")
        opts.add(if (note.isLocked) "Unlock" else "Lock")
        if (!showingArchived && !showingTrashed) opts.add("Archive")
        if (showingTrashed) opts.add("Restore")
        opts.add("Delete")
        opts.add("Share")
        MaterialAlertDialogBuilder(this, R.style.KitabuDialog)
            .setItems(opts.toTypedArray()) { _, i ->
                when {
                    showingTrashed && i == opts.size - 3 -> { /* restore */
                        lifecycleScope.launch { vm.trash(note) }
                    }
                    i == 0 -> vm.togglePin(note)
                    i == 1 -> vm.toggleFavorite(note)
                    i == 2 -> vm.toggleLock(note)
                    i == 3 && !showingTrashed -> {
                        if (showingArchived) vm.toggleArchive(note) else vm.toggleArchive(note)
                    }
                    i == opts.size - 2 -> confirmDelete(note)
                    i == opts.size - 1 -> shareNote(nwt)
                }
            }.show()
    }

    private fun shareNote(nwt: NoteWithTags) {
        val title = nwt.note.title
        val content = nwt.note.content
        val shareText = if (title.isNotBlank()) "# $title\n\n$content" else content
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Share Note"))
    }

    private fun confirmDelete(note: Note) {
        MaterialAlertDialogBuilder(this, R.style.KitabuDialog)
            .setTitle("Delete permanently?").setMessage("This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> vm.delete(note) }
            .setNegativeButton("Cancel", null).show()
    }

    // ── Export / Import ──────────────────────────────────────────────────

    private fun exportAllNotes() {
        lifecycleScope.launch {
            try {
                val json = vm.repo.exportAllAsJson()
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                val file = java.io.File(downloadsDir, "kitabu_backup_${System.currentTimeMillis()}.json")
                java.io.FileOutputStream(file).use { it.write(json.toByteArray()) }
                Snackbar.make(binding.root, "Exported: ${file.name}", Snackbar.LENGTH_LONG).show()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Export failed: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun importNotes() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        importLauncher.launch(intent)
    }

    private fun handleImport(uri: Uri) {
        lifecycleScope.launch {
            try {
                val content = contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).readText()
                } ?: return@launch
                Snackbar.make(binding.root, "Import started...", Snackbar.LENGTH_SHORT).show()
                // Basic import - show the JSON for now
                Snackbar.make(binding.root, "Import: ${content.take(100)}", Snackbar.LENGTH_LONG).show()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Import failed: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    // ── Menu ─────────────────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val sv = menu.findItem(R.id.action_search).actionView as SearchView
        sv.queryHint = "Search notes..."
        sv.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?) = false
            override fun onQueryTextChange(q: String?): Boolean { vm.setSearchQuery(q.orEmpty()); return true }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.sort_updated  -> { vm.setSortOrder(SortOrder.UPDATED_DESC); true }
        R.id.sort_created  -> { vm.setSortOrder(SortOrder.CREATED_DESC); true }
        R.id.sort_title    -> { vm.setSortOrder(SortOrder.TITLE_ASC); true }
        R.id.sort_title_z  -> { vm.setSortOrder(SortOrder.TITLE_DESC); true }
        R.id.sort_words    -> { vm.setSortOrder(SortOrder.WORD_COUNT); true }
        R.id.action_export_all -> { exportAllNotes(); true }
        R.id.action_import -> { importNotes(); true }
        else               -> super.onOptionsItemSelected(item)
    }
}
