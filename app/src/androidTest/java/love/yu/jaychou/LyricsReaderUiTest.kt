package love.yu.jaychou

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.screens.lyrics.LyricsScreen
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class LyricsReaderUiTest {
    @get:Rule val app = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val fixture = (0..39).joinToString("\n") {
        when (it) {
            3, 14, 28 -> ""
            2, 10, 25 -> "灯塔 $it · 这里是自拟阅读测试文本。"
            else -> "阅读测试第 $it 行，保留原始的段落与标点。"
        }
    }

    @Test fun settingsSearchCopyAndImmersiveReadingWorkWithoutChangingLyrics() {
        val repository = SettingsRepository(context)
        val original = runBlocking { repository.lyricsReadingSettings.first() }
        val album = AlbumRepository.load(context).find("1551833317039542272")!!.copy(id = "reader-interaction-fixture")
        val track = album.tracks[8]
        val file = File(context.cacheDir, "reader-fixture.txt").apply { writeText(fixture) }
        try {
            runBlocking {
                repository.setLyricsReadingSettings(LyricsReadingSettings())
                LyricsRepository.import(context, album, track, Uri.fromFile(file))
            }
            val restoration = StateRestorationTester(app)
            restoration.setContent { ArchiveTheme(AppSettings(blur = false, liquidGlass = false)) {
                LyricsScreen(album, track, {})
            } }
            app.waitUntil(5_000) { app.onAllNodesWithTag("lyric-line-0").fetchSemanticsNodes().isNotEmpty() }
            app.onNodeWithTag("lyrics-card").assertDoesNotExist()
            app.onNodeWithTag("lyrics-cover", useUnmergedTree = true).assertIsDisplayed()
            app.onNodeWithTag("lyrics-reading-settings").performClick()
            app.onNodeWithTag("reader-font-size").performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(30f) }
            app.onNodeWithTag("reader-line-spacing").performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(2f) }
            app.onNodeWithTag("reader-align-1").performScrollTo().performClick().assertIsSelected()
            app.onNodeWithText(AppText.get(R.string.ui_reader_done)).performClick()
            app.waitUntil(5_000) { runBlocking { repository.lyricsReadingSettings.first().fontSize == 30f } }
            val saved = runBlocking { repository.lyricsReadingSettings.first() }
            assertEquals(2f, saved.lineSpacing, .02f)
            assertEquals(LyricsAlignment.CENTER, saved.alignment)
            restoration.emulateSavedInstanceStateRestore()
            app.onNodeWithTag("lyrics-reading-settings").performClick()
            app.onNodeWithTag("reader-align-1").performScrollTo().assertIsSelected()
            val panel = File(context.getExternalFilesDir(null), "lyrics-reader").apply { mkdirs() }
            File(panel, "reader-settings.png").outputStream().use {
                app.onNodeWithTag("lyrics-reading-sheet").captureToImage().asAndroidBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            app.onNodeWithTag("reader-copy-all").performScrollTo().performClick()
            app.runOnIdle {
                val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                assertEquals(fixture, manager.primaryClip!!.getItemAt(0).coerceToText(context).toString())
            }
            app.onNodeWithTag("reader-search").performScrollTo().performClick()
            app.onNodeWithTag("lyrics-search-input").performTextInput("灯塔")
            app.onNodeWithTag("lyrics-search-count").assertTextEquals(AppText.get(R.string.ui_reader_match_count, 1, 3))
            app.onNodeWithTag("lyrics-search-next").performClick()
            app.onNodeWithTag("lyrics-search-count").assertTextEquals(AppText.get(R.string.ui_reader_match_count, 2, 3))
            app.onNodeWithTag("lyric-line-10").assertIsDisplayed()
            screenshot("reader-search.png")
            app.onNodeWithContentDescription(AppText.get(R.string.ui_reader_close_search)).performClick()
            app.onNodeWithTag("lyrics-list").performTouchInput {
                doubleClick(Offset(width * .9f, height * .6f))
            }
            app.onNodeWithTag("lyrics-top-bar").assertDoesNotExist()
            app.onNodeWithTag("reader-exit-immersive").assertIsDisplayed().performClick()
            app.onNodeWithTag("lyrics-top-bar").assertIsDisplayed()
            app.onNodeWithTag("lyrics-list").performScrollToIndex(30)
            app.onNodeWithTag("lyrics-song-card").assertDoesNotExist()
            app.onNodeWithTag("lyrics-list").performScrollToIndex(0)
            app.onNodeWithTag("lyrics-song-card").assertIsDisplayed()
            assertEquals(fixture, runBlocking { LyricsRepository.local(context, album, track) })
            screenshot("reader-controls.png")
            app.onNodeWithTag("lyric-line-0").performTouchInput { longClick(Offset(width * .2f, height * .4f)) }
            screenshot("reader-selection.png")
        } finally {
            runBlocking {
                repository.setLyricsReadingSettings(original)
                LyricsRepository.removeLocal(context, album, track)
            }
            file.delete()
        }
    }

    @Test fun readerAdaptsToThemesAndLandscapeAtMaximumTypeSize() {
        val repository = SettingsRepository(context)
        val original = runBlocking { repository.lyricsReadingSettings.first() }
        val album = AlbumRepository.load(context).find("1551833317039542272")!!.copy(id = "reader-visual-fixture")
        val track = album.tracks[8]
        val file = File(context.cacheDir, "reader-visual.txt").apply { writeText(fixture) }
        val appearance = mutableStateOf(AppSettings(blur = false, liquidGlass = false, theme = ThemeMode.LIGHT))
        val viewport = mutableStateOf(DpSize(400.dp, 800.dp))
        try {
            runBlocking {
                repository.setLyricsReadingSettings(LyricsReadingSettings(background = LyricsBackground.ALBUM))
                LyricsRepository.import(context, album, track, Uri.fromFile(file))
            }
            app.setContent {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(viewport.value)) {
                    ArchiveTheme(appearance.value) { LyricsScreen(album, track, {}) }
                }
            }
            app.waitUntil(5_000) { app.onAllNodesWithTag("lyric-line-0").fetchSemanticsNodes().isNotEmpty() }
            for (style in UiStyle.entries) for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
                app.runOnIdle { appearance.value = appearance.value.copy(uiStyle = style, theme = theme) }
                app.onNodeWithTag("lyrics-list").performScrollToIndex(0)
                app.onNodeWithTag("lyrics-reading-settings").performClick()
                app.onNodeWithTag("reader-font-size").performScrollTo()
                    .performSemanticsAction(SemanticsActions.SetProgress) { it(23f) }
                app.onNodeWithTag("reader-line-spacing").performScrollTo()
                    .performSemanticsAction(SemanticsActions.SetProgress) { it(1.7f) }
                app.onNodeWithText(AppText.get(R.string.ui_reader_done)).performClick()
                screenshot("reader-$style-$theme.png")
            }
            app.runOnIdle { viewport.value = DpSize(1000.dp, 520.dp) }
            app.onNodeWithTag("lyrics-reading-settings").performClick()
            app.onNodeWithTag("reader-font-size").performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(32f) }
            app.onNodeWithTag("reader-line-spacing").performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(2.2f) }
            app.onNodeWithText(AppText.get(R.string.ui_reader_done)).performClick()
            app.onNodeWithTag("lyrics-list").performScrollToIndex(10)
            app.onNodeWithTag("lyric-line-10").assertIsDisplayed()
            screenshot("reader-landscape-large-type.png")
        } finally {
            runBlocking {
                repository.setLyricsReadingSettings(original)
                LyricsRepository.removeLocal(context, album, track)
            }
            file.delete()
        }
    }

    private fun screenshot(name: String) {
        val folder = File(context.getExternalFilesDir(null), "lyrics-reader").apply { mkdirs() }
        File(folder, name).outputStream().use {
            app.onNodeWithTag("lyrics-reader").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
