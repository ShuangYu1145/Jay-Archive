package love.yu.jaychou

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.components.ArchiveScaffold
import love.yu.jaychou.ui.components.LocalArchiveBlur
import love.yu.jaychou.ui.effects.GlassLayer
import love.yu.jaychou.ui.navigation.ArchiveBottomNavigationBar
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Broad color fields survive frost while fine stripes disappear; solid fallback loses both. */
class MaterialBlurRenderingTest {
    @get:Rule val app = createComposeRule()

    @Test fun materialHeaderAndStandardNavigationSampleAndBlurTheirBackground() {
        assumeTrue(Build.VERSION.SDK_INT >= 33)
        val blur = mutableStateOf(true)
        app.setContent {
            val settings = AppSettings(uiStyle = UiStyle.MATERIAL3, theme = ThemeMode.LIGHT,
                floatingBottomBar = false, liquidGlass = false, blur = blur.value)
            ArchiveTheme(settings) {
                CompositionLocalProvider(LocalArchiveBlur provides blur.value) {
                    GlassLayer(Color.White) { capture, _ ->
                        Box(Modifier.fillMaxSize()) {
                            Box(Modifier.fillMaxSize().then(capture)) {
                                ArchiveScaffold("MD3 blur", small = true) {
                                    Canvas(Modifier.fillMaxSize()) {
                                        for (x in 0 until size.width.toInt() step 6) {
                                            val red = x < size.width / 2
                                            val bright = (x / 6) % 2 == 0
                                            drawRect(if (red) (if (bright) Color.Red else Color.Black)
                                                else (if (bright) Color.Blue else Color.Black),
                                                Offset(x.toFloat(), 0f), Size(6f, size.height))
                                        }
                                    }
                                }
                            }
                            ArchiveBottomNavigationBar(settings, 0, { 0f }, {}, {}, true,
                                Modifier.align(Alignment.BottomCenter))
                        }
                    }
                }
            }
        }
        fun row(tag: String, atBottom: Boolean): List<Double> {
            val bounds = app.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            val pixels = app.onRoot().captureToImage().asAndroidBitmap()
            val y = (if (atBottom) bounds.bottom - 4 else bounds.top + 4).toInt()
            return (pixels.width / 5 until pixels.width * 4 / 5).map {
                android.graphics.Color.red(pixels.getPixel(it, y)).toDouble()
            }
        }
        fun variance(values: List<Double>): Double {
            val mean = values.average()
            return values.map { (it - mean) * (it - mean) }.average()
        }
        app.waitForIdle()
        for ((tag, bottom) in listOf("top-bar-MD3 blur" to true, "standard-navigation" to false)) {
            app.waitUntil(5_000) { variance(row(tag, bottom)) > 2 }
            val values = row(tag, bottom)
            val roughness = values.zipWithNext { a, b -> (a - b) * (a - b) }.average()
            assertTrue("$tag must smooth the fine stripes: $roughness", roughness < 4)
        }
        app.runOnIdle { blur.value = false }
        for ((tag, bottom) in listOf("top-bar-MD3 blur" to true, "standard-navigation" to false)) {
            assertTrue("$tag must restore an opaque fallback", variance(row(tag, bottom)) < 1)
        }
    }
}
