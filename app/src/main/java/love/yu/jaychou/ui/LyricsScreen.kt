package love.yu.jaychou.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import love.yu.jaychou.data.*
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LyricsScreen(album: Album, track: Track, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = rememberLazyListState()
    var retry by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var versions by remember { mutableStateOf(emptyList<LyricsVersion>()) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var localText by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(album.id, track.number, retry) {
        loading = true; error = null
        try {
            localText = LyricsRepository.local(context, album, track)
            if (localText == null) versions = LyricsRepository.search(track)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) {
            // Diagnostic types only: never write lyrics or user-imported text to Logcat.
            android.util.Log.w("JayLyrics", "${failure.javaClass.simpleName}; cause=${failure.cause?.javaClass?.simpleName}")
            error = "未能加载在线歌词，请检查网络后重试，或导入本地歌词。"
        }
        finally { loading = false }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try { localText = LyricsRepository.import(context, album, track, uri); error = null }
            catch (_: Exception) { error = "导入失败：请使用不超过 1 MB 的 UTF-8/GB18030 LRC 或 TXT 文件。" }
        }
    }
    val selected = versions.find { it.id == selectedId }
    val text = localText ?: selected?.text
    ArchiveScaffold("歌词", small = true,
        navigationIcon = { BackButton(onBack, "返回专辑详情") },
        actions = { TextButton("导入", onClick = { importer.launch(arrayOf("text/*", "application/octet-stream", "application/x-subrip")) }) },
    ) { padding ->
        LazyColumn(state = state, modifier = Modifier.fillMaxSize().testTag("lyrics-list"),
            contentPadding = PaddingValues(24.dp, padding.calculateTopPadding() + 22.dp, 24.dp,
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 30.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item("identity") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(track.title, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                    QuietText("周杰伦 · ${album.title}")
                    QuietText(if (localText != null) "本地导入 · 已保存到应用私有目录" else "LRCLIB 社区歌词 · 请核对歌曲、专辑及现场/录音室版本")
                }
            }
            if (loading) item("loading") { Text("正在查找歌词…", Modifier.testTag("lyrics-loading")) }
            if (error != null) item("error") {
                Card(insideMargin = PaddingValues(18.dp)) {
                    QuietText(error!!)
                    TextButton("重试", onClick = { retry++ })
                }
            }
            if (!loading && error == null && localText == null && selected == null) {
                item("choose") {
                    Text(if (versions.isEmpty()) "暂无可核实的歌词版本，可导入本地 LRC/TXT。" else "选择歌词版本", style = MiuixTheme.textStyles.title3)
                }
                itemsIndexed(versions, key = { _, it -> "version-${it.id}" }) { _, version ->
                    Card(onClick = { selectedId = version.id }, insideMargin = PaddingValues(18.dp),
                        modifier = Modifier.testTag("lyrics-version-${version.id}")) {
                        Text(version.title, fontSize = 18.sp)
                        QuietText("${version.artist} · ${version.album}")
                        if (version.instrumental) QuietText("该来源标记为纯音乐")
                    }
                }
            }
            if (selected != null && localText == null) item("version") {
                QuietText("当前版本：${selected.artist} · ${selected.album} · #${selected.id}")
                if (selected.instrumental) Text("此版本为纯音乐，无歌词。")
                TextButton("更换歌词版本", onClick = { selectedId = null })
            }
            if (localText != null) item("remove-local") {
                TextButton("删除本地版本", onClick = { scope.launch {
                    try { LyricsRepository.removeLocal(context, album, track); localText = null; retry++ }
                    catch (_: Exception) { error = "删除失败，请重试。" }
                } })
            }
            if (text != null) itemsIndexed(LyricsRepository.lines(text), key = { index, _ -> "line-$index" }) { _, line ->
                Text(line.ifBlank { " " }, fontSize = 19.sp, lineHeight = 32.sp)
            }
            item("source") {
                SourceLink("歌词来源与服务：LRCLIB", LyricsRepository.source)
                QuietText("歌词由来源服务或用户文件提供，版权归原权利人所有。此页仅静态阅读，不播放或下载音乐。未找到的内容不会自动编造或替换为其他歌曲。")
            }
        }
    }
}
