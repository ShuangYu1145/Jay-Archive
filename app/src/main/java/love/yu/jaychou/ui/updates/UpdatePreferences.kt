package love.yu.jaychou.ui.updates

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import java.io.IOException
import kotlinx.coroutines.launch
import love.yu.jaychou.BuildConfig
import love.yu.jaychou.R
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.components.PreferenceCard
import love.yu.jaychou.ui.components.SettingSwitch
import love.yu.jaychou.ui.designsystem.ArrowPreference
import love.yu.jaychou.ui.designsystem.BasicComponent

@Composable
internal fun UpdatePreferences(settings: AppSettings) {
    val context = LocalContext.current
    val store = remember(context) { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    fun save(action: suspend () -> Unit) {
        scope.launch {
            try { action(); error = null }
            catch (_: IOException) { error = AppText.get(R.string.ui_unable_to_save_settings_please_try_again) }
        }
    }
    PreferenceCard {
        SettingSwitch(AppText.get(R.string.ui_update_auto_check), AppText.get(R.string.ui_update_auto_summary),
            settings.autoCheckUpdates, { save { store.setAutoCheckUpdates(it) } }, true)
        SettingSwitch(AppText.get(R.string.ui_update_prereleases), AppText.get(R.string.ui_update_prereleases_summary),
            settings.includePrereleaseUpdates, { save { store.setIncludePrereleaseUpdates(it) } }, true)
        UpdateCheckRow()
        error?.let { BasicComponent(title = it) }
    }
}

@Composable
internal fun UpdateCheckRow() {
    val controller = LocalUpdateController.current
    ArrowPreference(
        title = AppText.get(R.string.ui_update_check),
        summary = AppText.get(if (controller?.checking == true) R.string.ui_update_checking else R.string.ui_update_current_version,
            BuildConfig.VERSION_NAME),
        onClick = controller?.let { { it.request(manual = true) } },
        modifier = Modifier.testTag("check-updates"),
    )
}

@Composable
internal fun AvailableUpdateRow() {
    val controller = LocalUpdateController.current ?: return
    val update = controller.availableUpdate ?: return
    ArrowPreference(
        title = AppText.get(R.string.ui_update_available, update.versionName),
        summary = AppText.get(R.string.ui_update_version_change, BuildConfig.VERSION_NAME, update.versionName),
        onClick = controller::showAvailableUpdate,
        modifier = Modifier.testTag("available-update"),
    )
}
