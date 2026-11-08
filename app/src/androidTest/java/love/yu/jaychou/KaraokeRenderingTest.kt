package love.yu.jaychou

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import love.yu.jaychou.data.AppSettings
import love.yu.jaychou.playback.*
import love.yu.jaychou.ui.screens.music.KaraokeLyricText
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class KaraokeRenderingTest {
    @get:Rule val app = createComposeRule()

    @Test fun halfwayThroughOneCharacterHasBothLitAndUnlitPixels() {
        var position by mutableLongStateOf(0L)
        val line = EmbeddedLyricLine(0, "国", listOf(EmbeddedLyricWord(0, 1_000, "国")))
        app.setContent {
            ArchiveTheme(AppSettings()) {
                Box(Modifier.background(Color.White).padding(12.dp).testTag("karaoke-render")) {
                    KaraokeLyricText(line, true, true, true, Color.Black, { position }, Modifier.width(150.dp))
                }
            }
        }
        fun pixels(): IntArray {
            val bitmap = app.onNodeWithTag("karaoke-render").captureToImage().asAndroidBitmap()
            return IntArray(bitmap.width * bitmap.height).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }
        }
        val before = pixels()
        app.runOnIdle { position = 500 }
        val halfway = pixels()
        app.runOnIdle { position = 1_000 }
        val after = pixels()
        val changed = before.indices.filter { before[it] != after[it] }
        assertTrue("The left part of the glyph has already filled", changed.count { halfway[it] == after[it] } > 20)
        assertTrue("The right part of the same glyph is still unfilled", changed.count { halfway[it] == before[it] } > 20)
    }
}
