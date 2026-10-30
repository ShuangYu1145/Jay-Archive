package love.yu.jaychou

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.theme.ArchiveTheme
import love.yu.jaychou.ui.updates.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class CouiDialogLayoutTest {
    @get:Rule val app = createComposeRule()

    @Test fun messagesHaveInsetsAndLongNotesLeaveActionsVisibleAcrossThemesAndSizes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var theme by mutableStateOf(ThemeMode.LIGHT)
        var notice by mutableStateOf(UpdateNotice(result = UpdateResult.UpToDate, manual = true))
        var visible by mutableStateOf(true)
        app.setContent {
                ArchiveTheme(AppSettings(uiStyle = UiStyle.COUI, theme = theme)) {
                    if (visible) UpdateDialog(notice, false, false, null,
                        onDismiss = { visible = false }, onDownload = { visible = false }, onDisableAutomatic = {})
                }
        }
        val notes = (1..60).joinToString("\n") { "更新说明第 $it 行：检查正文留白与滚动，按钮始终可用。" }
        val cases = listOf(
            UpdateNotice(result = UpdateResult.UpToDate, manual = true),
            UpdateNotice(problem = UpdateProblem.NETWORK, manual = true),
            UpdateNotice(result = UpdateResult.Available(AppUpdate("99.0", 99, notes,
                "https://example.com/app.apk", "https://example.com/release", false)), manual = true),
        )
        // Run in the device's actual window: WindowDialog owns a separate Compose root.
            for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
                cases.forEachIndexed { index, value ->
                    app.runOnIdle { theme = mode; notice = value; visible = true }
                    val panel = app.onNodeWithTag("update-dialog-coui").getUnclippedBoundsInRoot()
                    val messageNode = if (index == 2) app.onNodeWithTag("update-message") else app.onNodeWithText(
                        AppText.get(if (index == 0) R.string.ui_update_current_version else R.string.ui_update_network_error,
                            *if (index == 0) arrayOf(BuildConfig.VERSION_NAME) else emptyArray()))
                    val message = messageNode.getUnclippedBoundsInRoot()
                    assertTrue("Message needs left and right insets", message.left >= panel.left + 20.dp && message.right <= panel.right - 20.dp)
                    val actions = app.onNodeWithTag("update-actions-coui").getUnclippedBoundsInRoot()
                    assertTrue("Actions must stay inside the panel", actions.top >= panel.top && actions.bottom <= panel.bottom + 1.dp)
                    val confirm = app.onNodeWithText(AppText.get(if (index == 2) R.string.ui_update_download else R.string.ui_ok))
                    confirm.assertIsDisplayed()
                    if (index == 2) {
                        app.onNode(hasScrollAction()).performSemanticsAction(
                            androidx.compose.ui.semantics.SemanticsActions.ScrollBy) { it(0f, 500f) }
                        confirm.assertIsDisplayed()
                    }
                    val folder = File(context.getExternalFilesDir(null), "coui-dialog-fix").apply { mkdirs() }
                    File(folder, "${context.resources.displayMetrics.widthPixels}-$mode-$index.png").outputStream().use {
                        app.onNodeWithTag("update-dialog-coui").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    confirm.performClick()
                    app.onNodeWithTag("update-dialog-coui").assertDoesNotExist()
                }
            }
    }
}
