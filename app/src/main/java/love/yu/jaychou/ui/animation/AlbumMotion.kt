package love.yu.jaychou.ui.animation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import love.yu.jaychou.data.Album
import love.yu.jaychou.data.Track

val LocalMovingAlbumId = staticCompositionLocalOf<String?> { null }
val LocalMovingTrack = staticCompositionLocalOf<Pair<String, Int>?> { null }

/** A live render-layer snapshot, never a bitmap allocation on the UI thread. */
data class AlbumOrigin(
    val windowBounds: Rect,
    val layer: GraphicsLayer,
    val coverBounds: Rect? = null,
    val currentCoverBounds: () -> Rect? = { null },
)

@Composable
fun AlbumMotionSource(
    album: Album,
    onOpen: (Album, AlbumOrigin) -> Unit,
    content: @Composable (Modifier, Modifier, () -> Unit) -> Unit,
) {
    CoverMotionSource(LocalMovingAlbumId.current == album.id, { onOpen(album, it) }, content)
}

@Composable
fun TrackMotionSource(
    album: Album,
    track: Track,
    onOpen: (AlbumOrigin) -> Unit,
    content: @Composable (Modifier, Modifier, () -> Unit) -> Unit,
) {
    CoverMotionSource(LocalMovingTrack.current == (album.id to track.number), onOpen, content)
}

@Composable
private fun CoverMotionSource(
    moving: Boolean,
    onOpen: (AlbumOrigin) -> Unit,
    content: @Composable (Modifier, Modifier, () -> Unit) -> Unit,
) {
    var coverBounds by remember { mutableStateOf<Rect?>(null) }
    var coverCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val layer = rememberGraphicsLayer()
    val coverCapture = Modifier.onGloballyPositioned {
        coverCoordinates = it
        coverBounds = it.boundsInWindow()
    }
        .graphicsLayer { alpha = if (moving) 0f else 1f }
        .drawWithContent {
            // Retain the exact source crop while the overlay owns the artwork.
            if (!moving) layer.record { this@drawWithContent.drawContent() }
            drawLayer(layer)
        }
    val capture = Modifier.onGloballyPositioned { bounds = it.boundsInWindow() }
    content(capture, coverCapture) {
        onOpen(AlbumOrigin(bounds, layer, coverBounds) {
            coverCoordinates?.takeIf { it.isAttached }?.boundsInWindow()
        })
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
