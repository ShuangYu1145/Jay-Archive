package love.yu.jaychou.ui.screens.home

import love.yu.jaychou.ui.animation.AlbumMotionSource
import love.yu.jaychou.ui.animation.AlbumOrigin
import love.yu.jaychou.ui.components.ArchiveGlyph
import love.yu.jaychou.ui.components.ArchiveScaffold
import love.yu.jaychou.ui.components.AlbumCoverShape
import love.yu.jaychou.ui.components.CatalogImage
import love.yu.jaychou.ui.components.Glyph
import love.yu.jaychou.ui.components.QuietText
import love.yu.jaychou.ui.components.SectionTitle
import love.yu.jaychou.data.AppText
import love.yu.jaychou.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.designsystem.Card
import love.yu.jaychou.ui.designsystem.Text
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun HomeScreen(catalog: AlbumCatalog, state: LazyListState, bottom: androidx.compose.ui.unit.Dp, onAlbum: (Album, AlbumOrigin) -> Unit, onAlbums: () -> Unit,
    profile: AppSettings = AppSettings(),
) {
    val pageInset = if (ArchiveUiTheme.isCoui) 16.dp else 24.dp
    val groupSpacing = if (ArchiveUiTheme.isCoui) 16.dp else 20.dp
    val behavior = love.yu.jaychou.ui.designsystem.rememberArchiveScrollBehavior()
    ArchiveScaffold(AppText.get(R.string.ui_home), behavior = behavior,
        actions = { HomeProfileButton(profile) },
    ) { padding ->
    LazyColumn(modifier = Modifier.nestedScroll(behavior.nestedScrollConnection), state = state, contentPadding = PaddingValues(start = pageInset, end = pageInset, top = padding.calculateTopPadding() + 12.dp, bottom = bottom), verticalArrangement = Arrangement.spacedBy(groupSpacing)) {
        item("heading") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(AppText.get(R.string.ui_jay_archive_music_archive), color = ArchiveUiTheme.colorScheme.primary, fontSize = 12.sp, letterSpacing = 2.sp)
                Text(AppText.get(R.string.ui_let_chinese_language_music_shine), fontSize = 29.sp, fontWeight = FontWeight.Bold)
            }
        }
        item("hero") {
            Column {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    insideMargin = PaddingValues(0.dp),
                    pressFeedbackType = PressFeedbackType.Tilt, showIndication = false,
                ) {
                Box(Modifier.fillMaxWidth().height(118.dp)) {
                    CatalogImage(AlbumRepository.portraitUrl, AppText.get(R.string.ui_official_jay_chou_portrait_source_jvr_music), Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .8f)), startY = 180f)))
                    Column(
                        Modifier.align(Alignment.BottomEnd).padding(22.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(AppText.get(R.string.ui_jay_chou), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                        Text("JAY CHOU", color = Color.White.copy(alpha = .85f), fontSize = 14.sp, letterSpacing = 4.sp)
                    }
                }
                }
            }
        }
        item("intro") {
            Card(insideMargin = PaddingValues(22.dp), pressFeedbackType = PressFeedbackType.Tilt) {
                Text(AppText.get(R.string.ui_listening_since_2000), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Text(AppText.get(R.string.ui_jay_chou_born_january_18_1979_is_a_singer_music_producer_actor_a), fontSize = 15.sp, lineHeight = 25.sp)
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat("16", AppText.get(R.string.ui_studio_albums))
                    Stat("2000—2026", AppText.get(R.string.ui_release_years))
                    Stat(catalog.albums.filter { it.category == ReleaseCategory.STUDIO }.sumOf { it.tracks.size }.toString(), AppText.get(R.string.ui_tracks_archived))
                }
            }
        }
        item("style") {
            Card(insideMargin = PaddingValues(22.dp), pressFeedbackType = PressFeedbackType.Tilt) {
                Text(AppText.get(R.string.ui_a_sound_of_his_own), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Text(AppText.get(R.string.ui_from_r_b_and_hip_hop_to_pop_ballads_his_albums_speak_many_musica), fontSize = 15.sp, lineHeight = 25.sp)
                Spacer(Modifier.height(15.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("R&B", AppText.get(R.string.ui_chinese_style), AppText.get(R.string.ui_pop)).forEach {
                        Text(it, Modifier.background(ArchiveUiTheme.colorScheme.primary.copy(alpha = .1f)).padding(horizontal = 12.dp, vertical = 6.dp), color = ArchiveUiTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                }
            }
        }
        item("works-heading") { SectionTitle(AppText.get(R.string.ui_start_with_these_albums), AppText.get(R.string.ui_discover_the_music_across_the_years)) }
        listOf("1152155344949088256" to AppText.get(R.string.ui__163), "1152152148126076928" to AppText.get(R.string.ui__164), "1152151605810958336" to AppText.get(R.string.ui__165)).forEach { (id, caption) ->
            val album = catalog.find(id)!!
            item("featured-$id") {
                AlbumMotionSource(album, onAlbum) { capture, coverCapture, open ->
                Card(modifier = capture, onClick = open, pressFeedbackType = PressFeedbackType.Tilt,
                    showIndication = false,
                    insideMargin = PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CatalogImage(album.coverUrl, AppText.get(R.string.ui_album_artwork, album.title), coverCapture.size(82.dp).clip(AlbumCoverShape))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(album.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            QuietText(caption)
                            QuietText(AppText.get(R.string.ui_view_album, album.year))
                        }
                        ArchiveGlyph(Glyph.LINK, Modifier.size(20.dp), ArchiveUiTheme.colorScheme.onBackgroundVariant)
                    }
                }
                }
            }
        }
        item("all-albums") {
            Card(onClick = onAlbums, insideMargin = PaddingValues(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(AppText.get(R.string.ui_explore_all_albums), color = ArchiveUiTheme.colorScheme.primary, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        QuietText(AppText.get(R.string.ui_16_albums_from_jay_to_children_of_the_sun))
                    }
                    ArchiveGlyph(Glyph.LINK, color = ArchiveUiTheme.colorScheme.primary)
                }
            }
        }
    }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(value, color = ArchiveUiTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        QuietText(label)
    }
}
