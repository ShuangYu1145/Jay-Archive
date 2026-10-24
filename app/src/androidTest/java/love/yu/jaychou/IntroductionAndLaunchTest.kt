package love.yu.jaychou

import love.yu.jaychou.ui.animation.JayLaunchAnimation
import love.yu.jaychou.ui.theme.ArchiveTheme
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import love.yu.jaychou.data.*
import love.yu.jaychou.navigation.ArchiveApp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class IntroductionAndLaunchTest {
    @get:Rule val app = createComposeRule()

    @Test fun handwritingWaitsForFirstFrame() {
        val ready = mutableStateOf(false)
        app.mainClock.autoAdvance = false
        app.setContent {
            ArchiveTheme(AppSettings(blur = false)) {
                JayLaunchAnimation(true, ready = ready.value) { Box(Modifier.fillMaxSize().testTag("launch-content")) }
            }
        }
        app.mainClock.advanceTimeBy(500)
        app.onNodeWithTag("jay-launch-animation").assertExists()
        app.onNodeWithTag("jay-launch-icon").assertIsDisplayed()
        app.onNodeWithTag("launch-content").assertDoesNotExist()
        app.runOnIdle { ready.value = true }
        app.mainClock.advanceTimeBy(700)
        app.onNodeWithTag("jay-launch-animation").assertExists()
        app.mainClock.advanceTimeBy(3_000)
        app.onNodeWithTag("jay-launch-animation").assertDoesNotExist()
        app.onNodeWithTag("launch-content").assertExists()
    }

    @Test fun missingStartSignalStillFinishesLaunchAndShowsContent() {
        app.mainClock.autoAdvance = false
        app.setContent {
            ArchiveTheme(AppSettings(blur = false)) {
                JayLaunchAnimation(true, ready = false) { Box(Modifier.fillMaxSize().testTag("launch-content")) }
            }
        }
        app.mainClock.advanceTimeBy(500)
        app.onNodeWithTag("jay-launch-icon").assertIsDisplayed()
        app.mainClock.advanceTimeBy(5_000)
        app.onNodeWithTag("jay-launch-animation").assertDoesNotExist()
        app.onNodeWithTag("launch-content").assertExists()
    }

    @Test fun handwritingProgressesThenDissolvesAndDoesNotReplayAfterRestoration() {
        val restoration = StateRestorationTester(app)
        app.mainClock.autoAdvance = false
        restoration.setContent {
            ArchiveTheme(AppSettings(blur = false)) {
                JayLaunchAnimation(true) { Box(Modifier.fillMaxSize().testTag("launch-content")) }
            }
        }
        app.mainClock.advanceTimeBy(32)
        app.onNodeWithTag("jay-launch-animation").assertExists()
        app.onNodeWithTag("launch-content").assertDoesNotExist()
        app.mainClock.advanceTimeBy(650)
        screenshot("jay-writing-middle.png")
        app.mainClock.advanceTimeBy(1_050)
        screenshot("jay-writing-complete.png")
        app.onNodeWithTag("jay-launch-animation").assertExists()
        app.mainClock.advanceTimeBy(400)
        screenshot("jay-dissolving.png")
        app.mainClock.advanceTimeBy(800)
        app.onNodeWithTag("jay-launch-animation").assertDoesNotExist()
        app.onNodeWithTag("launch-content").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        app.onNodeWithTag("jay-launch-animation").assertDoesNotExist()
        app.onNodeWithTag("launch-content").assertExists()
    }

    @Test fun introductionSwitchesSectionsAndRestoresAlbumPositionAndSelectedSection() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val catalog = AlbumRepository.load(context)
        val settings = AppSettings(blur = false)
        val restoration = StateRestorationTester(app)
        app.mainClock.autoAdvance = false
        restoration.setContent {
            ArchiveTheme(settings) { ArchiveApp(catalog, settings, {}, {}, {}, {}, {}, {}, {}, null, {}) }
        }
        settle()
        app.onNodeWithTag("tab-INTRODUCTION").performClick()
        settle()
        app.onNodeWithTag("tab-ALBUMS").assertIsSelected()
        app.onAllNodes(hasAnyAncestor(hasTestTag("floating-navigation")) and hasTestTag("tab-CONCERTS"))
            .assertCountEquals(0)
        app.onNodeWithTag("albums-grid").performScrollToIndex(8)
        val before = albumPosition()
        screenshot("introduction-albums.png")
        app.onNodeWithTag("tab-CONCERTS").performClick()
        settle()
        app.onNodeWithTag("tab-CONCERTS").assertIsSelected()
        app.onNodeWithTag("concerts-list").assertIsDisplayed()
        screenshot("introduction-concerts.png")
        app.onNodeWithTag("tab-SETTINGS").performClick()
        settle()
        app.onNodeWithTag("tab-INTRODUCTION").performClick()
        settle()
        app.onNodeWithTag("tab-CONCERTS").assertIsSelected()
        restoration.emulateSavedInstanceStateRestore()
        settle()
        app.onNodeWithTag("tab-CONCERTS").assertIsSelected()
        app.onNodeWithTag("tab-ALBUMS").performClick()
        settle()
        assertTrue("Section switching preserves the album list", kotlin.math.abs(before - albumPosition()) < .001f)
    }

    private fun albumPosition() = app.onNodeWithTag("albums-grid").fetchSemanticsNode()
        .config[SemanticsProperties.VerticalScrollAxisRange].value()

    private fun settle() {
        app.mainClock.advanceTimeBy(1_500)
        app.waitForIdle()
    }

    private fun screenshot(name: String) {
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "introduction-launch").apply { mkdirs() }
        File(dir, name).outputStream().use {
            app.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
