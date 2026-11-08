package love.yu.jaychou.ui.animation

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import love.yu.jaychou.playback.MusicPlaybackState
import love.yu.jaychou.ui.components.LocalMiniPlayerArtworkMoving
import love.yu.jaychou.ui.screens.music.MusicPlayerDetails
import love.yu.jaychou.ui.screens.music.rememberPlayerAtmosphere
import love.yu.jaychou.ui.screens.music.rememberPlayerArtwork
import kotlin.math.roundToInt
import kotlin.math.max

/** The surface returns to the entire bar; the artwork follows its own measured endpoints. */
@Composable
internal fun MiniPlayerDetailsHost(
    open: Boolean, sourceBounds: Rect?, sourceArtworkBounds: Rect?, playback: MusicPlaybackState,
    title: String, artist: String, onBack: () -> Unit, root: @Composable () -> Unit,
) {
    val expansion = remember { Animatable(if (open) 1f else 0f) }
    val gesture = remember { Animatable(0f) }
    val visible = open || expansion.value > 0f
    val predictive = Build.VERSION.SDK_INT >= 34 && LocalSecondaryPredictiveBack.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var hostOrigin by remember { mutableStateOf(Offset.Zero) }
    var hostSize by remember { mutableStateOf(IntSize.Zero) }
    var dragPixels by remember { mutableFloatStateOf(0f) }
    var springBack by remember { mutableStateOf<Job?>(null) }
    val back by rememberUpdatedState(onBack)
    val artwork = rememberPlayerArtwork(playback.currentSong?.cover.orEmpty())
    val atmosphere = rememberPlayerAtmosphere(artwork)
    var artworkDestination by remember { mutableStateOf<Rect?>(null) }
    var artworkPageVisible by remember { mutableStateOf(true) }
    val progress = {
        val dragFraction = (dragPixels / hostSize.height.coerceAtLeast(1)).coerceIn(0f, .9f)
        (expansion.value * (1f - max(gesture.value, dragFraction))).coerceIn(0f, 1f)
    }
    fun finishDrag(velocity: Float = 0f, cancelled: Boolean = false) {
        springBack?.cancel()
        val farEnough = dragPixels > hostSize.height * .16f
        val fling = dragPixels > with(density) { 24.dp.toPx() } && velocity > 1_200f
        if (!cancelled && open && (farEnough || fling)) back()
        else springBack = scope.launch {
            animate(dragPixels, 0f, animationSpec = spring(dampingRatio = .85f, stiffness = 420f)) { value, _ ->
                dragPixels = value.coerceAtLeast(0f)
            }
        }
    }
    fun dragBy(delta: Float) {
        springBack?.cancel()
        dragPixels = (dragPixels + delta).coerceIn(0f, hostSize.height * .9f)
    }
    // A lyrics list keeps normal scrolling; an unconsumed pull at its top dismisses the page.
    val nestedScroll = remember(open, density) { object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (open && dragPixels > 0 && source == NestedScrollSource.UserInput) {
                dragBy(available.y)
                return Offset(0f, available.y)
            }
            return Offset.Zero
        }
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (open && available.y > 0 && source == NestedScrollSource.UserInput) {
                dragBy(available.y)
                return Offset(0f, available.y)
            }
            return Offset.Zero
        }
        override suspend fun onPreFling(available: Velocity): Velocity {
            if (dragPixels > 0) { finishDrag(available.y); return available }
            return Velocity.Zero
        }
    } }
    LaunchedEffect(open) {
        springBack?.cancel()
        if (open) {
            artworkPageVisible = true
            dragPixels = 0f
            gesture.snapTo(0f)
            expansion.animateTo(1f, spring(dampingRatio = .88f, stiffness = 360f))
        } else {
            val current = progress()
            expansion.snapTo(current)
            dragPixels = 0f
            gesture.snapTo(0f)
            expansion.animateTo(0f, tween(300))
            artworkDestination = null
        }
    }
    BackHandler(visible && !open) { }
    BackHandler(open && !predictive) { back() }
    PredictiveBackHandler(open && predictive) { events ->
        try {
            events.collect { gesture.snapTo(it.progress.coerceIn(0f, 1f)) }
            back()
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { gesture.animateTo(0f, spring(dampingRatio = 1f, stiffness = 400f)) }
            throw cancelled
        }
    }
    Box(Modifier.fillMaxSize().onGloballyPositioned {
        hostOrigin = it.positionInWindow(); hostSize = it.size
    }) {
        CompositionLocalProvider(LocalMiniPlayerArtworkMoving provides (visible && artwork != null)) {
            Box(Modifier.fillMaxSize().then(if (visible) Modifier.clearAndSetSemantics {} else Modifier)) { root() }
        }
        if (visible && hostSize.width > 0 && hostSize.height > 0) {
            val full = Rect(Offset.Zero, Size(hostSize.width.toFloat(), hostSize.height.toFloat()))
            val mini = sourceBounds?.translate(-hostOrigin) ?: Rect(
                with(density) { 20.dp.toPx() }, full.height * .84f,
                full.width - with(density) { 20.dp.toPx() }, full.height * .84f + with(density) { 68.dp.toPx() })
            val inset = with(density) { 10.dp.toPx() }
            val thumbnail = with(density) { 48.dp.toPx() }
            val sourceArtwork = sourceArtworkBounds?.translate(-hostOrigin)
                ?: Rect(mini.topLeft + Offset(inset, inset), Size(thumbnail, thumbnail))
            fun surface() = lerp(mini, full, progress())
            fun radius() = with(density) { 20.dp.toPx() } * (1f - progress())
            Box(Modifier.fillMaxSize().testTag("player-details-transition").nestedScroll(nestedScroll)
                .semantics { dismiss { if (open) { back(); true } else false } }
                .pointerInput(open, hostSize) {
                    if (open) {
                        val velocity = VelocityTracker()
                        detectVerticalDragGestures(
                            onDragStart = { springBack?.cancel(); velocity.resetTracking() },
                            onDragEnd = { finishDrag(velocity.calculateVelocity().y) },
                            onDragCancel = { finishDrag(cancelled = true) },
                        ) { change, delta ->
                            velocity.addPosition(change.uptimeMillis, change.position)
                            if (dragPixels > 0 || delta > 0) { change.consume(); dragBy(delta) }
                        }
                    } else awaitPointerEventScope { while (true) awaitPointerEvent() }
                }) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(Color.Black.copy(alpha = .12f * progress()))
                }
                // This independently measured surface collapses to the whole mini-player bar.
                val rect = surface()
                Box(Modifier.offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                    .requiredSize(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
                    .graphicsLayer {
                        shape = RoundedCornerShape(with(density) { radius().toDp() })
                        clip = true
                        alpha = (progress() / .2f).coerceIn(0f, 1f)
                    }
                    .background(Brush.verticalGradient(listOf(atmosphere.top, atmosphere.center, atmosphere.bottom)))
                    .testTag("player-details-card"))
                // Text and controls fade at their final layout, without inheriting the cover transform.
                Box(Modifier.fillMaxSize().drawWithContent {
                    clipPath(Path().apply { addRoundRect(RoundRect(surface(), CornerRadius(radius()))) }) {
                        this@drawWithContent.drawContent()
                    }
                }.graphicsLayer { alpha = ((progress() - .12f) / .68f).coerceIn(0f, 1f) }) {
                    MusicPlayerDetails(playback, title, artist, atmosphere, artwork,
                        artworkVisible = { progress() >= .999f || artwork == null },
                        onArtworkBounds = { artworkDestination = it.translate(-hostOrigin) },
                        onArtworkPageVisibility = { artworkPageVisible = it })
                }
                if (artwork != null && artworkPageVisible) {
                    val target = artworkDestination ?: sourceArtwork
                    // The already decoded bitmap stays at destination resolution during the flight.
                    Image(artwork, title,
                        Modifier.requiredSize(with(density) { target.width.toDp() }, with(density) { target.height.toDp() })
                            .graphicsLayer {
                                val cover = lerp(sourceArtwork, target, progress())
                                transformOrigin = TransformOrigin(0f, 0f)
                                scaleX = cover.width / target.width
                                scaleY = cover.height / target.height
                                translationX = cover.left; translationY = cover.top
                                shape = RoundedCornerShape((10f / scaleX.coerceAtLeast(.001f)).dp)
                                clip = true
                                alpha = if (progress() < .999f) 1f else 0f
                            }
                            .then(if (progress() < .999f) Modifier.testTag("player-details-artwork-flight") else Modifier)
                            .clearAndSetSemantics {}, contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                }
            }
        }
    }
}
