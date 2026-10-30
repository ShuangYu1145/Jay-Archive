package love.yu.jaychou.ui.navigation

import love.yu.jaychou.ui.effects.LocalNavigationBackdrop
import love.yu.jaychou.ui.effects.liquid.lens
import love.yu.jaychou.ui.effects.navigationSurfaceBlur
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import love.yu.jaychou.data.AppSettings
import love.yu.jaychou.data.UiStyle
import io.github.suqi8.coui.kmp.basic.NavigationBarItem as CouiNavigationBarItem
import androidx.compose.material3.NavigationBarItem as MaterialNavigationBarItem
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Native Miuix bars share page selection with the optional liquid lens. */
@Composable
internal fun ArchiveBottomNavigationBar(
    settings: AppSettings,
    selectedIndex: Int,
    pagePosition: () -> Float,
    onSelect: (Int) -> Unit,
    onScrub: (Float) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    if (settings.uiStyle == UiStyle.MATERIAL3) {
        val backdrop = LocalNavigationBackdrop.current
        val liquid = settings.floatingBottomBar && settings.liquidGlass &&
            Build.VERSION.SDK_INT >= 33 && backdrop != null
        val tint = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
        val blurred = settings.floatingBottomBar && !liquid && settings.blur &&
            Build.VERSION.SDK_INT >= 33 && backdrop != null
        val entries: @Composable RowScope.() -> Unit = {
            rootNavigationItems().forEachIndexed { index, item ->
                MaterialNavigationBarItem(
                    selected = selectedIndex == index, onClick = { onSelect(index) }, enabled = enabled,
                    icon = { androidx.compose.material3.Icon(requireNotNull(item.icon), contentDescription = null) },
                    label = { androidx.compose.material3.Text(item.label) },
                    modifier = Modifier.testTag("tab-${item.key}"),
                )
            }
        }
        Box(modifier.testTag("bottom-navigation")) {
            Box(Modifier.testTag("material-navigation")) {
                when {
                    liquid -> DraggableNavigationBar(
                        selectedIndex, pagePosition, onSelect, onScrub, Modifier, enabled,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp,
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp)
                            .widthIn(max = 480.dp).fillMaxWidth(),
                    )
                    settings.floatingBottomBar -> {
                        val shape = RoundedCornerShape(28.dp)
                        val surface = if (blurred) Modifier.navigationSurfaceBlur(backdrop, shape, tint) else Modifier
                        Box(Modifier.padding(start = 20.dp, end = 20.dp,
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp)
                            .widthIn(max = 480.dp).fillMaxWidth()) {
                            androidx.compose.material3.NavigationBar(
                                modifier = surface.clip(shape).testTag("floating-navigation"),
                                containerColor = if (blurred) Color.Transparent else tint,
                                tonalElevation = 0.dp, windowInsets = WindowInsets(0), content = entries,
                            )
                        }
                    }
                    else -> androidx.compose.material3.NavigationBar(
                        Modifier.testTag("standard-navigation"), content = entries)
                }
            }
        }
        return
    }
    if (settings.uiStyle == UiStyle.COUI) {
        val backdrop = LocalNavigationBackdrop.current
        val colors = io.github.suqi8.coui.kmp.theme.COUITheme.colorScheme
        val liquid = settings.floatingBottomBar && settings.liquidGlass &&
            Build.VERSION.SDK_INT >= 33 && backdrop != null
        val blurred = !liquid && settings.blur && Build.VERSION.SDK_INT >= 33 && backdrop != null
        Box(modifier.testTag("bottom-navigation")) {
            Box(Modifier.testTag("coui-navigation")) {
                when {
                    liquid -> DraggableNavigationBar(
                        selectedIndex, pagePosition, onSelect, onScrub, Modifier, enabled,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp,
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp)
                            .widthIn(max = 480.dp).fillMaxWidth(),
                    )
                    settings.floatingBottomBar -> {
                        CouiFloatingNavigationBar(selectedIndex, onSelect, enabled, blur = settings.blur)
                    }
                    else -> {
                        val tint = colors.background
                        val surface = if (blurred) Modifier.navigationSurfaceBlur(backdrop, RectangleShape, tint) else Modifier
                        io.github.suqi8.coui.kmp.basic.NavigationBar(surface.testTag("standard-navigation"),
                            color = if (blurred) Color.Transparent else tint) {
                            rootNavigationItems().forEachIndexed { index, item ->
                                CouiNavigationBarItem(selected = selectedIndex == index, onClick = { onSelect(index) },
                                    icon = requireNotNull(item.icon), label = item.label, enabled = enabled,
                                    modifier = Modifier.testTag("tab-${item.key}"))
                            }
                        }
                    }
                }
            }
        }
        return
    }
    val backdrop = LocalNavigationBackdrop.current
    val liquid = settings.floatingBottomBar && settings.liquidGlass &&
        Build.VERSION.SDK_INT >= 33 && backdrop != null
    val blurred = !liquid && settings.blur && Build.VERSION.SDK_INT >= 33 && backdrop != null
    val items = rootNavigationItems()

    // Measure the entire occupied height, including each component's own system insets.
    Box(modifier.testTag("bottom-navigation")) {
        when {
            liquid -> DraggableNavigationBar(
                selectedIndex = selectedIndex,
                pagePosition = pagePosition,
                onSelect = onSelect,
                onScrub = onScrub,
                glass = Modifier,
                enabled = enabled,
                items = items,
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp,
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp)
                    .widthIn(max = 480.dp).fillMaxWidth(),
            )
            settings.floatingBottomBar -> {
                val tint = MiuixTheme.colorScheme.surfaceContainer
                val surface = if (blurred) Modifier.navigationSurfaceBlur(
                    backdrop, RoundedCornerShape(FloatingToolbarDefaults.CornerRadius), tint,
                ) else Modifier
                FloatingNavigationBar(
                    modifier = surface.testTag("floating-navigation"),
                    color = if (blurred) Color.Transparent else tint,
                ) {
                    items.forEachIndexed { index, item ->
                        FloatingNavigationBarItem(
                            selected = selectedIndex == index,
                            onClick = { onSelect(index) },
                            icon = requireNotNull(item.icon), label = item.label, enabled = enabled,
                            modifier = Modifier.testTag("tab-${item.key}"),
                        )
                    }
                }
            }
            else -> {
                val tint = MiuixTheme.colorScheme.surface
                val surface = if (blurred) Modifier.navigationSurfaceBlur(backdrop, RectangleShape, tint)
                    else Modifier
                NavigationBar(
                    modifier = surface.testTag("standard-navigation"),
                    color = if (blurred) Color.Transparent else tint,
                ) {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedIndex == index,
                            onClick = { onSelect(index) },
                            icon = requireNotNull(item.icon), label = item.label, enabled = enabled,
                            modifier = Modifier.testTag("tab-${item.key}"),
                        )
                    }
                }
            }
        }
    }
}
