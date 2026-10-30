package love.yu.jaychou

import android.graphics.Bitmap
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.screens.albums.ReleaseCategorySheet
import love.yu.jaychou.ui.screens.lyrics.LyricsReadingSheet
import love.yu.jaychou.ui.screens.settings.SettingsScreen
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CouiPreferenceLayoutTest {
    @get:Rule val app = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun settingsRowsStayCompactAndConditionalControlsStillWork() {
        var settings by mutableStateOf(AppSettings(uiStyle = UiStyle.COUI, theme = ThemeMode.LIGHT))
        var scale by mutableFloatStateOf(1f)
        app.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                ArchiveTheme(settings) {
                    SettingsScreen(settings, rememberLazyListState(), 24.dp,
                        onTheme = { settings = settings.copy(theme = it) },
                        onMonet = { settings = settings.copy(monet = it) },
                        onBlur = { settings = settings.copy(blur = it) }, onPredictive = {},
                        onAlbumLayout = { settings = settings.copy(albumLayout = it) }, onCustomColor = {},
                        onFloatingBottomBar = { settings = settings.copy(floatingBottomBar = it) })
                }
            }
        }
        app.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("ui-style"))
        val style = app.onNodeWithTag("ui-style").getUnclippedBoundsInRoot()
        assertTrue("A selected value must not squeeze the description into a tall column", (style.bottom - style.top).value < 110)
        capture("settings-light")
        app.onNodeWithContentDescription(AppText.get(R.string.ui_floating_navigation_bar)).performClick()
        app.onNodeWithContentDescription(AppText.get(R.string.ui_liquid_glass)).assertDoesNotExist()
        app.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("theme-mode"))
        val theme = app.onNodeWithTag("theme-mode").getUnclippedBoundsInRoot()
        assertTrue("A single-line row should keep a 52dp touch target", (theme.bottom - theme.top).value in 52f..60f)
        app.runOnIdle { settings = settings.copy(theme = ThemeMode.DARK) }
        capture("settings-dark")
        app.runOnIdle { scale = 1.5f }
        app.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("custom-color"))
        app.onNodeWithTag("custom-color").assertIsDisplayed()
        capture("settings-large-font")
    }

    @Test fun groupedCategoryAndReaderControlsPreserveSelections() {
        val catalog = AlbumRepository.load(context)
        var categories by mutableStateOf(listOf(ReleaseCategory.STUDIO.name))
        var categorySheet by mutableStateOf(true)
        var reader by mutableStateOf(LyricsReadingSettings())
        app.setContent {
            ArchiveTheme(AppSettings(uiStyle = UiStyle.COUI, theme = ThemeMode.LIGHT)) {
                ReleaseCategorySheet(categorySheet, categories, catalog,
                    onDismiss = { categorySheet = false }, onApply = { categories = it })
                LyricsReadingSheet(!categorySheet, reader, true, true,
                    onChange = { value, _ -> reader = value }, onCommit = {}, onDismiss = {}, onCopy = {}, onSearch = {})
            }
        }
        app.onNodeWithTag("category-LIVE").performClick()
        capture("categories", "category-sheet")
        app.onNodeWithTag("category-apply").performClick()
        app.runOnIdle { assertTrue(ReleaseCategory.LIVE.name in categories) }
        app.onNodeWithTag("reader-font-size").performSemanticsAction(SemanticsActions.SetProgress) { it(27f) }
        app.runOnIdle { assertEquals(27f, reader.fontSize, .01f) }
        capture("reader", "lyrics-reading-sheet")
    }

    private fun capture(name: String, tag: String? = null) {
        app.waitForIdle()
        val folder = File(context.filesDir, "coui-style-previews").apply { mkdirs() }
        val node = if (tag == null) app.onRoot() else app.onNodeWithTag(tag)
        File(folder, "$name.png").outputStream().use {
            node.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
