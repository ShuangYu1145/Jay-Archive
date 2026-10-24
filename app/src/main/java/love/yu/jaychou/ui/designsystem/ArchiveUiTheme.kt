package love.yu.jaychou.ui.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import love.yu.jaychou.data.UiStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalUiStyle = staticCompositionLocalOf { UiStyle.MIUIX }

@Immutable
data class ArchiveColors(
    val background: Color, val onBackground: Color, val onBackgroundVariant: Color,
    val surface: Color, val onSurface: Color, val surfaceContainer: Color,
    val onSurfaceContainer: Color, val onSurfaceContainerVariant: Color,
    val surfaceContainerHigh: Color, val surfaceContainerHighest: Color,
    val primary: Color, val primaryContainer: Color, val onPrimaryContainer: Color,
    val secondaryContainer: Color, val onSecondaryContainer: Color,
    val errorContainer: Color, val onErrorContainer: Color, val dividerLine: Color,
)

@Immutable
data class ArchiveTextStyles(
    val main: TextStyle, val title1: TextStyle, val title2: TextStyle,
    val body1: TextStyle, val footnote1: TextStyle,
)

/** Shared content reads tokens here; each design system keeps its native components. */
object ArchiveUiTheme {
    val isMaterial: Boolean @Composable get() = LocalUiStyle.current == UiStyle.MATERIAL3

    val colorScheme: ArchiveColors
        @Composable get() = if (isMaterial) {
            val c = MaterialTheme.colorScheme
            ArchiveColors(c.background, c.onBackground, c.onSurfaceVariant,
                c.surface, c.onSurface, c.surfaceContainerLow, c.onSurface, c.onSurfaceVariant,
                c.surfaceContainerHigh, c.surfaceContainerHighest, c.primary, c.primaryContainer,
                c.onPrimaryContainer, c.secondaryContainer, c.onSecondaryContainer,
                c.errorContainer, c.onErrorContainer, c.outlineVariant)
        } else {
            val c = MiuixTheme.colorScheme
            ArchiveColors(c.background, c.onBackground, c.onBackgroundVariant,
                c.surface, c.onSurface, c.surfaceContainer, c.onSurfaceContainer, c.onSurfaceContainerVariant,
                c.surfaceContainerHigh, c.surfaceContainerHighest, c.primary, c.primaryContainer,
                c.onPrimaryContainer, c.secondaryContainer, c.onSecondaryContainer,
                c.errorContainer, c.onErrorContainer, c.dividerLine)
        }

    val textStyles: ArchiveTextStyles
        @Composable get() = if (isMaterial) {
            val t = MaterialTheme.typography
            ArchiveTextStyles(t.bodyLarge, t.headlineLarge, t.titleLarge, t.bodyLarge, t.bodySmall)
        } else {
            val t = MiuixTheme.textStyles
            ArchiveTextStyles(t.main, t.title1, t.title2, t.body1, t.footnote1)
        }
}
