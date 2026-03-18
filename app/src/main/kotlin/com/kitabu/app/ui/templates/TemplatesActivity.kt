package com.kitabu.app.ui.templates

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.*
import com.kitabu.app.data.BuiltInTemplates
import com.kitabu.app.data.Template
import com.kitabu.app.databinding.ActivityTemplatesBinding
import com.kitabu.app.databinding.ItemTemplateBinding
import com.kitabu.app.ui.editor.EditorActivity
import com.kitabu.app.ui.notes.NoteViewModel
import com.kitabu.app.util.ThemeManager

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

        val adapter = TemplateAdapter { template ->
            startActivity(Intent(this, EditorActivity::class.java)
                .putExtra(EditorActivity.EXTRA_TEMPLATE_ID, template.id))
            if (pickMode) finish()
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        vm.templateRepo.allTemplates.asLiveData().observe(this) { userTemplates ->
            adapter.submitList(BuiltInTemplates.all + userTemplates)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == android.R.id.home) { finish(); true } else super.onOptionsItemSelected(item)
}

class TemplateAdapter(private val onClick: (Template) -> Unit) :
    ListAdapter<Template, TemplateAdapter.VH>(object : DiffUtil.ItemCallback<Template>() {
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
        h.b.tvTemplatePreview.text = t.content.take(80).trim()
        h.b.root.setOnClickListener { onClick(t) }
    }
}
