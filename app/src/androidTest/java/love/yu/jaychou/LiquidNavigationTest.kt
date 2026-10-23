package love.yu.jaychou

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import love.yu.jaychou.data.AppSettings
import love.yu.jaychou.ui.ArchiveTheme
import love.yu.jaychou.ui.DraggableNavigationBar
import love.yu.jaychou.ui.GlassLayer
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Isolated host avoids the activity's launch-window pre-draw gate under instrumentation. */
class LiquidNavigationTest {
    @get:Rule val app = createComposeRule()

    @Test fun liquidLensPressDragCancelAndTap() {
        app.mainClock.autoAdvance = false
        app.setContent {
            ArchiveTheme(AppSettings()) {
                var selected by remember { mutableIntStateOf(0) }
                GlassLayer(Color(0xFFF6F6F8)) { capture, glass ->
                    Box(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize().then(capture)) {
                            repeat(24) { index ->
                                Box(Modifier.fillMaxWidth().height(40.dp)
                                    .background(if (index % 2 == 0) Color(0xFFDCE5FA) else Color.White))
                            }
                        }
                        DraggableNavigationBar(selected, { selected.toFloat() }, { selected = it }, {},
                            glass, true, Modifier.align(Alignment.Center).padding(horizontal = 28.dp))
                    }
                }
            }
        }
        app.mainClock.advanceTimeBy(1500)
        app.waitForIdle()
        val bar = app.onNodeWithTag("floating-navigation")
        val initialWidth = app.onNodeWithTag("navigation-indicator").fetchSemanticsNode().boundsInRoot.width
        app.mainClock.autoAdvance = false
        bar.performTouchInput { down(Offset(width / 10f, centerY)) }
        app.mainClock.advanceTimeBy(400)
        app.mainClock.advanceTimeBy(1500)
        app.waitForIdle()
        val pressedWidth = app.onNodeWithTag("navigation-indicator").fetchSemanticsNode().boundsInRoot.width
        assertTrue("Pressed lens must expand beyond its resting capsule", pressedWidth > initialWidth * 1.2f)
        screenshot("liquid-pressed.png")
        bar.performTouchInput { moveTo(Offset(width * .9f, centerY), 500) }
        app.mainClock.advanceTimeBy(500)
        // Scrubbing previews the lens; navigation commits only when the finger lifts.
        app.onNodeWithTag("tab-HOME").assertIsSelected()
        bar.performTouchInput { up() }
        app.mainClock.advanceTimeBy(1500)
        app.mainClock.advanceTimeBy(1500)
        app.waitForIdle()
        app.onNodeWithTag("tab-ABOUT").assertIsSelected()
        screenshot("liquid-idle.png")
        bar.performTouchInput {
            down(Offset(width * .9f, centerY))
            moveTo(Offset(width * .1f, centerY), 500)
            cancel()
        }
        app.mainClock.advanceTimeBy(1500)
        app.waitForIdle()
        app.onNodeWithTag("tab-ABOUT").assertIsSelected()
        app.onNodeWithTag("tab-ALBUMS").performClick()
        app.mainClock.advanceTimeBy(1500)
        app.waitForIdle()
        app.onNodeWithTag("tab-ALBUMS").assertIsSelected()
    }

    private fun screenshot(name: String) {
        val directory = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
        File(directory, name).outputStream().use {
            app.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
