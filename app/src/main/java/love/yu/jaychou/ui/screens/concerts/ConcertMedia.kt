package love.yu.jaychou.ui.screens.concerts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import love.yu.jaychou.R
import love.yu.jaychou.data.AppText
import love.yu.jaychou.data.ConcertImage
import love.yu.jaychou.ui.designsystem.*

/** A one-shot reveal: honours the system animation scale and does no idle animation work. */
@Composable
internal fun ConcertReveal(order: Int = 0, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(420, delayMillis = (order * 55).coerceAtMost(220))) }
    Box(Modifier.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 24.dp.toPx()
    }) { content() }
}

@Composable
internal fun ConcertRemoteImage(image: ConcertImage, label: String, modifier: Modifier = Modifier, allowPreview: Boolean = true) {
    var preview by remember { mutableStateOf(false) }
    val request = ImageRequest.Builder(LocalContext.current).data(image.url)
        .addHeader("Referer", "https://detail.damai.cn/").crossfade(400).build()
    SubcomposeAsyncImage(model = request, contentDescription = null,
        modifier = modifier.fillMaxWidth().aspectRatio(image.aspectRatio)
            .clip(RoundedCornerShape(16.dp)).background(ArchiveUiTheme.colorScheme.surfaceContainerHigh)
            .semantics { contentDescription = label }
            .then(if (allowPreview) Modifier.clickable { preview = true } else Modifier), contentScale = ContentScale.Fit,
        loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } },
        error = { Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
            Text(AppText.get(R.string.concert_image_failed), fontSize = 13.sp)
        } },
    )
    if (preview) Dialog(onDismissRequest = { preview = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val transform = rememberTransformableState { zoom, pan, _ ->
            scale = (scale * zoom).coerceIn(1f, 5f)
            offset = if (scale == 1f) Offset.Zero else offset + pan
        }
        Column(Modifier.fillMaxSize().background(ArchiveUiTheme.colorScheme.background).safeDrawingPadding()
            .testTag("concert-image-preview")) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(AppText.get(R.string.concert_close_image), { preview = false })
            }
            Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(0.dp))
                .pointerInput(Unit) { detectTapGestures(onDoubleTap = { scale = if (scale > 1f) 1f else 2.5f; offset = Offset.Zero }) }
                .transformable(transform), contentAlignment = Alignment.Center) {
                SubcomposeAsyncImage(model = request, contentDescription = label, contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y
                    })
            }
            Text(AppText.get(R.string.concert_zoom_hint), Modifier.padding(20.dp), fontSize = 13.sp)
        }
    }
}
