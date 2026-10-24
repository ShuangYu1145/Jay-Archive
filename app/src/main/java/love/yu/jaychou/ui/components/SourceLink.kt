package love.yu.jaychou.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yu.jaychou.R
import love.yu.jaychou.data.AppText
import love.yu.jaychou.ui.designsystem.Text
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme

@Composable
fun SourceLink(title: String, url: String) {
    val uri = LocalUriHandler.current
    Text(title, Modifier.clickable(role = Role.Button, onClickLabel = AppText.get(R.string.ui_open_source_reference)) { uri.openUri(url) }
        .padding(vertical = 12.dp), fontSize = 13.sp, color = ArchiveUiTheme.colorScheme.primary)
}
