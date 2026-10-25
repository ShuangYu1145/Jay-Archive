package love.yu.jaychou.ui.components

import love.yu.jaychou.data.AppText
import love.yu.jaychou.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import love.yu.jaychou.ui.designsystem.Text
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme

// Custom vector glyphs keep the three navigation symbols consistent at every density.
enum class Glyph { HOME, ALBUMS, CONCERTS, SETTINGS, INFO, PALETTE, BLUR, GLASS, BACK, LINK, GESTURE, SEARCH }

@Composable
fun ArchiveGlyph(glyph: Glyph, modifier: Modifier = Modifier, color: Color = ArchiveUiTheme.colorScheme.onBackground) {
    Canvas(modifier.size(26.dp)) {
        val unit = size.minDimension / 24f
        fun point(x: Float, y: Float) = Offset(x * unit, y * unit)
        fun path(vararg points: Pair<Float, Float>) {
            val p = Path().apply {
                moveTo(points[0].first * unit, points[0].second * unit)
                points.drop(1).forEach { lineTo(it.first * unit, it.second * unit) }
            }
            drawPath(p, color, style = Stroke(1.9f * unit, cap = StrokeCap.Round))
        }
        when (glyph) {
            Glyph.SEARCH -> {
                drawCircle(color, 6.8f * unit, point(10f, 10f), style = Stroke(1.9f * unit))
                path(15f to 15f, 21f to 21f)
            }
            Glyph.HOME -> {
                path(3f to 10f, 12f to 3f, 21f to 10f, 21f to 21f, 15f to 21f, 15f to 14f, 9f to 14f, 9f to 21f, 3f to 21f, 3f to 10f)
            }
            Glyph.ALBUMS -> {
                drawRect(color, point(3f, 3f), Size(18f * unit, 18f * unit), style = Stroke(1.9f * unit))
                drawCircle(color, 5.2f * unit, point(12f, 12f), style = Stroke(1.7f * unit))
                drawCircle(color, 1.4f * unit, point(12f, 12f))
            }
            Glyph.CONCERTS -> {
                path(9f to 17f, 9f to 4f, 20f to 2f, 20f to 15f)
                path(9f to 8f, 20f to 6f)
                drawOval(color, point(3f, 16f), Size(6f * unit, 5f * unit))
                drawOval(color, point(14f, 14f), Size(6f * unit, 5f * unit))
            }
            Glyph.INFO -> {
                drawCircle(color, 9f * unit, point(12f, 12f), style = Stroke(1.9f * unit))
                drawCircle(color, 1f * unit, point(12f, 7f))
                path(12f to 11f, 12f to 17f)
            }
            Glyph.SETTINGS -> {
                val gear = Path()
                repeat(32) { index ->
                    val angle = index * kotlin.math.PI / 16 - kotlin.math.PI / 2
                    val radius = if (index % 4 in 1..2) 10f else 8f
                    val x = (12f + kotlin.math.cos(angle).toFloat() * radius) * unit
                    val y = (12f + kotlin.math.sin(angle).toFloat() * radius) * unit
                    if (index == 0) gear.moveTo(x, y) else gear.lineTo(x, y)
                }
                gear.close()
                drawPath(gear, color, style = Stroke(1.7f * unit))
                drawCircle(color, 3.2f * unit, point(12f, 12f), style = Stroke(1.7f * unit))
            }
            Glyph.PALETTE -> {
                path(8f to 3f, 3f to 3f, 3f to 8f)
                path(16f to 3f, 21f to 3f, 21f to 8f)
                path(3f to 16f, 3f to 21f, 8f to 21f)
                path(16f to 21f, 21f to 21f, 21f to 16f)
                path(7f to 16f, 11f to 11f, 14f to 14f, 17f to 10f)
                drawCircle(color, 1.5f * unit, point(8f, 8f))
            }
            Glyph.BLUR -> for (row in -2..2) for (col in -2..2) {
                val distance = kotlin.math.abs(row) + kotlin.math.abs(col)
                if (distance < 4) drawCircle(color.copy(alpha = (1f - distance * .18f)), (1.3f - distance * .18f) * unit, point(12f + col * 4f, 12f + row * 4f))
            }
            Glyph.GLASS -> {
                val p = Path().apply {
                    moveTo(12f * unit, 2f * unit)
                    cubicTo(6f * unit, 10f * unit, 3f * unit, 13f * unit, 6f * unit, 18f * unit)
                    cubicTo(10f * unit, 25f * unit, 22f * unit, 20f * unit, 18f * unit, 12f * unit)
                    close()
                }
                drawPath(p, color)
                path(8f to 15f, 9f to 18f, 12f to 19f)
            }
            Glyph.BACK -> path(14f to 5f, 7f to 12f, 14f to 19f)
            Glyph.LINK -> path(9f to 5f, 16f to 12f, 9f to 19f)
            Glyph.GESTURE -> {
                path(3f to 7f, 14f to 7f)
                path(3f to 12f, 11f to 12f)
                path(3f to 17f, 14f to 17f)
                path(21f to 8f, 17f to 12f, 21f to 16f)
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 23.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
        if (subtitle != null) QuietText(subtitle)
    }
}

@Composable
fun QuietText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(text, modifier, fontSize = 13.sp, lineHeight = 20.sp, maxLines = maxLines, overflow = overflow,
        color = ArchiveUiTheme.colorScheme.onBackgroundVariant)
}

/** Network loading, explicit progress/error states, retry, and a bundled offline fallback. */
@Composable
fun CatalogImage(
    url: String, asset: String, description: String, modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    onAspectRatio: ((Float) -> Unit)? = null,
    crossfade: Boolean = true,
    showStatus: Boolean = true,
) {
    var attempt by remember(url) { mutableIntStateOf(0) }
    val context = LocalContext.current
    val loadingLabel = stringResource(R.string.ui_loading_online_image)
    val offlineLabel = stringResource(R.string.ui_offline_image_tap_to_retry)
    val retryLabel = stringResource(R.string.ui_retry_online_image)
    key(url, attempt) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context).data(url).crossfade(crossfade).build(),
            contentDescription = description,
            contentScale = contentScale,
            onSuccess = { state ->
                val drawable = state.result.drawable
                if (drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
                    onAspectRatio?.invoke(drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight)
                }
            },
            modifier = modifier,
            loading = {
                Box(Modifier.fillMaxSize()) {
                    AsyncImage("file:///android_asset/$asset", null, Modifier.fillMaxSize(), contentScale = contentScale)
                    if (showStatus) ImageStatus(loadingLabel, Modifier.align(Alignment.BottomStart))
                }
            },
            error = {
                Box(Modifier.fillMaxSize()) {
                    AsyncImage("file:///android_asset/$asset", description, Modifier.fillMaxSize(), contentScale = contentScale,
                        onSuccess = { state ->
                            val drawable = state.result.drawable
                            if (drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
                                onAspectRatio?.invoke(drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight)
                            }
                        })
                    if (showStatus) ImageStatus(offlineLabel, Modifier.align(Alignment.BottomStart)
                        .clickable(role = Role.Button, onClickLabel = retryLabel) { attempt++ })
                }
            },
        )
    }
}

@Composable
private fun ImageStatus(label: String, modifier: Modifier) {
    Text(label, modifier.background(Color.Black.copy(alpha = .65f)).padding(horizontal = 8.dp, vertical = 4.dp),
        color = Color.White, fontSize = 10.sp)
}

@Composable
fun BackButton(onClick: () -> Unit, label: String = AppText.get(R.string.ui_back_to_albums)) {
    Box(Modifier.size(48.dp).clip(CircleShape)
        .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
        .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        ArchiveGlyph(Glyph.BACK)
    }
}
