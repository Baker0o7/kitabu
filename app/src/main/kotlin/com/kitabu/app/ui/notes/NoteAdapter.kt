package com.kitabu.app.ui.notes

import android.view.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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

    inner class VH(val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(nwt: NoteWithTags) {
            val note = nwt.note
            binding.apply {
                tvTitle.text   = note.title.ifBlank { "Untitled" }
                tvContent.text = note.content.stripMarkdown().truncate(200)
                tvDate.text    = note.updatedAt.toNoteDate()
                cardNote.setCardBackgroundColor(note.color)

                ivPin.visibility  = if (note.isPinned)  View.VISIBLE else View.GONE
                ivLock.visibility = if (note.isLocked)  View.VISIBLE else View.GONE

                // Word count badge
                val wc = com.kitabu.app.util.MarkdownHelper.wordCount(note.content)
                tvWordCount.text = if (wc > 0) "${wc}w" else ""
                tvWordCount.visibility = if (wc > 0) View.VISIBLE else View.GONE

                // Tag chips (show up to 3)
                chipGroup.removeAllViews()
                nwt.tags.take(3).forEach { tag ->
                    val chip = com.google.android.material.chip.Chip(root.context).apply {
                        text = "#${tag.leaf}"
                        textSize = 9f
                        chipMinHeight = 20f.dpToPx(context)
                        setChipBackgroundColorResource(android.R.color.transparent)
                        chipStrokeWidth = 1f
                        setChipStrokeColorResource(android.R.color.white)
                        setTextColor(android.graphics.Color.WHITE)
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
                        setChipStrokeColorResource(android.R.color.white)
                        setTextColor(android.graphics.Color.WHITE)
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
}
