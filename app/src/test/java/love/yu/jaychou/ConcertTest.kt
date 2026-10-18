package love.yu.jaychou

import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import love.yu.jaychou.data.ConcertRepository
import love.yu.jaychou.data.ShowStatus
import love.yu.jaychou.data.TicketState
import org.junit.Assert.*
import org.junit.Test

class ConcertTest {
    private val snapshot = ConcertRepository.decode(
        File("src/main/assets/concerts.json").readText(),
        refreshedAt = null,
    )
    private val shanghai = ZoneId.of("Asia/Shanghai")

    @Test fun bundledScheduleIsOrderedAndComplete() {
        assertEquals(LocalDate.of(2026, 10, 17), snapshot.verifiedAt)
        assertEquals(36, snapshot.concerts.size)
        assertEquals(14, snapshot.venues.size)
        assertEquals(snapshot.concerts.sortedWith(
            compareBy({ it.date }, { it.startTime ?: java.time.LocalTime.MIDNIGHT })), snapshot.concerts)
        assertEquals(LocalDate.of(2026, 4, 3), snapshot.concerts.first().date)
        assertEquals(LocalDate.of(2027, 4, 11), snapshot.concerts.last().date)
        snapshot.concerts.forEach { concert ->
            assertTrue(concert.venue.name.isNotBlank())
            assertTrue(concert.venue.address.isNotBlank())
            assertTrue(concert.sourceUrls.isNotEmpty())
        }
    }

    @Test fun nearestShowSkipsAnAlreadyFinishedMelbourneNight() {
        val now = ZonedDateTime.of(2026, 10, 17, 22, 18, 0, 0, shanghai)
        val featured = snapshot.next(now)
        assertNotNull(featured)
        assertEquals("sydney", featured!!.id.substringAfterLast('-'))
        assertEquals(LocalDate.of(2026, 11, 21), featured.date)
        assertEquals(34L, featured.daysUntil(now))
        assertEquals(ShowStatus.UPCOMING, featured.status(now))
        assertEquals("售票中", featured.ticketLabel(now))
    }

    @Test fun melbourneIsStillTodayBeforeItsLocalStart() {
        val now = ZonedDateTime.of(2026, 10, 17, 10, 0, 0, 0, shanghai)
        val featured = snapshot.next(now)
        assertNotNull(featured)
        assertEquals("melbourne", featured!!.id.substringAfterLast('-'))
        assertEquals(ShowStatus.TODAY, featured.status(now))
        assertEquals("今天", featured.countdown(now))
    }

    @Test fun endedAndOnSaleStatesAreDistinguished() {
        val now = ZonedDateTime.of(2026, 10, 17, 22, 18, 0, 0, shanghai)
        val hangzhou = snapshot.concerts.first { it.date == LocalDate.of(2026, 4, 3) }
        assertEquals(ShowStatus.ENDED, hangzhou.status(now))
        assertEquals("已结束", hangzhou.ticketLabel(now))

        val guangzhou = snapshot.concerts.first { it.venue.city == "广州" }
        assertEquals(TicketState.ON_SALE, guangzhou.ticketState)
        assertEquals("售票中", guangzhou.ticketLabel(now))
        assertEquals("19:00", guangzhou.timeLabel)
        assertNotNull(guangzhou.priceRange)
    }

    @Test fun pendingShowsWithoutAVerifiedTimeStayHonest() {
        val fuzhou = snapshot.concerts.first { it.venue.city == "福州" }
        assertEquals(TicketState.PENDING, fuzhou.ticketState)
        assertEquals("时间待公布", fuzhou.timeLabel)
        assertEquals("待开票", fuzhou.ticketLabel(ZonedDateTime.of(2026, 10, 17, 22, 18, 0, 0, shanghai)))
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
