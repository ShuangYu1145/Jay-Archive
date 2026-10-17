package love.yu.jaychou.navigation

import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.*
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.min
import kotlin.math.roundToInt

private enum class RootPage { HOME, ALBUMS, CONCERTS, SETTINGS, ABOUT }

/** Every tab tap, page swipe and bottom-bar scrub shares the same pager state. */
private class PageMotion(private val pager: PagerState, private val scope: CoroutineScope) {
    private var job: Job? = null
    fun select(index: Int) {
        job?.cancel()
        job = scope.launch { pager.animateScrollToPage(index, animationSpec = spring(dampingRatio = .9f, stiffness = 400f)) }
    }
    fun scrub(position: Float) {
        job?.cancel()
        val page = position.roundToInt().coerceIn(0, pager.pageCount - 1)
        job = scope.launch { pager.scrollToPage(page, (position - page).coerceIn(-.5f, .5f)) }
    }
}

@Composable
fun ArchiveApp(
    catalog: AlbumCatalog, settings: AppSettings,
    onTheme: (ThemeMode) -> Unit, onMonet: (Boolean) -> Unit,
    onBlur: (Boolean) -> Unit, onPredictive: (Boolean) -> Unit,
    onAlbumLayout: (AlbumLayout) -> Unit,
    onCustomColor: (Int?) -> Unit,
    error: String?, onDismissError: () -> Unit,
) {
    val homeState = rememberLazyListState()
    val albumStateHolder = rememberSaveableStateHolder()
    var categories by rememberSaveable { mutableStateOf(listOf(ReleaseCategory.STUDIO.name)) }
    val settingsState = rememberLazyListState()
    val aboutState = rememberLazyListState()
    val pager = rememberPagerState(pageCount = { RootPage.entries.size })
    val scope = rememberCoroutineScope()
    val pageMotion = remember(pager, scope) { PageMotion(pager, scope) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var lastDetailId by rememberSaveable { mutableStateOf<String?>(null) }
    val detail = catalog.find(lastDetailId)
    // Fractions survive Activity recreation and changing viewport dimensions.
    var origin by rememberSaveable { mutableStateOf(listOf(.28f, .3f, .44f, .3f)) }
    var sourceLayer by remember { mutableStateOf<GraphicsLayer?>(null) }
    var hostOrigin by remember { mutableStateOf(Offset.Zero) }
    var hostSize by remember { mutableStateOf(IntSize.Zero) }
    val expansion = remember { Animatable(if (detailId != null) 1f else 0f) }
    val backProgress = remember { Animatable(0f) }
    var lyricNumber by rememberSaveable { mutableStateOf<Int?>(null) }
    var lastLyricNumber by rememberSaveable { mutableStateOf<Int?>(null) }
    var lyricOrigin by rememberSaveable { mutableStateOf(listOf(.1f, .4f, .8f, .08f)) }
    var lyricLayer by remember { mutableStateOf<GraphicsLayer?>(null) }
    val lyricExpansion = remember { Animatable(if (lyricNumber != null) 1f else 0f) }
    val lyricBack = remember { Animatable(0f) }
    val lyricShown by remember { derivedStateOf { lyricNumber != null || lyricExpansion.value > 0f } }
    val lyricTrack = detail?.tracks?.find { it.number == lastLyricNumber }
    var swipeDirection by remember { mutableFloatStateOf(1f) }
    val detailShown by remember { derivedStateOf { detailId != null || expansion.value > 0f } }
    val navigationVisible by remember { derivedStateOf { detailId == null || expansion.value < .999f } }
    val predictiveSupported = Build.VERSION.SDK_INT >= 34 && settings.predictiveBack
    val dark = settings.isDark()
    val background = MiuixTheme.colorScheme.background
    val density = LocalDensity.current
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var navHeightPx by remember { mutableIntStateOf(with(density) { 76.dp.roundToPx() }) }
    val bottom = with(density) { navHeightPx.toDp() } + navInset + 38.dp

    fun openAlbum(album: Album, source: AlbumOrigin) {
        if (detailShown || pager.isScrollInProgress || hostSize.width == 0 || hostSize.height == 0) return
        val bounds = source.windowBounds
        if (bounds.width > 0f && bounds.height > 0f) {
            origin = listOf((bounds.left - hostOrigin.x) / hostSize.width,
                (bounds.top - hostOrigin.y) / hostSize.height,
                bounds.width / hostSize.width, bounds.height / hostSize.height)
        }
        lyricNumber = null; lastLyricNumber = null
        sourceLayer = source.layer
        lastDetailId = album.id
        detailId = album.id
    }

    LaunchedEffect(detailId) {
        if (detailId != null) {
            expansion.animateTo(1f, spring(dampingRatio = .88f, stiffness = 320f))
        } else {
            coroutineScope {
                launch { backProgress.animateTo(0f, tween(320)) }
                expansion.animateTo(0f, tween(360))
            }
            sourceLayer = null
        }
    }
    LaunchedEffect(lyricNumber) {
        if (lyricNumber != null) lyricExpansion.animateTo(1f, spring(dampingRatio = .9f, stiffness = 320f))
        else {
            coroutineScope {
                launch { lyricBack.animateTo(0f, tween(320)) }
                lyricExpansion.animateTo(0f, tween(360))
            }
            lyricLayer = null
        }
    }
    fun closeTop() {
        if (lyricShown) lyricNumber = null else detailId = null
    }
    BackHandler(enabled = detailId != null && !predictiveSupported) { closeTop() }
    BackHandler(enabled = (detailId == null && detailShown) || (lyricNumber == null && lyricShown)) { }
    PredictiveBackHandler(enabled = detailId != null && predictiveSupported && !(lyricShown && lyricNumber == null)) { events ->
        val progress = if (lyricNumber != null) lyricBack else backProgress
        try {
            events.collect { event ->
                swipeDirection = if (event.swipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
                progress.snapTo(event.progress)
            }
            closeTop()
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { progress.animateTo(0f, spring(dampingRatio = 1f, stiffness = 500f)) }
            throw cancelled
        }
    }

    val scene: @Composable (Modifier, Modifier) -> Unit = { capture, glass ->
        Box(Modifier.fillMaxSize().background(background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)).clipToBounds()
            .onGloballyPositioned { hostOrigin = it.positionInWindow(); hostSize = it.size }) {
            Box(Modifier.fillMaxSize().then(capture)) {
                Box(Modifier.fillMaxSize().graphicsLayer {
                    val depth = expansion.value.coerceIn(0f, 1f)
                    scaleX = 1f - depth * .035f
                    scaleY = 1f - depth * .035f
                    alpha = 1f - depth * .12f
                }.then(if (detailShown) Modifier.clearAndSetSemantics { } else Modifier)) {
                    HorizontalPager(
                        state = pager, beyondViewportPageCount = 1,
                        userScrollEnabled = !detailShown,
                        modifier = Modifier.fillMaxSize().testTag("root-pager"),
                        key = { RootPage.entries[it].name },
                    ) { page ->
                        Box(Modifier.fillMaxSize().background(background).testTag("page-${RootPage.entries[page].name}")) {
                            when (RootPage.entries[page]) {
                                RootPage.HOME -> HomeScreen(catalog, homeState, bottom, ::openAlbum, { pageMotion.select(1) })
                                RootPage.ALBUMS -> albumStateHolder.SaveableStateProvider("${settings.albumLayout.name}-${categories.sorted().joinToString()}") {
                                    val albumState = rememberLazyGridState()
                                    AlbumsScreen(catalog, settings.albumLayout, albumState, bottom, ::openAlbum,
                                        categories, { categories = it })
                                }
                                RootPage.CONCERTS -> ArchiveScaffold("演唱会") { }
                                RootPage.SETTINGS -> SettingsScreen(settings, settingsState, bottom,
                                    onTheme, onMonet, onBlur, onPredictive, onAlbumLayout, onCustomColor)
                                RootPage.ABOUT -> AboutScreen(aboutState, bottom)
                            }
                        }
                    }
                }
                if (detailShown && detail != null) {
                    key(detail.id) {
                        val detailState = rememberLazyListState()
                        AnimatedArchivePanel(expansion, backProgress, origin, sourceLayer, swipeDirection,
                            "album-detail", Modifier.graphicsLayer {
                                scaleX = 1f - lyricExpansion.value * .035f
                                scaleY = 1f - lyricExpansion.value * .035f
                            }.then(if (lyricShown) Modifier.clearAndSetSemantics { } else Modifier)) {
                            AlbumDetailScreen(detail, detailState, navInset + 28.dp, onBack = {
                                if (!lyricShown) detailId = null
                            }, onTrack = { track, source ->
                                if (!lyricShown && expansion.value >= .999f) {
                                    val bounds = source.windowBounds
                                    lyricOrigin = listOf((bounds.left - hostOrigin.x) / hostSize.width,
                                        (bounds.top - hostOrigin.y) / hostSize.height,
                                        bounds.width / hostSize.width, bounds.height / hostSize.height)
                                    lyricLayer = source.layer; lastLyricNumber = track.number; lyricNumber = track.number
                                }
                            })
                        }
                    }
                    if (lyricShown && lyricTrack != null) {
                        key(detail.id, lyricTrack.number) {
                            AnimatedArchivePanel(lyricExpansion, lyricBack, lyricOrigin, lyricLayer, swipeDirection, "lyrics-detail") {
                                LyricsScreen(detail, lyricTrack, onBack = { lyricNumber = null })
                            }
                        }
                    }
                }
            }
            if (navigationVisible) {
                DraggableNavigationBar(
                    selectedIndex = pager.currentPage,
                    pagePosition = { pager.currentPage + pager.currentPageOffsetFraction },
                    onSelect = pageMotion::select, onScrub = pageMotion::scrub,
                    glass = glass, enabled = !detailShown,
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .padding(start = 28.dp, end = 28.dp, bottom = navInset + 12.dp)
                        .widthIn(max = 460.dp).fillMaxWidth().onSizeChanged { navHeightPx = it.height }
                        .graphicsLayer { alpha = 1f - expansion.value.coerceIn(0f, 1f); translationY = expansion.value * 24.dp.toPx() }
                        .then(if (detailShown) Modifier.clearAndSetSemantics { } else Modifier),
                )
            }
            if (error != null) {
                Card(Modifier.align(Alignment.TopCenter).padding(20.dp), insideMargin = PaddingValues(16.dp)) {
                    Text(error)
                    Button(onClick = onDismissError) { Text("知道了") }
                }
            }
        }
    }
    CompositionLocalProvider(LocalArchiveBlur provides settings.blur) {
    if (Build.VERSION.SDK_INT >= 33 && settings.blur) {
        GlassLayer(background, dark, scene)
    } else {
        scene(Modifier, Modifier.background(MiuixTheme.colorScheme.surfaceContainer, RoundedCornerShape(40.dp)))
    }
}
}

/** Each overlay owns a sibling touch shield; list ancestors never consume Final-pass events. */
@Composable
private fun AnimatedArchivePanel(
    expansion: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    back: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    origin: List<Float>, layer: GraphicsLayer?, direction: Float, tag: String,
    modifier: Modifier = Modifier, content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize().pointerInput(Unit) {
        awaitPointerEventScope { while (true) awaitPointerEvent() }
    })
    Box(modifier.fillMaxSize().graphicsLayer {
        val q = expansion.value.coerceIn(0f, 1f)
        val p = back.value
        val sx = (origin[2].coerceIn(.05f, 1f) + (1f - origin[2].coerceIn(.05f, 1f)) * q) * (1f - p * .08f)
        val sy = (origin[3].coerceIn(.025f, 1f) + (1f - origin[3].coerceIn(.025f, 1f)) * q) * (1f - p * .08f)
        transformOrigin = TransformOrigin(0f, 0f)
        scaleX = sx; scaleY = sy
        translationX = origin[0] * size.width * (1f - q) + direction * p * size.width * .24f
        translationY = origin[1] * size.height * (1f - q) + p * size.height * .04f
        shape = ProjectedCornerShape((23f * (1f - q) + p * 30f).dp.toPx(), sx, sy)
        clip = true
        shadowElevation = 12.dp.toPx() * (1f - q) + p * 18.dp.toPx()
    }.background(MiuixTheme.colorScheme.background).testTag(tag)) {
        Box(Modifier.fillMaxSize().graphicsLayer {
            alpha = if (layer == null) 1f else ((expansion.value - .12f) / .55f).coerceIn(0f, 1f)
        }) { content() }
        AlbumSourcePreview(layer) { (1f - expansion.value / .55f).coerceIn(0f, 1f) }
    }
}
