package love.yu.jaychou

import love.yu.jaychou.ui.designsystem.CouiWindowMetrics
import love.yu.jaychou.ui.designsystem.CouiWindowSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.unit.dp

class CouiAdaptiveMetricsTest {
    @Test fun portraitPhoneKeepsCompactPhoneMetrics() {
        val metrics = CouiWindowMetrics.of(400.dp, 800.dp)
        assertEquals(CouiWindowSize.COMPACT, metrics.size)
        assertFalse(metrics.isLandscape)
        assertFalse(metrics.isWide)
        assertEquals(72.dp, metrics.railWidth)
        assertEquals(16.dp, metrics.pageInset)
        assertEquals(72.dp, metrics.sectionHeaderHeight)
        assertNull(metrics.formMaxWidth)
        assertNull(metrics.homeMaxWidth)
    }

    @Test fun phoneLandscapeCompactsTheHeaderButStaysNarrow() {
        val metrics = CouiWindowMetrics.of(560.dp, 360.dp)
        assertEquals(CouiWindowSize.COMPACT, metrics.size)
        assertTrue(metrics.isLandscape)
        assertFalse(metrics.isWide)
        assertEquals(52.dp, metrics.headerActionSize)
        assertEquals(104.dp, metrics.headerActionWidth)
        assertEquals(64.dp, metrics.sectionHeaderHeight)
        assertTrue(kotlin.math.abs(metrics.aboutHeroHeight.value - 198f) < .01f)
        assertNull(metrics.aboutMaxWidth)
    }

    @Test fun tallTabletIsTallRatherThanWide() {
        val metrics = CouiWindowMetrics.of(800.dp, 1200.dp)
        assertEquals(CouiWindowSize.MEDIUM, metrics.size)
        assertFalse(metrics.isLandscape)
        assertFalse(metrics.isWide)
        assertEquals(80.dp, metrics.railWidth)
        assertNull(metrics.formMaxWidth)
    }

    @Test fun landscapeTabletCentresItsReadingColumns() {
        val metrics = CouiWindowMetrics.of(800.dp, 600.dp)
        assertEquals(CouiWindowSize.MEDIUM, metrics.size)
        assertTrue(metrics.isLandscape)
        assertTrue(metrics.isWide)
        assertEquals(940.dp, metrics.homeMaxWidth)
        assertEquals(720.dp, metrics.formMaxWidth)
    }

    @Test fun expandedTabletUsesWiderRailAndInsets() {
        val metrics = CouiWindowMetrics.of(1200.dp, 800.dp)
        assertEquals(CouiWindowSize.EXPANDED, metrics.size)
        assertTrue(metrics.isLandscape)
        assertTrue(metrics.isWide)
        assertEquals(88.dp, metrics.railWidth)
        assertEquals(24.dp, metrics.pageInset)
        assertEquals(720.dp, metrics.formMaxWidth)
        assertEquals(780.dp, metrics.aboutMaxWidth)
        assertEquals(240.dp, metrics.aboutHeroHeight)
    }

    @Test fun widthAloneCanMakeAPortraitWindowWide() {
        val metrics = CouiWindowMetrics.of(840.dp, 1000.dp)
        assertEquals(CouiWindowSize.EXPANDED, metrics.size)
        assertFalse(metrics.isLandscape)
        assertTrue(metrics.isWide)
        assertEquals(88.dp, metrics.railWidth)
    }

    @Test fun sixHundredWideOnlySplitsWhenItIsNearlySquare() {
        val square = CouiWindowMetrics.of(600.dp, 700.dp)
        assertTrue(square.isWide)
        val tall = CouiWindowMetrics.of(600.dp, 1000.dp)
        assertFalse(tall.isWide)
    }
}
