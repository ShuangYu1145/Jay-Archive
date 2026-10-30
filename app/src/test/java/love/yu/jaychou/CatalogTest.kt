package love.yu.jaychou

import java.io.File
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import love.yu.jaychou.data.AlbumRepository
import love.yu.jaychou.data.ReleaseCategory
import org.junit.Assert.*
import org.junit.Test

class CatalogTest {
    private val catalog = AlbumRepository.decode(File("src/main/assets/albums.json").readText())

    @Test fun originalStudioEditionsAreCompleteAndOrdered() {
        assertEquals("2026-10-17", catalog.verifiedAt)
        assertEquals(16, catalog.albums.size)
        assertEquals(listOf(13, 12, 10, 12, 12, 11, 11, 11, 10, 10, 12, 10, 11, 10, 10, 10), catalog.albums.map { it.tracks.size })
        assertEquals("太阳之子", catalog.albums.first().title)
        assertEquals("Jay", catalog.albums.last().title)
        assertEquals(catalog.albums.map { it.releaseDate }.sortedDescending(), catalog.albums.map { it.releaseDate })
        catalog.albums.forEach { album ->
            assertEquals(album.releaseDate, OffsetDateTime.parse(album.sourceDateRaw).atZoneSameInstant(ZoneOffset.ofHours(8)).toLocalDate().toString())
            LocalDate.parse(album.releaseDate)
            assertTrue(album.tracks.all { it.title.isNotBlank() })
            assertEquals((1..album.tracks.size).toList(), album.tracks.map { it.number })
            assertTrue(album.coverUrl.startsWith("https://"))
            assertTrue(album.sourceUrl.startsWith("https://www.jvrmusic.com/artist/gallery/detail/"))
        }
    }

    @Test fun representativeTrackOrderMatchesOriginalAlbumPages() {
        assertEquals("安静", catalog.find("1152155344949088256")!!.tracks.last().title)
        assertEquals("东风破", catalog.find("1152152148126076928")!!.tracks[4].title)
        assertEquals("七里香", catalog.find("1152151605810958336")!!.tracks[1].title)
        assertEquals("Intro", catalog.albums[1].tracks.first().title)
        assertEquals("圣诞星（feat. 杨瑞代）", catalog.albums.first().tracks.last().title)
    }

    @Test fun extendedEditionsHaveReviewedCategoriesDiscOrderAndAssets() {
        val extra = AlbumRepository.decode(File("src/main/assets/extended-albums.json").readText())
        assertEquals(mapOf(ReleaseCategory.LIVE to 6, ReleaseCategory.EP to 4,
            ReleaseCategory.SINGLE to 21, ReleaseCategory.OTHER to 2), extra.albums.groupingBy { it.category }.eachCount())
        extra.albums.forEach { album ->
            assertTrue(album.sourceUrl.startsWith("https://music.apple.com/"))
            assertTrue(album.tracks.isNotEmpty())
            assertEquals(album.tracks.map { it.disc to it.sourceTrackNumber },
                album.tracks.sortedWith(compareBy({ it.disc }, { it.sourceTrackNumber })).map { it.disc to it.sourceTrackNumber })
            assertTrue(album.coverUrl.startsWith("https://"))
            assertTrue(album.dateNote.contains("数字版"))
            LocalDate.parse(album.releaseDate)
        }
        assertEquals(25, extra.find("apple-1485210180")!!.tracks.size)
        assertEquals(20, extra.find("apple-536686684")!!.tracks.size)
    }
}
