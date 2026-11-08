package love.yu.jaychou

import android.graphics.Bitmap
import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsActions
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

@OptIn(ExperimentalTestApi::class)
class CoverFlowReturnMotionTest {
    @get:Rule val app = createAndroidComposeRule<ComponentActivity>()

    private fun show(): Album {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val file = File(context.cacheDir, "return-portrait-art.png")
        val image = Bitmap.createBitmap(120, 180, Bitmap.Config.ARGB_8888)
        image.eraseColor(android.graphics.Color.rgb(32, 125, 190))
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
        val catalog = AlbumRepository.load(context)
        val album = catalog.albums.first { it.category == ReleaseCategory.STUDIO }.copy(coverUrl = file.toURI().toString())
        app.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(900.dp, 420.dp))) {
                val settings = AppSettings(uiStyle = UiStyle.COUI, albumLayout = AlbumLayout.COVER_FLOW,
                    theme = ThemeMode.LIGHT, blur = true, liquidGlass = false)
                ArchiveTheme(settings) {
                    ArchiveApp(catalog.copy(albums = catalog.albums.map { if (it.id == album.id) album else it }),
                        settings, {}, {}, {}, {}, {}, {}, {}, null, {})
                }
            }
        }
        app.onNodeWithTag("tab-INTRODUCTION").performClick()
        return album
    }

    @Test fun committedPredictiveReturnDoesNotFreezeBeforeTheLastFrame() {
        assumeTrue(Build.VERSION.SDK_INT >= 34)
        val album = show()
        val sourceHeight = app.onNodeWithTag("cover-flow-${album.id}").fetchSemanticsNode().boundsInRoot.height
        app.onNodeWithTag("cover-flow-${album.id}").performSemanticsAction(SemanticsActions.OnClick) { it() }
        app.waitUntil(5_000) {
            val bounds = app.onNodeWithTag("album-detail-cover", useUnmergedTree = true).getUnclippedBoundsInRoot()
            (bounds.bottom - bounds.top).value > (bounds.right - bounds.left).value * 1.2f
        }
        app.mainClock.autoAdvance = false
        app.runOnIdle {
            app.activity.onBackPressedDispatcher.dispatchOnBackStarted(BackEventCompat(0f, 200f, 0f, 0))
            app.activity.onBackPressedDispatcher.dispatchOnBackProgressed(BackEventCompat(170f, 210f, .6f, 0))
        }
        app.mainClock.advanceTimeByFrame()
        app.runOnIdle { app.activity.onBackPressedDispatcher.onBackPressed() }
        app.mainClock.advanceTimeByFrame()
        app.waitForIdle()
        app.mainClock.advanceTimeBy(224)
        val firstHeight = app.onNodeWithTag("album-detail-cover-flight", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.height
        assertTrue("Cover must not have reached the square wall endpoint while the exit is still playing",
            firstHeight > sourceHeight + 1f)
        app.mainClock.advanceTimeBy(64)
        val secondHeight = app.onNodeWithTag("album-detail-cover-flight", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.height
        assertTrue("Cover must continue moving during the tail of the return: $firstHeight -> $secondHeight (source $sourceHeight)",
            firstHeight - secondHeight > .5f)
        app.mainClock.advanceTimeBy(200)
        app.onNodeWithTag("album-detail").assertDoesNotExist()
        app.onNodeWithTag("cover-flow-${album.id}").assertIsSelected()
        app.mainClock.autoAdvance = true
    }

    @Test fun displayPanelScalesInAndFinishesItsExitBeforeDisposal() {
        show()
        app.mainClock.autoAdvance = false
        app.onNodeWithTag("cover-flow-display").performSemanticsAction(SemanticsActions.OnClick) { it() }
        app.waitUntil(3_000) {
            app.mainClock.advanceTimeByFrame()
            app.onAllNodesWithTag("cover-flow-display-panel").fetchSemanticsNodes().isNotEmpty()
        }
        val initial = app.onNodeWithTag("cover-flow-display-panel").fetchSemanticsNode().boundsInRoot
        app.mainClock.advanceTimeBy(500)
        val opened = app.onNodeWithTag("cover-flow-display-panel").fetchSemanticsNode().boundsInRoot
        assertTrue("Panel must grow from the toolbar corner", opened.width > initial.width + 2f)
        app.onNodeWithTag("cover-flow-display-done").performSemanticsAction(SemanticsActions.OnClick) { it() }
        app.mainClock.advanceTimeByFrame()
        app.onNodeWithTag("cover-flow-display-panel").assertExists()
        app.mainClock.advanceTimeBy(240)
        app.onNodeWithTag("cover-flow-display-panel").assertDoesNotExist()
        app.onNodeWithTag("cover-flow-display").assertIsDisplayed()
        app.mainClock.autoAdvance = true
    }
}
