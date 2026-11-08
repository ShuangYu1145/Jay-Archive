package love.yu.jaychou.ui.screens.music

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yu.jaychou.R
import love.yu.jaychou.data.AppText
import love.yu.jaychou.playback.MusicPlaybackState
import love.yu.jaychou.ui.designsystem.Text

internal enum class PlayerSymbol { PLAY, PAUSE, PREVIOUS, NEXT, STOP, DOWN }

@Composable
internal fun PlayerSymbolButton(symbol: PlayerSymbol, label: String, ink: Color, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, buttonSize: Dp = 48.dp, iconSize: Dp = 24.dp) {
    Box(modifier.size(buttonSize).clickable(interactionSource = null, indication = null,
        enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(iconSize)) {
            val color = ink.copy(alpha = if (enabled) 1f else .25f)
            fun triangle(left: Float, right: Float, reversed: Boolean = false) {
                drawPath(Path().apply {
                    moveTo(size.width * (if (reversed) right else left), size.height * .08f)
                    lineTo(size.width * (if (reversed) left else right), size.height * .5f)
                    lineTo(size.width * (if (reversed) right else left), size.height * .92f)
                    close()
                }, color)
            }
            when (symbol) {
                PlayerSymbol.PLAY -> triangle(.2f, .88f)
                PlayerSymbol.PAUSE -> for (x in listOf(.15f, .57f)) {
                    drawRoundRect(color, Offset(size.width * x, 0f), Size(size.width * .28f, size.height), CornerRadius(size.width * .07f))
                }
                PlayerSymbol.STOP -> drawRect(color, Offset(size.width * .23f, size.height * .23f), Size(size.width * .54f, size.height * .54f))
                PlayerSymbol.PREVIOUS -> {
                    triangle(.22f, .92f, true)
                    drawRoundRect(color, Offset(0f, 0f), Size(size.width * .13f, size.height), CornerRadius(size.width * .04f))
                }
                PlayerSymbol.NEXT -> {
                    triangle(.08f, .78f)
                    drawRoundRect(color, Offset(size.width * .87f, 0f), Size(size.width * .13f, size.height), CornerRadius(size.width * .04f))
                }
                PlayerSymbol.DOWN -> drawPath(Path().apply {
                    moveTo(0f, size.height * .25f); lineTo(size.width * .5f, size.height * .7f)
                    lineTo(size.width, size.height * .25f)
                }, color, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
        }
    }
}

@Composable
internal fun PlayerTransport(playback: MusicPlaybackState, ink: Color) {
    var scrubbing by remember { mutableStateOf<Float?>(null) }
    val ready = playback.controller != null && playback.duration > 0
    val maximum = playback.duration.toFloat().coerceAtLeast(1f)
    val value = scrubbing ?: playback.position.toFloat().coerceIn(0f, maximum)
    val seekLabel = AppText.get(R.string.music_seek)
    Column(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(48.dp)
            .semantics {
                contentDescription = seekLabel
                progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..maximum)
                if (!ready) disabled()
                setProgress { target ->
                    if (!ready || !target.isFinite()) false
                    else { playback.seek(target.coerceIn(0f, maximum).toLong()); true }
                }
            }
            .pointerInput(ready, maximum) {
                if (ready) detectTapGestures { point ->
                    playback.seek((point.x / size.width * maximum).coerceIn(0f, maximum).toLong())
                }
            }
            .pointerInput(ready, maximum) {
                if (ready) detectHorizontalDragGestures(
                    onDragStart = { scrubbing = (it.x / size.width * maximum).coerceIn(0f, maximum) },
                    onDragEnd = { scrubbing?.let { playback.seek(it.toLong()) }; scrubbing = null },
                    onDragCancel = { scrubbing = null },
                ) { change, _ ->
                    change.consume()
                    scrubbing = (change.position.x / size.width * maximum).coerceIn(0f, maximum)
                }
            }.testTag("music-seek")) {
            val y = size.height / 2f
            val inset = 2.dp.toPx()
            val end = size.width - inset
            val progress = value / maximum
            drawLine(ink.copy(alpha = .22f), Offset(inset, y), Offset(end, y),
                strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(ink, Offset(inset, y), Offset(inset + (end - inset) * progress, y),
                strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(playerClock(value.toLong()), fontSize = 14.sp, color = ink.copy(alpha = .65f), modifier = Modifier.testTag("music-position"))
            Text(playerClock(playback.duration), fontSize = 14.sp, color = ink.copy(alpha = .65f), modifier = Modifier.testTag("music-duration"))
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            PlayerSymbolButton(PlayerSymbol.PREVIOUS, AppText.get(R.string.music_restart), ink,
                playback::previous, enabled = ready, buttonSize = 64.dp, iconSize = 24.dp, modifier = Modifier.testTag("music-restart"))
            PlayerSymbolButton(if (playback.playRequested) PlayerSymbol.PAUSE else PlayerSymbol.PLAY,
                AppText.get(if (playback.playRequested) R.string.music_pause else R.string.music_play), ink,
                playback::toggle, enabled = playback.canPlay, buttonSize = 80.dp, iconSize = 32.dp,
                modifier = Modifier.testTag("music-play-pause"))
            PlayerSymbolButton(PlayerSymbol.NEXT, AppText.get(R.string.online_next), ink, playback::next, enabled = playback.hasNext,
                buttonSize = 64.dp, iconSize = 24.dp, modifier = Modifier.testTag("music-next"))
        }
    }
}

private fun playerClock(position: Long): String {
    val seconds = position.coerceAtLeast(0) / 1000
    return "%02d:%02d".format(java.util.Locale.ROOT, seconds / 60, seconds % 60)
}
