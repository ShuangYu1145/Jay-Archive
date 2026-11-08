package love.yu.jaychou

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import love.yu.jaychou.data.*
import love.yu.jaychou.navigation.ArchiveApp
import love.yu.jaychou.ui.components.AlbumCoverFlow
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalTestApi::class)
class CoverFlowPresentationTest {
    @get:Rule val app = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun squareCoversAndThemedBackgroundInPortraitAndLandscape() {
        AppText.update(context.resources)
        val albums = AlbumRepository.load(context).albums.filter { it.category == ReleaseCategory.STUDIO }.take(8)
        var settings by mutableStateOf(AppSettings(theme = ThemeMode.LIGHT, blur = false))
        var size by mutableStateOf(DpSize(400.dp, 800.dp))
        app.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size)) {
                ArchiveTheme(settings) { AlbumCoverFlow(albums, { _, _ -> }, Modifier.fillMaxSize()) }
            }
        }
        app.onNodeWithTag("cover-flow-${albums[4].id}").performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
        app.mainClock.advanceTimeBy(1500)
        app.onNodeWithTag("cover-flow-${albums[4].id}").assertIsSelected()
        for (style in UiStyle.entries) for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            for (viewport in listOf(DpSize(400.dp, 800.dp), DpSize(900.dp, 420.dp))) {
                app.runOnIdle { settings = settings.copy(theme = theme, uiStyle = style); size = viewport }
                val cover = app.onNodeWithTag("cover-flow-${albums[4].id}").getUnclippedBoundsInRoot()
                assertEquals((cover.right - cover.left).value, (cover.bottom - cover.top).value, .01f)
                val image = app.onNodeWithTag("album-cover-flow").captureToImage()
                val pixels = image.toPixelMap()
                val luminance = pixels[8, 8].luminance()
                if (theme == ThemeMode.LIGHT) assertTrue("$style light background", luminance > .7f)
                else assertTrue("$style dark background", luminance < .15f)
                val folder = File(context.getExternalFilesDir(null), "cd-cases").apply { mkdirs() }
                File(folder, "${style.name}-${theme.name}-${viewport.width.value.toInt()}.png").outputStream().use {
                    image.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
    }

    @Test fun hideBothBarsRestoreAndExpandPastLandscapeRail() {
        AppText.update(context.resources)
        val catalog = AlbumRepository.load(context)
        var size by mutableStateOf(DpSize(400.dp, 800.dp))
        val settings = AppSettings(albumLayout = AlbumLayout.COVER_FLOW, uiStyle = UiStyle.COUI,
            blur = false, liquidGlass = false)
        app.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size)) {
                ArchiveTheme(settings) { ArchiveApp(catalog, settings, {}, {}, {}, {}, {}, {}, {}, null, {}) }
            }
        }
        app.onNodeWithTag("tab-INTRODUCTION").performClick()
        app.onNodeWithTag("introduction-header").assertIsDisplayed()
        app.onNodeWithTag("bottom-navigation").assertIsDisplayed()
        hideBars()
        app.onNodeWithTag("introduction-header").assertDoesNotExist()
        app.onNodeWithTag("bottom-navigation").assertDoesNotExist()
        app.onNodeWithTag("cover-flow-display").assertIsDisplayed()
        app.onNode(isSelected()).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
        app.onNodeWithTag("album-detail").assertIsDisplayed()
        Espresso.pressBack()
        app.waitUntil(5_000) { app.onAllNodesWithTag("album-detail").fetchSemanticsNodes().isEmpty() }
        app.onNodeWithTag("bottom-navigation").assertDoesNotExist()
        app.onNodeWithTag("introduction-header").assertDoesNotExist()
        Espresso.pressBack()
        app.onNodeWithTag("introduction-header").assertIsDisplayed()
        app.onNodeWithTag("bottom-navigation").assertIsDisplayed()

        app.runOnIdle { size = DpSize(900.dp, 420.dp) }
        val before = app.onNodeWithTag("album-cover-flow").getUnclippedBoundsInRoot().let { it.right - it.left }
        app.onNodeWithTag("side-navigation").assertIsDisplayed()
        hideBars()
        app.onNodeWithTag("side-navigation").assertDoesNotExist()
        val after = app.onNodeWithTag("album-cover-flow").getUnclippedBoundsInRoot().let { it.right - it.left }
        assertTrue("Hidden rail must release its reserved width", after > before + 40.dp)
        app.onNodeWithTag("cover-flow-display").performClick()
        app.onNodeWithContentDescription(context.getString(R.string.ui_cover_flow_show_header)).performClick()
        app.onNodeWithContentDescription(context.getString(R.string.ui_cover_flow_show_navigation)).performClick()
        app.onNodeWithTag("cover-flow-display-done").performClick()
        app.onNodeWithTag("introduction-header").assertIsDisplayed()
        app.onNodeWithTag("side-navigation").assertIsDisplayed()
    }

    @Test fun portraitArtworkFillsTheSquareFaceWithoutLetterboxing() {
        AppText.update(context.resources)
        val fixture = File(context.cacheDir, "portrait-cover.png")
        val bitmap = android.graphics.Bitmap.createBitmap(40, 120, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.rgb(18, 171, 52))
        fixture.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val album = AlbumRepository.load(context).albums.first().copy(coverUrl = fixture.toURI().toString())
        app.setContent {
            ArchiveTheme(AppSettings(theme = ThemeMode.LIGHT)) {
                AlbumCoverFlow(listOf(album), { _, _ -> }, Modifier.fillMaxSize())
            }
        }
        app.waitUntil(5_000) {
            val pixels = app.onNodeWithTag("cover-flow-${album.id}").captureToImage().toPixelMap()
            pixels[(pixels.width * .1f).toInt(), pixels.height / 2].green > .5f
        }
        val pixels = app.onNodeWithTag("cover-flow-${album.id}").captureToImage().toPixelMap()
        for (fraction in listOf(.1f, .9f)) {
            val pixel = pixels[(pixels.width * fraction).toInt(), pixels.height / 2]
            assertEquals(171f / 255f, pixel.green, .03f)
            assertEquals(18f / 255f, pixel.red, .03f)
        }
    }

    private fun hideBars() {
        app.onNodeWithTag("cover-flow-display").performClick()
        app.onNodeWithContentDescription(context.getString(R.string.ui_cover_flow_show_header)).performClick()
        app.onNodeWithContentDescription(context.getString(R.string.ui_cover_flow_show_navigation)).performClick()
        app.onNodeWithTag("cover-flow-display-done").performClick()
    }
}
