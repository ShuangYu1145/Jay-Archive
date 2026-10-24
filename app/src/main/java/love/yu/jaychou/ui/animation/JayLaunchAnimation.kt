package love.yu.jaychou.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import love.yu.jaychou.R

/** Real vector strokes are revealed by travelled distance, then dissolve into the app. */
@Composable
fun JayLaunchAnimation(playOnLaunch: Boolean, ready: Boolean = true, content: @Composable () -> Unit) {
    var playing by rememberSaveable { mutableStateOf(playOnLaunch) }
    var waitingForFrame by remember { mutableStateOf(playing && !ready) }
    val latestReady by rememberUpdatedState(ready)
    val writing = remember { Animatable(0f) }
    val dissolve = remember { Animatable(if (playing) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (playing) {
            // A missing platform/renderer signal must never leave the app transparent forever.
            var firstFrame: Long? = null
            while (!latestReady) {
                val frame = withFrameNanos { it }
                val start = firstFrame ?: frame.also { firstFrame = it }
                if (frame - start >= 1_200_000_000L) break
            }
            waitingForFrame = false
            writing.animateTo(1f, tween(1_650, easing = LinearEasing))
            delay(160)
            dissolve.animateTo(1f, tween(600))
            playing = false
        }
    }
    val background = ArchiveUiTheme.colorScheme.background
    val ink = ArchiveUiTheme.colorScheme.primary
    val strokes = remember { jaySignatureStrokes() }
    Box(Modifier.fillMaxSize().background(background)) {
        Box(Modifier.fillMaxSize().graphicsLayer {
            alpha = dissolve.value
        }.then(if (playing) Modifier.clearAndSetSemantics {} else Modifier)) { content() }
        if (playing) Box(
            Modifier.fillMaxSize().testTag("jay-launch-animation")
                .graphicsLayer { alpha = 1f - dissolve.value }
                .background(background)
                .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
            contentAlignment = Alignment.Center,
        ) {
            if (waitingForFrame) Image(
                painterResource(R.drawable.splash_icon), contentDescription = null,
                modifier = Modifier.size(112.dp).testTag("jay-launch-icon"),
            ) else Canvas(Modifier.width(280.dp).height(180.dp).graphicsLayer {
                scaleX = 1f + dissolve.value * .05f
                scaleY = scaleX
            }) {
                val factor = size.width / 360f
                val totalLength = strokes.sumOf { it.length.toDouble() }.toFloat()
                var remaining = totalLength * writing.value
                translate(top = (size.height - 210f * factor) / 2f) {
                    scale(factor, factor, pivot = Offset.Zero) {
                        for (stroke in strokes) {
                            if (remaining <= 0f) break
                            val revealed = Path()
                            stroke.getSegment(0f, remaining.coerceAtMost(stroke.length), revealed, true)
                            drawPath(revealed, ink, style = Stroke(width = 5f,
                                cap = StrokeCap.Round, join = StrokeJoin.Round))
                            remaining -= stroke.length
                        }
                    }
                }
            }
        }
    }
}

private fun jaySignatureStrokes(): List<PathMeasure> {
    val top = Path().apply {
        moveTo(42f, 61f)
        cubicTo(72f, 27f, 145f, 23f, 162f, 42f)
        cubicTo(174f, 60f, 111f, 80f, 87f, 61f)
    }
    val j = Path().apply {
        moveTo(138f, 36f)
        cubicTo(129f, 73f, 118f, 129f, 86f, 151f)
        cubicTo(55f, 175f, 26f, 153f, 41f, 134f)
        cubicTo(48f, 125f, 61f, 128f, 68f, 138f)
    }
    val a = Path().apply {
        moveTo(122f, 125f)
        cubicTo(136f, 118f, 143f, 98f, 163f, 94f)
        cubicTo(190f, 86f, 182f, 114f, 165f, 127f)
        cubicTo(146f, 142f, 139f, 124f, 154f, 105f)
        cubicTo(164f, 94f, 177f, 91f, 184f, 96f)
        cubicTo(177f, 109f, 169f, 132f, 181f, 133f)
        cubicTo(193f, 134f, 204f, 118f, 213f, 107f)
    }
    val y = Path().apply {
        moveTo(216f, 94f)
        cubicTo(207f, 110f, 201f, 134f, 214f, 132f)
        cubicTo(229f, 130f, 246f, 102f, 250f, 94f)
        cubicTo(241f, 121f, 230f, 155f, 211f, 176f)
        cubicTo(192f, 198f, 179f, 183f, 200f, 163f)
        cubicTo(225f, 140f, 273f, 124f, 310f, 107f)
    }
    val flourish = Path().apply {
        moveTo(74f, 183f)
        cubicTo(132f, 165f, 253f, 160f, 313f, 169f)
    }
    return listOf(top, j, a, y, flourish).map { path -> PathMeasure().apply { setPath(path, false) } }
}
