package love.yu.jaychou

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import love.yu.jaychou.data.*
import love.yu.jaychou.navigation.ArchiveApp
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LyricsCoverTransitionTest {
    @get:Rule val app = createComposeRule()

    @Test fun lyricsShareTheSongCoverAndAnimateOnlyTheArtworkInBothStyles() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val catalog = AlbumRepository.load(context)
        val album = catalog.find("1551833317039542272")!!
        val settings = mutableStateOf(AppSettings(blur = false, liquidGlass = false))
        app.setContent {
            ArchiveTheme(settings.value) {
                ArchiveApp(catalog, settings.value, {}, {}, {}, {}, {}, {}, {}, null, {})
            }
        }
        app.onNodeWithTag("tab-INTRODUCTION").performClick()
        app.onNodeWithTag("album-${album.id}").performClick()
        for (style in UiStyle.entries) {
            app.runOnIdle { settings.value = settings.value.copy(uiStyle = style) }
            app.onNodeWithTag("album-detail-list").performScrollToIndex(12)
            val source = app.onNodeWithTag("track-cover-9", useUnmergedTree = true).getUnclippedBoundsInRoot()
            val sourceWidth = app.onNodeWithTag("track-cover-9", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.width
            val list = app.onNodeWithTag("album-detail-list").getUnclippedBoundsInRoot()
            app.mainClock.autoAdvance = false
            try {
                app.onNodeWithTag("track-9").performClick()
                app.mainClock.advanceTimeBy(96)
                val flight = app.onNodeWithTag("lyrics-detail-cover-flight", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                val destination = app.onNodeWithTag("lyrics-cover", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                assertTrue("Only the cover grows from the song thumbnail: $sourceWidth -> ${flight.width} -> ${destination.width}",
                    flight.width > sourceWidth && flight.width < destination.width)
                assertTrue("The moving artwork keeps a square aspect ratio",
                    kotlin.math.abs(flight.width - flight.height) < 1f)
                val page = app.onNodeWithTag("lyrics-detail").getUnclippedBoundsInRoot()
                assertTrue("Lyrics text is not stretched from a song row",
                    kotlin.math.abs(((page.right - page.left) - (list.right - list.left)).value) < 1f)
            } finally {
                app.mainClock.autoAdvance = true
            }
            app.onNodeWithTag("lyrics-detail-cover-flight", useUnmergedTree = true).assertDoesNotExist()
            app.onNodeWithTag("lyrics-cover", useUnmergedTree = true).assertIsDisplayed()
            val folder = File(context.getExternalFilesDir(null), "lyrics-cover-transition").apply { mkdirs() }
            File(folder, "$style.png").outputStream().use {
                app.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            app.onNodeWithContentDescription(AppText.get(R.string.ui_back_to_album_details)).performClick()
            app.onNodeWithTag("lyrics-detail").assertDoesNotExist()
            val returned = app.onNodeWithTag("track-cover-9", useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertTrue("Returning restores the source artwork and list position", kotlin.math.abs((returned.top - source.top).value) < 1f)
        }
    }
}
