package love.yu.jaychou.ui.theme

import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import love.yu.jaychou.data.AppSettings

@Composable
internal fun MaterialArchiveTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = settings.isDark()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val colors = remember(dark, settings.monet, settings.customColor, context, configuration) {
        val palette = when {
            settings.monet && Build.VERSION.SDK_INT >= 31 ->
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            settings.customColor != null -> seededMaterialColors(Color(settings.customColor), dark)
            dark -> darkColorScheme()
            else -> lightColorScheme()
        }
        if (dark) palette.copy(background = Color.Black, surface = Color.Black) else palette
    }
    MaterialTheme(colorScheme = colors) {
        Surface(Modifier.fillMaxSize(), color = colors.background, contentColor = colors.onBackground,
            content = content)
    }
}

/** Keep custom Material colors independent of Miuix's palette generator. */
private fun seededMaterialColors(seed: Color, dark: Boolean): androidx.compose.material3.ColorScheme {
    fun on(color: Color) = if (color.luminance() > .45f) Color(0xFF17151C) else Color.White
    val primary = if (dark) lerp(seed, Color.White, .65f) else lerp(seed, Color.Black, .18f)
    val container = lerp(seed, if (dark) Color.Black else Color.White, if (dark) .65f else .83f)
    val surface = lerp(if (dark) Color(0xFF141218) else Color(0xFFFFFBFE), seed, .035f)
    val secondary = lerp(primary, if (dark) Color(0xFFCCC2DC) else Color(0xFF625B71), .65f)
    val secondaryContainer = lerp(container, if (dark) Color(0xFF4A4458) else Color(0xFFE8DEF8), .7f)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary, onPrimary = on(primary),
        primaryContainer = container, onPrimaryContainer = on(container),
        secondary = secondary, onSecondary = on(secondary),
        secondaryContainer = secondaryContainer, onSecondaryContainer = on(secondaryContainer),
        background = surface, surface = surface, onBackground = on(surface), onSurface = on(surface),
        surfaceContainerLow = lerp(surface, primary, .035f),
        surfaceContainer = lerp(surface, primary, .055f),
        surfaceContainerHigh = lerp(surface, primary, .075f),
        surfaceContainerHighest = lerp(surface, primary, .1f),
    )
}
