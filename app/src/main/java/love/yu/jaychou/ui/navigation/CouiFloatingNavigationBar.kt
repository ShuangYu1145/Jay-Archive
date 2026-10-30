package love.yu.jaychou.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import love.yu.jaychou.ui.effects.couiFrostedSurface
import love.yu.jaychou.ui.designsystem.CouiLayout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import love.yu.jaychou.ui.designsystem.Icon
import love.yu.jaychou.ui.designsystem.Text

/** A frosted central tab capsule flanked by the home and about destinations. */
@Composable
internal fun CouiFloatingNavigationBar(selectedIndex: Int, onSelect: (Int) -> Unit,
                                       enabled: Boolean, blur: Boolean) {
    val items = rootNavigationItems()
    Box(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp,
        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp),
        contentAlignment = Alignment.Center) {
        Row(Modifier.widthIn(max = 440.dp).fillMaxWidth().selectableGroup().testTag("floating-navigation"),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CouiNavigationDestination(items.first(), selectedIndex == 0, { onSelect(0) }, enabled,
                Modifier.size(CouiLayout.ActionSize).couiFrostedSurface(blur), showLabel = false)
            Row(Modifier.weight(1f).couiFrostedSurface(blur).padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                for (index in 1..2) CouiNavigationDestination(items[index], selectedIndex == index,
                    { onSelect(index) }, enabled, Modifier.weight(1f).heightIn(min = CouiLayout.ActionSize - 8.dp), showLabel = true)
            }
            CouiNavigationDestination(items.last(), selectedIndex == 3, { onSelect(3) }, enabled,
                Modifier.size(CouiLayout.ActionSize).couiFrostedSurface(blur), showLabel = false)
        }
    }
}

@Composable
private fun CouiNavigationDestination(item: ArchiveNavigationItem, selected: Boolean,
                                      onClick: () -> Unit, enabled: Boolean, modifier: Modifier,
                                      showLabel: Boolean) {
    val colors = ArchiveUiTheme.colorScheme
    val color by animateColorAsState(
        if (!enabled) colors.onBackground.copy(alpha = .38f)
        else if (selected) colors.onBackground else colors.onBackground.copy(alpha = .64f),
        label = "coui-navigation-color")
    Column(modifier.clip(CircleShape).selectable(selected, enabled = enabled, role = Role.Tab, onClick = onClick)
        .semantics { contentDescription = item.label }.testTag("tab-${item.key}")
        .padding(horizontal = 4.dp, vertical = if (showLabel) 3.dp else 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
        Icon(requireNotNull(item.icon), null, Modifier.size(24.dp), tint = color)
        if (showLabel) Text(item.label, color = color, fontSize = 11.sp, lineHeight = 14.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
    }
}

