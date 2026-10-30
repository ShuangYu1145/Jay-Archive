package love.yu.jaychou

import androidx.compose.runtime.mutableStateOf
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import love.yu.jaychou.data.*
import love.yu.jaychou.navigation.ArchiveApp
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

class CompositionsNavigationTest {
    @get:Rule val app = createComposeRule()

    @Test fun compositionsSupportBothStylesLayoutsSearchAndBack() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val catalog = AlbumRepository.load(context)
        val songs = CompositionRepository.load(context)
        val rewind = songs.songs.single { it.text.title == "倒带" }
        val settings = mutableStateOf(AppSettings(blur = false, liquidGlass = false))
        app.setContent {
            ArchiveTheme(settings.value) { ArchiveApp(catalog, settings.value, {}, {}, {}, {}, {}, {}, {}, null, {}) }
        }
        app.onNodeWithTag("tab-INTRODUCTION").performClick()
        app.waitForIdle()
        app.onNodeWithTag("tab-COMPOSITIONS").performClick()
        app.waitUntil(10_000) { app.onAllNodesWithTag(songs.songs.first().id).fetchSemanticsNodes().isNotEmpty() }
        for (style in UiStyle.entries) {
            app.runOnIdle { settings.value = settings.value.copy(uiStyle = style) }
            app.onNodeWithTag("tab-COMPOSITIONS").assertIsSelected()
            for (layout in AlbumLayout.entries) {
                app.runOnIdle { settings.value = settings.value.copy(albumLayout = layout) }
                app.onNodeWithTag("compositions-grid").assertIsDisplayed()
                app.onNodeWithTag(songs.songs.first().id).assertIsDisplayed()
                val output = File(context.getExternalFilesDir(null), "composition-checks").apply { mkdirs() }
                File(output, "${style.name}-${layout.name}.png").outputStream().use {
                    app.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
            app.onNodeWithTag("composition-search-toggle").performClick()
            app.onNodeWithTag("composition-search-input").performTextInput("倒帶 蔡依林")
            app.onNodeWithTag(rewind.id).assertIsDisplayed().performClick()
            app.onNodeWithTag("composition-detail").assertIsDisplayed()
            app.onNodeWithTag("introduction-header").assertDoesNotExist()
            app.onNodeWithTag("composition-detail-back").onChild().performClick()
            app.onNodeWithTag("tab-COMPOSITIONS").assertIsSelected()
            app.onNodeWithTag("composition-search-input").performTextReplacement("no-such-song-91827")
            app.onNodeWithTag("compositions-empty").assertIsDisplayed()
            app.onNodeWithTag("composition-search-toggle").performClick()
            app.onNodeWithTag("tab-ALBUMS").performClick()
            app.onNodeWithTag("albums-grid").assertIsDisplayed()
            app.onNodeWithTag("tab-COMPOSITIONS").performClick()
        }
    }
}
