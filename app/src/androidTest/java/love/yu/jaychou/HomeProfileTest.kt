package love.yu.jaychou

import love.yu.jaychou.ui.screens.home.HomeProfileButton
import love.yu.jaychou.ui.theme.ArchiveTheme
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import love.yu.jaychou.data.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class HomeProfileTest {
    @get:Rule val app = createComposeRule()

    @Test fun profileEditsCancelSaveImportAndRestoreWithoutSourcePhoto() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val repository = SettingsRepository(context)
        val original = runBlocking { repository.settings.first() }
        val source = File(context.cacheDir, "profile-test.png")
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.MAGENTA)
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        var imported: String? = null
        try {
            runBlocking { repository.setProfile("", null) }
            val registry = object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I,
                    options: ActivityOptionsCompat?) {
                    dispatchResult(requestCode, Activity.RESULT_OK, Intent().setData(Uri.fromFile(source)))
                }
            }
            val owner = object : ActivityResultRegistryOwner { override val activityResultRegistry = registry }
            val restoration = StateRestorationTester(app)
            restoration.setContent {
                val profile by repository.settings.collectAsState(initial = AppSettings())
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                    ArchiveTheme(profile) { Box(Modifier.fillMaxSize()) { HomeProfileButton(profile) } }
                }
            }
            app.onNodeWithTag("home-profile").performClick()
            app.onNodeWithTag("profile-name-input").performTextReplacement("取消的名字")
            app.onNodeWithText(AppText.get(R.string.ui_cancel)).performClick()
            assertEquals("", runBlocking { repository.settings.first().userName })
            app.onNodeWithTag("home-profile").performClick()
            app.onNodeWithTag("profile-name-input").performTextReplacement("  小杰迷  ")
            app.onNodeWithTag("profile-choose-avatar").performClick()
            app.waitUntil(5_000) { app.onAllNodesWithTag("profile-avatar-image").fetchSemanticsNodes().isNotEmpty() }
            app.onNodeWithTag("profile-save").performClick()
            app.waitUntil(5_000) { runBlocking { repository.settings.first().userName == "小杰迷" } }
            imported = runBlocking { repository.settings.first().userAvatarPath }
            assertNotNull(imported)
            assertTrue(File(imported!!).exists())
            assertTrue(source.delete())
            restoration.emulateSavedInstanceStateRestore()
            app.onNodeWithTag("home-profile").assertIsDisplayed()
            app.onNodeWithText("小杰迷").assertIsDisplayed()
            app.onNode(hasTestTag("profile-avatar-image") and hasAnyAncestor(hasTestTag("home-profile")),
                useUnmergedTree = true).assertIsDisplayed()
            val screenshot = File(context.getExternalFilesDir(null), "home-profile.png")
            screenshot.outputStream().use {
                app.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            app.onNodeWithTag("home-profile").performClick()
            app.onNodeWithTag("profile-remove-avatar").performClick()
            app.onNodeWithTag("profile-save").performClick()
            app.waitUntil(5_000) { runBlocking { repository.settings.first().userAvatarPath == null } }
            app.waitUntil(5_000) { !File(imported).exists() }
        } finally {
            runBlocking {
                repository.setProfile(original.userName, original.userAvatarPath)
                UserAvatarStore.remove(context, imported)
            }
            source.delete()
        }
    }
}
