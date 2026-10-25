package love.yu.jaychou

import java.io.File
import love.yu.jaychou.data.AlbumRepository
import org.junit.Assert.*
import org.junit.Test

class TrackArtworkTest {
    private val catalog = AlbumRepository.decode(File("src/main/assets/albums.json").readText())

    @Test fun singlesHaveVerifiedArtworkAndBundledJpegFallbacks() {
        val works = catalog.find("1551833317039542272")!!
        assertEquals(setOf(4, 6, 7, 9, 12), works.tracks.filter { it.artwork != null }.map { it.number }.toSet())
        val waiting = works.tracks[8]
        assertEquals("等你下课", waiting.title)
        assertTrue(waiting.artwork!!.sourceUrl.endsWith("/212877900"))
        assertNotEquals(works.coverUrl, waiting.artwork.coverUrl)
        val special = catalog.albums.flatMap { it.tracks }.mapNotNull { it.artwork }
        assertEquals(6, special.size)
        special.forEach { art ->
            assertTrue(art.coverUrl.startsWith("https://y.qq.com/music/photo_new/"))
            val bytes = File("src/main/assets/${art.coverAsset}").readBytes()
            assertTrue("Bundled thumbnail must be JPEG", bytes.size > 1000 &&
                bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte())
        }
        assertNull("Ordinary songs use the album artwork", works.tracks.first().artwork)
        assertTrue(catalog.find("1152151605810958336")!!.tracks.all { it.artwork == null })
    }
}
