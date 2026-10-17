package love.yu.jaychou.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import love.yu.jaychou.data.Album
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/** A live render-layer snapshot, never a bitmap allocation on the UI thread. */
data class AlbumOrigin(val windowBounds: Rect, val layer: GraphicsLayer)

@Composable
fun AlbumMotionSource(
    album: Album,
    onOpen: (Album, AlbumOrigin) -> Unit,
    content: @Composable (Modifier, () -> Unit) -> Unit,
) {
    MotionSource(onOpen = { onOpen(album, it) }, content = content)
}

@Composable
fun MotionSource(onOpen: (AlbumOrigin) -> Unit, content: @Composable (Modifier, () -> Unit) -> Unit) {
    val layer = rememberGraphicsLayer()
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val capture = Modifier.onGloballyPositioned { bounds = it.boundsInWindow() }
        .drawWithContent {
            layer.record { this@drawWithContent.drawContent() }
            drawLayer(layer)
        }
    content(capture) { onOpen(AlbumOrigin(bounds, layer)) }
}

/** Compensate X/Y independently so a wide, short card retains circular screen-space corners. */
class ProjectedCornerShape(private val radiusPx: Float, private val scaleX: Float, private val scaleY: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val radius = radiusPx.coerceAtMost(minOf(size.width * scaleX, size.height * scaleY) / 2f).coerceAtLeast(0f)
        return Outline.Rounded(RoundRect(Rect(Offset.Zero, size),
            CornerRadius(radius / scaleX.coerceAtLeast(.001f), radius / scaleY.coerceAtLeast(.001f))))
    }
}

/** The original card fades into the expanding detail surface and back on dismissal. */
@Composable
fun AlbumSourcePreview(layer: GraphicsLayer?, opacity: () -> Float) {
    if (layer != null) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = opacity() }) {
            Canvas(Modifier.fillMaxSize()) {
                if (layer.size.width > 0 && layer.size.height > 0) {
                    scale(size.width / layer.size.width, size.height / layer.size.height, Offset.Zero) {
                        drawLayer(layer)
                    }
                }
            }
        }
    }
}
