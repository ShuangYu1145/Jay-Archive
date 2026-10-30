package love.yu.jaychou.ui.designsystem

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import love.yu.jaychou.ui.components.LocalArchiveBlur
import love.yu.jaychou.ui.effects.couiFrostedSurface
import androidx.compose.ui.unit.dp

/** Fits the 52dp toolbar and shares the floating navigation frost. */
@Composable
internal fun CouiToolbarButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    io.github.suqi8.coui.kmp.basic.IconButton(
        onClick = onClick, enabled = enabled,
        modifier = modifier.size(48.dp).couiFrostedSurface(LocalArchiveBlur.current),
        backgroundColor = Color.Transparent,
        cornerRadius = 24.dp,
        minWidth = 48.dp, minHeight = 48.dp,
        content = content,
    )
}
