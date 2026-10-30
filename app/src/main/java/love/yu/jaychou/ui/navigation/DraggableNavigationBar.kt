package love.yu.jaychou.ui.navigation

import love.yu.jaychou.ui.components.LocalArchiveBlur
import love.yu.jaychou.ui.effects.LocalArchiveLiquidGlass
import love.yu.jaychou.ui.effects.LocalNavigationBackdrop
import love.yu.jaychou.ui.effects.navigationSurfaceBlur
import love.yu.jaychou.data.AppText
import love.yu.jaychou.R
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yu.jaychou.ui.designsystem.Icon
import love.yu.jaychou.ui.designsystem.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Settings
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import kotlin.math.roundToInt

data class ArchiveNavigationItem(val key: String, val label: String, val icon: ImageVector? = null)

internal fun rootNavigationItems() = listOf(
    ArchiveNavigationItem("HOME", AppText.get(R.string.ui_home), MiuixIcons.Home),
    ArchiveNavigationItem("INTRODUCTION", AppText.get(R.string.ui_introduction), MiuixIcons.Album),
    ArchiveNavigationItem("SETTINGS", AppText.get(R.string.ui_settings), MiuixIcons.Settings),
    ArchiveNavigationItem("ABOUT", AppText.get(R.string.ui_about), MiuixIcons.Info),
)

/** Custom gesture/indicator shell over the real Miuix backdrop; not an iOS framework API. */
@Composable
fun DraggableNavigationBar(
    selectedIndex: Int, pagePosition: () -> Float,
    onSelect: (Int) -> Unit, onScrub: (Float) -> Unit,
    glass: Modifier, enabled: Boolean, modifier: Modifier = Modifier,
    items: List<ArchiveNavigationItem> = rootNavigationItems(),
    barTag: String = "floating-navigation",
) {
    val backdrop = LocalNavigationBackdrop.current
    if (android.os.Build.VERSION.SDK_INT >= 33 && backdrop != null && LocalArchiveLiquidGlass.current) {
        love.yu.jaychou.ui.effects.liquid.LiquidGlassNavigationBar(
            selectedIndex, onSelect, backdrop, enabled, modifier, items, barTag,
        )
        return
    }
    val colors = ArchiveUiTheme.colorScheme
    val count = items.size
    val lastIndex = items.lastIndex.toFloat()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val paddingPx = with(density) { 4.dp.toPx() }
    val dark = colors.background.luminance() < .5f
    var widthPx by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }
    var originalIndex by remember { mutableIntStateOf(0) }
    var lastDetent by remember { mutableIntStateOf(0) }
    val currentIndex by rememberUpdatedState(selectedIndex)
    val currentSelect by rememberUpdatedState(onSelect)
    val currentScrub by rememberUpdatedState(onScrub)
    val stretch by animateFloatAsState(if (dragging) 1.08f else 1f,
        spring(dampingRatio = .65f, stiffness = 500f), label = "glass-drag-stretch")
    val capsule = CircleShape
    val surface = if (android.os.Build.VERSION.SDK_INT >= 33 && backdrop != null && LocalArchiveBlur.current) {
        Modifier.navigationSurfaceBlur(backdrop, capsule, colors.surfaceContainer)
    } else glass
    Box(modifier.onSizeChanged { widthPx = it.width.toFloat() }
        .shadow(12.dp, capsule, ambientColor = Color.Black.copy(alpha = .10f), spotColor = Color.Black.copy(alpha = .12f))
        .then(surface).clip(capsule)
        .border(1.5.dp, Brush.verticalGradient(listOf(
            Color.White.copy(alpha = if (dark) .30f else .90f),
            Color.White.copy(alpha = if (dark) .06f else .24f),
            Color.White.copy(alpha = if (dark) .18f else .72f),
        )), capsule)
        .pointerInput(enabled, rtl, density, count) {
            if (!enabled) return@pointerInput
            detectHorizontalDragGestures(
                onDragStart = { position ->
                    originalIndex = currentIndex
                    lastDetent = originalIndex
                    val step = ((widthPx - paddingPx * 2) / count).coerceAtLeast(1f)
                    val x = if (rtl) widthPx - position.x else position.x
                    dragPosition = ((x - paddingPx) / step - .5f).coerceIn(0f, lastIndex)
                    dragging = true
                    currentScrub(dragPosition)
                },
                onHorizontalDrag = { change, _ ->
                    change.consume()
                    val step = ((widthPx - paddingPx * 2) / count).coerceAtLeast(1f)
                    // onDragStart already contains the first movement past touch slop.
                    // Read the finger position so that movement is never counted twice.
                    val x = if (rtl) widthPx - change.position.x else change.position.x
                    dragPosition = ((x - paddingPx) / step - .5f).coerceIn(0f, lastIndex)
                    val detent = dragPosition.roundToInt()
                    if (detent != lastDetent) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        lastDetent = detent
                    }
                    currentScrub(dragPosition)
                },
                onDragEnd = { currentSelect(dragPosition.roundToInt()); dragging = false },
                onDragCancel = { currentSelect(originalIndex); dragging = false },
            )
        }.padding(4.dp).selectableGroup().testTag(barTag)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val step = maxWidth / count
            val position = if (dragging) dragPosition else pagePosition().coerceIn(0f, lastIndex)
            Box(Modifier.matchParentSize()) {
                Box(Modifier.offset { IntOffset((step.toPx() * position).roundToInt(), 0) }
                    .width(step).fillMaxHeight().graphicsLayer { scaleX = stretch; scaleY = 1f + (stretch - 1f) * .5f }
                    .background((if (dark) Color.White else Color.Black)
                        .copy(alpha = if (dragging) .14f else .09f), capsule)
                    .border(.7.dp, Color.White.copy(alpha = if (dragging) .45f else 0f), capsule)
                    .testTag(if (barTag == "floating-navigation") "navigation-indicator" else "$barTag-indicator"))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                items.forEachIndexed { index, item ->
                    val active = selectedIndex == index
                    val color by animateColorAsState(if (active) colors.primary else colors.onBackground, label = "navigation-color")
                    Column(Modifier.weight(1f).clip(capsule)
                        .selectable(active, enabled = enabled, role = Role.Tab, onClick = { currentSelect(index) })
                        .testTag("tab-${item.key}")
                        .heightIn(min = 50.dp).padding(horizontal = 2.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                        item.icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(25.dp), tint = color) }
                        Text(item.label, color = color, fontSize = if (item.icon == null) 16.sp else 12.sp, maxLines = 1,
                            lineHeight = if (item.icon == null) 22.sp else 14.sp,
                            fontWeight = FontWeight.Normal)
                    }
                }
            }
        }
    }
}
