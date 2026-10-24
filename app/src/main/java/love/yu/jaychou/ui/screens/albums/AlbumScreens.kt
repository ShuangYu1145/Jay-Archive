package love.yu.jaychou.ui.screens.albums

import love.yu.jaychou.ui.animation.AlbumMotionSource
import love.yu.jaychou.ui.animation.AlbumOrigin
import love.yu.jaychou.ui.animation.MotionSource
import love.yu.jaychou.ui.components.AlbumCoverShape
import love.yu.jaychou.ui.components.LeadTrackStar
import love.yu.jaychou.ui.components.ArchiveGlyph
import love.yu.jaychou.ui.components.ArchiveIconButton
import love.yu.jaychou.ui.components.ArchiveScaffold
import love.yu.jaychou.ui.components.BackButton
import love.yu.jaychou.ui.components.CatalogImage
import love.yu.jaychou.ui.components.Glyph
import love.yu.jaychou.ui.components.QuietText
import love.yu.jaychou.ui.components.SectionTitle
import love.yu.jaychou.ui.components.SourceLink
import love.yu.jaychou.data.AppText
import love.yu.jaychou.R
import androidx.compose.animation.animateContentSize
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yu.jaychou.data.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Filter
import love.yu.jaychou.ui.designsystem.Card
import love.yu.jaychou.ui.designsystem.Text
import love.yu.jaychou.ui.designsystem.rememberArchiveScrollBehavior
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme

@Composable
fun AlbumsScreen(
    catalog: AlbumCatalog, layout: AlbumLayout, state: LazyGridState,
    bottom: Dp, onAlbum: (Album, AlbumOrigin) -> Unit,
    categories: List<String>, onCategories: (List<String>) -> Unit,
    onSearchSong: (Album, Track, AlbumOrigin) -> Unit = { album, _, origin -> onAlbum(album, origin) },
    onSearchVisibility: (Boolean) -> Unit = {},
    searchInteractive: Boolean = true,
    sectionHeader: Boolean = false,
) {
    val behavior = rememberArchiveScrollBehavior()
    var showCategories by rememberSaveable { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val visibilityCallback by rememberUpdatedState(onSearchVisibility)
    LaunchedEffect(searching) { visibilityCallback(searching) }
    DisposableEffect(Unit) { onDispose { visibilityCallback(false) } }
    BackHandler(searching && searchInteractive) { searching = false }
    val searchProgress by animateFloatAsState(if (searching) 1f else 0f, tween(260), label = "search-open")
    val albums = catalog.albums.filter { it.category.name in categories }
    Box(Modifier.fillMaxSize()) {
    Box(Modifier.fillMaxSize().graphicsLayer {
        scaleX = 1f - searchProgress * .025f; scaleY = scaleX
        alpha = 1f - searchProgress
    }.then(if (searching) Modifier.clearAndSetSemantics {} else Modifier)) {
    ArchiveScaffold(AppText.get(R.string.ui_albums), behavior = behavior,
        sectionHeader = sectionHeader,
        actions = {
            ArchiveIconButton(MiuixIcons.Search, AppText.get(R.string.ui_search_albums_or_songs), { searching = true }, Modifier.testTag("album-search-open"))
            ArchiveIconButton(MiuixIcons.Filter, AppText.get(R.string.ui_filter_album_categories), { showCategories = true }, Modifier.testTag("release-filter"))
        },
    ) { padding ->
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize()
                .then(if (sectionHeader) Modifier else Modifier.nestedScroll(behavior.nestedScrollConnection))
                .testTag("albums-grid").semantics { stateDescription = layout.label },
            columns = GridCells.Fixed(if (layout == AlbumLayout.GRID) 2 else 1), state = state,
            contentPadding = PaddingValues(12.dp, padding.calculateTopPadding() + 8.dp, 12.dp, bottom),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(if (layout == AlbumLayout.LIST) 12.dp else 18.dp),
        ) {
            item("heading", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(AppText.get(R.string.ui_releases, albums.size, ReleaseCategory.entries.filter { it.name in categories }.joinToString(" / ") { it.label }), fontSize = 15.sp)
                    QuietText(AppText.get(R.string.ui_newest_releases_first_updated_nlive_albums_eps_and_singles_are_l, catalog.verifiedAt))
                    Spacer(Modifier.height(2.dp))
                }
            }
            items(albums, key = { it.id }, contentType = { "album-${layout.name}" }) { album ->
                AlbumMotionSource(album, onAlbum) { capture, coverCapture, open ->
                    AlbumCatalogCard(album, layout, capture, coverCapture, open)
                }
            }
            item("source", span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuietText(AppText.get(R.string.ui_sources_jvr_music_qq_music_apple_music))
                }
            }
        }
    }
    }
    AnimatedVisibility(searching,
        enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { -it / 24 },
        exit = fadeOut(tween(180)) + slideOutVertically(tween(220)) { -it / 30 },
    ) {
        AlbumSearchScreen(catalog, query, { query = it }, { searching = false }, onAlbum, onSearchSong)
    }
    }
    ReleaseCategorySheet(showCategories, categories, catalog, { showCategories = false }, onCategories)
}

@Composable
private fun AlbumCatalogCard(album: Album, layout: AlbumLayout, capture: Modifier, coverCapture: Modifier, onClick: () -> Unit) {
    val large = layout == AlbumLayout.LARGE
    Card(
        modifier = capture.fillMaxWidth().testTag("album-${album.id}"), onClick = onClick,
        pressFeedbackType = PressFeedbackType.Tilt, showIndication = false,
        insideMargin = PaddingValues(if (layout == AlbumLayout.GRID) 10.dp else 14.dp),
    ) {
        if (layout == AlbumLayout.LIST) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CatalogImage(album.coverUrl, album.coverAsset, AppText.get(R.string.ui_album_artwork_source, album.title, album.sourceLabel),
                    coverCapture.size(88.dp).clip(AlbumCoverShape))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(album.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    QuietText(album.releaseDate)
                    Text(AppText.get(R.string.ui_tracks, album.tracks.size), fontSize = 12.sp, color = ArchiveUiTheme.colorScheme.primary)
                }
            }
        } else {
            CatalogImage(album.coverUrl, album.coverAsset, AppText.get(R.string.ui_album_artwork_source, album.title, album.sourceLabel),
                coverCapture.fillMaxWidth().aspectRatio(1f).clip(AlbumCoverShape))
            Column(Modifier.padding(start = 3.dp, end = 3.dp, top = 13.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(if (large) 9.dp else 6.dp)) {
                Text(album.title, fontSize = if (large) 23.sp else 16.sp, fontWeight = FontWeight.SemiBold,
                    lineHeight = if (large) 30.sp else 22.sp,
                    modifier = Modifier.testTag("album-title-${album.id}"),
                    minLines = if (large) 1 else 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                QuietText(album.releaseDate, Modifier.testTag("album-date-${album.id}"))
                Text(AppText.get(R.string.ui_tracks_26, album.tracks.size, album.year), color = ArchiveUiTheme.colorScheme.primary,
                    fontSize = if (large) 13.sp else 11.sp)
            }
        }
    }
}

@Composable
fun AlbumDetailScreen(album: Album, state: LazyListState, bottom: Dp, onBack: () -> Unit, onTrack: (Track, AlbumOrigin) -> Unit,
    coverModifier: Modifier = Modifier,
    bodyModifier: Modifier = Modifier,
    onCoverDetached: () -> Unit = {},
    highlightedTrack: Int? = null,
) {
    var coverRatio by remember(album.id) { mutableFloatStateOf(1f) }
    ArchiveScaffold(AppText.get(R.string.ui_album_archive), small = true, navigationIcon = { BackButton(onBack) },
        actions = { Text(album.year, color = ArchiveUiTheme.colorScheme.primary, fontSize = 14.sp) },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().testTag("album-detail-list"), state = state,
            contentPadding = PaddingValues(20.dp, padding.calculateTopPadding() + 12.dp, 20.dp, bottom), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item("cover") {
                val detach by rememberUpdatedState(onCoverDetached)
                DisposableEffect(album.id) { onDispose { detach() } }
                Box(Modifier.fillMaxWidth().animateContentSize(), contentAlignment = Alignment.Center) {
                    CatalogImage(album.coverUrl, album.coverAsset, AppText.get(R.string.ui_artwork_source, album.title, album.sourceLabel),
                        Modifier.widthIn(max = 360.dp).fillMaxWidth()
                            .aspectRatio(coverRatio.coerceIn(.65f, 1.8f))
                            .testTag("album-detail-cover").then(coverModifier).clip(AlbumCoverShape),
                        contentScale = ContentScale.Fit,
                        onAspectRatio = { coverRatio = it },
                    )
                }
            }
            item("title") {
                Column(bodyModifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(album.title, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                    val alternateTitle = if (album.title == album.englishTitle) album.originalTitle else album.englishTitle
                    if (alternateTitle.isNotBlank() && alternateTitle != album.title) QuietText(alternateTitle)
                    Text(AppText.get(R.string.ui_tracks_29, album.releaseDate, album.tracks.size), color = ArchiveUiTheme.colorScheme.primary, fontSize = 14.sp)
                }
            }
            item("summary") {
                Card(modifier = bodyModifier, insideMargin = PaddingValues(20.dp)) {
                    Text(AppText.get(R.string.ui_about_this_album), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Text(album.summary, fontSize = 15.sp, lineHeight = 25.sp)
                    Spacer(Modifier.height(12.dp))
                    QuietText(album.dateNote)
                }
            }
            item("tracks-title") { SectionTitle(AppText.get(R.string.ui_track_list), AppText.get(R.string.ui_original_track_order_tap_a_song_to_read_its_lyrics), bodyModifier) }
            items(album.tracks, key = { "${album.id}-${it.number}" }, contentType = { "track" }) { track ->
                MotionSource(onOpen = { onTrack(track, it) }) { capture, open ->
                    AlbumTrackCard(album, track, highlightedTrack == track.number,
                        capture.then(bodyModifier), open)
                }
            }
            item("source") {
                Column(bodyModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuietText(AppText.get(R.string.ui_information_track_order_and_artwork_nrelease_type, album.sourceLabel, album.category.label))
                    SourceLink(AppText.get(R.string.ui_view_website), album.sourceUrl)
                }
            }
        }
    }
}

@Composable
private fun AlbumTrackCard(album: Album, track: Track, highlighted: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().testTag("track-${track.number}"),
        cornerRadius = 18.dp, onClick = onClick, pressFeedbackType = PressFeedbackType.Sink,
        colors = love.yu.jaychou.ui.designsystem.CardDefaults.defaultColors(
            color = if (highlighted) ArchiveUiTheme.colorScheme.primaryContainer else ArchiveUiTheme.colorScheme.surfaceContainer,
        ),
        insideMargin = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(42.dp).background(ArchiveUiTheme.colorScheme.primary.copy(alpha = .09f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center) {
                Text(if (album.tracks.any { it.disc > 1 }) "${track.disc}.${track.sourceTrackNumber}"
                    else track.number.toString().padStart(2, '0'),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ArchiveUiTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (track.isLeadTrack) LeadTrackStar(Modifier.testTag("lead-track-${track.number}"))
                    Text(track.title, fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(AppText.get(if (track.isLeadTrack) R.string.ui_lead_track_read_lyrics else R.string.ui_read_lyrics),
                    fontSize = 12.sp, color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
            }
            ArchiveGlyph(Glyph.LINK, Modifier.size(18.dp), ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
        }
    }
}
