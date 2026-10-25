package love.yu.jaychou.ui.updates

import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import love.yu.jaychou.BuildConfig
import love.yu.jaychou.R
import love.yu.jaychou.data.*

@Composable
internal fun UpdateHost(
    settings: AppSettings,
    repository: UpdateRepository? = null,
    recordAttempt: (suspend (Long) -> Unit)? = null,
    disableAutomatic: (suspend () -> Unit)? = null,
    automaticDelayMillis: Long = 2_500,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val store = remember(context) { SettingsRepository(context) }
    val network = repository ?: remember { UpdateRepository() }
    val scope = rememberCoroutineScope()
    val latestSettings by rememberUpdatedState(settings)
    val record by rememberUpdatedState(recordAttempt ?: { time: Long -> store.recordUpdateCheck(time) })
    val disable by rememberUpdatedState(disableAutomatic ?: { store.setAutoCheckUpdates(false) })
    val controller = remember(network, scope) {
        UpdateController(scope, check = {
            val result = network.check(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME, BuildConfig.DEBUG,
                latestSettings.includePrereleaseUpdates, Build.VERSION.SDK_INT)
            if (result is UpdateResult.Available && result.update.prerelease && !latestSettings.includePrereleaseUpdates)
                UpdateResult.NoCompatibleRelease else result
        }, recordAttempt = { record(it) })
    }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    LaunchedEffect(settings.autoCheckUpdates, settings.includePrereleaseUpdates, lifecycle) {
        controller.applyChannel(settings.includePrereleaseUpdates)
        controller.cancelAutomatic()
        if (lifecycle != Lifecycle.State.RESUMED || !settings.autoCheckUpdates) return@LaunchedEffect
        if (shouldAutomaticallyCheckUpdates(true, latestSettings.lastUpdateCheckAt, System.currentTimeMillis())) {
            delay(automaticDelayMillis)
            if (shouldAutomaticallyCheckUpdates(latestSettings.autoCheckUpdates,
                    latestSettings.lastUpdateCheckAt, System.currentTimeMillis())) controller.request(manual = false)
        }
    }
    CompositionLocalProvider(LocalUpdateController provides controller) {
        content()
        val notice = controller.notice
        if (notice != null) {
            val uri = LocalUriHandler.current
            var saving by remember { mutableStateOf(false) }
            var saveError by remember(notice) { mutableStateOf<String?>(null) }
            UpdateDialog(
                notice = notice, autoCheckEnabled = settings.autoCheckUpdates,
                saving = saving, saveError = saveError,
                onDismiss = controller::dismiss,
                onDownload = {
                    val update = (notice.result as? UpdateResult.Available)?.update
                    if (update != null) {
                        try { uri.openUri(update.downloadUrl); controller.dismiss() }
                        catch (_: RuntimeException) { controller.downloadFailed() }
                    }
                },
                onDisableAutomatic = {
                    if (!saving) scope.launch {
                        saving = true
                        try { disable(); controller.dismiss() }
                        catch (_: IOException) { saveError = AppText.get(R.string.ui_unable_to_save_settings_please_try_again) }
                        finally { saving = false }
                    }
                },
            )
        }
    }
}
