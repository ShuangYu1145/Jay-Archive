package love.yu.jaychou

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import love.yu.jaychou.data.*
import org.junit.Assert.*
import org.junit.Test

/** Real frames allow the app's first-frame splash gate to complete normally. */
class AppLanguageTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun allFiveLanguagesRecreateTheActivityAndKeepTheOpenAlbum() {
        val original = AppCompatDelegate.getApplicationLocales()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            fun select(language: AppLanguage) {
                instrumentation.runOnMainSync { language.select() }
                await {
                    var matches = false
                    scenario.onActivity { matches = AppLanguage.contentTag(it.resources.configuration.locales) == language.tag }
                    matches && instrumentation.uiAutomation.rootInActiveWindow != null
                }
            }
            try {
                select(AppLanguage.SIMPLIFIED)
                clickText("介绍")
                clickText("最伟大的作品")
                await { hasText("专辑档案") }
                listOf(
                    AppLanguage.SIMPLIFIED to "最伟大的作品",
                    AppLanguage.TRADITIONAL to "最偉大的作品",
                    AppLanguage.ENGLISH to "Greatest Works of Art",
                    AppLanguage.JAPANESE to "Greatest Works of Art",
                    AppLanguage.KOREAN to "Greatest Works of Art",
                ).forEach { (language, title) ->
                    select(language)
                    var detailHeading = ""
                    scenario.onActivity { activity ->
                        detailHeading = activity.getString(R.string.ui_album_archive)
                        val album = AlbumRepository.load(activity).find("1551833317039542272")!!
                        assertEquals(title, album.title)
                        assertEquals("最伟大的作品", album.originalTitle)
                        assertTrue(album.summary.isNotBlank())
                        if (language == AppLanguage.ENGLISH) {
                            assertEquals("1 track", AppText.get(R.string.ui_tracks, 1))
                            assertEquals("2 tracks", AppText.get(R.string.ui_tracks, 2))
                        }
                    }
                    await { hasText(title) && hasText(detailHeading) }
                    saveScreenshot("album-${language.tag}.png")
                }
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                await { !hasText("앨범 기록") && hasText("앨범") }
                clickText("설정")
                await { hasText("언어 / Language") }
                saveScreenshot("settings-ko.png")
            } finally {
                instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(original) }
            }
        }
    }

    @Test fun unsupportedSystemLanguageFallsBackAndSecondarySupportedLanguageMatchesContent() {
        val context = instrumentation.targetContext
        listOf("fr-FR" to "zh-Hans", "fr-FR,en-US" to "en", "zh-HK" to "zh-Hant", "zh-SG" to "zh-Hans").forEach { (tags, expected) ->
            val config = android.content.res.Configuration(context.resources.configuration)
            config.setLocales(android.os.LocaleList.forLanguageTags(tags))
            val localized = context.createConfigurationContext(config)
            assertEquals(expected, AppLanguage.contentTag(localized.resources.configuration.locales))
            val title = AlbumRepository.load(localized).find("1551833317039542272")!!.title
            assertEquals(when(expected) { "en" -> "Greatest Works of Art"; "zh-Hant" -> "最偉大的作品"; else -> "最伟大的作品" }, title)
        }
    }

    private fun nodes(): List<AccessibilityNodeInfo> {
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return emptyList()
        val result = mutableListOf<AccessibilityNodeInfo>()
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(root)
        while (pending.isNotEmpty()) {
            val node = pending.removeFirst()
            result.add(node)
            for (index in 0 until node.childCount) node.getChild(index)?.let(pending::add)
        }
        return result
    }

    private fun hasText(text: String): Boolean = nodes().any { it.text?.toString() == text }

    private fun clickText(text: String) {
        await { hasText(text) }
        for (match in nodes()) {
            if (match.text?.toString() != text) continue
            var node: AccessibilityNodeInfo? = match
            while (node != null) {
                if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return
                node = node.parent
            }
        }
        fail("No clickable node for $text")
    }

    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 20_000
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(100)
        }
        fail("Timed out waiting for localized UI")
    }

    private fun saveScreenshot(name: String) {
        instrumentation.waitForIdleSync()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()!!
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "language-checks").apply { mkdirs() }
        File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
