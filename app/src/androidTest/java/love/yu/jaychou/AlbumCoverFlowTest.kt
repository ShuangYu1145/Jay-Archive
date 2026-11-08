package love.yu.jaychou

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.components.AlbumCoverFlow
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AlbumCoverFlowTest {
    @get:Rule val app = createComposeRule()

    @Test fun sideClickCentersThenOpensAndRestoresIdentityAfterReorder() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val source = AlbumRepository.load(context).albums.take(8)
        val albums = mutableStateOf(source)
        val restoration = StateRestorationTester(app)
        var opened: String? = null
        app.mainClock.autoAdvance = false
        restoration.setContent {
            ArchiveTheme(AppSettings()) {
                AlbumCoverFlow(albums.value, { album, _ -> opened = album.id }, Modifier.fillMaxSize())
            }
        }
        app.mainClock.advanceTimeBy(100)
        // Tap the actual projected side face; the shelf owns solid-geometry hit testing.
        app.onNodeWithTag("cover-flow-shelf").performTouchInput {
            click(androidx.compose.ui.geometry.Offset(width * .94f, centerY))
        }
        app.mainClock.advanceTimeBy(1500)
        java.io.File(context.getExternalFilesDir(null), "cover-flow.png").outputStream().use {
            app.onNodeWithTag("album-cover-flow").captureToImage().asAndroidBitmap()
                .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        app.onNodeWithTag("cover-flow-${source[1].id}").assertIsSelected()
        app.runOnIdle { assertEquals(null, opened) }
        app.onNodeWithTag("cover-flow-${source[1].id}").performClick()
        app.runOnIdle { assertEquals(source[1].id, opened) }
        restoration.emulateSavedInstanceStateRestore()
        app.mainClock.advanceTimeBy(100)
        app.onNodeWithTag("cover-flow-${source[1].id}").assertIsSelected()
        app.runOnIdle { albums.value = source.reversed() }
        app.mainClock.advanceTimeBy(100)
        app.onNodeWithTag("cover-flow-${source[1].id}").assertIsSelected()
        app.runOnIdle { albums.value = listOf(source.last()) }
        app.mainClock.advanceTimeBy(100)
        app.onNodeWithTag("cover-flow-${source.last().id}").assertIsSelected()
        app.runOnIdle { albums.value = emptyList() }
        app.mainClock.advanceTimeBy(100)
        app.onNodeWithText(context.getString(R.string.ui_cover_flow_empty)).assertIsDisplayed()
    }

    @Test fun dragInterruptsClickAnimationAndReverseDragStillSnaps() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val albums = AlbumRepository.load(context).albums.take(12)
        var selected: Album? = null
        app.mainClock.autoAdvance = false
        app.setContent {
            ArchiveTheme(AppSettings()) {
                AlbumCoverFlow(albums, { _, _ -> }, Modifier.fillMaxSize(), { selected = it })
            }
        }
        app.mainClock.advanceTimeBy(100)
        app.onNodeWithTag("cover-flow-${albums[2].id}").performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
        app.mainClock.advanceTimeBy(64)
        app.onNodeWithTag("cover-flow-shelf").performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(-120f, 0f), 100)
            moveBy(androidx.compose.ui.geometry.Offset(170f, 0f), 160)
            up()
        }
        app.mainClock.advanceTimeBy(2500)
        app.waitForIdle()
        app.runOnIdle { check(selected != null) }
        app.onNodeWithTag("cover-flow-${selected!!.id}").assertIsSelected()
        app.onAllNodes(isSelected()).assertCountEquals(1)
        val range = app.onNode(SemanticsMatcher.keyIsDefined(
            androidx.compose.ui.semantics.SemanticsProperties.HorizontalScrollAxisRange))
            .fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.HorizontalScrollAxisRange]
        val position = range.value()
        org.junit.Assert.assertEquals(kotlin.math.round(position), position, .001f)
    }
}
