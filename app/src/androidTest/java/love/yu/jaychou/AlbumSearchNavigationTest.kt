package love.yu.jaychou

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import love.yu.jaychou.data.*
import love.yu.jaychou.navigation.ArchiveApp
import love.yu.jaychou.ui.ArchiveTheme
import org.junit.Rule
import org.junit.Test

class AlbumSearchNavigationTest {
    @get:Rule val app = createComposeRule()

    @Test fun searchesAlbumsAndTracksAndReturnsToResults() {
        val catalog = AlbumRepository.load(InstrumentationRegistry.getInstrumentation().targetContext)
        val settings = AppSettings(blur = false)
        app.mainClock.autoAdvance = false
        app.setContent {
            ArchiveTheme(settings) {
                ArchiveApp(catalog, settings, {}, {}, {}, {}, {}, {}, {}, null, {})
            }
        }
        settle()
        app.onNodeWithTag("tab-ALBUMS").performClick()
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
