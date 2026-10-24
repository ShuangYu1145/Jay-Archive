package love.yu.jaychou

import android.graphics.Bitmap
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Use real window frames, including Android's own starting window. */
class StartupSmokeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun realActivityStartsRecreatesAndResumesWithoutBlankContent() {
        val context = instrumentation.targetContext
        try {
            // Avoid ActivityScenario's wait-for-idle gate: glass can keep producing frames.
            context.startActivity(Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            awaitHome("initial launch")
            screenshot("startup-home.png")
            val previous = resumedActivity()!!
            instrumentation.runOnMainSync { previous.recreate() }
            val deadline = SystemClock.uptimeMillis() + 20_000
            while (SystemClock.uptimeMillis() < deadline) {
                val current = resumedActivity()
                if (current != null && current !== previous) break
                SystemClock.sleep(100)
            }
            assertTrue("Activity must be recreated", resumedActivity()?.let { it !== previous } == true)
            awaitHome("Activity recreation")
            instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_HOME").close()
            SystemClock.sleep(500)
            context.startActivity(Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            awaitHome("resume")
        } finally {
            val current = resumedActivity()
            instrumentation.runOnMainSync { current?.finish() }
        }
    }

    private fun resumedActivity(): MainActivity? {
        var activity: MainActivity? = null
        instrumentation.runOnMainSync {
            activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<MainActivity>().firstOrNull()
        }
        return activity
    }

    private fun awaitHome(phase: String) {
        val labels = setOf("介绍", "Explore", "介紹", "紹介", "소개")
        val deadline = SystemClock.uptimeMillis() + 20_000
        while (SystemClock.uptimeMillis() < deadline) {
            if (labels.any(::hasText)) return
            SystemClock.sleep(100)
        }
        assertTrue("Home navigation must be visible after $phase", labels.any(::hasText))
    }

    private fun hasText(label: String): Boolean {
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return false
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(root)
        while (pending.isNotEmpty()) {
            val node = pending.removeFirst()
            if (node.packageName?.toString() == "love.yu.jaychou" && node.isVisibleToUser && node.text?.toString() == label) return true
            for (index in 0 until node.childCount) node.getChild(index)?.let(pending::add)
        }
        return false
    }

    private fun screenshot(name: String) {
        val context = instrumentation.targetContext
        val directory = File(context.getExternalFilesDir(null), "startup-repair").apply { mkdirs() }
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(directory, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
