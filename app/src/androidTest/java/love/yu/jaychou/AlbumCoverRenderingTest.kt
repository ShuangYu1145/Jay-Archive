package love.yu.jaychou

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import love.yu.jaychou.data.Album
import love.yu.jaychou.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AlbumCoverRenderingTest {
    @get:Rule val app = createComposeRule()

    @Test fun fittedTransparentCoverDoesNotPaintALetterboxBackground() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "transparent-wide-cover.png")
        val bitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        // Leave transparent corners in addition to the space created by ContentScale.Fit.
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply { color = android.graphics.Color.GREEN }
        canvas.drawRect(50f, 0f, 150f, 100f, paint)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val loaded = AtomicBoolean(false)
        try {
            app.setContent {
                Box(Modifier.size(120.dp).background(Color.Magenta).testTag("cover")) {
                    CatalogImage(file.toURI().toString(), "", "cover", Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit, onAspectRatio = { loaded.set(true) }, crossfade = false)
                }
            }
            app.waitUntil(10_000) { loaded.get() }
            val pixels = app.onNodeWithTag("cover").captureToImage().toPixelMap()
            assertEquals(Color.Magenta, pixels[pixels.width / 2, 2])
            assertEquals(Color.Magenta, pixels[2, pixels.height / 2])
            assertEquals(Color.Green, pixels[pixels.width / 2, pixels.height / 2])
        } finally {
            file.delete()
        }
    }

    @Test fun sourceSnapshotRetainsItsCropAndTracksTheReturningCardBounds() {
        val album = Album("cover-test", "cover", "", "2026-01-01", "", "", "", "", emptyList())
        var source by mutableStateOf<AlbumOrigin?>(null)
        var moving by mutableStateOf(false)
        var depth by mutableFloatStateOf(1f)
        app.setContent {
            CompositionLocalProvider(LocalMovingAlbumId provides if (moving) album.id else null) {
                Column {
                    Box(Modifier.graphicsLayer { scaleX = depth; scaleY = depth }) {
                        AlbumMotionSource(album, { _, origin -> source = origin }) { capture, cover, open ->
                            Box(capture.testTag("source-card").clickable(onClick = open)) {
                                Row(cover.size(120.dp).testTag("source")) {
                                    Box(Modifier.weight(1f).fillMaxHeight().background(Color.Red))
                                    Box(Modifier.weight(1f).fillMaxHeight().background(Color.Blue))
                                }
                            }
                        }
                    }
                    Box(Modifier.size(120.dp).testTag("snapshot")) {
                        AlbumSourcePreview(source?.layer) { 1f }
                    }
                }
            }
        }
        val original = app.onNodeWithTag("source", useUnmergedTree = true).captureToImage().toPixelMap()
        app.onNodeWithTag("source-card").performClick()
        var originalWidth = 0f
        app.runOnIdle {
            originalWidth = source!!.currentCoverBounds()!!.width
            moving = true
            depth = .8f
        }
        app.waitForIdle()
        val retained = app.onNodeWithTag("snapshot").captureToImage().toPixelMap()
        assertEquals(original[2, original.height / 2], retained[2, retained.height / 2])
        assertEquals(original[original.width - 3, original.height / 2], retained[retained.width - 3, retained.height / 2])
        app.runOnIdle {
            assertEquals(originalWidth * .8f, source!!.currentCoverBounds()!!.width, 1f)
            moving = false
            depth = 1f
        }
        app.waitForIdle()
        app.runOnIdle { assertEquals(originalWidth, source!!.currentCoverBounds()!!.width, 1f) }
    }
}
