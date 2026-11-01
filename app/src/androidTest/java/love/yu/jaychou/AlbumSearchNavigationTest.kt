package love.yu.jaychou

import love.yu.jaychou.ui.theme.ArchiveTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.runtime.mutableStateOf
import love.yu.jaychou.data.*
import love.yu.jaychou.navigation.ArchiveApp
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

class AlbumSearchNavigationTest {
    @get:Rule val app = createComposeRule()

    @Test fun searchesAlbumsAndTracksAndReturnsToResults() = verifySearch(AppSettings(blur = false))

    @Test fun couiSearchWithBlurReturnsToResults() = verifySearch(AppSettings(
        uiStyle = UiStyle.COUI, blur = true, liquidGlass = false, floatingBottomBar = true))

    @Test fun searchEntryFocusKeepsIntroductionOnAlbums() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val catalog = AlbumRepository.load(context)
        val settings = mutableStateOf(AppSettings(blur = false, liquidGlass = false))
        app.mainClock.autoAdvance = false
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
            val left = app.onNodeWithTag("page-ALBUMS").getUnclippedBoundsInRoot().left
            app.onNodeWithTag("album-search-open").performClick()
            // Sample throughout entry and the focus relocation that follows it. A final
            // screenshot alone misses the neighbouring concert page flashing on screen.
            repeat(60) {
                app.mainClock.advanceTimeBy(16)
                app.waitForIdle()
                val current = app.onNodeWithTag("page-ALBUMS").getUnclippedBoundsInRoot().left
                assertTrue("Search must not scroll the introduction pager in $style: $current vs $left",
                    kotlin.math.abs((current - left).value) < 1f)
            }
            app.onNodeWithTag("album-search-input").assertIsFocused()
            app.onNodeWithTag("album-search-close").performClick()
            settle()
            app.onNodeWithTag("tab-ALBUMS").assertIsSelected()
        }
    }

    private fun verifySearch(settings: AppSettings) {
        AppText.update(InstrumentationRegistry.getInstrumentation().targetContext.resources)
        val catalog = AlbumRepository.load(InstrumentationRegistry.getInstrumentation().targetContext)
        app.mainClock.autoAdvance = false
        app.setContent {
            ArchiveTheme(settings) {
                ArchiveApp(catalog, settings, {}, {}, {}, {}, {}, {}, {}, null, {})
            }
        }
        settle()
        app.onNodeWithTag("tab-INTRODUCTION").performClick()
        settle()
        app.onNodeWithTag("album-search-open").performClick()
        settle()
        app.onNodeWithTag("album-search-input").performTextInput("七里香")
        settle()
        val album = catalog.albums.first { it.originalTitle == "七里香" }
        app.onNodeWithTag("search-album-${album.id}").assertIsDisplayed()
        app.onNodeWithTag("album-search-input").performTextReplacement("晴天")
        settle()
        val songAlbum = catalog.albums.first { it.originalTitle == "叶惠美" }
        val song = songAlbum.tracks.first { it.title == "晴天" }
        app.onNodeWithTag("search-song-${songAlbum.id}-${song.number}").performClick()
        settle()
        app.onNodeWithTag("track-${song.number}").assertIsDisplayed()
        app.onNodeWithContentDescription(AppText.get(R.string.ui_back_to_albums)).performClick()
        settle()
        app.onNodeWithTag("album-search-input").assertTextEquals("晴天")
        app.onNodeWithTag("album-search-clear").performClick()
        settle()
        app.onNodeWithTag("album-search-input").performTextInput("不存在xyz")
        settle()
        app.onNodeWithText(AppText.get(R.string.ui_no_matching_albums_or_songs)).assertIsDisplayed()
        app.onNodeWithTag("album-search-close").performClick()
        settle()
        app.onNodeWithTag("albums-grid").assertIsDisplayed()
        app.onNodeWithTag("tab-ALBUMS").assertIsSelected()
    }

    private fun settle() {
        app.mainClock.advanceTimeBy(1500)
        app.waitForIdle()
    }
}
