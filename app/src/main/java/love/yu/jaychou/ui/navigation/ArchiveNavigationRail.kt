package love.yu.jaychou.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import love.yu.jaychou.data.UiStyle
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import love.yu.jaychou.ui.effects.materialFrostedSurface
import love.yu.jaychou.ui.effects.materialSurfaceBlurEnabled
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import love.yu.jaychou.ui.designsystem.CouiWindowMetrics
import io.github.suqi8.coui.kmp.basic.NavigationRailItem as CouiNavigationRailItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem

/** Both native rails use the same destinations and pager selection as the bottom bar. */
@Composable
internal fun ArchiveNavigationRail(
    uiStyle: UiStyle,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    enabled: Boolean,
    metrics: CouiWindowMetrics,
    modifier: Modifier = Modifier,
) {
    Box(modifier.testTag("side-navigation")) {
        if (uiStyle == UiStyle.MATERIAL3) {
            val tint = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
            androidx.compose.material3.NavigationRail(
                containerColor = if (materialSurfaceBlurEnabled()) Color.Transparent else tint,
                modifier = Modifier.materialFrostedSurface(RectangleShape, tint)
                    .fillMaxHeight().verticalScroll(rememberScrollState())
                    .testTag("material-navigation-rail"),
            ) {
                rootNavigationItems().forEachIndexed { index, item ->
                    androidx.compose.material3.NavigationRailItem(
                        selected = selectedIndex == index,
                        onClick = { onSelect(index) },
                        enabled = enabled,
                        icon = { androidx.compose.material3.Icon(requireNotNull(item.icon), contentDescription = null) },
                        label = { androidx.compose.material3.Text(item.label) },
                        modifier = Modifier.testTag("tab-${item.key}"),
                    )
                }
            }
        } else if (uiStyle == UiStyle.COUI) {
            // The page already consumes horizontal safe-drawing insets. Only vertical insets belong
            // to the rail, otherwise cutouts and side navigation bars were padded twice.
            val surface = ArchiveUiTheme.pageBackground
            Box(Modifier.fillMaxHeight().background(surface)) {
                io.github.suqi8.coui.kmp.basic.NavigationRail(
                    modifier = Modifier.fillMaxHeight()
                        .statusBarsPadding()
                        .windowInsetsPadding(
                            WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                        )
                        .testTag("coui-navigation-rail"),
                    color = surface,
                    defaultWindowInsetsPadding = false,
                    minWidth = metrics.railWidth,
                ) {
                    rootNavigationItems().forEachIndexed { index, item ->
                        CouiNavigationRailItem(selected = selectedIndex == index, onClick = { onSelect(index) },
                            enabled = enabled, icon = requireNotNull(item.icon), label = item.label,
                            modifier = Modifier.testTag("tab-${item.key}"))
                    }
                }
            }
        } else {
            NavigationRail(modifier = Modifier.testTag("miuix-navigation-rail")) {
                rootNavigationItems().forEachIndexed { index, item ->
                    NavigationRailItem(
                        selected = selectedIndex == index,
                        onClick = { onSelect(index) },
                        enabled = enabled,
                        icon = requireNotNull(item.icon),
                        label = item.label,
                        modifier = Modifier.testTag("tab-${item.key}"),
                    )
                }
            }
        }
    }
}
