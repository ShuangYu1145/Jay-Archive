package love.yu.jaychou

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.effects.CustomBackgroundHost
import love.yu.jaychou.ui.screens.settings.SettingsScreen
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class BackgroundCustomizationTest {
    @get:Rule val app = createComposeRule()

    @Test fun importBlurRestoreAndResetKeepToolbarBlurIndependent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val repository = SettingsRepository(context)
        val original = runBlocking { repository.settings.first() }
        val source = File(context.cacheDir, "background-test.png")
        val bitmap = Bitmap.createBitmap(512, 1024, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        for (x in 0 until 512 step 8) {
            paint.color = if ((x / 8) % 2 == 0) android.graphics.Color.rgb(255, 80, 80) else android.graphics.Color.rgb(30, 110, 240)
            canvas.drawRect(x.toFloat(), 0f, (x + 8).toFloat(), 1024f, paint)
        }
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        var imported: String? = null
        var pickerUri = Uri.fromFile(source)
        try {
            runBlocking {
                repository.setUiStyle(UiStyle.MIUIX)
                repository.setBlur(false)
                repository.setCustomBackground(null)
                repository.setBackgroundCardBlur(false)
            }
            val registry = object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I,
                    options: ActivityOptionsCompat?) {
                    dispatchResult(requestCode, Activity.RESULT_OK, Intent().setData(pickerUri))
                }
            }
            val owner = object : ActivityResultRegistryOwner { override val activityResultRegistry = registry }
            val restoration = StateRestorationTester(app)
            restoration.setContent {
                val settings by repository.settings.collectAsState(initial = AppSettings(blur = false))
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                    ArchiveTheme(settings) {
                        CustomBackgroundHost(settings) {
                            SettingsScreen(settings, rememberLazyListState(), 24.dp,
                                {}, {}, {}, {}, {}, {})
                        }
                    }
                }
            }
            app.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("custom-background"))
            app.onNodeWithTag("custom-background").performClick()
            app.waitUntil(10_000) { runBlocking { repository.settings.first().customBackgroundPath != null } }
            imported = runBlocking { repository.settings.first().customBackgroundPath }
            assertTrue(File(imported!!).isFile)
            assertTrue(source.delete())
            app.onNodeWithTag("custom-background-image").assertExists()

            val blurLabel = AppText.get(R.string.ui_background_card_blur)
            app.onNodeWithTag("settings-list").performScrollToNode(hasContentDescription(blurLabel))
            app.onNodeWithContentDescription(blurLabel).performClick()
            app.waitUntil(5_000) { runBlocking { repository.settings.first().backgroundCardBlur } }
            assertFalse(runBlocking { repository.settings.first().blur })
            restoration.emulateSavedInstanceStateRestore()
            app.onNodeWithTag("custom-background-image").assertExists()
            assertTrue(runBlocking { SettingsRepository(context).settings.first().backgroundCardBlur })
            val screenshot = File(context.getExternalFilesDir(null), "custom-background-blur.png")
            screenshot.outputStream().use {
                app.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
            }

            // A failed replacement must leave the selected wallpaper intact.
            val invalid = File(context.cacheDir, "invalid-background.txt").apply { writeText("invalid image") }
            pickerUri = Uri.fromFile(invalid)
            app.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("custom-background"))
            app.onNodeWithTag("custom-background").performClick()
            app.waitUntil(5_000) { app.onAllNodesWithTag("custom-background-error").fetchSemanticsNodes().isNotEmpty() }
            assertEquals(imported, runBlocking { repository.settings.first().customBackgroundPath })
            assertTrue(File(imported).isFile)
            invalid.delete()

            app.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("custom-background-reset"))
            app.onNodeWithTag("custom-background-reset").performClick()
            app.waitUntil(5_000) { runBlocking { repository.settings.first().customBackgroundPath == null } }
            app.waitUntil(5_000) { !File(imported).exists() }
            app.onNodeWithTag("custom-background-image").assertDoesNotExist()
            assertFalse(runBlocking { repository.settings.first().blur })
        } finally {
            runBlocking {
                repository.setCustomBackground(original.customBackgroundPath)
                repository.setBackgroundCardBlur(original.backgroundCardBlur)
                repository.setBlur(original.blur)
                repository.setUiStyle(original.uiStyle)
                BackgroundImageStore.remove(context, imported)
            }
            source.delete()
        }
    }
}
