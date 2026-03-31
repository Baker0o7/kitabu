package com.kitabu.app.ui.templates

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.kitabu.app.R
import com.kitabu.app.data.BuiltInTemplates
import com.kitabu.app.data.Template
import com.kitabu.app.databinding.ActivityTemplatesBinding
import com.kitabu.app.databinding.ItemTemplateBinding
import com.kitabu.app.ui.editor.EditorActivity
import com.kitabu.app.ui.notes.NoteViewModel
import com.kitabu.app.util.ThemeManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TemplatesActivity : AppCompatActivity() {
    companion object { const val EXTRA_PICK_MODE = "extra_pick_mode" }
    private lateinit var binding: ActivityTemplatesBinding
    private val vm: NoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.apply(this)
        binding = ActivityTemplatesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Templates"
        val pickMode = intent.getBooleanExtra(EXTRA_PICK_MODE, false)

        val adapter = TemplateAdapter(
            onClick = { template ->
                startActivity(Intent(this, EditorActivity::class.java)
                    .putExtra(EditorActivity.EXTRA_TEMPLATE_ID, template.id))
                if (pickMode) finish()
            },
            onDelete = { template ->
                if (!template.isBuiltIn) {
                    MaterialAlertDialogBuilder(this, R.style.KitabuDialog)
                        .setTitle("Delete template \"${template.name}\"?")
                        .setPositiveButton("Delete") { _, _ ->
                            GlobalScope.launch { vm.templateRepo.delete(template) }
                            Snackbar.make(binding.root, "Template deleted", Snackbar.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancel", null).show()
                }
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        vm.templateRepo.allTemplates.asLiveData().observe(this) { userTemplates ->
            adapter.submitList(BuiltInTemplates.all + userTemplates)
        }

        binding.fabAddTemplate.setOnClickListener {
            showCreateTemplateDialog()
        }
    }

    private fun showCreateTemplateDialog() {
        val nameEt = EditText(this).apply {
            hint = "Template name"
            setPadding(48, 24, 48, 24)
        }
        val contentEt = EditText(this).apply {
            hint = "Template content (Markdown)\nUse {{date}} for today's date"
            setPadding(48, 24, 48, 24)
            minLines = 5
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 16, 48, 16)
            addView(nameEt)
            addView(contentEt)
        }
        MaterialAlertDialogBuilder(this, R.style.KitabuDialog)
            .setTitle("Create Template")
            .setView(container)
            .setPositiveButton("Create") { _, _ ->
                val name = nameEt.text.toString().trim()
                val content = contentEt.text.toString()
                if (name.isNotBlank()) {
                    GlobalScope.launch {
                        vm.templateRepo.insert(Template(
                            name = name,
                            content = content,
                            icon = "📝",
                            isBuiltIn = false
                        ))
                    }
                    Snackbar.make(binding.root, "Template created", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == android.R.id.home) { finish(); true } else super.onOptionsItemSelected(item)
}

class TemplateAdapter(
    private val onClick: (Template) -> Unit,
    private val onDelete: (Template) -> Unit
) : ListAdapter<Template, TemplateAdapter.VH>(object : DiffUtil.ItemCallback<Template>() {
    override fun areItemsTheSame(a: Template, b: Template) = a.id == b.id
    override fun areContentsTheSame(a: Template, b: Template) = a == b
}) {
    inner class VH(val b: ItemTemplateBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemTemplateBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, pos: Int) {
        val t = getItem(pos)
        h.b.tvTemplateIcon.text    = t.icon
        h.b.tvTemplateName.text    = t.name
        h.b.tvTemplatePreview.text = t.content.take(100).trim()
        if (!t.isBuiltIn) {
            h.b.btnDeleteTemplate.visibility = View.VISIBLE
            h.b.btnDeleteTemplate.setOnClickListener { onDelete(t) }
        } else {
            h.b.btnDeleteTemplate.visibility = View.GONE
        }
        h.b.root.setOnClickListener { onClick(t) }
        h.b.root.setOnLongClickListener {
            if (!t.isBuiltIn) { onDelete(t); true } else false
        }
    }
}
