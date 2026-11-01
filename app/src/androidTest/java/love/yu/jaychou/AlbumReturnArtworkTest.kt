package love.yu.jaychou

import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
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
import kotlin.math.abs

@OptIn(ExperimentalTestApi::class)
class AlbumReturnArtworkTest {
    @get:Rule val app = createAndroidComposeRule<ComponentActivity>()
    private var style by mutableStateOf(UiStyle.MIUIX)
    private var direction by mutableStateOf(PredictiveBackExitDirection.FOLLOW_GESTURE)

    private fun show(): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val catalog = AlbumRepository.load(context)
        app.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(400.dp, 800.dp))) {
                val settings = AppSettings(uiStyle = style, predictiveBackExitDirection = direction,
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
}
