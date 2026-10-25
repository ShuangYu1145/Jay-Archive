package love.yu.jaychou

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import love.yu.jaychou.data.AppSettings
import love.yu.jaychou.data.ThemeMode
import love.yu.jaychou.ui.components.ArchiveScaffold
import love.yu.jaychou.ui.components.LocalArchiveBlur
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import love.yu.jaychou.ui.designsystem.Card
import love.yu.jaychou.ui.effects.CustomBackgroundHost
import love.yu.jaychou.ui.effects.GlassLayer
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Uses isolated UI state; never changes the connected device's saved preferences. */
class BackgroundBlurRenderingTest {
    @get:Rule val app = createComposeRule()

    @Test fun cardBlurRemovesWallpaperDetailInsideNestedToolbarCapture() {
        assumeTrue(Build.VERSION.SDK_INT >= 33)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val image = File(context.cacheDir, "blur-rendering-stripes.png")
        val bitmap = Bitmap.createBitmap(512, 1024, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        for (x in 0 until 512 step 8) {
            paint.color = if ((x / 8) % 2 == 0) android.graphics.Color.RED else android.graphics.Color.BLUE
            canvas.drawRect(x.toFloat(), 0f, (x + 8).toFloat(), 1024f, paint)
        }
        image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val showCard = mutableStateOf(false)
        val blurCards = mutableStateOf(true)
        try {
            app.setContent {
                val settings = AppSettings(theme = ThemeMode.DARK,
                    customBackgroundPath = image.path, backgroundCardBlur = blurCards.value)
                ArchiveTheme(settings) {
                    CustomBackgroundHost(settings) {
                        CompositionLocalProvider(LocalArchiveBlur provides true) {
                            GlassLayer(ArchiveUiTheme.colorScheme.background) { capture, _ ->
                                Box(Modifier.fillMaxSize().then(capture)) {
                                    ArchiveScaffold("Blur rendering", small = true) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            if (showCard.value) Card(Modifier.fillMaxWidth().height(180.dp)
                                                .padding(horizontal = 24.dp).testTag("blur-test-card")) {
                                                Spacer(Modifier.fillMaxSize())
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            fun variance(): Double {
                val pixels = app.onRoot().captureToImage().asAndroidBitmap()
                val values = (pixels.width / 4 until pixels.width * 3 / 4 step 3).map { x ->
                    android.graphics.Color.red(pixels.getPixel(x, pixels.height / 2)).toDouble()
                }
                val mean = values.average()
                return values.map { (it - mean) * (it - mean) }.average()
            }
            app.waitUntil(5_000) { variance() > 1_000 }
            val wallpaperVariance = variance()
            app.runOnIdle { showCard.value = true }
            app.onNodeWithTag("blur-test-card").assertIsDisplayed()
            val blurredVariance = variance()
            assertTrue("Wallpaper detail must be blurred, not shown through a transparent card: $blurredVariance / $wallpaperVariance",
                blurredVariance < wallpaperVariance * .01)
            File(context.getExternalFilesDir(null), "background-blur-rendering.png").outputStream().use {
                app.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            app.runOnIdle { blurCards.value = false }
            app.onNodeWithTag("blur-test-card").assertIsDisplayed()
            assertTrue("Disabling card blur restores its solid surface", variance() < 1.0)
        } finally { image.delete() }
    }
}
