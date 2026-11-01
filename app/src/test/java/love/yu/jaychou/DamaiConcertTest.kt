package love.yu.jaychou

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.net.URLEncoder
import love.yu.jaychou.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DamaiConcertTest {
    private fun page(time: String = "2026.12.04-12.06") = JSONObject("""
        {"itemBase":{"itemId":1077189102025,"itemName":"时光机 广州 嘉年华2026周杰伦世界巡回演唱会",
        "cityName":"广州市","showTime":"$time","itemPic":"//img.alicdn.com/cover.jpg",
        "seatImg":"https://img.alicdn.com/seats.jpg","seatImages":["https://img.alicdn.com/seats.jpg"]},
        "venue":{"venueName":"体育场","venueAddr":"地址"},"itemExtendInfo":{}}
    """)

    @Test fun publicPageExtractsCoverSeatsAndEncodedIntroduction() {
        val json = page()
        json.getJSONObject("itemExtendInfo").put("itemExtend", URLEncoder.encode(
            "<p>演出介绍 &amp; 观演指南</p><img src=\"//img.alicdn.com/intro.jpg\" width=\"1000\" height=\"4000\"><script>bad()</script>", "UTF-8").replace("+", "%20"))
        val result = DamaiConcertRepository.parsePage("<div id=\"staticDataDefault\" style=\"display:none\">$json</div>")
        assertEquals("https://img.alicdn.com/cover.jpg", result.cover)
        assertEquals(1, result.seats.size)
        assertEquals(.25f, result.introductionImages.single().aspectRatio)
        assertEquals("演出介绍 & 观演指南", result.introduction)
        assertEquals(LocalDate.of(2026, 12, 6), result.lastDate)
    }

    @Test fun onlyMatchingCityAndEditionCanUseStationMedia() {
        val result = DamaiConcertRepository.parse(page())
        val now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
        val concert = ConcertRepository.fromOfficial(listOf(OfficialShow(LocalDate.of(2026, 12, 4), "广州", "时光机")), now).concerts.single()
        assertTrue(result.matches(concert))
        assertFalse(result.matches(concert.copy(date = LocalDate.of(2027, 12, 4))))
        assertFalse(result.matches(concert.copy(venue = concert.venue.copy(city = "三亚"))))
        assertFalse(DamaiConcertRepository.parse(page("时间待定")).matches(concert))
    }

    @Test fun untrustedImagesAndVerificationPagesAreRejected() {
        assertNull(DamaiConcertRepository.imageUrl("https://img.alicdn.com.evil.test/a.jpg"))
        assertNull(DamaiConcertRepository.imageUrl("http://img.alicdn.com/a.jpg"))
        assertNull(DamaiConcertRepository.imageUrl("javascript:alert(1)"))
        assertEquals("https://img.alicdn.com/a.jpg", DamaiConcertRepository.imageUrl("https://img.alicdn.com/bao/uploaded/https://img.alicdn.com/a.jpg"))
        assertThrows(IllegalStateException::class.java) { DamaiConcertRepository.parsePage("<html>验证</html>") }
    }
}
