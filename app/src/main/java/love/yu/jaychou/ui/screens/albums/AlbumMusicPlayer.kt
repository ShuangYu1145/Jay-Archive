package love.yu.jaychou.ui.screens.albums

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yu.jaychou.R
import love.yu.jaychou.data.AppText
import love.yu.jaychou.playback.MusicPlaybackState
import love.yu.jaychou.ui.designsystem.*

/** Inline controls keep listening in the album, beside the original track listing. */
@Composable
internal fun AlbumMusicPlayer(playback: MusicPlaybackState) {
    val colors = ArchiveUiTheme.colorScheme
    var seeking by remember { mutableStateOf<Float?>(null) }
    val ready = playback.controller != null && playback.duration > 0
    val seekLabel = AppText.get(R.string.music_seek)
    Column(Modifier.fillMaxWidth().testTag("album-music-player"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(color = colors.dividerLine)
        Text(AppText.get(when {
            playback.failed -> R.string.music_error
            playback.buffering || playback.connecting -> R.string.music_loading
            playback.playing -> R.string.music_playing
            else -> R.string.music_paused
        }), color = colors.onSurfaceContainerVariant, fontSize = 12.sp,
            modifier = Modifier.testTag("music-status"))
        androidx.compose.material3.Slider(
            value = seeking ?: playback.position.toFloat().coerceIn(0f, playback.duration.toFloat().coerceAtLeast(1f)),
            onValueChange = { seeking = it },
            onValueChangeFinished = { seeking?.let { playback.seek(it.toLong()) }; seeking = null },
            valueRange = 0f..playback.duration.toFloat().coerceAtLeast(1f), enabled = ready,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = colors.primary, activeTrackColor = colors.primary,
                inactiveTrackColor = colors.onSurfaceContainerVariant.copy(alpha = .18f)),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = seekLabel }.testTag("music-seek"),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(musicTime((seeking?.toLong() ?: playback.position)), fontSize = 12.sp,
                color = colors.onSurfaceContainerVariant, modifier = Modifier.testTag("music-position"))
            Text(musicTime(playback.duration), fontSize = 12.sp, color = colors.onSurfaceContainerVariant,
                modifier = Modifier.testTag("music-duration"))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(AppText.get(R.string.music_back_ten), { playback.seek(playback.position - 10_000) },
                enabled = ready, modifier = Modifier.testTag("music-back"))
            Button(onClick = playback::toggle, enabled = playback.canPlay,
                modifier = Modifier.testTag("music-play-pause")) {
                Text(AppText.get(if (playback.playRequested) R.string.music_pause else R.string.music_play))
            }
            TextButton(AppText.get(R.string.music_forward_ten), { playback.seek(playback.position + 10_000) },
                enabled = ready, modifier = Modifier.testTag("music-forward"))
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(AppText.get(R.string.music_restart), playback::restart,
                enabled = ready, modifier = Modifier.testTag("music-restart"))
            TextButton(AppText.get(if (playback.repeat) R.string.music_repeat_one else R.string.music_repeat_off),
                playback::toggleRepeat, enabled = playback.controller != null, modifier = Modifier.testTag("music-repeat"))
            TextButton(AppText.get(R.string.music_speed, playback.speed), playback::cycleSpeed,
                enabled = playback.controller != null, modifier = Modifier.testTag("music-speed"))
            TextButton(AppText.get(R.string.music_stop), playback::stop,
                enabled = ready, modifier = Modifier.testTag("music-stop"))
        }
        if (playback.failed) TextButton(AppText.get(R.string.music_retry), playback::retry,
            modifier = Modifier.testTag("music-retry"))
    }
}

internal fun musicTime(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1000
    return "%d:%02d".format(java.util.Locale.ROOT, seconds / 60, seconds % 60)
}
