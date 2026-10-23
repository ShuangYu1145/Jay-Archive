package love.yu.jaychou

import love.yu.jaychou.data.Album
import love.yu.jaychou.data.LyricsRepository
import love.yu.jaychou.data.QQTrack
import love.yu.jaychou.data.ReleaseCategory
import love.yu.jaychou.data.Track
import org.junit.Assert.*
import org.junit.Test

class MotionAndLyricsTest {
    @Test fun lrcTimestampsAndMetadataDoNotPolluteReadingLines() {
        assertEquals(listOf("测试第一行", "测试第二行"), LyricsRepository.lines(
            "\uFEFF[ti:自写测试文本]\n[ar:测试作者]\n[00:01.20][00:04.20]测试第一行\n[00:02]测试第二行"))
    }

    @Test fun vkeysLyricPairsTranslationByNearestTimestamp() {
        val json = """{"code":200,"data":{
            "lrc":"[ti:测试曲]\n[ar:周杰伦]\n[00:01.00]自写第一句\n[00:03.00]自写第二句\n[00:05.00]没有翻译",
            "trans":"[00:01.05]First authored line\n[00:03.10]Second authored line",
            "yrc":"","roma":""}}"""
        val lyrics = LyricsRepository.parseLyric(json, qqTrack())
        assertEquals(listOf("自写第一句", "自写第二句", "没有翻译"), lyrics.lines.map { it.text })
        assertEquals("First authored line", lyrics.lines[0].translation)
        assertEquals("Second authored line", lyrics.lines[1].translation)
        assertNull(lyrics.lines[2].translation)
        assertTrue(lyrics.hasTranslation)
    }

    @Test fun vkeysLyricDropsSongTitleAndTechnicalCredits() {
        val json = """{"code":200,"data":{
            "lrc":"[00:00.00]测试曲 - 周杰伦 (Jay Chou)\n[00:00.50]词：测试作者\n[00:01.00]合声编写：测试作者\n[00:02.00]录音工程：测试工作室\n[00:03.00]真正的第一句",
            "trans":"","yrc":"","roma":""}}"""
        val lyrics = LyricsRepository.parseLyric(json, qqTrack())
        assertEquals(listOf("真正的第一句"), lyrics.lines.map { it.text })
    }

    @Test fun vkeysLyricUsesYrcFallbackWhenPlainLrcIsEmpty() {
        val json = """{"code":200,"data":{
            "lrc":"","trans":"","roma":"",
            "yrc":"[ti:测试曲]\n[0,1000]自(0,100)写(100,100)歌词(200,300)\n[1000,1000]第(1000,100)二(1100,100)句(1200,100)"}}"""
        val lyrics = LyricsRepository.parseLyric(json, qqTrack())
        assertEquals(listOf("自写歌词", "第二句"), lyrics.lines.map { it.text })
        assertFalse(lyrics.hasTranslation)
    }

    @Test fun vkeysSearchPrefersJayChouStudioAlbumOverLiveAndCovers() {
        val json = """{"code":200,"data":[
            {"id":1,"mid":"live","song":"晴天 (Live)","singer":"周杰伦","album":"周杰伦地表最强世界巡回演唱会","time":"2019-11-01"},
            {"id":2,"mid":"studio","song":"晴天","singer":"周杰伦","album":"叶惠美","time":"2003-07-31"},
            {"id":3,"mid":"other","song":"晴天","singer":"其他歌手","album":"叶惠美","time":"2003-07-31"},
            {"id":4,"mid":"cover","song":"晴天 (Cover)","singer":"周杰伦","album":"叶惠美","time":"2003-07-31"}
        ]}"""
        val candidates = LyricsRepository.parseSearch(json)
        assertEquals(4, candidates.size)
        assertEquals("studio", LyricsRepository.chooseTrack(album("叶惠美"), Track(1, "晴天"), candidates)?.mid)
    }

    @Test fun vkeysSearchKeepsTheLiveVersionForALiveAlbum() {
        val json = """{"code":200,"data":[
            {"id":1,"mid":"studio","song":"晴天","singer":"周杰伦","album":"叶惠美","time":"2003-07-31"},
            {"id":2,"mid":"live","song":"晴天 (Live)","singer":"周杰伦","album":"周杰伦地表最强世界巡回演唱会","time":"2019-11-01"}
        ]}"""
        val liveAlbum = album("周杰伦地表最强世界巡回演唱会", "2019-11-01", ReleaseCategory.LIVE)
        val candidates = LyricsRepository.parseSearch(json)
        assertEquals("live", LyricsRepository.chooseTrack(liveAlbum, Track(1, "晴天 (Live)"), candidates)?.mid)
    }

    @Test fun searchTitleDropsCreditsButKeepsVersionLabels() {
        assertEquals("说好不哭", LyricsRepository.cleanTitle("说好不哭 with 五月天阿信 Mayday Ashin"))
        assertEquals("圣徒", LyricsRepository.cleanTitle("圣徒（feat. 杨瑞代）"))
        assertEquals("一路向北 (Live)", LyricsRepository.cleanTitle("一路向北 (Live)"))
    }

    @Test fun apiErrorMessagesArePreservedForTheReader() {
        val failure = assertThrows(love.yu.jaychou.data.LyricsUnavailableException::class.java) {
            LyricsRepository.parseLyric("""{"code":404,"message":"歌曲不存在"}""", qqTrack())
        }
        assertEquals("歌曲不存在", failure.message)
    }

    private fun album(
        title: String,
        releaseDate: String = "2003-07-31",
        category: ReleaseCategory = ReleaseCategory.STUDIO,
    ) = Album(
        id = "test-album", title = title, englishTitle = "", releaseDate = releaseDate,
        sourceDateRaw = "", sourceUrl = "", coverUrl = "", coverAsset = "", tracks = emptyList(),
        category = category,
    )

    private fun qqTrack() = QQTrack(
        id = 1, mid = "test-mid", title = "测试曲", singer = "周杰伦",
        album = "测试专辑", releaseDate = "2003-07-31",
    )
}
