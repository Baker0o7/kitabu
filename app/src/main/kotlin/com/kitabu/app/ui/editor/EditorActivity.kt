package com.kitabu.app.ui.editor

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.*
import android.view.*
import android.widget.ArrayAdapter
import android.widget.DatePicker
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
import dagger.hilt.android.AndroidEntryPoint
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
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
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
    private var isFavorite = false
    private var isPreviewMode = false
    private var isLivePreview = false
    private var currentNoteId = -1
    private var livePreviewJob: Job? = null
    private var selectedTagIds = mutableListOf<Int>()
    private var lastSavedHash = 0
    private var reminderTime: Long? = null

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
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
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

    // ── Keyboard Shortcuts ───────────────────────────────────────────
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val ctrl = event.isCtrlPressed || event.isMetaPressed
            when {
                ctrl && event.keyCode == KeyEvent.KEYCODE_B -> { wrap("**", "**"); return true }
                ctrl && event.keyCode == KeyEvent.KEYCODE_I -> { wrap("_", "_"); return true }
                ctrl && event.keyCode == KeyEvent.KEYCODE_S -> { performSave(); return true }
                ctrl && event.keyCode == KeyEvent.KEYCODE_K -> { insertWikiLink(); return true }
                ctrl && event.keyCode == KeyEvent.KEYCODE_P -> { togglePreview(); return true }
                ctrl && event.keyCode == KeyEvent.KEYCODE_N -> { showTableDialog(); return true }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // ── Load / Template ────────────────────────────────────────────────────

    private fun loadNote(id: Int) {
        currentNoteId = id
        lifecycleScope.launch {
            val nwt = vm.getNoteWithTagsById(id) ?: return@launch
            existingNote   = nwt.note
            isPinned       = nwt.note.isPinned
            isLocked       = nwt.note.isLocked
            isFavorite     = nwt.note.isFavorite
            selectedColor  = nwt.note.color
            reminderTime   = nwt.note.reminderTime
            selectedTagIds = nwt.tags.map { it.id }.toMutableList()

            // Decrypt content if locked
            val content = if (nwt.note.isLocked) {
                CryptoHelper.decrypt(nwt.note.content) ?: nwt.note.content
            } else {
                nwt.note.content
            }

            lastSavedHash = computeHash(nwt.note.title, content)

            binding.etTitle.setText(nwt.note.title)
            binding.etContent.setText(content)
            applyColor(nwt.note.color)
            updateTagChips(nwt.tags)
            invalidateOptionsMenu()

            // Observe backlinks - show as tappable list
            vm.getBacklinks(nwt.note.title, id).observe(this@EditorActivity) { links ->
                setupBacklinksPanel(links)
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

    // ── Backlinks Panel ──────────────────────────────────────────────
    private fun setupBacklinksPanel(links: List<Note>) {
        binding.tvBacklinks.visibility = View.GONE
        binding.backlinksContainer.removeAllViews()

        if (links.isEmpty()) return

        // Show header
        val header = AndroidTextView(this).apply {
            text = "Backlinks (${links.size})"
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setPadding(0, 16, 0, 4)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        binding.backlinksContainer.addView(header)

        links.take(5).forEach { link ->
            val item = AndroidTextView(this).apply {
                text = "  -> ${link.title.ifBlank { "Untitled" }}"
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                setPadding(16, 8, 0, 8)
                setBackgroundResource(R.drawable.bg_preview_dot)
                setOnClickListener {
                    startActivity(Intent(this@EditorActivity, EditorActivity::class.java)
                        .putExtra(EXTRA_NOTE_ID, link.id))
                }
            }
            binding.backlinksContainer.addView(item)
        }

        if (links.size > 5) {
            val more = AndroidTextView(this).apply {
                text = "  +${links.size - 5} more"
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.text_hint))
                setPadding(16, 4, 0, 8)
            }
            binding.backlinksContainer.addView(more)
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
            "UL" to { insertLine("- ") },
            "OL" to { insertLine("1. ") },
            "[]" to { insertLine("- [ ] ") },
            "---" to { insertAtCursor("\n---\n") },
            "[[" to { insertWikiLink() },
            "Table" to { showTableDialog() },
            "Mic" to { startVoiceInput() },
            "Eye" to { togglePreview() }
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
        et.setSelection(pos + 4)
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

    // Holds grid cell references so we can read user input on "Insert Table"
    private data class TableGridData(
        val headers: List<EditText>,
        val rows: List<List<EditText>>
    )
    private var currentTableGridData: TableGridData? = null

    private fun showTableDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_table_input, null)
        val etRows    = dialogView.findViewById<EditText>(R.id.etTableRows)
        val etCols    = dialogView.findViewById<EditText>(R.id.etTableCols)
        val gridContainer = dialogView.findViewById<GridLayout>(R.id.gridTableInput)
        val btnBuild  = dialogView.findViewById<android.widget.Button>(R.id.btnBuildTable)

        etRows.setText("3")
        etCols.setText("3")

        fun rebuildGrid() {
            val rowCount = etRows.text.toString().toIntOrNull()?.coerceIn(1, 20) ?: 0
            val colCount = etCols.text.toString().toIntOrNull()?.coerceIn(1, 8)  ?: 0
            if (rowCount == 0 || colCount == 0) {
                currentTableGridData = null
                return
            }

            gridContainer.removeAllViews()
            gridContainer.columnCount = colCount + 1

            // Corner spacer
            gridContainer.addView(AndroidTextView(this).apply {
                text = ""
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.text_hint))
            })

            // Header row
            val headerEdits = mutableListOf<EditText>()
            for (c in 0 until colCount) {
                val headerEt = EditText(this).apply {
                    hint = "Col ${c + 1}"
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(context, R.color.accent))
                    setHintTextColor(ContextCompat.getColor(context, R.color.text_hint))
                    setBackgroundColor(0x00000000)
                    setPadding(6, 8, 6, 8)
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = GridLayout.LayoutParams.WRAP_CONTENT
                        columnSpec = GridLayout.spec(c + 1, 1f)
                        rowSpec = GridLayout.spec(0)
                    }
                }
                headerEdits.add(headerEt)
                gridContainer.addView(headerEt)
            }

            // Data rows
            val allRowEdits = mutableListOf<MutableList<EditText>>()
            for (r in 0 until rowCount) {
                // Row label
                gridContainer.addView(AndroidTextView(this).apply {
                    text = "R${r + 1}"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.text_hint))
                    gravity = android.view.Gravity.CENTER
                    setPadding(4, 8, 8, 8)
                    layoutParams = GridLayout.LayoutParams().apply {
                        rowSpec = GridLayout.spec(r + 1)
                    }
                })

                val rowEdits = mutableListOf<EditText>()
                for (c in 0 until colCount) {
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
                            height = GridLayout.LayoutParams.WRAP_CONTENT
                            columnSpec = GridLayout.spec(c + 1, 1f)
                            rowSpec = GridLayout.spec(r + 1)
                        }
                    }
                    rowEdits.add(cellEt)
                    gridContainer.addView(cellEt)
                }
                allRowEdits.add(rowEdits)
            }

            // Store references directly in a class field — avoids View.setTag(int, Object) issues
            currentTableGridData = TableGridData(headerEdits, allRowEdits)
        }

        rebuildGrid()

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
            val data = currentTableGridData
            if (data == null) {
                dialog.dismiss()
                return@setOnClickListener
            }
            val headers = data.headers.map { it.text.toString().trim() }
            val allRows = data.rows.map { row ->
                row.map { it.text.toString().trim() }
            }

            val tableMd = buildMarkdownTable(headers, allRows)
            if (tableMd.isNotBlank()) {
                insertAtCursor(tableMd)
            }
            currentTableGridData = null
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun buildMarkdownTable(headers: List<String>, rows: List<List<String>>): String {
        val cols = headers.size.coerceAtLeast(if (rows.isNotEmpty()) rows[0].size else 0)
        if (cols == 0) return ""

        val sb = StringBuilder()
        sb.append("\n")
        val headerLine = headers.map { it.ifBlank { "Col" } }.joinToString(" | ")
        sb.append("| $headerLine |\n")
        val sepLine = headers.indices.joinToString(" | ") { "---" }
        sb.append("| $sepLine |\n")
        rows.forEach { row ->
            val cells = row + List(cols - row.size) { "" }
            val rowLine = cells.joinToString(" | ")
            sb.append("| $rowLine |\n")
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
            // Full preview mode — disable live preview, hide editor, show rendered content
            isLivePreview = false
            livePreviewJob?.cancel()
            updateLivePreview()
        } else {
            // Exiting preview mode — re-enable live preview if table detected
            val content = binding.etContent.text.toString()
            if (content.contains("| --- |")) {
                enableLivePreview()
            }
        }
        updatePreviewVisibility()
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
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
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

    // ── Word count / autosave / live preview ────────────────────────────────

    private fun setupWordCount() {
        binding.etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val stats = s.toString().textStats()
                supportActionBar?.subtitle = stats.label
                // Auto-detect table syntax and enable live preview
                if (!isLivePreview && s.toString().contains("| --- |")) {
                    enableLivePreview()
                }
                // Update live preview if active
                if (isLivePreview) {
                    scheduleLivePreviewUpdate(s.toString())
                }
            }
        })
    }

    private fun enableLivePreview() {
        isLivePreview = true
        updatePreviewVisibility()
        updateLivePreview()
    }

    private fun disableLivePreview() {
        isLivePreview = false
        livePreviewJob?.cancel()
        updatePreviewVisibility()
    }

    private fun scheduleLivePreviewUpdate(content: String) {
        livePreviewJob?.cancel()
        livePreviewJob = lifecycleScope.launch {
            delay(300)
            updateLivePreview()
        }
    }

    private fun updateLivePreview() {
        val content = binding.etContent.text.toString()
        val rendered = MarkdownHelper.renderWikiLinks(content)
        markwon.setMarkdown(binding.tvPreview, rendered)
    }

    private fun updatePreviewVisibility() {
        val showPreview = isPreviewMode || isLivePreview
        if (showPreview) {
            binding.scrollPreview.visibility = View.VISIBLE
            binding.tvPreview.visibility = View.VISIBLE
            if (isPreviewMode) {
                // Full preview: hide editor
                binding.etContent.visibility = View.GONE
                binding.previewDivider.visibility = View.GONE
                binding.scrollPreview.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
                )
            } else {
                // Live preview: show both, split vertically
                binding.etContent.visibility = View.VISIBLE
                binding.previewDivider.visibility = View.VISIBLE
                binding.editorRoot.orientation = LinearLayout.VERTICAL
                binding.etContent.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
                )
                binding.scrollPreview.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
                )
            }
        } else {
            binding.scrollPreview.visibility = View.GONE
            binding.tvPreview.visibility = View.GONE
            binding.previewDivider.visibility = View.GONE
            binding.etContent.visibility = View.VISIBLE
            binding.editorRoot.orientation = LinearLayout.VERTICAL
            binding.etContent.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
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

    // ── Tags ────────────────────────────────────────────────────────────

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

    // ── Reminder ──────────────────────────────────────────────────────

    private fun showReminderDialog() {
        val calendar = Calendar.getInstance()
        if (reminderTime != null) calendar.timeInMillis = reminderTime!!

        val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendar.set(year, month, day)
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                setReminder(calendar.timeInMillis)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }

        DatePickerDialog(this, dateListener,
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun setReminder(time: Long) {
        reminderTime = time
        Snackbar.make(binding.root, "Reminder set", Snackbar.LENGTH_SHORT).show()
    }

    private fun removeReminder() {
        reminderTime = null
        if (currentNoteId > 0) {
            ReminderHelper.cancelReminder(this, currentNoteId)
        }
        Snackbar.make(binding.root, "Reminder removed", Snackbar.LENGTH_SHORT).show()
    }

    // ── Share ──────────────────────────────────────────────────────────

    private fun shareNote() {
        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()
        if (title.isBlank() && content.isBlank()) return

        val shareText = if (title.isNotBlank()) "# $title\n\n$content" else content
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Share Note"))
    }

    // ── Export single note ────────────────────────────────────────────

    private val exportNoteLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = binding.etContent.text.toString()
                contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                Snackbar.make(binding.root, "Note exported", Snackbar.LENGTH_LONG).show()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Export failed: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportNote() {
        val title = binding.etTitle.text.toString().ifBlank { "Untitled" }
        exportNoteLauncher.launch("$title.md")
    }

    // ── Save ──────────────────────────────────────────────────────────

    private fun performSave(silent: Boolean = false) {
        val title   = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()
        if (title.isBlank() && content.isBlank()) { if (!silent) finish(); return }

        val currentHash = computeHash(title, content)
        if (silent && currentHash == lastSavedHash) return

        // Encrypt content if locked
        val storedContent = if (isLocked) CryptoHelper.encrypt(content) else content

        val note = existingNote?.copy(
            title = title, content = storedContent, color = selectedColor,
            isPinned = isPinned, isLocked = isLocked, isFavorite = isFavorite,
            reminderTime = reminderTime,
            updatedAt = System.currentTimeMillis()
        ) ?: Note(title = title, content = storedContent, color = selectedColor,
            isPinned = isPinned, isLocked = isLocked, isFavorite = isFavorite,
            reminderTime = reminderTime)

        lifecycleScope.launch {
            if (existingNote != null) {
                vm.saveVersion(existingNote!!)
                vm.update(note)
                vm.setTagsForNote(note.id, selectedTagIds)
                // Schedule/cancel reminder
                reminderTime?.let { ReminderHelper.scheduleReminder(this@EditorActivity, note.copy(reminderTime = it)) }
                    ?: ReminderHelper.cancelReminder(this@EditorActivity, note.id)
                lastSavedHash = currentHash
            } else {
                val id = KitabuDatabase
                    .getDatabase(applicationContext).noteDao().insertNote(note)
                currentNoteId = id.toInt()
                existingNote  = note.copy(id = currentNoteId)
                vm.setTagsForNote(currentNoteId, selectedTagIds)
                reminderTime?.let {
                    ReminderHelper.scheduleReminder(this@EditorActivity, note.copy(id = currentNoteId, reminderTime = it))
                }
                lastSavedHash = currentHash
            }
            if (!silent) finish()
        }
    }

    // ── Options menu ──────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        menu.findItem(R.id.action_pin)?.setIcon(if (isPinned) R.drawable.ic_pin_filled else R.drawable.ic_pin_outline)
        menu.findItem(R.id.action_favorite)?.setIcon(
            if (isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
        )
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home     -> { performSave(); true }
        R.id.action_pin       -> { isPinned = !isPinned; invalidateOptionsMenu(); true }
        R.id.action_lock      -> { isLocked = !isLocked
            Snackbar.make(binding.root, if (isLocked) "Note locked" else "Note unlocked", Snackbar.LENGTH_SHORT).show()
            true }
        R.id.action_history   -> {
            startActivity(Intent(this, VersionHistoryActivity::class.java)
                .putExtra(VersionHistoryActivity.EXTRA_NOTE_ID, currentNoteId))
            true }
        R.id.action_ai        -> {
            startActivity(Intent(this, AiAssistantActivity::class.java)
                .putExtra(AiAssistantActivity.EXTRA_NOTE_CONTENT, binding.etContent.text.toString()))
            true }
        R.id.action_save      -> { performSave(); true }
        R.id.action_share     -> { shareNote(); true }
        R.id.action_reminder  -> { showReminderDialog(); true }
        R.id.action_favorite  -> {
            isFavorite = !isFavorite
            invalidateOptionsMenu()
            Snackbar.make(binding.root,
                if (isFavorite) "Added to favorites" else "Removed from favorites",
                Snackbar.LENGTH_SHORT).show()
            true }
        R.id.action_export    -> { exportNote(); true }
        else                  -> super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() { performSave() }
    override fun onPause()       { super.onPause(); performSave(silent = true) }
}
