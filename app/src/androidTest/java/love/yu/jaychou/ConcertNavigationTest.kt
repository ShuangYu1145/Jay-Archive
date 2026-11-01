package love.yu.jaychou

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicInteger
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.components.LocalArchiveBlur
import love.yu.jaychou.ui.effects.LocalArchiveLiquidGlass
import love.yu.jaychou.ui.effects.LocalTopAppBarBlur
import love.yu.jaychou.ui.screens.concerts.ConcertsScreen
import love.yu.jaychou.ui.screens.introduction.IntroductionScreen
import love.yu.jaychou.ui.theme.ArchiveTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConcertNavigationTest {
    @get:Rule val app = createComposeRule()

    @Test fun detailsSurviveHeaderHidingInEveryThemeAndBlurMode() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppText.update(context.resources)
        val date = LocalDate.now().plusDays(10)
        val schedule = ConcertRepository.fromOfficial(listOf(OfficialShow(date, "广州", "时光机")),
            ZonedDateTime.now(ZoneId.of("Asia/Shanghai")))
        val calls = AtomicInteger()
        val settings = mutableStateOf(AppSettings(topAppBarBlur = TopAppBarBlur.PROGRESSIVE))
        val image = "android.resource://${context.packageName}/${R.drawable.icon}"
        val station = DamaiConcert("1077189102025", "广州市", date, date, image,
            listOf(ConcertImage(image)), "演出介绍", emptyList(), "体育场", "地址")
        app.mainClock.autoAdvance = false
        app.setContent {
            ArchiveTheme(settings.value) {
                CompositionLocalProvider(LocalArchiveBlur provides settings.value.blur,
                    LocalArchiveLiquidGlass provides settings.value.liquidGlass,
                    LocalTopAppBarBlur provides settings.value.topAppBarBlur) {
                    val pager = rememberPagerState(initialPage = 1) { 3 }
                    var detail by remember { mutableStateOf(false) }
                    IntroductionScreen(pager, !detail, !detail, {}) { section ->
                        if (section == 1) ConcertsScreen(0.dp, { detail = it }, sectionHeader = true,
                            loadSchedule = { calls.incrementAndGet(); schedule }, loadMedia = { listOf(station) })
                    }
                }
            }
        }
        settle()
        for (style in UiStyle.entries) for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            for (blur in TopAppBarBlur.entries) {
                app.runOnIdle { settings.value = settings.value.copy(uiStyle = style, theme = theme, topAppBarBlur = blur) }
                settle()
                app.waitUntil(10_000) { app.onAllNodesWithTag("concert-featured").fetchSemanticsNodes().isNotEmpty() }
                app.onNodeWithTag("concerts-list").performScrollToKey("concert-featured")
                settle()
                val before = calls.get()
                app.onNodeWithTag("concert-featured").performClick()
                settle()
                app.onNodeWithTag("concert-detail").assertIsDisplayed()
                app.onNodeWithTag("introduction-header").assertDoesNotExist()
                app.onNodeWithTag("concerts-list").assertDoesNotExist()
                assertEquals("Opening a detail must preserve its schedule: $style/$theme/$blur", before, calls.get())
                app.onNodeWithTag("concert-detail").performScrollToKey("concert-detail-seats")
                settle()
                app.onNodeWithContentDescription(AppText.get(R.string.concert_seating_plan)).performClick()
                settle()
                app.onNodeWithTag("concert-image-preview").assertIsDisplayed()
                app.onNodeWithText(AppText.get(R.string.concert_close_image)).performClick()
                settle()
                app.onNodeWithTag("concert-detail-back").onChild().performClick()
                settle()
                app.onNodeWithTag("concert-detail").assertDoesNotExist()
                app.onNodeWithTag("concert-featured").assertIsDisplayed()
                assertEquals("Returning must not reload the schedule", before, calls.get())
            }
        }
    }

    private fun settle() {
        app.mainClock.advanceTimeBy(1_200)
        app.waitForIdle()
    }
}
