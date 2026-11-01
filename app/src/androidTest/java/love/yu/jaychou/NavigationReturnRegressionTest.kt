package love.yu.jaychou

import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import love.yu.jaychou.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import love.yu.jaychou.navigation.ArchiveApp
import love.yu.jaychou.ui.animation.*
import love.yu.jaychou.ui.designsystem.TextButton
import love.yu.jaychou.ui.screens.settings.SettingsScreen
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class NavigationReturnRegressionTest {
    @get:Rule val app = createAndroidComposeRule<ComponentActivity>()

    @Test fun exitDirectionPersistsWithoutChangingOtherSettings() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = SettingsRepository(context)
        val saved = repository.settings.first()
        try {
            for (policy in PredictiveBackExitDirection.entries) {
                repository.setPredictiveBackExitDirection(policy)
                val restored = SettingsRepository(context).settings.first()
                assertEquals(saved.copy(predictiveBackExitDirection = policy), restored)
            }
        } finally { repository.setPredictiveBackExitDirection(saved.predictiveBackExitDirection) }
    }

    @Test fun themeGesturesScrollBothWaysWithoutMovingTheToolbar() {
        AppText.update(InstrumentationRegistry.getInstrumentation().targetContext.resources)
        var style by mutableStateOf(UiStyle.MIUIX)
        app.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(400.dp, 800.dp))) {
                val settings = AppSettings(uiStyle = style, blur = false, liquidGlass = false)
                ArchiveTheme(settings) {
                    SettingsScreen(settings, rememberLazyListState(), 24.dp, {}, {}, {}, {}, {}, {})
                }
            }
        }
        app.onNodeWithTag("theme-settings").performClick()
        for (theme in UiStyle.entries) {
            app.runOnIdle { style = theme }
            val list = app.onNodeWithTag("settings-list")
            list.performScrollToIndex(0)
            val toolbar = app.onNodeWithTag("settings-back").getUnclippedBoundsInRoot()
            list.performTouchInput { swipeUp(durationMillis = 350) }
            val scrolled = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
            assertTrue("$theme should scroll content on the first swipe", scrolled > 0f)
            assertEquals(toolbar, app.onNodeWithTag("settings-back").getUnclippedBoundsInRoot())
            // Start below the fixed toolbar, which intentionally does not scroll the list.
            list.performTouchInput { swipe(Offset(centerX, height * .35f), Offset(centerX, height * .85f), 350) }
            val returned = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
            assertTrue("$theme should reverse scroll direction immediately: $scrolled -> $returned", returned < scrolled)
            assertEquals(toolbar, app.onNodeWithTag("settings-back").getUnclippedBoundsInRoot())
        }
    }

    @Test fun bottomBarSlidesUpOnEveryReturnInEveryStyle() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val catalog = AlbumRepository.load(context)
        var style by mutableStateOf(UiStyle.MIUIX)
        app.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(400.dp, 800.dp))) {
                val settings = AppSettings(uiStyle = style, blur = false, liquidGlass = false)
                ArchiveTheme(settings) { ArchiveApp(catalog, settings, {}, {}, {}, {}, {}, {}, {}, null, {}) }
            }
        }
        app.onNodeWithTag("tab-SETTINGS").performClick()
        for (theme in UiStyle.entries) {
            app.runOnIdle { style = theme }
            for (entry in listOf("theme-settings", "motion-settings")) {
                app.onNodeWithTag(entry).performClick()
                app.onNodeWithTag("bottom-navigation").assertDoesNotExist()
                app.mainClock.autoAdvance = false
                try {
                    app.onNodeWithTag("settings-back").performClick()
                    app.mainClock.advanceTimeBy(280)
                    val entering = app.onNodeWithTag("bottom-navigation").getUnclippedBoundsInRoot()
                    app.mainClock.advanceTimeBy(400)
                    val settled = app.onNodeWithTag("bottom-navigation").getUnclippedBoundsInRoot()
                    assertTrue("$theme/$entry bar must travel up after returning", entering.top > settled.top + 1.dp)
                } finally { app.mainClock.autoAdvance = true }
                app.onNodeWithTag("bottom-navigation").assertIsDisplayed()
            }
        }
    }

    @Test fun exitDirectionControlsBothGestureEdgesAndTheCommittedExit() {
        assumeTrue(Build.VERSION.SDK_INT >= 34)
        var page by mutableStateOf<String?>(null)
        var direction by mutableStateOf(PredictiveBackExitDirection.FOLLOW_GESTURE)
        app.setContent {
            ArchiveTheme(AppSettings(uiStyle = UiStyle.MATERIAL3)) {
                CompositionLocalProvider(LocalSecondaryExitDirection provides direction,
                    LocalSecondaryBackAnimation provides PredictiveBackAnimation.SCALE) {
                    SecondaryPageHost(page, { page = null },
                        root = { TextButton("Open", { page = "detail" }) },
                        content = { Box(Modifier.fillMaxSize().testTag("exit-page")) })
                }
            }
        }
        for (policy in PredictiveBackExitDirection.entries) for (edge in 0..1) {
            app.runOnIdle { direction = policy }
            app.onNodeWithText("Open").performClick()
            val origin = app.onNodeWithTag("exit-page").getUnclippedBoundsInRoot().left
            app.runOnIdle {
                val dispatcher = app.activity.onBackPressedDispatcher
                dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 400f, 0f, edge))
                dispatcher.dispatchOnBackProgressed(BackEventCompat(160f, 420f, .6f, edge))
            }
            app.waitForIdle()
            app.mainClock.autoAdvance = false
            try {
                app.runOnIdle { app.activity.onBackPressedDispatcher.onBackPressed() }
                app.mainClock.advanceTimeBy(112)
                val moved = app.onNodeWithTag("exit-page").getUnclippedBoundsInRoot().left - origin
                val sign = policy.sign(if (edge == 0) 1f else -1f)
                assertTrue("$policy from edge $edge should exit with sign $sign", moved.value * sign > 0f)
                app.mainClock.advanceTimeBy(400)
            } finally { app.mainClock.autoAdvance = true }
            app.onNodeWithText("Open").assertIsDisplayed()
        }
    }
}
