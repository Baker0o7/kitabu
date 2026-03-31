package com.kitabu.app.ui.graph

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.kitabu.app.databinding.ActivityGraphBinding
import com.kitabu.app.ui.editor.EditorActivity
import com.kitabu.app.ui.notes.NoteViewModel
import com.kitabu.app.util.MarkdownHelper
import com.kitabu.app.util.ThemeManager

class GraphActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGraphBinding
    private val vm: NoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.apply(this)
        binding = ActivityGraphBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Knowledge Graph"

        vm.notes.observe(this) { notes ->
            if (notes.isEmpty()) {
                binding.tvGraphEmpty.visibility = View.VISIBLE
                binding.graphView.visibility = View.GONE
                binding.tvGraphInfo.visibility = View.GONE
                return@observe
            }
            binding.tvGraphEmpty.visibility = View.GONE
            binding.graphView.visibility = View.VISIBLE
            binding.tvGraphInfo.visibility = View.VISIBLE

            val nodeList = notes.map { nwt ->
                GraphView.Node(nwt.note.id, nwt.note.title.ifBlank { "Untitled" }, nwt.note.color)
            }
            val edgeList = mutableListOf<GraphView.Edge>()
            notes.forEach { nwt ->
                MarkdownHelper.extractWikiLinks(nwt.note.content).forEach { linkedTitle ->
                    notes.firstOrNull { it.note.title.equals(linkedTitle, ignoreCase = true) }
                        ?.let { edgeList.add(GraphView.Edge(nwt.note.id, it.note.id)) }
                }
            }
            binding.graphView.setData(nodeList, edgeList)
            binding.tvGraphInfo.text = "${nodeList.size} notes, ${edgeList.size} connections"
            binding.graphView.onNodeClick = { id ->
                startActivity(Intent(this, EditorActivity::class.java)
                    .putExtra(EditorActivity.EXTRA_NOTE_ID, id))
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == android.R.id.home) { finish(); true } else super.onOptionsItemSelected(item)
}
