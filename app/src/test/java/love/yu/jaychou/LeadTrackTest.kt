package love.yu.jaychou

import java.io.File
import love.yu.jaychou.data.AlbumRepository
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LeadTrackTest {
    private val json = File("src/main/assets/albums.json").readText()

    @Test fun allTenCommonJasminOrangeSongsAreMarkedWithoutChangingNames() {
        val album = AlbumRepository.decode(json).find("1152151605810958336")!!
        assertEquals(10, album.tracks.size)
        assertTrue(album.tracks.all { it.isLeadTrack })
        assertTrue(album.tracks.none { it.title.contains("🌟") })
        assertEquals("七里香", album.tracks[1].title)
        val works = AlbumRepository.decode(json).find("1551833317039542272")!!
        assertFalse("Intro is not a lead track", works.tracks.first().isLeadTrack)
    }

    @Test fun olderCatalogsWithoutMarkersStillDecodeNormally() {
        val root = JSONObject(json)
        val first = root.getJSONArray("albums").getJSONObject(0)
        val originalNames = first.getJSONArray("tracks").toString()
        first.remove("leadTrackNumbers")
        val album = AlbumRepository.decode(root.toString()).find(first.getString("id"))!!
        assertTrue(album.tracks.none { it.isLeadTrack })
        assertEquals(originalNames, first.getJSONArray("tracks").toString())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMarkersOutsideTheOriginalTrackOrder() {
        val root = JSONObject(json)
        root.getJSONArray("albums").getJSONObject(0).getJSONArray("leadTrackNumbers").put(999)
        AlbumRepository.decode(root.toString())
    }
}
