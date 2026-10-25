package love.yu.jaychou.ui.updates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import love.yu.jaychou.BuildConfig
import love.yu.jaychou.R
import love.yu.jaychou.data.AppText
import love.yu.jaychou.data.UpdateResult
import love.yu.jaychou.ui.designsystem.*

@Composable
internal fun UpdateDialog(
    notice: UpdateNotice, autoCheckEnabled: Boolean, saving: Boolean, saveError: String?,
    onDismiss: () -> Unit, onDownload: () -> Unit, onDisableAutomatic: () -> Unit,
) {
    val update = (notice.result as? UpdateResult.Available)?.update
    val title = when {
        update != null -> AppText.get(R.string.ui_update_available, update.versionName)
        notice.problem != null -> AppText.get(R.string.ui_update_failed)
        notice.result == UpdateResult.UpToDate -> AppText.get(R.string.ui_update_latest)
        else -> AppText.get(R.string.ui_update_no_release)
    }
    val message = when {
        update != null -> AppText.get(R.string.ui_update_version_change, BuildConfig.VERSION_NAME, update.versionName) +
            "\n\n" + update.notes.ifBlank { AppText.get(R.string.ui_update_no_notes) }
        notice.problem == UpdateProblem.RATE_LIMIT -> AppText.get(R.string.ui_update_rate_limit)
        notice.problem == UpdateProblem.OPEN_DOWNLOAD -> AppText.get(R.string.ui_update_open_failed)
        notice.problem != null -> AppText.get(R.string.ui_update_network_error)
        notice.result == UpdateResult.UpToDate -> AppText.get(R.string.ui_update_current_version, BuildConfig.VERSION_NAME)
        else -> AppText.get(R.string.ui_update_no_release_summary)
    }
    val body: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().heightIn(max = LocalWindowInfo.current.containerDpSize.height * .5f)
            .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(message, modifier = Modifier.testTag("update-message"))
            if (update != null && autoCheckEnabled) TextButton(
                AppText.get(R.string.ui_update_disable_automatic), onDisableAutomatic,
                modifier = Modifier.testTag("update-disable-automatic"), enabled = !saving,
            )
            saveError?.let { Text(it) }
        }
    }
    val confirm: @Composable () -> Unit = {
        TextButton(AppText.get(if (update == null) R.string.ui_ok else R.string.ui_update_download),
            if (update == null) onDismiss else onDownload,
            modifier = (if (ArchiveUiTheme.isMaterial) Modifier else Modifier.fillMaxWidth())
                .testTag("update-confirm"), enabled = !saving)
    }
    val dismiss: @Composable () -> Unit = {
        TextButton(AppText.get(R.string.ui_update_later), onDismiss,
            modifier = (if (ArchiveUiTheme.isMaterial) Modifier else Modifier.fillMaxWidth())
                .testTag("update-dismiss"), enabled = !saving)
    }
    if (ArchiveUiTheme.isMaterial) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss, modifier = Modifier.testTag("update-dialog-material"),
            title = { Text(title) }, text = body, confirmButton = confirm,
            dismissButton = if (update == null) null else dismiss,
        )
    } else {
        Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            top.yukonga.miuix.kmp.basic.Card(
                Modifier.padding(horizontal = 24.dp).widthIn(max = 420.dp).fillMaxWidth().testTag("update-dialog-miuix"),
                cornerRadius = 32.dp, insideMargin = PaddingValues(24.dp),
            ) {
                Text(title, Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    style = ArchiveUiTheme.textStyles.title2, textAlign = TextAlign.Center)
                body()
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (update != null) Box(Modifier.weight(1f)) { dismiss() }
                    Box(Modifier.weight(1f)) { confirm() }
                }
            }
        }
    }
}
