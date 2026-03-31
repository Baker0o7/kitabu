package com.kitabu.app.ui.editor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.*
import android.view.*
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.GridLayout
import android.widget.TextView as AndroidTextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.kitabu.app.R
import com.kitabu.app.data.*
import com.kitabu.app.databinding.ActivityEditorBinding
import com.kitabu.app.ui.ai.AiAssistantActivity
import com.kitabu.app.ui.history.VersionHistoryActivity
import com.kitabu.app.ui.notes.NoteViewModel
import com.kitabu.app.util.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class EditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTE_ID     = "extra_note_id"
        const val EXTRA_TEMPLATE_ID = "extra_template_id"
    }

    private lateinit var binding: ActivityEditorBinding
    private val vm: NoteViewModel by viewModels()

    private var existingNote: Note? = null
    private var selectedColor = NoteColor.DEFAULT
    private var isPinned = false
    private var isLocked = false
    private var isPreviewMode = false
    private var currentNoteId = -1
    private var selectedTagIds = mutableListOf<Int>()
    private var lastSavedHash = 0

    private val markwon by lazy { MarkdownHelper.buildMarkwon(this) }
    private var autoSaveJob: Job? = null

    // Voice recognition
    private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            text?.let { insertAtCursor(" $it") }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.apply(this)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        setupMarkdownToolbar()
        setupColorPicker()
        setupWordCount()
        setupAutoSave()

        val noteId     = intent.getIntExtra(EXTRA_NOTE_ID, -1)
        val templateId = intent.getIntExtra(EXTRA_TEMPLATE_ID, -1)

        when {
            noteId != -1     -> loadNote(noteId)
            templateId != -1 -> applyTemplate(templateId)
            else             -> applyColor(NoteColor.DEFAULT)
        }
    }

    // ── Load / Template ────────────────────────────────────────────────────

    private fun loadNote(id: Int) {
        currentNoteId = id
        lifecycleScope.launch {
            val nwt = vm.getNoteWithTagsById(id) ?: return@launch
            existingNote   = nwt.note
            isPinned       = nwt.note.isPinned
            isLocked       = nwt.note.isLocked
            selectedColor  = nwt.note.color
            selectedTagIds = nwt.tags.map { it.id }.toMutableList()
            lastSavedHash = computeHash(nwt.note.title, nwt.note.content)

            binding.etTitle.setText(nwt.note.title)
            binding.etContent.setText(nwt.note.content)
            applyColor(nwt.note.color)
            updateTagChips(nwt.tags)
            invalidateOptionsMenu()

            // Observe backlinks
            vm.getBacklinks(nwt.note.title, id).observe(this@EditorActivity) { links ->
                binding.tvBacklinks.text = if (links.isEmpty()) ""
                    else "← ${links.size} backlink${if (links.size > 1) "s" else ""}"
                binding.tvBacklinks.visibility = if (links.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun applyTemplate(templateId: Int) {
        lifecycleScope.launch {
            val tmpl = vm.templateRepo.getById(templateId)
                ?: BuiltInTemplates.all.firstOrNull { it.id == templateId }
                ?: return@launch
            val resolved = MarkdownHelper.resolveTemplateVars(tmpl.content)
            binding.etContent.setText(resolved)
            applyColor(NoteColor.DEFAULT)
        }
    }

    // ── Markdown toolbar ───────────────────────────────────────────────────

    private fun setupMarkdownToolbar() {
        val actions = listOf(
            "B"  to { wrap("**", "**") },
            "I"  to { wrap("_", "_") },
            "~~" to { wrap("~~", "~~") },
            "`"  to { wrap("`", "`") },
            "```" to { insertCodeBlock() },
            "H1" to { insertLine("# ") },
            "H2" to { insertLine("## ") },
            "H3" to { insertLine("### ") },
            "• " to { insertLine("- ") },
            "1." to { insertLine("1. ") },
            "☑"  to { insertLine("- [ ] ") },
            "—"  to { insertAtCursor("\n---\n") },
            "🔗" to { insertWikiLink() },
            "⊞"  to { showTableDialog() },
            "🎙" to { startVoiceInput() },
            "👁" to { togglePreview() }
        )

        val toolbar = binding.markdownToolbar
        actions.forEach { (label, action) ->
            val tv = android.widget.TextView(this).apply {
                text = label
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).also { lp -> lp.marginEnd = 2.dp.toInt() }
                setPadding(14.dp.toInt(), 0, 14.dp.toInt(), 0)
                gravity = android.view.Gravity.CENTER
                background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_btn)
                setOnClickListener { action() }
            }
            toolbar.addView(tv)
        }
    }

    private val Float.dp get() = this * resources.displayMetrics.density
    private val Int.dp    get() = (this * resources.displayMetrics.density).toInt()

    private fun wrap(open: String, close: String) {
        val et  = binding.etContent
        val s   = et.selectionStart.coerceAtLeast(0)
        val e   = et.selectionEnd.coerceAtLeast(s)
        val sel = et.text?.substring(s, e) ?: ""
        et.text?.replace(s, e, "$open$sel$close")
        et.setSelection(s + open.length, s + open.length + sel.length)
    }

    private fun insertCodeBlock() {
        val et = binding.etContent
        val pos = et.selectionStart.coerceAtLeast(0)
        val text = "\n```\n\n```\n"
        et.text?.insert(pos, text)
        et.setSelection(pos + 4) // cursor inside code block
    }

    private fun insertLine(prefix: String) {
        val et  = binding.etContent
        val pos = et.selectionStart.coerceAtLeast(0)
        val txt = et.text ?: return
        val lineStart = txt.lastIndexOf('\n', pos - 1) + 1
        txt.insert(lineStart, prefix)
        et.setSelection(lineStart + prefix.length)
    }

    private fun insertAtCursor(text: String) {
        val et  = binding.etContent
        val pos = et.selectionStart.coerceAtLeast(0)
        et.text?.insert(pos, text)
        et.setSelection(pos + text.length)
    }

    // ── Table insertion ────────────────────────────────────────────────

    private fun showTableDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_table_input, null)
        val etRows    = dialogView.findViewById<EditText>(R.id.etTableRows)
        val etCols    = dialogView.findViewById<EditText>(R.id.etTableCols)
        val gridContainer = dialogView.findViewById<GridLayout>(R.id.gridTableInput)
        val btnBuild  = dialogView.findViewById<android.widget.Button>(R.id.btnBuildTable)

        etRows.setText("3")
        etCols.setText("3")

        fun rebuildGrid() {
            val rows = etRows.text.toString().toIntOrNull()?.coerceIn(1, 20) ?: 0
            val cols = etCols.text.toString().toIntOrNull()?.coerceIn(1, 8)  ?: 0
            if (rows == 0 || cols == 0) return

            gridContainer.removeAllViews()
            gridContainer.columnCount = cols + 1 // +1 for row label column

            // Top-left empty cell
            gridContainer.addView(AndroidTextView(this).apply {
                text = ""
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.text_hint))
            })

            // Column headers (editable)
            val headerLabels = mutableListOf<EditText>()
            for (c in 0 until cols) {
                val headerEt = EditText(this).apply {
                    hint = "Col ${c + 1}"
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(context, R.color.accent))
                    setHintTextColor(ContextCompat.getColor(context, R.color.text_hint))
                    setBackgroundColor(0x00000000)
                    setPadding(6, 8, 6, 8)
                    setTextColor(android.graphics.Color.WHITE)
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        columnSpec = GridLayout.spec(c + 1, 1f)
                    }
                }
                headerLabels.add(headerEt)
                gridContainer.addView(headerEt)
            }

            // Data rows
            val dataEdits = mutableListOf<MutableList<EditText>>()
            for (r in 0 until rows) {
                // Row number label
                gridContainer.addView(AndroidTextView(this).apply {
                    text = "R${r + 1}"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.text_hint))
                    gravity = android.view.Gravity.CENTER
                    setPadding(4, 8, 8, 8)
                })

                val rowEdits = mutableListOf<EditText>()
                for (c in 0 until cols) {
                    val cellEt = EditText(this).apply {
                        hint = "..."
                        textSize = 13f
                        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                        setHintTextColor(ContextCompat.getColor(context, R.color.text_hint))
                        setBackgroundColor(0x00000000)
                        setPadding(6, 8, 6, 8)
                        inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = 0
                            columnSpec = GridLayout.spec(c + 1, 1f)
                        }
                    }
                    rowEdits.add(cellEt)
                    gridContainer.addView(cellEt)
                }
                dataEdits.add(rowEdits)
            }

            // Store references for the build button
            gridContainer.setTag(0, headerLabels)
            gridContainer.setTag(1, dataEdits)
        }

        // Initial build
        rebuildGrid()

        // Rebuild when rows/cols change
        etRows.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = rebuildGrid()
            override fun afterTextChanged(s: Editable?) = Unit
        })
        etCols.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = rebuildGrid()
            override fun afterTextChanged(s: Editable?) = Unit
        })

        val dialog = MaterialAlertDialogBuilder(this, R.style.KitabuDialog)
            .setTitle("Insert Table")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        btnBuild.setOnClickListener {
            @Suppress("UNCHECKED_CAST")
            val headers = (gridContainer.getTag(0) as? List<EditText>)?.map { it.text.toString().trim() } ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val allRows = (gridContainer.getTag(1) as? List<List<EditText>>)?.map { row ->
                row.map { it.text.toString().trim() }
            } ?: emptyList()

            val tableMd = buildMarkdownTable(headers, allRows)
            insertAtCursor(tableMd)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun buildMarkdownTable(headers: List<String>, rows: List<List<String>>): String {
        val cols = headers.size.coerceAtLeast(if (rows.isNotEmpty()) rows[0].size else 0)
        if (cols == 0) return ""

        val sb = StringBuilder()
        sb.append("\n")

        // Header row
        sb.append("| ${headers.map { it.ifBlank { "Col" } }.joinToString(" | ")} |\n")

        // Separator
        sb.append("| ${headers.indices.joinToString(" | ") { "---" } |\n")

        // Data rows
        rows.forEach { row ->
            val cells = row + List(cols - row.size) { "" }
            sb.append("| ${cells.joinToString(" | ")} |\n")
        }

        sb.append("\n")
        return sb.toString()
    }

    private fun insertWikiLink() {
        lifecycleScope.launch {
            val titlePrefix = binding.etContent.text.toString()
                .substring(0, binding.etContent.selectionStart.coerceAtLeast(0))
                .substringAfterLast("[[")
            val notes = vm.searchTitlesByPrefix(titlePrefix)
            if (notes.isEmpty()) {
                insertAtCursor("[[]]")
                Snackbar.make(binding.root, "No notes found to link", Snackbar.LENGTH_SHORT).show()
                return@launch
            }
            val arr = notes.map { it.title }.toTypedArray()
            MaterialAlertDialogBuilder(this@EditorActivity, R.style.KitabuDialog)
                .setTitle("Link to note")
                .setItems(arr) { _, i -> insertAtCursor("[[${arr[i]}]]") }
                .show()
        }
    }

    private fun togglePreview() {
        isPreviewMode = !isPreviewMode
        if (isPreviewMode) {
            val content = binding.etContent.text.toString()
            val rendered = MarkdownHelper.renderWikiLinks(content)
            markwon.setMarkdown(binding.tvPreview, rendered)
            binding.etContent.visibility = View.GONE
            binding.tvPreview.visibility = View.VISIBLE
        } else {
            binding.etContent.visibility = View.VISIBLE
            binding.tvPreview.visibility = View.GONE
        }
    }

    // ── Voice ─────────────────────────────────────────────────────────────

    private fun startVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now…")
        }
        voiceLauncher.launch(intent)
    }

    // ── Color picker ──────────────────────────────────────────────────────

    private fun setupColorPicker() {
        binding.colorPickerToggle.setOnClickListener {
            binding.colorPickerPanel.visibility =
                if (binding.colorPickerPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        NoteColor.all.forEachIndexed { i, color ->
            val viewId = resources.getIdentifier("color$i", "id", packageName)
            if (viewId != 0) {
                val v = findViewById<View>(viewId)
                if (v != null) {
                    v.setBackgroundColor(color)
                    v.setOnClickListener { _ ->
                        selectedColor = color
                        applyColor(selectedColor)
                        NoteColor.all.forEachIndexed { j, _ ->
                            val otherId = resources.getIdentifier("color$j", "id", packageName)
                            if (otherId != 0) findViewById<View>(otherId)?.let { w ->
                                w.scaleX = if (i == j) 1.3f else 1f
                                w.scaleY = if (i == j) 1.3f else 1f
                            }
                        }
                    }
                }
            }
        }
    }

    private fun applyColor(color: Int) {
        binding.editorRoot.setBackgroundColor(color); selectedColor = color
    }

    // ── Word count / autosave ─────────────────────────────────────────────

    private fun setupWordCount() {
        binding.etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val stats = s.toString().textStats()
                supportActionBar?.subtitle = stats.label
            }
        })
    }

    private fun setupAutoSave() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                autoSaveJob?.cancel()
                autoSaveJob = lifecycleScope.launch {
                    delay(1500)
                    performSave(silent = true)
                }
            }
        }
        binding.etTitle.addTextChangedListener(watcher)
        binding.etContent.addTextChangedListener(watcher)
    }

    private fun computeHash(title: String, content: String): Int {
        return (title + content).hashCode()
    }

    // ── Tags ────────────────────────────────────────────────────────────────

    private fun updateTagChips(tags: List<Tag>) {
        binding.chipGroupTags.removeAllViews()
        tags.forEach { tag ->
            val chip = Chip(this).apply {
                text = tag.display
                isCloseIconVisible = true
                setOnCloseIconClickListener { selectedTagIds.remove(tag.id); binding.chipGroupTags.removeView(this) }
            }
            binding.chipGroupTags.addView(chip)
        }
        val add = Chip(this).apply {
            text = "+ tag"
            chipBackgroundColor = android.content.res.ColorStateList.valueOf(0xFF2A2A3A.toInt())
            setTextColor(ContextCompat.getColor(this@EditorActivity, R.color.text_secondary))
            setOnClickListener { showTagPicker() }
        }
        binding.chipGroupTags.addView(add)
    }

    private fun showTagPicker() {
        val sheet = BottomSheetDialog(this, R.style.KitabuBottomSheet)
        val view  = layoutInflater.inflate(R.layout.sheet_tag_picker, null)
        val cg    = view.findViewById<ChipGroup>(R.id.chipGroupAllTags)
        val etNew = view.findViewById<android.widget.EditText>(R.id.etNewTag)
        val btnAdd = view.findViewById<android.widget.Button>(R.id.btnAddTag)

        vm.allTags.value?.forEach { tag ->
            val chip = Chip(this).apply {
                text = tag.display
                isCheckable = true
                isChecked   = tag.id in selectedTagIds
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedTagIds.add(tag.id) else selectedTagIds.remove(tag.id)
                }
            }
            cg.addView(chip)
        }

        btnAdd.setOnClickListener {
            val name = etNew.text.toString().trim().removePrefix("#")
            if (name.isNotBlank()) {
                lifecycleScope.launch {
                    val tag = vm.getOrCreateTag(name)
                    if (tag.id !in selectedTagIds) selectedTagIds.add(tag.id)
                    sheet.dismiss()
                    refreshTagChipsFromIds()
                }
            }
        }
        sheet.setContentView(view)
        sheet.show()
    }

    private fun refreshTagChipsFromIds() {
        lifecycleScope.launch {
            val tags = vm.allTags.value?.filter { it.id in selectedTagIds } ?: emptyList()
            updateTagChips(tags)
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────

    private fun performSave(silent: Boolean = false) {
        val title   = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()
        if (title.isBlank() && content.isBlank()) { if (!silent) finish(); return }

        val currentHash = computeHash(title, content)
        if (silent && currentHash == lastSavedHash) return

        val note = existingNote?.copy(
            title = title, content = content, color = selectedColor,
            isPinned = isPinned, isLocked = isLocked,
            updatedAt = System.currentTimeMillis()
        ) ?: Note(title = title, content = content, color = selectedColor,
            isPinned = isPinned, isLocked = isLocked)

        lifecycleScope.launch {
            if (existingNote != null) {
                vm.saveVersion(existingNote!!)
                vm.update(note)
                vm.setTagsForNote(note.id, selectedTagIds)
                lastSavedHash = currentHash
            } else {
                val id = KitabuDatabase
                    .getDatabase(applicationContext).noteDao().insertNote(note)
                currentNoteId = id.toInt()
                existingNote  = note.copy(id = currentNoteId)
                vm.setTagsForNote(currentNoteId, selectedTagIds)
                lastSavedHash = currentHash
            }
        }
        if (!silent) finish()
    }

    // ── Options menu ──────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        menu.findItem(R.id.action_pin)?.setIcon(if (isPinned) R.drawable.ic_pin_filled else R.drawable.ic_pin_outline)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home  -> { performSave(); true }
        R.id.action_pin    -> { isPinned = !isPinned; invalidateOptionsMenu(); true }
        R.id.action_lock   -> { isLocked = !isLocked
            Snackbar.make(binding.root, if (isLocked) "Note locked 🔒" else "Note unlocked 🔓", Snackbar.LENGTH_SHORT).show()
            true }
        R.id.action_history -> {
            startActivity(Intent(this, VersionHistoryActivity::class.java)
                .putExtra(VersionHistoryActivity.EXTRA_NOTE_ID, currentNoteId))
            true }
        R.id.action_ai     -> {
            startActivity(Intent(this, AiAssistantActivity::class.java)
                .putExtra(AiAssistantActivity.EXTRA_NOTE_CONTENT, binding.etContent.text.toString()))
            true }
        R.id.action_save   -> { performSave(); true }
        else               -> super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() { performSave() }
    override fun onPause()       { super.onPause(); performSave(silent = true) }
}
