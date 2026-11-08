package love.yu.jaychou.playback

import android.content.ComponentName
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import love.yu.jaychou.online.OnlineSong
import love.yu.jaychou.online.OnlineMusicRepository
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Stable
class MusicPlaybackState(
    connectToExistingSession: Boolean = false,
) {
    internal var connectionRequested by mutableStateOf(connectToExistingSession)
    internal var pendingPlay = false
    var connecting by mutableStateOf(connectToExistingSession)
        internal set
    val canPlay: Boolean get() = !connecting && (currentSong != null || pendingItems != null)
    var hasPlayback by mutableStateOf(false)
        private set
    var controller by mutableStateOf<MediaController?>(null)
        internal set
    var playing by mutableStateOf(false)
        private set
    var playRequested by mutableStateOf(false)
        private set
    var buffering by mutableStateOf(false)
        private set
    var position by mutableLongStateOf(0)
        private set
    var duration by mutableLongStateOf(0)
        private set
    var repeat by mutableStateOf(false)
        private set
    var speed by mutableFloatStateOf(1f)
        private set
    var failed by mutableStateOf(false)
        internal set
    internal var connectionAttempt by mutableIntStateOf(0)
    private var stoppedByUser = false
    private var positionSampleTime by mutableLongStateOf(0L)
    private var pendingItems: Pair<List<MediaItem>, Int>? = null
    var currentSong by mutableStateOf<OnlineSong?>(null)
        private set
    var lyrics by mutableStateOf<List<EmbeddedLyricLine>>(emptyList())
        internal set
    var lyricsLoading by mutableStateOf(false)
        internal set
    var lyricsFailed by mutableStateOf(false)
        internal set
    var hasNext by mutableStateOf(false)
        private set
    var hasPrevious by mutableStateOf(false)
        private set

    fun playSongs(songs: List<OnlineSong>, index: Int = 0) {
        if (songs.isEmpty() || index !in songs.indices) return
        pendingItems = songs.map(OnlineMedia::item) to index
        play()
    }
    fun next() { controller?.let { if (it.hasNextMediaItem()) { it.seekToNextMediaItem(); it.play() } } }
    fun previous() { controller?.let { if (it.currentPosition > 3_000 || !it.hasPreviousMediaItem()) it.seekTo(0) else it.seekToPreviousMediaItem(); it.play() } }

    /** Interpolate only between real player samples; seeks, pauses and speed changes reset the anchor. */
    internal fun framePosition(): Long {
        val elapsed = if (playing && !buffering) (android.os.SystemClock.elapsedRealtime() - positionSampleTime).coerceAtLeast(0) else 0L
        return (position + (elapsed * speed).toLong()).coerceIn(0, duration.coerceAtLeast(position))
    }

    internal fun update() {
        val player = controller ?: return
        currentSong = player.currentMediaItem?.let(OnlineMedia::song)
        hasNext = player.hasNextMediaItem()
        hasPrevious = player.hasPreviousMediaItem()
        if (player.playWhenReady && !playRequested) stoppedByUser = false
        playRequested = player.playWhenReady
        playing = player.isPlaying
        buffering = player.playbackState == Player.STATE_BUFFERING
        position = player.currentPosition.coerceAtLeast(0)
        positionSampleTime = android.os.SystemClock.elapsedRealtime()
        // Keep paused playback available when switching pages.
        if (!stoppedByUser && (playRequested || playing || position > 0)) hasPlayback = true
        duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: currentSong?.durationMs ?: 0L
        repeat = player.repeatMode == Player.REPEAT_MODE_ONE
        speed = player.playbackParameters.speed
        failed = player.playerError != null
    }

    fun play() {
        val player = controller
        if (player == null) {
            pendingPlay = true
            if (!connecting) {
                if (connectionRequested) connectionAttempt++
                connecting = true
                failed = false
                connectionRequested = true
            }
            return
        }
        val pending = pendingItems
        if (pending != null) {
            player.setMediaItems(pending.first, pending.second, 0)
            pendingItems = null
        } else if (player.mediaItemCount == 0) { failed = true; return }
        stoppedByUser = false
        hasPlayback = true
        if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
        if (player.playbackState == Player.STATE_IDLE || player.playerError != null) player.prepare()
        player.play()
    }
    fun toggle() {
        if (pendingPlay) pendingPlay = false
        else if (controller?.playWhenReady == true) controller?.pause() else play()
    }
    fun seek(position: Long) { controller?.seekTo(position.coerceIn(0, duration)); update() }
    fun restart() { seek(0); play() }
    fun stop() {
        // Remote seek acknowledgements can briefly report the previous position.
        // They must not reopen a bar that the user has explicitly stopped.
        stoppedByUser = true
        pendingPlay = false
        pendingItems = null
        controller?.pause()
        seek(0)
        hasPlayback = false
    }
    fun toggleRepeat() { controller?.repeatMode = if (repeat) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE }
    fun clearAccountQueue(provider: love.yu.jaychou.online.MusicProvider) {
        val queuedProvider = pendingItems?.first?.firstOrNull()?.let(OnlineMedia::song)?.provider
        if (currentSong?.provider != provider && queuedProvider != provider) return
        stop()
        controller?.stop()
        controller?.clearMediaItems()
        currentSong = null
        lyrics = emptyList()
    }
    fun cycleSpeed() {
        val speeds = listOf(.5f, .75f, 1f, 1.25f, 1.5f, 2f)
        controller?.setPlaybackSpeed(speeds[(speeds.indexOf(speed).coerceAtLeast(0) + 1) % speeds.size])
    }
    fun retry() {
        play()
    }
}

@UnstableApi
@Composable
fun rememberMusicPlaybackState(repository: OnlineMusicRepository): MusicPlaybackState {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state = remember(context) {
        MusicPlaybackState(connectToExistingSession = MusicPlaybackService.isRunning)
    }
    LaunchedEffect(state.currentSong?.key) {
        state.lyrics = emptyList()
        state.lyricsFailed = false
        val song = state.currentSong ?: return@LaunchedEffect
        state.lyricsLoading = true
        try {
            state.lyrics = repository.lyrics(song)
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { state.lyricsFailed = true }
        finally { state.lyricsLoading = false }
    }
    DisposableEffect(context, state.connectionRequested, state.connectionAttempt) {
        if (!state.connectionRequested) return@DisposableEffect onDispose { }
        var disposed = false
        state.connecting = true
        state.failed = false
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) { state.update() }
        }
        val future = MediaController.Builder(context,
            SessionToken(context, ComponentName(context, MusicPlaybackService::class.java)))
            .setListener(object : MediaController.Listener {
                override fun onDisconnected(controller: MediaController) {
                    if (!disposed) { state.controller = null; state.connecting = false; state.failed = true }
                }
            }).buildAsync()
        future.addListener({
            if (!disposed) {
                try {
                    state.controller = future.get().also { it.addListener(listener) }
                    state.connecting = false
                    state.update()
                    if (state.pendingPlay) { state.pendingPlay = false; state.play() }
                } catch (_: Exception) { state.connecting = false; state.pendingPlay = false; state.failed = true }
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed = true
            state.controller?.removeListener(listener)
            state.controller = null
            MediaController.releaseFuture(future)
        }
    }
    LaunchedEffect(state, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) { state.update(); delay(if (state.playing) 100 else 400) }
        }
    }
    return state
}
