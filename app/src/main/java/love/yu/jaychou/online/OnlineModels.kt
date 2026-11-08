package love.yu.jaychou.online

import java.net.URI
import java.text.Normalizer
import org.json.JSONArray
import org.json.JSONObject

enum class MusicProvider { NETEASE, QQ }

data class OnlineUser(val id: String, val nickname: String, val avatar: String)
data class OnlinePlaylist(
    val id: String, val name: String, val cover: String, val count: Int,
    val liked: Boolean = false, val dirId: Int = 0, val owned: Boolean = false,
)
data class OnlineSong(
    val provider: MusicProvider, val id: String, val title: String, val artists: List<String>,
    val album: String, val cover: String, val durationMs: Long, val mediaMid: String = "",
) {
    val key get() = "${provider.name}:$id"
    val artist get() = artists.joinToString(" / ")
}
data class SongPage(val songs: List<OnlineSong>, val more: Boolean)
data class LoginQr(val key: String, val image: String)
data class PendingMusicLogin(
    val qr: LoginQr, val channel: String, val endpoint: String,
    val code: Int = 801, val credential: String = "",
)
data class LoginCheck(val code: Int, val credential: String = "")
data class OnlineAudio(val url: String, val preview: Boolean = false)
class OnlineMusicException(val reason: Reason) : Exception(reason.name) {
    enum class Reason { NOT_CONFIGURED, INVALID_ADDRESS, NETWORK, TLS, SERVICE, AUTH, RESTRICTED, INVALID_RESPONSE, NO_MATCH }
}

internal fun JSONObject.text(vararg keys: String): String = keys.firstNotNullOfOrNull { key ->
    opt(key)?.takeUnless { it == JSONObject.NULL }?.toString()?.takeIf { it.isNotBlank() && it != "null" }
}.orEmpty()
internal fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }

/** Service credentials are only sent to an explicitly configured HTTPS origin. */
fun serviceAddress(value: String): String {
    if (value.isBlank()) return ""
    val uri = try { URI(value.trim()) } catch (_: Exception) { throw OnlineMusicException(OnlineMusicException.Reason.INVALID_ADDRESS) }
    if (uri.scheme != "https" || uri.host.isNullOrBlank() || uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null)
        throw OnlineMusicException(OnlineMusicException.Reason.INVALID_ADDRESS)
    return uri.toASCIIString().trimEnd('/')
}

fun playbackUrl(value: String): String {
    val uri = runCatching { URI(value) }.getOrNull()
    if (uri == null || uri.scheme !in listOf("https", "http") || uri.host.isNullOrBlank() || uri.rawUserInfo != null)
        throw OnlineMusicException(OnlineMusicException.Reason.RESTRICTED)
    return if (uri.scheme == "http") "https" + uri.toASCIIString().removePrefix("http") else uri.toASCIIString()
}

object OnlineSongCodec {
    fun decode(provider: MusicProvider, item: JSONObject): OnlineSong {
        val qq = provider == MusicProvider.QQ
        val album = item.optJSONObject(if (qq) "album" else "al") ?: item.optJSONObject("album") ?: JSONObject()
        val artists = (item.optJSONArray(if (qq) "singer" else "ar") ?: item.optJSONArray("artists"))
            ?.objects()?.map { it.text("name", "title") }?.filter(String::isNotBlank).orEmpty()
        val mid = album.text("mid", "albummid").ifBlank { item.text("albummid") }
        val id = item.text(*(if (qq) arrayOf("mid", "songmid") else arrayOf("id")))
        if (id.isBlank() || item.text("title", "name", "songname").isBlank())
            throw OnlineMusicException(OnlineMusicException.Reason.INVALID_RESPONSE)
        return OnlineSong(provider, id, item.text("title", "name", "songname"), artists,
            album.text("name", "title").ifBlank { item.text("albumname") },
            album.text("picUrl", "coverUrl").ifBlank {
                if (qq && mid.isNotBlank()) "https://y.gtimg.cn/music/photo_new/T002R800x800M000$mid.jpg" else ""
            }, if (qq) item.optLong("interval", item.optLong("duration")) * 1000 else item.optLong("dt", item.optLong("duration")),
            item.optJSONObject("file")?.text("media_mid").orEmpty())
    }
}

object CatalogSongMatcher {
    private fun normalized(value: String) = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase(java.util.Locale.ROOT).replace(Regex("[\\s\\p{P}\\p{S}]"), "")

    /** Never play a fuzzy first result: title and artist must agree, then prefer this album. */
    fun match(title: String, artists: List<String>, album: String, candidates: List<OnlineSong>): OnlineSong? {
        val names = artists.map(::normalized)
        val matches = candidates.filter { song -> normalized(song.title) == normalized(title) &&
            song.artists.any { normalized(it) in names } }
        return matches.firstOrNull { normalized(it.album) == normalized(album) } ?: matches.singleOrNull()
    }
}
