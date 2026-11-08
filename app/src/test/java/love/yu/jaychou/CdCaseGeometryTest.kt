package love.yu.jaychou

import love.yu.jaychou.ui.components.CdCaseGeometry
import org.junit.Assert.*
import org.junit.Test

class CdCaseGeometryTest {
    @Test fun frontIsAnExactSquareWhenCentered() {
        val case = CdCaseGeometry()
        case.update(300f, 0f, 0f, 1f)
        assertArrayEquals(floatArrayOf(0f, 0f, 300f, 0f, 300f, 300f, 0f, 300f), case.front, .001f)
        assertTrue(case.contains(150f, 150f))
        assertFalse(case.contains(-10f, 150f))
    }

    @Test fun turningEitherWayExposesASolidSpineWithSharedFrontEdges() {
        for (yaw in listOf(-1.15f, 1.15f)) {
            val case = CdCaseGeometry()
            case.update(300f, yaw, -.05f, .88f)
            val spine = case.spine
            val area = (0..3).sumOf { i ->
                val j = (i + 1) % 4
                (spine[i * 2] * spine[j * 2 + 1] - spine[j * 2] * spine[i * 2 + 1]).toDouble()
            }
            assertTrue("Spine must have visible area rather than being a flat line", kotlin.math.abs(area) > 1000)
            assertTrue(case.contains((spine[0] + spine[2] + spine[4] + spine[6]) / 4f,
                (spine[1] + spine[3] + spine[5] + spine[7]) / 4f))
            val frontVertex = if (yaw < 0) 1 else 0
            val spineVertex = if (yaw < 0) 0 else 1
            assertEquals(case.front[frontVertex * 2], spine[spineVertex * 2], .001f)
            assertEquals(case.front[frontVertex * 2 + 1], spine[spineVertex * 2 + 1], .001f)
            assertTrue(case.front.all { it.isFinite() } && case.back.all { it.isFinite() })
        }
    }
}
