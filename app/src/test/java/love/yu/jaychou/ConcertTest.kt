package love.yu.jaychou

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import love.yu.jaychou.data.ConcertRepository
import love.yu.jaychou.data.ShowStatus
import love.yu.jaychou.data.TicketState
import org.junit.Assert.*
import org.junit.Test

class ConcertTest {
    private val shanghai = ZoneId.of("Asia/Shanghai")

    @Test fun networkSnapshotDoesNotInventVenueOrTicketDetails() {
        val now = ZonedDateTime.of(2026, 10, 23, 12, 0, 0, 0, shanghai)
        val snapshot = ConcertRepository.fromOfficial(listOf(
            love.yu.jaychou.data.OfficialShow(LocalDate.of(2026, 11, 21), "悉尼", "海洋"),
            love.yu.jaychou.data.OfficialShow(LocalDate.of(2026, 10, 17), "墨尔本", "粉色"),
            love.yu.jaychou.data.OfficialShow(LocalDate.of(2026, 11, 21), "悉尼", "海洋"),
        ), now)
        assertEquals(2, snapshot.concerts.size)
        assertEquals(now, snapshot.refreshedAt)
        assertEquals("悉尼", snapshot.next(now)!!.venue.city)
        snapshot.concerts.forEach {
            assertNull(it.ticketUrl)
            assertNull(it.startTime)
            assertNull(it.priceRange)
            assertEquals(TicketState.UNKNOWN, it.ticketState)
            assertEquals("场馆请查阅官方公告", it.venue.name)
        }
        val melbourne = snapshot.concerts.first()
        assertEquals("Australia/Melbourne", melbourne.venue.timeZone.id)
        assertEquals(ShowStatus.ENDED, melbourne.status(now))
    }

    @Test fun emptyOfficialScheduleIsAValidEmptyState() {
        val snapshot = ConcertRepository.fromOfficial(emptyList(), ZonedDateTime.now(shanghai))
        assertTrue(snapshot.concerts.isEmpty())
        assertNull(snapshot.next())
    }

    @Test fun newCitiesAreNotDroppedAndOtherEventsAreExcluded() {
        val html = """
            <div class="item"><p data-value="2027-05-01T16:00:00+00:00"></p><div class="group">
            <p class="title concert">周杰倫嘉年華II世界巡迴演唱會 - 晴天 新城市</p></div></div>
            <div class="item"><p data-value="2027-05-01T16:00:00+00:00"></p><div class="group">
            <p class="title event">品牌活動 - 見面 北京</p></div></div>
        """.trimIndent()
        val shows = ConcertRepository.parseOfficialSchedule(html)
        assertEquals(1, shows.size)
        assertEquals("新城市", shows.single().city)
    }

    @Test fun officialMonthlyMarkupIsParsedIntoCanonicalDatesAndCities() {
        val html = """
            <div class="item">
              <p class="date"><span class="pub-date" data-value="2026-12-03T16:00:00.0000000+00:00"></span></p>
              <div class="group">
                <p class="title concert">&#x5468;&#x6770;&#x502B;&#x5609;&#x5E74;&#x83EF;II&#x4E16;&#x754C;&#x5DE1;&#x8FF4;&#x6F14;&#x5531;&#x6703; - &#x6642;&#x5149;&#x6A5F; &#x5EE3;&#x5DDE;</p>
              </div>
            </div>
        """.trimIndent()
        val shows = ConcertRepository.parseOfficialSchedule(html)
        assertEquals(1, shows.size)
        assertEquals(LocalDate.of(2026, 12, 4), shows.single().date)
        assertEquals("广州", shows.single().city)
        assertEquals("时光机", shows.single().theme)
    }

    @Test fun cityAliasesAndEntitiesAreNormalized() {
        assertEquals("周杰倫嘉年華II世界巡迴演唱會 - 時光機 廣州",
            ConcertRepository.decodeEntities("&#x5468;&#x6770;&#x502B;&#x5609;&#x5E74;&#x83EF;II&#x4E16;&#x754C;&#x5DE1;&#x8FF4;&#x6F14;&#x5531;&#x6703; - &#x6642;&#x5149;&#x6A5F; &#x5EE3;&#x5DDE;"))
        assertEquals(TicketState.PENDING, TicketState.from("pending"))
        assertEquals(TicketState.UNKNOWN, TicketState.from("售罄"))
    }
}
