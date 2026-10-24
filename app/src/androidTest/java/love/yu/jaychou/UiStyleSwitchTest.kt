package love.yu.jaychou

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import love.yu.jaychou.data.*
import love.yu.jaychou.navigation.ArchiveApp
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class UiStyleSwitchTest {
    @get:Rule val app = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun show(initial: UiStyle) {
        val catalog = AlbumRepository.load(context)
        app.setContent {
            var settings by remember { mutableStateOf(AppSettings(
                uiStyle = initial, theme = ThemeMode.LIGHT, monet = false, blur = false, liquidGlass = false)) }
            ArchiveTheme(settings) {
                ArchiveApp(catalog, settings,
                    onTheme = { settings = settings.copy(theme = it) },
                    onMonet = { settings = settings.copy(monet = it) },
                    onBlur = { settings = settings.copy(blur = it) },
                    onPredictive = { settings = settings.copy(predictiveBack = it) },
                    onAlbumLayout = { settings = settings.copy(albumLayout = it) },
                    onAlbumCategories = { settings = settings.copy(albumCategories = it) },
                    onCustomColor = { settings = settings.copy(customColor = it) },
                    onUiStyle = { settings = settings.copy(uiStyle = it) },
                    error = null, onDismissError = {})
            }
        }
    }

    @Test fun switchingNativeControlsKeepsTheCurrentPageAndCanSwitchBack() {
        show(UiStyle.MIUIX)
        app.onNodeWithTag("tab-SETTINGS").performClick()
        app.onNodeWithTag("ui-style").performScrollTo().performClick()
        app.onNodeWithText("Material Design 3").performClick()
        app.onNodeWithTag("material-navigation").assertIsDisplayed()
        app.onNodeWithTag("settings-list").assertIsDisplayed()
        app.onAllNodesWithTag("floating-navigation").assertCountEquals(0)
        screenshot("material-settings.png")
        app.onNodeWithTag("ui-style").performScrollTo().performClick()
        app.onNodeWithText("Miuix").performClick()
        app.onNodeWithTag("floating-navigation").assertIsDisplayed()
        app.onNodeWithTag("settings-list").assertIsDisplayed()
        app.onAllNodesWithTag("material-navigation").assertCountEquals(0)
        screenshot("miuix-settings.png")
    }

    @Test fun materialCategoriesSearchAlbumAndLyricsUseTheSameCatalog() {
        val album = AlbumRepository.load(context).find("1152151605810958336")!!
        val track = album.tracks[1]
        val original = runBlocking { LyricsRepository.local(context, album, track) }
        val fixture = File(context.cacheDir, "material-lyrics-preview.txt")
        fixture.writeText("界面预览用示例文字\n\n原文在独立阅读卡片中自然换行\n每段之间保留舒适的阅读间距\n\n这里只用于检查字号、留白和封面圆角\n歌词页面不显示主打星标\n")
        runBlocking { LyricsRepository.import(context, album, track, Uri.fromFile(fixture)) }
        try {
            show(UiStyle.MATERIAL3)
            app.onNodeWithTag("tab-INTRODUCTION").performClick()
            app.onNodeWithTag("tab-ALBUMS").performClick()
            app.onNodeWithTag("release-filter").performClick()
            app.onNodeWithTag("category-SINGLE").performClick()
            app.onNodeWithText(AppText.get(R.string.ui_confirm)).performClick()
            app.onAllNodesWithTag("category-sheet").assertCountEquals(0)
            app.onNodeWithTag("album-search-open").performClick()
            app.onNodeWithTag("album-search-input").performTextInput("Common")
            app.onNodeWithTag("search-album-${album.id}").performClick()
            app.onNodeWithTag("album-detail").assertIsDisplayed()
            app.onNodeWithTag("album-detail-list").performScrollToIndex(4)
            app.onNodeWithTag("track-2").assertIsDisplayed()
            screenshot("material-tracks.png")
            app.onNodeWithTag("track-2").performClick()
            app.onNodeWithTag("lyrics-song-card").assertIsDisplayed()
            app.onNodeWithTag("lyrics-list").assertIsDisplayed()
            app.onNodeWithText("界面预览用示例文字").assertIsDisplayed()
            app.onAllNodesWithText("🌟").assertCountEquals(0)
            screenshot("material-lyrics.png")
            app.onNodeWithContentDescription(AppText.get(R.string.ui_back_to_album_details)).performClick()
            app.onNodeWithTag("track-2").assertIsDisplayed()
        } finally {
            runBlocking {
                if (original == null) LyricsRepository.removeLocal(context, album, track)
                else {
                    fixture.writeText(original)
                    LyricsRepository.import(context, album, track, Uri.fromFile(fixture))
                }
            }
            fixture.delete()
        }
    }

    @Test fun appearanceProfilesPersistWithoutOverwritingEachOther() = runBlocking {
        val repository = SettingsRepository(context)
        val initialStyle = repository.settings.first().uiStyle
        repository.setUiStyle(UiStyle.MIUIX)
        val miuix = repository.settings.first()
        repository.setUiStyle(UiStyle.MATERIAL3)
        val material = repository.settings.first()
        suspend fun restore(style: UiStyle, saved: AppSettings) {
            repository.setUiStyle(style)
            repository.setTheme(saved.theme)
            repository.setMonet(saved.monet)
            repository.setCustomColor(saved.customColor)
            repository.setAlbumLayout(saved.albumLayout)
        }
        try {
            repository.setUiStyle(UiStyle.MIUIX)
            repository.setTheme(ThemeMode.LIGHT)
            repository.setMonet(false)
            repository.setCustomColor(0xFF4979F5.toInt())
            repository.setAlbumLayout(AlbumLayout.LIST)
            repository.setUiStyle(UiStyle.MATERIAL3)
            repository.setTheme(ThemeMode.DARK)
            repository.setMonet(true)
            repository.setCustomColor(0xFF2D8B70.toInt())
            repository.setAlbumLayout(AlbumLayout.LARGE)
            repository.setUiStyle(UiStyle.MIUIX)
            val first = SettingsRepository(context).settings.first()
            assertEquals(ThemeMode.LIGHT, first.theme)
            assertFalse(first.monet)
            assertEquals(0xFF4979F5.toInt(), first.customColor)
            assertEquals(AlbumLayout.LIST, first.albumLayout)
            assertEquals(miuix.blur, first.blur)
            assertEquals(miuix.liquidGlass, first.liquidGlass)
            assertEquals(miuix.floatingBottomBar, first.floatingBottomBar)
            repository.setUiStyle(UiStyle.MATERIAL3)
            val second = SettingsRepository(context).settings.first()
            assertEquals(ThemeMode.DARK, second.theme)
            assertTrue(second.monet)
            assertEquals(0xFF2D8B70.toInt(), second.customColor)
            assertEquals(AlbumLayout.LARGE, second.albumLayout)
            assertEquals(first.userName, second.userName)
            assertEquals(first.albumCategories, second.albumCategories)
        } finally {
            restore(UiStyle.MIUIX, miuix)
            restore(UiStyle.MATERIAL3, material)
            repository.setUiStyle(initialStyle)
        }
    }

    private fun screenshot(name: String) {
        val directory = File(context.getExternalFilesDir(null), "dual-ui").apply { mkdirs() }
        File(directory, name).outputStream().use {
            app.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
