package love.yu.jaychou.ui.screens.lyrics

import love.yu.jaychou.ui.components.AlbumCoverShape
import love.yu.jaychou.ui.components.ArchiveScaffold
import love.yu.jaychou.ui.components.BackButton
import love.yu.jaychou.ui.components.QuietText
import love.yu.jaychou.ui.components.SourceLink
import love.yu.jaychou.data.AppText
import love.yu.jaychou.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.designsystem.*
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun LyricsScreen(album: Album, track: Track, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var retry by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var online by remember { mutableStateOf<OnlineLyrics?>(null) }
    var localText by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showTranslation by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(album.id, track.number, retry) {
        loading = true
        error = null
        online = null
        try {
            localText = LyricsRepository.local(context, album, track)
            if (localText == null) online = LyricsRepository.fetch(album, track)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            // Diagnostic types only: never write lyrics or user-imported text to Logcat.
            android.util.Log.w("JayLyrics", "${failure.javaClass.simpleName}; cause=${failure.cause?.javaClass?.simpleName}")
            error = when (failure) {
                is LyricsUnavailableException -> failure.message ?: AppText.get(R.string.ui_unable_to_get_lyrics_please_try_again_later)
                else -> AppText.get(R.string.ui_unable_to_connect_to_the_lyrics_service_check_your_connection_an)
            }
        } finally {
            loading = false
        }
    }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                localText = LyricsRepository.import(context, album, track, uri)
                online = null
                error = null
            } catch (_: Exception) {
                error = AppText.get(R.string.ui_import_failed_use_a_utf_8_or_gb18030_lrc_txt_file_no_larger_than)
            }
        }
    }
    val pickFile: () -> Unit = {
        importer.launch(arrayOf("text/*", "application/octet-stream", "application/x-subrip"))
    }

    val lines = remember(localText, online) {
        localText?.let { text ->
            LyricsRepository.lines(text).map { LyricLine(null, it) }
                .dropWhile { it.text.isBlank() }
                .dropLastWhile { it.text.isBlank() }
        } ?: online?.lines.orEmpty()
    }
    val translationAvailable = localText == null && online?.hasTranslation == true
    val sourceLabel = when {
        localText != null -> AppText.get(R.string.ui_local_lyrics_saved_in_app_storage)
        loading -> AppText.get(R.string.ui_matching_qq_music_lyrics)
        online != null -> AppText.get(R.string.ui_qq_music_lyrics_matched)
        else -> AppText.get(R.string.ui_qq_music_no_match_yet)
    }

    ArchiveScaffold(
        AppText.get(R.string.ui_lyrics), small = true,
        navigationIcon = { BackButton(onBack, AppText.get(R.string.ui_back_to_album_details)) },
        actions = {
            TextButton(AppText.get(R.string.ui_import), onClick = pickFile, modifier = Modifier.testTag("lyrics-import"))
        },
    ) { padding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 680.dp).fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = padding.calculateTopPadding() + 8.dp,
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Card(modifier = Modifier.fillMaxWidth().testTag("lyrics-song-card"),
                    cornerRadius = 20.dp, insideMargin = PaddingValues(0.dp)) {
                    LyricsHeader(
                        album = album, track = track, sourceLabel = sourceLabel,
                        translationAvailable = translationAvailable, showTranslation = showTranslation,
                        onToggleTranslation = { showTranslation = !showTranslation },
                    )
                }
                Card(
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("lyrics-card"),
                    cornerRadius = 20.dp, insideMargin = PaddingValues(0.dp),
                ) {
                    when {
                        loading -> LyricsLoading()
                        error != null -> LyricsError(error!!, onRetry = { retry++ }, onImport = pickFile)
                        lines.isEmpty() -> LyricsError(
                            if (localText != null) AppText.get(R.string.ui_the_local_lyrics_file_has_no_displayable_lines)
                            else AppText.get(R.string.ui_qq_music_has_no_lyrics_for_this_song),
                            onRetry = { retry++ }, onImport = pickFile,
                            title = AppText.get(R.string.ui_no_lyrics_yet),
                        )
                        else -> LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().testTag("lyrics-list"),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 26.dp),
                        ) {
                            itemsIndexed(lines, key = { index, _ -> "line-$index" }) { _, line ->
                                LyricLineView(line, showTranslation && translationAvailable)
                            }
                            item("lyrics-footer") {
                                LyricsFooter(
                                    local = localText != null, online = online,
                                    onRemoveLocal = {
                                        scope.launch {
                                            try {
                                                LyricsRepository.removeLocal(context, album, track)
                                                localText = null
                                                online = null
                                                retry++
                                            } catch (_: Exception) {
                                                error = AppText.get(R.string.ui_unable_to_delete_please_try_again)
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsHeader(
    album: Album,
    track: Track,
    sourceLabel: String,
    translationAvailable: Boolean,
    showTranslation: Boolean,
    onToggleTranslation: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(AppText.get(R.string.ui_lyrics), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                color = ArchiveUiTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            if (translationAvailable) {
                TranslationChip(active = showTranslation, onClick = onToggleTranslation)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AlbumThumbnail(album)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(track.title, fontSize = 23.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() })
                QuietText(AppText.get(R.string.ui_jay_chou_184, album.title), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(sourceLabel, fontSize = 12.sp, color = ArchiveUiTheme.colorScheme.primary,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AlbumThumbnail(album: Album) {
    val context = LocalContext.current
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context).data(album.coverUrl).crossfade(true).build(),
        contentDescription = AppText.get(R.string.ui_album_artwork_185, album.title),
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(76.dp).clip(AlbumCoverShape)
            .background(ArchiveUiTheme.colorScheme.surfaceContainerHigh),
        loading = { AlbumThumbnailFallback(album) },
        error = { AlbumThumbnailFallback(album) },
    )
}

@Composable
private fun AlbumThumbnailFallback(album: Album) {
    AsyncImage("file:///android_asset/${album.coverAsset}", null,
        Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
}

@Composable
private fun TranslationChip(active: Boolean, onClick: () -> Unit) {
    val background = if (active) ArchiveUiTheme.colorScheme.primaryContainer else ArchiveUiTheme.colorScheme.surfaceContainerHigh
    val foreground = if (active) ArchiveUiTheme.colorScheme.onPrimaryContainer else ArchiveUiTheme.colorScheme.onSurfaceContainerVariant
    Card(
        onClick = onClick,
        insideMargin = PaddingValues(horizontal = 13.dp, vertical = 7.dp),
        colors = CardDefaults.defaultColors(color = background, contentColor = foreground),
        pressFeedbackType = PressFeedbackType.Sink,
    ) {
        Text(
            if (active) AppText.get(R.string.ui_bilingual) else AppText.get(R.string.ui_original), fontSize = 12.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.semantics { contentDescription = AppText.get(R.string.ui_toggle_lyric_translations) },
        )
    }
}

@Composable
private fun LyricsLoading() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(18.dp))
        Text(AppText.get(R.string.ui_finding_lyrics), fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("lyrics-loading"))
        Spacer(Modifier.height(6.dp))
        Text(AppText.get(R.string.ui_finding_jay_chou_recordings_through_the_qq_music_lyrics_service), fontSize = 13.sp, lineHeight = 20.sp,
            color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LyricsError(
    message: String,
    onRetry: () -> Unit,
    onImport: () -> Unit,
    title: String = AppText.get(R.string.ui_lyrics_unavailable),
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(9.dp))
        Text(message, fontSize = 14.sp, lineHeight = 22.sp,
            color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRetry, modifier = Modifier.testTag("lyrics-retry")) { Text(AppText.get(R.string.ui_retry)) }
            TextButton(AppText.get(R.string.ui_import_local_file), onClick = onImport)
        }
    }
}

@Composable
private fun LyricLineView(line: LyricLine, showTranslation: Boolean) {
    if (line.text.isBlank()) {
        Spacer(Modifier.height(12.dp))
    } else {
        Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            Text(line.text, fontSize = 20.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium)
            if (showTranslation && line.translation != null) {
                Spacer(Modifier.height(4.dp))
                Text(line.translation, fontSize = 15.sp, lineHeight = 24.sp,
                    color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
            }
        }
    }
}

@Composable
private fun LyricsFooter(local: Boolean, online: OnlineLyrics?, onRemoveLocal: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
        HorizontalDivider(color = ArchiveUiTheme.colorScheme.dividerLine)
        Spacer(Modifier.height(14.dp))
        Text(
            if (local) AppText.get(R.string.ui_local_lyrics_saved_in_app_storage_193) else AppText.get(R.string.ui_qq_music_lyrics_read_from_the_lyrics_service),
            fontSize = 13.sp, lineHeight = 20.sp, color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant,
        )
        if (!local && online != null) {
            Spacer(Modifier.height(3.dp))
            Text(
                AppText.get(R.string.ui_qq_music_album, online.album.ifBlank { AppText.get(R.string.ui_album_not_listed) }) +
                    if (online.releaseDate.isBlank()) "" else " · ${online.releaseDate}",
                fontSize = 12.sp, lineHeight = 19.sp, color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant,
            )
        }
        SourceLink(AppText.get(R.string.ui_lyrics_service_vkeys_api_qq_music), LyricsRepository.source)
        Text(
            AppText.get(R.string.ui_lyrics_belong_to_their_rights_holders_this_page_is_for_reading_o),
            fontSize = 12.sp, lineHeight = 19.sp, color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant,
        )
        if (local) {
            Spacer(Modifier.height(4.dp))
            TextButton(AppText.get(R.string.ui_delete_local_lyrics), onClick = onRemoveLocal)
        }
    }
}
