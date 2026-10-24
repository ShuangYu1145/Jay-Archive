package love.yu.jaychou.ui.effects

import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.*
internal val LocalNavigationBackdrop = staticCompositionLocalOf<Backdrop?> { null }
internal val LocalArchiveLiquidGlass = staticCompositionLocalOf { true }

/** A single blur pass for native bars and the non-liquid section capsule. */
@RequiresApi(33)
internal fun Modifier.navigationSurfaceBlur(backdrop: Backdrop, shape: Shape, tint: Color): Modifier =
    drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = { blur(20.dp.toPx(), 20.dp.toPx()) },
        onDrawSurface = { drawRect(tint.copy(alpha = .78f)) },
    )

// The caller gates backdrop creation at SDK 33;
// no backdrop, highlight or RuntimeShader objects are instantiated on older systems.
@RequiresApi(33)
@Composable
fun GlassLayer(
    tint: Color,
    content: @Composable (captureModifier: Modifier, glassModifier: Modifier) -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    CompositionLocalProvider(LocalNavigationBackdrop provides backdrop) {
        content(Modifier.layerBackdrop(backdrop).background(tint), Modifier)
    }
}

/** Includes the status-bar inset in the sampled, tinted header surface. */
@RequiresApi(33)
@Composable
fun HeaderGlassLayer(tint: Color, content: @Composable (Modifier, Modifier) -> Unit) {
    val backdrop = rememberLayerBackdrop()
    val header = Modifier.textureBlur(backdrop, RectangleShape, blurRadius = 22f, noiseCoefficient = 0f)
        .background(tint.copy(alpha = .86f))
    content(Modifier.layerBackdrop(backdrop), header)
}
