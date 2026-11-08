package love.yu.jaychou

import android.graphics.Bitmap
import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import love.yu.jaychou.data.*
import love.yu.jaychou.navigation.ArchiveApp
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.abs

@OptIn(ExperimentalTestApi::class)
class AlbumReturnArtworkTest {
    @get:Rule val app = createAndroidComposeRule<ComponentActivity>()
    private var style by mutableStateOf(UiStyle.MIUIX)
    private var direction by mutableStateOf(PredictiveBackExitDirection.FOLLOW_GESTURE)

    private fun show(coverUrl: String? = null, theme: ThemeMode = ThemeMode.SYSTEM,
        animation: PredictiveBackAnimation = PredictiveBackAnimation.SCALE,
    ): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val loaded = AlbumRepository.load(context)
        val album = loaded.albums.first { it.category == ReleaseCategory.STUDIO }
        val catalog = if (coverUrl == null) loaded else loaded.copy(albums = loaded.albums.map {
            if (it.id == album.id) it.copy(coverUrl = coverUrl) else it
        })
        app.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(400.dp, 800.dp))) {
                val settings = AppSettings(uiStyle = style, theme = theme, predictiveBackAnimation = animation,
                    predictiveBackExitDirection = direction,
                    blur = false, liquidGlass = false)
                ArchiveTheme(settings) { ArchiveApp(catalog, settings, {}, {}, {}, {}, {}, {}, {}, null, {}) }
            }
        }
        app.onNodeWithTag("tab-INTRODUCTION").performClick()
        return "album-${catalog.albums.first { it.category == ReleaseCategory.STUDIO }.id}"
    }

    @Test fun artworkReturnPathIsIndependentOfThePageExitDirection() {
        val albumTag = show()
        for (theme in UiStyle.entries) {
            var firstArtwork: androidx.compose.ui.unit.DpRect? = null
            for (policy in listOf(PredictiveBackExitDirection.ALWAYS_RIGHT, PredictiveBackExitDirection.ALWAYS_LEFT)) {
                app.runOnIdle { style = theme; direction = policy }
                app.onNodeWithTag(albumTag).performClick()
                val pageOrigin = app.onNodeWithTag("album-detail-list").getUnclippedBoundsInRoot().left
                app.mainClock.autoAdvance = false
                try {
                    app.onNodeWithContentDescription(AppText.get(R.string.ui_back_to_albums)).performClick()
                    app.mainClock.advanceTimeBy(112)
                    val artwork = app.onNodeWithTag("album-detail-cover-flight", useUnmergedTree = true).getUnclippedBoundsInRoot()
                    val page = app.onNodeWithTag("album-detail-list").getUnclippedBoundsInRoot()
                    assertTrue("$theme/$policy page must move in its exit direction",
                        (page.left - pageOrigin).value * policy.sign(1f) > 1f)
                    firstArtwork?.let { expected ->
                        assertTrue("$theme artwork must follow the same path for opposite page exits: $expected vs $artwork",
                            abs((expected.left - artwork.left).value) < 1f &&
                                abs((expected.top - artwork.top).value) < 1f &&
                                abs((expected.right - artwork.right).value) < 1f)
                    }
                    firstArtwork = artwork
                    app.mainClock.advanceTimeBy(500)
                } finally { app.mainClock.autoAdvance = true }
                app.onNodeWithTag("album-detail").assertDoesNotExist()
                app.onNodeWithTag(albumTag).assertIsDisplayed()
            }
        }
    }

    @Test fun predictiveBackDetachesArtworkAndCancellationRestoresIt() {
        assumeTrue(Build.VERSION.SDK_INT >= 34)
        val albumTag = show()
        for (theme in UiStyle.entries) {
            app.runOnIdle { style = theme }
            app.onNodeWithTag(albumTag).performClick()
            val destination = app.onNodeWithTag("album-detail-cover", useUnmergedTree = true).getUnclippedBoundsInRoot()
            val destinationWidth = app.onNodeWithTag("album-detail-cover", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.width
            app.runOnIdle {
                app.activity.onBackPressedDispatcher.dispatchOnBackStarted(BackEventCompat(0f, 400f, 0f, 0))
                app.activity.onBackPressedDispatcher.dispatchOnBackProgressed(BackEventCompat(150f, 410f, .6f, 0))
            }
            val artwork = app.onNodeWithTag("album-detail-cover-flight", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue("$theme artwork should shrink independently toward its list position: ${artwork.width} < $destinationWidth",
                artwork.width < destinationWidth)
            app.runOnIdle { app.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
            app.onNodeWithTag("album-detail-cover-flight", useUnmergedTree = true).assertDoesNotExist()
            app.onNodeWithTag("album-detail-cover", useUnmergedTree = true).assertIsDisplayed()
            val restored = app.onNodeWithTag("album-detail-cover", useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertTrue("$theme cancelling restores the cover position", abs((restored.top - destination.top).value) < 1f)
            app.onNodeWithContentDescription(AppText.get(R.string.ui_back_to_albums)).performClick()
            app.onNodeWithTag(albumTag).assertIsDisplayed()
        }
    }

    @Test fun predictiveBackMovesTheCoverBackgroundAndRestoresItOnCancellation() {
        assumeTrue(Build.VERSION.SDK_INT >= 34)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "album-return-transparent-cover.png")
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawRect(60f, 60f, 140f, 140f,
            android.graphics.Paint().apply { color = android.graphics.Color.GREEN })
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        try {
            // A sideways page preview exposes the old surface separately from the artwork overlay.
            val albumTag = show(file.toURI().toString(), ThemeMode.DARK, PredictiveBackAnimation.MIUIX)
            for (theme in UiStyle.entries) {
                app.runOnIdle { style = theme }
                app.onNodeWithTag(albumTag).performClick()
                val cover = app.onNodeWithTag("album-detail-cover", useUnmergedTree = true)
                app.waitUntil(10_000) {
                    val pixels = cover.captureToImage().toPixelMap()
                    pixels[pixels.width / 2, pixels.height / 2] == Color.Green
                }
                val destination = cover.fetchSemanticsNode().boundsInRoot
                val before = app.onRoot().captureToImage().toPixelMap()
                val surfaceColor = before[(destination.left + destination.width * .08f).toInt(), destination.center.y.toInt()]
                val pageColor = before[(destination.left - 5f).toInt(), destination.center.y.toInt()]
                assertTrue("$theme fixture must expose the cover background", surfaceColor != pageColor)
                app.runOnIdle {
                    app.activity.onBackPressedDispatcher.dispatchOnBackStarted(BackEventCompat(0f, 400f, 0f, 0))
                    app.activity.onBackPressedDispatcher.dispatchOnBackProgressed(BackEventCompat(150f, 410f, .6f, 0))
                }
                val stationary = app.onNodeWithTag("album-detail-cover-surface", useUnmergedTree = true)
                    .fetchSemanticsNode().boundsInRoot
                val flight = app.onNodeWithTag("album-detail-cover-flight", useUnmergedTree = true)
                    .fetchSemanticsNode().boundsInRoot
                val exposed = listOf(
                    Offset(stationary.left + stationary.width * .08f, stationary.center.y),
                    Offset(stationary.right - stationary.width * .08f, stationary.center.y),
                    Offset(stationary.center.x, stationary.top + stationary.height * .08f),
                    Offset(stationary.center.x, stationary.bottom - stationary.height * .08f),
                ).first { !flight.inflate(4f).contains(it) }
                val during = app.onRoot().captureToImage().toPixelMap()
                fun assertColor(label: String, expected: Color, point: Offset) {
                    val actual = during[point.x.toInt(), point.y.toInt()]
                    assertTrue("$theme $label: expected $expected, got $actual at $point",
                        abs(expected.red - actual.red) < .02f &&
                            abs(expected.green - actual.green) < .02f &&
                            abs(expected.blue - actual.blue) < .02f)
                }
                assertColor("must leave no stationary cover background", pageColor, exposed)
                assertColor("moving transparent artwork must retain its background", surfaceColor,
                    Offset(flight.left + flight.width * .08f, flight.center.y))
                app.runOnIdle { app.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
                app.onNodeWithTag("album-detail-cover-flight", useUnmergedTree = true).assertDoesNotExist()
                val restored = app.onRoot().captureToImage().toPixelMap()
                assertTrue("$theme cancellation must restore the cover background",
                    restored[(destination.left + destination.width * .08f).toInt(), destination.center.y.toInt()] == surfaceColor)
                app.onNodeWithContentDescription(AppText.get(R.string.ui_back_to_albums)).performClick()
            }
        } finally { file.delete() }
    }
}
