package love.yu.jaychou.ui.designsystem

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import love.yu.jaychou.ui.effects.LocalNavigationBackdrop
import love.yu.jaychou.ui.effects.nativeBackdropBlur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import kotlin.math.roundToInt

/** One measured surface owns its blur, tint, contents and movement. No outer blur rectangle. */
@Composable
internal fun MiuixModalSheet(
    show: Boolean, title: String, onDismiss: () -> Unit,
    startAction: @Composable () -> Unit, endAction: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val visibility = remember { MutableTransitionState(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val dismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(show) {
        if (show) dragOffset = 0f
        visibility.targetState = show
    }
    if (!show && !visibility.currentState && !visibility.targetState) return
    val backdrop = LocalNavigationBackdrop.current
    val blurAvailable = Build.VERSION.SDK_INT >= 33 && backdrop != null
    val tint = top.yukonga.miuix.kmp.layout.BottomSheetDefaults.backgroundColor()
    val dim by animateFloatAsState(if (show) .36f else 0f, tween(220), label = "sheet-dim")

    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().testTag("sheet-window")) {
            val sheetHeight = maxHeight * .75f
            val density = LocalDensity.current
            val heightPx = with(density) { sheetHeight.toPx() }
            val flingThreshold = with(density) { 900.dp.toPx() }
            val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = dim))
                .clickable(onClick = onDismiss))
            AnimatedVisibility(
                visibleState = visibility,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(tween(280)) { it } + fadeIn(tween(180)),
                exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(180)),
            ) {
                val blur = if (blurAvailable) Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { RectangleShape },
                    effects = { nativeBackdropBlur(32.dp.toPx()) },
                ) else Modifier
                Column(
                    Modifier.widthIn(max = 560.dp).fillMaxWidth().height(sheetHeight)
                        .offset { IntOffset(0, dragOffset.roundToInt()) }
                        .clip(shape).then(blur)
                        .background(tint.copy(alpha = if (blurAvailable) .82f else 1f))
                        .testTag("raised-sheet")
                        .pointerInput(Unit) { detectTapGestures { /* Keep blank surface taps inside. */ } },
                ) {
                    Box(
                        Modifier.fillMaxWidth().height(28.dp).testTag("sheet-drag-handle")
                            .draggable(
                                state = rememberDraggableState { dragOffset = (dragOffset + it).coerceAtLeast(0f) },
                                orientation = Orientation.Vertical,
                                onDragStopped = { velocity ->
                                    if (dragOffset > heightPx * .2f || velocity > flingThreshold) dismiss()
                                    else animate(dragOffset, 0f, animationSpec = spring()) { value, _ -> dragOffset = value }
                                },
                            ), contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(42.dp, 4.dp).clip(RoundedCornerShape(2.dp))
                            .background(ArchiveUiTheme.colorScheme.onBackground.copy(alpha = .22f)))
                    }
                    Row(
                        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp).testTag("sheet-header"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { startAction() }
                        top.yukonga.miuix.kmp.basic.Text(
                            title, Modifier.weight(1f).padding(horizontal = 8.dp).semantics { heading() },
                            style = ArchiveUiTheme.textStyles.title2, fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { endAction() }
                    }
                    Column(
                        Modifier.weight(1f).fillMaxWidth().navigationBarsPadding()
                            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp),
                        content = content,
                    )
                }
            }
        }
    }
}
