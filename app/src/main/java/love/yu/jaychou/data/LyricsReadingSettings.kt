package love.yu.jaychou.data

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

enum class LyricsAlignment { LEFT, CENTER, RIGHT }
enum class LyricsBackground { SYSTEM, ALBUM, PLAIN }

data class LyricsReadingSettings(
    val fontSize: Float = 23f,
    val lineSpacing: Float = 1.7f,
    val alignment: LyricsAlignment = LyricsAlignment.LEFT,
    val background: LyricsBackground = LyricsBackground.SYSTEM,
    val immersive: Boolean = false,
    val showTranslation: Boolean = true,
) {
    fun normalized() = copy(
        fontSize = if (fontSize.isFinite()) fontSize.coerceIn(18f, 32f) else 23f,
        lineSpacing = if (lineSpacing.isFinite()) lineSpacing.coerceIn(1.2f, 2.2f) else 1.7f,
    )
}

internal object LyricsReadingKeys {
    val font = floatPreferencesKey("lyrics_reader_font_size")
    val spacing = floatPreferencesKey("lyrics_reader_line_spacing")
    val alignment = stringPreferencesKey("lyrics_reader_alignment")
    val background = stringPreferencesKey("lyrics_reader_background")
    val immersive = booleanPreferencesKey("lyrics_reader_immersive")
    val translation = booleanPreferencesKey("lyrics_reader_translation")

    fun decode(prefs: Preferences) = LyricsReadingSettings(
        fontSize = prefs[font] ?: 23f,
        lineSpacing = prefs[spacing] ?: 1.7f,
        alignment = LyricsAlignment.entries.find { it.name == prefs[alignment] } ?: LyricsAlignment.LEFT,
        background = LyricsBackground.entries.find { it.name == prefs[background] } ?: LyricsBackground.SYSTEM,
        immersive = prefs[immersive] ?: false,
        showTranslation = prefs[translation] ?: true,
    ).normalized()
}

data class LyricsMatch(val lineIndex: Int, val start: Int, val end: Int, val translation: Boolean)

/** Literal, case-insensitive matches keep offsets in the original, unmodified text. */
fun searchLyrics(lines: List<LyricLine>, query: String, includeTranslation: Boolean): List<LyricsMatch> {
    if (query.isBlank()) return emptyList()
    return buildList {
        lines.forEachIndexed { index, line ->
            fun find(text: String, translated: Boolean) {
                var start = text.indexOf(query, ignoreCase = true)
                while (start >= 0) {
                    add(LyricsMatch(index, start, start + query.length, translated))
                    start = text.indexOf(query, start + query.length, ignoreCase = true)
                }
            }
            find(line.text, false)
            if (includeTranslation) line.translation?.let { find(it, true) }
        }
    }
}

fun copyableLyrics(lines: List<LyricLine>, includeTranslation: Boolean): String =
    lines.joinToString("\n") { line ->
        line.text + if (includeTranslation && !line.translation.isNullOrBlank()) "\n${line.translation}" else ""
    }
