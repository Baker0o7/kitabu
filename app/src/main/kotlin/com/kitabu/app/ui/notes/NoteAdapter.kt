package com.kitabu.app.ui.notes

import android.view.*
import androidx.recyclerview.widget.DiffUtil
import com.kitabu.app.R
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kitabu.app.data.NoteColor
import com.kitabu.app.data.NoteWithTags
import com.kitabu.app.databinding.ItemNoteBinding
import com.kitabu.app.util.*

class NoteAdapter(
    private val onClick: (NoteWithTags) -> Unit,
    private val onLongClick: (NoteWithTags) -> Boolean
) : ListAdapter<NoteWithTags, NoteAdapter.VH>(Diff) {

    companion object Diff : DiffUtil.ItemCallback<NoteWithTags>() {
        override fun areItemsTheSame(a: NoteWithTags, b: NoteWithTags) = a.note.id == b.note.id
        override fun areContentsTheSame(a: NoteWithTags, b: NoteWithTags) = a == b
    }

    // Multi-select state
    var isMultiSelectMode: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    val selectedIds: MutableSet<Long> = mutableSetOf()

    // Drag & drop state
    var isDragMode: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var onDragStart: ((RecyclerView.ViewHolder) -> Unit)? = null

    inner class VH(val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(nwt: NoteWithTags) {
            val note = nwt.note
            binding.apply {
                tvTitle.text   = note.title.ifBlank { "Untitled" }
                tvContent.text = note.content.stripMarkdown().truncate(200)
                tvDate.text    = note.updatedAt.toNoteDate()
                cardNote.setCardBackgroundColor(note.color)

                // Color the accent strip based on note color
                val noteColors = listOf(
                    0xFFC8A2FF.toInt(), // DEFAULT - purple accent
                    0xFFF48FB1.toInt(), // ROSE - pink accent
                    0xFF4FC3F7.toInt(), // OCEAN - blue accent
                    0xFF69F0AE.toInt(), // FOREST - green accent
                    0xFFFFB74D.toInt(), // AMBER - amber accent
                    0xFFB388FF.toInt(), // LAVENDER - lavender accent
                    0xFF80DEEA.toInt(), // TEAL - teal accent
                    0xFF90A4AE.toInt()  // CHARCOAL - grey accent
                )
                val accentColorIndex = NoteColor.all.indexOf(note.color).coerceIn(0, noteColors.size - 1)
                binding.accentStrip.setBackgroundColor(noteColors[accentColorIndex])

                ivPin.visibility  = if (note.isPinned)  View.VISIBLE else View.GONE
                ivLock.visibility = if (note.isLocked)  View.VISIBLE else View.GONE

                // Multi-select checkbox
                if (isMultiSelectMode) {
                    checkboxSelect.visibility = View.VISIBLE
                    checkboxSelect.setOnCheckedChangeListener(null)
                    checkboxSelect.isChecked = note.id.toLong() in selectedIds
                    checkboxSelect.setOnCheckedChangeListener { _, checked ->
                        if (checked) selectedIds.add(note.id.toLong()) else selectedIds.remove(note.id.toLong())
                    }
                    cardNote.alpha = if (note.id.toLong() in selectedIds) 1.0f else 0.5f
                    cardNote.strokeWidth = if (note.id.toLong() in selectedIds) 2 else 0
                    cardNote.setStrokeColor(
                        android.content.res.ColorStateList.valueOf(
                            root.context.resources.getColor(R.color.accent, root.context.theme)
                        )
                    )
                } else {
                    checkboxSelect.visibility = View.GONE
                    cardNote.alpha = 1.0f
                    cardNote.strokeWidth = 0
                }

                // Drag handle
                ivDragHandle.visibility = if (isDragMode && !isMultiSelectMode) View.VISIBLE else View.GONE
                ivDragHandle.setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        onDragStart?.invoke(this@VH)
                    }
                    false
                }

                // Word count badge
                val wc = com.kitabu.app.util.MarkdownHelper.wordCount(note.content)
                tvWordCount.text = if (wc > 0) "${wc}w" else ""
                tvWordCount.visibility = if (wc > 0) View.VISIBLE else View.GONE

                // Tag chips (show up to 3)
                chipGroup.removeAllViews()
                val chipColor = getContrastChipColor(note.color)
                nwt.tags.take(3).forEach { tag ->
                    val chip = com.google.android.material.chip.Chip(root.context).apply {
                        text = "#${tag.leaf}"
                        textSize = 9f
                        chipMinHeight = 20f.dpToPx(context)
                        setChipBackgroundColorResource(android.R.color.transparent)
                        chipStrokeWidth = 1f
                        chipStrokeColor = android.content.res.ColorStateList.valueOf(chipColor)
                        setTextColor(chipColor)
                        isClickable = false
                    }
                    chipGroup.addView(chip)
                }
                if (nwt.tags.size > 3) {
                    val more = com.google.android.material.chip.Chip(root.context).apply {
                        text = "+${nwt.tags.size - 3}"
                        textSize = 9f
                        chipMinHeight = 20f.dpToPx(context)
                        setChipBackgroundColorResource(android.R.color.transparent)
                        chipStrokeWidth = 1f
                        chipStrokeColor = android.content.res.ColorStateList.valueOf(chipColor)
                        setTextColor(chipColor)
                        isClickable = false
                    }
                    chipGroup.addView(more)
                }

                root.setOnClickListener { onClick(nwt) }
                root.setOnLongClickListener { onLongClick(nwt) }
            }
        }

        private fun Float.dpToPx(ctx: android.content.Context) =
            this * ctx.resources.displayMetrics.density
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    /** Clear all selections and exit multi-select mode. */
    fun clearSelections() {
        selectedIds.clear()
        isMultiSelectMode = false
    }

    /** Select or deselect all visible items. */
    fun toggleSelectAll() {
        if (selectedIds.size == currentList.size) {
            selectedIds.clear()
        } else {
            selectedIds.clear()
            currentList.forEach { selectedIds.add(it.note.id.toLong()) }
        }
        notifyDataSetChanged()
    }

    /** Returns count of selected items. */
    fun selectionCount(): Int = selectedIds.size

    /** Returns a list of selected Note items. */
    fun getSelectedNotes(): List<com.kitabu.app.data.Note> =
        currentList.filter { it.note.id.toLong() in selectedIds }.map { it.note }

    /** Returns a list of IDs in the current display order. */
    fun noteIds(): List<Long> = currentList.map { it.note.id.toLong() }

    /** Returns a copy of the current list reordered from->to. */
    fun reorderItem(fromPos: Int, toPos: Int): List<NoteWithTags> {
        val list = currentList.toMutableList()
        val item = list.removeAt(fromPos)
        list.add(toPos, item)
        return list
    }

    /** Returns a copy of the current list sorted by a given ID order. */
    fun sortByIdOrder(idOrder: List<Long>): List<NoteWithTags> {
        val orderMap = idOrder.withIndex().associate { (idx, id) -> id to idx }
        return currentList.sortedBy { orderMap[it.note.id.toLong()] ?: Int.MAX_VALUE }
    }

    /** Returns a chip color that is legible on any note card background. */
    private fun getContrastChipColor(noteColor: Int): Int {
        val r = android.graphics.Color.red(noteColor)
        val g = android.graphics.Color.green(noteColor)
        val b = android.graphics.Color.blue(noteColor)
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255f
        return if (luminance > 0.55) {
            android.graphics.Color.parseColor("#CC333333") // dark translucent on light notes
        } else {
            android.graphics.Color.parseColor("#B3FFFFFF") // white translucent on dark notes
        }
    }
}
