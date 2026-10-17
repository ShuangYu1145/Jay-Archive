package love.yu.jaychou.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class LyricsVersion(val id: Long, val artist: String, val title: String, val album: String,
                         val text: String, val instrumental: Boolean)

object LyricsRepository {
    const val source = "https://lrclib.net/"
    private const val maxBytes = 1_048_576
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
        if (target.exists() && !target.delete()) throw java.io.IOException("无法删除本地歌词")
    }

    suspend fun import(context: Context, album: Album, track: Track, uri: Uri): String = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.boundedBytes(maxBytes) }
            ?: throw java.io.IOException("无法读取所选文件")
        require(bytes.size <= maxBytes) { "歌词文件不能超过 1 MB" }
        val text = try {
            Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: java.nio.charset.CharacterCodingException) { bytes.toString(charset("GB18030")) }
        require(text.isNotBlank() && !text.contains('\u0000')) { "请选择 UTF-8 或 GB18030 编码的 LRC/TXT 文本" }
        file(context, album, track).also { it.parentFile!!.mkdirs() }.writeText(text)
        text
    }

    suspend fun search(track: Track): List<LyricsVersion> = withContext(Dispatchers.IO) {
        val name = track.title.replace(Regex("^\\d+[.、]"), "").trim()
        val query = "track_name=${URLEncoder.encode(name, "UTF-8")}&artist_name=Jay%20Chou"
        val connection = URI("https://lrclib.net/api/search?$query").toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 12_000; connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", "JayArchive/0.2 (Android; personal music archive)")
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode !in 200..299) throw java.io.IOException("歌词服务暂不可用（${connection.responseCode}）")
            val body = connection.inputStream.use { it.boundedBytes(4 * maxBytes) }
            if (body.size > 4 * maxBytes) throw java.io.IOException("歌词服务返回内容过大")
            decodeCandidates(body.toString(Charsets.UTF_8))
        } finally { connection.disconnect() }
    }

    fun decodeCandidates(json: String): List<LyricsVersion> {
        val array = JSONArray(json)
        return (0 until array.length()).mapNotNull { index ->
            val value = array.getJSONObject(index)
            val artist = value.optString("artistName")
            val compact = artist.lowercase().replace(Regex("[\\s·&/]+"), "")
            if (!listOf("jaychou", "周杰伦").any { it in compact }) return@mapNotNull null
            val plain = value.optString("plainLyrics").takeUnless { it == "null" }.orEmpty()
            val synced = value.optString("syncedLyrics").takeUnless { it == "null" }.orEmpty()
            val instrumental = value.optBoolean("instrumental")
            if (plain.isBlank() && synced.isBlank() && !instrumental) return@mapNotNull null
            val albumName = value.optString("albumName").takeUnless { it.isBlank() || it == "null" }
                ?: "来源未标注专辑"
            LyricsVersion(value.getLong("id"), artist, value.optString("trackName"), albumName,
                plain.ifBlank { synced }, instrumental)
        }.distinctBy { listOf(it.artist, it.title, it.album, it.text) }.take(20)
    }

    fun lines(text: String): List<String> = text.removePrefix("\uFEFF").lineSequence()
        .filterNot { Regex("^\\[(ar|ti|al|by|offset|length):.*]$", RegexOption.IGNORE_CASE).matches(it.trim()) }
        .map { it.replace(Regex("\\[\\d{1,3}:\\d{2}(?:[.:]\\d{1,3})?]"), "").trim() }.toList()
}
