package love.yu.jaychou

import kotlinx.coroutines.runBlocking
import love.yu.jaychou.online.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class OnlineMusicRepositoryTest {
    private class Store : MusicAccountStore {
        val sessions = mutableMapOf<MusicProvider, String>()
        override fun endpoint(provider: MusicProvider) = "https://music.example.com/${provider.name.lowercase()}"
        override fun setEndpoint(provider: MusicProvider, endpoint: String) { }
        override fun credential(provider: MusicProvider) = sessions[provider].orEmpty()
        override fun setCredential(provider: MusicProvider, credential: String) { sessions[provider] = credential }
    }
    private val qqSong = """{"mid":"song-mid","title":"晴天","singer":[{"name":"周杰伦"}],"album":{"mid":"album-mid","name":"叶惠美"},"interval":269,"file":{"media_mid":"media-mid"}}"""
    private val neteaseSong = """{"id":123,"name":"示例歌曲","ar":[{"name":"歌手"}],"al":{"name":"专辑","picUrl":"https://example.com/art.jpg"},"dt":200000}"""

    @Test fun bothProvidersKeepStableIdentityArtistAndCover() {
        val qq = OnlineSongCodec.decode(MusicProvider.QQ, JSONObject(qqSong))
        assertEquals("QQ:song-mid", qq.key)
        assertEquals(269_000L, qq.durationMs)
        assertEquals("周杰伦", qq.artist)
        assertEquals("media-mid", qq.mediaMid)
        assertTrue(qq.cover.contains("album-mid"))
        val netease = OnlineSongCodec.decode(MusicProvider.NETEASE, JSONObject(neteaseSong))
        assertEquals("NETEASE:123", netease.key)
        assertEquals(200_000L, netease.durationMs)
        assertEquals("https://example.com/art.jpg", netease.cover)
    }

    @Test fun albumMatchingRejectsCoversWrongVersionsAndAmbiguousResults() {
        val song = OnlineSongCodec.decode(MusicProvider.QQ, JSONObject(qqSong))
        assertEquals(song, CatalogSongMatcher.match("晴天", listOf("周杰伦"), "叶惠美", listOf(song)))
        assertNull(CatalogSongMatcher.match("晴天", listOf("其他歌手"), "叶惠美", listOf(song)))
        assertNull(CatalogSongMatcher.match("晴天 (Live)", listOf("周杰伦"), "演唱会", listOf(song)))
        assertNull(CatalogSongMatcher.match("晴天", listOf("周杰伦"), "未知专辑", listOf(song, song.copy(id = "second"))))
        assertEquals(song, CatalogSongMatcher.match("晴天", listOf("周杰伦"), "叶惠美", listOf(song.copy(album = "其他专辑"), song)))
    }

    @Test fun serviceAddressesRejectCredentialsCleartextAndQueries() {
        assertEquals("https://example.com/api", serviceAddress(" https://example.com/api/ "))
        for (invalid in listOf("http://example.com", "https://user:pass@example.com", "https://example.com?cookie=secret", "https://example.com#part", "file:///music")) {
            assertThrows(OnlineMusicException::class.java) { serviceAddress(invalid) }
        }
        assertEquals("https://cdn.example.com/song.mp3", playbackUrl("http://cdn.example.com/song.mp3"))
        assertThrows(OnlineMusicException::class.java) { playbackUrl("file:///music.flac") }
    }

    @Test fun qrPollingReturnsCredentialsWithoutPersistingBeforeIdentityCheck() = runBlocking {
        val store = Store()
        val calls = mutableListOf<String>()
        val repository = OnlineMusicRepository(store) { _, path, params, credential, provider ->
            assertEquals(MusicProvider.QQ, provider)
            assertEquals("", credential)
            calls += path
            when (path) {
                "/login/qr/key" -> { assertEquals("wechat", params["channel"]); JSONObject("""{"code":200,"data":{"unikey":"qr-key"}}""") }
                "/login/qr/create" -> { assertEquals("qr-key", params["key"]); JSONObject("""{"code":200,"data":{"qrimg":"data:image/png;base64,aW1hZ2U="}}""") }
                else -> JSONObject("""{"code":803,"cookie":"qqmusic_session=opaque"}""")
            }
        }
        val qr = repository.startLogin(MusicProvider.QQ, "wechat")
        val check = repository.checkLogin(MusicProvider.QQ, qr.key)
        assertEquals(803, check.code)
        assertEquals("qqmusic_session=opaque", check.credential)
        assertTrue(store.sessions.isEmpty())
        assertEquals(listOf("/login/qr/key", "/login/qr/create", "/login/qr/check"), calls)
    }

    @Test fun searchKeepsProviderPagingAndCredentialsSeparate() = runBlocking {
        val store = Store().apply { sessions[MusicProvider.QQ] = "qqmusic_session=qq-token"; sessions[MusicProvider.NETEASE] = "MUSIC_U=netease-token" }
        val repository = OnlineMusicRepository(store) { _, path, params, credential, provider ->
            if (provider == MusicProvider.QQ) {
                assertEquals("/search", path); assertEquals("2", params["page"]); assertEquals("qqmusic_session=qq-token", credential)
                JSONObject("""{"code":200,"items":[$qqSong],"more":true}""")
            } else {
                assertEquals("/cloudsearch", path); assertEquals("20", params["offset"]); assertEquals("MUSIC_U=netease-token", credential)
                JSONObject("""{"code":200,"result":{"songs":[$neteaseSong],"songCount":30}}""")
            }
        }
        assertTrue(repository.search(MusicProvider.QQ, "晴天", 20).more)
        assertTrue(repository.search(MusicProvider.NETEASE, "示例", 20).more)
    }

    @Test fun qqOwnedPrivateAndLikedListsUseAuthenticatedRoutes() = runBlocking {
        val store = Store().apply { sessions[MusicProvider.QQ] = "qqmusic_session=opaque" }
        val paths = mutableListOf<String>()
        val repository = OnlineMusicRepository(store) { _, path, params, credential, _ ->
            assertEquals("qqmusic_session=opaque", credential)
            paths += path
            when (path) {
                "/user/playlist" -> JSONObject("""{"code":200,"playlist":[{"tid":"100","dirId":3,"dirName":"私密歌单","songNum":101},{"tid":"200","dirId":201,"dirName":"我喜欢","songNum":1},{"tid":"300","dirId":201,"name":"别人的喜欢","songnum":2}]}""")
                else -> { assertEquals("100", params["offset"]); JSONObject("""{"code":200,"songs":[$qqSong],"total":101,"more":false}""") }
            }
        }
        val lists = repository.playlists(MusicProvider.QQ, OnlineUser("1", "昵称", ""))
        assertEquals("私密歌单", lists[0].name); assertTrue(lists[0].owned); assertTrue(lists[1].liked); assertFalse(lists[2].liked)
        assertFalse(repository.tracks(MusicProvider.QQ, lists[0], 100).more)
        repository.tracks(MusicProvider.QQ, lists[1], 100)
        assertEquals(listOf("/user/playlist", "/user/playlist-detail", "/user/liked-songs"), paths)
    }

    @Test fun neteaseLikedPlaylistMustBelongToCurrentUser() = runBlocking {
        val repository = OnlineMusicRepository(Store()) { _, _, _, _, _ -> JSONObject("""{"code":200,"playlist":[{"id":1,"name":"我的喜欢","specialType":5,"creator":{"userId":12}},{"id":2,"name":"别人的喜欢","specialType":5,"creator":{"userId":13}}],"more":false}""") }
        val lists = repository.playlists(MusicProvider.NETEASE, OnlineUser("12", "", ""))
        assertTrue(lists[0].liked); assertFalse(lists[1].liked)
    }

    @Test fun audioUsesAuthenticatedIdentityAndExposesTrials() = runBlocking {
        val store = Store().apply { sessions[MusicProvider.NETEASE] = "MUSIC_U=token" }
        val song = OnlineSongCodec.decode(MusicProvider.NETEASE, JSONObject(neteaseSong))
        val repository = OnlineMusicRepository(store) { _, path, params, credential, _ ->
            assertEquals("/song/url/v1", path); assertEquals("123", params["id"]); assertEquals("MUSIC_U=token", credential)
            JSONObject("""{"code":200,"data":[{"url":"https://cdn.example.com/preview.mp3","freeTrialInfo":{"start":0,"end":30}}]}""")
        }
        assertTrue(repository.audio(song).preview)
        store.sessions.clear()
        try { repository.audio(song); fail("Anonymous playback should be rejected") } catch (failure: OnlineMusicException) { assertEquals(OnlineMusicException.Reason.AUTH, failure.reason) }
    }

    @Test fun rejectedAndMalformedResponsesAreNotReportedAsEmptyMusic() = runBlocking {
        for (body in listOf("""{"code":301}""", """{"code":200}""")) {
            val repository = OnlineMusicRepository(Store()) { _, _, _, _, _ -> JSONObject(body) }
            try { repository.search(MusicProvider.QQ, "test"); fail("Expected an error") } catch (expected: OnlineMusicException) { }
        }
    }

    @Test fun qqLyricsUseQueryRouteAndDecodeTimedText() = runBlocking {
        val repository = OnlineMusicRepository(Store()) { _, path, params, _, _ ->
            assertEquals("/getLyric", path); assertEquals("song-mid", params["songmid"]); assertFalse(params.containsKey("isFormat"))
            JSONObject("""{"response":{"code":0,"lyric":"[00:01.25]测试歌词"}}""")
        }
        val lines = repository.lyrics(OnlineSongCodec.decode(MusicProvider.QQ, JSONObject(qqSong)))
        assertEquals(1250L, lines.single().startMs); assertEquals("测试歌词", lines.single().text)
    }
}
