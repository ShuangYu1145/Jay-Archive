package love.yu.jaychou.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import love.yu.jaychou.ui.designsystem.Icon
import love.yu.jaychou.ui.designsystem.IconButton
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme

/** Compact artwork with an accessible touch target and Miuix press feedback. */
@Composable
internal fun ArchiveIconButton(
    icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp),
        backgroundColor = Color.Transparent, minWidth = 48.dp, minHeight = 48.dp) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(24.dp),
            tint = ArchiveUiTheme.colorScheme.onBackground)
    }
}
