package love.yu.jaychou

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import love.yu.jaychou.ui.theme.ArchiveTheme
import love.yu.jaychou.ui.updates.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class UpdateFlowTest {
    @get:Rule val app = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun releaseJson(name: String, metadata: Boolean = true): String {
        val base = "https://github.com/ShuangYu1145/Jay-Archive/releases/"
        val apk = if (BuildConfig.DEBUG) "app-debug.apk" else "app-release.apk"
        val assets = JSONArray().put(JSONObject().put("name", apk)
            .put("browser_download_url", "${base}download/$name/$apk"))
        if (metadata) assets.put(JSONObject().put("name", "update.json")
            .put("browser_download_url", "${base}download/$name/update.json"))
        return JSONArray().put(JSONObject().put("tag_name", name).put("html_url", "${base}tag/$name")
            .put("prerelease", true).put("body", "更新功能界面测试\n版本信息与下载入口")
            .put("assets", assets)).toString()
    }
    private fun waitForDialog(tag: String) {
        app.waitUntil(5_000) { app.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        app.onNodeWithTag(tag).assertIsDisplayed()
    }
    private fun screenshot(tag: String, name: String) {
        File(context.getExternalFilesDir(null), name).outputStream().use {
            app.onNodeWithTag(tag).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun manualChecksFollowThemeAndAutomaticOffSurvivesRecreation() {
        val store = SettingsRepository(context)
        val original = runBlocking { store.settings.first() }
        val calls = AtomicInteger()
        val downloaded = AtomicReference<String?>()
        val style = mutableStateOf(UiStyle.MIUIX)
        val repository = UpdateRepository { url ->
            if (url == UpdateRepository.releasesUrl) { calls.incrementAndGet(); releaseJson("99.0") }
            else """{"versionCode":${BuildConfig.VERSION_CODE + 1},"versionName":"99.0","minSdk":26}"""
        }
        try {
            runBlocking {
                store.setAutoCheckUpdates(false)
                store.setIncludePrereleaseUpdates(true)
                store.recordUpdateCheck(0)
            }
            val initial = runBlocking { store.settings.first() }
            val restoration = StateRestorationTester(app)
            restoration.setContent {
                val saved by store.settings.collectAsState(initial = initial)
                val settings = saved.copy(uiStyle = style.value)
                ArchiveTheme(settings) {
                    CompositionLocalProvider(LocalUriHandler provides object : UriHandler {
                        override fun openUri(uri: String) { downloaded.set(uri) }
                    }) {
                        UpdateHost(settings, repository = repository, automaticDelayMillis = 0) {
                            Box(Modifier.fillMaxSize().background(ArchiveUiTheme.colorScheme.background)) {
                                UpdatePreferences(settings)
                            }
                        }
                    }
                }
            }
            app.waitForIdle()
            assertEquals(0, calls.get())
            app.onNodeWithTag("check-updates").performClick()
            waitForDialog("update-dialog-miuix")
            screenshot("update-dialog-miuix", "update-miuix.png")
            app.onNodeWithTag("update-confirm").performClick()
            app.waitUntil(5_000) { downloaded.get() != null }
            assertTrue(downloaded.get()!!.endsWith(if (BuildConfig.DEBUG) "/app-debug.apk" else "/app-release.apk"))

            app.runOnIdle { style.value = UiStyle.MATERIAL3 }
            app.onNodeWithTag("check-updates").performClick()
            waitForDialog("update-dialog-material")
            screenshot("update-dialog-material", "update-material.png")
            app.onNodeWithTag("update-dismiss").performClick()

            app.runOnIdle { style.value = UiStyle.COUI }
            app.onNodeWithTag("check-updates").performClick()
            waitForDialog("update-dialog-coui")
            screenshot("update-dialog-coui", "update-coui.png")
            app.onNodeWithText(AppText.get(R.string.ui_update_later)).performClick()
            app.runOnIdle { style.value = UiStyle.MATERIAL3 }

            // Reset the throttle before enabling automatic checks.
            runBlocking { store.recordUpdateCheck(0) }
            app.onNodeWithContentDescription(AppText.get(R.string.ui_update_auto_check)).performClick()
            waitForDialog("update-dialog-material")
            app.onNodeWithTag("update-disable-automatic").performClick()
            app.waitUntil(5_000) { !runBlocking { store.settings.first().autoCheckUpdates } }
            app.waitUntil(5_000) { app.onAllNodesWithTag("update-dialog-material").fetchSemanticsNodes().isEmpty() }
            val previousCalls = calls.get()
            restoration.emulateSavedInstanceStateRestore()
            app.waitForIdle()
            assertFalse(runBlocking { SettingsRepository(context).settings.first().autoCheckUpdates })
            assertEquals(previousCalls, calls.get())
            app.onNodeWithTag("check-updates").performClick()
            waitForDialog("update-dialog-material")
            assertEquals(previousCalls + 1, calls.get())
        } finally {
            runBlocking {
                store.setAutoCheckUpdates(original.autoCheckUpdates)
                store.setIncludePrereleaseUpdates(original.includePrereleaseUpdates)
                store.recordUpdateCheck(original.lastUpdateCheckAt)
            }
        }
    }

    @Test fun automaticNoUpdateStaysQuietAndManualNetworkFailureIsVisible() {
        val calls = AtomicInteger()
        val failed = AtomicBoolean(false)
        val settings = mutableStateOf(AppSettings(autoCheckUpdates = true))
        val currentController = AtomicReference<UpdateController?>()
        val repository = UpdateRepository {
            calls.incrementAndGet()
            if (failed.get()) throw IOException("Offline fixture")
            releaseJson(BuildConfig.VERSION_NAME, metadata = false)
        }
        app.setContent {
            ArchiveTheme(settings.value) {
                UpdateHost(settings.value, repository = repository,
                    recordAttempt = { settings.value = settings.value.copy(lastUpdateCheckAt = it) },
                    automaticDelayMillis = 0) {
                    currentController.set(LocalUpdateController.current)
                    UpdateCheckRow()
                }
            }
        }
        app.waitUntil(5_000) { calls.get() == 1 && currentController.get()?.checking == false }
        app.onAllNodesWithTag("update-dialog-miuix").assertCountEquals(0)
        app.onNodeWithTag("check-updates").performClick()
        waitForDialog("update-dialog-miuix")
        app.onNodeWithText(AppText.get(R.string.ui_update_latest)).assertIsDisplayed()
        app.onNodeWithTag("update-confirm").performClick()
        failed.set(true)
        app.onNodeWithTag("check-updates").performClick()
        waitForDialog("update-dialog-miuix")
        app.onNodeWithText(AppText.get(R.string.ui_update_failed)).assertIsDisplayed()
        app.onNodeWithText(AppText.get(R.string.ui_update_network_error)).assertIsDisplayed()
    }
}
