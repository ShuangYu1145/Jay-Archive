package love.yu.jaychou.ui.designsystem

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** ColorOS window buckets used by the landscape and wide-screen rules. */
internal enum class CouiWindowSize { COMPACT, MEDIUM, EXPANDED }

/**
 * One source of truth for COUI's orientation-dependent measurements.
 *
 * The breakpoints follow the wide-screen rules used by the official COUI/Miuix examples:
 * 840dp is always wide, 600dp is wide only when the window is not tall, and everything below
 * 600dp keeps phone proportions. Landscape itself is decided by width > height, so a short
 * phone window still gets the side rail and the compact header.
 */
@Immutable
internal data class CouiWindowMetrics(
    val windowWidth: Dp,
    val windowHeight: Dp,
    val size: CouiWindowSize,
    val isLandscape: Boolean,
    val isWide: Boolean,
) {
    val railWidth: Dp
        get() = when (size) {
            CouiWindowSize.COMPACT -> 72.dp
            CouiWindowSize.MEDIUM -> 80.dp
            CouiWindowSize.EXPANDED -> 88.dp
        }

    val pageInset: Dp
        get() = when (size) {
            CouiWindowSize.COMPACT -> 16.dp
            CouiWindowSize.MEDIUM -> 20.dp
            CouiWindowSize.EXPANDED -> 24.dp
        }

    val groupSpacing: Dp
        get() = if (isLandscape) 14.dp else 16.dp

    /** The fixed introduction capsule shrinks slightly when vertical space is scarce. */
    val headerVerticalPadding: Dp
        get() = if (isLandscape) 6.dp else 8.dp

    val headerActionSize: Dp
        get() = if (isLandscape) 52.dp else 56.dp

    /** Two 48dp COUI toolbar buttons plus their 8dp gap. */
    val headerActionWidth: Dp
        get() = 104.dp

    /** Exactly the height the fixed introduction header occupies, excluding the status bar. */
    val sectionHeaderHeight: Dp
        get() = headerVerticalPadding * 2 + headerActionSize

    val toolbarHeight: Dp
        get() = 52.dp

    val homeHeroHeight: Dp
        get() = if (isLandscape) 132.dp else 118.dp

    /** The about banner turns into a short strip in landscape instead of a tall phone card. */
    val aboutHeroHeight: Dp
        get() = if (isLandscape) {
            (windowHeight * .55f).coerceIn(180.dp, 240.dp)
        } else {
            300.dp
        }

    val homeMaxWidth: Dp?
        get() = if (isWide) 940.dp else null

    val formMaxWidth: Dp?
        get() = if (isWide) 720.dp else null

    val aboutMaxWidth: Dp?
        get() = if (isWide) 780.dp else null

    val detailMaxWidth: Dp?
        get() = if (isWide) 860.dp else null

    val listMaxWidth: Dp?
        get() = if (isWide) 920.dp else null

    companion object {
        fun of(width: Dp, height: Dp): CouiWindowMetrics {
            val size = when {
                width >= 840.dp -> CouiWindowSize.EXPANDED
                width >= 600.dp -> CouiWindowSize.MEDIUM
                else -> CouiWindowSize.COMPACT
            }
            val isLandscape = width > height
            val ratio = if (width.value > 0f) height.value / width.value else 0f
            val isWide = width >= 840.dp || (width >= 600.dp && ratio < 1.2f)
            return CouiWindowMetrics(width, height, size, isLandscape, isWide)
        }
    }
}

internal val LocalCouiWindowMetrics = staticCompositionLocalOf<CouiWindowMetrics?> { null }

/**
 * Reads the metrics provided by [love.yu.jaychou.navigation.ArchiveApp]. Screens composed on
 * their own (previews and focused UI tests) fall back to the real window size.
 */
@Composable
internal fun rememberCouiWindowMetrics(): CouiWindowMetrics {
    LocalCouiWindowMetrics.current?.let { return it }
    val window = LocalWindowInfo.current
    val density = LocalDensity.current
    return remember(window.containerSize, density) {
        with(density) {
            CouiWindowMetrics.of(
                window.containerSize.width.toDp(),
                window.containerSize.height.toDp(),
            )
        }
    }
}

/**
 * Keeps a reading column centred on wide windows without changing its scroll position.
 * A null [maxWidth] keeps the original edge-to-edge measurement.
 */
internal fun Modifier.couiContentColumn(maxWidth: Dp?): Modifier =
    if (maxWidth == null) fillMaxSize()
    else widthIn(max = maxWidth).fillMaxWidth().fillMaxHeight()
