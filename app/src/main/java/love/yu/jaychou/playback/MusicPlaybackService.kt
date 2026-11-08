package love.yu.jaychou.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.runBlocking
import love.yu.jaychou.online.*
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import love.yu.jaychou.MainActivity

/** Owns audio independently of Compose and Activity recreation. */
@UnstableApi
class MusicPlaybackService : MediaSessionService() {
    companion object {
        @Volatile internal var isRunning = false
            private set
    }
    private var session: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())
    private val preferences by lazy { getSharedPreferences("online_music_playback", MODE_PRIVATE) }
    private val savePosition = object : Runnable {
        override fun run() {
            persist()
            handler.postDelayed(this, 5_000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        val repository = OnlineMusicRepository(DeviceMusicAccountStore(this))
        // Resolve each queued track on the loader thread, including background/lock-screen next.
        // Expiring CDN links are never persisted or baked into a playlist.
        val sources = ResolvingDataSource.Factory(DefaultDataSource.Factory(this)) { spec ->
            if (spec.uri.scheme != "jay-online") spec else {
                val provider = MusicProvider.valueOf(spec.uri.host.orEmpty().uppercase(java.util.Locale.ROOT))
                val id = spec.uri.lastPathSegment.orEmpty()
                try {
                    val audio = runBlocking { repository.audio(OnlineSong(provider, id, "", emptyList(), "", "", 0,
                        spec.uri.getQueryParameter("mediaMid").orEmpty())) }
                    if (audio.preview) throw java.io.IOException("Account cannot play the full recording")
                    spec.withUri(android.net.Uri.parse(audio.url))
                } catch (failure: OnlineMusicException) { throw java.io.IOException(failure.reason.name, failure) }
            }
        }
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(sources))
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
        player.repeatMode = if (preferences.getBoolean("repeat", false)) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.setPlaybackSpeed(preferences.getFloat("speed", 1f).coerceIn(.5f, 2f))
        val activity = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).putExtra("open_music_player", true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        session = MediaSession.Builder(this, player).setSessionActivity(activity).build()
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) { persist() }
        })
        handler.postDelayed(savePosition, 5_000)
    }

    private fun persist() {
        val player = session?.player ?: return
        if (player.mediaItemCount == 0) return
        preferences.edit()
            .putLong("position", if (player.playbackState == Player.STATE_ENDED) 0 else player.currentPosition.coerceAtLeast(0))
            .putBoolean("repeat", player.repeatMode == Player.REPEAT_MODE_ONE)
            .putFloat("speed", player.playbackParameters.speed)
            .apply()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        session?.takeIf { controllerInfo.isTrusted || controllerInfo.packageName == packageName }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(savePosition)
        persist()
        session?.run { player.release(); release() }
        session = null
        super.onDestroy()
    }
}
