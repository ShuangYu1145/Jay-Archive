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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme

/** Applied to the visible capsule only; system-inset space must remain transparent. */
@Composable
internal fun Modifier.couiFrostedSurface(blur: Boolean): Modifier {
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
