package love.yu.jaychou

import android.util.Base64
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.atomic.AtomicInteger
import love.yu.jaychou.data.*
import love.yu.jaychou.online.*
import love.yu.jaychou.playback.MusicPlaybackState
import love.yu.jaychou.ui.screens.music.OnlineMusicScreen
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class OnlineQrLifecycleTest {
    @get:Rule val app = createComposeRule()

    @Test fun backgroundScanningKeepsTheQrAndOnlyExplicitRefreshReplacesIt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val owner = object : LifecycleOwner { override val lifecycle = LifecycleRegistry(this) }
        InstrumentationRegistry.getInstrumentation().runOnMainSync { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        val generated = AtomicInteger(); val cancelled = AtomicInteger(); val polls = AtomicInteger()
        val image = "data:image/png;base64," + Base64.encodeToString(qrTestImage(), Base64.NO_WRAP)
        val accounts = object : MusicAccountStore {
            private var pending: PendingMusicLogin? = null
            override fun endpoint(provider: MusicProvider) = "https://fixture.example.com"
            override fun setEndpoint(provider: MusicProvider, endpoint: String) { }
            override fun credential(provider: MusicProvider) = ""
            override fun setCredential(provider: MusicProvider, credential: String) { }
            override fun pendingLogin(provider: MusicProvider) = pending
            override fun setPendingLogin(provider: MusicProvider, login: PendingMusicLogin?) { pending = login }
        }
        val repository = OnlineMusicRepository(accounts) { _, path, _, _, _ ->
            when (path) {
                "/login/qr/key" -> JSONObject().put("code", 200).put("data", JSONObject().put("unikey", "key-${generated.incrementAndGet()}"))
                "/login/qr/create" -> JSONObject().put("code", 200).put("data", JSONObject().put("qrimg", image))
                "/login/qr/check" -> { polls.incrementAndGet(); JSONObject("""{"code":801}""") }
                "/login/qr/cancel" -> { cancelled.incrementAndGet(); JSONObject("""{"code":200}""") }
                else -> JSONObject("""{"code":200}""")
            }
        }
        lateinit var state: OnlineMusicState
        app.setContent {
            val scope = rememberCoroutineScope()
            state = remember { OnlineMusicState(repository, scope, MusicPlaybackState()) }
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                ArchiveTheme(AppSettings(uiStyle = UiStyle.MATERIAL3, blur = false)) { OnlineMusicScreen(state, 80.dp, true) }
            }
        }
        app.onNodeWithTag("online-login").performClick()
        app.waitUntil(5_000) { generated.get() == 1 && polls.get() > 0 }
        app.onNodeWithTag("online-save-qr").assertExists().assertIsEnabled()
        val before = polls.get()
        app.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.CREATED }
        app.waitUntil(6_000) { polls.get() > before }
        app.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        app.onNodeWithTag("online-save-qr").assertExists().assertIsEnabled()
        app.runOnIdle { assertEquals("key-1", state.qr?.key); assertEquals(1, generated.get()); assertEquals(0, cancelled.get()) }
        app.onNodeWithTag("online-refresh-qr").performClick()
        app.waitUntil(5_000) { generated.get() == 2 }
        app.runOnIdle { assertEquals("key-2", state.qr?.key); assertEquals(1, cancelled.get()) }
    }
}
