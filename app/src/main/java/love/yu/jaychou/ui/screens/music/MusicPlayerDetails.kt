package love.yu.jaychou.ui.screens.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import love.yu.jaychou.R
import love.yu.jaychou.data.AppText
import love.yu.jaychou.playback.EmbeddedMusicMetadata

import love.yu.jaychou.playback.MusicPlaybackState
import love.yu.jaychou.ui.components.CatalogImage
import love.yu.jaychou.ui.designsystem.Text
import love.yu.jaychou.ui.designsystem.TextButton
import love.yu.jaychou.ui.designsystem.WindowBottomSheet

@Composable
internal fun MusicPlayerDetails(
    playback: MusicPlaybackState, title: String, artist: String, palette: PlayerAtmosphere,
    artwork: ImageBitmap?,
    artworkVisible: () -> Boolean,
    onArtworkBounds: (Rect) -> Unit,
    onArtworkPageVisibility: (Boolean) -> Unit,
) {
    val pager = rememberPagerState(pageCount = { 2 })
    var showSettings by remember { mutableStateOf(false) }
    val ink = palette.ink
    val settingsLabel = AppText.get(R.string.music_playback_settings)
    LaunchedEffect(pager.currentPage) { onArtworkPageVisibility(pager.currentPage == 0) }
    Box(Modifier.fillMaxSize().testTag("music-player-details")) {
        PlayerAtmosphereBackground(palette)
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, color = ink, fontSize = 29.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(artist, color = ink.copy(alpha = .6f), fontSize = 15.sp)
                }
            }
            HorizontalPager(state = pager, beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxWidth().weight(1f).testTag("player-details-pager")
                    .semantics { stateDescription = AppText.get(if (pager.currentPage == 0) R.string.music_cover_page else R.string.music_lyrics_page) }) { page ->
                if (page == 0) BoxWithConstraints(Modifier.fillMaxSize().testTag("player-cover-page")) {
                    val landscape = maxWidth > maxHeight
                    val compact = maxHeight < 530.dp
                    val cover: @Composable (Modifier) -> Unit = { modifier ->
                        val frame = modifier.aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                            .onGloballyPositioned {
                                if (pager.currentPage == 0 && !pager.isScrollInProgress) onArtworkBounds(it.boundsInWindow())
                            }
                            .graphicsLayer { alpha = if (artworkVisible()) 1f else 0f }
                            .testTag("player-details-artwork")
                        if (artwork != null) Image(artwork, title, frame, contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                        else CatalogImage(playback.currentSong?.cover.orEmpty(), title, frame, crossfade = false, showStatus = false)
                    }
                    val controls: @Composable () -> Unit = {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(Modifier.clickable(onClickLabel = settingsLabel) { showSettings = true }
                                .semantics { contentDescription = settingsLabel }.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(playback.currentSong?.provider?.let(::providerLabel).orEmpty(), color = ink.copy(alpha = .65f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(AppText.get(R.string.online_stream), color = ink.copy(alpha = .65f), fontSize = 13.sp,
                                    modifier = Modifier.testTag("player-audio-format"))
                            }
                            PlayerTransport(playback, ink)
                            if (playback.failed) TextButton(AppText.get(R.string.music_retry), playback::retry)
                        }
                    }
                    if (landscape) Row(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { cover(Modifier.widthIn(max = 300.dp).fillMaxWidth()) }
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { controls() }
                    } else Column(Modifier.fillMaxSize().padding(horizontal = 28.dp)
                        .then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        if (!compact) Spacer(Modifier.weight(.7f)) else Spacer(Modifier.height(16.dp))
                        cover(Modifier.widthIn(max = if (compact) 230.dp else 380.dp).fillMaxWidth(.92f))
                        if (!compact) Spacer(Modifier.weight(.9f)) else Spacer(Modifier.height(24.dp))
                        controls()
                        Spacer(Modifier.height(6.dp))
                    }
                } else Box(Modifier.fillMaxSize().padding(horizontal = 30.dp)) {
                    EmbeddedLyricsView(playback.lyrics, playback, ink,
                        loading = playback.lyricsLoading, failed = playback.lyricsFailed,
                        visible = pager.currentPage == 1)
                }
            }

        }
    }
    WindowBottomSheet(showSettings, settingsLabel, onDismissRequest = { showSettings = false }) {
        TextButton(AppText.get(R.string.music_stop), { playback.stop(); showSettings = false }, modifier = Modifier.testTag("music-stop"))
        TextButton(AppText.get(if (playback.repeat) R.string.music_repeat_one else R.string.music_repeat_off),
            playback::toggleRepeat, modifier = Modifier.testTag("music-repeat"))
        TextButton(AppText.get(R.string.music_speed, playback.speed), playback::cycleSpeed, modifier = Modifier.testTag("music-speed"))
        TextButton(AppText.get(R.string.music_back_ten), { playback.seek(playback.position - 10_000) }, modifier = Modifier.testTag("music-back"))
        TextButton(AppText.get(R.string.music_forward_ten), { playback.seek(playback.position + 10_000) }, modifier = Modifier.testTag("music-forward"))
    }
}
