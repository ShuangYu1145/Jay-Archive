package love.yu.jaychou

import java.io.File
import love.yu.jaychou.data.CompositionRepository
import org.junit.Assert.*
import org.junit.Test

class CompositionTest {
    private val catalog = CompositionRepository.decode(File("src/main/assets/compositions.json").readText())

    @Test fun catalogueContainsOtherArtistsAndRetainsOriginalVersions() {
        assertTrue(catalog.songs.size >= 170)
        assertEquals(catalog.songs.size, catalog.songs.map { it.id }.distinct().size)
        assertEquals(catalog.songs.map { it.releaseDate }.sortedDescending(), catalog.songs.map { it.releaseDate })
        assertTrue(catalog.songs.none { it.text.artist == "周杰伦" || it.text.artist == "周杰倫" })
        val roof = catalog.songs.single { it.text.title == "屋顶" }
        assertEquals("吴宗宪/温岚", roof.text.artist)
        assertEquals("1999-04-12", roof.releaseDate)
        assertEquals("周杰伦", roof.text.lyricist)
        val elimination = catalog.songs.single { it.text.title == "淘汰" }
        assertEquals("陈奕迅", elimination.text.artist)
        assertEquals("2007-04-24", elimination.releaseDate)
    }

    @Test fun searchWorksAcrossScriptsArtistsAlbumsAndMultipleTerms() {
        val rewind = catalog.songs.single { it.text.title == "倒带" }
        assertTrue(rewind.matches(" 倒帶 蔡依林 "))
        assertTrue(rewind.matches("城堡 2004"))
        assertTrue(rewind.matches("倒带"))
        assertFalse(rewind.matches("倒带 温岚"))
        assertTrue(catalog.songs.all { it.matches("   ") })
        assertTrue(catalog.songs.any { it.matches("ｓ．ｈ．ｅ") })
        assertTrue(catalog.songs.none { it.matches("no-such-song-91827") })
        assertEquals("倒帶", rewind.display("zh-Hant").title)
        assertEquals("倒带", rewind.display("zh-Hans").title)
    }

    @Test fun artworkHasOnlineUrlAndTraceableSource() {
        catalog.songs.filter { it.coverUrl.isNotEmpty() }.forEach {
            assertTrue(it.coverUrl.startsWith("https://"))
            assertTrue(it.artworkSourceUrl.startsWith("https://music.apple.com/"))
        }
        assertTrue(catalog.sourceUrl.startsWith("https://zh.wikipedia.org/"))
    }
}
