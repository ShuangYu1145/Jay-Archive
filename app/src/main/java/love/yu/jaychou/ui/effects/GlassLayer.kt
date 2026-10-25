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
import androidx.compose.ui.unit.dp
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
fun HeaderGlassLayer(tint: Color, content: @Composable (Modifier, Modifier) -> Unit) {
    val backdrop = rememberLayerBackdrop()
    val composite = withWallpaper(backdrop)
    val surfaceTint = if (LocalFloatingBottomBar.current && backgroundCardBlurEnabled())
        ArchiveUiTheme.colorScheme.surfaceContainer.copy(alpha = BackgroundBlurSurfaceAlpha)
    else tint.copy(alpha = .86f)
    val header = Modifier.drawBackdrop(composite, shape = { RectangleShape },
        effects = { nativeBackdropBlur(22.dp.toPx()) })
        .background(surfaceTint)
    content(Modifier.layerBackdrop(backdrop), header)
}
