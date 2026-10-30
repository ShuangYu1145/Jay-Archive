package love.yu.jaychou

import android.graphics.Bitmap
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.screens.about.AboutScreen
import love.yu.jaychou.ui.theme.ArchiveTheme
import love.yu.jaychou.ui.updates.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CouiAboutScreenTest {
    @get:Rule val app = createComposeRule()

    @Test fun versionCardChecksUpdatesAndDeveloperStillOpensEasterEgg() {
        var dark by mutableStateOf(false)
        var fontScale by mutableFloatStateOf(1f)
        var checks = 0
        app.setContent {
            val scope = rememberCoroutineScope()
            val controller = remember { UpdateController(scope, { checks++; UpdateResult.UpToDate }, {}) }
            val density = LocalDensity.current
            CompositionLocalProvider(LocalUpdateController provides controller,
                LocalDensity provides Density(density.density, fontScale)) {
                ArchiveTheme(AppSettings(uiStyle = UiStyle.COUI,
                    theme = if (dark) ThemeMode.DARK else ThemeMode.LIGHT)) {
                    AboutScreen(rememberLazyListState(), 24.dp)
                }
            }
        }
        app.onNodeWithTag("about-identity").assertIsDisplayed()
        capture("coui-about-light")
        app.onNodeWithTag("about-identity").performClick()
        app.waitUntil { checks == 1 }
        repeat(4) { app.onNodeWithTag("about-developer-card").performClick() }
        app.onNodeWithTag("about-easter-egg-input").assertIsDisplayed()
        app.onNodeWithTag("about-easter-egg-dismiss").performClick()
        app.runOnIdle { dark = true }
        capture("coui-about-dark")
        app.runOnIdle { fontScale = 1.5f }
        capture("coui-about-large-font")
        app.onNodeWithTag("about-list").performScrollToNode(hasTestTag("about-support-qr"))
        app.onNodeWithTag("about-support-qr").assertIsDisplayed()
        val qr = app.onNodeWithTag("about-support-qr").getUnclippedBoundsInRoot()
        assertTrue(kotlin.math.abs((qr.right - qr.left).value - (qr.bottom - qr.top).value) < 1f)
    }

    private fun capture(name: String) {
        app.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.filesDir, "coui-about-previews").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use {
            app.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
