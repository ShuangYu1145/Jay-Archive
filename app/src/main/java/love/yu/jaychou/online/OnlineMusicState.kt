package love.yu.jaychou.online

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import love.yu.jaychou.data.Album
import love.yu.jaychou.data.Track
import love.yu.jaychou.playback.MusicPlaybackState

@Stable
class OnlineMusicState(val repository: OnlineMusicRepository, private val scope: CoroutineScope, private val playback: MusicPlaybackState) {
    var verifyServiceCertificates by mutableStateOf(repository.accounts.certificateVerificationEnabled())
        private set
    var provider by mutableStateOf(repository.accounts.selectedProvider())
        private set
    val users = mutableStateMapOf<MusicProvider, OnlineUser>()
    var playlists by mutableStateOf<List<OnlinePlaylist>>(emptyList())
        private set
    var selectedPlaylist by mutableStateOf<OnlinePlaylist?>(null)
        private set
    var songs by mutableStateOf<List<OnlineSong>>(emptyList())
        private set
    var query by mutableStateOf("")
    var searchedQuery by mutableStateOf("")
        private set
    var more by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set
    var accountLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<OnlineMusicException.Reason?>(null)
        private set
    private fun restoredLogin(source: MusicProvider) = repository.accounts.pendingLogin(source)?.takeIf {
        it.endpoint == repository.accounts.endpoint(source)
    }
    private var pendingLogin = restoredLogin(provider)
    var qr by mutableStateOf(pendingLogin?.qr)
        private set
    var loginChannel by mutableStateOf(pendingLogin?.channel ?: "qq")
        private set
    var qrCode by mutableIntStateOf(pendingLogin?.code ?: 801)
        private set
    var loginLoading by mutableStateOf(false)
        private set
    var loginError by mutableStateOf<OnlineMusicException.Reason?>(null)
        private set
    var matching by mutableStateOf(false)
        private set
    private var browseJob: Job? = null
    private var accountJob: Job? = null
    private var loginJob: Job? = null
    private var matchJob: Job? = null
    private var offset = 0
    private var browseGeneration = 0
    private var accountGeneration = 0
    private var loginGeneration = 0
    private var matchGeneration = 0
    private fun cancelBrowse() { browseGeneration++; browseJob?.cancel(); loading = false }
    private fun cancelAccount() { accountGeneration++; accountJob?.cancel(); accountLoading = false }
    private fun cancelMatch() { matchGeneration++; matchJob?.cancel(); matching = false }

    private fun reason(failure: Exception) = (failure as? OnlineMusicException)?.reason ?: OnlineMusicException.Reason.NETWORK
    fun dismissError() { error = null }
    fun setCertificateVerification(enabled: Boolean) {
        if (enabled == verifyServiceCertificates) return
        pauseLogin(); cancelAccount(); cancelBrowse(); cancelMatch()
        repository.accounts.setCertificateVerificationEnabled(enabled)
        verifyServiceCertificates = enabled
        error = null
    }
    fun select(provider: MusicProvider) {
        if (this.provider == provider) return
        pauseLogin(); cancelBrowse(); cancelAccount()
        this.provider = provider
        repository.accounts.setSelectedProvider(provider)
        pendingLogin = restoredLogin(provider)
        qr = pendingLogin?.qr
        loginChannel = pendingLogin?.channel ?: "qq"
        qrCode = pendingLogin?.code ?: 801
        loginError = null
        query = ""; searchedQuery = ""; selectedPlaylist = null; songs = emptyList(); playlists = emptyList(); more = false; error = null; loading = false
        refreshAccount()
    }
    fun configure(netease: String, qq: String): Boolean {
        return try {
            val addresses = mapOf(MusicProvider.NETEASE to serviceAddress(netease), MusicProvider.QQ to serviceAddress(qq))
            pauseLogin(); cancelAccount(); cancelBrowse(); cancelMatch()
            addresses.forEach { (source, address) ->
                if (repository.accounts.endpoint(source) != address) {
                    playback.clearAccountQueue(source)
                    users.remove(source)
                    repository.accounts.setPendingLogin(source, null)
                    repository.accounts.setEndpoint(source, address)
                    if (source == provider) { pendingLogin = null; qr = null; qrCode = 801 }
                }
            }
            songs = emptyList(); playlists = emptyList(); selectedPlaylist = null; more = false; loading = false; error = null
            refreshAccount(); true
        } catch (failure: Exception) { error = reason(failure); false }
    }
    fun refreshAccount() {
        cancelAccount()
        val generation = accountGeneration
        val source = provider
        accountJob = scope.launch {
            accountLoading = true; error = null
            try {
                try { repository.refreshSession(source) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* The current session may still be valid; verify it below. */ }
                val user = repository.user(source)
                if (user == null) {
                    users.remove(source); playlists = emptyList()
                    repository.accounts.setCredential(source, "")
                } else {
                    users[source] = user
                    playlists = repository.playlists(source, user)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                error = reason(failure)
                if (error == OnlineMusicException.Reason.AUTH) { users.remove(source); repository.accounts.setCredential(source, ""); playlists = emptyList() }
            } finally { if (generation == accountGeneration) accountLoading = false }
        }
    }
    private fun keepPending(login: PendingMusicLogin?) {
        pendingLogin = login
        repository.accounts.setPendingLogin(provider, login)
    }

    /** Closing a sheet or leaving the app pauses polling; it never cancels the server's QR. */
    fun pauseLogin() {
        loginGeneration++
        loginJob?.cancel(); loginJob = null; loginLoading = false
    }
    fun startLogin(channel: String = loginChannel) = beginLogin(channel, refresh = false)
    fun refreshLogin(channel: String = loginChannel) = beginLogin(channel, refresh = true)

    private fun beginLogin(channel: String, refresh: Boolean) {
        if (!refresh && channel == loginChannel && loginJob?.isActive == true) return
        if (refresh || (pendingLogin != null && pendingLogin?.channel != channel)) cancelLogin() else pauseLogin()
        loginChannel = channel
        val generation = loginGeneration
        val source = provider
        loginJob = scope.launch {
            loginLoading = pendingLogin == null
            loginError = null
            try {
                if (pendingLogin == null) {
                    val generated = repository.startLogin(source, channel)
                    ensureActive()
                    keepPending(PendingMusicLogin(generated, channel, repository.accounts.endpoint(source)))
                    qr = generated
                    qrCode = 801
                }
                loginLoading = false
                if (pendingLogin?.code == 800) { qrCode = 800; return@launch }
                while (isActive) {
                    var pending = pendingLogin ?: return@launch
                    // An already-confirmed session is saved before fetching the profile, so a
                    // network interruption or process recreation does not lose the confirmation.
                    val check = if (pending.credential.isNotBlank()) LoginCheck(803, pending.credential)
                        else repository.checkLogin(source, pending.qr.key)
                    ensureActive()
                    if (check.code !in 800..803) throw OnlineMusicException(OnlineMusicException.Reason.INVALID_RESPONSE)
                    qrCode = check.code
                    pending = pending.copy(code = check.code, credential = check.credential.ifBlank { pending.credential })
                    if (pending != pendingLogin) keepPending(pending)
                    if (check.code == 800) return@launch
                    if (check.code == 803) {
                        loginLoading = true
                        if (pending.credential.isBlank()) throw OnlineMusicException(OnlineMusicException.Reason.INVALID_RESPONSE)
                        val user = repository.user(source, pending.credential) ?: throw OnlineMusicException(OnlineMusicException.Reason.AUTH)
                        ensureActive()
                        repository.accounts.setCredential(source, pending.credential)
                        keepPending(null)
                        users[source] = user
                        qr = null
                        refreshAccount()
                        return@launch
                    }
                    delay(2_000)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { loginError = reason(failure) }
            finally { if (generation == loginGeneration) loginLoading = false }
        }
    }

    /** Only explicit refresh, channel changes and sign-out abandon the current QR. */
    fun cancelLogin() {
        val old = pendingLogin; val source = provider
        pauseLogin()
        keepPending(null)
        qr = null; qrCode = 801; loginError = null
        if (old != null && old.code != 803) scope.launch {
            try { repository.cancelLogin(source, old.qr.key) } catch (_: Exception) { }
        }
    }
    fun logout() {
        val source = provider
        val credential = repository.accounts.credential(source)
        cancelLogin(); cancelAccount(); cancelBrowse(); cancelMatch()
        repository.accounts.setCredential(source, "")
        users.remove(source); playlists = emptyList(); songs = emptyList(); selectedPlaylist = null; more = false; loading = false; matching = false
        playback.clearAccountQueue(source)
        scope.launch { try { repository.logout(source, credential) } catch (_: Exception) { } }
    }
    fun library() {
        cancelBrowse(); selectedPlaylist = null; searchedQuery = ""; songs = emptyList(); more = false; loading = false; error = null
    }
    fun search() {
        if (query.isBlank()) { library(); return }
        selectedPlaylist = null; searchedQuery = query.trim(); load(false)
    }
    fun open(playlist: OnlinePlaylist) { selectedPlaylist = playlist; searchedQuery = ""; load(false) }
    fun liked() {
        val liked = playlists.firstOrNull { it.liked }
            ?: if (provider == MusicProvider.QQ) OnlinePlaylist("liked", "", "", 0, liked = true) else null
        if (liked != null) open(liked) else error = OnlineMusicException.Reason.NO_MATCH
    }
    fun load(append: Boolean) {
        if (append && (loading || !more)) return
        cancelBrowse()
        val generation = browseGeneration
        if (!append) { songs = emptyList(); offset = 0; more = false }
        val source = provider; val playlist = selectedPlaylist; val search = searchedQuery
        browseJob = scope.launch {
            loading = true; error = null
            try {
                val page = if (playlist != null) repository.tracks(source, playlist, offset) else repository.search(source, search, offset)
                ensureActive()
                songs = (if (append) songs + page.songs else page.songs).distinctBy { it.key }
                offset += page.songs.size; more = page.more && page.songs.isNotEmpty()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error = reason(failure) }
            finally { if (generation == browseGeneration) loading = false }
        }
    }
    fun play(index: Int = 0) {
        if (repository.accounts.credential(provider).isBlank()) { error = OnlineMusicException.Reason.AUTH; return }
        if (index !in songs.indices) return
        cancelMatch()
        val generation = matchGeneration
        val source = provider; val playlist = selectedPlaylist
        val loaded = songs; val firstOffset = offset; val hasMore = more
        matchJob = scope.launch {
            matching = true; error = null
            try {
                var queue = loaded
                var nextOffset = firstOffset
                var remaining = hasMore && playlist != null
                while (remaining && queue.size < 10_000) {
                    val page = repository.tracks(source, playlist!!, nextOffset)
                    ensureActive()
                    queue = (queue + page.songs).distinctBy { it.key }
                    nextOffset += page.songs.size
                    remaining = page.more && page.songs.isNotEmpty()
                }
                ensureActive()
                playback.playSongs(queue, index)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error = reason(failure) }
            finally { if (generation == matchGeneration) matching = false }
        }
    }
    fun playCatalog(album: Album, track: Track) {
        cancelMatch(); error = null
        val generation = matchGeneration
        matchJob = scope.launch {
            matching = true
            try {
                if (repository.accounts.credential(MusicProvider.QQ).isBlank()) throw OnlineMusicException(OnlineMusicException.Reason.AUTH)
                val page = repository.search(MusicProvider.QQ, "${track.originalTitle} ${track.artists.firstOrNull().orEmpty()}")
                val song = CatalogSongMatcher.match(track.originalTitle, track.artists, album.originalTitle, page.songs)
                    ?: throw OnlineMusicException(OnlineMusicException.Reason.NO_MATCH)
                ensureActive()
                playback.playSongs(listOf(song))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error = reason(failure) }
            finally { if (generation == matchGeneration) matching = false }
        }
    }
}
