package love.yu.jaychou.data

import java.net.HttpURLConnection
import java.net.URI
import java.net.URLDecoder
import java.time.LocalDate
import kotlinx.coroutines.*
import org.json.JSONObject

data class ConcertImage(val url: String, val aspectRatio: Float = 1f)
data class DamaiConcert(
    val itemId: String,
    val city: String,
    val firstDate: LocalDate?,
    val lastDate: LocalDate?,
    val cover: String?,
    val seats: List<ConcertImage>,
    val introduction: String,
    val introductionImages: List<ConcertImage>,
    val venue: String,
    val address: String,
) {
    val sourceUrl get() = "https://detail.damai.cn/item.htm?id=$itemId"
    fun matches(concert: Concert): Boolean = city.removeSuffix("市") == concert.venue.city &&
        firstDate != null && lastDate != null && concert.date >= firstDate && concert.date <= lastDate
}

/** Public page data only. Missing dates never borrow artwork from another edition of a tour. */
object DamaiConcertRepository {
    const val searchUrl = "https://search.damai.cn/search.html?keyword=%E5%91%A8%E6%9D%B0%E4%BC%A6"
    // Verified tour entry; its tourProjects supplies current stations even if search requires verification.
    private const val tourEntry = "1021197513737"
    private val staticData = Regex("<div\\s+id=[\"']staticDataDefault[\"'][^>]*>([\\s\\S]*?)</div>")
    private val dateRange = Regex("(20\\d{2})[.年/-](\\d{1,2})[.月/-](\\d{1,2})(?:日)?(?:\\s*[-~至]\\s*(?:(20\\d{2})[.年/-])?(\\d{1,2})[.月/-](\\d{1,2}))?")

    suspend fun load(): List<DamaiConcert> = withContext(Dispatchers.IO) {
        val root = optional { fetch(tourEntry) }
        val ids = linkedSetOf(tourEntry)
        // Search can be unavailable behind a verification page; the public tour graph remains useful.
        val searched = optional {
            val json = JSONObject(read("https://search.damai.cn/searchajax.html?keyword=%E5%91%A8%E6%9D%B0%E4%BC%A6&currPage=1&pageSize=30"))
            val results = json.optJSONObject("pageData")?.optJSONArray("resultData")
            if (results != null) for (i in 0 until results.length()) {
                val row = results.optJSONObject(i) ?: continue
                if (row.optString("name").contains("周杰伦") && row.optString("name").contains("演唱会"))
                    ids.add(row.optString("projectid", row.optString("id")))
            }
            results != null
        }
        check(root != null || searched == true) { "Damai details unavailable" }
        val tours = root?.optJSONArray("tourProjects")
        if (tours != null) for (i in 0 until tours.length()) ids.add(tours.getJSONObject(i).optString("itemId"))
        val pages = ids.filter { it.matches(Regex("\\d{5,20}")) }.take(30).chunked(4).flatMap { batch ->
            coroutineScope { batch.map { id -> async { optional { parse(if (id == tourEntry && root != null) root else fetch(id)) } } }.awaitAll().filterNotNull() }
        }
        pages
    }

    internal fun parsePage(html: String): DamaiConcert = parse(JSONObject(
        staticData.find(html)?.groupValues?.get(1) ?: error("Damai page data unavailable"),
    ))

    private fun fetch(id: String): JSONObject {
        val html = read("https://detail.damai.cn/item.htm?id=$id")
        return JSONObject(staticData.find(html)?.groupValues?.get(1) ?: error("Damai verification or missing data"))
    }

    internal fun parse(json: JSONObject): DamaiConcert {
        val base = json.getJSONObject("itemBase")
        val title = base.optString("itemName")
        require(title.contains("周杰伦") && title.contains("演唱会"))
        val id = base.get("itemId").toString()
        require(id.matches(Regex("\\d{5,20}")))
        val dates = dateRange.find(base.optString("showTime"))?.groupValues
        val first = dates?.let { date(it[1], it[2], it[3]) }
        val last = if (dates == null) null else if (dates[5].isBlank()) first
            else date(dates[4].ifBlank { dates[1] }, dates[5], dates[6])
        val seats = mutableListOf<ConcertImage>()
        imageUrl(base.optString("seatImg"))?.let { seats.add(ConcertImage(it)) }
        base.optJSONArray("seatImages")?.let { array ->
            for (i in 0 until array.length()) imageUrl(array.optString(i))?.let { seats.add(ConcertImage(it)) }
        }
        val encoded = json.optJSONObject("itemExtendInfo")?.optString("itemExtend").orEmpty()
        val html = if (encoded.trimStart().startsWith("<")) encoded else
            URLDecoder.decode(encoded.replace("+", "%2B"), "UTF-8")
        val images = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE).findAll(html).mapNotNull { tag ->
            fun attr(name: String) = Regex("\\b$name\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
                .find(tag.value)?.groupValues?.get(1)
            val url = imageUrl(attr("src").orEmpty()) ?: return@mapNotNull null
            val width = attr("width")?.toFloatOrNull() ?: 1f
            val height = attr("height")?.toFloatOrNull() ?: width
            ConcertImage(url, (width / height.coerceAtLeast(1f)).coerceIn(.08f, 5f))
        }.distinctBy { it.url }.take(40).toList()
        val text = ConcertRepository.decodeEntities(html
            .replace(Regex("<(script|style)\\b[^>]*>[\\s\\S]*?</\\1>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<(?:br\\s*/?|/p|/div)>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")).replace(Regex("[ \\t]+"), " ").trim().take(24_000)
        val venue = json.optJSONObject("venue")
        return DamaiConcert(id, base.optString("cityName"), first, last,
            imageUrl(base.optJSONObject("itemPics")?.optJSONArray("itemPicList")?.optJSONObject(0)?.optString("picUrl")
                ?: base.optString("itemPic")), seats.distinctBy { it.url }, text, images,
            venue?.optString("venueName").orEmpty(), venue?.optString("venueAddr").orEmpty())
    }

    internal fun imageUrl(raw: String): String? {
        val value = ConcertRepository.decodeEntities(raw).let { if (it.startsWith("//")) "https:$it" else it }
            .replace("https://img.alicdn.com/bao/uploaded/https://", "https://")
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        return value.takeIf { uri.scheme == "https" && uri.host?.let { host -> host == "alicdn.com" || host.endsWith(".alicdn.com") } == true && uri.userInfo == null }
    }

    private fun date(y: String, m: String, d: String) = runCatching { LocalDate.of(y.toInt(), m.toInt(), d.toInt()) }.getOrNull()
    private inline fun <T> optional(block: () -> T): T? = try { block() }
        catch (e: CancellationException) { throw e } catch (_: Exception) { null }

    private fun read(url: String): String {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 6_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) JayArchive/0.6")
            connection.setRequestProperty("Referer", searchUrl)
            require(connection.responseCode in 200..299)
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (output.size() <= 2_000_000) {
                    val count = input.read(buffer, 0, minOf(buffer.size, 2_000_001 - output.size()))
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            require(bytes.size <= 2_000_000)
            return bytes.toString(Charsets.UTF_8)
        } finally { connection.disconnect() }
    }
}
