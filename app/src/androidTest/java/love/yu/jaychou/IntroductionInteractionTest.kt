package love.yu.jaychou

import love.yu.jaychou.ui.effects.liquid.lens
import love.yu.jaychou.ui.theme.ArchiveTheme
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.runtime.mutableStateOf
import java.io.File
import love.yu.jaychou.data.*
import love.yu.jaychou.navigation.ArchiveApp
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class IntroductionInteractionTest {
    @get:Rule val app = createComposeRule()

    @Test fun listsScrollBothWaysWithBlurDisabled() = verifyInteractions(false)

    @Test fun sectionTabClicksAnimateBothDirectionsInBothStyles() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val catalog = AlbumRepository.load(context)
        val settings = mutableStateOf(AppSettings(blur = false, liquidGlass = false))
        app.mainClock.autoAdvance = false
        try {
            app.setContent {
                ArchiveTheme(settings.value) {
                    ArchiveApp(catalog, settings.value, {}, {}, {}, {}, {}, {}, {}, null, {})
                }
            }
            settle()
            app.onNodeWithTag("tab-INTRODUCTION").performClick()
            settle()
            for (style in UiStyle.entries) {
                app.runOnIdle { settings.value = settings.value.copy(uiStyle = style) }
                settle()
                val host = app.onNodeWithTag("introduction-pager").getUnclippedBoundsInRoot()
                app.onNodeWithTag("tab-CONCERTS").performClick()
                app.mainClock.advanceTimeBy(96)
                val incoming = app.onNodeWithTag("page-CONCERTS").getUnclippedBoundsInRoot()
                assertTrue("Concerts must slide in from the right in $style",
                    incoming.left > host.left && incoming.left < host.right)
                assertTrue("Section transition stays horizontal", incoming.top == host.top)
                settle()
                app.onNodeWithTag("tab-CONCERTS").assertIsSelected()
                app.onNodeWithTag("tab-ALBUMS").performClick()
                app.mainClock.advanceTimeBy(96)
                val returning = app.onNodeWithTag("page-ALBUMS").getUnclippedBoundsInRoot()
                assertTrue("Albums must slide back from the left in $style", returning.left < host.left)
                settle()
                app.onNodeWithTag("tab-ALBUMS").assertIsSelected()
            }
        } finally {
            app.mainClock.autoAdvance = true
        }
    }

    @Test fun liquidTopBarPressDragCancelAndListsScrollBothWays() {
        assumeTrue(Build.VERSION.SDK_INT >= 33)
        verifyInteractions(true)
    }

    @Test fun sectionActionsStayFixedDuringPageSwipe() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val catalog = AlbumRepository.load(context)
        val settings = AppSettings(blur = true, liquidGlass = false, userName = "测试用户")
        app.mainClock.autoAdvance = false
        app.setContent {
            ArchiveTheme(settings) { ArchiveApp(catalog, settings, {}, {}, {}, {}, {}, {}, {}, null, {}) }
        }
        settle()
        app.onNodeWithTag("tab-INTRODUCTION").performClick()
        settle()
        val actions = app.onNodeWithTag("introduction-actions")
        val tabs = app.onNodeWithTag("introduction-tabs")
        val original = actions.fetchSemanticsNode().boundsInRoot
        val pager = app.onNodeWithTag("introduction-pager")
        pager.performTouchInput {
            down(Offset(width * .8f, height * .6f))
            moveTo(Offset(width * .5f, height * .6f), 600)
        }
        app.mainClock.advanceTimeBy(100)
        app.waitForIdle()
        val movedPage = app.onNodeWithTag("page-ALBUMS").fetchSemanticsNode().positionInRoot
        assertTrue("The page must actually move during this check", movedPage.x < -10f)
        val during = actions.fetchSemanticsNode().boundsInRoot
        assertTrue("Actions stay at their original position", kotlin.math.abs(during.left - original.left) < 1f)
        assertTrue("Actions never enter the tabs", during.left > tabs.fetchSemanticsNode().boundsInRoot.right)
        screenshot("header-during-page-swipe.png")
        pager.performTouchInput { cancel() }
        settle()
        app.onNodeWithTag("tab-CONCERTS").performClick()
        settle()
        val refresh = app.onNodeWithTag("concerts-refresh").fetchSemanticsNode().boundsInRoot
        val surface = actions.fetchSemanticsNode().boundsInRoot
        assertTrue("Refresh stays centered in its surface", (refresh.center - surface.center).getDistance() < 1f)
        app.onNodeWithTag("album-search-open").assertDoesNotExist()
        screenshot("header-concert-refresh.png")
        app.onNodeWithTag("tab-ALBUMS").performClick()
        settle()
        app.onNodeWithTag("concerts-refresh").assertDoesNotExist()
        app.onNodeWithTag("album-search-open").assertIsDisplayed()
        app.onNodeWithTag("release-filter").performClick()
        settle()
        // The sheet has its own Dialog composition and can start on the next frame.
        app.mainClock.advanceTimeBy(500)
        app.waitForIdle()
        app.onNodeWithTag("category-sheet").assertIsDisplayed()
    }

    private fun verifyInteractions(blur: Boolean) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val catalog = AlbumRepository.load(context)
        val settings = AppSettings(blur = blur, liquidGlass = blur, userName = "测试用户")
        app.mainClock.autoAdvance = false
        app.setContent {
            ArchiveTheme(settings) { ArchiveApp(catalog, settings, {}, {}, {}, {}, {}, {}, {}, null, {}) }
        }
        settle()
        val profile = app.onNodeWithTag("home-profile").fetchSemanticsNode().boundsInRoot
        val viewport = app.onNodeWithTag("root-pager").fetchSemanticsNode().boundsInRoot
        assertTrue("Profile is at the right of the home header", profile.center.x > viewport.width * .65f)
        app.onNodeWithTag("tab-INTRODUCTION").performClick()
        settle()
        verifyScroll("albums-grid")
        val top = app.onNodeWithTag("introduction-tabs")
        val indicator = app.onNodeWithTag("introduction-tabs-indicator")
        val albumLeft = app.onNodeWithTag("page-ALBUMS").fetchSemanticsNode().boundsInRoot.left
        fun assertContentStill() {
            val left = app.onNodeWithTag("page-ALBUMS").fetchSemanticsNode().boundsInRoot.left
            assertTrue("Dragging the capsule must not move the album page", kotlin.math.abs(left - albumLeft) < 1f)
            app.onNodeWithTag("tab-INTRODUCTION").assertIsSelected()
            app.onNodeWithTag("tab-ALBUMS").assertIsSelected()
        }
        if (blur) {
            val initialWidth = indicator.fetchSemanticsNode().boundsInRoot.width
            top.performTouchInput { down(Offset(width / 6f, centerY)) }
            app.mainClock.advanceTimeBy(600)
            app.waitForIdle()
            assertTrue("Top lens expands on press like the bottom lens",
                indicator.fetchSemanticsNode().boundsInRoot.width > initialWidth * 1.2f)
            screenshot("top-liquid-pressed.png")
            top.performTouchInput { moveTo(Offset(width * .5f, centerY), 500) }
            app.mainClock.advanceTimeBy(500)
            assertContentStill()
            top.performTouchInput { up() }
        } else {
            top.performTouchInput {
                down(Offset(width / 6f, centerY))
                moveTo(Offset(width * .5f, centerY), 500)
            }
            app.mainClock.advanceTimeBy(500)
            app.waitForIdle()
            assertContentStill()
            top.performTouchInput { up() }
        }
        app.mainClock.advanceTimeBy(32)
        app.waitForIdle()
        val concertLeft = app.onNodeWithTag("page-CONCERTS").fetchSemanticsNode().boundsInRoot.left
        assertTrue("Release starts an animated horizontal page transition", concertLeft > albumLeft + 1f)
        settle()
        app.onNodeWithTag("tab-CONCERTS").assertIsSelected()
        app.onNodeWithTag("tab-INTRODUCTION").assertIsSelected()
        app.waitUntil(20_000) { app.onAllNodesWithTag("concert-featured").fetchSemanticsNodes().isNotEmpty() }
        verifyScroll("concerts-list")
        screenshot(if (blur) "concerts-liquid.png" else "concerts-solid.png")
        top.performTouchInput {
            down(Offset(width * .5f, centerY))
            moveTo(Offset(width / 6f, centerY), 500)
            cancel()
        }
        settle()
        app.onNodeWithTag("tab-CONCERTS").assertIsSelected()
        // A drag from an unselected segment must not reach the outer root pager either.
        top.performTouchInput {
            down(Offset(width / 6f, centerY))
            moveTo(Offset(width * .5f, centerY), 500)
        }
        app.mainClock.advanceTimeBy(300)
        app.waitForIdle()
        val rootLeft = app.onNodeWithTag("page-INTRODUCTION").fetchSemanticsNode().boundsInRoot.left
        val pagerLeft = app.onNodeWithTag("root-pager").fetchSemanticsNode().boundsInRoot.left
        assertTrue("Capsule gestures must not move the root page", kotlin.math.abs(rootLeft - pagerLeft) < 1f)
        top.performTouchInput { cancel() }
        settle()
        app.onNodeWithTag("tab-ALBUMS").performClick()
        settle()
        verifyScroll("albums-grid")
        screenshot(if (blur) "albums-liquid.png" else "albums-solid.png")
    }

    private fun verifyScroll(tag: String) {
        val list = app.onNodeWithTag(tag)
        fun position() = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        val before = position()
        list.performTouchInput { swipe(Offset(centerX, height * .7f), Offset(centerX, height * .28f), 700) }
        settle()
        val lower = position()
        assertTrue("$tag must scroll up after a real finger drag: $before -> $lower", lower > before + .2f)
        list.performTouchInput { swipe(Offset(centerX, height * .3f), Offset(centerX, height * .7f), 700) }
        settle()
        assertTrue("$tag must scroll down after a real finger drag", position() < lower - .2f)
        app.onNodeWithTag("tab-INTRODUCTION").assertIsSelected()
    }

    private fun settle() {
        app.mainClock.advanceTimeBy(1_500)
        app.waitForIdle()
    }

    private fun screenshot(name: String) {
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "introduction-fixes").apply { mkdirs() }
        File(dir, name).outputStream().use {
            app.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
