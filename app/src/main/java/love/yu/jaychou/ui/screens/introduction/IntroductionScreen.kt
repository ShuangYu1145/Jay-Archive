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
    val actionHosts = remember { List(2) { SectionActionHost() } }
    val background = ArchiveUiTheme.colorScheme.background
    val material = ArchiveUiTheme.isMaterial
    val scene: @Composable (Modifier, Modifier) -> Unit = { capture, glass ->
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
            if (headerVisible) Row(
                (if (material) Modifier.fillMaxWidth().background(background).statusBarsPadding()
                    .height(74.dp).padding(horizontal = 8.dp)
                else Modifier.fillMaxWidth().statusBarsPadding()
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
                        listOf("ALBUMS" to AppText.get(R.string.ui_albums), "CONCERTS" to AppText.get(R.string.ui_concerts))
                            .forEachIndexed { index, (key, label) ->
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
                        items = listOf(
                            ArchiveNavigationItem("ALBUMS", AppText.get(R.string.ui_albums)),
                            ArchiveNavigationItem("CONCERTS", AppText.get(R.string.ui_concerts)),
                        ),
                        barTag = "introduction-tabs",
                    )
                }
                SectionHeaderActions {
                    actionHosts[pager.currentPage].content?.value?.invoke(this)
                }
            }
        }
    }
    if (Build.VERSION.SDK_INT >= 33 && (LocalArchiveBlur.current || LocalArchiveLiquidGlass.current)) GlassLayer(background, scene)
    else scene(Modifier, Modifier.background(ArchiveUiTheme.colorScheme.surfaceContainer))
}
