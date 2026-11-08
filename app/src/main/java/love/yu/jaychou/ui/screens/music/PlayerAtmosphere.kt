package love.yu.jaychou.ui.screens.music

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.drawable.BitmapDrawable
import coil.imageLoader
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme

internal data class PlayerAtmosphere(val top: Color, val center: Color, val bottom: Color, val ink: Color)

/** Warm the real artwork before the mini player is tapped, so the transition has no placeholder. */
@Composable
internal fun rememberPlayerArtwork(url: String): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val artwork by produceState<ImageBitmap?>(null, context, url) {
        if (url.isBlank()) return@produceState
        val result = context.imageLoader.execute(ImageRequest.Builder(context).data(url).size(600).allowHardware(false).build())
        value = (result.drawable as? BitmapDrawable)?.bitmap?.asImageBitmap()
    }
    return artwork
}

/** Diffuse cover colors, without decoding a full-resolution image or running blur every frame. */
@Composable
internal fun rememberPlayerAtmosphere(artwork: ImageBitmap?): PlayerAtmosphere {
    val dark = ArchiveUiTheme.colorScheme.background.luminance() < .5f
    val sampled by produceState(listOf(Color(0xFF8B9EAB), Color(0xFFC6BCA3), Color(0xFF758E9D)), artwork) {
        val bitmap = artwork?.asAndroidBitmap() ?: return@produceState
        value = withContext(Dispatchers.Default) {
            (0..2).map { band ->
                var red = 0L; var green = 0L; var blue = 0L; var count = 0
                for (y in (bitmap.height * band / 3) until (bitmap.height * (band + 1) / 3) step 8) {
                    for (x in 0 until bitmap.width step 8) {
                        val pixel = bitmap.getPixel(x, y)
                        red += android.graphics.Color.red(pixel); green += android.graphics.Color.green(pixel)
                        blue += android.graphics.Color.blue(pixel); count++
                    }
                }
                if (count == 0) Color.Gray else Color((red / count).toInt(), (green / count).toInt(), (blue / count).toInt())
            }
        }
    }
    val base = if (dark) Color(0xFF101920) else Color(0xFFE9E7DF)
    val amount = if (dark) .28f else .27f
    return remember(sampled, dark) {
        PlayerAtmosphere(lerp(base, sampled[0], amount), lerp(base, sampled[1], amount),
            lerp(base, sampled[2], amount), if (dark) Color(0xFFF1F3F1) else Color(0xFF07141D))
    }
}

@Composable
internal fun PlayerAtmosphereBackground(palette: PlayerAtmosphere) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(palette.top, palette.center, palette.bottom)))
        drawRect(Brush.radialGradient(listOf(palette.center.copy(alpha = .5f), Color.Transparent),
            center = Offset(size.width * .8f, size.height * .65f), radius = size.width * 1.2f))
    }
}
