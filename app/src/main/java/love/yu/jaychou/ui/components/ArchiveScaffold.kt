package love.yu.jaychou.ui.components

import love.yu.jaychou.ui.effects.GlassLayer
import love.yu.jaychou.ui.effects.HeaderGlassLayer
import love.yu.jaychou.ui.effects.LocalArchiveLiquidGlass
import love.yu.jaychou.ui.effects.LocalNavigationBackdrop
import love.yu.jaychou.ui.effects.liquid.liquidGlassBackground
import love.yu.jaychou.ui.effects.navigationSurfaceBlur
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import top.yukonga.miuix.kmp.basic.*
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import love.yu.jaychou.ui.designsystem.ArchiveScrollBehavior
import love.yu.jaychou.ui.designsystem.rememberCouiWindowMetrics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.lerp

val LocalArchiveBlur = staticCompositionLocalOf { false }

/** Each page publishes its actions; the shared header renders only the active page. */
internal class SectionActionHost {
    var content by mutableStateOf<State<@Composable RowScope.() -> Unit>?>(null)
}

internal val LocalInlineSectionHeader = staticCompositionLocalOf<(@Composable () -> Unit)?> { null }

internal val LocalSectionActionHost = staticCompositionLocalOf<SectionActionHost?> { null }

/** Separate sibling capture and header: never sample the blurred bar into itself. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ArchiveScaffold(
    title: String,
    behavior: ArchiveScrollBehavior? = null,
    small: Boolean = false,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    sectionHeader: Boolean = false,
    progressiveBlur: Boolean = false,
    contentMaxWidth: androidx.compose.ui.unit.Dp? = null,
    topBarVisible: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    val inlineHeader = LocalInlineSectionHeader.current.takeIf { sectionHeader }
    val actionHost = LocalSectionActionHost.current.takeIf { sectionHeader }
    val currentActions = rememberUpdatedState(actions)
    DisposableEffect(actionHost) {
        actionHost?.content = currentActions
        onDispose {
            if (actionHost?.content === currentActions) actionHost.content = null
        }
    }
    val background = ArchiveUiTheme.colorScheme.background
    val pageBackground = ArchiveUiTheme.pageBackground
    if (ArchiveUiTheme.isMaterial) {
        val blurred = Build.VERSION.SDK_INT >= 33 && LocalArchiveBlur.current && !sectionHeader && topBarVisible
        val materialScene: @Composable (Modifier, Modifier) -> Unit = { capture, header ->
            val barColors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                containerColor = if (blurred) Color.Transparent else background,
                scrolledContainerColor = if (blurred) Color.Transparent else ArchiveUiTheme.colorScheme.surfaceContainer,
            )
            androidx.compose.material3.Scaffold(
                modifier = Modifier.fillMaxSize().clipToBounds(),
                containerColor = pageBackground, contentWindowInsets = WindowInsets(0),
                topBar = { if (topBarVisible) {
                    val bar = header.testTag("top-bar-$title")
                    when {
                        sectionHeader -> Spacer(bar.fillMaxWidth().statusBarsPadding().height(74.dp))
                        small -> androidx.compose.material3.TopAppBar(
                            title = { androidx.compose.material3.Text(title) }, modifier = bar, colors = barColors,
                            navigationIcon = {
                                Box(Modifier.padding(start = 12.dp).size(48.dp)
                                    .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer, CircleShape),
                                    contentAlignment = Alignment.Center) { navigationIcon() }
                            }, actions = actions)
                        else -> androidx.compose.material3.LargeTopAppBar(
                            title = { androidx.compose.material3.Text(title) }, modifier = bar,
                            navigationIcon = navigationIcon, actions = actions, scrollBehavior = behavior?.material,
                            colors = barColors)
                    }
                } },
            ) { padding ->
                Box(Modifier.fillMaxSize().clipToBounds().then(capture).background(pageBackground)) { content(padding) }
            }
        }
        if (blurred) HeaderGlassLayer(background, materialScene,
            progressive = progressiveBlur || love.yu.jaychou.ui.effects.LocalTopAppBarBlur.current == love.yu.jaychou.data.TopAppBarBlur.PROGRESSIVE)
        else materialScene(Modifier, Modifier)
        return
    }
    if (ArchiveUiTheme.isCoui) {
        val metrics = rememberCouiWindowMetrics()
        // Observe descendant lists without consuming scroll or changing compact-toolbar geometry.
        var contentScroll by remember(title) { mutableFloatStateOf(0f) }
        val scrollObserver = remember(title) { object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                contentScroll = (contentScroll - consumed.y).coerceAtLeast(0f)
                if (available.y > .5f) contentScroll = 0f
                return Offset.Zero
            }
        } }
        val scrolled by remember(behavior, title) { derivedStateOf {
            contentScroll > .5f || (behavior?.coui?.state?.heightOffset ?: 0f) < -.5f
        } }
        val headerProgress by animateFloatAsState(if (scrolled) 1f else 0f,
            tween(180), label = "coui-header-surface")
        // Landscape and wide windows use the single-line COUI toolbar so lists keep enough height;
        // the large collapsing title stays a portrait affordance.
        val compactToolbar = small || metrics.isLandscape || metrics.isWide
        val couiScene: @Composable (Modifier, Modifier) -> Unit = { capture, header ->
            io.github.suqi8.coui.kmp.basic.Scaffold(
                modifier = Modifier.fillMaxSize().clipToBounds().nestedScroll(scrollObserver),
                containerColor = pageBackground, contentWindowInsets = WindowInsets(0),
                topBar = { if (topBarVisible) {
                    val bar = header.testTag("top-bar-$title")
                    when {
                        inlineHeader != null -> Spacer(Modifier.fillMaxWidth().statusBarsPadding()
                            .height(metrics.sectionHeaderHeight))
                        sectionHeader -> Row(bar.fillMaxWidth().statusBarsPadding()
                            .height(metrics.sectionHeaderHeight),
                            horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            if (actionHost == null) SectionHeaderActions(content = actions)
                        }
                        // COUI SmallTopAppBar clips its children to the 52dp toolbar bounds,
                        // cutting off the frosted action's diffuse shadow. Keep actions outside that clip.
                        compactToolbar -> Box(bar.fillMaxWidth().statusBarsPadding(),
                            contentAlignment = Alignment.TopCenter) {
                            Row((if (contentMaxWidth != null)
                                Modifier.widthIn(max = contentMaxWidth).fillMaxWidth() else Modifier.fillMaxWidth())
                                .height(metrics.toolbarHeight).padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                navigationIcon()
                                Spacer(Modifier.width(8.dp))
                                io.github.suqi8.coui.kmp.basic.Text(title,
                                    modifier = Modifier.weight(1f).testTag("coui-compact-title"),
                                    fontSize = 18.sp, fontWeight = FontWeight.Medium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.width(8.dp))
                                actions()
                            }
                        }
                        else -> io.github.suqi8.coui.kmp.basic.TopAppBar(title, modifier = bar, color = Color.Transparent,
                            scrollBehavior = behavior?.coui, navigationIcon = navigationIcon, actions = actions)
                    }
                } },
            ) { padding -> Box(Modifier.fillMaxSize().then(capture).background(pageBackground)) { content(padding) } }
        }
        if (topBarVisible && Build.VERSION.SDK_INT >= 33 && LocalArchiveBlur.current && !sectionHeader)
            HeaderGlassLayer(ArchiveUiTheme.colorScheme.surfaceContainer, couiScene,
                progressive = progressiveBlur || small, surfaceProgress = { headerProgress })
        else couiScene(Modifier, if (sectionHeader) Modifier else
            Modifier.background(lerp(background, ArchiveUiTheme.colorScheme.surfaceContainer, headerProgress)))
        return
    }
    val scene: @Composable (Modifier, Modifier) -> Unit = { capture, header ->
        Scaffold(
            modifier = Modifier.fillMaxSize().clipToBounds(),
            containerColor = pageBackground, contentWindowInsets = WindowInsets(0),
            topBar = { if (topBarVisible) {
                val bar = header.testTag("top-bar-$title")
                if (sectionHeader) Row(
                    bar.fillMaxWidth().statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 58.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // The shared section capsule is drawn above the introduction pager.
                    Spacer(Modifier.weight(1f))
                    if (actionHost == null) SectionHeaderActions(content = actions)
                }
                else if (small) SmallTopAppBar(title, modifier = bar, color = Color.Transparent,
                    navigationIcon = navigationIcon, actions = actions, defaultWindowInsetsPadding = true)
                else TopAppBar(title, modifier = bar, color = Color.Transparent,
                    scrollBehavior = behavior?.miuix, titlePadding = 24.dp,
                    navigationIcon = navigationIcon, actions = actions, defaultWindowInsetsPadding = true)
            } },
        ) { padding ->
            Box(Modifier.fillMaxSize().clipToBounds().then(capture).background(pageBackground)) { content(padding) }
        }
    }
    // Section actions sample only the sibling list, never the toolbar containing themselves.
    if (topBarVisible && sectionHeader && actionHost == null && Build.VERSION.SDK_INT >= 33 && (LocalArchiveBlur.current || LocalArchiveLiquidGlass.current)) GlassLayer(background, scene)
    else if (topBarVisible && Build.VERSION.SDK_INT >= 33 && LocalArchiveBlur.current && !sectionHeader) HeaderGlassLayer(background, scene, progressive = love.yu.jaychou.ui.effects.LocalTopAppBarBlur.current == love.yu.jaychou.data.TopAppBarBlur.PROGRESSIVE)
    else scene(Modifier, if (sectionHeader) Modifier else Modifier.background(background))
}

@Composable
internal fun SectionHeaderActions(singleButton: Boolean = false, content: @Composable RowScope.() -> Unit) {
    if (ArchiveUiTheme.isCoui) {
        val metrics = rememberCouiWindowMetrics()
        Row(Modifier.width(metrics.headerActionWidth).height(metrics.sectionHeaderHeight)
                .testTag("introduction-actions"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically, content = content)
        return
    }
    val backdrop = LocalNavigationBackdrop.current
    if (ArchiveUiTheme.isMaterial) {
        Row(Modifier.width(if (singleButton) 48.dp else 96.dp).height(if (singleButton) 48.dp else 64.dp)
            .then(if (singleButton) Modifier else Modifier.testTag("introduction-actions")),
            verticalAlignment = Alignment.CenterVertically, content = content)
        return
    }
    val tint = ArchiveUiTheme.colorScheme.surfaceContainer
    val dark = ArchiveUiTheme.colorScheme.background.luminance() < .5f
    val surface = when {
        Build.VERSION.SDK_INT >= 33 && backdrop != null && LocalArchiveLiquidGlass.current ->
            Modifier.liquidGlassBackground(backdrop)
        Build.VERSION.SDK_INT >= 33 && backdrop != null && LocalArchiveBlur.current ->
            Modifier.navigationSurfaceBlur(backdrop, CircleShape, tint)
        else -> Modifier.background(tint, CircleShape)
    }
    Row(
        Modifier.width(if (singleButton) 58.dp else 96.dp).height(58.dp)
            .dropShadow(CircleShape, Shadow(radius = 10.dp, color = Color.Black, alpha = if (dark) .2f else .1f))
            .then(surface).clip(CircleShape)
            .then(if (singleButton) Modifier else Modifier.testTag("introduction-actions")),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content,
    )
}
