package love.yu.jaychou.ui.screens.lyrics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import love.yu.jaychou.R
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.components.SourceLink
import love.yu.jaychou.ui.designsystem.*

/** Reading state is separate from acquisition; the existing repository remains authoritative. */
@Composable
fun LyricsScreen(
    album: Album, track: Track, onBack: () -> Unit,
    coverModifier: Modifier = Modifier, bodyModifier: Modifier = Modifier,
    onCoverDetached: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saveScope = LocalLifecycleOwner.current.lifecycleScope
    val repository = remember(context) { SettingsRepository(context.applicationContext) }
    val stored by remember(repository) { repository.lyricsReadingSettings.map { it as LyricsReadingSettings? } }
        .collectAsStateWithLifecycle(initialValue = null)
    var preview by remember { mutableStateOf<LyricsReadingSettings?>(null) }
    val reading = preview ?: stored ?: LyricsReadingSettings()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var activeMatch by rememberSaveable { mutableIntStateOf(0) }
    val list = rememberLazyListState()
    val duration = rememberReaderMotionDuration()
    var retry by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var online by remember { mutableStateOf<OnlineLyrics?>(null) }
    var localText by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun changeReading(value: LyricsReadingSettings, persist: Boolean) {
        preview = value.normalized()
        if (persist) saveScope.launch {
            try { repository.setLyricsReadingSettings(value) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: java.io.IOException) {
                Toast.makeText(context, AppText.get(R.string.ui_reader_save_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

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
            android.util.Log.w("JayLyrics", failure.javaClass.simpleName + "; cause=" + failure.cause?.javaClass?.simpleName)
            error = when (failure) {
                is LyricsUnavailableException -> failure.message ?: AppText.get(R.string.ui_unable_to_get_lyrics_please_try_again_later)
                else -> AppText.get(R.string.ui_unable_to_connect_to_the_lyrics_service_check_your_connection_an)
            }
        } finally { loading = false }
    }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                localText = LyricsRepository.import(context, album, track, uri)
                online = null
                error = null
                // Cancel an older lookup and reload the imported text through the existing effect.
                retry++
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
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
                .dropWhile { it.text.isBlank() }.dropLastWhile { it.text.isBlank() }
        } ?: online?.lines.orEmpty()
    }
    val translationAvailable = localText == null && online?.hasTranslation == true
    val includeTranslation = translationAvailable && reading.showTranslation
    val sourceLabel = when {
        localText != null -> AppText.get(R.string.ui_local_lyrics_saved_in_app_storage)
        loading -> AppText.get(R.string.ui_matching_qq_music_lyrics)
        online != null -> AppText.get(R.string.ui_qq_music_lyrics_matched)
        else -> AppText.get(R.string.ui_qq_music_no_match_yet)
    }
    val displayError = error ?: if (!loading && lines.isEmpty()) AppText.get(
        if (localText != null) R.string.ui_the_local_lyrics_file_has_no_displayable_lines
        else R.string.ui_qq_music_has_no_lyrics_for_this_song) else null
    val matches = remember(lines, query, includeTranslation) { searchLyrics(lines, query, includeTranslation) }
    val selected = activeMatch.coerceIn(0, (matches.size - 1).coerceAtLeast(0))
    val palette = rememberReaderPalette(album, track, reading.background)
    LaunchedEffect(query, includeTranslation) { activeMatch = 0 }
    LaunchedEffect(matches, selected, searchOpen) {
        if (searchOpen && matches.isNotEmpty()) {
            val index = matches[selected].lineIndex + 1
            if (duration == 0) list.scrollToItem(index) else list.animateScrollToItem(index)
        }
    }
    BackHandler(searchOpen && !reading.immersive && !showSettings) { searchOpen = false; query = "" }

    LyricsReader(
        album = album, track = track, lines = lines, sourceLabel = sourceLabel,
        settings = reading, palette = palette, list = list, loading = loading, error = displayError,
        translationAvailable = translationAvailable,
        query = query, onQuery = { query = it }, searchOpen = searchOpen,
        onCloseSearch = { searchOpen = false; query = "" },
        matches = matches, activeMatch = selected, onMatch = { activeMatch = it },
        onBack = onBack, onImport = pickFile, onSettings = { showSettings = true }, onRetry = { retry++ },
        onImmersive = { changeReading(reading.copy(immersive = it), true) },
        onToggleTranslation = { changeReading(reading.copy(showTranslation = !reading.showTranslation), true) },
        coverModifier = coverModifier, bodyModifier = bodyModifier, onCoverDetached = onCoverDetached,
    ) {
        LyricsFooter(localText != null, online, onRemoveLocal = {
            scope.launch {
                try {
                    LyricsRepository.removeLocal(context, album, track)
                    localText = null; online = null; retry++
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { error = AppText.get(R.string.ui_unable_to_delete_please_try_again) }
            }
        })
    }
    LyricsReadingSheet(showSettings, reading, translationAvailable, lines.isNotEmpty(), ::changeReading,
        onCommit = { preview?.let { changeReading(it, true) } },
        onDismiss = { preview?.let { changeReading(it, true) }; showSettings = false },
        onCopy = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(track.title, copyableLyrics(lines, includeTranslation)))
            Toast.makeText(context, AppText.get(R.string.ui_reader_copied), Toast.LENGTH_SHORT).show()
        },
        onSearch = {
            showSettings = false
            if (reading.immersive) changeReading(reading.copy(immersive = false), true)
            searchOpen = true
        })
}

@Composable
private fun LyricsFooter(local: Boolean, online: OnlineLyrics?, onRemoveLocal: () -> Unit) {
    val colors = ArchiveUiTheme.colorScheme
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(color = colors.dividerLine.copy(alpha = .4f))
        Text(AppText.get(if (local) R.string.ui_local_lyrics_saved_in_app_storage_193
            else R.string.ui_qq_music_lyrics_read_from_the_lyrics_service), fontSize = 12.sp,
            lineHeight = 20.sp, color = colors.onSurfaceContainerVariant)
        if (!local && online != null) Text(
            AppText.get(R.string.ui_qq_music_album, online.album.ifBlank { AppText.get(R.string.ui_album_not_listed) }) +
                if (online.releaseDate.isBlank()) "" else " · " + online.releaseDate,
            fontSize = 12.sp, lineHeight = 20.sp, color = colors.onSurfaceContainerVariant)
        SourceLink(AppText.get(R.string.ui_lyrics_service_vkeys_api_qq_music), LyricsRepository.source)
        Text(AppText.get(R.string.ui_lyrics_belong_to_their_rights_holders_this_page_is_for_reading_o),
            fontSize = 12.sp, lineHeight = 20.sp, color = colors.onSurfaceContainerVariant)
        if (local) TextButton(AppText.get(R.string.ui_delete_local_lyrics), onRemoveLocal)
    }
}
