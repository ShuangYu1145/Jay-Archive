package love.yu.jaychou

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import love.yu.jaychou.data.*
import love.yu.jaychou.navigation.ArchiveApp
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** COUI must re-flow for landscape without losing the selected page or scroll position. */
@OptIn(ExperimentalTestApi::class)
class CouiLandscapeTest {
    @get:Rule val app = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun show(size: () -> DpSize, settings: () -> AppSettings) {
        val catalog = AlbumRepository.load(context)
        app.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size())) {
                ArchiveTheme(settings()) {
                    ArchiveApp(catalog, settings(), {}, {}, {}, {}, {}, {}, {}, null, {})
                }
            }
        }
    }

    private fun bounds(tag: String, unmerged: Boolean = false) =
        app.onNodeWithTag(tag, useUnmergedTree = unmerged).getUnclippedBoundsInRoot()

    @Test fun phoneLandscapeCompactsHeaderAndKeepsContentInsideTheRail() {
        AppText.update(context.resources)
        var size by mutableStateOf(DpSize(400.dp, 800.dp))
        val settings = AppSettings(uiStyle = UiStyle.COUI, blur = false, liquidGlass = false, theme = ThemeMode.LIGHT)
        show({ size }, { settings })

        app.onNodeWithTag("tab-INTRODUCTION").performClick()
        val portraitHeader = bounds("introduction-header")
        val portraitHeight = portraitHeader.bottom - portraitHeader.top

        app.runOnIdle { size = DpSize(560.dp, 360.dp) }
        app.waitForIdle()

        app.onNodeWithTag("bottom-navigation").assertDoesNotExist()
        app.onNodeWithTag("coui-navigation-rail").assertIsDisplayed()
        app.onNodeWithTag("tab-INTRODUCTION").assertIsSelected()
        val header = bounds("introduction-header")
        val actions = bounds("introduction-actions")
        val rail = bounds("side-navigation")
        val grid = bounds("albums-grid")

        assertTrue("Landscape header must be shorter than the portrait header",
            (header.bottom - header.top).value < portraitHeight.value)
        assertTrue("Header actions must stay inside the header", actions.bottom <= header.bottom)
        assertTrue("Header must not be clipped by the top edge", header.top.value >= 0f)
        assertTrue("Grid must start after the rail", grid.left >= rail.right - 1.dp)
        assertTrue("Grid must not be clipped on the right", grid.right.value <= 561f)

        capture("coui-phone-landscape-introduction")

        // A 560dp-wide phone fits two album columns. Only the first visible row is composed, so
        // compare the composed leading cards instead of assuming repository order is grid order.
        val albums = AlbumRepository.load(context).albums.filter { it.category.name in settings.albumCategories }
        val visible = albums.mapNotNull { album ->
            app.onAllNodesWithTag("album-${album.id}").fetchSemanticsNodes().firstOrNull()?.boundsInRoot
        }.sortedWith(compareBy({ it.top }, { it.left }))
        assertTrue("At least two landscape album cards should be composed", visible.size >= 2)
        assertTrue("First two albums should share a landscape row",
            kotlin.math.abs(visible[0].top - visible[1].top) <= 1f)
    }

    @Test fun aboutBannerBecomesAShortStripInLandscape() {
        AppText.update(context.resources)
        var size by mutableStateOf(DpSize(400.dp, 800.dp))
        val settings = AppSettings(uiStyle = UiStyle.COUI, blur = false, liquidGlass = false, theme = ThemeMode.LIGHT)
        show({ size }, { settings })

        app.onNodeWithTag("tab-SETTINGS").performClick()
        app.onNodeWithTag("about-settings").performScrollTo().performClick()
        val portrait = bounds("about-identity")
        assertTrue("Portrait keeps the tall identity card", (portrait.bottom - portrait.top).value >= 300f)

        app.runOnIdle { size = DpSize(800.dp, 360.dp) }
        app.waitForIdle()

        val landscape = bounds("about-identity")
        val check = bounds("check-updates", unmerged = true)
        app.onNodeWithTag("side-navigation").assertDoesNotExist()
        val list = bounds("about-list")
        assertTrue("Landscape identity should be a strip, was ${(landscape.bottom - landscape.top).value}",
            (landscape.bottom - landscape.top).value <= 240f)
        assertTrue("Update pill must stay inside the banner", check.bottom <= landscape.bottom)
        assertTrue("About list stays inside the window", list.left >= 0.dp)
        capture("coui-phone-landscape-about")
    }

    @Test fun rotationKeepsSelectionAndScrollPosition() {
        AppText.update(context.resources)
        var size by mutableStateOf(DpSize(400.dp, 800.dp))
        val settings = AppSettings(uiStyle = UiStyle.COUI, blur = false, liquidGlass = false, theme = ThemeMode.LIGHT)
        show({ size }, { settings })

        app.onNodeWithTag("tab-SETTINGS").performClick()
        app.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("app-language"))
        app.onNodeWithTag("app-language").assertIsDisplayed()

        app.runOnIdle { size = DpSize(800.dp, 360.dp) }
        app.waitForIdle()
        app.onNodeWithTag("tab-SETTINGS").assertIsSelected()
        app.onNodeWithTag("app-language").assertIsDisplayed()
        val landscapeList = bounds("settings-list")
        val rail = bounds("side-navigation")
        assertTrue(landscapeList.left >= rail.right - 1.dp)

        app.runOnIdle { size = DpSize(400.dp, 800.dp) }
        app.waitForIdle()
        app.onNodeWithTag("bottom-navigation").assertIsDisplayed()
        app.onNodeWithTag("tab-SETTINGS").assertIsSelected()
        app.onNodeWithTag("app-language").assertIsDisplayed()
    }

    @Test fun albumDetailSurvivesRotationAndRestoresTheRailOnBack() {
        AppText.update(context.resources)
        var size by mutableStateOf(DpSize(400.dp, 800.dp))
        val settings = AppSettings(uiStyle = UiStyle.COUI, blur = false, liquidGlass = false, theme = ThemeMode.LIGHT)
        val catalog = AlbumRepository.load(context)
        show({ size }, { settings })

        app.onNodeWithTag("tab-INTRODUCTION").performClick()
        app.onNodeWithTag("album-${catalog.albums.first().id}").performClick()
        app.onNodeWithTag("album-detail").assertIsDisplayed()

        app.runOnIdle { size = DpSize(800.dp, 360.dp) }
        app.waitForIdle()
        app.onNodeWithTag("album-detail").assertIsDisplayed()
        app.onNodeWithTag("side-navigation").assertDoesNotExist()
        val detail = bounds("album-detail-list")
        val cover = bounds("album-detail-cover", unmerged = true)
        assertTrue("Landscape detail must not be clipped", detail.right.value <= 800f)
        assertTrue("Landscape cover should be height-sized, was ${(cover.right - cover.left).value}",
            (cover.right - cover.left).value <= 301f)
        capture("coui-phone-landscape-detail")

        app.onNodeWithContentDescription(AppText.get(R.string.ui_back_to_albums)).performClick()
        app.waitForIdle()
        app.onNodeWithTag("side-navigation").assertIsDisplayed()
        app.onNodeWithTag("tab-INTRODUCTION").assertIsSelected()
    }

    @Test fun expandedLandscapeCapsReadingColumnsAndUsesTheWideRail() {
        AppText.update(context.resources)
        var size by mutableStateOf(DpSize(400.dp, 800.dp))
        val settings = AppSettings(uiStyle = UiStyle.COUI, blur = false, liquidGlass = false, theme = ThemeMode.LIGHT)
        show({ size }, { settings })
        app.onNodeWithTag("coui-compact-title").assertDoesNotExist()

        app.runOnIdle { size = DpSize(1200.dp, 800.dp) }
        app.waitForIdle()

        val rail = bounds("side-navigation")
        assertTrue("Expanded COUI rail should grow past the compact width", (rail.right - rail.left).value >= 80f)

        app.onNodeWithTag("tab-SETTINGS").performClick()
        val settingsList = bounds("settings-list")
        assertTrue("Settings rows must not stretch across a tablet: ${(settingsList.right - settingsList.left).value}",
            (settingsList.right - settingsList.left).value <= 721f)
        val settingsCenter = (settingsList.left + settingsList.right) / 2
        val contentCenter = (rail.right + 1200.dp) / 2
        assertTrue("Capped settings column should be centred: list=${settingsList.left.value}..${settingsList.right.value}, rail=${rail.right.value}, delta=${(settingsCenter - contentCenter).value}",
            kotlin.math.abs((settingsCenter - contentCenter).value) < 2f)

        app.onNodeWithTag("tab-SETTINGS").performClick()
        app.onNodeWithTag("about-settings").performScrollTo().performClick()
        val about = bounds("about-identity")
        assertTrue("About banner must stay short on a wide screen: ${(about.bottom - about.top).value}",
            (about.bottom - about.top).value <= 241f)
        val aboutList = bounds("about-list")
        assertTrue("About list must not be wider than 780dp: ${(aboutList.right - aboutList.left).value}",
            (aboutList.right - aboutList.left).value <= 781f)

        app.onNodeWithTag("about-back").performClick()
        app.onNodeWithTag("tab-HOME").performClick()
        app.onNodeWithTag("coui-compact-title").assertExists()
        capture("coui-expanded-landscape-home")
    }

    @Test fun landscapeAboutSwipeScrollsContentWithoutChangingTabs() {
        AppText.update(context.resources)
        val settings = AppSettings(uiStyle = UiStyle.COUI, blur = false, liquidGlass = false)
        show({ DpSize(800.dp, 360.dp) }, { settings })
        app.onNodeWithTag("tab-SETTINGS").performClick()
        app.onNodeWithTag("about-settings").performScrollTo().performClick()
        val before = bounds("about-identity").top
        app.onNodeWithTag("about-list").performTouchInput { swipeUp() }
        app.onNodeWithTag("page-ABOUT").assertIsDisplayed()
        app.onNodeWithTag("side-navigation").assertDoesNotExist()
        val banners = app.onAllNodesWithTag("about-identity").fetchSemanticsNodes()
        if (banners.isNotEmpty()) assertTrue(bounds("about-identity").top < before)
        app.onNodeWithTag("about-list").performTouchInput { swipeDown() }
        app.onNodeWithTag("page-ABOUT").assertIsDisplayed()
        app.onNodeWithTag("side-navigation").assertDoesNotExist()
    }

    @Test fun allRootListsConsumeLandscapeSwipesWithAndWithoutBlur() {
        AppText.update(context.resources)
        var settings by mutableStateOf(AppSettings(uiStyle = UiStyle.COUI, blur = false, liquidGlass = false))
        show({ DpSize(800.dp, 360.dp) }, { settings })
        for (blur in listOf(false, true)) {
            app.runOnIdle { settings = settings.copy(blur = blur) }
            for ((tab, list) in listOf("HOME" to "home-list", "SETTINGS" to "settings-list")) {
                app.onNodeWithTag("tab-$tab").performClick()
                app.onNodeWithTag(list).performScrollToIndex(0)
                fun position() = app.onNodeWithTag(list).fetchSemanticsNode().config[
                    androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange].value()
                val start = position()
                app.onNodeWithTag(list).performTouchInput { swipeUp() }
                val scrolled = position()
                assertTrue("$tab must scroll up (blur=$blur)", scrolled > start)
                app.onNodeWithTag("tab-$tab").assertIsSelected()
                app.onNodeWithTag(list).performTouchInput { swipeDown() }
                assertTrue("$tab must scroll back down (blur=$blur)", position() < scrolled)
                app.onNodeWithTag("tab-$tab").assertIsSelected()
            }
        }
    }

    private fun capture(name: String) {
        app.waitForIdle()
        val folder = File(context.getExternalFilesDir(null), "coui-landscape").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use {
            app.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
