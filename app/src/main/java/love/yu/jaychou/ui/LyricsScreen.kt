package love.yu.jaychou.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
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
                is LyricsUnavailableException -> failure.message ?: "未能获取歌词，请稍后重试。"
                else -> "未能连接歌词服务，请检查网络后重试。"
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
                error = "导入失败：请使用不超过 1 MB 的 UTF-8 / GB18030 LRC 或 TXT 文件。"
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
        localText != null -> "本地歌词 · 已保存到应用私有目录"
        loading -> "正在匹配 QQ 音乐歌词"
        online != null -> "QQ 音乐歌词 · 已匹配"
        else -> "QQ 音乐 · 暂未匹配"
    }

    ArchiveScaffold(
        "歌词", small = true,
        navigationIcon = { BackButton(onBack, "返回专辑详情") },
        actions = {
            TextButton("导入", onClick = pickFile, modifier = Modifier.testTag("lyrics-import"))
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize()
                .padding(horizontal = 12.dp)
                .padding(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp,
                ),
        ) {
            Card(
                modifier = Modifier.align(Alignment.TopCenter).widthIn(max = 600.dp).fillMaxWidth()
                    .fillMaxHeight().testTag("lyrics-card"),
                cornerRadius = 30.dp,
                insideMargin = PaddingValues(0.dp),
            ) {
                Column(Modifier.fillMaxSize()) {
                    LyricsHeader(
                        album = album, track = track, sourceLabel = sourceLabel,
                        translationAvailable = translationAvailable, showTranslation = showTranslation,
                        onToggleTranslation = { showTranslation = !showTranslation },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 22.dp), color = MiuixTheme.colorScheme.dividerLine)
                    when {
                        loading -> LyricsLoading()
                        error != null -> LyricsError(error!!, onRetry = { retry++ }, onImport = pickFile)
                        lines.isEmpty() -> LyricsError(
                            if (localText != null) "本地歌词文件里没有可显示的文本行。"
                            else "QQ 音乐没有收录这首歌的歌词。",
                            onRetry = { retry++ }, onImport = pickFile,
                            title = "还没有歌词",
                        )
                        else -> LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f).fillMaxWidth().testTag("lyrics-list"),
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 20.dp),
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
                                                error = "删除失败，请重试。"
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
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("歌词", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                color = MiuixTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            if (translationAvailable) {
                TranslationChip(active = showTranslation, onClick = onToggleTranslation)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AlbumThumbnail(album)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    track.title, fontSize = 25.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                QuietText("周杰伦 · ${album.title}", maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(sourceLabel, fontSize = 12.sp, color = MiuixTheme.colorScheme.primary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AlbumThumbnail(album: Album) {
    val context = LocalContext.current
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context).data(album.coverUrl).crossfade(true).build(),
        contentDescription = "《${album.title}》专辑封面",
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(20.dp))
            .background(MiuixTheme.colorScheme.surfaceContainerHigh),
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
    val background = if (active) MiuixTheme.colorScheme.primaryContainer else MiuixTheme.colorScheme.surfaceContainerHigh
    val foreground = if (active) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onSurfaceContainerVariant
    Card(
        onClick = onClick,
        cornerRadius = 50.dp,
        insideMargin = PaddingValues(horizontal = 13.dp, vertical = 7.dp),
        colors = CardDefaults.defaultColors(color = background, contentColor = foreground),
        pressFeedbackType = PressFeedbackType.Sink,
    ) {
        Text(
            if (active) "双语" else "原文", fontSize = 12.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.semantics { contentDescription = "切换歌词翻译显示" },
        )
    }
}

@Composable
private fun LyricsLoading() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(18.dp))
        Text("正在匹配歌词", fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("lyrics-loading"))
        Spacer(Modifier.height(6.dp))
        Text("通过 QQ 音乐歌词接口查找周杰伦版本", fontSize = 13.sp, lineHeight = 20.sp,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LyricsError(
    message: String,
    onRetry: () -> Unit,
    onImport: () -> Unit,
    title: String = "暂时没有拿到歌词",
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(9.dp))
        Text(message, fontSize = 14.sp, lineHeight = 22.sp,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRetry, modifier = Modifier.testTag("lyrics-retry")) { Text("重试") }
            TextButton("导入本地", onClick = onImport)
        }
    }
}

@Composable
private fun LyricLineView(line: LyricLine, showTranslation: Boolean) {
    if (line.text.isBlank()) {
        Spacer(Modifier.height(12.dp))
    } else {
        Column(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
            Text(line.text, fontSize = 19.sp, lineHeight = 31.sp)
            if (showTranslation && line.translation != null) {
                Spacer(Modifier.height(4.dp))
                Text(line.translation, fontSize = 14.sp, lineHeight = 22.sp,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
            }
        }
    }
}

@Composable
private fun LyricsFooter(local: Boolean, online: OnlineLyrics?, onRemoveLocal: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
        HorizontalDivider(color = MiuixTheme.colorScheme.dividerLine)
        Spacer(Modifier.height(14.dp))
        Text(
            if (local) "本地歌词 · 已保存在应用私有目录" else "QQ 音乐歌词 · 逐行读取自歌词接口",
            fontSize = 13.sp, lineHeight = 20.sp, color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
        if (!local && online != null) {
            Spacer(Modifier.height(3.dp))
            Text(
                "QQ 音乐标注：${online.album.ifBlank { "未标注专辑" }}" +
                    if (online.releaseDate.isBlank()) "" else " · ${online.releaseDate}",
                fontSize = 12.sp, lineHeight = 19.sp, color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
        }
        SourceLink("歌词接口：落月 API · QQ 音乐", LyricsRepository.source)
        Text(
            "歌词版权归原权利人所有；此页仅供静态阅读，不播放或下载音乐。",
            fontSize = 12.sp, lineHeight = 19.sp, color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
        if (local) {
            Spacer(Modifier.height(4.dp))
            TextButton("删除本地歌词", onClick = onRemoveLocal)
        }
    }
}
