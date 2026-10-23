package love.yu.jaychou.data

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class QQTrack(
    val id: Long,
    val mid: String,
    val title: String,
    val singer: String,
    val album: String,
    val releaseDate: String,
)

data class LyricLine(val timeMs: Long?, val text: String, val translation: String? = null)

data class OnlineLyrics(
    val mid: String,
    val title: String,
    val singer: String,
    val album: String,
    val releaseDate: String,
    val lines: List<LyricLine>,
    val hasTranslation: Boolean,
)

class LyricsUnavailableException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Reads local LRC/TXT files and resolves online lyrics through the VKeys QQ Music API.
 *
 * Online lookup is a two-step request: search QQ Music for the Jay Chou version, then request the
 * lyric sheet for the matched song mid. No lyric text is ever written to Logcat.
 */
object LyricsRepository {
    const val source =
        "https://doc.vkeys.cn/v2/%E9%9F%B3%E4%B9%90%E6%A8%A1%E5%9D%97/QQ%E9%9F%B3%E4%B9%90/8-lyric.html"
    private const val searchEndpoint = "https://api.vkeys.cn/v2/music/tencent/search/song"
    private const val lyricEndpoint = "https://api.vkeys.cn/v2/music/tencent/lyric"
    private const val artist = "周杰伦"
    private const val maxBytes = 1_048_576

    private val metadataTag = Regex(
        "^\\[(ar|ti|al|by|offset|length|re|ve|kana):.*]$",
        RegexOption.IGNORE_CASE,
    )
    private val timeTag = Regex("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]")
    private val yrcTag = Regex("\\[(\\d+),(\\d+)]")
    private val yrcCharacterTag = Regex("\\(\\d+,\\d+\\)")
    private val leadingNumber = Regex("^\\d+\\s*[.、]\\s*")
    private val creditBrackets = Regex(
        "[（(]\\s*(?:feat\\.?|with|ft\\.?)[^)）]*[)）]",
        RegexOption.IGNORE_CASE,
    )
    private val trailingCredit = Regex(
        "\\s+(?:feat\\.?|with|ft\\.?)\\s+.*$",
        RegexOption.IGNORE_CASE,
    )
    private val technicalCredit = Regex(
        "^(?:词|曲|编曲|制作人|合声|和声|和音|吉他|贝斯|鼓|录音|混音|母带|监制|出品|发行|弦乐|钢琴|" +
            "program|mixing|producer|composer|lyrics|arranger|mastering)[^:：]{0,8}\\s*[:：]",
        RegexOption.IGNORE_CASE,
    )
    private val liveMark = Regex(
        "(?:^|[\\s(（\\[])live(?:$|[\\s)）\\]])",
        RegexOption.IGNORE_CASE,
    )
    private val unwantedMarks = listOf(
        "伴奏", "纯音乐", "铃声", "片段", "demo", "remix", "cover", "翻唱",
        "karaoke", "dj版", "dj 版", "加速", "降调",
    )

    private val cache = object : LinkedHashMap<String, OnlineLyrics>(16, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, OnlineLyrics>) = size > 12
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

    private fun file(context: Context, album: Album, track: Track): File {
        val key = "${album.id}:${track.number}".toByteArray()
        val hash = MessageDigest.getInstance("SHA-256").digest(key).joinToString("") { "%02x".format(it) }
        return File(File(context.filesDir, "lyrics"), "$hash.txt")
    }

    suspend fun local(context: Context, album: Album, track: Track): String? = withContext(Dispatchers.IO) {
        file(context, album, track).takeIf { it.isFile }?.readText()
    }

    suspend fun removeLocal(context: Context, album: Album, track: Track): Unit = withContext(Dispatchers.IO) {
        val target = file(context, album, track)
        if (target.exists() && !target.delete()) throw IOException(AppText.fromOriginal("无法删除本地歌词"))
    }

    suspend fun import(context: Context, album: Album, track: Track, uri: Uri): String = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.boundedBytes(maxBytes) }
            ?: throw IOException(AppText.fromOriginal("无法读取所选文件"))
        require(bytes.size <= maxBytes) { AppText.fromOriginal("歌词文件不能超过 1 MB") }
        val text = try {
            Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: CharacterCodingException) {
            bytes.toString(charset("GB18030"))
        }
        require(text.isNotBlank() && !text.contains('\u0000')) { AppText.fromOriginal("请选择 UTF-8 或 GB18030 编码的 LRC/TXT 文本") }
        file(context, album, track).also { it.parentFile!!.mkdirs() }.writeText(text)
        text
    }

    /** Searches QQ Music, then loads lyric, translation and timed text for the best matching version. */
    suspend fun fetch(album: Album, track: Track): OnlineLyrics = withContext(Dispatchers.IO) {
        val key = "${album.id}:${track.number}"
        synchronized(cache) { cache[key] }?.let { return@withContext it }

        val title = cleanTitle(track.originalTitle)
        val precise = searchTracks(readJson(searchUrl("$artist $title ${album.originalTitle}")))
        val matched = chooseTrack(album, track, precise) ?: run {
            val broad = searchTracks(readJson(searchUrl("$artist $title")))
            chooseTrack(album, track, broad)
        } ?: throw LyricsUnavailableException(AppText.fromOriginal("QQ 音乐中没有找到与《%1\$s》匹配的周杰伦版本。", track.originalTitle))

        val lyrics = parseLyric(readJson("$lyricEndpoint?mid=${encode(matched.mid)}"), matched)
        synchronized(cache) { cache[key] = lyrics }
        lyrics
    }

    private fun searchUrl(word: String) = "$searchEndpoint?word=${encode(word)}&num=30"

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun readJson(url: String): JSONObject {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 12_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "JayArchive/0.2 (Android; personal music archive)")
            connection.setRequestProperty("Accept", "application/json")
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { it.boundedBytes(maxBytes) } ?: ByteArray(0)
            if (body.size > maxBytes) throw LyricsUnavailableException(AppText.fromOriginal("歌词服务返回内容过大"))
            val text = body.toString(Charsets.UTF_8)
            if (status !in 200..299) {
                val message = runCatching { JSONObject(text).optString("message") }.getOrNull()
                throw LyricsUnavailableException(
                    message?.takeIf { it.isNotBlank() } ?: "歌词服务暂不可用（HTTP $status）",
                )
            }
            return try {
                JSONObject(text)
            } catch (failure: JSONException) {
                throw LyricsUnavailableException(AppText.fromOriginal("歌词服务返回了无法解析的数据"), failure)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.requireSuccess(): JSONObject {
        if (optInt("code", -1) != 200) {
            val message = optString("message").takeUnless { it == "null" }.orEmpty()
            throw LyricsUnavailableException(message.ifBlank { AppText.fromOriginal("歌词服务暂不可用") })
        }
        return this
    }

    fun parseSearch(json: String): List<QQTrack> = searchTracks(JSONObject(json))

    private fun searchTracks(root: JSONObject): List<QQTrack> {
        val data = root.requireSuccess().optJSONArray("data") ?: JSONArray()
        return (0 until data.length()).mapNotNull { index ->
            val item = data.optJSONObject(index) ?: return@mapNotNull null
            val mid = item.optString("mid").trim()
            if (mid.isEmpty()) return@mapNotNull null
            QQTrack(
                id = item.optLong("id"),
                mid = mid,
                title = item.optString("song").trim(),
                singer = item.optString("singer").trim(),
                album = item.optString("album").trim(),
                releaseDate = item.optString("time").trim(),
            )
        }
    }

    /** Ranks candidates by title, artist, album and live/studio agreement; search order breaks ties. */
    fun chooseTrack(album: Album, track: Track, candidates: List<QQTrack>): QQTrack? =
        candidates.withIndex()
            .mapNotNull { (index, candidate) ->
                val score = scoreCandidate(album, track, candidate, index)
                if (score >= 0) candidate to score else null
            }
            .maxByOrNull { it.second }
            ?.first

    private fun scoreCandidate(album: Album, track: Track, candidate: QQTrack, index: Int): Int {
        val wantedTitle = comparable(cleanTitle(track.originalTitle))
        val offeredTitle = comparable(cleanTitle(candidate.title))
        if (wantedTitle.isEmpty() || offeredTitle.isEmpty()) return -1
        val titleScore = when {
            offeredTitle == wantedTitle -> 16
            offeredTitle.startsWith(wantedTitle) || wantedTitle.startsWith(offeredTitle) -> 9
            offeredTitle.contains(wantedTitle) || wantedTitle.contains(offeredTitle) -> 5
            else -> return -1
        }

        val singer = comparable(candidate.singer)
        if (listOf("周杰伦", "周杰倫", "jaychou").none { it in singer }) return -1

        var score = titleScore + 8
        val wantedAlbum = comparable(album.originalTitle)
        val offeredAlbum = comparable(candidate.album)
        when {
            offeredAlbum.isNotEmpty() && offeredAlbum == wantedAlbum -> score += 12
            offeredAlbum.isNotEmpty() &&
                (offeredAlbum.contains(wantedAlbum) || wantedAlbum.contains(offeredAlbum)) -> score += 6
        }

        val wantedLive = album.category == ReleaseCategory.LIVE || isLiveText(track.originalTitle)
        val offeredLive = isLiveText(candidate.title) || isLiveText(candidate.album)
        if (wantedLive == offeredLive) {
            score += 4
        } else {
            score -= if (offeredLive) 8 else 2
        }

        val wantedYear = album.releaseDate.take(4).toIntOrNull()
        val offeredYear = candidate.releaseDate.take(4).toIntOrNull()
        if (wantedYear != null && offeredYear != null) {
            if (wantedYear == offeredYear) score += 3
            else if (kotlin.math.abs(wantedYear - offeredYear) <= 1) score += 1
        }

        val wantedMarked = hasUnwantedMark("${track.originalTitle}${album.originalTitle}")
        if (hasUnwantedMark("${candidate.title}${candidate.album}") && !wantedMarked) score -= 15

        if (index < 10) score += 5 - index / 3
        return score
    }

    /** Returns the plain lyric lines used by both online and imported text. */
    fun lines(text: String): List<String> = text.removePrefix("\uFEFF").lineSequence()
        .filterNot { metadataTag.matches(it.trim()) }
        .map { it.replace(timeTag, "").trim() }
        .toList()

    fun parseLyric(json: String, matched: QQTrack): OnlineLyrics = parseLyric(JSONObject(json), matched)

    private fun parseLyric(root: JSONObject, matched: QQTrack): OnlineLyrics {
        val data = root.requireSuccess().optJSONObject("data")
            ?: throw LyricsUnavailableException(AppText.fromOriginal("QQ 音乐没有返回歌词内容"))
        val lrc = data.plainText("lrc")
        val translation = data.plainText("trans")
        val yrc = data.plainText("yrc")

        val original = timedLines(lrc).ifEmpty { yrcLines(yrc) }
            .filterNot { technicalCredit.containsMatchIn(it.second) }
            .filterNot { isTitleLine(it.second, matched) }
        if (original.isEmpty()) throw LyricsUnavailableException(AppText.fromOriginal("QQ 音乐没有收录这首歌的歌词。"))

        val translated = timedLines(translation)
            .filterNot { (_, text) ->
                text == "//" || text == "/" || technicalCredit.containsMatchIn(text) ||
                    isTitleLine(text, matched)
            }
        val lines = pairTranslations(original, translated)
        return OnlineLyrics(
            mid = matched.mid,
            title = matched.title,
            singer = matched.singer,
            album = matched.album,
            releaseDate = matched.releaseDate,
            lines = lines,
            hasTranslation = lines.any { it.translation != null },
        )
    }

    private fun JSONObject.plainText(key: String): String {
        if (isNull(key)) return ""
        return optString(key).trim().takeUnless { it == "null" }.orEmpty()
    }

    private fun timedLines(text: String): List<Pair<Long, String>> {
        if (text.isBlank()) return emptyList()
        val result = ArrayList<Pair<Long, String>>()
        text.removePrefix("\uFEFF").lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || metadataTag.matches(line)) return@forEach
            val stamps = timeTag.findAll(line).toList()
            if (stamps.isEmpty()) return@forEach
            val body = line.replace(timeTag, "").trim()
            if (body.isEmpty()) return@forEach
            stamps.forEach { result += it.timestampMillis() to body }
        }
        return result
    }

    private fun yrcLines(text: String): List<Pair<Long, String>> {
        if (text.isBlank()) return emptyList()
        return text.removePrefix("\uFEFF").lineSequence().mapNotNull { raw ->
            val line = raw.trim()
            if (line.isEmpty() || metadataTag.matches(line)) return@mapNotNull null
            val stamp = yrcTag.find(line) ?: return@mapNotNull null
            val body = line.replace(yrcTag, "").replace(yrcCharacterTag, "").trim()
            if (body.isEmpty()) null else stamp.groupValues[1].toLong() to body
        }.toList()
    }

    private fun MatchResult.timestampMillis(): Long {
        val minutes = groupValues[1].toLong()
        val seconds = groupValues[2].toLong()
        val fraction = groupValues[3]
        val millis = when (fraction.length) {
            0 -> 0L
            1 -> fraction.toLong() * 100
            2 -> fraction.toLong() * 10
            else -> fraction.take(3).toLong()
        }
        return minutes * 60_000 + seconds * 1_000 + millis
    }

    private fun pairTranslations(
        original: List<Pair<Long, String>>,
        translation: List<Pair<Long, String>>,
    ): List<LyricLine> {
        if (translation.isEmpty()) return original.map { (time, text) -> LyricLine(time, text) }
        val used = BooleanArray(translation.size)
        return original.map { (time, text) ->
            var best = -1
            var bestDelta = Long.MAX_VALUE
            for (index in translation.indices) {
                if (used[index]) continue
                val delta = kotlin.math.abs(translation[index].first - time)
                if (delta <= 1_200 && delta < bestDelta) {
                    best = index
                    bestDelta = delta
                }
            }
            if (best >= 0) {
                used[best] = true
                if (translation[best].second != text) {
                    LyricLine(time, text, translation[best].second)
                } else {
                    LyricLine(time, text)
                }
            } else {
                LyricLine(time, text)
            }
        }
    }

    internal fun cleanTitle(raw: String): String {
        var title = raw.removePrefix("\uFEFF").replace(leadingNumber, "")
        title = title.replace(creditBrackets, " ")
        title = title.replace(trailingCredit, "")
        return title.trim().replace(Regex("\\s+"), " ")
    }

    private fun comparable(value: String): String =
        value.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), "")

    private fun isTitleLine(text: String, matched: QQTrack): Boolean {
        val line = comparable(text)
        val title = comparable(matched.title)
        if (title.isEmpty() || !line.contains(title)) return false
        val singers = matched.singer.split(Regex("[/&、,，]"))
            .map(::comparable).filter { it.isNotEmpty() }
        return singers.any { it in line } || "jaychou" in line
    }

    private fun isLiveText(value: String): Boolean =
        value.contains("演唱会") || value.contains("巡迴") || value.contains("巡回") ||
            liveMark.containsMatchIn(value)

    private fun hasUnwantedMark(value: String): Boolean {
        val lower = value.lowercase()
        return unwantedMarks.any { it in lower }
    }
}
