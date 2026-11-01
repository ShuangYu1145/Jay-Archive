package love.yu.jaychou.ui.effects

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import love.yu.jaychou.ui.components.LocalArchiveBlur

/** Shared opt-in and solid fallback for Material rails, dialogs and sheets. */
@Composable
internal fun materialSurfaceBlurEnabled(): Boolean =
    Build.VERSION.SDK_INT >= 33 && LocalArchiveBlur.current && LocalNavigationBackdrop.current != null

@Composable
internal fun Modifier.materialFrostedSurface(shape: Shape, tint: Color): Modifier {
    val backdrop = LocalNavigationBackdrop.current
    return if (materialSurfaceBlurEnabled() && backdrop != null)
        navigationSurfaceBlur(backdrop, shape, tint)
    else this
}

/** Preference rows are cards too; sample wallpaper only, never their own text. */
@Composable
internal fun Modifier.materialCardSurface(shape: Shape, tint: Color): Modifier =
    backgroundCardBlur(shape).background(
        if (backgroundCardBlurEnabled()) tint.copy(alpha = BackgroundBlurSurfaceAlpha) else tint, shape)
