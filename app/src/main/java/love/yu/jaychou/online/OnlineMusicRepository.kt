package love.yu.jaychou.online

import org.json.JSONArray
import org.json.JSONObject
import love.yu.jaychou.playback.EmbeddedMusicMetadata
import love.yu.jaychou.playback.EmbeddedLyricLine

class OnlineMusicRepository(val accounts: MusicAccountStore,
    private val transport: MusicApiTransport = HttpsMusicApiTransport(accounts::certificateVerificationEnabled),
) {
    private suspend fun api(provider: MusicProvider, path: String, params: Map<String, String> = emptyMap(),
        credential: String = accounts.credential(provider), qr: Boolean = false): JSONObject {
        val endpoint = accounts.endpoint(provider)
        if (endpoint.isBlank()) throw OnlineMusicException(OnlineMusicException.Reason.NOT_CONFIGURED)
        val result = transport.request(endpoint, path, params, credential, provider)
        val code = result.optInt("code", 200)
        if (code == 301 || code == 401 || code == -101) throw OnlineMusicException(OnlineMusicException.Reason.AUTH)
        if (code !in listOf(0, 200) && !(qr && code in 800..803))
            throw OnlineMusicException(OnlineMusicException.Reason.SERVICE)
        return result
    }

    suspend fun startLogin(provider: MusicProvider, channel: String): LoginQr {
        val result = api(provider, "/login/qr/key", if (provider == MusicProvider.QQ) mapOf("channel" to channel) else emptyMap(), credential = "")
        val key = result.optJSONObject("data")?.text("unikey").orEmpty()
        if (key.isBlank()) throw OnlineMusicException(OnlineMusicException.Reason.INVALID_RESPONSE)
        val image = api(provider, "/login/qr/create", mapOf("key" to key, "qrimg" to "true"), credential = "")
            .optJSONObject("data")?.text("qrimg").orEmpty()
        if (image.isBlank()) throw OnlineMusicException(OnlineMusicException.Reason.INVALID_RESPONSE)
        return LoginQr(key, image)
    }

    suspend fun checkLogin(provider: MusicProvider, key: String): LoginCheck {
        val result = api(provider, "/login/qr/check", mapOf("key" to key), credential = "", qr = true)
        return LoginCheck(result.optInt("code"), result.text("cookie"))
    }

    suspend fun cancelLogin(provider: MusicProvider, key: String) {
        if (provider == MusicProvider.QQ) api(provider, "/login/qr/cancel", mapOf("key" to key), credential = "")
    }

    suspend fun user(provider: MusicProvider, credential: String = accounts.credential(provider)): OnlineUser? {
        if (credential.isBlank()) return null
        val result = api(provider, "/login/status", credential = credential)
        val data = result.optJSONObject("data") ?: result
        val baseProfile = data.optJSONObject("profile") ?: return null
        val detail = if (provider == MusicProvider.QQ) try { api(provider, "/user/detail", credential = credential).optJSONObject("profile") }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { null } else null
        val profile = detail ?: baseProfile
        val info = profile.optJSONObject("info") ?: JSONObject()
        fun idOf(source: JSONObject): String = listOf("userId", "str_musicid", "musicid", "uin", "id")
            .map { source.text(it) }.firstOrNull { it.isNotBlank() && it != "0" }.orEmpty()
        val id = idOf(baseProfile).ifBlank { idOf(profile) }.ifBlank { idOf(info) }
        if (id.isBlank() || id == "0") throw OnlineMusicException(OnlineMusicException.Reason.INVALID_RESPONSE)
        return OnlineUser(id, profile.text("nickname", "nick", "nickName", "name").ifBlank { info.text("nick", "nickname", "nickName", "name") }
            .ifBlank { baseProfile.text("nickname", "nick", "name") },
            profile.text("avatarUrl", "avatar", "headurl", "logo").ifBlank { info.text("avatarUrl", "headurl", "logo", "face") }
                .ifBlank { baseProfile.text("avatarUrl", "headurl", "logo") })
    }

    suspend fun refreshSession(provider: MusicProvider) {
        if (provider != MusicProvider.QQ) return
        val credential = accounts.credential(provider)
        if (credential.isBlank()) return
        val endpoint = accounts.endpoint(provider)
        val response = api(provider, "/login/refresh", credential = credential)
        val data = response.optJSONObject("data") ?: return
        val token = data.text("token")
        if (data.optBoolean("rotated") && token.isNotBlank() && accounts.credential(provider) == credential && accounts.endpoint(provider) == endpoint)
            accounts.setCredential(provider, "qqmusic_session=$token")
    }

    suspend fun logout(provider: MusicProvider, credential: String) {
        api(provider, "/logout", credential = credential)
    }

    suspend fun playlists(provider: MusicProvider, user: OnlineUser): List<OnlinePlaylist> {
        val result = mutableListOf<OnlinePlaylist>()
        var offset = 0
        var favoriteCursor: Int? = null
        do {
            val response = api(provider, "/user/playlist", if (provider == MusicProvider.NETEASE)
                mapOf("uid" to user.id, "limit" to "100", "offset" to offset.toString())
                else favoriteCursor?.let { mapOf("favoriteOffset" to it.toString()) }.orEmpty())
            val list = response.optJSONArray("playlist") ?: throw OnlineMusicException(OnlineMusicException.Reason.INVALID_RESPONSE)
            result += list.objects().map { item ->
                val owned = item.has("dirName") || item.optBoolean("owned")
                val dirId = item.optInt("dirId", item.optInt("dirid"))
                val liked = if (provider == MusicProvider.NETEASE) item.optInt("specialType") == 5 &&
                    item.optJSONObject("creator")?.text("userId") == user.id else owned && dirId == 201
                OnlinePlaylist(item.text("tid", "dissid", "id"), item.text("dirName", "dirname", "name", "title", "dissname"),
                    item.text("coverImgUrl", "bigpicUrl", "picUrl", "cover", "picurl", "logo"), item.optInt("songNum", item.optInt("trackCount", item.optInt("song_cnt", item.optInt("songnum")))),
                    liked, dirId, owned)
            }.filter { it.id.isNotBlank() }
            offset += list.length()
            val nextCursor = response.optInt("nextFavoriteOffset", -1)
            val continueQq = provider == MusicProvider.QQ && nextCursor > (favoriteCursor ?: 0)
            favoriteCursor = nextCursor.takeIf { continueQq }
        } while (((provider == MusicProvider.NETEASE && response.optBoolean("more") && list.length() > 0) || continueQq) && offset < 10_000)
        return result.distinctBy { it.id }
    }

    suspend fun search(provider: MusicProvider, query: String, offset: Int = 0): SongPage {
        if (query.isBlank()) return SongPage(emptyList(), false)
        val response = if (provider == MusicProvider.QQ)
            api(provider, "/search", mapOf("keyword" to query.take(60), "type" to "song", "page" to (offset / 20 + 1).toString(), "limit" to "20"))
        else api(provider, "/cloudsearch", mapOf("keywords" to query, "type" to "1", "limit" to "20", "offset" to offset.toString()))
        val data = if (provider == MusicProvider.QQ) response else response.optJSONObject("result") ?: JSONObject()
        val items = data.optJSONArray(if (provider == MusicProvider.QQ) "items" else "songs")
            ?: if (data.optInt("songCount", -1) == 0) JSONArray() else throw OnlineMusicException(OnlineMusicException.Reason.INVALID_RESPONSE)
        val songs = items.objects().map { OnlineSongCodec.decode(provider, it) }
        return SongPage(songs, if (provider == MusicProvider.QQ) response.optBoolean("more") else offset + songs.size < data.optInt("songCount"))
    }

    suspend fun tracks(provider: MusicProvider, playlist: OnlinePlaylist, offset: Int = 0): SongPage {
        val response: JSONObject
        val raw: JSONArray
        val more: Boolean
        if (provider == MusicProvider.NETEASE) {
            response = api(provider, "/playlist/track/all", mapOf("id" to playlist.id, "offset" to offset.toString(), "limit" to "100"))
            raw = response.optJSONArray("songs") ?: throw OnlineMusicException(OnlineMusicException.Reason.INVALID_RESPONSE)
            more = raw.length() == 100
        } else if (playlist.liked || playlist.owned) {
            response = api(provider, if (playlist.liked) "/user/liked-songs" else "/user/playlist-detail",
                mapOf("tid" to playlist.id, "dirid" to playlist.dirId.toString(), "offset" to offset.toString(), "limit" to "100"))
            raw = response.optJSONArray("songs") ?: throw OnlineMusicException(OnlineMusicException.Reason.INVALID_RESPONSE)
            more = response.optBoolean("more") || offset + raw.length() < response.optInt("total")
        } else {
            response = api(provider, "/getSongListDetail/${playlist.id}")
            val detail = response.optJSONObject("response")?.optJSONArray("cdlist")?.optJSONObject(0)
                ?: throw OnlineMusicException(OnlineMusicException.Reason.INVALID_RESPONSE)
            val all = detail.optJSONArray("songlist") ?: throw OnlineMusicException(OnlineMusicException.Reason.INVALID_RESPONSE)
            raw = JSONArray(all.objects().drop(offset).take(100))
            more = offset + raw.length() < all.length()
        }
        return SongPage(raw.objects().map { OnlineSongCodec.decode(provider, it.optJSONObject("songInfo") ?: it) }, more && raw.length() > 0)
    }

    suspend fun audio(song: OnlineSong): OnlineAudio {
        if (accounts.credential(song.provider).isBlank()) throw OnlineMusicException(OnlineMusicException.Reason.AUTH)
        if (song.provider == MusicProvider.NETEASE) {
            val response = api(song.provider, "/song/url/v1", mapOf("id" to song.id, "level" to "exhigh"))
            val data = response.optJSONArray("data")?.optJSONObject(0) ?: throw OnlineMusicException(OnlineMusicException.Reason.INVALID_RESPONSE)
            return OnlineAudio(playbackUrl(data.text("url")), !data.isNull("freeTrialInfo"))
        }
        val response = api(song.provider, "/getMusicPlay/${song.id}", mapOf("quality" to "best", "mediaId" to song.mediaMid))
        val entries = response.optJSONObject("data")?.optJSONObject("playUrl")
            ?: throw OnlineMusicException(OnlineMusicException.Reason.INVALID_RESPONSE)
        val data = entries.optJSONObject(song.id) ?: throw OnlineMusicException(OnlineMusicException.Reason.RESTRICTED)
        return OnlineAudio(playbackUrl(data.text("url")), data.optBoolean("preview"))
    }

    suspend fun lyrics(song: OnlineSong): List<EmbeddedLyricLine> {
        val response = api(song.provider, if (song.provider == MusicProvider.NETEASE) "/lyric" else "/getLyric",
            if (song.provider == MusicProvider.NETEASE) mapOf("id" to song.id) else mapOf("songmid" to song.id))
        val text = if (song.provider == MusicProvider.NETEASE) response.optJSONObject("lrc")?.text("lyric").orEmpty()
            else (response.optJSONObject("response") ?: response.optJSONObject("data") ?: response).text("lyric")
        val decoded = if (text.isNotBlank() && !text.contains('[') && !text.contains('\n'))
            runCatching { String(java.util.Base64.getDecoder().decode(text), Charsets.UTF_8) }.getOrDefault(text) else text
        return EmbeddedMusicMetadata.parseLyrics(decoded)
    }
}
