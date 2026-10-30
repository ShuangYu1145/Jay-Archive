package love.yu.jaychou.ui.screens.introduction

import love.yu.jaychou.ui.components.LocalArchiveBlur
import love.yu.jaychou.ui.components.LocalSectionActionHost
import love.yu.jaychou.ui.components.SectionActionHost
import love.yu.jaychou.ui.components.SectionHeaderActions
import love.yu.jaychou.ui.effects.GlassLayer
import love.yu.jaychou.ui.effects.LocalArchiveLiquidGlass
import love.yu.jaychou.ui.effects.liquid.lens
import love.yu.jaychou.ui.navigation.ArchiveNavigationItem
import love.yu.jaychou.ui.navigation.DraggableNavigationBar
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import love.yu.jaychou.R
import love.yu.jaychou.data.AppText
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme

/** A sibling capture keeps the top lens out of the backdrop it samples. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntroductionScreen(
    pager: PagerState,
    enabled: Boolean,
    headerVisible: Boolean,
    onSelect: (Int) -> Unit,
    content: @Composable (Int) -> Unit,
) {
    val sections = listOf(
        ArchiveNavigationItem("ALBUMS", AppText.get(R.string.ui_albums)),
        ArchiveNavigationItem("CONCERTS", AppText.get(R.string.ui_concerts)),
        ArchiveNavigationItem("COMPOSITIONS", AppText.get(R.string.ui_compositions)),
    )
    val actionHosts = remember { List(3) { SectionActionHost() } }
    val background = ArchiveUiTheme.colorScheme.background
    val coui = ArchiveUiTheme.isCoui
    val material = ArchiveUiTheme.isMaterial
    val progressive = LocalArchiveBlur.current && (coui || (!material &&
        love.yu.jaychou.ui.effects.LocalTopAppBarBlur.current == love.yu.jaychou.data.TopAppBarBlur.PROGRESSIVE))
    val scene: @Composable (Modifier, Modifier, Modifier) -> Unit = { capture, glass, header ->
        Box(Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pager, beyondViewportPageCount = 1,
                userScrollEnabled = enabled,
                modifier = Modifier.fillMaxSize().then(capture).testTag("introduction-pager"),
            ) { section ->
                CompositionLocalProvider(LocalSectionActionHost provides actionHosts[section]) {
                    content(section)
                }
            }
            if (headerVisible && coui) CouiIntroductionHeader(
                sections, pager.currentPage, onSelect, enabled,
                modifier = header.fillMaxWidth().statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .pointerInput(enabled) {
                        if (enabled) detectHorizontalDragGestures { change, _ -> change.consume() }
                    },
            ) { actionHosts[pager.currentPage].content?.value?.invoke(this) }
            else if (headerVisible) Row(
                (if (material) Modifier.fillMaxWidth().background(background).statusBarsPadding()
                    .height(74.dp).padding(horizontal = 8.dp)
                else header.fillMaxWidth().statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp).heightIn(min = 58.dp))
                    .testTag("introduction-header")
                    .pointerInput(enabled) {
                        if (enabled) detectHorizontalDragGestures { change, _ -> change.consume() }
                    }, verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (material) androidx.compose.material3.PrimaryTabRow(
                        selectedTabIndex = pager.currentPage,
                        modifier = Modifier.testTag("introduction-tabs"),
                    ) {
                        sections.forEachIndexed { index, (key, label) ->
                            androidx.compose.material3.Tab(selected = pager.currentPage == index,
                                onClick = { onSelect(index) }, enabled = enabled,
                                modifier = Modifier.testTag("tab-$key"),
                                text = { androidx.compose.material3.Text(label) })
                        }
                    } else DraggableNavigationBar(
                        selectedIndex = pager.targetPage,
                        pagePosition = { pager.currentPage + pager.currentPageOffsetFraction },
                        // Only the lens follows the finger; content switches on release.
                        onSelect = onSelect, onScrub = {},
                        glass = glass, enabled = enabled,
                        items = sections,
                        barTag = "introduction-tabs",
                    )
                }
                SectionHeaderActions {
                    actionHosts[pager.currentPage].content?.value?.invoke(this)
                }
            }
        }
    }
    if (coui && Build.VERSION.SDK_INT >= 33 && LocalArchiveBlur.current) {
        love.yu.jaychou.ui.effects.HeaderGlassLayer(background, progressive = true, content = { capture, header ->
            scene(capture, Modifier, header)
        })
    } else if (Build.VERSION.SDK_INT >= 33 && (LocalArchiveBlur.current || LocalArchiveLiquidGlass.current)) {
        GlassLayer(background) { capture, glass ->
            if (progressive && headerVisible) love.yu.jaychou.ui.effects.HeaderGlassLayer(
                background, progressive = true, content = { headerCapture, header ->
                    scene(capture.then(headerCapture), glass, header)
                },
            ) else scene(capture, glass, Modifier)
        }
    } else scene(Modifier, Modifier.background(ArchiveUiTheme.colorScheme.surfaceContainer), Modifier)
}
