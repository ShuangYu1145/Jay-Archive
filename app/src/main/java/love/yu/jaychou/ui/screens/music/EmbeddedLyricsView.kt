package love.yu.jaychou.ui.screens.music

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import love.yu.jaychou.R
import love.yu.jaychou.data.AppText
import love.yu.jaychou.playback.karaokeGlyphs
import love.yu.jaychou.playback.EmbeddedLyricLine
import love.yu.jaychou.playback.MusicPlaybackState
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import kotlin.math.abs

@Composable
internal fun EmbeddedLyricsView(lines: List<EmbeddedLyricLine>, playback: MusicPlaybackState,
    ink: Color, loading: Boolean, failed: Boolean, visible: Boolean) {
    if (lines.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp).testTag("embedded-lyrics-empty"), contentAlignment = Alignment.Center) {
            Text(AppText.get(when {
                loading -> R.string.music_lyrics_loading
                failed -> R.string.music_lyrics_error
                else -> R.string.music_lyrics_empty
            }), color = ink.copy(alpha = .65f), lineHeight = 26.sp)
        }
        return
    }
    val frameTick = remember { mutableLongStateOf(0L) }
    LaunchedEffect(visible, playback.playing) {
        if (visible && playback.playing) while (true) withFrameNanos { frameTick.longValue = it }
    }
    val framePosition = { frameTick.longValue; playback.framePosition() }
    val list = rememberLazyListState()
    val dragging by list.interactionSource.collectIsDraggedAsState()
    var following by remember { mutableStateOf(true) }
    val timed = lines.any { it.startMs != null }
    val active = if (timed) lines.indexOfLast { it.startMs != null && it.startMs <= playback.position } else -1
    LaunchedEffect(dragging) {
        if (dragging) following = false
        else { delay(3_000); following = true }
    }
    LaunchedEffect(active, following, visible) {
        if (active >= 0 && following && visible) {
            if (list.layoutInfo.visibleItemsInfo.none { it.index == active }) list.scrollToItem(active)
            val item = list.layoutInfo.visibleItemsInfo.firstOrNull { it.index == active }
            if (item != null) {
                val center = (list.layoutInfo.viewportStartOffset + list.layoutInfo.viewportEndOffset) / 2f
                list.animateScrollBy(item.offset + item.size / 2f - center)
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().testTag("embedded-lyrics-view")) {
        LazyColumn(state = list, modifier = Modifier.fillMaxSize().testTag("embedded-lyrics-list"),
            contentPadding = PaddingValues(top = maxHeight * .4f, bottom = maxHeight * .4f),
            verticalArrangement = Arrangement.spacedBy(30.dp)) {
            itemsIndexed(lines) { index, line ->
                val current = index == active
                val distance = if (active < 0) 0 else abs(index - active)
                val blur = if (timed && !current && active >= 0) (distance * .8f).coerceAtMost(3f).dp else 0.dp
                KaraokeLyricText(line, current, timed, active >= 0, ink, framePosition,
                    modifier = Modifier.fillMaxWidth()
                        .then(if (Build.VERSION.SDK_INT >= 31 && blur > 0.dp) Modifier.blur(blur) else Modifier)
                        .clickable(enabled = line.startMs != null) { line.startMs?.let { playback.seek(it); following = true } }
                        .semantics { selected = current }.testTag("embedded-lyric-$index"))
            }
        }
    }
}


/** Keep text layout cached and paint a continuously moving mask through each glyph. */
@Composable
internal fun KaraokeLyricText(line: EmbeddedLyricLine, current: Boolean, timed: Boolean,
    hasActive: Boolean, ink: Color, position: () -> Long, modifier: Modifier = Modifier) {
    var layout by remember(line.text) { mutableStateOf<TextLayoutResult?>(null) }
    val glyphs = remember(line) { karaokeGlyphs(line) }
    val baseAlpha = when {
        !timed || !hasActive -> .65f
        current && glyphs.isEmpty() -> 1f
        current -> .3f
        else -> .28f
    }
    Text(line.text, color = ink.copy(alpha = baseAlpha),
        style = ArchiveUiTheme.textStyles.main.copy(fontSize = 29.sp, lineHeight = 43.sp, fontWeight = FontWeight.Bold),
        onTextLayout = { layout = it }, modifier = modifier.drawWithContent {
            drawContent()
            val measured = layout
            if (current && measured != null && glyphs.isNotEmpty()) {
                val now = position()
                val mask = Path()
                for (glyph in glyphs) {
                    val fraction = glyph.progress(now)
                    if (fraction <= 0f) continue
                    val box = measured.getBoundingBox(glyph.startOffset)
                    val rtl = measured.getBidiRunDirection(glyph.startOffset) == ResolvedTextDirection.Rtl
                    mask.addRect(if (rtl) Rect(box.right - box.width * fraction, box.top, box.right, box.bottom)
                        else Rect(box.left, box.top, box.left + box.width * fraction, box.bottom))
                }
                clipPath(mask) { drawText(measured, color = ink) }
            }
        })
}
