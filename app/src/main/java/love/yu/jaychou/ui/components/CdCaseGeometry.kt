package love.yu.jaychou.ui.components

import kotlin.math.cos
import kotlin.math.sin

/** Eight vertices of a solid jewel case. Reused by drawing and touch hit testing. */
internal class CdCaseGeometry {
    val front = FloatArray(8)
    val back = FloatArray(8)
    val spine = FloatArray(8)
    val top = FloatArray(8)

    fun update(size: Float, yaw: Float, pitch: Float, scale: Float) {
        val cy = cos(yaw); val sy = sin(yaw)
        val cx = cos(pitch); val sx = sin(pitch)
        val cameraDistance = size * 6f
        val half = size / 2f
        fun vertex(target: FloatArray, index: Int, x: Float, y: Float, z: Float) {
            val rotatedX = x * cy + z * sy
            val rotatedZ = -x * sy + z * cy
            val rotatedY = y * cx - rotatedZ * sx
            val depth = y * sx + rotatedZ * cx
            val perspective = cameraDistance / (cameraDistance - depth)
            target[index * 2] = half + rotatedX * perspective * scale
            target[index * 2 + 1] = half + rotatedY * perspective * scale
        }
        for (i in 0..3) {
            val x = if (i == 0 || i == 3) -half else half
            val y = if (i < 2) -half else half
            vertex(front, i, x, y, 0f)
            vertex(back, i, x, y, -size * Thickness)
        }
        fun copy(target: FloatArray, i: Int, source: FloatArray, j: Int) {
            target[i * 2] = source[j * 2]
            target[i * 2 + 1] = source[j * 2 + 1]
        }
        if (yaw <= 0f) {
            copy(spine, 0, front, 1); copy(spine, 1, back, 1)
            copy(spine, 2, back, 2); copy(spine, 3, front, 2)
        } else {
            copy(spine, 0, back, 0); copy(spine, 1, front, 0)
            copy(spine, 2, front, 3); copy(spine, 3, back, 3)
        }
        copy(top, 0, back, 0); copy(top, 1, back, 1)
        copy(top, 2, front, 1); copy(top, 3, front, 0)
    }

    fun contains(x: Float, y: Float): Boolean = contains(front, x, y) || contains(spine, x, y)

    private fun contains(points: FloatArray, x: Float, y: Float): Boolean {
        var positive = false; var negative = false
        for (i in 0..3) {
            val j = (i + 1) % 4
            val cross = (points[j * 2] - points[i * 2]) * (y - points[i * 2 + 1]) -
                (points[j * 2 + 1] - points[i * 2 + 1]) * (x - points[i * 2])
            positive = positive || cross > .001f
            negative = negative || cross < -.001f
        }
        return !(positive && negative) && (positive || negative)
    }

    companion object { const val Thickness = .065f }
}
