package love.yu.jaychou.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AlbumsScreen(
    catalog: AlbumCatalog, layout: AlbumLayout, state: LazyGridState,
    bottom: Dp, onAlbum: (Album, AlbumOrigin) -> Unit,
    categories: List<String>, onCategories: (List<String>) -> Unit,
) {
    val behavior = MiuixScrollBehavior()
    var showCategories by rememberSaveable { mutableStateOf(false) }
    val albums = catalog.albums.filter { it.category.name in categories }
    ArchiveScaffold("专辑", behavior = behavior,
        actions = { TextButton("分类", onClick = { showCategories = true }, modifier = Modifier.testTag("release-filter")) },
    ) { padding ->
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize().nestedScroll(behavior.nestedScrollConnection)
                .testTag("albums-grid").semantics { stateDescription = layout.label },
            columns = GridCells.Fixed(if (layout == AlbumLayout.GRID) 2 else 1), state = state,
            contentPadding = PaddingValues(12.dp, padding.calculateTopPadding() + 8.dp, 12.dp, bottom),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(if (layout == AlbumLayout.LIST) 12.dp else 18.dp),
        ) {
            item("heading", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${albums.size} 张发行作品 · ${ReleaseCategory.entries.filter { it.name in categories }.joinToString(" / ") { it.label }}", fontSize = 15.sp)
                    QuietText("按发行时间倒序 · 更新日期 ${catalog.verifiedAt}\n演唱会、EP 与单曲分开归类；日期与曲序依所注版本。")
                    Spacer(Modifier.height(2.dp))
                }
            }
            items(albums, key = { it.id }, contentType = { "album-${layout.name}" }) { album ->
                AlbumMotionSource(album, onAlbum) { capture, open ->
                    AlbumCatalogCard(album, layout, capture, open)
                }
            }
            item("source", span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuietText("来源：jvrmusic，QQ Music，Apple Music")
                }
            }
        }
    }
    ReleaseCategorySheet(showCategories, categories, catalog, { showCategories = false }, onCategories)
}

@Composable
private fun AlbumCatalogCard(album: Album, layout: AlbumLayout, capture: Modifier, onClick: () -> Unit) {
    val large = layout == AlbumLayout.LARGE
    Card(
        modifier = capture.fillMaxWidth().testTag("album-${album.id}"), onClick = onClick,
        pressFeedbackType = PressFeedbackType.Sink,
        insideMargin = PaddingValues(if (layout == AlbumLayout.GRID) 10.dp else 14.dp), cornerRadius = 23.dp,
    ) {
        if (layout == AlbumLayout.LIST) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CatalogImage(album.coverUrl, album.coverAsset, "《${album.title}》专辑封面，来源：${album.sourceLabel}",
                    Modifier.size(88.dp).clip(RoundedCornerShape(16.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(album.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    QuietText(album.releaseDate)
                    Text("${album.tracks.size} 首曲目", fontSize = 12.sp, color = MiuixTheme.colorScheme.primary)
                }
            }
        } else {
            CatalogImage(album.coverUrl, album.coverAsset, "《${album.title}》专辑封面，来源：${album.sourceLabel}",
                Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(if (large) 20.dp else 16.dp)))
            Column(Modifier.padding(start = 3.dp, end = 3.dp, top = 13.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(if (large) 9.dp else 6.dp)) {
                Text(album.title, fontSize = if (large) 23.sp else 16.sp, fontWeight = FontWeight.SemiBold,
                    lineHeight = if (large) 30.sp else 22.sp,
                    modifier = Modifier.testTag("album-title-${album.id}"),
                    minLines = if (large) 1 else 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                QuietText(album.releaseDate, Modifier.testTag("album-date-${album.id}"))
                Text("${album.tracks.size} 首  ·  ${album.year}", color = MiuixTheme.colorScheme.primary,
                    fontSize = if (large) 13.sp else 11.sp)
            }
        }
    }
}

@Composable
fun AlbumDetailScreen(album: Album, state: LazyListState, bottom: Dp, onBack: () -> Unit, onTrack: (Track, AlbumOrigin) -> Unit) {
    ArchiveScaffold("专辑档案", small = true, navigationIcon = { BackButton(onBack) },
        actions = { Text(album.year, color = MiuixTheme.colorScheme.primary, fontSize = 14.sp) },
    ) { padding ->
        LazyColumn(modifier = Modifier.testTag("album-detail-list"), state = state,
            contentPadding = PaddingValues(24.dp, padding.calculateTopPadding() + 16.dp, 24.dp, bottom), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item("cover") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CatalogImage(album.coverUrl, album.coverAsset, "《${album.title}》封面，来源：${album.sourceLabel}",
                        Modifier.widthIn(max = 420.dp).fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp)))
                }
            }
            item("title") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(album.title, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                    if (album.englishTitle.isNotBlank()) QuietText(album.englishTitle)
                    Text("${album.releaseDate}  ·  ${album.tracks.size} 首曲目", color = MiuixTheme.colorScheme.primary, fontSize = 14.sp)
                }
            }
            item("summary") {
                Card(insideMargin = PaddingValues(20.dp), cornerRadius = 24.dp) {
                    Text("关于这张专辑", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Text(album.summary, fontSize = 15.sp, lineHeight = 25.sp)
                    Spacer(Modifier.height(12.dp))
                    QuietText(album.dateNote)
                }
            }
            item("tracks-title") { SectionTitle("完整曲目", "原始曲序 · 点击歌曲查看歌词") }
            items(album.tracks, key = { "${album.id}-${it.number}" }, contentType = { "track" }) { track ->
                MotionSource(onOpen = { onTrack(track, it) }) { capture, open ->
                Card(modifier = capture.testTag("track-${track.number}"), onClick = open,
                    insideMargin = PaddingValues(horizontal = 18.dp, vertical = 17.dp), cornerRadius = 19.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(17.dp)) {
                        Text(if (album.tracks.any { it.disc > 1 }) "${track.disc}.${track.sourceTrackNumber}" else track.number.toString().padStart(2, '0'), fontSize = 14.sp, color = MiuixTheme.colorScheme.primary)
                        Text(track.title, fontSize = 16.sp, lineHeight = 23.sp, modifier = Modifier.weight(1f))
                        ArchiveGlyph(Glyph.LINK, Modifier.size(16.dp), MiuixTheme.colorScheme.onBackgroundVariant)
                    }
                }
                }
            }
            item("source") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuietText("资料、曲序与封面：${album.sourceLabel}\n专辑类型 · ${album.category.label}")
                    SourceLink("查看网页", album.sourceUrl)
                }
            }
        }
    }
}
