package love.yu.jaychou

import androidx.datastore.preferences.core.mutablePreferencesOf
import love.yu.jaychou.data.*
import org.junit.Assert.*
import org.junit.Test

class LyricsReadingTest {
    @Test fun settingsUseReadingDefaultsAndClampInvalidValues() {
        assertEquals(LyricsReadingSettings(), LyricsReadingKeys.decode(mutablePreferencesOf()))
        val decoded = LyricsReadingKeys.decode(mutablePreferencesOf(
            LyricsReadingKeys.font to 100f, LyricsReadingKeys.spacing to Float.NaN,
            LyricsReadingKeys.alignment to "unknown", LyricsReadingKeys.background to "unknown"))
        assertEquals(32f, decoded.fontSize)
        assertEquals(1.7f, decoded.lineSpacing)
        assertEquals(LyricsAlignment.LEFT, decoded.alignment)
        assertEquals(LyricsBackground.SYSTEM, decoded.background)
        assertEquals(18f, LyricsReadingSettings(fontSize = -9f).normalized().fontSize)
    }

    @Test fun literalSearchKeepsEveryOccurrenceAndOriginalOffsets() {
        val lines = listOf(LyricLine(null, "灯塔 灯塔"), LyricLine(null, ""), LyricLine(null, "[a.b] A.B"))
        assertEquals(listOf(LyricsMatch(0, 0, 2, false), LyricsMatch(0, 3, 5, false)), searchLyrics(lines, "灯塔", false))
        assertEquals(listOf(LyricsMatch(2, 1, 4, false), LyricsMatch(2, 6, 9, false)), searchLyrics(lines, "a.b", false))
        assertTrue(searchLyrics(lines, " ", true).isEmpty())
        assertTrue(searchLyrics(lines, "missing", true).isEmpty())
        assertEquals("[a.b] A.B", lines.last().text)
    }

    @Test fun translationsCanBeSearchedAndCopiedWithoutChangingParagraphs() {
        val lines = listOf(LyricLine(null, "First light", "第一道光"), LyricLine(null, ""), LyricLine(null, "归来"))
        assertTrue(searchLyrics(lines, "光", false).isEmpty())
        assertEquals(listOf(LyricsMatch(0, 3, 4, true)), searchLyrics(lines, "光", true))
        assertEquals("First light\n\n归来", copyableLyrics(lines, false))
        assertEquals("First light\n第一道光\n\n归来", copyableLyrics(lines, true))
    }
}
