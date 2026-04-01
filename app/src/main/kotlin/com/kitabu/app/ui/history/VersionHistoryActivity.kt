package com.kitabu.app.ui.history

import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.kitabu.app.R
import com.kitabu.app.data.NoteVersion
import com.kitabu.app.databinding.ActivityVersionHistoryBinding
import com.kitabu.app.databinding.ItemVersionBinding
import com.kitabu.app.ui.notes.NoteViewModel
import com.kitabu.app.util.toNoteDate
import kotlinx.coroutines.launch
import com.kitabu.app.util.ThemeManager

class VersionHistoryActivity : AppCompatActivity() {
    companion object { const val EXTRA_NOTE_ID = "extra_note_id" }
    private lateinit var binding: ActivityVersionHistoryBinding
    private val vm: NoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        binding = ActivityVersionHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Version History"
        val noteId = intent.getIntExtra(EXTRA_NOTE_ID, -1)
        if (noteId == -1) { finish(); return }
        val adapter = VersionAdapter(
            onRestore = { version ->
                MaterialAlertDialogBuilder(this, R.style.KitabuDialog)
                    .setTitle("Restore this version?")
                    .setMessage("Saved ${version.savedAt.toNoteDate()}")
                    .setPositiveButton("Restore") { _, _ ->
                        lifecycleScope.launch {
                            vm.restoreVersion(version)
                            Snackbar.make(binding.root, "Restored", Snackbar.LENGTH_SHORT).show()
                            finish()
                        }
                    }.setNegativeButton("Cancel", null).show()
            },
            onPreview = { version ->
                MaterialAlertDialogBuilder(this, R.style.KitabuDialog)
                    .setTitle(version.title.ifBlank { "Untitled" })
                    .setMessage(version.content.take(2000))
                    .setPositiveButton("Close", null)
                    .show()
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        vm.getVersions(noteId).observe(this) {
            adapter.submitList(it)
            supportActionBar?.subtitle = "${it.size} version${if (it.size != 1) "s" else ""}"
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == android.R.id.home) { finish(); true } else super.onOptionsItemSelected(item)
}

class VersionAdapter(
    private val onRestore: (NoteVersion) -> Unit,
    private val onPreview: (NoteVersion) -> Unit
) : ListAdapter<NoteVersion, VersionAdapter.VH>(object : DiffUtil.ItemCallback<NoteVersion>() {
    override fun areItemsTheSame(a: NoteVersion, b: NoteVersion) = a.id == b.id
    override fun areContentsTheSame(a: NoteVersion, b: NoteVersion) = a == b
}) {
    inner class VH(val b: ItemVersionBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemVersionBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, pos: Int) {
        val v = getItem(pos)
        h.b.tvVersionTitle.text   = v.title.ifBlank { "Untitled" }
        h.b.tvVersionDate.text    = v.savedAt.toNoteDate()
        h.b.tvVersionPreview.text = v.content.take(150)
        h.b.root.setOnClickListener { onPreview(v) }
        h.b.btnRestore.setOnClickListener { onRestore(v) }
    }
}
