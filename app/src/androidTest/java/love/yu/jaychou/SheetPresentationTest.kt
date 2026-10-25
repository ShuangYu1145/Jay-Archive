package love.yu.jaychou

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.screens.albums.ReleaseCategorySheet
import love.yu.jaychou.ui.screens.settings.CustomColorSheet
import love.yu.jaychou.ui.theme.ArchiveTheme
import love.yu.jaychou.ui.theme.DefaultMiuixBlue
import love.yu.jaychou.ui.effects.GlassLayer
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Exercise expanded sheets without writing the user's persisted settings. */
class SheetPresentationTest {
    @get:Rule val app = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Composable private fun Scene(largeFont: Boolean = false, content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides if (largeFont) Density(density.density, 1.4f) else density) {
        ArchiveTheme(AppSettings(theme = ThemeMode.DARK)) {
            GlassLayer(Color.Black) { capture, _ ->
            Box(Modifier.fillMaxSize().then(capture)) {
            Canvas(Modifier.fillMaxSize()) {
                for (x in 0 until size.width.toInt() step 20) {
                    drawRect(if (x % 40 == 0) Color.Blue else Color.Red,
                        topLeft = Offset(x.toFloat(), 0f), size = Size(20f, size.height))
                }
            }
            }
            content()
            }
        }
        }
    }

    private fun assertRaised(title: String) {
        val titleBounds = app.onNodeWithText(title).fetchSemanticsNode().boundsInRoot
        val height = context.resources.displayMetrics.heightPixels
        assertTrue("Sheet must leave visible page space above it: $titleBounds / $height",
            titleBounds.top > height * .15f && titleBounds.top < height * .5f)
        val sheet = app.onNodeWithTag("raised-sheet").fetchSemanticsNode().boundsInRoot
        assertTrue("Sheet must stay below 85% screen height: $sheet / $height", sheet.height < height * .85f)
        val handle = app.onNodeWithTag("sheet-drag-handle").fetchSemanticsNode().boundsInRoot
        val header = app.onNodeWithTag("sheet-header").fetchSemanticsNode().boundsInRoot
        assertEquals("The handle must start at the actual blurred surface edge", sheet.top, handle.top, 1f)
        assertEquals("The title row must follow the handle without a second panel offset", handle.bottom, header.top, 1f)
    }

    private fun screenshot(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        fun rowVariance(y: Int, left: Int = bitmap.width / 5, right: Int = bitmap.width * 4 / 5): Double {
            val values = (left until right step 3).map {
                android.graphics.Color.red(bitmap.getPixel(it, y)).toDouble()
            }
            val mean = values.average()
            return values.map { (it - mean) * (it - mean) }.average()
        }
        val outside = rowVariance((bitmap.height * .15f).toInt())
        val inside = rowVariance((bitmap.height * .9f).toInt())
        assertTrue("The page outside the sheet must retain sharp stripes: $outside", outside > 100)
        assertTrue("Only the sheet surface should blur the stripes: $inside / $outside", inside < outside * .015)
        val sheet = app.onNodeWithTag("raised-sheet").fetchSemanticsNode().boundsInRoot
        val aboveEdge = rowVariance(sheet.top.toInt() - 8)
        val belowEdge = rowVariance(sheet.top.toInt() + 8,
            bitmap.width / 5, bitmap.width * 2 / 5)
        assertTrue("Wallpaper immediately above the panel must remain sharp: $aboveEdge", aboveEdge > 100)
        assertTrue("Blur must begin at the panel edge, not above or below it: $belowEdge / $aboveEdge",
            belowEdge < aboveEdge * .015)
        File(context.getExternalFilesDir(null), name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    @Test fun colorSheetUsesIconsAndAppliesDraft() {
        assumeTrue(android.os.Build.VERSION.SDK_INT >= 33)
        val visible = mutableStateOf(true)
        var applied: Int? = null
        app.setContent { Scene {
            CustomColorSheet(visible.value, null, { visible.value = false }, { applied = it })
        } }
        app.onNodeWithTag("color-cancel").assertIsDisplayed()
        app.onNodeWithContentDescription(AppText.get(R.string.ui_cancel)).assertExists()
        app.onNodeWithContentDescription(AppText.get(R.string.ui_apply)).assertExists()
        assertRaised(AppText.get(R.string.ui_custom_theme_color))
        screenshot("expanded-color-sheet.png")
        app.onNodeWithTag("color-apply").performClick()
        app.waitUntil(5_000) { app.onAllNodesWithTag("color-sheet").fetchSemanticsNodes().isEmpty() }
        assertEquals(DefaultMiuixBlue.toInt(), applied)
    }

    @Test fun categorySheetKeepsSelectionUntilApplyAndSupportsScrolling() {
        assumeTrue(android.os.Build.VERSION.SDK_INT >= 33)
        val visible = mutableStateOf(true)
        val initial = listOf(ReleaseCategory.STUDIO.name)
        val catalog = AlbumRepository.load(context)
        var applied: List<String>? = null
        app.setContent { Scene {
            ReleaseCategorySheet(visible.value, initial, catalog,
                onDismiss = { visible.value = false }, onApply = { applied = it })
        } }
        assertRaised(AppText.get(R.string.ui_album_categories))
        app.onNodeWithTag("category-cancel").assertIsDisplayed()
        app.onNodeWithTag("category-apply").assertIsEnabled()
        app.onNodeWithTag("category-STUDIO").performClick()
        app.onNodeWithTag("category-apply").assertIsNotEnabled()
        app.onNodeWithTag("category-sheet").performScrollToNode(hasTestTag("category-OTHER"))
        app.onNodeWithTag("category-OTHER").performClick()
        app.onNodeWithTag("category-apply").assertIsEnabled()
        screenshot("expanded-category-sheet.png")
        app.onNodeWithTag("category-apply").performClick()
        app.waitUntil(5_000) { app.onAllNodesWithTag("category-sheet").fetchSemanticsNodes().isEmpty() }
        assertEquals(listOf(ReleaseCategory.OTHER.name), applied)
    }

    @Test fun draggingTheHandleMovesTheSameSurfaceAndCancelsWithoutApplying() {
        assumeTrue(android.os.Build.VERSION.SDK_INT >= 33)
        val visible = mutableStateOf(true)
        var applied = false
        app.setContent { Scene {
            CustomColorSheet(visible.value, null, { visible.value = false }, { applied = true })
        } }
        assertRaised(AppText.get(R.string.ui_custom_theme_color))
        app.mainClock.autoAdvance = false
        app.onNodeWithTag("sheet-drag-handle").performTouchInput {
            down(center)
            moveBy(Offset(0f, 80f))
        }
        app.mainClock.advanceTimeByFrame()
        val surface = app.onNodeWithTag("raised-sheet").fetchSemanticsNode().boundsInRoot
        val handle = app.onNodeWithTag("sheet-drag-handle").fetchSemanticsNode().boundsInRoot
        assertEquals("Blur surface and handle must stay attached during drag", surface.top, handle.top, 1f)
        app.onNodeWithTag("sheet-drag-handle").performTouchInput {
            moveBy(Offset(0f, 900f))
            up()
        }
        app.mainClock.autoAdvance = true
        app.waitUntil(5_000) { app.onAllNodesWithTag("color-sheet").fetchSemanticsNodes().isEmpty() }
        assertFalse(applied)
    }

    @Test fun largerSystemFontDoesNotSeparateTheBlurFromThePanel() {
        assumeTrue(android.os.Build.VERSION.SDK_INT >= 33)
        val catalog = AlbumRepository.load(context)
        app.setContent { Scene(largeFont = true) {
            ReleaseCategorySheet(true, listOf(ReleaseCategory.STUDIO.name), catalog, {}, {})
        } }
        assertRaised(AppText.get(R.string.ui_album_categories))
        screenshot("category-sheet-large-font.png")
    }
}
