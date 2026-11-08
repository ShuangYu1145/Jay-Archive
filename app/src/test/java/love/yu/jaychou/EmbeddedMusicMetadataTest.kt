package love.yu.jaychou

import java.io.ByteArrayInputStream
import java.io.IOException
import love.yu.jaychou.playback.EmbeddedMusicMetadata
import org.junit.Assert.*
import org.junit.Test

class EmbeddedMusicMetadataTest {
    @Test fun preservesInlineWordTimingAndTerminalStamp() {
        val lines = EmbeddedMusicMetadata.parseLyrics("[offset:100]\n[00:01.2]甲[00:01.50]乙[00:02.000]")
        assertEquals("甲乙", lines.single().text)
        assertEquals(1_300L, lines.single().startMs)
        assertEquals(listOf(1_300L, 1_600L), lines.single().words.map { it.startMs })
        assertEquals(listOf(1_600L, 2_100L), lines.single().words.map { it.endMs })
    }

    @Test fun repeatedLrcStampsRemainSeparateLinesAndPlainLyricsRemainUntimed() {
        val repeated = EmbeddedMusicMetadata.parseLyrics("[ti:Example]\n[00:01][00:05]repeated line")
        assertEquals(listOf(1_000L, 5_000L), repeated.map { it.startMs })
        assertTrue(repeated.all { it.words.isEmpty() && it.text == "repeated line" })
        val plain = EmbeddedMusicMetadata.parseLyrics("first line\n\nsecond line")
        assertEquals(listOf("first line", "second line"), plain.map { it.text })
        assertTrue(plain.all { it.startMs == null })
    }

    @Test fun invalidAndTruncatedMetadataFailInsteadOfReadingPastTheBlock() {
        assertThrows(IOException::class.java) { EmbeddedMusicMetadata.read(ByteArrayInputStream(byteArrayOf(0, 0, 0, 0))) }
        assertThrows(IOException::class.java) {
            EmbeddedMusicMetadata.read(ByteArrayInputStream(byteArrayOf(0x66, 0x4c, 0x61, 0x43, 0x80.toByte(), 0, 0, 34)))
        }
        assertTrue(EmbeddedMusicMetadata.parseLyrics("[ti:Only metadata]\n[offset:0]").isEmpty())
    }
}
