package love.yu.jaychou

import love.yu.jaychou.ui.animation.albumTransitionProgress
import org.junit.Assert.*
import org.junit.Test

class AlbumTransitionProgressTest {
    @Test fun aCommittedGestureKeepsMovingUntilTheExitActuallyEnds() {
        for (gesture in listOf(.3f, .6f, 1f)) {
            var previous = albumTransitionProgress(1f, gesture)
            for (open in listOf(.8f, .5f, .25f, .1f, .02f, .001f)) {
                val progress = albumTransitionProgress(open, gesture)
                assertTrue("Cover must not arrive early and pause while the page is still closing", progress > 0f)
                assertTrue("Return motion must keep moving toward its endpoint", progress < previous)
                previous = progress
            }
            assertEquals(0f, albumTransitionProgress(0f, gesture), 0f)
        }
    }

    @Test fun cancellationRestoresTheDetailAndOrdinaryBackKeepsItsFullRange() {
        assertEquals(1f, albumTransitionProgress(1f, 0f), 0f)
        assertEquals(.5f, albumTransitionProgress(.5f, 0f), 0f)
        assertEquals(.85f, albumTransitionProgress(1f, .6f), .001f)
        assertEquals(0f, albumTransitionProgress(-.1f, .6f), 0f)
        assertEquals(1f, albumTransitionProgress(1.1f, -.1f), 0f)
    }
}
