package love.yu.jaychou

import java.io.File
import love.yu.jaychou.data.AlbumRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackArtistsTest {
    private val studio = AlbumRepository.decode(File("src/main/assets/albums.json").readText())
    private val extra = AlbumRepository.decode(File("src/main/assets/extended-albums.json").readText())

    @Test fun soloAndDuetCreditsUseTheActualRelease() {
        val works = studio.find("1551833317039542272")!!
        assertEquals(listOf("周杰伦"), works.tracks[1].artists)
        assertEquals(listOf("周杰伦", "杨瑞代"), works.tracks[8].artists)
        assertEquals(listOf("周杰伦", "阿信"), works.tracks[3].artists)
        val studioHumor = studio.find("1152155845522493440")!!.tracks.first { it.title == "黑色幽默" }
        val liveHumor = extra.find("apple-536637791")!!.tracks[8]
        assertEquals(listOf("周杰伦"), studioHumor.artists)
        assertEquals(listOf("周杰伦", "袁咏琳"), liveHumor.artists)
        assertEquals(listOf("派伟俊", "周杰伦"), extra.find("apple-1789036121")!!.tracks.first().artists)
    }
}
