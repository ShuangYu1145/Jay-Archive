package love.yu.jaychou.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import love.yu.jaychou.data.UiStyle
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem

/** Both native rails use the same destinations and pager selection as the bottom bar. */
@Composable
internal fun ArchiveNavigationRail(
    uiStyle: UiStyle,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier.testTag("side-navigation")) {
        if (uiStyle == UiStyle.MATERIAL3) {
            androidx.compose.material3.NavigationRail(
                modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState())
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
