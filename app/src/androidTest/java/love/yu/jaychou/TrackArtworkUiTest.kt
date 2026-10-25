package love.yu.jaychou

import android.graphics.Bitmap
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.screens.albums.AlbumDetailScreen
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TrackArtworkUiTest {
    @get:Rule val app = createComposeRule()

    @Test fun numbersArtworkAndTitlesStayInOrderWithoutStarsInBothStyles() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val album = AlbumRepository.load(context).find("1551833317039542272")!!
        val settings = mutableStateOf(AppSettings(blur = false, liquidGlass = false))
        var clicked = false
        app.setContent {
            ArchiveTheme(settings.value) {
                AlbumDetailScreen(album, rememberLazyListState(), 24.dp, {}, { _, _ -> clicked = true })
            }
        }
        for (style in UiStyle.entries) {
            app.runOnIdle { settings.value = settings.value.copy(uiStyle = style) }
            app.onNodeWithTag("album-detail-list").performScrollToIndex(12)
            val number = app.onNodeWithTag("track-number-9", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val cover = app.onNodeWithTag("track-cover-9", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue("Artwork follows the plain track number", cover.left > number.right)
            val title = app.onNodeWithText(album.tracks[8].title, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue("Song title follows artwork", title.left > cover.right)
            app.onAllNodesWithText("🌟", useUnmergedTree = true).assertCountEquals(0)
            app.onAllNodesWithTag("lead-track-9", useUnmergedTree = true).assertCountEquals(0)
            val folder = File(context.getExternalFilesDir(null), "track-artwork").apply { mkdirs() }
            File(folder, "$style.png").outputStream().use {
                app.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            app.runOnIdle { clicked = false }
            app.onNodeWithTag("track-9").performClick()
            app.runOnIdle { assertTrue("Song row still opens lyrics", clicked) }
        }
    }
}
