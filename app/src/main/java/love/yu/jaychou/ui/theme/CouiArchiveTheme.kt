package love.yu.jaychou.ui.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.ColorSchemeMode
import io.github.suqi8.coui.kmp.theme.ThemeController
import love.yu.jaychou.data.AppSettings
import love.yu.jaychou.data.ThemeMode

/** Native COUI defaults preserve ColorOS colors, typography and motion. */
@Composable
internal fun CouiArchiveTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dynamic = settings.monet && Build.VERSION.SDK_INT >= 31
    val seed = if (dynamic) null else settings.customColor?.let { Color(it) }
    val mode = when (settings.theme) {
        ThemeMode.SYSTEM -> if (dynamic || seed != null) ColorSchemeMode.MonetSystem else ColorSchemeMode.System
        ThemeMode.LIGHT -> if (dynamic || seed != null) ColorSchemeMode.MonetLight else ColorSchemeMode.Light
        ThemeMode.DARK -> if (dynamic || seed != null) ColorSchemeMode.MonetDark else ColorSchemeMode.Dark
    }
    val controller = remember(mode, seed) { ThemeController(colorSchemeMode = mode, keyColor = seed) }
    val dark = settings.isDark()
    COUITheme(controller = controller) {
        val colors = COUITheme.colorScheme
        // A grouped settings canvas separates native COUI cards from the page.
        COUITheme(colors = colors.copy(
            background = if (dark) Color(0xFF101010) else Color(0xFFF0F1F2),
            surfaceContainer = if (dark) Color(0xFF242426) else colors.surfaceContainer,
        ), content = content)
    }
}
