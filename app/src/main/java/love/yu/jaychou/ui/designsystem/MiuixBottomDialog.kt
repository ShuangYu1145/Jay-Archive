package love.yu.jaychou.ui.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.squircle.squircleSurface

/** Native Miuix surface and controls, bottom aligned in every window size. */
@Composable
internal fun MiuixBottomDialog(
    show: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!show) return
    Dialog(onDismissRequest, properties = DialogProperties(
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
    )) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()
            .clickable(onClick = onDismissRequest).padding(12.dp)) {
            Column(modifier.align(Alignment.BottomCenter)
                .widthIn(max = DialogDefaults.MaxWidth).fillMaxWidth()
                .heightIn(max = maxHeight)
                .squircleSurface(color = ArchiveUiTheme.colorScheme.surfaceContainer, cornerRadius = 32.dp)
                .pointerInput(Unit) { detectTapGestures { } }
                .verticalScroll(rememberScrollState()).padding(24.dp)) {
                Text(title, Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    style = ArchiveUiTheme.textStyles.title2, fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center)
                content()
            }
        }
    }
}
