package love.yu.jaychou.data

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.time.Duration
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
import org.json.JSONArray
import org.json.JSONObject

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
        DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.SIMPLIFIED_CHINESE),
    )

    val timeLabel: String get() = startTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "时间待公布"

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
        if (status == ShowStatus.ENDED) return "已结束"
        if (status == ShowStatus.CANCELLED) return "已取消"
        val days = daysUntil(now)
        return when {
            days <= 0L -> "今天"
            days == 1L -> "明天"
            else -> "还有 $days 天"
        }
    }

    fun ticketLabel(now: ZonedDateTime = ZonedDateTime.now()): String = when (status(now)) {
        ShowStatus.ENDED -> "已结束"
        ShowStatus.CANCELLED -> "已取消"
        else -> when (ticketState) {
            TicketState.ON_SALE -> "售票中"
            TicketState.PENDING -> "待开票"
            TicketState.SOLD_OUT -> "已售罄"
            TicketState.UNKNOWN -> if (status(now) == ShowStatus.TODAY) "今天开演" else "即将举行"
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

/**
 * Loads the versioned concert snapshot from assets and refreshes announced dates from JVR's
 * official monthly schedule. Venue metadata, ticket links and prices remain local so that a
 * network failure never invents ticketing details.
 */
object ConcertRepository {
    const val source = "https://www.jvrmusic.com/activity/list/1150822038412333056"
    private const val officialSchedule = "https://www.jvrmusic.com/activity/next/1150822038412333056"
    private const val assetName = "concerts.json"
    private const val cacheName = "concert-schedule.json"
    private const val maxBytes = 524_288
    private val cacheTtl = Duration.ofHours(12)
    private val cacheMaxAge = Duration.ofDays(7)

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

    private data class OfficialCache(val refreshedAt: ZonedDateTime, val shows: List<OfficialShow>)

    suspend fun load(context: Context): ConcertSnapshot = withContext(Dispatchers.IO) {
        val bundled = context.assets.open(assetName).bufferedReader().use { it.readText() }
        val baseline = decode(bundled)
        val cache = readCache(context) ?: return@withContext baseline
        val age = runCatching { Duration.between(cache.refreshedAt, ZonedDateTime.now()) }.getOrNull()
        if (age == null || age.seconds > cacheMaxAge.seconds || age.seconds < -cacheMaxAge.seconds) {
            baseline
        } else {
            merge(baseline, cache.shows, cache.refreshedAt)
        }
    }

    fun isStale(snapshot: ConcertSnapshot, now: ZonedDateTime = ZonedDateTime.now()): Boolean {
        val refreshedAt = snapshot.refreshedAt ?: return true
        val age = Duration.between(refreshedAt, now).seconds
        return age > cacheTtl.seconds || age < -cacheTtl.seconds
    }

    suspend fun refresh(context: Context, baseline: ConcertSnapshot): ConcertSnapshot = withContext(Dispatchers.IO) {
        val now = ZonedDateTime.now()
        val official = fetchOfficial(now)
        if (official.isEmpty()) throw ConcertDataException("JVR 官方行程暂不可用")
        val merged = merge(baseline, official, now)
        writeCache(context, official, now)
        merged
    }

    fun decode(json: String, refreshedAt: ZonedDateTime? = null): ConcertSnapshot {
        val root = try {
            JSONObject(json)
        } catch (failure: Exception) {
            throw ConcertDataException("演唱会数据无法解析", failure)
        }
        val verifiedAt = runCatching { LocalDate.parse(root.optString("verifiedAt")) }
            .getOrElse { throw ConcertDataException("演唱会数据缺少核对日期", it) }
        val tourName = root.optString("tourName").ifBlank { "周杰伦「嘉年华II」世界巡回演唱会" }
        val sourceUrl = root.optString("source").ifBlank { source }

        val venuesJson = root.optJSONObject("venues") ?: JSONObject()
        val venues = LinkedHashMap<String, ConcertVenue>()
        val keys = venuesJson.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val item = venuesJson.optJSONObject(key) ?: continue
            val city = item.text("city") ?: key
            val zone = runCatching { ZoneId.of(item.text("tz") ?: "Asia/Shanghai") }
                .getOrDefault(ZoneId.of("Asia/Shanghai"))
            venues[city] = ConcertVenue(
                country = item.text("country").orEmpty(),
                city = city,
                name = item.text("venue").orEmpty(),
                address = item.text("address").orEmpty(),
                latitude = item.optDouble("lat").takeUnless { it.isNaN() },
                longitude = item.optDouble("lng").takeUnless { it.isNaN() },
                timeZone = zone,
            )
        }

        val shows = root.optJSONArray("shows") ?: JSONArray()
        val concerts = (0 until shows.length()).mapNotNull { index ->
            val item = shows.optJSONObject(index) ?: return@mapNotNull null
            val city = item.text("city") ?: return@mapNotNull null
            val venue = venues[city] ?: return@mapNotNull null
            val date = runCatching { LocalDate.parse(item.text("date")) }.getOrNull() ?: return@mapNotNull null
            val start = item.text("startTime")?.let { value ->
                runCatching { LocalTime.parse(value) }.getOrNull()
            }
            val sources = item.optJSONArray("sourceUrls")?.let { array ->
                (0 until array.length()).mapNotNull { position ->
                    array.optString(position).takeIf { it.isNotBlank() }
                }
            }.orEmpty()
            Concert(
                id = item.text("id") ?: "$date-${citySlugs[city] ?: city}",
                tourName = tourName,
                date = date,
                startTime = start,
                theme = item.text("theme").orEmpty(),
                venue = venue,
                ticketState = TicketState.from(item.text("ticketState")),
                ticketPlatform = item.text("ticketPlatform"),
                ticketUrl = item.text("ticketUrl"),
                saleStartsAt = item.text("saleStartsAt")?.let {
                    runCatching { OffsetDateTime.parse(it).toZonedDateTime() }.getOrNull()
                },
                priceRange = item.text("priceRange"),
                organizer = item.text("organizer"),
                officialUrl = item.text("officialUrl") ?: sourceUrl,
                sourceUrls = sources.ifEmpty { listOf(sourceUrl) },
                note = item.text("note"),
                updatedAt = verifiedAt,
                cancelled = item.optBoolean("cancelled", false),
            )
        }.distinctBy { it.id }
            .sortedWith(compareBy({ it.date }, { it.startTime ?: LocalTime.MIDNIGHT }))

        return ConcertSnapshot(
            verifiedAt = verifiedAt,
            tourName = tourName,
            source = sourceUrl,
            venues = venues,
            concerts = concerts,
            refreshedAt = refreshedAt,
        )
    }

    internal fun parseOfficialSchedule(html: String): List<OfficialShow> {
        return itemRegex.findAll(html).mapNotNull { match ->
            val block = match.groupValues[1]
            val dateValue = dateValueRegex.find(block)?.groupValues?.get(1) ?: return@mapNotNull null
            val rawTitle = titleRegex.find(block)?.groupValues?.get(1) ?: return@mapNotNull null
            val title = decodeEntities(rawTitle).replace(Regex("\\s+"), " ").trim()
            val suffix = title.substringAfter(" - ", "").trim()
            if (suffix.isEmpty()) return@mapNotNull null
            val parts = suffix.split(Regex("\\s+"))
            if (parts.size < 2) return@mapNotNull null
            val city = cityAliases[parts.last()] ?: return@mapNotNull null
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
        val months = (0..11).map { firstMonth.plusMonths(it.toLong()) }
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
        if (results.all { it == null }) throw ConcertDataException("JVR 官方行程暂不可用")
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
                throw ConcertDataException("JVR 官方行程暂不可用（HTTP ${connection.responseCode}）")
            }
            val body = connection.inputStream.use { it.boundedBytes(maxBytes) }
            if (body.size > maxBytes) throw ConcertDataException("JVR 官方行程返回内容过大")
            return parseOfficialSchedule(body.toString(Charsets.UTF_8))
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

    private fun merge(
        base: ConcertSnapshot,
        official: List<OfficialShow>,
        refreshedAt: ZonedDateTime,
    ): ConcertSnapshot {
        val byKey = LinkedHashMap<String, Concert>()
        base.concerts.forEach { byKey["${it.date}:${it.venue.city}"] = it }
        val cityTemplates = base.concerts.groupBy { it.venue.city }
            .mapValues { (_, shows) -> shows.firstOrNull { it.ticketUrl != null } ?: shows.first() }

        official.forEach { item ->
            val key = "${item.date}:${item.city}"
            val venue = base.venues[item.city] ?: return@forEach
            val existing = byKey[key]
            if (existing != null) {
                byKey[key] = existing.copy(theme = item.theme.ifBlank { existing.theme })
                return@forEach
            }
            val template = cityTemplates[item.city]
            byKey[key] = Concert(
                id = "${item.date}-${citySlugs[item.city] ?: item.city}",
                tourName = base.tourName,
                date = item.date,
                startTime = null,
                theme = item.theme,
                venue = venue,
                ticketState = TicketState.UNKNOWN,
                ticketPlatform = template?.ticketPlatform,
                ticketUrl = template?.ticketUrl,
                saleStartsAt = null,
                priceRange = null,
                organizer = template?.organizer,
                officialUrl = base.source,
                sourceUrls = listOf(base.source).distinct(),
                note = "JVR 官方行程新增；票务信息待公布。",
                updatedAt = base.verifiedAt,
            )
        }

        return base.copy(
            concerts = byKey.values.sortedWith(compareBy({ it.date }, { it.startTime ?: LocalTime.MIDNIGHT })),
            refreshedAt = refreshedAt,
        )
    }

    private fun readCache(context: Context): OfficialCache? {
        val file = File(context.filesDir, cacheName)
        if (!file.isFile) return null
        return runCatching {
            val root = JSONObject(file.readText())
            val refreshedAt = ZonedDateTime.parse(root.getString("refreshedAt"))
            val array = root.optJSONArray("shows") ?: JSONArray()
            val shows = (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val date = runCatching { LocalDate.parse(item.getString("date")) }.getOrNull()
                    ?: return@mapNotNull null
                val city = item.optString("city").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                OfficialShow(date, city, item.optString("theme"))
            }
            OfficialCache(refreshedAt, shows)
        }.getOrNull()
    }

    private fun writeCache(context: Context, shows: List<OfficialShow>, refreshedAt: ZonedDateTime) {
        val array = JSONArray()
        shows.forEach { show ->
            array.put(JSONObject().put("date", show.date.toString()).put("city", show.city).put("theme", show.theme))
        }
        val root = JSONObject()
            .put("refreshedAt", refreshedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            .put("shows", array)
        runCatching { File(context.filesDir, cacheName).writeText(root.toString()) }
    }

    private fun JSONObject.text(key: String): String? {
        if (isNull(key)) return null
        return optString(key).trim().takeUnless { it.isEmpty() || it == "null" }
    }
}
