package love.yu.jaychou.ui.components

import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import kotlin.math.abs
import kotlin.math.sign

internal fun caseScale(distance: Float): Float =
    (1f - .12f * abs(distance).coerceAtMost(1f) - .025f * (abs(distance) - 1f).coerceAtLeast(0f)).coerceAtLeast(.62f)

internal fun caseYaw(distance: Float): Float = -sign(distance) * 66f * abs(distance).coerceAtMost(1f) * (Math.PI.toFloat() / 180f)
internal fun casePitch(distance: Float): Float = -3f * abs(distance).coerceAtMost(1f) * (Math.PI.toFloat() / 180f)
internal fun caseTranslation(distance: Float, size: Float): Float = sign(distance) * size *
    (.66f * abs(distance).coerceAtMost(1f) + .30f * (abs(distance) - 1f).coerceAtLeast(0f))

/**
 * Draw a solid CD jewel case, not a rotated rectangle. Each face uses the same eight
 * perspective-projected vertices. Artwork stays in a cached GPU layer; no bitmap readback.
 */
@Composable
internal fun CdCaseArtwork(title: String, year: String, distance: () -> Float,
                           modifier: Modifier, content: @Composable () -> Unit) {
    val artwork = rememberGraphicsLayer()
    Box(modifier.drawWithCache {
        val geometry = CdCaseGeometry()
        val matrix = Matrix()
        val path = Path()
        val edge = Stroke(.75f.coerceAtLeast(size.width * .0015f))
        val source = floatArrayOf(0f, 0f, size.width, 0f, size.width, size.height, 0f, size.height)
        val spineSource = floatArrayOf(0f, 0f, size.width * CdCaseGeometry.Thickness, 0f,
            size.width * CdCaseGeometry.Thickness, size.height, 0f, size.height)
        val spinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(35, 39, 44)
            textSize = size.width * CdCaseGeometry.Thickness * .46f
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }
        val label = "$title  ·  $year"
        val labelLength = spinePaint.breakText(label, true, size.height * .86f, null)
        val spineLabel = if (labelLength < label.length) label.take((labelLength - 1).coerceAtLeast(0)) + "…" else label
        val hinge = Brush.horizontalGradient(listOf(Color.Black.copy(alpha = .35f),
            Color.White.copy(alpha = .32f), Color.Black.copy(alpha = .12f)))
        fun outline(vertices: FloatArray) {
            path.reset(); path.moveTo(vertices[0], vertices[1])
            for (i in 1..3) path.lineTo(vertices[i * 2], vertices[i * 2 + 1])
            path.close()
        }
        onDrawWithContent {
            val d = distance()
            geometry.update(size.width, caseYaw(d), casePitch(d), caseScale(d))
            outline(geometry.back)
            drawPath(path, Color(0xFF252A30))
            outline(geometry.top)
            drawPath(path, Color(0xFFCCD1D6))
            drawPath(path, Color.White.copy(alpha = .7f), style = edge)
            if (abs(d) > .001f) {
                outline(geometry.spine)
                drawPath(path, Color(0xFFB8BEC5))
                drawPath(path, Color(0xFFF0F2F4), style = edge)
                matrix.setPolyToPoly(spineSource, 0, geometry.spine, 0, 4)
                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas
                    native.save(); native.concat(matrix)
                    native.translate(size.width * CdCaseGeometry.Thickness * .5f, size.height * .07f)
                    native.rotate(90f)
                    native.drawText(spineLabel, 0f, spinePaint.textSize * .35f, spinePaint)
                    native.restore()
                }
            }
            artwork.record {
                this@onDrawWithContent.drawContent()
                // Thin clear-plastic hinge and cover rim, retaining a square front face.
                drawRect(hinge, size = Size(size.width * .018f, size.height))
                drawRect(Color.White.copy(alpha = .34f), style = edge)
                drawLine(Color.White.copy(alpha = .38f), Offset(0f, 1f), Offset(size.width, 1f))
            }
            matrix.setPolyToPoly(source, 0, geometry.front, 0, 4)
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                native.save(); native.concat(matrix)
                drawLayer(artwork)
                native.restore()
            }
        }
    }.fillMaxSize(), content = { content() })
}
