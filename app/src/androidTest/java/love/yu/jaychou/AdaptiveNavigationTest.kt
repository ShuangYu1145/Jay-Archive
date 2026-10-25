package love.yu.jaychou

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import love.yu.jaychou.data.*
import love.yu.jaychou.navigation.ArchiveApp
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import android.graphics.Bitmap
import java.io.File

@OptIn(ExperimentalTestApi::class)
class AdaptiveNavigationTest {
    @get:Rule val app = createComposeRule()

    @Test fun nativeRailsResizeWithoutLosingPageAndLeaveRoomForContent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val catalog = AlbumRepository.load(context)
        var settings by mutableStateOf(AppSettings(blur = false, liquidGlass = false))
        var size by mutableStateOf(DpSize(400.dp, 800.dp))
        app.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size)) {
                ArchiveTheme(settings) {
                    ArchiveApp(catalog, settings, {}, {}, {}, {}, {}, {}, {}, null, {})
                }
            }
        }
        for (style in UiStyle.entries) {
            app.runOnIdle { settings = settings.copy(uiStyle = style) }
            app.onNodeWithTag("bottom-navigation").assertIsDisplayed()
            app.onNodeWithTag("side-navigation").assertDoesNotExist()
            app.onNodeWithTag("tab-ABOUT").performClick()
            app.onNodeWithTag("tab-ABOUT").assertIsSelected()

            // Tablet landscape, tablet portrait, and a phone landscape below 600dp.
            for (viewport in listOf(DpSize(1200.dp, 800.dp), DpSize(800.dp, 1200.dp), DpSize(560.dp, 360.dp))) {
                app.runOnIdle { size = viewport }
                app.onNodeWithTag("bottom-navigation").assertDoesNotExist()
                val nativeTag = if (style == UiStyle.MIUIX) "miuix-navigation-rail" else "material-navigation-rail"
                app.onNodeWithTag(nativeTag).assertIsDisplayed()
                app.onNodeWithTag("tab-ABOUT").assertIsSelected()
                if (viewport.width == 1200.dp) {
                    app.onNodeWithTag("root-pager").performTouchInput { swipeDown() }
                    app.onNodeWithTag("tab-SETTINGS").assertIsSelected()
                    app.mainClock.autoAdvance = false
                    try {
                        app.onNodeWithTag("tab-ABOUT").performScrollTo().performClick()
                        app.mainClock.advanceTimeBy(96)
                        val page = app.onNodeWithTag("page-ABOUT").getUnclippedBoundsInRoot()
                        val host = app.onNodeWithTag("root-pager").getUnclippedBoundsInRoot()
                        assertTrue("Rail navigation must slide pages vertically", page.top > host.top)
                        assertTrue("Rail navigation must not slide pages horizontally",
                            kotlin.math.abs((page.left - host.left).value) < 1f)
                    } finally {
                        app.mainClock.autoAdvance = true
                    }
                }
                app.onNodeWithTag("tab-INTRODUCTION").performScrollTo().performClick()
                val rail = app.onNodeWithTag("side-navigation").fetchSemanticsNode().boundsInRoot
                val content = app.onNodeWithTag("albums-grid").fetchSemanticsNode().boundsInRoot
                assertTrue("Content must not overlap the rail", content.left >= rail.right - 1f)
                if (viewport.width == 1200.dp) {
                    val bitmap = app.onRoot().captureToImage().asAndroidBitmap()
                    val folder = File(context.getExternalFilesDir(null), "adaptive-navigation").apply { mkdirs() }
                    File(folder, "${style.name}-tablet.png").outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                }
                app.onNodeWithTag("tab-ABOUT").performScrollTo().performClick()
            }
            app.runOnIdle { size = DpSize(400.dp, 800.dp) }
            app.onNodeWithTag("bottom-navigation").assertIsDisplayed()
            app.onNodeWithTag("tab-ABOUT").assertIsSelected()
        }
    }

    @Test fun tabletGridUsesMoreColumnsAndDetailsHideTheRail() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val catalog = AlbumRepository.load(context)
        val settings = AppSettings(blur = false, liquidGlass = false, albumLayout = AlbumLayout.GRID)
        app.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(1200.dp, 800.dp))) {
                ArchiveTheme(settings) {
                    ArchiveApp(catalog, settings, {}, {}, {}, {}, {}, {}, {}, null, {})
                }
            }
        }
        app.onNodeWithTag("tab-INTRODUCTION").performClick()
        val albums = catalog.albums.filter { it.category.name in settings.albumCategories }
        val first = app.onNodeWithTag("album-${albums[0].id}")
        val third = app.onNodeWithTag("album-${albums[2].id}")
        assertTrue("Tablet grid should fit at least three albums per row",
            kotlin.math.abs(first.fetchSemanticsNode().boundsInRoot.top - third.fetchSemanticsNode().boundsInRoot.top) <= 1f)
        first.performClick()
        app.onNodeWithTag("album-detail").assertIsDisplayed()
        app.onNodeWithTag("side-navigation").assertDoesNotExist()
        app.onNodeWithContentDescription(AppText.get(R.string.ui_back_to_albums)).performClick()
        app.onNodeWithTag("side-navigation").assertIsDisplayed()
        app.onNodeWithTag("tab-INTRODUCTION").assertIsSelected()
    }
}
