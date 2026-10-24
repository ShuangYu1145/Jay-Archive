package love.yu.jaychou

import love.yu.jaychou.ui.components.LocalArchiveBlur
import love.yu.jaychou.ui.effects.GlassLayer
import love.yu.jaychou.ui.effects.LocalArchiveLiquidGlass
import love.yu.jaychou.ui.navigation.ArchiveBottomNavigationBar
import love.yu.jaychou.ui.screens.settings.SettingsScreen
import love.yu.jaychou.ui.theme.ArchiveTheme
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import love.yu.jaychou.data.AppSettings
import love.yu.jaychou.data.AppText
import love.yu.jaychou.data.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class BottomNavigationModesTest {
    @get:Rule val app = createComposeRule()

    @Test fun nativeBarsBlurAndModeChangesKeepSelection() {
        assumeTrue(Build.VERSION.SDK_INT >= 33)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val settings = mutableStateOf(AppSettings())
        app.mainClock.autoAdvance = false
        app.setContent {
            val current = settings.value
            var selected by remember { mutableIntStateOf(0) }
            ArchiveTheme(current) {
                CompositionLocalProvider(
                    LocalArchiveBlur provides current.blur,
                    LocalArchiveLiquidGlass provides (current.liquidGlass && current.floatingBottomBar),
                ) {
                    GlassLayer(MiuixBackground) { capture, _ ->
                        Box(Modifier.fillMaxSize()) {
                            Canvas(Modifier.fillMaxSize().then(capture)) {
                                val stripe = 6.dp.toPx()
                                repeat((size.width / stripe).toInt() + 1) { index ->
                                    drawRect(if (index % 2 == 0) Color.Red else Color.Blue,
                                        Offset(index * stripe, 0f), Size(stripe, size.height))
                                }
                            }
                            ArchiveBottomNavigationBar(current, selected, { selected.toFloat() },
                                { selected = it }, {}, true, Modifier.align(Alignment.BottomCenter))
                        }
                    }
                }
            }
        }
        settle()
        app.onNodeWithTag("tab-ABOUT").performClick()
        settle()

        for (floating in listOf(true, false)) {
            var solid: Bitmap? = null
            for (blur in listOf(false, true)) {
                app.runOnIdle { settings.value = AppSettings(floatingBottomBar = floating, liquidGlass = false, blur = blur) }
                settle()
                app.onNodeWithTag("tab-ABOUT").assertIsSelected()
                app.onNodeWithTag("navigation-indicator").assertDoesNotExist()
                val tag = if (floating) "floating-navigation" else "standard-navigation"
                val bitmap = screenshot(tag, "${if (floating) "floating" else "standard"}-${if (blur) "blur" else "solid"}.png")
                if (!blur) solid = bitmap else {
                    // Sample the bar surface above the icons, between the middle two items.
                    val before = solid!!
                    val x = bitmap.width / 2
                    val y = 5
                    val a = before.getPixel(x, y)
                    val b = bitmap.getPixel(x, y)
                    val delta = listOf(16, 8, 0).sumOf { shift ->
                        kotlin.math.abs(((a shr shift) and 255) - ((b shr shift) and 255))
                    }
                    assertTrue("$tag must actually sample the colored background when blur is on: $delta", delta > 8)
                }
                app.onNodeWithTag("tab-SETTINGS").performClick()
                settle()
                app.onNodeWithTag("tab-SETTINGS").assertIsSelected()
                app.onNodeWithTag("tab-ABOUT").performClick()
                settle()
            }
        }
        // Liquid needs both switches, and keeps its material when general blur is off.
        for (blur in listOf(false, true)) {
            app.runOnIdle { settings.value = AppSettings(blur = blur) }
            settle()
            app.onNodeWithTag("navigation-indicator").assertIsDisplayed()
            app.onNodeWithTag("tab-ABOUT").assertIsSelected()
            screenshot("floating-navigation", "liquid-${if (blur) "blur" else "solid"}.png")
        }
        app.runOnIdle { settings.value = AppSettings(floatingBottomBar = false, liquidGlass = true) }
        settle()
        app.onNodeWithTag("standard-navigation").assertIsDisplayed()
        app.onNodeWithTag("navigation-indicator").assertDoesNotExist()
        app.onNodeWithTag("tab-ABOUT").assertIsSelected()
    }

    @Test fun settingsSwitchesPersistIndependently() {
        assumeTrue(Build.VERSION.SDK_INT >= 33)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val repository = SettingsRepository(context)
        val original = runBlocking { repository.settings.first() }
        try {
            runBlocking {
                repository.setLiquidGlass(true)
                repository.setFloatingBottomBar(true)
            }
            app.setContent {
                val settings by repository.settings.collectAsState(initial = original.copy(liquidGlass = true, floatingBottomBar = true))
                val scope = rememberCoroutineScope()
                ArchiveTheme(settings) {
                    SettingsScreen(settings, rememberLazyListState(), 24.dp, {}, {}, {}, {}, {}, {},
                        onLiquidGlass = { value -> scope.launch { repository.setLiquidGlass(value) } },
                        onFloatingBottomBar = { value -> scope.launch { repository.setFloatingBottomBar(value) } })
                }
            }
            val liquid = AppText.get(R.string.ui_liquid_glass)
            val floating = AppText.get(R.string.ui_floating_navigation_bar)
            app.onNodeWithContentDescription(liquid).performScrollTo().performClick()
            app.waitUntil(5_000) {
                app.onNodeWithContentDescription(liquid).fetchSemanticsNode()
                    .config[SemanticsProperties.ToggleableState] == ToggleableState.Off
            }
            app.onNodeWithContentDescription(liquid).assertIsOff()
            app.onNodeWithContentDescription(floating).performScrollTo().performClick()
            app.waitUntil(5_000) {
                app.onNodeWithContentDescription(floating).fetchSemanticsNode()
                    .config[SemanticsProperties.ToggleableState] == ToggleableState.Off
            }
            app.onNodeWithContentDescription(floating).assertIsOff()
            val persisted = runBlocking { SettingsRepository(context).settings.first() }
            assertEquals(original.copy(liquidGlass = false, floatingBottomBar = false), persisted)
        } finally {
            runBlocking {
                repository.setLiquidGlass(original.liquidGlass)
                repository.setFloatingBottomBar(original.floatingBottomBar)
            }
        }
    }

    private fun settle() {
        app.mainClock.advanceTimeBy(1_500)
        app.waitForIdle()
    }

    private fun screenshot(tag: String, name: String): Bitmap {
        val bitmap = app.onNodeWithTag(tag).captureToImage().asAndroidBitmap()
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "bottom-modes").apply { mkdirs() }
        File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bitmap
    }
}

private val MiuixBackground = Color(0xFFF6F6F8)
