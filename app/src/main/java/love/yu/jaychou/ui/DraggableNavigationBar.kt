package love.yu.jaychou.ui

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
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

/** Custom gesture/indicator shell over the real Miuix backdrop; not an iOS framework API. */
@Composable
fun DraggableNavigationBar(
    selectedIndex: Int, pagePosition: () -> Float,
    onSelect: (Int) -> Unit, onScrub: (Float) -> Unit,
    glass: Modifier, enabled: Boolean, modifier: Modifier = Modifier,
) {
    val backdrop = LocalNavigationBackdrop.current
    if (android.os.Build.VERSION.SDK_INT >= 33 && backdrop != null) {
        love.yu.jaychou.ui.liquid.LiquidGlassNavigationBar(
            selectedIndex, onSelect, backdrop, enabled, modifier,
        )
        return
    }
    val colors = MiuixTheme.colorScheme
    val labels = listOf(AppText.get(R.string.ui_home), AppText.get(R.string.ui_albums), AppText.get(R.string.ui_concerts), AppText.get(R.string.ui_settings), AppText.get(R.string.ui_about))
    val keys = listOf("HOME", "ALBUMS", "CONCERTS", "SETTINGS", "ABOUT")
    val glyphs = listOf(Glyph.HOME, Glyph.ALBUMS, Glyph.CONCERTS, Glyph.SETTINGS, Glyph.INFO)
    val count = labels.size
    val lastIndex = labels.lastIndex.toFloat()
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
    Box(modifier.onSizeChanged { widthPx = it.width.toFloat() }
        .shadow(12.dp, capsule, ambientColor = Color.Black.copy(alpha = .10f), spotColor = Color.Black.copy(alpha = .12f))
        .then(glass).clip(capsule)
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
                onHorizontalDrag = { change, delta ->
                    change.consume()
                    val step = ((widthPx - paddingPx * 2) / count).coerceAtLeast(1f)
                    dragPosition = (dragPosition + (if (rtl) -delta else delta) / step).coerceIn(0f, lastIndex)
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
        }.padding(4.dp).selectableGroup().testTag("floating-navigation")) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val step = maxWidth / count
            val position = if (dragging) dragPosition else pagePosition().coerceIn(0f, lastIndex)
            Box(Modifier.matchParentSize()) {
                Box(Modifier.offset { IntOffset((step.toPx() * position).roundToInt(), 0) }
                    .width(step).fillMaxHeight().graphicsLayer { scaleX = stretch; scaleY = 1f + (stretch - 1f) * .5f }
                    .background((if (dark) Color.White else Color.Black)
                        .copy(alpha = if (dragging) .14f else .09f), capsule)
                    .border(.7.dp, Color.White.copy(alpha = if (dragging) .45f else 0f), capsule)
                    .testTag("navigation-indicator"))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                labels.forEachIndexed { index, label ->
                    val active = selectedIndex == index
                    val color by animateColorAsState(if (active) colors.primary else colors.onBackground, label = "navigation-color")
                    Column(Modifier.weight(1f).clip(capsule)
                        .selectable(active, enabled = enabled, role = Role.Tab, onClick = { currentSelect(index) })
                        .testTag("tab-${keys[index]}")
                        .heightIn(min = 60.dp).padding(horizontal = 2.dp, vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        ArchiveGlyph(glyphs[index], Modifier.size(25.dp), color)
                        Text(label, color = color, fontSize = 12.sp, maxLines = 1,
                            fontWeight = FontWeight.Normal)
                    }
                }
            }
        }
    }
}
