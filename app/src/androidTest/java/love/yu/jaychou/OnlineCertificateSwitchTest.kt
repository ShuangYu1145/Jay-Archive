package love.yu.jaychou

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import love.yu.jaychou.data.*
import love.yu.jaychou.online.*
import love.yu.jaychou.playback.MusicPlaybackState
import love.yu.jaychou.ui.screens.music.OnlineMusicScreen
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OnlineCertificateSwitchTest {
    @get:Rule val app = createComposeRule()
    @Test fun materialSwitchPersistsAndShowsTheWarning() = verifySwitch(UiStyle.MATERIAL3)
    @Test fun miuixSwitchPersistsAndShowsTheWarning() = verifySwitch(UiStyle.MIUIX)
    @Test fun couiSwitchPersistsAndShowsTheWarning() = verifySwitch(UiStyle.COUI)

    private fun verifySwitch(style: UiStyle) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val saved = mutableListOf<Boolean>()
        val accounts = object : MusicAccountStore {
            private var verify = true
            override fun endpoint(provider: MusicProvider) = "https://fixture.example.com"
            override fun setEndpoint(provider: MusicProvider, endpoint: String) { }
            override fun credential(provider: MusicProvider) = ""
            override fun setCredential(provider: MusicProvider, credential: String) { }
            override fun certificateVerificationEnabled() = verify
            override fun setCertificateVerificationEnabled(enabled: Boolean) { verify = enabled; saved += enabled }
        }
        val repository = OnlineMusicRepository(accounts) { _, _, _, _, _ -> JSONObject("""{"code":200}""") }
        app.setContent {
            val scope = rememberCoroutineScope()
            val state = remember { OnlineMusicState(repository, scope, MusicPlaybackState()) }
            ArchiveTheme(AppSettings(uiStyle = style, blur = false)) { OnlineMusicScreen(state, 80.dp, true) }
        }
        app.onNodeWithTag("online-services").performClick()
        // Miuix/COUI expose state on their native switch child; Material exposes it on the row.
        fun toggle() = if (style == UiStyle.MATERIAL3) app.onNodeWithTag("online-verify-certificates") else
            app.onNode(isToggleable() and hasAnyAncestor(hasTestTag("online-verify-certificates")), useUnmergedTree = true)
        toggle().assertIsOn()
        app.onNodeWithTag("online-verify-certificates").performClick()
        toggle().assertIsOff()
        app.onNodeWithText(AppText.get(R.string.online_verify_certificates_off)).assertExists()
        app.onNodeWithTag("online-verify-certificates").performClick()
        toggle().assertIsOn()
        app.runOnIdle { assertEquals(listOf(false, true), saved) }
    }
}
