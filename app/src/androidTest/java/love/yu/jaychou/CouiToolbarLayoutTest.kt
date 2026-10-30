package love.yu.jaychou

import android.graphics.Bitmap
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import love.yu.jaychou.data.*
import love.yu.jaychou.navigation.ArchiveApp
import love.yu.jaychou.ui.screens.albums.AlbumDetailScreen
import love.yu.jaychou.ui.theme.ArchiveTheme
import love.yu.jaychou.ui.components.LocalArchiveBlur
import love.yu.jaychou.ui.effects.HeaderGlassLayer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CouiToolbarLayoutTest {
    @get:Rule val app = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun introductionHeaderStaysFixedDuringScrollAndSwipe() {
        AppText.update(context.resources)
        val catalog = AlbumRepository.load(context)
        var settings by mutableStateOf(AppSettings(uiStyle = UiStyle.COUI, blur = true,
            liquidGlass = false, floatingBottomBar = true, theme = ThemeMode.LIGHT))
        app.mainClock.autoAdvance = false
        app.setContent {
            ArchiveTheme(settings) { ArchiveApp(catalog, settings, {}, {}, {}, {}, {}, {}, {}, null, {}) }
        }
        settle()
        app.onNodeWithTag("tab-INTRODUCTION").performClick()
        settle()
        val header = app.onNodeWithTag("introduction-header")
        val original = header.fetchSemanticsNode().boundsInRoot
        val grid = app.onNodeWithTag("albums-grid")
        grid.performTouchInput { swipeUp() }
        settle()
        assertTrue(grid.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value() > 0f)
        assertEquals(original, header.fetchSemanticsNode().boundsInRoot)
        app.onNodeWithTag("album-search-open").assertIsDisplayed()
        capture("introduction-scrolled")
        val pager = app.onNodeWithTag("introduction-pager")
        pager.performTouchInput {
            down(Offset(width * .8f, height * .6f))
            moveTo(Offset(width * .4f, height * .6f), 600)
        }
        app.mainClock.advanceTimeBy(100)
        app.waitForIdle()
        assertTrue(app.onNodeWithTag("page-ALBUMS").fetchSemanticsNode().positionInRoot.x < -10f)
        assertEquals(original, header.fetchSemanticsNode().boundsInRoot)
        pager.performTouchInput { cancel() }
        settle()
        app.onNodeWithTag("tab-CONCERTS").performClick()
        settle()
        app.onNodeWithTag("concerts-refresh").assertIsDisplayed()
        app.onNodeWithTag("album-search-open").assertDoesNotExist()
        app.runOnIdle { settings = settings.copy(theme = ThemeMode.DARK) }
        settle()
        capture("introduction-dark")
        app.runOnIdle { settings = settings.copy(blur = false) }
        settle()
        app.onNodeWithTag("tab-ALBUMS").performClick()
        settle()
        capture("introduction-blur-off")
        app.onNodeWithTag("album-search-open").assertIsDisplayed()
        assertEquals(original, header.fetchSemanticsNode().boundsInRoot)
        app.mainClock.autoAdvance = true
    }

    @Test fun albumBackButtonFitsToolbarAndRemainsClickable() {
        AppText.update(context.resources)
        val album = AlbumRepository.load(context).albums.first()
        var backs = 0
        app.setContent {
            ArchiveTheme(AppSettings(uiStyle = UiStyle.COUI, blur = true, liquidGlass = false)) {
                CompositionLocalProvider(LocalArchiveBlur provides true) {
                    AlbumDetailScreen(album, rememberLazyListState(), 24.dp, { backs++ }, { _, _ -> })
                }
            }
        }
        val back = app.onNodeWithContentDescription(AppText.get(R.string.ui_back_to_albums))
        back.assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
        val button = back.getUnclippedBoundsInRoot()
        val toolbar = app.onNodeWithTag("top-bar-${AppText.get(R.string.ui_album_archive)}").getUnclippedBoundsInRoot()
        assertTrue(button.top >= toolbar.top && button.bottom <= toolbar.bottom)
        val image = app.onRoot().captureToImage().toPixelMap()
        val bounds = back.fetchSemanticsNode().boundsInRoot
        val barBounds = app.onNodeWithTag("top-bar-${AppText.get(R.string.ui_album_archive)}")
            .fetchSemanticsNode().boundsInRoot
        val y = barBounds.bottom.toInt() + 4
        val shadow = image[bounds.center.x.toInt(), y].red
        val background = image[image.width - 8, y].red
        assertTrue("Button shadow must continue below the toolbar: $shadow / $background",
            background - shadow > .01f)
        capture("album-detail")
        back.performClick()
        app.runOnIdle { assertEquals(1, backs) }
    }

    @Test fun progressiveBackdropFadesBeyondToolbarWithoutASeam() {
        org.junit.Assume.assumeTrue(Build.VERSION.SDK_INT >= 33)
        app.setContent {
            ArchiveTheme(AppSettings(uiStyle = UiStyle.COUI, theme = ThemeMode.LIGHT)) {
                HeaderGlassLayer(Color.White, progressive = true, content = { capture, header ->
                    Box(Modifier.fillMaxSize()) {
                        Canvas(Modifier.fillMaxSize().then(capture)) {
                            for (x in 0 until size.width.toInt() step 12) {
                                drawRect(if (x / 12 % 2 == 0) Color.Black else Color.White,
                                    topLeft = Offset(x.toFloat(), 0f),
                                    size = androidx.compose.ui.geometry.Size(12f, size.height))
                            }
                        }
                        Spacer(header.fillMaxWidth().height(72.dp).testTag("fade-header"))
                    }
                })
            }
        }
        val image = app.onRoot().captureToImage().toPixelMap()
        val bottom = app.onNodeWithTag("fade-header").fetchSemanticsNode().boundsInRoot.bottom.toInt()
        val density = bottom / 72f
        val referenceY = bottom + (40 * density).toInt()
        fun difference(y: Int): Double = (24 until image.width - 24).map { x ->
            kotlin.math.abs(image[x, y].red - image[x, referenceY].red).toDouble()
        }.average()
        assertTrue("Blur must extend past the old toolbar edge", difference(bottom + 2) > .02)
        assertTrue("Blur must fade back to the original content", difference(referenceY - 2) < .005)
        val largestStep = (bottom - 4 until referenceY).maxOf { y ->
            kotlin.math.abs(difference(y + 1) - difference(y))
        }
        assertTrue("No abrupt horizontal seam: $largestStep", largestStep < .04)
        capture("progressive-fade-stripes")
    }

    private fun settle() { app.mainClock.advanceTimeBy(1500); app.waitForIdle() }

    private fun capture(name: String) {
        val dir = File(context.filesDir, "coui-toolbar-previews").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use {
            app.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
