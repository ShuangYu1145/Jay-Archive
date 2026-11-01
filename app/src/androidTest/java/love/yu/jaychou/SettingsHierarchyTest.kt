package love.yu.jaychou

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import love.yu.jaychou.data.*
import love.yu.jaychou.navigation.ArchiveApp
import love.yu.jaychou.ui.screens.settings.SettingsScreen
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SettingsHierarchyTest {
    @get:Rule val app = createComposeRule()

    @Test fun themePageRestoresAndKeepsOptionsAcrossStyleChanges() {
        AppText.update(InstrumentationRegistry.getInstrumentation().targetContext.resources)
        var settings by mutableStateOf(AppSettings(blur = false, liquidGlass = false))
        val restoration = StateRestorationTester(app)
        restoration.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(400.dp, 800.dp))) {
            ArchiveTheme(settings) {
                SettingsScreen(settings, rememberLazyListState(), 24.dp,
                    {}, {}, {}, {}, {}, {}, onUiStyle = { settings = settings.copy(uiStyle = it) })
            }
            }
        }
        app.onNodeWithTag("app-language").assertIsDisplayed()
        app.onNodeWithTag("theme-mode").assertDoesNotExist()
        app.onNodeWithTag("theme-settings").performClick()
        restoration.emulateSavedInstanceStateRestore()
        app.onNodeWithTag("settings-back").assertIsDisplayed()
        for (style in UiStyle.entries) {
            app.runOnIdle { settings = settings.copy(uiStyle = style) }
            app.onNodeWithTag("settings-back").assertIsDisplayed()
            app.onNodeWithTag("settings-list").performScrollToIndex(7)
            app.onNodeWithTag("custom-background").assertIsDisplayed()
        }
        app.onNodeWithTag("settings-back").performClick()
        app.onNodeWithTag("app-language").assertIsDisplayed()
        app.onNodeWithTag("motion-settings").performClick()
        app.onNodeWithTag("settings-back").assertIsDisplayed()
        app.onNodeWithTag("app-language").assertDoesNotExist()
        app.onNodeWithTag("settings-back").performClick()
        app.onNodeWithTag("theme-settings").assertIsDisplayed()
    }

    @Test fun materialThemeUsesGroupedRadioChoices() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val settings = AppSettings(uiStyle = UiStyle.MATERIAL3, theme = ThemeMode.DARK,
            blur = false, liquidGlass = false)
        app.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(400.dp, 800.dp))) {
                ArchiveTheme(settings) {
                    SettingsScreen(settings, rememberLazyListState(), 24.dp, {}, {}, {}, {}, {}, {})
                }
            }
        }
        app.onNodeWithTag("theme-settings").performClick()
        app.onNodeWithTag("ui-style-MATERIAL3").assertIsSelected()
        app.onNodeWithTag("settings-back").assertIsDisplayed()
        val directory = java.io.File(context.getExternalFilesDir(null), "secondary-pages").apply { mkdirs() }
        java.io.File(directory, "gd3-theme.png").outputStream().use {
            app.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun landscapeThemeHidesRailAndBackRestoresSettings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val catalog = AlbumRepository.load(context)
        val settings = AppSettings(uiStyle = UiStyle.COUI, blur = false, liquidGlass = false)
        app.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(800.dp, 360.dp))) {
                ArchiveTheme(settings) {
                    ArchiveApp(catalog, settings, {}, {}, {}, {}, {}, {}, {}, null, {})
                }
            }
        }
        app.onNodeWithTag("tab-SETTINGS").performClick()
        app.onNodeWithTag("theme-settings").performClick()
        app.onNodeWithTag("side-navigation").assertDoesNotExist()
        app.onNodeWithTag("settings-list").performScrollToIndex(7)
        app.onNodeWithTag("custom-background").assertIsDisplayed()
        app.onNodeWithTag("settings-back").performClick()
        app.onNodeWithTag("side-navigation").assertIsDisplayed()
        app.onNodeWithTag("tab-SETTINGS").assertIsSelected()
        app.onNodeWithTag("app-language").assertIsDisplayed()
    }
}
