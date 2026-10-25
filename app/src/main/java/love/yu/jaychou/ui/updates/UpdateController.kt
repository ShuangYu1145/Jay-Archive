package love.yu.jaychou.ui.updates

import androidx.compose.runtime.*
import java.io.IOException
import kotlinx.coroutines.*
import love.yu.jaychou.data.UpdateRateLimitException
import love.yu.jaychou.data.UpdateResult
import love.yu.jaychou.data.AppUpdate

internal enum class UpdateProblem { NETWORK, RATE_LIMIT, OPEN_DOWNLOAD }
internal data class UpdateNotice(val result: UpdateResult? = null, val problem: UpdateProblem? = null, val manual: Boolean)

@Stable
internal class UpdateController(
    private val scope: CoroutineScope,
    private val check: suspend () -> UpdateResult,
    private val recordAttempt: suspend (Long) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    var checking by mutableStateOf(false)
        private set
    var notice by mutableStateOf<UpdateNotice?>(null)
        private set
    var availableUpdate by mutableStateOf<AppUpdate?>(null)
        private set
    private var job: Job? = null
    private var manualRequest = false

    fun request(manual: Boolean) {
        if (checking) {
            if (manual) manualRequest = true
            return
        }
        checking = true
        manualRequest = manual
        job = scope.launch {
            try {
                // An attempted automatic check is throttled even when the network is unavailable.
                try { recordAttempt(clock()) } catch (_: IOException) { /* Cache failure doesn't block a manual check. */ }
                val result = check()
                ensureActive()
                availableUpdate = (result as? UpdateResult.Available)?.update
                if (manualRequest || result is UpdateResult.Available)
                    notice = UpdateNotice(result = result, manual = manualRequest)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IOException) {
                if (manualRequest) notice = UpdateNotice(
                    problem = if (error is UpdateRateLimitException) UpdateProblem.RATE_LIMIT else UpdateProblem.NETWORK,
                    manual = true,
                )
            } finally { checking = false }
        }
    }

    fun cancelAutomatic() {
        if (!manualRequest) job?.cancel()
        if (notice?.manual == false) notice = null
    }

    fun dismiss() { notice = null }
    fun showAvailableUpdate() {
        availableUpdate?.let { notice = UpdateNotice(result = UpdateResult.Available(it), manual = true) }
    }
    fun applyChannel(includePrereleases: Boolean) {
        if (!includePrereleases && availableUpdate?.prerelease == true) {
            availableUpdate = null
            if ((notice?.result as? UpdateResult.Available)?.update?.prerelease == true) notice = null
        }
    }
    fun downloadFailed() { notice = UpdateNotice(problem = UpdateProblem.OPEN_DOWNLOAD, manual = true) }
}

internal val LocalUpdateController = staticCompositionLocalOf<UpdateController?> { null }
