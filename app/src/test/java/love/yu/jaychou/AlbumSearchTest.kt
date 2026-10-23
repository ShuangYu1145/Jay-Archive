package love.yu.jaychou

import love.yu.jaychou.data.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AlbumSearchTest {
    private val studio = AlbumRepository.decode(File("src/main/assets/albums.json").readText())
    private val extra = AlbumRepository.decode(File("src/main/assets/extended-albums.json").readText())
    private val index = AlbumSearchIndex(AlbumCatalog(studio.verifiedAt, studio.albums + extra.albums))

    @Test fun sameNameAlbumAndSongAreBothReturned() {
        val result = index.search("七里香")
        assertTrue(result.albums.any { it.title == "七里香" })
        assertTrue(result.songs.any { it.album.title == "七里香" && it.track.title == "七里香" })
    }

    @Test fun songMatchRetainsItsAlbumAndOriginalTrackNumber() {
        val result = index.search("晴天")
        val match = result.songs.first { it.album.title == "叶惠美" }
        assertEquals(3, match.track.number)
        assertEquals("晴天", match.track.title)
        assertTrue(result.albums.isEmpty())
    }

    @Test fun normalizesWidthCaseAndWhitespaceAndIncludesOtherCategories() {
        assertEquals(index.search("jay"), index.search("  ＪＡＹ  "))
        assertEquals(index.search("Mine Mine"), index.search("minemine"))
        val single = extra.albums.first { it.category == ReleaseCategory.SINGLE }
        assertTrue(index.search(single.title).albums.any { it.id == single.id })
    }

    @Test fun blankAndMissingQueriesHaveNoResults() {
        for (query in listOf("", "   ", "完全不存在的曲目xyz")) {
            assertEquals(AlbumSearchResults(emptyList(), emptyList()), index.search(query))
        }
    }
}
