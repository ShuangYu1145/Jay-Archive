// Gradient field inspired by 1812z/HyperIsland's AboutPage (MIT).
// See assets/licenses/HyperIsland-MIT.txt and about-gradient-NOTICE.txt.
package love.yu.jaychou.ui.screens.about

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Soft overlapping color fields; no platform blur or RuntimeShader is required. */
@Composable
internal fun AboutBackground(active: Boolean, modifier: Modifier = Modifier) {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val running = active && lifecycle.isAtLeast(Lifecycle.State.STARTED)
    val phase: State<Float> = if (running) {
        rememberInfiniteTransition(label = "about-gradient").animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(48_000, easing = LinearEasing), RepeatMode.Restart),
            label = "about-gradient-drift",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    // Use the actual app palette, including a manually selected theme.
    val background = ArchiveUiTheme.colorScheme.background
    val dark = background.luminance() < .5f
    Box(modifier.testTag("about-gradient").clipToBounds().drawWithCache {
        val blue = if (dark) Color(0xFF293B73) else Color(0xFFB2C6F5)
        val pink = if (dark) Color(0xFF563149) else Color(0xFFF0C3DB)
        val violet = if (dark) Color(0xFF393363) else Color(0xFFD4C4F2)
        val wash = Brush.linearGradient(
            listOf(blue, pink, violet),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        )
        val radius = size.maxDimension * .8f
        fun glow(color: Color, center: Offset) = Brush.radialGradient(
            listOf(color.copy(alpha = .85f), color.copy(alpha = 0f)),
            center = center, radius = radius,
        )
        val blueCenter = Offset(size.width * .05f, size.height * .2f)
        val pinkCenter = Offset(size.width * .95f, size.height * .3f)
        val violetCenter = Offset(size.width * .25f, size.height * .7f)
        val blueGlow = glow(blue, blueCenter)
        val pinkGlow = glow(pink, pinkCenter)
        val violetGlow = glow(violet, violetCenter)
        val fade = Brush.verticalGradient(
            0f to background.copy(alpha = 0f),
            .64f to background.copy(alpha = 0f),
            1f to background,
        )
        onDrawBehind {
            val time = phase.value
            drawRect(wash)
            translate(left = sin(time) * size.width * .12f, top = (cos(time) - 1f) * size.height * .04f) {
                drawCircle(blueGlow, radius, blueCenter)
            }
            translate(left = -sin(time) * size.width * .1f, top = sin(time) * size.height * .07f) {
                drawCircle(pinkGlow, radius, pinkCenter)
            }
            translate(left = (cos(time) - 1f) * size.width * .06f, top = -sin(time) * size.height * .05f) {
                drawCircle(violetGlow, radius, violetCenter)
            }
            drawRect(fade)
        }
    })
}
