package love.yu.jaychou

import love.yu.jaychou.playback.*
import org.junit.Assert.*
import org.junit.Test

class KaraokeTimingTest {
    @Test fun aGlyphCanBePartiallyHighlightedAndSeekBackwards() {
        val glyph = karaokeGlyphs(EmbeddedLyricLine(0, "字", listOf(EmbeddedLyricWord(100, 500, "字")))).single()
        assertEquals(0f, glyph.progress(50))
        assertEquals(.5f, glyph.progress(300))
        assertEquals(.75f, glyph.progress(400))
        assertEquals(1f, glyph.progress(600))
        assertEquals(.25f, glyph.progress(200))
    }

    @Test fun wordTimingSpansMultipleGlyphsWithoutSplittingSurrogates() {
        val glyphs = karaokeGlyphs(EmbeddedLyricLine(0, "A😀字", listOf(EmbeddedLyricWord(0, 900, "A😀字"))))
        assertEquals(listOf(0, 1, 3), glyphs.map { it.startOffset })
        assertEquals(listOf(1, 3, 4), glyphs.map { it.endOffset })
        assertEquals(.5f, glyphs[1].progress(450))
        assertEquals(0f, glyphs[2].progress(450))
    }

    @Test fun zeroLengthWordsAndTrimmedSpaceHaveValidBounds() {
        val glyphs = karaokeGlyphs(EmbeddedLyricLine(0, "字", listOf(EmbeddedLyricWord(100, 100, " 字 "))))
        assertEquals(1, glyphs.size)
        assertEquals(0, glyphs.single().startOffset)
        assertEquals(1f, glyphs.single().progress(100))
    }
}
