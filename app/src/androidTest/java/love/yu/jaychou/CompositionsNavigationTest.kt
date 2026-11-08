package love.yu.jaychou

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import love.yu.jaychou.data.*
import love.yu.jaychou.navigation.ArchiveApp
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.junit.Rule
import org.junit.Test

class CompositionsNavigationTest {
    @get:Rule val app = createComposeRule()

    @Test fun introductionOnlyContainsAlbumsAndConcertsAcrossStyles() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val catalog = AlbumRepository.load(context)
        val settings = mutableStateOf(AppSettings(blur = false, liquidGlass = false))
        app.setContent {
            ArchiveTheme(settings.value) { ArchiveApp(catalog, settings.value, {}, {}, {}, {}, {}, {}, {}, null, {}) }
        }
        app.onNodeWithTag("tab-INTRODUCTION").performClick()
        for (style in UiStyle.entries) {
            app.runOnIdle { settings.value = settings.value.copy(uiStyle = style) }
            app.onNodeWithTag("tab-COMPOSITIONS").assertDoesNotExist()
            app.onNodeWithTag("page-COMPOSITIONS").assertDoesNotExist()
            app.onNodeWithTag("tab-ALBUMS").performClick()
            app.onNodeWithTag("tab-ALBUMS").assertIsSelected()
            app.onNodeWithTag("tab-CONCERTS").performClick()
            app.onNodeWithTag("tab-CONCERTS").assertIsSelected()
            app.onNodeWithTag("introduction-pager").performTouchInput { swipeLeft() }
            app.onNodeWithTag("tab-CONCERTS").assertIsSelected()
        }
    }
}
