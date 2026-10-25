package love.yu.jaychou

import android.os.Build
import androidx.activity.BackEventCompat
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import org.junit.Assume.assumeTrue
import love.yu.jaychou.data.ReleaseCategory
import love.yu.jaychou.data.LyricsRepository
import love.yu.jaychou.data.AlbumLayout
import love.yu.jaychou.data.AlbumRepository
import love.yu.jaychou.data.SettingsRepository
import androidx.compose.ui.semantics.SemanticsProperties
import kotlinx.coroutines.runBlocking

class ArchiveNavigationTest {
    @get:Rule val app = createAndroidComposeRule<MainActivity>()

    private fun ready() {
        app.waitUntil(15_000) { app.onAllNodesWithTag("tab-INTRODUCTION").fetchSemanticsNodes().isNotEmpty() }
    }

    @Before fun establishGridFixture() {
        ready()
        runBlocking {
            SettingsRepository(app.activity).setAlbumLayout(AlbumLayout.GRID)
            SettingsRepository(app.activity).setCustomColor(null)
        }
        app.waitUntil(5_000) {
            app.onAllNodesWithTag("albums-grid").fetchSemanticsNodes().firstOrNull()
                ?.config?.getOrElse(SemanticsProperties.StateDescription) { "" } == AlbumLayout.GRID.label
        }
    }

    private fun openAlbums() {
        app.onNodeWithTag("tab-INTRODUCTION").performClick()
        app.onNodeWithTag("tab-ALBUMS").performClick()
    }

    private fun openConcerts() {
        app.onNodeWithTag("tab-INTRODUCTION").performClick()
        app.onNodeWithTag("tab-CONCERTS").performClick()
    }

    private fun selectTheme(label: String) {
        // Position the entire settings group above the floating navigation overlay.
        app.onNodeWithTag("settings-list").performScrollToIndex(5)
        val selector = app.onNodeWithTag("theme-mode").performScrollTo()
        if (app.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()) return
        selector.performClick()
        app.onNodeWithText(label).performClick()
    }

    @Test fun gridPositionSurvivesTabSwitchDetailBackAndRecreation() {
        ready()
        openAlbums()
        // Align the whole album row: bringing only its title to the viewport edge
        // can leave that title behind a floating navigation bar on short screens.
        app.onNodeWithTag("albums-grid").performScrollToIndex(15)
        val titleBounds = app.onNodeWithText("范特西").fetchSemanticsNode().boundsInRoot
        val navigationBounds = app.onNodeWithTag("floating-navigation").fetchSemanticsNode().boundsInRoot
        assertTrue("The clicked title must be above the floating bar", titleBounds.bottom < navigationBounds.top)
        app.onNodeWithText("范特西").assertIsDisplayed()
        app.onNodeWithTag("tab-ABOUT").performClick()
        openAlbums()
        app.onNodeWithText("范特西").assertIsDisplayed().performClick()
        app.onNodeWithTag("album-detail").assertIsDisplayed()
        app.onNodeWithText("范特西").assertIsDisplayed()
        app.activityRule.scenario.recreate()
        app.waitUntil(15_000) { app.onAllNodesWithTag("album-detail").fetchSemanticsNodes().isNotEmpty() }
        app.onNodeWithText("范特西").assertIsDisplayed()
        app.runOnUiThread { app.activity.onBackPressedDispatcher.onBackPressed() }
        app.waitUntil(5_000) { app.onAllNodesWithTag("album-detail").fetchSemanticsNodes().isEmpty() }
        app.onNodeWithText("范特西").assertIsDisplayed()
    }

    @Test fun appearancePreferencesSurviveRecreation() {
        ready()
        app.onNodeWithTag("tab-SETTINGS").performClick()
        selectTheme("深色")
        val monet = app.onNodeWithContentDescription("启用 Monet 动态配色")
        monet.performScrollTo()
        val monetBounds = monet.fetchSemanticsNode().boundsInRoot
        val navigationBounds = app.onNodeWithTag("floating-navigation").fetchSemanticsNode().boundsInRoot
        assertTrue("The setting must be fully above the floating bar", monetBounds.bottom < navigationBounds.top)
        if (Build.VERSION.SDK_INT >= 31) {
            // Establish an explicit saved state independent of prior test runs.
            if (monet.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ToggleableState] != androidx.compose.ui.state.ToggleableState.On) monet.performClick()
            app.waitUntil(5_000) {
                monet.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ToggleableState] == androidx.compose.ui.state.ToggleableState.On
            }
            monet.assertIsOn()
        } else monet.assertIsNotEnabled()
        app.activityRule.scenario.recreate()
        ready()
        app.onNodeWithText("深色").assertIsDisplayed()
        if (Build.VERSION.SDK_INT >= 31) app.onNodeWithContentDescription("启用 Monet 动态配色").assertIsOn()
        // Restore defaults for a useful first launch after testing.
        selectTheme("跟随系统")
        if (Build.VERSION.SDK_INT >= 31) app.onNodeWithContentDescription("启用 Monet 动态配色").performClick()
    }

    private fun chooseAlbumLayout(layout: AlbumLayout) {
        app.onNodeWithTag("tab-SETTINGS").performClick()
        app.onNodeWithTag("settings-list").performScrollToIndex(1)
        if (app.onAllNodesWithText(layout.label).fetchSemanticsNodes().isNotEmpty()) return
        app.onNodeWithTag("album-layout").performClick()
        app.onNodeWithText(layout.label).performClick()
        app.onNodeWithText(layout.label).assertIsDisplayed()
    }

    private fun catalogScrollPosition(): Float = app.onNodeWithTag("albums-grid")
        .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

    @Test fun albumLayoutsAndIndependentSettingsSurviveRecreation() {
        ready()
        app.onNodeWithTag("tab-ABOUT").performClick()
        app.onNodeWithText("JAY ARCHIVE").assertIsDisplayed()
        app.onAllNodes(hasText("主题模式") and hasAnyAncestor(hasTestTag("page-ABOUT"))).assertCountEquals(0)
        val albums = AlbumRepository.load(app.activity).albums.filter { it.category == ReleaseCategory.STUDIO }
        val first = "album-${albums.first().id}"
        val target = "album-${albums[6].id}"
        val saved = mutableMapOf<AlbumLayout, Float>()
        AlbumLayout.entries.forEach { layout ->
            chooseAlbumLayout(layout)
            openAlbums()
            val card = app.onNodeWithTag(first).fetchSemanticsNode().boundsInRoot
            val viewport = app.onNodeWithTag("root-pager").fetchSemanticsNode().boundsInRoot
            when (layout) {
                AlbumLayout.GRID -> {
                    assertTrue("Grid cards occupy one of two columns", card.width < viewport.width * .55f)
                    val second = app.onNodeWithTag("album-${albums[1].id}").fetchSemanticsNode().boundsInRoot
                    assertTrue("Grid albums share a row", kotlin.math.abs(second.top - card.top) < 1f && second.left > card.left)
                }
                AlbumLayout.LIST -> assertTrue("List rows are wide and compact", card.width > viewport.width * .9f && card.height < card.width * .6f)
                AlbumLayout.LARGE -> assertTrue("Large cards feature a full-width square cover", card.width > viewport.width * .9f && card.height > card.width)
            }
            app.onNodeWithTag("albums-grid").performScrollToIndex(7)
            app.onNodeWithTag(target).assertIsDisplayed()
            val before = catalogScrollPosition()
            app.onNodeWithTag(target).performClick()
            app.onNodeWithTag("album-detail").assertIsDisplayed()
            app.onNodeWithContentDescription("返回专辑列表").performClick()
            app.waitUntil(5_000) { app.onAllNodesWithTag("album-detail").fetchSemanticsNodes().isEmpty() }
            assertTrue("Detail back preserves ${layout.label}", kotlin.math.abs(catalogScrollPosition() - before) < .001f)
            saved[layout] = catalogScrollPosition()
            app.onNodeWithTag("tab-SETTINGS").performClick()
            app.onNodeWithTag("tab-SETTINGS").assertIsSelected()
            app.activityRule.scenario.recreate()
            ready()
            app.onNodeWithTag("tab-SETTINGS").assertIsSelected()
            openAlbums()
            app.onNodeWithTag("albums-grid").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, layout.label))
            app.onNodeWithTag(target).assertIsDisplayed()
            assertTrue("Recreation preserves ${layout.label} position", kotlin.math.abs(catalogScrollPosition() - saved.getValue(layout)) < .001f)
        }
        AlbumLayout.entries.forEach { layout ->
            chooseAlbumLayout(layout)
            openAlbums()
            app.onNodeWithTag(target).assertIsDisplayed()
            assertTrue("Each style keeps its own position", kotlin.math.abs(catalogScrollPosition() - saved.getValue(layout)) < .001f)
        }
        chooseAlbumLayout(AlbumLayout.GRID)
    }

    @Test fun bottomBarScrubsAcrossTabsAndCancellationRestoresTheTab() {
        ready()
        val navigation = app.onNodeWithTag("floating-navigation")
        navigation.performTouchInput {
            down(Offset(width / 10f, centerY))
            moveTo(Offset(width * 9f / 10f, centerY), 500)
            up()
        }
        app.onNodeWithTag("tab-ABOUT").assertIsSelected()
        navigation.performTouchInput {
            down(Offset(width * 9f / 10f, centerY))
            moveTo(Offset(width / 10f, centerY), 500)
            cancel()
        }
        app.onNodeWithTag("tab-ABOUT").assertIsSelected()
        app.onNodeWithTag("root-pager").performTouchInput { swipeRight() }
        app.onNodeWithTag("tab-SETTINGS").assertIsSelected()
        app.onNodeWithTag("root-pager").performTouchInput { swipeRight() }
        app.onNodeWithTag("tab-INTRODUCTION").assertIsSelected()
        app.onNodeWithTag("tab-ALBUMS").assertIsSelected()
    }

    @Test fun tabTapProducesAnIntermediateSlidingPage() {
        ready()
        val width = app.onNodeWithTag("root-pager").fetchSemanticsNode().boundsInRoot.width
        app.mainClock.autoAdvance = false
        try {
            app.onNodeWithTag("tab-INTRODUCTION").performClick()
            app.mainClock.advanceTimeBy(96)
            app.waitForIdle()
            val left = app.onNodeWithTag("page-ALBUMS").fetchSemanticsNode().boundsInRoot.left
            assertTrue("The incoming page must be between the viewport edge and its settled position: $left", left > 1f && left < width - 1f)
        } finally {
            app.mainClock.autoAdvance = true
        }
        app.waitForIdle()
        app.onNodeWithTag("tab-ALBUMS").assertIsSelected()
    }

    @Test fun detailExpandsFromTheTappedCardAndReturnsToItsPosition() {
        ready()
        openAlbums()
        val card = app.onNodeWithTag("album-2037128764168736768")
        val sourceWidth = card.fetchSemanticsNode().boundsInRoot.width
        val viewportWidth = app.onNodeWithTag("root-pager").fetchSemanticsNode().boundsInRoot.width
        app.mainClock.autoAdvance = false
        try {
            card.performClick()
            app.mainClock.advanceTimeBy(96)
            app.waitForIdle()
            val midWidth = app.onNodeWithTag("album-detail").fetchSemanticsNode().boundsInRoot.width
            assertTrue("The detail must grow from its card rather than appear full-screen: $midWidth", midWidth > sourceWidth && midWidth < viewportWidth - 1f)
        } finally {
            app.mainClock.autoAdvance = true
        }
        app.waitForIdle()
        app.runOnUiThread { app.activity.onBackPressedDispatcher.onBackPressed() }
        app.waitUntil(5_000) { app.onAllNodesWithTag("album-detail").fetchSemanticsNodes().isEmpty() }
        card.assertIsDisplayed()
    }

    private fun detailScrollPosition(): Float = app.onNodeWithTag("album-detail-list")
        .fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange].value()

    @Test fun detailScrollsFromTheCoverAndTrackCardsAfterReopening() {
        ready()
        openAlbums()
        val card = app.onNodeWithTag("album-2037128764168736768")
        val gridTop = card.fetchSemanticsNode().boundsInRoot.top
        repeat(2) { attempt ->
            card.performClick()
            val list = app.onNodeWithTag("album-detail-list")
            list.assertIsDisplayed()
            // Inject actual continuous touch movement; semantic scrollTo bypasses the bug.
            repeat(2) { drag ->
                val before = detailScrollPosition()
                list.performTouchInput {
                    // Begin below touch slop, like a finger that moves gently before dragging.
                    down(Offset(width * .5f, height * .8f))
                    repeat(6) { moveBy(Offset(0f, -1f), 16) }
                    repeat(40) { moveBy(Offset(width * .001f, -height * .015f), 16) }
                    up()
                }
                val after = detailScrollPosition()
                assertTrue("Detail drag $drag on opening $attempt must scroll: $before -> $after", after > before + .5f)
            }
            val lower = detailScrollPosition()
            list.performTouchInput { swipeDown(durationMillis = 700) }
            assertTrue("Dragging down over track cards must scroll back", detailScrollPosition() < lower - .5f)
            list.performTouchInput { down(center); moveBy(Offset(0f, -12f)); cancel() }
            val afterCancel = detailScrollPosition()
            list.performTouchInput { swipeUp(durationMillis = 700) }
            assertTrue("A cancelled touch must not lock subsequent scrolling", detailScrollPosition() > afterCancel + .5f)
            app.runOnUiThread { app.activity.onBackPressedDispatcher.onBackPressed() }
            app.waitUntil(5_000) { app.onAllNodesWithTag("album-detail").fetchSemanticsNodes().isEmpty() }
            card.assertIsDisplayed()
            assertTrue("Detail gestures must not move the underlying grid", kotlin.math.abs(card.fetchSemanticsNode().boundsInRoot.top - gridTop) < 1f)
        }
    }

    @Test fun predictiveBackCanCancelThenCommitWithoutLosingTheGrid() {
        assumeTrue(Build.VERSION.SDK_INT >= 34)
        ready()
        openAlbums()
        app.onNodeWithTag("albums-grid").performScrollToNode(hasText("七里香"))
        app.onNodeWithText("七里香").performClick()
        app.onNodeWithTag("album-detail").assertIsDisplayed()
        app.runOnUiThread {
            app.activity.onBackPressedDispatcher.dispatchOnBackStarted(BackEventCompat(0f, 300f, 0f, BackEventCompat.EDGE_LEFT))
            app.activity.onBackPressedDispatcher.dispatchOnBackProgressed(BackEventCompat(180f, 300f, .6f, BackEventCompat.EDGE_LEFT))
        }
        app.waitForIdle()
        app.onNodeWithTag("album-detail").assertIsDisplayed()
        app.runOnUiThread { app.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
        app.waitForIdle()
        app.onNodeWithTag("album-detail").assertIsDisplayed()
        val beforeScroll = detailScrollPosition()
        app.onNodeWithTag("album-detail-list").performTouchInput { swipeUp(durationMillis = 700) }
        assertTrue("Scrolling must work after cancelling predictive back", detailScrollPosition() > beforeScroll + .5f)
        app.runOnUiThread {
            app.activity.onBackPressedDispatcher.dispatchOnBackStarted(BackEventCompat(0f, 300f, 0f, BackEventCompat.EDGE_LEFT))
            app.activity.onBackPressedDispatcher.dispatchOnBackProgressed(BackEventCompat(240f, 300f, .8f, BackEventCompat.EDGE_LEFT))
            app.activity.onBackPressedDispatcher.onBackPressed()
        }
        app.waitUntil(5_000) { app.onAllNodesWithTag("album-detail").fetchSemanticsNodes().isEmpty() }
        app.onNodeWithText("七里香").assertIsDisplayed()
    }
    @Test fun releaseCategoriesConfirmCancelAndRestorePosition() {
        openAlbums()
        app.onNodeWithTag("albums-grid").performScrollToIndex(7)
        val before = catalogScrollPosition()
        app.onNodeWithTag("release-filter").performClick()
        app.onNodeWithTag("category-STUDIO").performClick()
        app.onNodeWithTag("category-LIVE").performClick()
        app.onNodeWithText("取消").performClick()
        assertTrue(kotlin.math.abs(catalogScrollPosition() - before) < .001f)
        app.onNodeWithTag("release-filter").performClick()
        app.onNodeWithTag("category-STUDIO").performClick()
        app.onNodeWithTag("category-LIVE").performClick()
        app.onNodeWithText("确定").performClick()
        val live = AlbumRepository.load(app.activity).albums.filter { it.category == ReleaseCategory.LIVE }
        app.onNodeWithTag("album-${live.first().id}").assertIsDisplayed().performClick()
        app.onNodeWithText(live.first().title).assertIsDisplayed()
        app.onNodeWithTag("album-detail-list").performScrollToIndex(4)
        app.onNodeWithTag("track-1").assertIsDisplayed()
        app.onNodeWithContentDescription("返回专辑列表").performClick()
        app.waitUntil(5_000) { app.onAllNodesWithTag("album-detail").fetchSemanticsNodes().isEmpty() }
        app.activityRule.scenario.recreate()
        ready()
        app.onNodeWithTag("album-${live.first().id}").assertIsDisplayed()
        app.onNodeWithTag("release-filter").performClick()
        app.onNodeWithTag("category-LIVE").performClick()
        app.onNodeWithTag("category-STUDIO").performClick()
        app.onNodeWithText("确定").performClick()
        assertTrue("A category set keeps its own catalogue position", kotlin.math.abs(catalogScrollPosition() - before) < .001f)
    }

    @Test fun customColorNativePickerPersistsAndCanBeReset() {
        app.onNodeWithTag("tab-SETTINGS").performClick()
        app.onNodeWithTag("settings-list").performScrollToIndex(5)
        val selector = app.onNodeWithTag("custom-color").performScrollTo()
        val barTop = app.onNodeWithTag("floating-navigation").fetchSemanticsNode().boundsInRoot.top
        if (selector.fetchSemanticsNode().boundsInRoot.bottom >= barTop) {
            // Bring the row above the floating overlay on short screens before touch injection.
            app.onNodeWithTag("settings-list").performTouchInput {
                swipe(Offset(centerX, height * .7f), Offset(centerX, height * .35f), 400)
            }
        }
        assertTrue("Color selector must be above navigation", selector.fetchSemanticsNode().boundsInRoot.bottom < barTop)
        selector.performClick()
        app.waitUntil(5_000) { app.onAllNodesWithTag("color-sheet").fetchSemanticsNodes().isNotEmpty() }
        app.onNodeWithTag("color-preset-1").performClick()
        app.onNodeWithText("应用").performClick()
        app.onNodeWithText("#8456CF").assertIsDisplayed()
        app.activityRule.scenario.recreate()
        ready()
        app.onNodeWithText("#8456CF").assertIsDisplayed()
        app.onNodeWithTag("custom-color").performClick()
        app.onNodeWithText("恢复预设蓝色").performClick()
        app.onNodeWithText("预设蓝色").assertIsDisplayed()
    }

    @Test fun lyricMorphBackAndRecreationPreserveAlbumPosition() {
        val album = AlbumRepository.load(app.activity).albums.first { it.category == ReleaseCategory.STUDIO }
        val track = album.tracks.first()
        val fixture = java.io.File(app.activity.cacheDir, "self-authored-lyrics.txt")
        fixture.writeText((1..40).joinToString("\n") { "测试文本第 $it 行，仅用于导航回归。" })
        runBlocking { LyricsRepository.import(app.activity, album, track, android.net.Uri.fromFile(fixture)) }
        openAlbums()
        app.onNodeWithTag("album-${album.id}").performClick()
        app.onNodeWithTag("album-detail-list").performScrollToIndex(4)
        val before = detailScrollPosition()
        val sourceWidth = app.onNodeWithTag("track-cover-1", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.width
        app.mainClock.autoAdvance = false
        try {
            app.onNodeWithTag("track-1").performClick()
            app.mainClock.advanceTimeBy(96)
            app.waitForIdle()
            val width = app.onNodeWithTag("lyrics-detail-cover-flight", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.width
            assertTrue("Lyrics expand from the song artwork", width > sourceWidth)
        } finally { app.mainClock.autoAdvance = true }
        app.waitUntil(8_000) { app.onAllNodesWithText("测试文本第 1 行，仅用于导航回归。").fetchSemanticsNodes().isNotEmpty() }
        app.activityRule.scenario.recreate()
        app.waitUntil(15_000) { app.onAllNodesWithTag("lyrics-detail").fetchSemanticsNodes().isNotEmpty() }
        app.onNodeWithTag("lyrics-detail").assertIsDisplayed()
        if (Build.VERSION.SDK_INT >= 34) {
            app.runOnUiThread {
                app.activity.onBackPressedDispatcher.dispatchOnBackStarted(BackEventCompat(0f, 300f, 0f, BackEventCompat.EDGE_LEFT))
                app.activity.onBackPressedDispatcher.dispatchOnBackProgressed(BackEventCompat(150f, 300f, .5f, BackEventCompat.EDGE_LEFT))
                app.activity.onBackPressedDispatcher.dispatchOnBackCancelled()
            }
            app.waitForIdle()
            app.onNodeWithTag("lyrics-detail").assertIsDisplayed()
        }
        app.onNodeWithTag("lyrics-list").performTouchInput { swipeUp(durationMillis = 600) }
        app.onNodeWithContentDescription("返回专辑详情").performClick()
        app.waitUntil(5_000) { app.onAllNodesWithTag("lyrics-detail").fetchSemanticsNodes().isEmpty() }
        assertTrue("Lyrics return keeps the album's track position", kotlin.math.abs(detailScrollPosition() - before) < .001f)
        app.onNodeWithTag("track-1").assertIsDisplayed()
        app.onNodeWithContentDescription("返回专辑列表").performClick()
        app.waitUntil(5_000) { app.onAllNodesWithTag("album-detail").fetchSemanticsNodes().isEmpty() }
        runBlocking { LyricsRepository.removeLocal(app.activity, album, track) }
        fixture.delete()
    }

    @Test fun concertCalendarOpensDetailAndHidesNavigation() {
        ready()
        openConcerts()
        app.waitUntil(15_000) { app.onAllNodesWithTag("concert-featured").fetchSemanticsNodes().isNotEmpty() }
        app.onNodeWithTag("concert-featured").performScrollTo().assertIsDisplayed()
        app.onNodeWithText("巡演日历").performScrollTo().assertIsDisplayed()
        app.onNodeWithTag("concert-featured").performScrollTo().performClick()
        app.waitUntil(8_000) { app.onAllNodesWithTag("concert-detail").fetchSemanticsNodes().isNotEmpty() }
        app.onNodeWithTag("concert-detail").assertIsDisplayed()
        assertTrue("Root navigation must hide behind concert details",
            app.onAllNodesWithTag("tab-CONCERTS").fetchSemanticsNodes().isEmpty())
        app.onNodeWithContentDescription("返回演唱会").performClick()
        app.waitUntil(5_000) { app.onAllNodesWithTag("concert-detail").fetchSemanticsNodes().isEmpty() }
        app.onNodeWithTag("concert-featured").assertIsDisplayed()
        app.onNodeWithTag("tab-CONCERTS").assertIsSelected()
    }

    @Test fun headersCoverStatusInsetAndTabPagesAreDistinct() {
        app.onNodeWithTag("top-bar-首页").assertIsDisplayed()
        assertTrue("Header surface includes the status bar", app.onNodeWithTag("top-bar-首页").fetchSemanticsNode().boundsInRoot.top < 1f)
        openAlbums()
        val albums = AlbumRepository.load(app.activity).albums.filter { it.category == ReleaseCategory.STUDIO }
        val firstDate = app.onNodeWithTag("album-date-${albums[0].id}", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val secondDate = app.onNodeWithTag("album-date-${albums[1].id}", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        // Fixed grids distribute an odd physical pixel to one column. Its square
        // cover can be one pixel taller; this is rounding, not a text-row offset.
        assertTrue("Metadata aligns within one physical pixel: ${firstDate.top} vs ${secondDate.top}",
            kotlin.math.abs(firstDate.top - secondDate.top) <= 1.01f)
        openConcerts()
        app.onNodeWithTag("tab-CONCERTS").assertIsSelected()
        app.onNodeWithTag("top-bar-演唱会").assertIsDisplayed()
        app.onAllNodes(hasTestTag("albums-grid") and hasAnyAncestor(hasTestTag("page-CONCERTS"))).assertCountEquals(0)
    }

}
