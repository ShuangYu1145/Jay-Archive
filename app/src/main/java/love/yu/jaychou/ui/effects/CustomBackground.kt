package love.yu.jaychou.ui.effects

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File
import love.yu.jaychou.data.AppSettings
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

internal val LocalCustomBackgroundPath = staticCompositionLocalOf<String?> { null }
internal val LocalWallpaperBackdrop = staticCompositionLocalOf<Backdrop?> { null }
internal val LocalFloatingBottomBar = staticCompositionLocalOf { false }
private val LocalBackgroundCardBlur = staticCompositionLocalOf { false }
internal const val BackgroundBlurSurfaceAlpha = .72f

/** Capture only the wallpaper: card text, artwork and other cards never enter this blur pass. */
@Composable
internal fun CustomBackgroundHost(settings: AppSettings, content: @Composable () -> Unit) {
    val path = settings.customBackgroundPath
    var failed by remember(path) { mutableStateOf(false) }
    val available = remember(path) { path?.takeIf { File(it).isFile } }.takeUnless { failed }
    // Keep the wallpaper source stable when either blur switch changes. Bars need it even
    // when card blur is off, because transparent page captures don't contain the wallpaper.
    val backdrop = if (Build.VERSION.SDK_INT >= 33 && available != null)
        rememberLayerBackdrop() else null
    CompositionLocalProvider(
        LocalCustomBackgroundPath provides available,
        LocalWallpaperBackdrop provides backdrop,
        LocalFloatingBottomBar provides settings.floatingBottomBar,
        LocalBackgroundCardBlur provides settings.backgroundCardBlur,
    ) {
        Box(Modifier.fillMaxSize().background(ArchiveUiTheme.colorScheme.background)) {
            Box(Modifier.fillMaxSize().then(
                if (Build.VERSION.SDK_INT >= 33 && backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier,
            )) {
                CustomBackgroundImage(onError = { failed = true })
            }
            content()
        }
    }
}

@Composable
internal fun CustomBackgroundImage(modifier: Modifier = Modifier, onError: () -> Unit = {}) {
    val path = LocalCustomBackgroundPath.current ?: return
    val dark = ArchiveUiTheme.colorScheme.background.luminance() < .5f
    Box(modifier.fillMaxSize().background(ArchiveUiTheme.colorScheme.background)) {
        AsyncImage(
            model = File(path), contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().testTag("custom-background-image"),
            onError = { onError() },
        )
        // Keep unboxed titles readable over both bright and dark photographs.
        Box(Modifier.fillMaxSize().background(if (dark) Color.Black.copy(alpha = .35f) else Color.White.copy(alpha = .38f)))
    }
}

@Composable
internal fun Modifier.backgroundCardBlur(shape: Shape): Modifier {
    val backdrop = LocalWallpaperBackdrop.current
    if (!backgroundCardBlurEnabled() || backdrop == null) return this
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = { nativeBackdropBlur(24.dp.toPx()) },
    )
}

@Composable
internal fun backgroundCardBlurEnabled(): Boolean =
    Build.VERSION.SDK_INT >= 33 && LocalWallpaperBackdrop.current != null && LocalBackgroundCardBlur.current
