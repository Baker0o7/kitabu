package com.kitabu.app.ui.theme

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
import com.kitabu.app.R
import com.kitabu.app.databinding.ActivityThemePickerBinding
import com.kitabu.app.databinding.ItemThemeBinding
import com.kitabu.app.util.KitabuTheme
import com.kitabu.app.util.ThemeManager

class ThemePickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemePickerBinding
    private var selectedTheme = KitabuTheme.NOIR

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        binding = ActivityThemePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Themes"

        selectedTheme = ThemeManager.get(this)

        val adapter = ThemeAdapter(selectedTheme) { theme ->
            if (theme == selectedTheme) return@ThemeAdapter
            selectedTheme = theme
            ThemeManager.set(this, theme)
            // Restart all activities to apply new theme
            recreate()
        }

        binding.recyclerView.layoutManager =
            GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false)
        binding.recyclerView.adapter = adapter
        adapter.submitList(KitabuTheme.entries.toList())
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (item.itemId == android.R.id.home) { finish(); true }
        else super.onOptionsItemSelected(item)
}

// ── Adapter ──────────────────────────────────────────────────────────────────

class ThemeAdapter(
    private var activeTheme: KitabuTheme,
    private val onSelect: (KitabuTheme) -> Unit
) : ListAdapter<KitabuTheme, ThemeAdapter.VH>(Diff) {

    companion object Diff : DiffUtil.ItemCallback<KitabuTheme>() {
        override fun areItemsTheSame(a: KitabuTheme, b: KitabuTheme) = a.id == b.id
        override fun areContentsTheSame(a: KitabuTheme, b: KitabuTheme) = a == b
    }

    inner class VH(val b: ItemThemeBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(theme: KitabuTheme) {
            val isActive = theme == activeTheme

            // ── Card background = theme's bg colour ──
            b.cardTheme.setCardBackgroundColor(Color.parseColor(theme.bgHex))

            // ── Border highlight when selected ──
            b.cardTheme.strokeColor = if (isActive)
                Color.parseColor(theme.accentHex) else Color.parseColor("#33FFFFFF")
            b.cardTheme.strokeWidth = if (isActive) 6 else 1

            // ── Mini UI preview ──
            // Top bar
            b.previewBar.setBackgroundColor(Color.parseColor(theme.surfaceHex))
            b.previewBarDot.setBackgroundColor(Color.parseColor(theme.accentHex))

            // Note cards preview
            drawPreviewCard(b.previewCard1, theme.surfaceHex, theme.accentHex, wide = true)
            drawPreviewCard(b.previewCard2, theme.surfaceHex, theme.accentHex, wide = false)
            drawPreviewCard(b.previewCard3, theme.surfaceHex, theme.accentHex, wide = false)

            // Accent swatch
            b.accentSwatch.setBackgroundColor(Color.parseColor(theme.accentHex))

            // Labels
            b.tvEmoji.text       = theme.emoji
            b.tvThemeName.text   = theme.label
            b.tvThemeDesc.text   = theme.description
            b.tvThemeName.setTextColor(Color.parseColor(theme.accentHex))
            b.tvThemeDesc.setTextColor(adjustAlpha(Color.parseColor(theme.accentHex), 0.6f))

            // Checkmark
            b.ivCheck.visibility = if (isActive) View.VISIBLE else View.GONE
            b.ivCheck.setColorFilter(Color.parseColor(theme.accentHex))

            b.root.setOnClickListener { onSelect(theme) }
        }

        private fun drawPreviewCard(view: View, surfaceHex: String, accentHex: String, wide: Boolean) {
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor(surfaceHex))
                cornerRadius = 8f
            }
            view.background = bg
            // Tint accent line inside the card using a tag
            (view.tag as? View)?.setBackgroundColor(Color.parseColor(accentHex))
        }

        private fun adjustAlpha(color: Int, factor: Float): Int {
            val alpha = (Color.alpha(color) * factor).toInt()
            return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemThemeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))
}
