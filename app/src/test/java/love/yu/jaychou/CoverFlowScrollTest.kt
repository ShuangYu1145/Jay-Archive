package love.yu.jaychou

import love.yu.jaychou.ui.components.CoverFlowScrollState
import org.junit.Assert.assertEquals
import org.junit.Test

class CoverFlowScrollTest {
    @Test fun consumesOnlyAvailableDistanceAtBothEdges() {
        val state = CoverFlowScrollState(5)
        state.stride = 100f
        assertEquals(0f, state.scroll.dispatchRawDelta(-150f), .001f)
        assertEquals(125f, state.scroll.dispatchRawDelta(125f), .001f)
        assertEquals(1.25f, state.position, .001f)
        assertEquals(275f, state.scroll.dispatchRawDelta(1000f), .001f)
        assertEquals(4, state.selectedIndex)
        assertEquals(-400f, state.scroll.dispatchRawDelta(-1000f), .001f)
        assertEquals(0, state.selectedIndex)
    }

    @Test fun reverseDragSelectsNearestCoverWithoutAccumulatedError() {
        val state = CoverFlowScrollState(8, 3)
        state.stride = 80f
        state.scroll.dispatchRawDelta(130f)
        assertEquals(5, state.selectedIndex)
        state.scroll.dispatchRawDelta(-100f)
        assertEquals(3, state.selectedIndex)
        assertEquals(3.375f, state.position, .001f)
    }

    @Test fun emptyAndSingletonRemainCentered() {
        for (count in 0..1) {
            val state = CoverFlowScrollState(count, 20)
            assertEquals(0f, state.scroll.dispatchRawDelta(1000f), .001f)
            assertEquals(0f, state.scroll.dispatchRawDelta(-1000f), .001f)
            assertEquals(0, state.selectedIndex)
        }
    }
}
