package love.yu.jaychou.ui.screens.albums

import love.yu.jaychou.ui.animation.AlbumMotionSource
import love.yu.jaychou.ui.animation.AlbumOrigin
import love.yu.jaychou.ui.components.ArchiveGlyph
import love.yu.jaychou.ui.components.ArchiveIconButton
import love.yu.jaychou.ui.components.AlbumCoverShape
import love.yu.jaychou.ui.components.CatalogImage
import love.yu.jaychou.ui.components.Glyph
import love.yu.jaychou.ui.components.QuietText
import love.yu.jaychou.ui.components.SectionTitle
import love.yu.jaychou.data.AppText
import love.yu.jaychou.R
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.designsystem.Card
import love.yu.jaychou.ui.designsystem.Text
import love.yu.jaychou.ui.designsystem.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Back
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme

@Composable
internal fun AlbumSearchScreen(
    catalog: AlbumCatalog, query: String, onQuery: (String) -> Unit, onClose: () -> Unit,
    onAlbum: (Album, AlbumOrigin) -> Unit,
    onSong: (Album, Track, AlbumOrigin) -> Unit,
) {
    val colors = ArchiveUiTheme.colorScheme
    val index = remember(catalog) { AlbumSearchIndex(catalog) }
    val results = remember(index, query) { index.search(query) }
    val list = rememberLazyListState()
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { delay(200); focus.requestFocus() }
    LaunchedEffect(query) { list.scrollToItem(0) }
    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding()
        .navigationBarsPadding().imePadding().testTag("album-search")) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            ArchiveIconButton(MiuixIcons.Back, AppText.get(R.string.ui_close_search), { keyboard?.hide(); onClose() },
                Modifier.testTag("album-search-close"))
            BasicTextField(
                value = query, onValueChange = onQuery, singleLine = true,
                textStyle = TextStyle(color = colors.onBackground, fontSize = 16.sp),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                modifier = Modifier.weight(1f).focusRequester(focus).testTag("album-search-input")
                    .semantics { contentDescription = AppText.get(R.string.ui_search_albums_or_songs) },
                decorationBox = { field ->
                    Row(Modifier.background(colors.surfaceContainer, RoundedCornerShape(20.dp))
                        .heightIn(min = 52.dp).padding(start = 15.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(MiuixIcons.Search, contentDescription = null, modifier = Modifier.size(21.dp), tint = colors.onBackgroundVariant)
                        Box(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            if (query.isEmpty()) Text(AppText.get(R.string.ui_search_albums_or_songs), fontSize = 16.sp, color = colors.onBackgroundVariant)
                            field()
                        }
                        if (query.isNotEmpty()) ArchiveIconButton(MiuixIcons.Close, AppText.get(R.string.ui_clear_search), { onQuery(""); focus.requestFocus() },
                            Modifier.testTag("album-search-clear"))
                    }
                },
            )
        }
        val mode = if (query.isBlank()) "prompt" else if (results.albums.isEmpty() && results.songs.isEmpty()) "empty" else "results"
        AnimatedContent(mode, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "search-results", modifier = Modifier.weight(1f)) { state ->
            if (state != "results") {
                Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
                    Icon(MiuixIcons.Search, contentDescription = null, modifier = Modifier.size(40.dp), tint = colors.onBackgroundVariant)
                    Text(if (state == "prompt") AppText.get(R.string.ui_find_an_album_or_a_favorite_song) else AppText.get(R.string.ui_no_matching_albums_or_songs),
                        fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    QuietText(if (state == "prompt") AppText.get(R.string.ui_search_original_titles_localized_titles_and_song_names_nall_rele) else AppText.get(R.string.ui_try_a_shorter_keyword_or_check_the_song_title))
                }
            } else {
                LazyColumn(state = list, modifier = Modifier.fillMaxSize().testTag("album-search-results"),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item("count") {
                        Text(AppText.get(R.string.ui_albums_songs,
                            AppText.quantity(R.plurals.album_count, results.albums.size),
                            AppText.quantity(R.plurals.song_count, results.songs.size)), fontSize = 13.sp,
                            color = colors.onBackgroundVariant, modifier = Modifier.padding(vertical = 8.dp).animateContentSize())
                    }
                    if (results.albums.isNotEmpty()) item("albums-heading") { SectionTitle(AppText.get(R.string.ui_albums)) }
                    items(results.albums, key = { "album-${it.id}" }) { album ->
                        AlbumMotionSource(album, { a, origin -> keyboard?.hide(); onAlbum(a, origin) }) { capture, cover, open ->
                            SearchResultRow(album, album.title, AppText.get(R.string.ui_tracks_42, album.year, album.category.label, album.tracks.size),
                                Modifier.animateItem().then(capture).testTag("search-album-${album.id}"), cover, open)
                        }
                    }
                    if (results.songs.isNotEmpty()) item("songs-heading") { SectionTitle(AppText.get(R.string.ui_songs), AppText.get(R.string.ui_open_the_album_at_this_song)) }
                    items(results.songs, key = { "song-${it.album.id}-${it.track.number}" }) { result ->
                        AlbumMotionSource(result.album, { a, origin -> keyboard?.hide(); onSong(a, result.track, origin) }) { capture, cover, open ->
                            SearchResultRow(result.album, result.track.title, "${result.album.title} · ${result.album.year}",
                                Modifier.animateItem().then(capture).testTag("search-song-${result.album.id}-${result.track.number}"), cover, open)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(album: Album, title: String, subtitle: String,
    modifier: Modifier, cover: Modifier, onClick: () -> Unit,
) {
    Card(modifier.fillMaxWidth(), onClick = onClick, insideMargin = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CatalogImage(album.coverUrl, album.coverAsset, AppText.get(R.string.ui_artwork, album.title), cover.size(56.dp).clip(AlbumCoverShape))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                QuietText(subtitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            ArchiveGlyph(Glyph.LINK, Modifier.size(17.dp), ArchiveUiTheme.colorScheme.onBackgroundVariant)
        }
    }
}
