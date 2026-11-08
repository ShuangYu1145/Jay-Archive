package love.yu.jaychou

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import love.yu.jaychou.data.*
import love.yu.jaychou.navigation.ArchiveApp
import love.yu.jaychou.online.*
import love.yu.jaychou.playback.MusicPlaybackService
import love.yu.jaychou.playback.MusicPlaybackState
import love.yu.jaychou.ui.screens.music.OnlineMusicScreen
import love.yu.jaychou.ui.screens.albums.AlbumDetailScreen
import androidx.compose.foundation.lazy.rememberLazyListState
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class OnlineMusicUiTest {
    @get:Rule val app = createComposeRule()
    private class Store : MusicAccountStore {
        private var verifyCertificates = true
        override fun certificateVerificationEnabled() = verifyCertificates
        override fun setCertificateVerificationEnabled(enabled: Boolean) { verifyCertificates = enabled }
        private val credentials = mutableMapOf(MusicProvider.QQ to "qqmusic_session=fixture")
        override fun endpoint(provider: MusicProvider) = "https://fixture.example.com"
        override fun setEndpoint(provider: MusicProvider, endpoint: String) { }
        override fun credential(provider: MusicProvider) = credentials[provider].orEmpty()
        override fun setCredential(provider: MusicProvider, credential: String) { credentials[provider] = credential }
    }

    @Test fun onlinePageRetainsNativeStylesAndSupportsSearchAndPrivateLists() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.createConfigurationContext(android.content.res.Configuration(context.resources.configuration).apply {
            setLocales(android.os.LocaleList.forLanguageTags("zh-Hans"))
        }).resources)
        val style = mutableStateOf(UiStyle.MIUIX)
        val repository = OnlineMusicRepository(Store()) { _, path, _, _, _ ->
            JSONObject(when (path) {
                "/login/status" -> """{"code":200,"data":{"profile":{"uin":"12345","nick":"测试听众"}}}"""
                "/user/playlist" -> """{"code":200,"playlist":[{"tid":"100","dirId":3,"dirName":"私密歌单","songNum":1},{"tid":"200","dirId":201,"dirName":"我喜欢的音乐","songNum":1}]}"""
                "/search" -> """{"code":200,"items":[{"mid":"search-song","title":"晴天","singer":[{"name":"周杰伦"}],"album":{"name":"叶惠美"}}],"more":false}"""
                else -> """{"code":200,"songs":[{"mid":"playlist-song","title":"七里香","singer":[{"name":"周杰伦"}],"album":{"name":"七里香"}}],"more":false}"""
            })
        }
        app.setContent {
            val scope = rememberCoroutineScope()
            val playback = remember { MusicPlaybackState() }
            val state = remember { OnlineMusicState(repository, scope, playback) }
            ArchiveTheme(AppSettings(uiStyle = style.value, blur = false)) { OnlineMusicScreen(state, 100.dp, active = true) }
        }
        app.waitUntil(5_000) { app.onAllNodesWithText("测试听众").fetchSemanticsNodes().isNotEmpty() }
        for (value in UiStyle.entries) {
            app.runOnIdle { style.value = value }
            app.waitForIdle()
            app.onNodeWithTag("online-account").assertIsDisplayed()
            app.onNodeWithTag("online-provider-QQ").assertIsDisplayed()
            app.onNodeWithTag("online-provider-NETEASE").assertIsDisplayed()
            val bitmap = app.onRoot().captureToImage().asAndroidBitmap()
            val file = File(context.getExternalFilesDir(null), "online-music/${value.name}.png").apply { parentFile!!.mkdirs() }
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
        app.onNodeWithTag("online-search-input").performTextInput("晴天")
        app.onNodeWithTag("online-search").performClick()
        app.waitUntil(5_000) { app.onAllNodesWithTag("online-song-search-song").fetchSemanticsNodes().isNotEmpty() }
        app.onNodeWithTag("online-song-search-song").assertIsDisplayed()
        app.onNodeWithText(AppText.get(R.string.online_library)).performClick()
        app.onNodeWithTag("online-list").performScrollToNode(hasTestTag("online-playlist-100"))
        app.onNodeWithTag("online-playlist-100").performClick()
        app.waitUntil(5_000) { app.onAllNodesWithTag("online-song-playlist-song").fetchSemanticsNodes().isNotEmpty() }
        app.onNodeWithTag("online-song-playlist-song").assertIsDisplayed()
        app.onNodeWithText(AppText.get(R.string.online_library)).performClick()
        app.onNodeWithTag("online-list").performScrollToIndex(0)
        app.onNodeWithTag("online-services").performClick()
        app.onNodeWithTag("online-netease-endpoint").assertExists()
        app.onNodeWithTag("online-qq-endpoint").assertExists()
    }

    @Test fun rootNavigationAddsOnlineWithoutStartingPlayback() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val catalog = AlbumRepository.load(context)
        app.setContent {
            val settings = AppSettings(uiStyle = UiStyle.MATERIAL3, blur = false, liquidGlass = false)
            ArchiveTheme(settings) { ArchiveApp(catalog, settings, {}, {}, {}, {}, {}, {}, {}, null, {}) }
        }
        app.onNodeWithTag("tab-ONLINE").performClick()
        app.waitForIdle()
        app.onNodeWithTag("online-search-input").assertIsDisplayed()
        app.onNodeWithTag("mini-music-player").assertDoesNotExist()
        app.runOnIdle { assertFalse(MusicPlaybackService.isRunning) }
        app.onNodeWithTag("tab-SETTINGS").performClick()
        app.waitForIdle()
        app.onNodeWithTag("tab-SETTINGS").assertIsSelected()
    }

    @Test fun albumTracksBeyondTheOldLocalRecordingInvokeOnlinePlayback() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val album = AlbumRepository.load(context).find("1152155845522493440")!!
        var requested: Track? = null
        app.setContent {
            ArchiveTheme(AppSettings(uiStyle = UiStyle.MIUIX, blur = false)) {
                AlbumDetailScreen(album, rememberLazyListState(), 40.dp, {}, onPlayTrack = { requested = it })
            }
        }
        app.onNodeWithTag("album-detail-list").performScrollToNode(hasTestTag("music-track-toggle-2"))
        app.onNodeWithTag("music-track-toggle-2").performClick()
        app.runOnIdle { assertEquals(album.tracks[1], requested); assertFalse(MusicPlaybackService.isRunning) }
    }
}
