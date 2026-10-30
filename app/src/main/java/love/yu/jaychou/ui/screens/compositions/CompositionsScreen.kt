package love.yu.jaychou.ui.screens.compositions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import love.yu.jaychou.R
import love.yu.jaychou.data.*
import love.yu.jaychou.data.Composition
import love.yu.jaychou.ui.components.*
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import love.yu.jaychou.ui.designsystem.Card
import love.yu.jaychou.ui.designsystem.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun CompositionsScreen(layout: AlbumLayout, bottom: Dp, active: Boolean, onDetailVisibility: (Boolean) -> Unit) {
    val context = LocalContext.current.applicationContext
    val focus = LocalFocusManager.current
    LaunchedEffect(active) { if (!active) focus.clearFocus() }
    val language = AppLanguage.contentTag(LocalConfiguration.current.locales)
    var catalog by remember { mutableStateOf<CompositionCatalog?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    LaunchedEffect(context) {
        try {
            catalog = withContext(Dispatchers.IO) { CompositionRepository.load(context) }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) { loadFailed = true }
    }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = catalog?.songs?.find { it.id == selectedId }
    val callback by rememberUpdatedState(onDetailVisibility)
    LaunchedEffect(selected) { callback(selected != null) }
    DisposableEffect(Unit) { onDispose { callback(false) } }
    BackHandler(active && (selected != null || searching)) {
        if (selected != null) selectedId = null else { searching = false; query = "" }
    }
    val songs = remember(catalog, query) { catalog?.songs.orEmpty().filter { it.matches(query) } }
    val states = rememberSaveableStateHolder()

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().then(if (selected != null) Modifier.clearAndSetSemantics {} else Modifier)) {
        val inlineSectionHeader = love.yu.jaychou.ui.components.LocalInlineSectionHeader.current
        ArchiveScaffold(AppText.get(R.string.ui_compositions), sectionHeader = true, actions = {
            ArchiveIconButton(if (searching) MiuixIcons.Close else MiuixIcons.Search,
                AppText.get(if (searching) R.string.ui_close_search else R.string.ui_search_compositions),
                { searching = !searching; if (!searching) query = "" }, Modifier.testTag("composition-search-toggle"))
        }) { padding ->
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val columns = when (layout) {
                    AlbumLayout.LIST -> 1
                    AlbumLayout.GRID -> maxOf(2, ((maxWidth - 24.dp + 14.dp) / 194.dp).toInt())
                    AlbumLayout.LARGE -> maxOf(1, ((maxWidth - 24.dp + 14.dp) / 374.dp).toInt())
                }
                states.SaveableStateProvider("${layout.name}-${if (searching) "search" else "catalog"}") {
                    val state = rememberLazyGridState()
                    LaunchedEffect(query) { if (searching) state.scrollToItem(0) }
                    LazyVerticalGrid(columns = GridCells.Fixed(columns), state = state,
                        modifier = Modifier.fillMaxSize().imePadding().testTag("compositions-grid")
                            .semantics { stateDescription = layout.label },
                        contentPadding = PaddingValues(
                if (ArchiveUiTheme.isCoui) 16.dp else 12.dp, padding.calculateTopPadding() + 8.dp,
                if (ArchiveUiTheme.isCoui) 16.dp else 12.dp, bottom),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(if (layout == AlbumLayout.LIST) 12.dp else 18.dp),
                    ) {
                        if (inlineSectionHeader != null) item("section-header", span = { GridItemSpan(maxLineSpan) }) { inlineSectionHeader() }
                        item("heading", span = { GridItemSpan(maxLineSpan) }) {
                            Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(AppText.quantity(R.plurals.song_count, songs.size), fontSize = 15.sp)
                                QuietText(AppText.get(R.string.ui_compositions_intro))
                            }
                        }
                        if (searching) item("search", span = { GridItemSpan(maxLineSpan) }) {
                            CompositionSearchField(query) { query = it }
                        }
                        if (songs.isEmpty()) item("empty", span = { GridItemSpan(maxLineSpan) }) {
                            QuietText(AppText.get(when {
                                loadFailed -> R.string.ui_compositions_load_failed
                                catalog == null -> R.string.ui_compositions_loading
                                else -> R.string.ui_compositions_empty
                            }), Modifier.padding(24.dp).testTag("compositions-empty"))
                        }
                        items(songs, key = { it.id }, contentType = { layout.name }) { song ->
                            CompositionCard(song, language, layout) {
                                focus.clearFocus()
                                selectedId = song.id
                            }
                        }
                        item("source", span = { GridItemSpan(maxLineSpan) }) {
                            QuietText(AppText.get(R.string.ui_compositions_catalog_note))
                        }
                    }
                }
            }
        }
        }
        if (selected != null) CompositionDetail(selected, language, catalog!!.sourceUrl) { selectedId = null }
    }
}

@Composable
private fun CompositionSearchField(query: String, onQuery: (String) -> Unit) {
    val colors = ArchiveUiTheme.colorScheme
    val label = AppText.get(R.string.ui_search_compositions)
    Card(insideMargin = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(query, onQuery, singleLine = true,
                textStyle = TextStyle(color = colors.onBackground, fontSize = 16.sp),
                cursorBrush = SolidColor(colors.primary),
                modifier = Modifier.weight(1f).testTag("composition-search-input")
                    .semantics { contentDescription = label },
                decorationBox = { field -> Box {
                    if (query.isEmpty()) QuietText(label)
                    field()
                } },
            )
            if (query.isNotEmpty()) ArchiveIconButton(MiuixIcons.Close, AppText.get(R.string.ui_clear_search),
                { onQuery("") }, Modifier.testTag("composition-search-clear"))
        }
    }
}

@Composable
private fun CompositionCard(song: Composition, language: String, layout: AlbumLayout, onClick: () -> Unit) {
    val text = song.display(language)
    val large = layout == AlbumLayout.LARGE
    Card(modifier = Modifier.fillMaxWidth().testTag(song.id), onClick = onClick,
        pressFeedbackType = PressFeedbackType.Tilt, showIndication = false,
        insideMargin = PaddingValues(if (layout == AlbumLayout.GRID) 10.dp else 14.dp)) {
        if (layout == AlbumLayout.LIST) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CompositionArtwork(song, text.title, Modifier.size(88.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(text.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    QuietText(text.artist, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(song.releaseDate.take(4), fontSize = 12.sp, color = ArchiveUiTheme.colorScheme.primary)
                }
            }
        } else {
            CompositionArtwork(song, text.title, Modifier.fillMaxWidth().aspectRatio(1f))
            Column(Modifier.padding(start = 3.dp, end = 3.dp, top = 13.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(if (large) 9.dp else 6.dp)) {
                Text(text.title, fontSize = if (large) 23.sp else 16.sp, fontWeight = FontWeight.SemiBold,
                    lineHeight = if (large) 30.sp else 22.sp, minLines = if (large) 1 else 2,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                QuietText(text.artist, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(song.releaseDate.take(4), fontSize = if (large) 13.sp else 11.sp,
                    color = ArchiveUiTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun CompositionArtwork(song: Composition, title: String, modifier: Modifier) {
    if (song.coverUrl.isNotBlank()) CatalogImage(song.coverUrl,
        AppText.get(R.string.ui_artwork, title), modifier.clip(AlbumCoverShape), showStatus = false)
    else Box(modifier.clip(AlbumCoverShape).background(Brush.linearGradient(listOf(
        ArchiveUiTheme.colorScheme.primaryContainer, ArchiveUiTheme.colorScheme.surfaceContainer))),
        contentAlignment = Alignment.Center) {
        ArchiveGlyph(Glyph.ALBUMS, Modifier.fillMaxSize(.38f), ArchiveUiTheme.colorScheme.primary)
    }
}

@Composable
private fun CompositionDetail(song: Composition, language: String, sourceUrl: String, onBack: () -> Unit) {
    val text = song.display(language)
    Box(Modifier.fillMaxSize().background(ArchiveUiTheme.colorScheme.background).testTag("composition-detail")) {
        ArchiveScaffold(AppText.get(R.string.ui_composition_details), small = true,
            navigationIcon = { Box(Modifier.testTag("composition-detail-back")) {
                BackButton(onBack, AppText.get(R.string.ui_compositions))
            } }) { padding ->
            LazyColumn(contentPadding = PaddingValues(20.dp, padding.calculateTopPadding() + 12.dp, 20.dp,
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                item("cover") {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CompositionArtwork(song, text.title, Modifier.widthIn(max = 360.dp).fillMaxWidth().aspectRatio(1f))
                    }
                }
                item("title") { SectionTitle(text.title, text.artist) }
                item("credits") {
                    Card(insideMargin = PaddingValues(20.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(AppText.get(R.string.ui_composed_by_jay), fontWeight = FontWeight.SemiBold)
                            QuietText(AppText.get(R.string.ui_composition_lyricist, text.lyricist))
                            QuietText(AppText.get(R.string.ui_composition_release, text.album))
                            QuietText(AppText.get(R.string.ui_composition_date, song.releaseDate))
                        }
                    }
                }
                item("sources") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuietText(AppText.get(R.string.ui_compositions_catalog_note))
                        SourceLink(AppText.get(R.string.ui_compositions_source), sourceUrl)
                        if (song.artworkSourceUrl.isNotBlank()) {
                            QuietText(AppText.get(R.string.ui_composition_artwork_note))
                            SourceLink(AppText.get(R.string.ui_composition_artwork_source), song.artworkSourceUrl)
                        }
                    }
                }
            }
        }
    }
}
