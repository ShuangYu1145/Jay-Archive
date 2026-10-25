package love.yu.jaychou.ui.screens.lyrics

import android.animation.ValueAnimator
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import love.yu.jaychou.R
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.components.ArchiveIconButton
import love.yu.jaychou.ui.components.BackButton
import love.yu.jaychou.ui.components.CatalogImage
import love.yu.jaychou.ui.designsystem.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Settings

@Composable
internal fun rememberReaderMotionDuration(): Int {
    val owner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) enabled = ValueAnimator.areAnimatorsEnabled()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    // Compose's duration scale also honors changes made while the activity is running.
    return if (enabled) 240 else 0
}

@Composable
internal fun LyricsReader(
    album: Album, track: Track, lines: List<LyricLine>, sourceLabel: String,
    settings: LyricsReadingSettings, palette: ReaderPalette, list: LazyListState,
    loading: Boolean, error: String?, translationAvailable: Boolean,
    query: String, onQuery: (String) -> Unit, searchOpen: Boolean, onCloseSearch: () -> Unit,
    matches: List<LyricsMatch>, activeMatch: Int, onMatch: (Int) -> Unit,
    onBack: () -> Unit, onImport: () -> Unit, onSettings: () -> Unit,
    onRetry: () -> Unit, onImmersive: (Boolean) -> Unit,
    onToggleTranslation: () -> Unit, coverModifier: Modifier, bodyModifier: Modifier,
    onCoverDetached: () -> Unit, footer: @Composable () -> Unit,
) {
    val duration = rememberReaderMotionDuration()
    val behavior = rememberArchiveScrollBehavior()
    val foreground = palette.foreground
    val fontSize by animateFloatAsState(settings.fontSize, tween(duration), label = "reader-font")
    val spacing by animateFloatAsState(settings.lineSpacing, tween(duration), label = "reader-spacing")
    val immersive = settings.immersive
    val currentToggle by rememberUpdatedState(onImmersive)
    val currentImmersive by rememberUpdatedState(immersive)
    val lineMatches = remember(matches) { matches.groupBy { it.lineIndex } }
    val selected = matches.getOrNull(activeMatch)
    var informationHeight by remember { mutableIntStateOf(0) }
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(palette.stops)).testTag("lyrics-reader")) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
            AnimatedVisibility(!immersive, enter = fadeIn(tween(duration)) + expandVertically(tween(duration)),
                exit = fadeOut(tween(duration)) + shrinkVertically(tween(duration))) {
                Column {
                    LyricsTopBar(onBack, onImport, onSettings, behavior, palette.stops.first())
                    AnimatedVisibility(searchOpen, enter = fadeIn(tween(duration)) + expandVertically(tween(duration)),
                        exit = fadeOut(tween(duration)) + shrinkVertically(tween(duration))) {
                        ReaderSearchBar(query, onQuery, onCloseSearch, matches.size, activeMatch, onMatch)
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                SelectionContainer {
                    LazyColumn(state = list,
                        modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()
                            .then(if (immersive) Modifier.statusBarsPadding() else Modifier)
                            .imePadding().nestedScroll(behavior.nestedScrollConnection)
                            .pointerInput(Unit) {
                                detectTapGestures(onDoubleTap = { currentToggle(!currentImmersive) })
                            }.testTag("lyrics-list"),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp,
                            top = if (immersive) 32.dp else 12.dp,
                            bottom = bottom + if (immersive) 100.dp else 48.dp),
                    ) {
                        item("song-information", contentType = "header") {
                            DisableSelection {
                                androidx.compose.animation.AnimatedVisibility(!immersive, enter = fadeIn(tween(duration)) + expandVertically(tween(duration)),
                                    exit = fadeOut(tween(duration)) + shrinkVertically(tween(duration))) {
                                    Column(Modifier.onSizeChanged { informationHeight = it.height }
                                        .graphicsLayer {
                                            // Read offsets during drawing, avoiding a recomposition on every scroll pixel.
                                            val fraction = if (list.firstVisibleItemIndex > 0) 1f else
                                                list.firstVisibleItemScrollOffset.toFloat() / informationHeight.coerceAtLeast(1)
                                            alpha = 1f - fraction.coerceIn(0f, 1f)
                                        }.padding(bottom = 32.dp)) {
                                        ReaderSongInformation(album, track, sourceLabel, translationAvailable,
                                            settings.showTranslation, onToggleTranslation, coverModifier,
                                            bodyModifier, onCoverDetached)
                                    }
                                }
                            }
                        }
                        when {
                            loading -> item("loading", contentType = "state") { DisableSelection {
                                ReaderStatus(loading = true, message = AppText.get(R.string.ui_finding_jay_chou_recordings_through_the_qq_music_lyrics_service),
                                    onRetry = onRetry, onImport = onImport, modifier = bodyModifier)
                            } }
                            error != null || lines.isEmpty() -> item("error", contentType = "state") { DisableSelection {
                                ReaderStatus(loading = false,
                                    message = error ?: AppText.get(R.string.ui_the_local_lyrics_file_has_no_displayable_lines),
                                    onRetry = onRetry, onImport = onImport, modifier = bodyModifier)
                            } }
                            else -> {
                                itemsIndexed(lines, key = { index, _ -> "line-$index" }, contentType = { _, _ -> "line" }) { index, line ->
                                    ReaderLyricLine(line, fontSize, spacing, settings, foreground,
                                        lineMatches[index].orEmpty(), selected, bodyModifier.testTag("lyric-line-$index"))
                                }
                                item("lyrics-footer", contentType = "footer") { DisableSelection {
                                    Column(bodyModifier.padding(top = 40.dp)) { footer() }
                                } }
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(immersive, Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp),
            enter = fadeIn(tween(duration)), exit = fadeOut(tween(duration))) {
            Card(cornerRadius = 24.dp, colors = CardDefaults.defaultColors(palette.stops.first().copy(alpha = .95f))) {
                TextButton(AppText.get(R.string.ui_reader_exit_immersive), { onImmersive(false) },
                    Modifier.heightIn(min = 48.dp).testTag("reader-exit-immersive"))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsTopBar(onBack: () -> Unit, onImport: () -> Unit, onSettings: () -> Unit,
                         behavior: ArchiveScrollBehavior, background: Color) {
    val actions: @Composable RowScope.() -> Unit = {
        TextButton(AppText.get(R.string.ui_import), onImport, Modifier.testTag("lyrics-import"))
        ArchiveIconButton(MiuixIcons.Settings, AppText.get(R.string.ui_reader_settings), onSettings,
            Modifier.testTag("lyrics-reading-settings"))
    }
    val back: @Composable () -> Unit = { BackButton(onBack, AppText.get(R.string.ui_back_to_album_details)) }
    if (ArchiveUiTheme.isMaterial) androidx.compose.material3.TopAppBar(
        title = { Text(AppText.get(R.string.ui_lyrics), fontWeight = FontWeight.SemiBold) },
        navigationIcon = back, actions = actions, scrollBehavior = behavior.material,
        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = background,
            scrolledContainerColor = background), modifier = Modifier.testTag("lyrics-top-bar"))
    else top.yukonga.miuix.kmp.basic.SmallTopAppBar(
        title = AppText.get(R.string.ui_lyrics), navigationIcon = back, actions = actions,
        scrollBehavior = behavior.miuix, color = background,
        modifier = Modifier.testTag("lyrics-top-bar"), defaultWindowInsetsPadding = true)
}

@Composable
private fun ReaderSongInformation(album: Album, track: Track, source: String, translationAvailable: Boolean,
                                  showTranslation: Boolean, onTranslation: () -> Unit,
                                  coverModifier: Modifier, bodyModifier: Modifier, onCoverDetached: () -> Unit) {
    val detached by rememberUpdatedState(onCoverDetached)
    DisposableEffect(album.id, track.number) { onDispose { detached() } }
    val colors = ArchiveUiTheme.colorScheme
    Card(Modifier.fillMaxWidth().testTag("lyrics-song-card"), cornerRadius = 24.dp,
        insideMargin = PaddingValues(20.dp), colors = CardDefaults.defaultColors(colors.surfaceContainer.copy(alpha = .76f))) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CatalogImage(track.artwork?.coverUrl ?: album.coverUrl, track.artwork?.coverAsset ?: album.coverAsset,
                AppText.get(R.string.ui_album_artwork, track.title),
                Modifier.size(72.dp).then(coverModifier).clip(RoundedCornerShape(16.dp)).testTag("lyrics-cover"), showStatus = false)
            Column(Modifier.weight(1f).then(bodyModifier), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(track.title, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() })
                Text(track.artistLabel, fontSize = 14.sp, lineHeight = 20.sp,
                    color = colors.onSurfaceContainerVariant)
                Text(album.title, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = colors.onSurfaceContainerVariant)
            }
        }
        Row(bodyModifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(source, Modifier.weight(1f), fontSize = 12.sp, lineHeight = 18.sp,
                color = colors.onSurfaceContainerVariant)
            if (translationAvailable) TextButton(
                AppText.get(if (showTranslation) R.string.ui_bilingual else R.string.ui_original), onTranslation,
                Modifier.semantics { contentDescription = AppText.get(R.string.ui_toggle_lyric_translations) })
        }
    }
}

@Composable
private fun ReaderLyricLine(line: LyricLine, size: Float, spacing: Float, settings: LyricsReadingSettings,
                            color: Color, matches: List<LyricsMatch>, active: LyricsMatch?, modifier: Modifier) {
    if (line.text.isBlank()) {
        val height = with(LocalDensity.current) { (size * spacing).sp.toDp() }
        Spacer(modifier.height(height))
        return
    }
    val colors = ArchiveUiTheme.colorScheme
    fun highlighted(text: String, translation: Boolean) = AnnotatedString.Builder(text).apply {
        matches.filter { it.translation == translation }.forEach {
            addStyle(SpanStyle(background = if (it == active) colors.primaryContainer else colors.primary.copy(alpha = .12f),
                color = if (it == active) colors.onPrimaryContainer else color,
                fontWeight = if (it == active) FontWeight.SemiBold else FontWeight.Normal), it.start, it.end)
        }
    }.toAnnotatedString()
    val style = ArchiveUiTheme.textStyles.main.copy(fontSize = size.sp, lineHeight = (size * spacing).sp,
        fontWeight = FontWeight.Normal, textAlign = settings.alignment.textAlign(), color = color)
    Column(modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        BasicText(highlighted(line.text, false), Modifier.fillMaxWidth(), style = style)
        if (settings.showTranslation && line.translation != null) {
            BasicText(highlighted(line.translation, true), Modifier.fillMaxWidth().padding(top = 4.dp),
                style = style.copy(fontSize = (size * .72f).sp, lineHeight = (size * .72f * spacing).sp,
                    color = color.copy(alpha = .68f)))
        }
    }
}

@Composable
private fun ReaderSearchBar(query: String, onQuery: (String) -> Unit, onClose: () -> Unit,
                            count: Int, active: Int, onMatch: (Int) -> Unit) {
    val colors = ArchiveUiTheme.colorScheme
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 8.dp).testTag("lyrics-search")) {
        Row(Modifier.fillMaxWidth().background(colors.surfaceContainerHigh, RoundedCornerShape(18.dp)).padding(start = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(query, onQuery, singleLine = true,
                textStyle = ArchiveUiTheme.textStyles.main.copy(fontSize = 16.sp, color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).wrapContentHeight(Alignment.CenterVertically)
                    .focusRequester(focus).testTag("lyrics-search-input")
                    .semantics { contentDescription = AppText.get(R.string.ui_reader_search_hint) },
                decorationBox = { input ->
                    Box { if (query.isEmpty()) Text(AppText.get(R.string.ui_reader_search_hint), fontSize = 16.sp,
                        color = colors.onSurfaceContainerVariant); input() }
                })
            ArchiveIconButton(MiuixIcons.Close, AppText.get(R.string.ui_reader_close_search), { keyboard?.hide(); onClose() })
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text(when {
                query.isBlank() -> AppText.get(R.string.ui_reader_search_empty)
                count == 0 -> AppText.get(R.string.ui_reader_no_matches)
                else -> AppText.get(R.string.ui_reader_match_count, active + 1, count)
            }, Modifier.align(Alignment.CenterVertically).semantics { liveRegion = LiveRegionMode.Polite }
                .testTag("lyrics-search-count"), fontSize = 12.sp, color = colors.onSurfaceContainerVariant)
            TextButton(AppText.get(R.string.ui_reader_previous), { onMatch((active - 1 + count) % count) },
                Modifier.testTag("lyrics-search-previous"), enabled = count > 0)
            TextButton(AppText.get(R.string.ui_reader_next), { onMatch((active + 1) % count) },
                Modifier.testTag("lyrics-search-next"), enabled = count > 0)
        }
    }
}

@Composable
private fun ReaderStatus(loading: Boolean, message: String, onRetry: () -> Unit, onImport: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 36.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        if (loading) CircularProgressIndicator()
        Text(AppText.get(if (loading) R.string.ui_finding_lyrics else R.string.ui_lyrics_unavailable),
            fontSize = 21.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag(if (loading) "lyrics-loading" else "lyrics-error"))
        Text(message, fontSize = 14.sp, lineHeight = 23.sp, color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
        if (!loading) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onRetry, Modifier.testTag("lyrics-retry")) { Text(AppText.get(R.string.ui_retry)) }
            TextButton(AppText.get(R.string.ui_import_local_file), onImport)
        }
    }
}
