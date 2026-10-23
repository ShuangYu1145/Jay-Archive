package love.yu.jaychou.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import love.yu.jaychou.data.AppSettings
import love.yu.jaychou.data.ThemeMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

@Composable
fun AppSettings.isDark(): Boolean = when (theme) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun ArchiveTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val textResources = remember(context, configuration) {
        context.createConfigurationContext(configuration).resources
    }
    love.yu.jaychou.data.AppText.update(textResources)
    val mode = if ((settings.monet && Build.VERSION.SDK_INT >= 31) || settings.customColor != null) {
        when (settings.theme) {
            ThemeMode.SYSTEM -> ColorSchemeMode.MonetSystem
            ThemeMode.LIGHT -> ColorSchemeMode.MonetLight
            ThemeMode.DARK -> ColorSchemeMode.MonetDark
        }
    } else {
        when (settings.theme) {
            ThemeMode.SYSTEM -> ColorSchemeMode.System
            ThemeMode.LIGHT -> ColorSchemeMode.Light
            ThemeMode.DARK -> ColorSchemeMode.Dark
        }
    }
    // System Monet has priority; a custom seed works on API 26+ using Miuix's palette generator.
    val seed = if (settings.monet && Build.VERSION.SDK_INT >= 31) null else settings.customColor?.let { Color(it) }
    val controller = remember(mode, seed) {
        ThemeController(
            colorSchemeMode = mode,
            keyColor = seed,
            lightColors = lightColorScheme(
                primary = Color(0xFF4979F5), background = Color(0xFFF6F6F8),
                onBackgroundVariant = Color(0xFF656872),
            ),
            darkColors = darkColorScheme(
                primary = Color(0xFF9CB8FF), background = Color(0xFF111318),
                surface = Color(0xFF111318), surfaceContainer = Color(0xFF22252D),
                onBackgroundVariant = Color(0xFFB5BAC9),
            ),
        )
    }
    MiuixTheme(controller = controller, content = content)
}
