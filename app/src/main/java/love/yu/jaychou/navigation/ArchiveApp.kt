package love.yu.jaychou.navigation

import love.yu.jaychou.ui.animation.AlbumOrigin
import love.yu.jaychou.ui.animation.AlbumSourcePreview
import love.yu.jaychou.ui.animation.LocalMovingAlbumId
import love.yu.jaychou.ui.components.AlbumCoverShape
import love.yu.jaychou.ui.components.CatalogImage
import love.yu.jaychou.ui.components.LocalArchiveBlur
import love.yu.jaychou.ui.effects.GlassLayer
import love.yu.jaychou.ui.effects.LocalArchiveLiquidGlass
import love.yu.jaychou.ui.navigation.ArchiveBottomNavigationBar
import love.yu.jaychou.ui.screens.about.AboutScreen
import love.yu.jaychou.ui.screens.albums.AlbumDetailScreen
import love.yu.jaychou.ui.screens.albums.AlbumsScreen
import love.yu.jaychou.ui.screens.concerts.ConcertsScreen
import love.yu.jaychou.ui.screens.home.HomeScreen
import love.yu.jaychou.ui.screens.introduction.IntroductionScreen
import love.yu.jaychou.ui.screens.lyrics.LyricsScreen
import love.yu.jaychou.ui.screens.settings.SettingsScreen
import love.yu.jaychou.data.AppText
import love.yu.jaychou.R
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
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
import love.yu.jaychou.ui.designsystem.Button
import love.yu.jaychou.ui.designsystem.Card
import love.yu.jaychou.ui.designsystem.Text
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import kotlin.math.min
import kotlin.math.roundToInt

private enum class RootPage { HOME, INTRODUCTION, SETTINGS, ABOUT }

/** Every tab tap, page swipe and bottom-bar scrub shares the same pager state. */
private class PageMotion(private val pager: PagerState, private val scope: CoroutineScope) {
    private var job: Job? = null
    fun select(index: Int) {
        job?.cancel()
        job = scope.launch { pager.animateScrollToPage(index, animationSpec = spring(dampingRatio = .9f, stiffness = 400f)) }
    }
    fun snap(index: Int) {
        job?.cancel()
        job = scope.launch { pager.scrollToPage(index) }
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
    onAlbumCategories: (List<String>) -> Unit,
    onCustomColor: (Int?) -> Unit,
    error: String?, onDismissError: () -> Unit,
    onLiquidGlass: (Boolean) -> Unit = {},
    onFloatingBottomBar: (Boolean) -> Unit = {},
    onUiStyle: (UiStyle) -> Unit = {},
) {
    val miuix = settings.uiStyle == UiStyle.MIUIX
    val useBlur = miuix && settings.blur
    val homeState = rememberLazyListState()
    val albumStateHolder = rememberSaveableStateHolder()
    val categories = settings.albumCategories
    val settingsState = rememberLazyListState()
    val aboutState = rememberLazyListState()
    val pager = rememberPagerState(pageCount = { RootPage.entries.size })
    val introductionPager = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val pageMotion = remember(pager, scope) { PageMotion(pager, scope) }
    val introductionMotion = remember(introductionPager, scope) { PageMotion(introductionPager, scope) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var searchTrackNumber by rememberSaveable { mutableStateOf<Int?>(null) }
    var albumSearchOpen by remember { mutableStateOf(false) }
    var lastDetailId by rememberSaveable { mutableStateOf<String?>(null) }
    val detail = catalog.find(lastDetailId)
    // Fractions survive Activity recreation and changing viewport dimensions.
    var coverOrigin by rememberSaveable { mutableStateOf(listOf(.28f, .3f, .44f, .3f)) }
    var albumSource by remember { mutableStateOf<AlbumOrigin?>(null) }
    var hostOrigin by remember { mutableStateOf(Offset.Zero) }
    var hostSize by remember { mutableStateOf(IntSize.Zero) }
    val expansion = remember { Animatable(if (detailId != null) 1f else 0f) }
    val backProgress = remember { Animatable(0f) }
    var concertDetailOpen by remember { mutableStateOf(false) }
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
    val navigationVisible by remember {
        derivedStateOf { (detailId == null || expansion.value < .999f) && !concertDetailOpen && !albumSearchOpen }
    }
    val predictiveSupported = Build.VERSION.SDK_INT >= 34 && settings.predictiveBack
    val background = ArchiveUiTheme.colorScheme.background
    val density = LocalDensity.current
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var navHeightPx by remember { mutableIntStateOf(with(density) { (96.dp + navInset).roundToPx() }) }
    val bottom = with(density) { navHeightPx.toDp() } + 16.dp
    val liquidGlass = miuix && settings.liquidGlass && settings.floatingBottomBar && Build.VERSION.SDK_INT >= 33

    fun openAlbum(album: Album, source: AlbumOrigin) {
        if (detailShown || pager.isScrollInProgress || introductionPager.isScrollInProgress || hostSize.width == 0 || hostSize.height == 0) return
        val bounds = source.windowBounds
        val cover = source.coverBounds ?: bounds
        albumSource = source
        coverOrigin = listOf((cover.left - hostOrigin.x) / hostSize.width,
            (cover.top - hostOrigin.y) / hostSize.height,
            cover.width / hostSize.width, cover.height / hostSize.height)
        lyricNumber = null; lastLyricNumber = null
        lastDetailId = album.id
        searchTrackNumber = null
        detailId = album.id
    }

    LaunchedEffect(detailId) {
        if (detailId != null) {
            expansion.animateTo(1f, spring(dampingRatio = .88f, stiffness = 320f))
        } else {
            expansion.snapTo((expansion.value - backProgress.value * .25f).coerceIn(0f, 1f))
            backProgress.snapTo(0f)
            expansion.animateTo(0f, tween(360))
            albumSource = null
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
                    val depth = (expansion.value - backProgress.value * .25f).coerceIn(0f, 1f)
                    scaleX = 1f - depth * .035f
                    scaleY = 1f - depth * .035f
                    alpha = 1f - depth * .12f
                    renderEffect = if (Build.VERSION.SDK_INT >= 33 && useBlur && depth > .001f) {
                        BlurEffect(18.dp.toPx() * depth, 18.dp.toPx() * depth, TileMode.Clamp)
                    } else null
                }.then(if (detailShown) Modifier.clearAndSetSemantics { } else Modifier)) {
                    HorizontalPager(
                        state = pager, beyondViewportPageCount = 1,
                        userScrollEnabled = !detailShown && !concertDetailOpen && !albumSearchOpen,
                        modifier = Modifier.fillMaxSize().testTag("root-pager"),
                        key = { RootPage.entries[it].name },
                    ) { page ->
                        Box(Modifier.fillMaxSize().background(background).testTag("page-${RootPage.entries[page].name}")) {
                            when (RootPage.entries[page]) {
                                RootPage.HOME -> HomeScreen(catalog, homeState, bottom, ::openAlbum, {
                                    introductionMotion.select(0)
                                    pageMotion.select(1)
                                }, profile = settings)
                                RootPage.INTRODUCTION -> IntroductionScreen(
                                    pager = introductionPager,
                                    enabled = !detailShown && !concertDetailOpen && !albumSearchOpen,
                                    headerVisible = !albumSearchOpen && !concertDetailOpen,
                                    onSelect = introductionMotion::snap,
                                ) { section ->
                                    Box(Modifier.fillMaxSize().testTag(if (section == 0) "page-ALBUMS" else "page-CONCERTS")) {
                                        if (section == 0) albumStateHolder.SaveableStateProvider("${settings.albumLayout.name}-${categories.sorted().joinToString()}") {
                                            val albumState = rememberLazyGridState()
                                            AlbumsScreen(catalog, settings.albumLayout, albumState, bottom, ::openAlbum,
                                                categories, onAlbumCategories,
                                                onSearchSong = { album, track, origin ->
                                                    openAlbum(album, origin)
                                                    if (detailId == album.id) searchTrackNumber = track.number
                                                },
                                                onSearchVisibility = { albumSearchOpen = it },
                                                searchInteractive = !detailShown,
                                                sectionHeader = true,
                                            )
                                        } else ConcertsScreen(bottom,
                                            onDetailVisibility = { concertDetailOpen = it },
                                            sectionHeader = true)
                                    }
                                }
                                RootPage.SETTINGS -> SettingsScreen(settings, settingsState, bottom,
                                    onTheme, onMonet, onBlur, onPredictive, onAlbumLayout, onCustomColor,
                                    onLiquidGlass, onFloatingBottomBar, onUiStyle)
                                RootPage.ABOUT -> AboutScreen(aboutState, bottom, active = pager.currentPage == page)
                            }
                        }
                    }
                }
                if (detailShown && useBlur) {
                    Box(Modifier.fillMaxSize().graphicsLayer {
                        alpha = (expansion.value - backProgress.value * .25f).coerceIn(0f, 1f) * .16f
                    }.background(androidx.compose.ui.graphics.Color.Black))
                }
                if (detailShown && detail != null) {
                    key(detail.id) {
                        val searchTrackIndex = detail.tracks.indexOfFirst { it.number == searchTrackNumber }
                        val detailState = rememberLazyListState(
                            initialFirstVisibleItemIndex = if (searchTrackIndex >= 0) 4 + searchTrackIndex else 0,
                        )
                        AnimatedAlbumPanel(detail, expansion, backProgress, coverOrigin, albumSource, hostOrigin,
                            hostSize, Modifier.graphicsLayer {
                                scaleX = 1f - lyricExpansion.value * .035f
                                scaleY = 1f - lyricExpansion.value * .035f
                            }.then(if (lyricShown) Modifier.clearAndSetSemantics { } else Modifier)) { coverMotion, bodyMotion, onCoverDetached ->
                            AlbumDetailScreen(detail, detailState, navInset + 28.dp, onBack = {
                                if (!lyricShown) detailId = null
                            }, coverModifier = coverMotion, bodyModifier = bodyMotion, onCoverDetached = onCoverDetached,
                                highlightedTrack = searchTrackNumber, onTrack = { track, source ->
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
                ArchiveBottomNavigationBar(
                    settings = settings,
                    selectedIndex = pager.targetPage,
                    pagePosition = { pager.currentPage + pager.currentPageOffsetFraction },
                    onSelect = pageMotion::select, onScrub = pageMotion::scrub,
                    enabled = !detailShown && !concertDetailOpen,
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .onSizeChanged { navHeightPx = it.height }
                        .graphicsLayer { alpha = 1f - expansion.value.coerceIn(0f, 1f); translationY = expansion.value * 24.dp.toPx() }
                        .then(if (detailShown) Modifier.clearAndSetSemantics { } else Modifier),
                )
            }
            if (error != null) {
                Card(Modifier.align(Alignment.TopCenter).padding(20.dp), insideMargin = PaddingValues(16.dp)) {
                    Text(error)
                    Button(onClick = onDismissError) { Text(AppText.get(R.string.ui_ok)) }
                }
            }
        }
    }
    CompositionLocalProvider(
        LocalArchiveBlur provides useBlur,
        LocalArchiveLiquidGlass provides liquidGlass,
        LocalMovingAlbumId provides if (detailShown) lastDetailId else null,
    ) {
    if (Build.VERSION.SDK_INT >= 33 && (useBlur || liquidGlass)) {
        GlassLayer(background, scene)
    } else {
        scene(Modifier, Modifier.background(ArchiveUiTheme.colorScheme.surfaceContainer))
    }
}
}

/** The surface, text and artwork share a clock but follow independent paths. */
@Composable
private fun AnimatedAlbumPanel(
    album: Album,
    expansion: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    back: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    origin: List<Float>, source: AlbumOrigin?, hostOrigin: Offset, hostSize: IntSize,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier, Modifier, () -> Unit) -> Unit,
) {
    var destination by remember(album.id) { mutableStateOf<Rect?>(null) }
    val density = LocalDensity.current
    val background = ArchiveUiTheme.colorScheme.background
    val progress = { (expansion.value - back.value * .25f).coerceIn(0f, 1f) }
    // Shield the pager while leaving the detail and its own scrolling interactive.
    Box(Modifier.fillMaxSize().pointerInput(Unit) {
        awaitPointerEventScope { while (true) awaitPointerEvent() }
    })
    Box(modifier.fillMaxSize().testTag("album-detail")) {
        Box(Modifier.fillMaxSize().graphicsLayer {
            val q = progress()
            alpha = q
            translationY = (1f - q) * 48.dp.toPx()
        }.background(background))
        val coverMotion = Modifier.onGloballyPositioned {
            val bounds = it.boundsInWindow()
            destination = bounds.translate(-hostOrigin)
        }.graphicsLayer {
            // Keep this node laid out so the independent artwork always has a real destination.
            alpha = if (progress() >= .999f || destination == null) 1f else 0f
        }
        val bodyMotion = Modifier.graphicsLayer {
            val q = progress()
            alpha = ((q - .12f) / .88f).coerceIn(0f, 1f)
            translationY = (1f - q) * 64.dp.toPx()
        }
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = progress() }) {
            content(coverMotion, bodyMotion) { destination = null }
        }
        val target = destination
        if (target != null && target.width > 0f && target.height > 0f && progress() < .999f) {
            // Draw at destination resolution and transform only the artwork, never the whole page.
            Box(
                Modifier.requiredSize(with(density) { target.width.toDp() }, with(density) { target.height.toDp() })
                    .graphicsLayer {
                        val q = progress()
                        // Follow the actual source as the pager depth and press feedback settle.
                        val start = source?.currentCoverBounds()?.translate(-hostOrigin)
                            ?: Rect(origin[0] * hostSize.width, origin[1] * hostSize.height,
                                (origin[0] + origin[2]) * hostSize.width,
                                (origin[1] + origin[3]) * hostSize.height)
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = (start.width / target.width) * (1f - q) + q
                        scaleY = (start.height / target.height) * (1f - q) + q
                        translationX = start.left * (1f - q) + target.left * q
                        translationY = start.top * (1f - q) + target.top * q
                        alpha = if (q < .999f) 1f else 0f
                    }.clip(AlbumCoverShape).clearAndSetSemantics { },
            ) {
                val sourceLayer = source?.layer?.takeIf { it.size.width > 0 && it.size.height > 0 }
                CatalogImage(
                    album.coverUrl, album.coverAsset, AppText.get(R.string.ui_artwork_232, album.title),
                    Modifier.fillMaxSize().graphicsLayer {
                        alpha = if (sourceLayer == null) 1f else progress()
                    },
                    contentScale = ContentScale.Fit,
                    crossfade = false,
                )
                // At the list endpoint this is the original rendering, including its crop.
                AlbumSourcePreview(sourceLayer) { 1f - progress() }
            }
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
        shadowElevation = 12.dp.toPx() * (1f - q) + p * 18.dp.toPx()
    }.background(ArchiveUiTheme.colorScheme.background).testTag(tag)) {
        Box(Modifier.fillMaxSize().graphicsLayer {
            alpha = if (layer == null) 1f else ((expansion.value - .12f) / .55f).coerceIn(0f, 1f)
        }) { content() }
        AlbumSourcePreview(layer) { (1f - expansion.value / .55f).coerceIn(0f, 1f) }
    }
}
