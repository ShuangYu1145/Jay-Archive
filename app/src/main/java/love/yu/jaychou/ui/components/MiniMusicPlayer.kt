package love.yu.jaychou.ui.components

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yu.jaychou.R
import love.yu.jaychou.data.AppText

import love.yu.jaychou.playback.MusicPlaybackState
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import love.yu.jaychou.ui.designsystem.IconButton
import love.yu.jaychou.ui.designsystem.Text
import love.yu.jaychou.ui.effects.LocalNavigationBackdrop
import love.yu.jaychou.ui.effects.LocalArchiveLiquidGlass
import love.yu.jaychou.ui.effects.liquid.liquidGlassBackground
import love.yu.jaychou.ui.effects.navigationSurfaceBlur

/** Keep the source artwork measured while the independent transition layer owns its pixels. */
internal val LocalMiniPlayerArtworkMoving = staticCompositionLocalOf { false }

/** A compact, shared-session player above the navigation, with a separate transport target. */
@Composable
internal fun MiniMusicPlayer(
    playback: MusicPlaybackState,
    title: String,
    artist: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    onArtworkBounds: (Rect) -> Unit = {},
) {
    val colors = ArchiveUiTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val backdrop = LocalNavigationBackdrop.current
    val density = LocalDensity.current
    val open by rememberUpdatedState(onOpen)
    val movingArtwork = LocalMiniPlayerArtworkMoving.current
    val liquid = Build.VERSION.SDK_INT >= 33 && backdrop != null && LocalArchiveLiquidGlass.current
    val surface = when {
        liquid -> Modifier.liquidGlassBackground(requireNotNull(backdrop), shape = shape)
        Build.VERSION.SDK_INT >= 33 && LocalArchiveBlur.current && backdrop != null ->
            Modifier.navigationSurfaceBlur(backdrop, shape, colors.surfaceContainer)
        else -> Modifier.background(colors.surfaceContainer, shape)
    }
    val openLabel = AppText.get(R.string.music_open_player_details)
    val toggleLabel = AppText.get(when {
        playback.failed -> R.string.music_retry
        playback.playRequested -> R.string.music_pause
        else -> R.string.music_play
    })
    Box(modifier.widthIn(max = 480.dp).fillMaxWidth().height(68.dp)
        .shadow(10.dp, shape, ambientColor = colors.onBackground.copy(alpha = .12f),
            spotColor = colors.onBackground.copy(alpha = .12f))
        .then(surface)
        .then(if (liquid) Modifier else Modifier.border(.5.dp, colors.onBackground.copy(alpha = .08f), shape))
        .clip(shape).clickable(onClickLabel = openLabel, onClick = onOpen)
        .pointerInput(density) {
            val velocity = VelocityTracker()
            var travel = 0f
            detectVerticalDragGestures(
                onDragStart = { travel = 0f; velocity.resetTracking() },
                onDragEnd = {
                    val distance = -travel
                    if (distance > 48.dp.toPx() ||
                        (distance > 16.dp.toPx() && velocity.calculateVelocity().y < -1_000f)) open()
                },
            ) { change, delta ->
                velocity.addPosition(change.uptimeMillis, change.position)
                travel += delta
                change.consume()
            }
        }
        .testTag("mini-music-player")) {
        Row(Modifier.fillMaxSize().padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CatalogImage(playback.currentSong?.cover.orEmpty(), title,
                Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                    .onGloballyPositioned { onArtworkBounds(it.boundsInWindow()) }
                    .graphicsLayer { alpha = if (movingArtwork) 0f else 1f }
                    .testTag("mini-player-artwork"),
                crossfade = false, showStatus = false)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceContainer, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("mini-player-title"))
                Text(artist, fontSize = 12.sp, color = colors.onSurfaceContainerVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { if (playback.failed) playback.retry() else playback.toggle() },
                modifier = Modifier.size(48.dp).semantics { contentDescription = toggleLabel }
                    .testTag("mini-player-toggle")) {
                Canvas(Modifier.size(22.dp)) {
                    if (playback.playRequested) {
                        val bar = size.width * .24f
                        for (left in listOf(size.width * .14f, size.width * .62f)) {
                            drawRoundRect(colors.onSurfaceContainer,
                                topLeft = Offset(left, size.height * .08f),
                                size = Size(bar, size.height * .84f), cornerRadius = CornerRadius(2.dp.toPx()))
                        }
                    } else {
                        drawPath(Path().apply {
                            moveTo(size.width * .2f, size.height * .05f)
                            lineTo(size.width * .9f, size.height * .5f)
                            lineTo(size.width * .2f, size.height * .95f)
                            close()
                        }, colors.onSurfaceContainer)
                    }
                }
            }
        }
        Canvas(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp)) {
            drawRect(colors.onSurfaceContainer.copy(alpha = .08f))
            val progress = if (playback.duration > 0)
                (playback.position.toFloat() / playback.duration).coerceIn(0f, 1f) else 0f
            drawRect(colors.primary, size = Size(size.width * progress, size.height))
        }
    }
}
