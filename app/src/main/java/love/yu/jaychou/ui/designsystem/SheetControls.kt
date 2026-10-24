package love.yu.jaychou.ui.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindowBottomSheet(show: Boolean, title: String, onDismissRequest: () -> Unit,
                      startAction: @Composable () -> Unit = {}, endAction: @Composable () -> Unit = {},
                      content: @Composable ColumnScope.() -> Unit) {
    if (!ArchiveUiTheme.isMaterial) {
        top.yukonga.miuix.kmp.window.WindowBottomSheet(show, title = title,
            onDismissRequest = onDismissRequest, startAction = startAction, endAction = endAction,
            content = { Column(content = content) })
        return
    }
    if (!show) return
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * .8f
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = maxHeight)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                startAction()
                androidx.compose.material3.Text(title, Modifier.weight(1f).padding(horizontal = 8.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                endAction()
            }
            Column(Modifier.weight(1f, fill = false).padding(horizontal = 24.dp, vertical = 16.dp), content = content)
        }
    }
}
