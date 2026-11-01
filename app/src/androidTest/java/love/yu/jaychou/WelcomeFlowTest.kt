package love.yu.jaychou

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.screens.welcome.FirstRunHost
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class WelcomeFlowTest {
    @get:Rule val app = createComposeRule()

    @Test fun consentRestorationThemePreviewAndSaveRetryGateTheApp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val repository = SettingsRepository(context)
        val settings = mutableStateOf(AppSettings(blur = false, liquidGlass = false))
        var entries = 0
        var attempts = 0
        val restoration = StateRestorationTester(app)
        app.mainClock.autoAdvance = false
        restoration.setContent {
            ArchiveTheme(settings.value) {
                FirstRunHost(settings.value, { style, theme ->
                    attempts++
                    if (attempts == 1) throw IOException("Test save failure")
                    repository.completeOnboarding(style, theme)
                    settings.value = repository.settings.first()
                }) {
                    DisposableEffect(Unit) { entries++; onDispose {} }
                    Box(Modifier.testTag("welcome-test-home"))
                }
            }
        }
        settle()
        assertEquals(0, entries)
        snapshot("welcome")
        next()
        app.onNodeWithTag("welcome-next").assertIsNotEnabled()
        app.onNodeWithTag("welcome-agreement").assertIsOff()
        snapshot("statement")
        app.onNodeWithTag("welcome-policy").performClick()
        settle()
        app.onNodeWithTag("welcome-step-4").assertIsDisplayed()
        next()
        app.onNodeWithTag("welcome-next").assertIsNotEnabled()
        app.onNodeWithTag("welcome-agreement").performClick()
        restoration.emulateSavedInstanceStateRestore()
        settle()
        app.onNodeWithTag("welcome-agreement").assertIsOn()
        next()
        for (style in UiStyle.entries) {
            app.onNodeWithTag("welcome-style-${style.name}").performScrollTo().performClick()
            settle()
            app.onNodeWithTag("welcome-style-${style.name}").assertIsSelected()
            for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
                app.onNodeWithTag("welcome-theme-${theme.name}").performScrollTo().performClick()
                settle()
                app.onNodeWithTag("welcome-theme-${theme.name}").assertIsSelected()
                snapshot("${style.name}-${theme.name}")
            }
        }
        next()
        app.onNodeWithTag("welcome-step-3").assertIsDisplayed()
        app.onNodeWithTag("welcome-previous").performClick()
        settle()
        app.onNodeWithTag("welcome-style-MATERIAL3").assertIsSelected()
        app.onNodeWithTag("welcome-theme-DARK").assertIsSelected()
        next()
        snapshot("ready")
        next()
        app.onNodeWithText(AppText.get(R.string.ui_unable_to_save_settings_please_try_again)).assertIsDisplayed()
        assertEquals(0, entries)
        app.onNodeWithTag("welcome-next").performClick()
        app.waitUntil(10_000) { settings.value.onboardingCompleted }
        app.mainClock.advanceTimeBy(160)
        app.waitForIdle()
        app.onNodeWithTag("welcome-home-reveal").assertExists()
        app.onNodeWithTag("welcome-screen").assertExists()
        app.onNodeWithTag("welcome-test-home").assertExists()
        assertEquals(1, entries)
        snapshot("home-reveal")
        settle()
        app.onNodeWithTag("welcome-home-reveal").assertDoesNotExist()
        app.onNodeWithTag("welcome-screen").assertDoesNotExist()
        app.onNodeWithTag("welcome-test-home").assertExists()
        assertEquals(1, entries)
        assertEquals(UiStyle.MATERIAL3, settings.value.uiStyle)
        assertEquals(ThemeMode.DARK, settings.value.theme)
        val persisted = runBlocking { SettingsRepository(context).settings.first() }
        assertTrue(persisted.onboardingCompleted)
        assertEquals(UiStyle.MATERIAL3, persisted.uiStyle)
        assertEquals(ThemeMode.DARK, persisted.theme)
        restoration.emulateSavedInstanceStateRestore()
        settle()
        app.onNodeWithTag("welcome-screen").assertDoesNotExist()
    }

    private fun next() { app.onNodeWithTag("welcome-next").performClick(); settle() }
    private fun settle() { app.mainClock.advanceTimeBy(800); app.waitForIdle() }
    private fun snapshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "welcome-checks").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            app.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
