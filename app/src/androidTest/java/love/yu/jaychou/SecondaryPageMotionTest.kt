package love.yu.jaychou

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.BackEventCompat
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.animation.*
import love.yu.jaychou.ui.designsystem.TextButton
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class SecondaryPageMotionTest {
    @get:Rule val app = createAndroidComposeRule<ComponentActivity>()

    @Test fun predictiveCancelKeepsPageAndCommitReturnsForEveryTheme() {
        assumeTrue(Build.VERSION.SDK_INT >= 34)
        var style by mutableStateOf(UiStyle.MATERIAL3)
        var animation by mutableStateOf(PredictiveBackAnimation.SCALE)
        var selected by mutableStateOf<String?>(null)
        var visible = false
        app.setContent {
            ArchiveTheme(AppSettings(uiStyle = style, blur = false, liquidGlass = false)) {
                CompositionLocalProvider(LocalSecondaryBackAnimation provides animation) {
                    SecondaryPageHost(selected, onBack = { selected = null },
                        onVisibilityChanged = { visible = it },
                        root = { TextButton("Open", { selected = "detail" }) },
                        content = { TextButton("Close", { selected = null }) })
                }
            }
        }
        for (theme in UiStyle.entries) for (motion in PredictiveBackAnimation.entries.filter { it != PredictiveBackAnimation.NONE }) {
            app.runOnIdle { style = theme; animation = motion }
            app.onNodeWithText("Open").performClick()
            app.onNodeWithText("Close").assertIsDisplayed()
            app.runOnIdle {
                val dispatcher = app.activity.onBackPressedDispatcher
                dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 400f, 0f, 0))
                dispatcher.dispatchOnBackProgressed(BackEventCompat(160f, 420f, .6f, 0))
            }
            app.waitForIdle()
            app.runOnIdle { app.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
            app.onNodeWithText("Close").assertIsDisplayed()
            app.runOnIdle { assertTrue(visible) }
            app.runOnIdle {
                val dispatcher = app.activity.onBackPressedDispatcher
                dispatcher.dispatchOnBackStarted(BackEventCompat(400f, 400f, 0f, 1))
                dispatcher.dispatchOnBackProgressed(BackEventCompat(180f, 420f, .8f, 1))
            }
            app.waitForIdle()
            app.runOnIdle { app.activity.onBackPressedDispatcher.onBackPressed() }
            app.onNodeWithText("Open").assertIsDisplayed()
            app.onNodeWithText("Close").assertDoesNotExist()
        }
    }
}
