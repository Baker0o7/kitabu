package com.kitabu.app.util

import android.content.Context
import androidx.annotation.StyleRes
import com.kitabu.app.R

enum class KitabuTheme(
    val id: String,
    val label: String,
    val emoji: String,
    val description: String,
    @StyleRes val styleRes: Int,
    val bgHex: String,
    val surfaceHex: String,
    val accentHex: String
) {
    NOIR(
        id = "noir", label = "Noir", emoji = "🌑",
        description = "Deep purple-black — the classic",
        styleRes = R.style.Theme_Kitabu_Noir,
        bgHex = "#111118", surfaceHex = "#1E1E2E", accentHex = "#C8A2FF"
    ),
    MIDNIGHT(
        id = "midnight", label = "Midnight", emoji = "🌌",
        description = "Pure AMOLED black + electric blue",
        styleRes = R.style.Theme_Kitabu_Midnight,
        bgHex = "#000000", surfaceHex = "#0D0D0D", accentHex = "#4FC3F7"
    ),
    FOREST(
        id = "forest", label = "Forest", emoji = "🌿",
        description = "Dark pine green + emerald glow",
        styleRes = R.style.Theme_Kitabu_Forest,
        bgHex = "#080F08", surfaceHex = "#0F1F0F", accentHex = "#69F0AE"
    ),
    CRIMSON(
        id = "crimson", label = "Crimson", emoji = "🔴",
        description = "Deep red — bold and dramatic",
        styleRes = R.style.Theme_Kitabu_Crimson,
        bgHex = "#0F0505", surfaceHex = "#1A0808", accentHex = "#FF6B6B"
    ),
    DUSK(
        id = "dusk", label = "Dusk", emoji = "🌅",
        description = "Warm amber on dark espresso",
        styleRes = R.style.Theme_Kitabu_Dusk,
        bgHex = "#100A04", surfaceHex = "#1C1208", accentHex = "#FFB74D"
    ),
    NORD(
        id = "nord", label = "Nord", emoji = "❄️",
        description = "Arctic blue-grey — calm and focused",
        styleRes = R.style.Theme_Kitabu_Nord,
        bgHex = "#2E3440", surfaceHex = "#3B4252", accentHex = "#88C0D0"
    ),
    ROSE(
        id = "rose", label = "Rose", emoji = "🌸",
        description = "Dark plum with blush pink accent",
        styleRes = R.style.Theme_Kitabu_Rose,
        bgHex = "#120009", surfaceHex = "#1C0D14", accentHex = "#F48FB1"
    ),
    SLATE(
        id = "slate", label = "Slate", emoji = "🌊",
        description = "Desaturated ocean — cool and clean",
        styleRes = R.style.Theme_Kitabu_Slate,
        bgHex = "#0D1317", surfaceHex = "#151C20", accentHex = "#80DEEA"
    ),
    AURORA(
        id = "aurora", label = "Aurora", emoji = "🌈",
        description = "Northern lights gradient — vivid purple to green",
        styleRes = R.style.Theme_Kitabu_Aurora,
        bgHex = "#0A0A14", surfaceHex = "#141428", accentHex = "#B388FF"
    );

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: NOIR
    }
}

object ThemeManager {
    private const val PREF_FILE  = "kitabu_prefs"
    private const val PREF_THEME = "app_theme"

    fun get(context: Context): KitabuTheme {
        val id = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .getString(PREF_THEME, KitabuTheme.NOIR.id) ?: KitabuTheme.NOIR.id
        return KitabuTheme.fromId(id)
    }

    fun set(context: Context, theme: KitabuTheme) {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .edit().putString(PREF_THEME, theme.id).apply()
    }

    /** Call this at the top of every Activity's onCreate(), before setContentView(). */
    fun apply(context: Context) {
        context as android.app.Activity
        context.setTheme(get(context).styleRes)
    }
}
