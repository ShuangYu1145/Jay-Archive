package love.yu.jaychou.navigation

import love.yu.jaychou.ui.animation.applySecondaryBackTransform
import love.yu.jaychou.ui.animation.albumTransitionProgress
import love.yu.jaychou.ui.animation.MiniPlayerDetailsHost
import love.yu.jaychou.ui.animation.AlbumOrigin
import love.yu.jaychou.ui.animation.AlbumSourcePreview
import love.yu.jaychou.ui.animation.LocalMovingAlbumId
import love.yu.jaychou.ui.components.AlbumCoverShape
import love.yu.jaychou.ui.components.CatalogImage
import love.yu.jaychou.ui.components.MiniMusicPlayer
import love.yu.jaychou.ui.components.LocalArchiveBlur
import love.yu.jaychou.ui.effects.GlassLayer
import love.yu.jaychou.ui.effects.CustomBackgroundHost
import love.yu.jaychou.ui.effects.CustomBackgroundImage
import love.yu.jaychou.ui.effects.LocalArchiveLiquidGlass
import love.yu.jaychou.ui.navigation.ArchiveBottomNavigationBar
import love.yu.jaychou.ui.navigation.ArchiveNavigationRail
import love.yu.jaychou.ui.screens.albums.AlbumDetailScreen
import love.yu.jaychou.ui.screens.albums.AlbumsScreen
import love.yu.jaychou.ui.screens.concerts.ConcertsScreen
import love.yu.jaychou.ui.screens.home.HomeScreen
import love.yu.jaychou.ui.screens.introduction.IntroductionScreen
import love.yu.jaychou.ui.screens.settings.SettingsScreen
import love.yu.jaychou.data.AppText
import love.yu.jaychou.playback.rememberMusicPlaybackState
import love.yu.jaychou.online.*
import love.yu.jaychou.ui.screens.music.OnlineMusicScreen
import love.yu.jaychou.ui.screens.music.onlineError
import love.yu.jaychou.R
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.PagerScope
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
import love.yu.jaychou.ui.designsystem.CouiWindowMetrics
import love.yu.jaychou.ui.designsystem.LocalCouiWindowMetrics
import kotlin.math.min
import kotlin.math.roundToInt

private enum class RootPage { HOME, INTRODUCTION, ONLINE, SETTINGS }

@Composable
private fun RootPager(
    state: PagerState,
    vertical: Boolean,
    userScrollEnabled: Boolean,
    content: @Composable PagerScope.(Int) -> Unit,
) {
    val modifier = Modifier.fillMaxSize().testTag("root-pager")
    if (vertical) {
        VerticalPager(
            state = state, beyondViewportPageCount = 1,
            userScrollEnabled = userScrollEnabled, modifier = modifier,
            key = { RootPage.entries[it].name }, pageContent = content,
        )
    } else {
        HorizontalPager(
            state = state, beyondViewportPageCount = 1,
            userScrollEnabled = userScrollEnabled, modifier = modifier,
            key = { RootPage.entries[it].name }, pageContent = content,
        )
    }
}

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
    onAlbumCategories: (List<String>) -> Unit,
    onCustomColor: (Int?) -> Unit,
    error: String?, onDismissError: () -> Unit,
    onLiquidGlass: (Boolean) -> Unit = {},
    onFloatingBottomBar: (Boolean) -> Unit = {},
    onUiStyle: (UiStyle) -> Unit = {},
    onTopAppBarBlur: (love.yu.jaychou.data.TopAppBarBlur) -> Unit = {},
    openMusicPlayer: Boolean = false,
    onMusicPlayerOpened: () -> Unit = {},
) {
    CustomBackgroundHost(settings) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
    // Use the current window, so rotation and tablet split-screen resize immediately.
    val useNavigationRail = maxWidth >= 600.dp || maxWidth > maxHeight
    // COUI reads one orientation-aware set of measurements on every screen.
    val couiMetrics = CouiWindowMetrics.of(maxWidth, maxHeight)
    val useBlur = settings.blur
    val scope = rememberCoroutineScope()
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val musicRepository = remember(appContext) { OnlineMusicRepository(DeviceMusicAccountStore(appContext)) }
    val musicPlayback = rememberMusicPlaybackState(musicRepository)
    val onlineMusic = remember(musicRepository, scope, musicPlayback) { OnlineMusicState(musicRepository, scope, musicPlayback) }
    var playerDetailsOpen by rememberSaveable { mutableStateOf(false) }
    var miniPlayerBounds by remember { mutableStateOf<Rect?>(null) }
    var miniArtworkBounds by remember { mutableStateOf<Rect?>(null) }
    val playingTrack = musicPlayback.currentSong
    val homeState = rememberLazyListState()
    val albumStateHolder = rememberSaveableStateHolder()
    val categories = settings.albumCategories
    val settingsState = rememberLazyListState()
    val pager = rememberPagerState(pageCount = { RootPage.entries.size })
    val introductionPager = rememberPagerState(pageCount = { 2 })
    val pageMotion = remember(pager, scope) { PageMotion(pager, scope) }
    val introductionMotion = remember(introductionPager, scope) { PageMotion(introductionPager, scope) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var searchTrackNumber by rememberSaveable { mutableStateOf<Int?>(null) }
    var settingsDetailOpen by remember { mutableStateOf(false) }
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
    var backEdge by remember { mutableFloatStateOf(1f) }
    var backPivotY by remember { mutableFloatStateOf(.5f) }
    var concertDetailOpen by remember { mutableStateOf(false) }
    val detailShown by remember { derivedStateOf { detailId != null || expansion.value > 0f } }
    var coverFlowHeaderHidden by rememberSaveable { mutableStateOf(false) }
    var coverFlowNavigationHidden by rememberSaveable { mutableStateOf(false) }
    val coverFlowLayout by rememberUpdatedState(settings.albumLayout == AlbumLayout.COVER_FLOW)
    val coverFlowSection by remember { derivedStateOf {
        coverFlowLayout && pager.currentPage == RootPage.INTRODUCTION.ordinal && introductionPager.currentPage == 0
    } }
    val coverFlowActive by remember { derivedStateOf {
        coverFlowSection && !detailShown && !albumSearchOpen && !concertDetailOpen
    } }
    val coverFlowHidesNavigation = coverFlowSection && coverFlowNavigationHidden
    BackHandler(enabled = coverFlowActive && (coverFlowHeaderHidden || coverFlowNavigationHidden)) {
        coverFlowHeaderHidden = false
        coverFlowNavigationHidden = false
    }
    val navigationVisible by remember {
        derivedStateOf { (detailId == null || expansion.value < .999f) && !concertDetailOpen && !albumSearchOpen && !settingsDetailOpen &&
            !(coverFlowSection && coverFlowNavigationHidden) }
    }
    val bottomReveal = remember { Animatable(if (navigationVisible) 1f else 0f) }
    LaunchedEffect(navigationVisible) {
        if (navigationVisible) bottomReveal.animateTo(1f, tween(320))
        else bottomReveal.snapTo(0f)
    }
    val predictiveSupported = Build.VERSION.SDK_INT >= 34 && settings.predictiveBack && settings.predictiveBackAnimation != PredictiveBackAnimation.NONE
    val background = ArchiveUiTheme.colorScheme.background
    val density = LocalDensity.current
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var navHeightPx by remember { mutableIntStateOf(with(density) { (96.dp + navInset).roundToPx() }) }
    var railWidthPx by remember { mutableIntStateOf(with(density) { 81.dp.roundToPx() }) }
    // Avoid a one-frame inset jump when COUI changes orientation or width bucket.
    LaunchedEffect(settings.uiStyle, couiMetrics.railWidth) {
        railWidthPx = with(density) {
            (if (settings.uiStyle == UiStyle.COUI) couiMetrics.railWidth else 81.dp).roundToPx()
        }
    }
    val bottom = if (useNavigationRail) navInset + (if (musicPlayback.hasPlayback) 96.dp else 16.dp) else with(density) { navHeightPx.toDp() } + 16.dp
    val liquidGlass = settings.liquidGlass && settings.floatingBottomBar && Build.VERSION.SDK_INT >= 33

    fun openAlbum(album: Album, source: AlbumOrigin) {
        if (detailShown || pager.isScrollInProgress || introductionPager.isScrollInProgress || hostSize.width == 0 || hostSize.height == 0) return
        val bounds = source.windowBounds
        val cover = source.coverBounds ?: bounds
        albumSource = source
        coverOrigin = listOf((cover.left - hostOrigin.x) / hostSize.width,
            (cover.top - hostOrigin.y) / hostSize.height,
            cover.width / hostSize.width, cover.height / hostSize.height)
        lastDetailId = album.id
        searchTrackNumber = null
        detailId = album.id
    }

    LaunchedEffect(detailId) {
        if (detailId != null) {
            backEdge = 1f; backPivotY = .5f
            expansion.animateTo(1f, spring(dampingRatio = .88f, stiffness = 320f))
        } else {
            if (backProgress.value == 0f) backEdge = 1f
            expansion.animateTo(0f, tween(360))
            backProgress.snapTo(0f)
            albumSource = null
        }
    }
    LaunchedEffect(openMusicPlayer) {
        if (openMusicPlayer) {
            pager.scrollToPage(RootPage.ONLINE.ordinal)
            playerDetailsOpen = true
            onMusicPlayerOpened()
        }
    }
    fun closeTop() { detailId = null }
    BackHandler(enabled = detailId != null && !predictiveSupported) { closeTop() }
    BackHandler(enabled = detailId == null && detailShown) { }
    PredictiveBackHandler(enabled = detailId != null && predictiveSupported) { events ->
        val progress = backProgress
        try {
            events.collect { event ->
                backEdge = if (event.swipeEdge == 0) 1f else -1f
                backPivotY = ((event.touchY - hostOrigin.y) / hostSize.height.coerceAtLeast(1)).coerceIn(.1f, .9f)
                progress.snapTo(event.progress)
            }
            closeTop()
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { progress.animateTo(0f, spring(dampingRatio = 1f, stiffness = 500f)) }
            throw cancelled
        }
    }

    val scene: @Composable (Modifier, Modifier) -> Unit = { capture, glass ->
        MiniPlayerDetailsHost(playerDetailsOpen, miniPlayerBounds, miniArtworkBounds, musicPlayback,
            playingTrack?.title ?: AppText.get(R.string.online_title),
            playingTrack?.artist ?: "",
            onBack = { playerDetailsOpen = false }, root = {
        Box(Modifier.fillMaxSize().background(ArchiveUiTheme.pageBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)).clipToBounds()
            .onGloballyPositioned { hostOrigin = it.positionInWindow(); hostSize = it.size }) {
            Box(Modifier.fillMaxSize().then(capture)) {
                Box(Modifier.fillMaxSize()
                    .padding(start = if (useNavigationRail && !coverFlowHidesNavigation && !concertDetailOpen && !albumSearchOpen && !settingsDetailOpen)
                        with(density) { railWidthPx.toDp() } else 0.dp)
                    .graphicsLayer {
                    val depth = albumTransitionProgress(expansion.value, backProgress.value)
                    scaleX = 1f - depth * .035f
                    scaleY = 1f - depth * .035f
                    alpha = 1f - depth * .12f
                    renderEffect = if (Build.VERSION.SDK_INT >= 33 && useBlur && depth > .001f) {
                        BlurEffect(18.dp.toPx() * depth, 18.dp.toPx() * depth, TileMode.Clamp)
                    } else null
                }.then(if (detailShown) Modifier.clearAndSetSemantics { } else Modifier)) {
                    RootPager(
                        state = pager, vertical = useNavigationRail && settings.uiStyle != UiStyle.COUI,
                        userScrollEnabled = !(useNavigationRail && settings.uiStyle == UiStyle.COUI) &&
                            !detailShown && !concertDetailOpen && !albumSearchOpen && !settingsDetailOpen &&
                            !(coverFlowActive && (coverFlowHeaderHidden || coverFlowNavigationHidden)),
                    ) { page ->
                        Box(Modifier.fillMaxSize().background(ArchiveUiTheme.pageBackground).testTag("page-${RootPage.entries[page].name}")) {
                            when (RootPage.entries[page]) {
                                RootPage.HOME -> HomeScreen(catalog, homeState, bottom, ::openAlbum, {
                                    introductionMotion.select(0)
                                    pageMotion.select(1)
                                }, profile = settings)
                                RootPage.INTRODUCTION -> IntroductionScreen(
                                    pager = introductionPager,
                                    enabled = !detailShown && !concertDetailOpen && !albumSearchOpen && !settingsDetailOpen &&
                                        !(coverFlowActive && (coverFlowHeaderHidden || coverFlowNavigationHidden)),
                                    headerVisible = !albumSearchOpen && !concertDetailOpen &&
                                        !(coverFlowSection && coverFlowHeaderHidden),
                                    onSelect = introductionMotion::select,
                                ) { section ->
                                    Box(Modifier.fillMaxSize().testTag(when (section) {
                                        0 -> "page-ALBUMS"
                                        else -> "page-CONCERTS"
                                    })) {
                                        if (section == 0) albumStateHolder.SaveableStateProvider("${settings.albumLayout.name}-${categories.sorted().joinToString()}") {
                                            val albumState = rememberLazyGridState()
                                            AlbumsScreen(catalog, settings.albumLayout, albumState,
                                                if (coverFlowHidesNavigation) navInset + 8.dp else bottom, ::openAlbum,
                                                categories, onAlbumCategories,
                                                onSearchSong = { album, track, origin ->
                                                    openAlbum(album, origin)
                                                    if (detailId == album.id) searchTrackNumber = track.number
                                                },
                                                onSearchVisibility = { albumSearchOpen = it },
                                                searchInteractive = !detailShown,
                                                sectionHeader = true,
                                                coverFlowHeaderHidden = coverFlowSection && coverFlowHeaderHidden,
                                                coverFlowNavigationHidden = coverFlowSection && coverFlowNavigationHidden,
                                                onCoverFlowHeaderHidden = { coverFlowHeaderHidden = it },
                                                onCoverFlowNavigationHidden = { coverFlowNavigationHidden = it },
                                            )
                                        } else ConcertsScreen(bottom,
                                            onDetailVisibility = { concertDetailOpen = it },
                                            sectionHeader = true)
                                    }
                                }
                                RootPage.ONLINE -> OnlineMusicScreen(onlineMusic, bottom, active = pager.currentPage == page)
                                RootPage.SETTINGS -> SettingsScreen(settings, settingsState, bottom,
                                    onTheme, onMonet, onBlur, onPredictive, onAlbumLayout, onCustomColor,
                                    onLiquidGlass, onFloatingBottomBar, onUiStyle, onTopAppBarBlur,
                                    onDetailVisibility = { settingsDetailOpen = it },
                                    active = pager.currentPage == page)
                            }
                        }
                    }
                }
                if (detailShown && useBlur) {
                    Box(Modifier.fillMaxSize().graphicsLayer {
                        alpha = albumTransitionProgress(expansion.value, backProgress.value) * .16f
                    }.background(androidx.compose.ui.graphics.Color.Black))
                }
                if (detailShown && detail != null) {
                    key(detail.id) {
                        val searchTrackIndex = detail.tracks.indexOfFirst { it.number == searchTrackNumber }
                        val detailState = rememberLazyListState(
                            initialFirstVisibleItemIndex = if (searchTrackIndex >= 0) 4 + searchTrackIndex else 0,
                        )
                        AnimatedAlbumPanel(detail, expansion, backProgress, coverOrigin, albumSource, hostOrigin,
                            hostSize, swipeEdge = backEdge, pivotY = backPivotY, exiting = detailId == null) { coverMotion, bodyMotion, onCoverDetached ->
                            AlbumDetailScreen(detail, detailState, navInset + 28.dp, onBack = { detailId = null },
                                coverModifier = coverMotion, bodyModifier = bodyMotion, onCoverDetached = onCoverDetached,
                                highlightedTrack = searchTrackNumber, playback = musicPlayback,
                                onPlayTrack = { onlineMusic.playCatalog(detail, it) })
                        }
                    }
                }
            }
            if (navigationVisible) {
                if (useNavigationRail) {
                    ArchiveNavigationRail(
                        uiStyle = settings.uiStyle,
                        selectedIndex = pager.targetPage,
                        onSelect = pageMotion::select,
                        enabled = !detailShown && !concertDetailOpen,
                        metrics = couiMetrics,
                        modifier = Modifier.align(Alignment.CenterStart)
                            .onSizeChanged { railWidthPx = it.width }
                            .graphicsLayer { alpha = 1f - expansion.value.coerceIn(0f, 1f) }
                            .then(if (detailShown) Modifier.clearAndSetSemantics { } else Modifier),
                    )
                    if (musicPlayback.hasPlayback) {
                        Box(Modifier.align(Alignment.BottomCenter)
                            .padding(start = with(density) { railWidthPx.toDp() }, bottom = navInset + 12.dp)
                            .padding(horizontal = 20.dp)
                            .graphicsLayer { alpha = 1f - expansion.value.coerceIn(0f, 1f) }
                            .then(if (detailShown) Modifier.clearAndSetSemantics { } else Modifier)) {
                            MiniMusicPlayer(musicPlayback,
                                playingTrack?.title ?: AppText.get(R.string.online_title),
                                playingTrack?.artist ?: "",
                                onOpen = { playerDetailsOpen = true },
                                    modifier = Modifier.onGloballyPositioned { miniPlayerBounds = it.boundsInWindow() },
                                    onArtworkBounds = { miniArtworkBounds = it })
                        }
                    }
                } else {
                    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .onSizeChanged { navHeightPx = it.height }
                        .graphicsLayer {
                            alpha = bottomReveal.value * (1f - expansion.value.coerceIn(0f, 1f))
                            translationY = (1f - bottomReveal.value) * size.height
                        }
                        .then(if (detailShown) Modifier.clearAndSetSemantics { } else Modifier),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedVisibility(musicPlayback.hasPlayback,
                            enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 2 },
                            exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 2 }) {
                            Box(Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp)) {
                                MiniMusicPlayer(musicPlayback,
                                    playingTrack?.title ?: AppText.get(R.string.online_title),
                                    playingTrack?.artist ?: "",
                                    onOpen = { playerDetailsOpen = true },
                                    modifier = Modifier.onGloballyPositioned { miniPlayerBounds = it.boundsInWindow() },
                                    onArtworkBounds = { miniArtworkBounds = it })
                            }
                        }
                        ArchiveBottomNavigationBar(
                            settings = settings,
                            selectedIndex = pager.targetPage,
                            pagePosition = { pager.currentPage + pager.currentPageOffsetFraction },
                            onSelect = pageMotion::select, onScrub = pageMotion::scrub,
                            enabled = !detailShown && !concertDetailOpen,
                        )
                    }
                }
            }
            if (error != null) {
                Card(Modifier.align(Alignment.TopCenter).padding(20.dp), insideMargin = PaddingValues(16.dp)) {
                    Text(error)
                    Button(onClick = onDismissError) { Text(AppText.get(R.string.ui_ok)) }
                }
            }
            if (onlineMusic.matching) {
                Card(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(20.dp), insideMargin = PaddingValues(16.dp)) {
                    Text(AppText.get(R.string.music_loading))
                }
            }
        }
        })
    }
    love.yu.jaychou.ui.designsystem.WindowBottomSheet(onlineMusic.error != null,
        AppText.get(R.string.online_title), onlineMusic::dismissError) {
        onlineMusic.error?.let { reason ->
            Text(onlineError(reason), Modifier.padding(20.dp))
            if (reason == OnlineMusicException.Reason.AUTH || reason == OnlineMusicException.Reason.NOT_CONFIGURED) {
                love.yu.jaychou.ui.designsystem.TextButton(AppText.get(R.string.online_open), {
                    onlineMusic.dismissError(); detailId = null
                    onlineMusic.select(MusicProvider.QQ)
                    pageMotion.select(RootPage.ONLINE.ordinal)
                })
            }
        }
    }
    CompositionLocalProvider(
        love.yu.jaychou.ui.animation.LocalSecondaryPredictiveBack provides settings.predictiveBack,
        love.yu.jaychou.ui.animation.LocalSecondaryBackAnimation provides settings.predictiveBackAnimation,
        love.yu.jaychou.ui.animation.LocalSecondaryExitDirection provides settings.predictiveBackExitDirection,
        LocalCouiWindowMetrics provides couiMetrics,
        LocalArchiveBlur provides useBlur,
        love.yu.jaychou.ui.effects.LocalTopAppBarBlur provides settings.topAppBarBlur,
        LocalArchiveLiquidGlass provides liquidGlass,
        LocalMovingAlbumId provides if (detailShown) lastDetailId else null,
    ) {
    // Sheets sample this activity layer even when bar blur is switched off.
    if (Build.VERSION.SDK_INT >= 33) {
        GlassLayer(background, scene)
    } else {
        scene(Modifier, Modifier.background(ArchiveUiTheme.colorScheme.surfaceContainer))
    }
}
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
    tag: String = "album-detail",
    swipeEdge: Float = 1f, pivotY: Float = .5f, exiting: Boolean = false,
    content: @Composable (Modifier, Modifier, () -> Unit) -> Unit,
) {
    var destination by remember(album.id) { mutableStateOf<Rect?>(null) }
    var pageCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    val density = LocalDensity.current
    val background = ArchiveUiTheme.colorScheme.background
    val progress = { expansion.value.coerceIn(0f, 1f) }
    val artworkProgress = { albumTransitionProgress(expansion.value, back.value) }
    val artworkInFlight by remember(expansion, back) { derivedStateOf {
        albumTransitionProgress(expansion.value, back.value) < .999f
    } }
    val backStyle = love.yu.jaychou.ui.animation.LocalSecondaryBackAnimation.current
    val exitDirection = love.yu.jaychou.ui.animation.LocalSecondaryExitDirection.current
    // Shield the pager while leaving the detail and its own scrolling interactive.
    Box(Modifier.fillMaxSize().pointerInput(Unit) {
        awaitPointerEventScope { while (true) awaitPointerEvent() }
    })
    Box(modifier.fillMaxSize().testTag(tag)) {
        // Only the page surface and body follow the directional exit transform.
        Box(Modifier.fillMaxSize().testTag("$tag-page").graphicsLayer {
            val exitProgress = if (exiting) 1f - expansion.value.coerceIn(0f, 1f) else 0f
            val scaleProgress = if (exiting && back.value > 0f) back.value + (1f - back.value) * exitProgress else back.value
            applySecondaryBackTransform(backStyle, scaleProgress, swipeEdge, pivotY, exitDirection)
            translationX += exitDirection.sign(swipeEdge) * exitProgress * size.width
        }.onGloballyPositioned { pageCoordinates = it }) {
            Box(Modifier.fillMaxSize().graphicsLayer {
                val q = progress()
                alpha = q
                translationY = (1f - q) * 48.dp.toPx()
            }.background(background)) { CustomBackgroundImage() }
            val coverMotion = Modifier.onGloballyPositioned {
                // Measure in the page's local space, excluding its back/exit transform.
                destination = pageCoordinates?.takeIf { page -> page.isAttached }
                    ?.localBoundingBoxOf(it, clipBounds = false)
                    ?: it.boundsInWindow().translate(-hostOrigin)
            }.graphicsLayer {
                // Hide the whole cover surface, including its card background, while the overlay owns it.
                // Keep it laid out so the independent artwork always has a real destination.
                alpha = if (artworkProgress() >= .999f || destination == null) 1f else 0f
            }
            val bodyMotion = Modifier.graphicsLayer {
                val q = progress()
                alpha = ((q - .12f) / .88f).coerceIn(0f, 1f)
                translationY = (1f - q) * 64.dp.toPx()
            }
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = progress() }) {
                content(coverMotion, bodyMotion) { destination = null }
            }
        }
        // Sibling overlay: the artwork never inherits the page translation or scale.
        val target = destination
        if (target != null && target.width > 0f && target.height > 0f) {
            // Keep the image/layers warm while the page is open. Returning only changes
            // layer properties; it does not recreate Coil content on the first exit frame.
            // Draw at destination resolution and transform only the artwork, never the whole page.
            Box(
                Modifier.requiredSize(with(density) { target.width.toDp() }, with(density) { target.height.toDp() })
                    .graphicsLayer {
                        val q = artworkProgress()
                        // Follow the actual source as the pager depth and press feedback settle.
                        val start = source?.currentCoverBounds()?.translate(-hostOrigin)
                            ?: Rect(origin[0] * hostSize.width, origin[1] * hostSize.height,
                                (origin[0] + origin[2]) * hostSize.width,
                                (origin[1] + origin[3]) * hostSize.height)
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = (start.width / target.width) * (1f - q) + q
                        scaleY = (start.height / target.height) * (1f - q) + q
                        translationX = if (q >= .999f) -hostSize.width * 2f
                            else start.left * (1f - q) + target.left * q
                        translationY = start.top * (1f - q) + target.top * q
                        alpha = if (q < .999f) 1f else 0f
                    }.clip(AlbumCoverShape)
                    .then(if (artworkInFlight) Modifier.testTag("$tag-cover-flight") else Modifier)
                    .clearAndSetSemantics { },
            ) {
                val sourceLayer = source?.layer?.takeIf { it.size.width > 0 && it.size.height > 0 }
                val coverContent: @Composable () -> Unit = {
                    Box(Modifier.fillMaxSize()) {
                        CatalogImage(
                            album.coverUrl,
                            AppText.get(R.string.ui_artwork_232, album.title),
                            Modifier.fillMaxSize().graphicsLayer {
                                alpha = if (sourceLayer == null) 1f else artworkProgress()
                            },
                            contentScale = ContentScale.Fit,
                            crossfade = false,
                            showStatus = true,
                        )
                        // At the list endpoint this is the original rendering, including its crop.
                        AlbumSourcePreview(sourceLayer) { 1f - artworkProgress() }
                    }
                }
                // The background follows exactly the same bounds, clip and transform as the image.
                Card(Modifier.fillMaxSize(), cornerRadius = 12.dp) { coverContent() }
            }
        }
    }
}
