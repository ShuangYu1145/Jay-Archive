package love.yu.jaychou.ui.effects

import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import top.yukonga.miuix.kmp.blur.*
import love.yu.jaychou.ui.effects.liquid.rememberCombinedBackdrop
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
internal val LocalNavigationBackdrop = staticCompositionLocalOf<Backdrop?> { null }
internal val LocalArchiveLiquidGlass = staticCompositionLocalOf { true }

/** Native Gaussian blur avoids the shader downsample passes on nested card/bar captures. */
@RequiresApi(33)
internal fun BackdropEffectScope.nativeBackdropBlur(radius: Float) {
    padding = radius * 2f
    effect(BlurEffect(radius, radius, TileMode.Clamp))
}

@Composable
private fun withWallpaper(backdrop: Backdrop): Backdrop {
    val wallpaper = LocalWallpaperBackdrop.current
    return if (wallpaper == null) backdrop else rememberCombinedBackdrop(wallpaper, backdrop)
}

/** A single blur pass for native bars and the non-liquid section capsule. */
@RequiresApi(33)
internal fun Modifier.navigationSurfaceBlur(backdrop: Backdrop, shape: Shape, tint: Color): Modifier =
    drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = { nativeBackdropBlur(20.dp.toPx()) },
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
    val composite = withWallpaper(backdrop)
    CompositionLocalProvider(LocalNavigationBackdrop provides composite) {
        content(Modifier.layerBackdrop(backdrop).background(
            if (LocalCustomBackgroundPath.current != null) Color.Transparent else tint), Modifier)
    }
}

/** Includes the status-bar inset in the sampled, tinted header surface. */
@RequiresApi(33)
@Composable
fun HeaderGlassLayer(tint: Color, content: @Composable (Modifier, Modifier) -> Unit, progressive: Boolean = false) {
    val backdrop = rememberLayerBackdrop()
    val composite = withWallpaper(backdrop)
    val surfaceTint = if (LocalFloatingBottomBar.current && backgroundCardBlurEnabled())
        ArchiveUiTheme.colorScheme.surfaceContainer.copy(alpha = BackgroundBlurSurfaceAlpha)
    else tint.copy(alpha = .86f)
    val maskPaint = androidx.compose.runtime.remember { Paint() }
    // Extend only the drawing region; toolbar metrics and content insets stay unchanged.
    val header = (if (progressive) Modifier.headerBackdropOutset() else Modifier)
        .drawBackdrop(composite, shape = { RectangleShape },
        effects = {
            if (progressive) progressiveHeaderBlur(22.dp.toPx()) else nativeBackdropBlur(22.dp.toPx())
        }, onDrawBackdrop = { draw ->
            if (progressive) {
                // Fade the sampled image itself, not just its tint. Never mask toolbar content.
                drawContext.canvas.saveLayer(Rect(Offset.Zero, size), maskPaint)
                draw()
                drawRect(Brush.verticalGradient(0f to Color.White, .55f to Color.White,
                    1f to Color.Transparent), blendMode = BlendMode.DstIn)
                drawContext.canvas.restore()
            } else draw()
        })
        .then(if (progressive) Modifier.background(androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(surfaceTint, Color.Transparent))) else Modifier.background(surfaceTint))
        .then(if (progressive) Modifier.headerContentInset() else Modifier)
    CompositionLocalProvider(LocalNavigationBackdrop provides composite) {
        content(Modifier.layerBackdrop(backdrop), header)
    }
}

private val HeaderFadeOutset = 32.dp

/** Paired layouts give the backdrop a fade tail without moving or resizing the toolbar. */
private fun Modifier.headerBackdropOutset() = layout { measurable, constraints ->
    val extra = HeaderFadeOutset.roundToPx()
    val placeable = measurable.measure(constraints.offset(vertical = extra))
    layout(placeable.width, placeable.height - extra) { placeable.placeRelative(0, 0) }
}

private fun Modifier.headerContentInset() = layout { measurable, constraints ->
    val extra = HeaderFadeOutset.roundToPx()
    val placeable = measurable.measure(constraints.offset(vertical = -extra))
    layout(placeable.width, placeable.height + extra) { placeable.placeRelative(0, 0) }
}

internal val LocalTopAppBarBlur = staticCompositionLocalOf { love.yu.jaychou.data.TopAppBarBlur.GAUSSIAN }

/** Variable-radius separable Gaussian for Miuix 0.9.3, before its progressive API. */
@RequiresApi(33)
private fun BackdropEffectScope.progressiveHeaderBlur(radius: Float) {
    padding = radius * 2f
    val inset = padding
    val height = size.height.coerceAtLeast(1f)
    for (vertical in listOf(false, true)) {
        runtimeShaderEffect("HeaderProgressive-$vertical", ProgressiveHeaderShader, "child") {
            setFloatUniform("inset", inset)
            setFloatUniform("height", height)
            setFloatUniform("radius", radius)
            setFloatUniform("axis", if (vertical) 0f else 1f, if (vertical) 1f else 0f)
        }
    }
}

private const val ProgressiveHeaderShader = """
    uniform shader child;
    uniform float inset;
    uniform float height;
    uniform float radius;
    uniform float2 axis;
    half4 main(float2 p) {
        float strength = 1.0 - smoothstep(0.0, height, p.y - inset);
        float stepSize = radius * strength / 8.0;
        half4 sum = half4(0.0);
        float total = 0.0;
        for (int i = -8; i <= 8; i++) {
            float x = float(i);
            float weight = exp(-0.5 * x * x / 7.111111);
            sum += child.eval(p + axis * x * stepSize) * half(weight);
            total += weight;
        }
        return sum / half(total);
    }
"""
