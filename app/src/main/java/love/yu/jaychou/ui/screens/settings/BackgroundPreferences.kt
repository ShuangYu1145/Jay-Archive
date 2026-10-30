package love.yu.jaychou.ui.screens.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import love.yu.jaychou.R
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.components.PreferenceCard
import love.yu.jaychou.ui.components.SettingSwitch
import love.yu.jaychou.ui.designsystem.ArrowPreference
import love.yu.jaychou.ui.designsystem.PreferenceDivider
import love.yu.jaychou.ui.designsystem.BasicComponent

@Composable
internal fun BackgroundPreferences(settings: AppSettings) {
    val context = LocalContext.current
    val repository = remember(context) { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val currentPath by rememberUpdatedState(settings.customBackgroundPath)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            error = null
            // Finish the file + preference transaction even if selecting an image recreates the page.
            withContext(NonCancellable) {
                var imported: String? = null
                try {
                    imported = BackgroundImageStore.import(context, uri)
                    val previous = currentPath
                    repository.setCustomBackground(imported)
                    imported = null
                    BackgroundImageStore.remove(context, previous)
                } catch (_: Exception) {
                    BackgroundImageStore.remove(context, imported)
                    error = AppText.get(R.string.ui_background_import_failed)
                } finally { busy = false }
            }
        }
    }
    PreferenceCard {
        ArrowPreference(
            title = AppText.get(R.string.ui_custom_background),
            summary = AppText.get(if (busy) R.string.ui_background_importing else R.string.ui_background_choose_summary),
            onClick = { if (!busy) picker.launch(arrayOf("image/*")) },
            modifier = Modifier.testTag("custom-background"),
        )
        PreferenceDivider()
        SettingSwitch(
            AppText.get(R.string.ui_background_card_blur),
            AppText.get(if (Build.VERSION.SDK_INT >= 33) R.string.ui_background_card_blur_summary
                else R.string.ui_background_card_blur_unsupported),
            settings.backgroundCardBlur,
            { value -> scope.launch {
                try { repository.setBackgroundCardBlur(value) }
                catch (_: java.io.IOException) { error = AppText.get(R.string.ui_unable_to_save_settings_please_try_again) }
            } },
            !busy && settings.customBackgroundPath != null && Build.VERSION.SDK_INT >= 33,
        )
        if (settings.customBackgroundPath != null) {
            PreferenceDivider()
            ArrowPreference(
                title = AppText.get(R.string.ui_background_reset),
                onClick = { if (!busy) scope.launch {
                    busy = true
                    withContext(NonCancellable) {
                        try {
                            val previous = currentPath
                            repository.setCustomBackground(null)
                            BackgroundImageStore.remove(context, previous)
                            error = null
                        } catch (_: Exception) {
                            error = AppText.get(R.string.ui_unable_to_save_settings_please_try_again)
                        } finally { busy = false }
                    }
                } },
                modifier = Modifier.testTag("custom-background-reset"),
            )
        }
        error?.let { PreferenceDivider(); BasicComponent(title = it, modifier = Modifier.testTag("custom-background-error")) }
    }
}
