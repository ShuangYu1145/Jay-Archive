package love.yu.jaychou

import kotlinx.coroutines.*
import love.yu.jaychou.online.*
import love.yu.jaychou.playback.MusicPlaybackState
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class OnlineMusicStateTest {
    private class Store : MusicAccountStore {
        var value = ""
        val pending = mutableMapOf<MusicProvider, PendingMusicLogin>()
        var selected = MusicProvider.QQ
        override fun endpoint(provider: MusicProvider) = "https://fixture.example.com"
        override fun setEndpoint(provider: MusicProvider, endpoint: String) { }
        override fun credential(provider: MusicProvider) = value
        override fun setCredential(provider: MusicProvider, credential: String) { value = credential }
        override fun pendingLogin(provider: MusicProvider) = pending[provider]
        override fun setPendingLogin(provider: MusicProvider, login: PendingMusicLogin?) {
            if (login == null) pending.remove(provider) else pending[provider] = login
        }
        override fun selectedProvider() = selected
        override fun setSelectedProvider(provider: MusicProvider) { selected = provider }
    }

    @Test fun cancelledSearchCannotReplaceNewerResultsOrLoadingState() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val releaseOld = CompletableDeferred<Unit>()
        val repository = OnlineMusicRepository(Store()) { _, _, params, _, _ ->
            val keyword = params["keyword"]
            if (keyword == "old") withContext(NonCancellable) { releaseOld.await() }
            JSONObject("""{"code":200,"items":[{"mid":"$keyword","title":"$keyword","singer":[],"album":{}}],"more":false}""")
        }
        try {
            val state = OnlineMusicState(repository, scope, MusicPlaybackState())
            state.query = "old"; state.search()
            assertTrue(state.loading)
            state.query = "new"; state.search()
            assertEquals("new", state.songs.single().title)
            releaseOld.complete(Unit)
            yield()
            assertEquals("new", state.songs.single().title)
            assertFalse(state.loading)
        } finally { scope.cancel() }
    }

    @Test fun cancellingLoginDuringConfirmationCannotSaveAnAccount() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val checking = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val store = Store()
        val repository = OnlineMusicRepository(store) { _, path, _, _, _ ->
            when (path) {
                "/login/qr/key" -> JSONObject("""{"code":200,"data":{"unikey":"key"}}""")
                "/login/qr/create" -> JSONObject("""{"code":200,"data":{"qrimg":"data:image/png;base64,aW1hZ2U="}}""")
                "/login/qr/check" -> {
                    checking.complete(Unit)
                    withContext(NonCancellable) { release.await() }
                    JSONObject("""{"code":803,"cookie":"qqmusic_session=late"}""")
                }
                else -> JSONObject("""{"code":200}""")
            }
        }
        try {
            val state = OnlineMusicState(repository, scope, MusicPlaybackState())
            state.startLogin("qq")
            withTimeout(5_000) { checking.await() }
            state.cancelLogin()
            release.complete(Unit)
            yield()
            assertEquals("", store.value)
            assertTrue(state.users.isEmpty())
            assertNull(state.qr)
        } finally { scope.cancel() }
    }

    @Test fun serverlessRefreshDoesNotResurrectLoggedOutSession() = runBlocking {
        val store = Store().apply { value = "qqmusic_session=old" }
        val repository = OnlineMusicRepository(store) { _, path, _, _, _ ->
            assertEquals("/login/refresh", path)
            store.value = "" // Logout occurs while the response is in flight.
            JSONObject("""{"code":200,"data":{"rotated":true,"token":"new"}}""")
        }
        repository.refreshSession(MusicProvider.QQ)
        assertEquals("", store.value)
    }

    @Test fun leavingAndRecreatingTheAppReusesTheSameQrUntilConfirmation() = runBlocking {
        val store = Store()
        var generated = 0; var cancelled = 0; var confirmed = false
        val repository = OnlineMusicRepository(store) { _, path, _, _, _ ->
            when (path) {
                "/login/qr/key" -> { generated++; JSONObject("""{"code":200,"data":{"unikey":"same-key"}}""") }
                "/login/qr/create" -> JSONObject("""{"code":200,"data":{"qrimg":"data:image/png;base64,aW1hZ2U="}}""")
                "/login/qr/check" -> JSONObject(if (confirmed) """{"code":803,"cookie":"qqmusic_session=confirmed"}""" else """{"code":801}""")
                "/login/qr/cancel" -> { cancelled++; JSONObject("""{"code":200}""") }
                "/login/status" -> JSONObject("""{"code":200,"data":{"profile":{"uin":"42","nick":"listener"}}}""")
                "/user/playlist" -> JSONObject("""{"code":200,"playlist":[]}""")
                else -> JSONObject("""{"code":200}""")
            }
        }
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val first = OnlineMusicState(repository, firstScope, MusicPlaybackState())
            first.startLogin("qq")
            val qr = first.qr
            first.pauseLogin(); firstScope.cancel()
            assertEquals(qr, store.pending[MusicProvider.QQ]?.qr)
            assertEquals(0, cancelled)
            val restored = OnlineMusicState(repository, secondScope, MusicPlaybackState())
            assertEquals(qr, restored.qr)
            confirmed = true // The user scans the saved picture while our Activity is gone.
            restored.startLogin("qq")
            assertEquals(1, generated)
            assertEquals(0, cancelled)
            assertEquals("qqmusic_session=confirmed", store.value)
            assertEquals("42", restored.users[MusicProvider.QQ]?.id)
            assertNull(store.pending[MusicProvider.QQ])
        } finally { firstScope.cancel(); secondScope.cancel() }
    }

    @Test fun confirmedSessionSurvivesProfileNetworkFailureWithoutPollingTheQrAgain() = runBlocking {
        val store = Store()
        var polls = 0; var failProfile = true
        val repository = OnlineMusicRepository(store) { _, path, _, _, _ ->
            when (path) {
                "/login/qr/key" -> JSONObject("""{"code":200,"data":{"unikey":"confirmed-key"}}""")
                "/login/qr/create" -> JSONObject("""{"code":200,"data":{"qrimg":"data:image/png;base64,aW1hZ2U="}}""")
                "/login/qr/check" -> { polls++; JSONObject("""{"code":803,"cookie":"qqmusic_session=kept"}""") }
                "/login/status" -> {
                    if (failProfile) throw OnlineMusicException(OnlineMusicException.Reason.NETWORK)
                    JSONObject("""{"code":200,"data":{"profile":{"uin":"42","nick":"listener"}}}""")
                }
                "/user/playlist" -> JSONObject("""{"code":200,"playlist":[]}""")
                else -> JSONObject("""{"code":200}""")
            }
        }
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val first = OnlineMusicState(repository, firstScope, MusicPlaybackState())
            first.startLogin("qq")
            assertEquals(OnlineMusicException.Reason.NETWORK, first.loginError)
            assertEquals("qqmusic_session=kept", store.pending[MusicProvider.QQ]?.credential)
            firstScope.cancel(); failProfile = false
            val restored = OnlineMusicState(repository, secondScope, MusicPlaybackState())
            restored.startLogin("qq")
            assertEquals(1, polls)
            assertEquals("qqmusic_session=kept", store.value)
            assertNull(restored.qr)
        } finally { firstScope.cancel(); secondScope.cancel() }
    }

    @Test fun expiredQrIsNotRegeneratedUntilTheUserExplicitlyRefreshes() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var generated = 0
        val repository = OnlineMusicRepository(Store()) { _, path, _, _, _ ->
            when (path) {
                "/login/qr/key" -> { generated++; JSONObject("""{"code":200,"data":{"unikey":"key-$generated"}}""") }
                "/login/qr/create" -> JSONObject("""{"code":200,"data":{"qrimg":"data:image/png;base64,aW1hZ2U="}}""")
                "/login/qr/check" -> JSONObject("""{"code":800}""")
                else -> JSONObject("""{"code":200}""")
            }
        }
        try {
            val state = OnlineMusicState(repository, scope, MusicPlaybackState())
            state.startLogin("qq"); assertEquals(800, state.qrCode)
            state.pauseLogin(); state.startLogin("qq")
            assertEquals(1, generated)
            assertEquals("key-1", state.qr?.key)
            state.refreshLogin("qq")
            assertEquals(2, generated)
            assertEquals("key-2", state.qr?.key)
        } finally { scope.cancel() }
    }
}
