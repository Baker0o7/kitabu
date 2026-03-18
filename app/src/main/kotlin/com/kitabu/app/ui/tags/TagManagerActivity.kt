package com.kitabu.app.ui.tags

import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kitabu.app.R
import com.kitabu.app.data.Tag
import com.kitabu.app.databinding.ActivityTagManagerBinding
import com.kitabu.app.databinding.ItemTagBinding
import com.kitabu.app.ui.notes.NoteViewModel
import kotlinx.coroutines.launch
import com.kitabu.app.util.ThemeManager

class TagManagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTagManagerBinding
    private val vm: NoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.apply(this)
        binding = ActivityTagManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Tags"

        val adapter = TagAdapter { tag ->
            MaterialAlertDialogBuilder(this, R.style.KitabuDialog)
                .setTitle("Delete #${tag.name}?")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch { vm.tagRepo.delete(tag) }
                }.setNegativeButton("Cancel", null).show()
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        vm.allTags.observe(this) { adapter.submitList(it) }

        binding.fabAddTag.setOnClickListener {
            val et = android.widget.EditText(this).apply {
                hint = "e.g. work/meetings"; setPadding(48, 24, 48, 24)
            }
            MaterialAlertDialogBuilder(this, R.style.KitabuDialog)
                .setTitle("New Tag").setView(et)
                .setPositiveButton("Create") { _, _ ->
                    val name = et.text.toString().trim().removePrefix("#")
                    if (name.isNotBlank()) lifecycleScope.launch { vm.getOrCreateTag(name) }
                }.setNegativeButton("Cancel", null).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == android.R.id.home) { finish(); true } else super.onOptionsItemSelected(item)
}

class TagAdapter(private val onDelete: (Tag) -> Unit) :
    ListAdapter<Tag, TagAdapter.VH>(object : DiffUtil.ItemCallback<Tag>() {
        override fun areItemsTheSame(a: Tag, b: Tag) = a.id == b.id
        override fun areContentsTheSame(a: Tag, b: Tag) = a == b
    }) {
    inner class VH(val b: ItemTagBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemTagBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, pos: Int) {
        h.b.tvTagName.text = getItem(pos).display
        h.b.btnDeleteTag.setOnClickListener { onDelete(getItem(pos)) }
    }
}
