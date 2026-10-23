package love.yu.jaychou.data

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

enum class TicketState(val raw: String) {
    ON_SALE("on_sale"), PENDING("pending"), SOLD_OUT("sold_out"), UNKNOWN("unknown");

    companion object {
        fun from(raw: String?): TicketState = entries.firstOrNull { it.raw == raw?.lowercase() } ?: UNKNOWN
    }
}

enum class ShowStatus { UPCOMING, TODAY, ENDED, CANCELLED }

data class ConcertVenue(
    val country: String,
    val city: String,
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val timeZone: ZoneId,
)

data class Concert(
    val id: String,
    val tourName: String,
    val date: LocalDate,
    val startTime: LocalTime?,
    val theme: String,
    val venue: ConcertVenue,
    val ticketState: TicketState,
    val ticketPlatform: String?,
    val ticketUrl: String?,
    val saleStartsAt: ZonedDateTime?,
    val priceRange: String?,
    val organizer: String?,
    val officialUrl: String?,
    val sourceUrls: List<String>,
    val note: String?,
    val updatedAt: LocalDate,
    val cancelled: Boolean = false,
) {
    val startAt: ZonedDateTime? get() = startTime?.let { ZonedDateTime.of(date, it, venue.timeZone) }

    val title: String get() = if (theme.isBlank()) tourName else "$tourName · $theme"

    val dateLabel: String get() = date.format(
        DateTimeFormatter.ofPattern(AppText.fromOriginal("yyyy年M月d日 EEEE"), AppText.locale),
    )

    val timeLabel: String get() = startTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: AppText.fromOriginal("时间待公布")

    val zoneLabel: String get() = "${venue.timeZone.id} · ${date.atStartOfDay(venue.timeZone).offset.id}"

    val updatedLabel: String get() = updatedAt.toString()

    fun status(now: ZonedDateTime = ZonedDateTime.now()): ShowStatus {
        if (cancelled) return ShowStatus.CANCELLED
        val localNow = now.withZoneSameInstant(venue.timeZone)
        val today = localNow.toLocalDate()
        if (date < today) return ShowStatus.ENDED
        if (date > today) return ShowStatus.UPCOMING
        val end = startAt?.plusHours(4)
        return if (end != null && !localNow.isBefore(end)) ShowStatus.ENDED else ShowStatus.TODAY
    }

    fun daysUntil(now: ZonedDateTime = ZonedDateTime.now()): Long {
        val today = now.withZoneSameInstant(venue.timeZone).toLocalDate()
        return ChronoUnit.DAYS.between(today, date)
    }

    fun countdown(now: ZonedDateTime = ZonedDateTime.now()): String {
        val status = status(now)
        if (status == ShowStatus.ENDED) return AppText.fromOriginal("已结束")
        if (status == ShowStatus.CANCELLED) return AppText.fromOriginal("已取消")
        val days = daysUntil(now)
        return when {
            days <= 0L -> AppText.fromOriginal("今天")
            days == 1L -> AppText.fromOriginal("明天")
            else -> AppText.fromOriginal("还有 %1\$s 天", days)
        }
    }

    fun ticketLabel(now: ZonedDateTime = ZonedDateTime.now()): String = when (status(now)) {
        ShowStatus.ENDED -> AppText.fromOriginal("已结束")
        ShowStatus.CANCELLED -> AppText.fromOriginal("已取消")
        else -> when (ticketState) {
            TicketState.ON_SALE -> AppText.fromOriginal("售票中")
            TicketState.PENDING -> AppText.fromOriginal("待开票")
            TicketState.SOLD_OUT -> AppText.fromOriginal("已售罄")
            TicketState.UNKNOWN -> if (status(now) == ShowStatus.TODAY) AppText.fromOriginal("今天开演") else AppText.fromOriginal("即将举行")
        }
    }
}

data class ConcertSnapshot(
    val verifiedAt: LocalDate,
    val tourName: String,
    val source: String,
    val venues: Map<String, ConcertVenue>,
    val concerts: List<Concert>,
    val refreshedAt: ZonedDateTime? = null,
) {
    val updatedLabel: String
        get() = refreshedAt?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) ?: verifiedAt.toString()

    val countries: List<String>
        get() = concerts.map { it.venue.country }.filter { it.isNotBlank() }.distinct().sorted()

    fun cities(country: String?): List<String> = concerts
        .filter { country == null || it.venue.country == country }
        .map { it.venue.city }.distinct().sorted()

    fun next(now: ZonedDateTime = ZonedDateTime.now()): Concert? = concerts
        .asSequence()
        .filter { it.status(now) == ShowStatus.UPCOMING || it.status(now) == ShowStatus.TODAY }
        .minWithOrNull(compareBy({ it.date }, { it.startTime ?: LocalTime.MIDNIGHT }))
}

class ConcertDataException(message: String, cause: Throwable? = null) : IOException(message, cause)

internal data class OfficialShow(val date: LocalDate, val city: String, val theme: String)

/** Reads announced shows directly from the official monthly schedule. */
object ConcertRepository {
    const val source = "https://www.jvrmusic.com/activity/list/1150822038412333056"
    private const val officialSchedule = "https://www.jvrmusic.com/activity/next/1150822038412333056"
    private const val maxBytes = 524_288

    private val itemRegex = Regex("<div class=\"item\">([\\s\\S]*?)</div>\\s*</div>")
    private val dateValueRegex = Regex("data-value=\"([^\"]+)\"")
    private val titleRegex = Regex("class=\"title[^\"]*\">([\\s\\S]*?)</p>")
    private val hexEntity = Regex("&#x([0-9a-fA-F]+);")
    private val decimalEntity = Regex("&#(\\d+);")

    private val cityAliases = mapOf(
        "杭州" to "杭州", "南寧" to "南宁", "南宁" to "南宁", "溫州" to "温州", "温州" to "温州",
        "北京" to "北京", "南京" to "南京", "青島" to "青岛", "青岛" to "青岛",
        "墨爾本" to "墨尔本", "墨尔本" to "墨尔本", "雪梨" to "悉尼", "悉尼" to "悉尼",
        "廣州" to "广州", "广州" to "广州", "三亞" to "三亚", "三亚" to "三亚",
        "新加坡" to "新加坡", "馬來西亞" to "吉隆坡", "吉隆坡" to "吉隆坡",
        "福州" to "福州", "武漢" to "武汉", "武汉" to "武汉",
    )
    private val themeAliases = mapOf(
        "煙花" to "烟花", "甜甜的" to "甜甜的", "蝸牛" to "蜗牛", "龍拳" to "龙拳",
        "愛在" to "爱在", "愛情海" to "爱情海", "粉色" to "粉色", "海洋" to "海洋",
        "時光機" to "时光机", "陽光" to "阳光", "晴天" to "晴天", "稻香" to "稻香",
        "幸福" to "幸福", "蘭亭序" to "兰亭序",
    )
    private val citySlugs = mapOf(
        "杭州" to "hangzhou", "南宁" to "nanning", "温州" to "wenzhou", "北京" to "beijing",
        "南京" to "nanjing", "青岛" to "qingdao", "墨尔本" to "melbourne", "悉尼" to "sydney",
        "广州" to "guangzhou", "三亚" to "sanya", "新加坡" to "singapore",
        "吉隆坡" to "kualalumpur", "福州" to "fuzhou", "武汉" to "wuhan",
    )

    suspend fun load(): ConcertSnapshot = withContext(Dispatchers.IO) {
        val now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
        fromOfficial(fetchOfficial(now), now)
    }

    internal fun fromOfficial(shows: List<OfficialShow>, now: ZonedDateTime): ConcertSnapshot {
        val concerts = shows.distinctBy { "${it.date}:${it.city}" }.map { show ->
            val (country, zone) = when (show.city) {
                "墨尔本" -> "澳大利亚" to "Australia/Melbourne"
                "悉尼" -> "澳大利亚" to "Australia/Sydney"
                "新加坡" -> "新加坡" to "Asia/Singapore"
                "吉隆坡" -> "马来西亚" to "Asia/Kuala_Lumpur"
                else -> (if (show.city in cityAliases.values) "中国大陆" else "地区待公布") to "Asia/Shanghai"
            }
            Concert(
                id = "${show.date}-${citySlugs[show.city] ?: show.city}",
                tourName = "周杰伦「嘉年华II」世界巡回演唱会",
                date = show.date, startTime = null, theme = show.theme,
                venue = ConcertVenue(country, show.city, "场馆请查阅官方公告", "", null, null, ZoneId.of(zone)),
                ticketState = TicketState.UNKNOWN, ticketPlatform = null, ticketUrl = null,
                saleStartsAt = null, priceRange = null, organizer = null,
                officialUrl = source, sourceUrls = listOf(source), note = null,
                updatedAt = now.toLocalDate(),
            )
        }.sortedBy { it.date }
        return ConcertSnapshot(now.toLocalDate(), "周杰伦「嘉年华II」世界巡回演唱会",
            source, concerts.associate { it.venue.city to it.venue }, concerts, now)
    }

    internal fun parseOfficialSchedule(html: String): List<OfficialShow> {
        return itemRegex.findAll(html).mapNotNull { match ->
            val block = match.groupValues[1]
            val dateValue = dateValueRegex.find(block)?.groupValues?.get(1) ?: return@mapNotNull null
            val rawTitle = titleRegex.find(block)?.groupValues?.get(1) ?: return@mapNotNull null
            val title = decodeEntities(rawTitle).replace(Regex("\\s+"), " ").trim()
            if (!title.contains("演唱會") && !title.contains("演唱会")) return@mapNotNull null
            val suffix = title.substringAfter(" - ", "").trim()
            if (suffix.isEmpty()) return@mapNotNull null
            val parts = suffix.split(Regex("\\s+"))
            if (parts.size < 2) return@mapNotNull null
            val city = cityAliases[parts.last()] ?: parts.last()
            val themeRaw = parts.dropLast(1).joinToString(" ")
            val theme = themeAliases[themeRaw] ?: themeRaw
            val date = runCatching {
                OffsetDateTime.parse(dateValue)
                    .atZoneSameInstant(ZoneOffset.ofHours(8))
                    .toLocalDate()
            }.getOrNull() ?: return@mapNotNull null
            OfficialShow(date, city, theme)
        }.distinctBy { "${it.date}:${it.city}" }
            .sortedBy { it.date }
            .toList()
    }

    internal fun decodeEntities(value: String): String {
        var text = value
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
        text = hexEntity.replace(text) { match ->
            match.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: match.value
        }
        return decimalEntity.replace(text) { match ->
            match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value
        }
    }

    private suspend fun fetchOfficial(now: ZonedDateTime): List<OfficialShow> = coroutineScope {
        val firstMonth = now.withZoneSameInstant(ZoneId.of("Asia/Shanghai"))
            .toLocalDate().withDayOfMonth(1)
        val months = (-6..11).map { firstMonth.plusMonths(it.toLong()) }
        val gate = Semaphore(4)
        val results = months.map { month ->
            async {
                gate.withPermit {
                    try {
                        fetchMonth(month)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }.awaitAll()
        if (results.any { it == null }) throw ConcertDataException(AppText.fromOriginal("JVR 官方行程暂不可用"))
        results.filterNotNull().flatten()
            .distinctBy { "${it.date}:${it.city}" }
            .sortedBy { it.date }
    }

    private fun fetchMonth(month: LocalDate): List<OfficialShow> {
        val url = "$officialSchedule/${month.year}/${month.monthValue}"
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("User-Agent", "JayArchive/0.2 (Android; personal music archive)")
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.outputStream.use { }
            if (connection.responseCode !in 200..299) {
                throw ConcertDataException(AppText.fromOriginal("JVR 官方行程暂不可用（HTTP %1\$s）", connection.responseCode))
            }
            val body = connection.inputStream.use { it.boundedBytes(maxBytes) }
            if (body.size > maxBytes) throw ConcertDataException(AppText.fromOriginal("JVR 官方行程返回内容过大"))
            val html = body.toString(Charsets.UTF_8)
            if (!html.contains("calendar-content")) {
                throw ConcertDataException(AppText.fromOriginal("官方行程格式已变化，请稍后重试"))
            }
            return parseOfficialSchedule(html)
        } finally {
            connection.disconnect()
        }
    }

    private fun InputStream.boundedBytes(limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (out.size() <= limit) {
            val count = read(buffer, 0, minOf(buffer.size, limit + 1 - out.size()))
            if (count < 0) break
            out.write(buffer, 0, count)
        }
        return out.toByteArray()
    }

}
