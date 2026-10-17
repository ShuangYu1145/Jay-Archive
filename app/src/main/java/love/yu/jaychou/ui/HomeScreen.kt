package love.yu.jaychou.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun HomeScreen(catalog: AlbumCatalog, state: LazyListState, bottom: androidx.compose.ui.unit.Dp, onAlbum: (Album, AlbumOrigin) -> Unit, onAlbums: () -> Unit) {
    val behavior = top.yukonga.miuix.kmp.basic.MiuixScrollBehavior()
    ArchiveScaffold("首页", behavior = behavior) { padding ->
    LazyColumn(modifier = Modifier.nestedScroll(behavior.nestedScrollConnection), state = state, contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = padding.calculateTopPadding() + 12.dp, bottom = bottom), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item("heading") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("JAY ARCHIVE  /  音乐档案", color = MiuixTheme.colorScheme.primary, fontSize = 12.sp, letterSpacing = 2.sp)
                Text("不要让韩流越来越嚣张，华流才是最屌的", fontSize = 29.sp, fontWeight = FontWeight.Bold)
            }
        }
        item("hero") {
            Column {
                Box(Modifier.fillMaxWidth().height(118.dp).clip(RoundedCornerShape(28.dp))) {
                    CatalogImage(AlbumRepository.portraitUrl, "jay-portrait.jpg", "周杰伦官方宣传照，来源：杰威尔音乐", Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .8f)), startY = 180f)))
                    Column(
                        Modifier.align(Alignment.BottomEnd).padding(22.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("周杰伦", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                        Text("JAY CHOU", color = Color.White.copy(alpha = .85f), fontSize = 14.sp, letterSpacing = 4.sp)
                    }
                }
            }
        }
        item("intro") {
            Card(insideMargin = PaddingValues(22.dp), cornerRadius = 26.dp,pressFeedbackType = PressFeedbackType.Tilt) {
                Text("从 2000 年，听到今天", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Text("周杰伦（Jay Chou），1979 年 1 月 18 日出生，华语流行音乐歌手、音乐制作人、演员与导演。2000 年推出首张同名专辑，开启个人音乐创作历程。", fontSize = 15.sp, lineHeight = 25.sp)
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat("16", "录音室专辑")
                    Stat("2000—2026", "发行跨度")
                    Stat(catalog.albums.filter { it.category == ReleaseCategory.STUDIO }.sumOf { it.tracks.size }.toString(), "收录曲目")
                }
            }
        }
        item("style") {
            Card(insideMargin = PaddingValues(22.dp), cornerRadius = 26.dp,pressFeedbackType = PressFeedbackType.Tilt) {
                Text("旋律之间，自成一派", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Text("从 R&B、嘻哈到抒情流行，他的专辑呈现多种音乐语言。《双截棍》的节奏、《东风破》的中国风旋律、《七里香》的抒情表达，构成不同的聆听入口。", fontSize = 15.sp, lineHeight = 25.sp)
                Spacer(Modifier.height(15.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("R&B", "中国风", "流行").forEach {
                        Text(it, Modifier.background(MiuixTheme.colorScheme.primary.copy(alpha = .1f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp), color = MiuixTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                }
            }
        }
        item("works-heading") { SectionTitle("从这些作品开始", "跨越年代的聆听入口") }
        listOf("1152155344949088256" to "双截棍 · 安静", "1152152148126076928" to "东风破 · 晴天", "1152151605810958336" to "七里香 · 搁浅").forEach { (id, caption) ->
            val album = catalog.find(id)!!
            item("featured-$id") {
                AlbumMotionSource(album, onAlbum) { capture, open ->
                Card(modifier = capture, onClick = open, pressFeedbackType = PressFeedbackType.Sink,
                    insideMargin = PaddingValues(12.dp), cornerRadius = 24.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CatalogImage(album.coverUrl, album.coverAsset, "${album.title}专辑封面", Modifier.size(82.dp).clip(RoundedCornerShape(17.dp)))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(album.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            QuietText(caption)
                            QuietText("${album.year}  ·  查看专辑")
                        }
                        ArchiveGlyph(Glyph.LINK, Modifier.size(20.dp), MiuixTheme.colorScheme.onBackgroundVariant)
                    }
                }
                }
            }
        }
        item("all-albums") {
            Card(onClick = onAlbums, insideMargin = PaddingValues(22.dp), cornerRadius = 24.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("翻开全部专辑", color = MiuixTheme.colorScheme.primary, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        QuietText("16 张专辑，从《Jay》到《太阳之子》")
                    }
                    ArchiveGlyph(Glyph.LINK, color = MiuixTheme.colorScheme.primary)
                }
            }
        }
    }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(value, color = MiuixTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        QuietText(label)
    }
}
