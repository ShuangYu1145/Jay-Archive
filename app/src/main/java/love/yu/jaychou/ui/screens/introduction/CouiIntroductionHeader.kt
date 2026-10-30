package love.yu.jaychou.ui.screens.introduction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yu.jaychou.ui.components.LocalArchiveBlur
import love.yu.jaychou.ui.effects.couiFrostedSurface
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import love.yu.jaychou.ui.designsystem.CouiLayout
import love.yu.jaychou.ui.designsystem.Text
import love.yu.jaychou.ui.navigation.ArchiveNavigationItem

/** Fixed section capsule, sharing the floating bottom navigation surface. */
@Composable
internal fun CouiIntroductionHeader(items: List<ArchiveNavigationItem>, selected: Int,
                                    onSelect: (Int) -> Unit, enabled: Boolean,
                                    modifier: Modifier = Modifier,
                                    actions: @Composable RowScope.() -> Unit) {
    val colors = ArchiveUiTheme.colorScheme
    Row(modifier.fillMaxWidth().testTag("introduction-header"),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.weight(1f).heightIn(min = CouiLayout.ActionSize)
            .couiFrostedSurface(LocalArchiveBlur.current).padding(4.dp)
            .selectableGroup().testTag("introduction-tabs"),
            verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { index, item ->
                Box(Modifier.weight(1f).heightIn(min = CouiLayout.ActionSize - 8.dp)
                    .clip(CircleShape)
                    .background(if (selected == index) colors.onBackground.copy(alpha = .08f) else Color.Transparent)
                    .selectable(selected == index, enabled = enabled, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(horizontal = 2.dp, vertical = 8.dp).testTag("tab-${item.key}"),
                    contentAlignment = Alignment.Center) {
                    Text(item.label, fontSize = 14.sp,
                        color = if (selected == index) colors.primary else colors.onBackground)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(104.dp).height(CouiLayout.ActionSize)
                .testTag("introduction-actions"), content = actions)
    }
}
