package love.yu.jaychou.ui.screens.lyrics

import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import love.yu.jaychou.data.Album
import love.yu.jaychou.data.LyricsBackground
import love.yu.jaychou.data.Track
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme

internal data class ReaderPalette(val stops: List<Color>, val foreground: Color)
private val artworkColors = LruCache<String, Int>(16)

@Composable
internal fun rememberReaderPalette(album: Album, track: Track, mode: LyricsBackground): ReaderPalette {
    val colors = ArchiveUiTheme.colorScheme
    val context = LocalContext.current
    val url = track.artwork?.coverUrl ?: album.coverUrl
    val asset = track.artwork?.coverAsset ?: album.coverAsset
    val accent by produceState<Color?>(null, url, asset, mode) {
        if (mode != LyricsBackground.ALBUM) return@produceState
        value = withContext(Dispatchers.IO) {
            artworkColors.get(url)?.let { return@withContext Color(it) }
            try {
                // Reuse Coil's cache; a bundled thumbnail also works without a network connection.
                suspend fun load(data: String) = context.imageLoader.execute(ImageRequest.Builder(context)
                    .data(data).size(48).allowHardware(false).build()) as? SuccessResult
                val bundled = context.assets.runCatching { open(asset).use { } }.isSuccess
                val result = load(if (bundled) "file:///android_asset/$asset" else url)
                val bitmap = (result?.drawable as? BitmapDrawable)?.bitmap ?: return@withContext null
                val bins = IntArray(512)
                var winner = -1
                for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                    val pixel = bitmap.getPixel(x, y)
                    if (android.graphics.Color.alpha(pixel) < 192) continue
                    val r = android.graphics.Color.red(pixel)
                    val g = android.graphics.Color.green(pixel)
                    val b = android.graphics.Color.blue(pixel)
                    if (maxOf(r, g, b) < 24 || minOf(r, g, b) > 238) continue
                    val index = (r shr 5 shl 6) or (g shr 5 shl 3) or (b shr 5)
                    bins[index]++
                    if (winner < 0 || bins[index] > bins[winner]) winner = index
                }
                if (winner < 0) return@withContext null
                val dominant = android.graphics.Color.rgb(
                    ((winner shr 6) and 7) * 32 + 16,
                    ((winner shr 3) and 7) * 32 + 16,
                    (winner and 7) * 32 + 16,
                )
                artworkColors.put(url, dominant)
                Color(dominant)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) { null }
        }
    }
    return remember(mode, accent, colors.background, colors.onSurface) {
        val dark = colors.background.luminance() < .5f
        val base = if (mode == LyricsBackground.PLAIN) {
            if (dark) Color(0xFF17191C) else Color(0xFFFAF7F0)
        } else colors.background
        val stops = if (mode == LyricsBackground.ALBUM && accent != null) {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(accent!!.toArgb(), hsv)
            hsv[1] = hsv[1].coerceAtMost(.22f)
            val muted = Color(android.graphics.Color.HSVToColor(hsv))
            listOf(lerp(base, muted, if (dark) .12f else .09f), lerp(base, muted, .025f), base)
        } else listOf(base, base, base)
        fun contrast(a: Color, b: Color): Float {
            val l1 = a.luminance(); val l2 = b.luminance()
            return (maxOf(l1, l2) + .05f) / (minOf(l1, l2) + .05f)
        }
        val foreground = colors.onSurface.takeIf { candidate -> stops.all { contrast(candidate, it) >= 4.5f } }
            ?: if (dark) Color.White else Color(0xFF151515)
        ReaderPalette(stops, foreground)
    }
}
