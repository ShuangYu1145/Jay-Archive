package love.yu.jaychou.ui.animation

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import love.yu.jaychou.data.PredictiveBackAnimation
import love.yu.jaychou.data.PredictiveBackExitDirection
import androidx.compose.ui.layout.onSizeChanged

val LocalSecondaryExitDirection = staticCompositionLocalOf { PredictiveBackExitDirection.FOLLOW_GESTURE }
val LocalSecondaryBackAnimation = staticCompositionLocalOf { PredictiveBackAnimation.SCALE }
val LocalSecondaryPredictiveBack = staticCompositionLocalOf { true }

/** Focus-driven relocation must wait until the entering page is in its final position. */
internal val LocalSecondaryPageReady = staticCompositionLocalOf { true }

/** Keeps the previous page alive during entry, gesture preview, cancellation and exit. */
@Composable
fun <T : Any> SecondaryPageHost(
    page: T?, onBack: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onVisibilityChanged: (Boolean) -> Unit = {},
    root: @Composable () -> Unit,
    content: @Composable (T) -> Unit,
) {
    var retained by remember { mutableStateOf(page) }
    val expansion = remember { Animatable(if (page == null) 0f else 1f) }
    val gesture = remember { Animatable(0f) }
    var edge by remember { mutableFloatStateOf(1f) }
    var pivotY by remember { mutableFloatStateOf(.5f) }
    var height by remember { mutableFloatStateOf(1f) }
    val back by rememberUpdatedState(onBack)
    val style = LocalSecondaryBackAnimation.current
    val exitDirection = LocalSecondaryExitDirection.current
    val predictive = Build.VERSION.SDK_INT >= 34 && LocalSecondaryPredictiveBack.current && style != PredictiveBackAnimation.NONE
    val visibilityCallback by rememberUpdatedState(onVisibilityChanged)
    val visible = page != null || retained != null
    val pageReady by remember(page, enabled) {
        derivedStateOf {
            page != null && enabled && !expansion.isRunning && expansion.value == 1f && gesture.value == 0f
        }
    }
    LaunchedEffect(visible) { visibilityCallback(visible) }
    DisposableEffect(Unit) { onDispose { visibilityCallback(false) } }
    LaunchedEffect(page) {
        if (page != null) {
            retained = page
            edge = 1f
            pivotY = .5f
            gesture.snapTo(0f)
            expansion.animateTo(1f, spring(dampingRatio = .9f, stiffness = 380f))
        } else {
            if (gesture.value == 0f) edge = 1f
            expansion.animateTo(0f, tween(220))
            retained = null
            gesture.snapTo(0f)
        }
    }
    BackHandler(enabled && page == null && retained != null) { }
    BackHandler(enabled && page != null && !predictive) { back() }
    PredictiveBackHandler(enabled && page != null && predictive) { events ->
        try {
            events.collect { event ->
                edge = if (event.swipeEdge == 0) 1f else -1f
                pivotY = (event.touchY / height).coerceIn(.1f, .9f)
                gesture.snapTo(event.progress.coerceIn(0f, 1f))
            }
            back()
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                gesture.animateTo(0f, spring(dampingRatio = 1f, stiffness = 500f))
            }
            throw cancelled
        }
    }
    val displayed = page ?: retained
    val background = ArchiveUiTheme.pageBackground
    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().graphicsLayer {
            val depth = expansion.value * (1f - gesture.value)
            scaleX = 1f - .035f * depth; scaleY = scaleX
            alpha = 1f - .12f * depth
        }.then(if (displayed != null) Modifier.clearAndSetSemantics {} else Modifier)) { root() }
        if (displayed != null) Box(Modifier.fillMaxSize().onSizeChanged { height = it.height.toFloat().coerceAtLeast(1f) }.graphicsLayer {
            val progress = gesture.value
            val open = expansion.value.coerceIn(0f, 1f)
            val closing = page == null
            val scaleProgress = if (closing && progress > 0f) progress + (1f - progress) * (1f - open) else progress
            applySecondaryBackTransform(style, scaleProgress, edge, pivotY, exitDirection)
            val travelSign = if (closing) exitDirection.sign(edge) else 1f
            translationX = travelSign * (1f - open) * size.width + translationX * open
            alpha = open.coerceIn(0f, 1f)
            shape = RoundedCornerShape((28f * progress).dp)
            clip = progress > 0f
        }.background(background)) {
            CompositionLocalProvider(LocalSecondaryPageReady provides pageReady) {
                content(displayed)
            }
        }
    }
}

/** Same gesture geometry for ordinary pages and the independent album artwork transition. */
internal fun androidx.compose.ui.graphics.GraphicsLayerScope.applySecondaryBackTransform(
    style: PredictiveBackAnimation, progress: Float, edge: Float, pivotY: Float,
    exitDirection: PredictiveBackExitDirection = PredictiveBackExitDirection.FOLLOW_GESTURE,
) {
    scaleX = when (style) {
        PredictiveBackAnimation.AOSP -> 1f - .1f * progress
        PredictiveBackAnimation.SCALE -> 1f - .15f * progress
        else -> 1f
    }
    scaleY = scaleX
    transformOrigin = TransformOrigin(if (edge > 0) .8f else .2f, pivotY)
    translationX = exitDirection.sign(edge) * when (style) {
        PredictiveBackAnimation.MIUIX -> progress * size.width * .7f
        PredictiveBackAnimation.CLASSIC -> progress * size.width * .4f
        PredictiveBackAnimation.AOSP -> progress * 12.dp.toPx()
        else -> 0f
    }
    shape = RoundedCornerShape((28f * progress).dp)
    clip = progress > 0f
}
