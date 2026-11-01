package love.yu.jaychou.ui.effects

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import top.yukonga.miuix.kmp.blur.drawBackdrop

/** Applied to the visible capsule only; system-inset space must remain transparent. */
@Composable
internal fun Modifier.couiFrostedSurface(
    blur: Boolean,
    shape: Shape = CircleShape,
    floating: Boolean = true,
): Modifier {
    val colors = ArchiveUiTheme.colorScheme
    val backdrop = LocalNavigationBackdrop.current
    val dark = colors.background.luminance() < .5f
    val surface = if (blur && Build.VERSION.SDK_INT >= 33 && backdrop != null)
        Modifier.drawBackdrop(backdrop, shape = { shape },
            effects = { nativeBackdropBlur(26.dp.toPx()) },
            onDrawSurface = { drawRect(colors.surfaceContainer.copy(alpha = if (dark) .68f else .74f)) })
    else Modifier.background(colors.surfaceContainer, shape)
    return then(if (floating) Modifier
        .dropShadow(shape, Shadow(radius = 14.dp, color = Color.Black,
            alpha = if (dark) .24f else .11f, offset = DpOffset(0.dp, 4.dp)))
        .dropShadow(shape, Shadow(radius = 10.dp, color = colors.primary,
            alpha = if (dark) .07f else .035f, offset = DpOffset(0.dp, 3.dp))) else Modifier)
        .then(surface)
        .couiSurfaceFinish(shape, floating)
        .clip(shape)
}

/** Crisp upper lip and broad lower reflection, without whitening the content itself. */
@Composable
internal fun Modifier.couiSurfaceFinish(shape: Shape, floating: Boolean = false): Modifier {
    val dark = ArchiveUiTheme.colorScheme.background.luminance() < .5f
    val strength = if (floating) 1f else .55f
    return border(2.dp, Brush.verticalGradient(
        0f to Color.Transparent,
        .55f to Color.Transparent,
        1f to Color.White.copy(alpha = (if (dark) .08f else .22f) * strength)), shape)
        .border(.65.dp, Brush.verticalGradient(
            0f to Color.White.copy(alpha = (if (dark) .28f else .9f) * strength),
            .18f to Color.White.copy(alpha = (if (dark) .07f else .26f) * strength),
            .6f to Color.White.copy(alpha = .025f * strength),
            1f to Color.White.copy(alpha = (if (dark) .16f else .5f) * strength)), shape)
}

/** Original capsule finish for section tabs; the new sheen belongs to buttons and bottom bars. */
@Composable
internal fun Modifier.couiSectionSurface(blur: Boolean): Modifier {
    val colors = ArchiveUiTheme.colorScheme
    val backdrop = LocalNavigationBackdrop.current
    val dark = colors.background.luminance() < .5f
    val surface = if (blur && Build.VERSION.SDK_INT >= 33 && backdrop != null)
        Modifier.navigationSurfaceBlur(backdrop, CircleShape, colors.surfaceContainer)
    else Modifier.background(colors.surfaceContainer, CircleShape)
    return dropShadow(CircleShape, Shadow(radius = 18.dp, color = Color.Black,
        alpha = if (dark) .32f else .16f, offset = DpOffset(0.dp, 5.dp)))
        .then(surface)
        .border(.5.dp, Color.White.copy(alpha = if (dark) .1f else .55f), CircleShape)
        .clip(CircleShape)
}
