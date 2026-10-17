package love.yu.jaychou.ui

import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.*

// Keep every miuix-blur call in this file. The caller gates entry at SDK 33;
// no backdrop, highlight or RuntimeShader objects are instantiated on older systems.
@RequiresApi(33)
@Composable
fun GlassLayer(
    tint: Color,
    dark: Boolean,
    content: @Composable (captureModifier: Modifier, glassModifier: Modifier) -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    val glass = Modifier.textureBlur(
        backdrop = backdrop,
        shape = RoundedCornerShape(40.dp),
        blurRadius = 24f,
        noiseCoefficient = 0f,
    ).background(tint.copy(alpha = if (dark) .67f else .58f), RoundedCornerShape(40.dp))
    // The solid background is inside the captured node, not on its parent.
    content(Modifier.layerBackdrop(backdrop).background(tint), glass)
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
