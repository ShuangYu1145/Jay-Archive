package love.yu.jaychou

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import love.yu.jaychou.data.LyricsRepository
import love.yu.jaychou.ui.ProjectedCornerShape
import org.junit.Assert.*
import org.junit.Test

class MotionAndLyricsTest {
    @Test fun horizontalCardCornersStayCircularAcrossTheMorph() {
        for (q in listOf(0f, .05f, .2f, .5f, .9f, .99f, 1f)) {
            val sx = .9f + .1f * q
            val sy = .09f + .91f * q
            val radius = 23f * (1f - q)
            val outline = ProjectedCornerShape(radius, sx, sy)
                .createOutline(Size(400f, 900f), LayoutDirection.Ltr, Density(1f)) as Outline.Rounded
            val corner = outline.roundRect.topLeftCornerRadius
            assertEquals(radius, corner.x * sx, .001f)
            assertEquals(radius, corner.y * sy, .001f)
        }
    }

    @Test fun lrcTimestampsAndMetadataDoNotPolluteReadingLines() {
        assertEquals(listOf("测试第一行", "测试第二行"), LyricsRepository.lines(
            "\uFEFF[ti:自写测试文本]\n[ar:测试作者]\n[00:01.20][00:04.20]测试第一行\n[00:02]测试第二行"))
    }

    @Test fun lyricsCandidatesExcludeOtherArtistsAndNullText() {
        val json = """[
            {"id":1,"artistName":"Jay Chou","trackName":"测试曲","albumName":"测试盘","plainLyrics":"自写测试文本"},
            {"id":2,"artistName":"Other Artist","trackName":"测试曲","plainLyrics":"其他"},
            {"id":3,"artistName":"周杰倫","plainLyrics":null,"syncedLyrics":null},
            {"id":4,"artistName":"周杰伦","trackName":"测试器乐","albumName":null,"instrumental":true}
        ]"""
        assertEquals(listOf(1L, 4L), LyricsRepository.decodeCandidates(json).map { it.id })
        assertEquals("来源未标注专辑", LyricsRepository.decodeCandidates(json).last().album)
    }
}
