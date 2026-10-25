package love.yu.jaychou

import java.io.IOException
import kotlinx.coroutines.*
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.updates.*
import org.junit.Assert.*
import org.junit.Test

class UpdateControllerTest {
    private val available = UpdateResult.Available(AppUpdate("0.5", 6, "Changes",
        "https://github.com/ShuangYu1145/Jay-Archive/releases/download/0.5/app-release.apk",
        "https://github.com/ShuangYu1145/Jay-Archive/releases/tag/0.5", true))

    @Test fun disablingAutomaticCheckingCancelsPendingResult() = runBlocking {
        val response = CompletableDeferred<UpdateResult>()
        val controller = UpdateController(this, { response.await() }, {})
        controller.request(manual = false)
        yield()
        assertTrue(controller.checking)
        controller.cancelAutomatic()
        response.complete(available)
        yield()
        assertFalse(controller.checking)
        assertNull(controller.notice)
    }

    @Test fun manualCheckStillWorksWhenAutomaticChecksAreCancelled() = runBlocking {
        val response = CompletableDeferred<UpdateResult>()
        val controller = UpdateController(this, { response.await() }, {})
        controller.request(manual = true)
        yield()
        controller.cancelAutomatic()
        response.complete(available)
        yield()
        assertEquals(available, controller.notice?.result)
        assertEquals(true, controller.notice?.manual)
    }

    @Test fun repeatedTapsShareOneRequestAndManualTapPromotesAutomaticCheck() = runBlocking {
        var requests = 0
        val response = CompletableDeferred<UpdateResult>()
        val controller = UpdateController(this, { requests++; response.await() }, {})
        controller.request(manual = false)
        controller.request(manual = true)
        controller.request(manual = true)
        yield()
        response.complete(UpdateResult.UpToDate)
        yield()
        assertEquals(1, requests)
        assertEquals(UpdateResult.UpToDate, controller.notice?.result)
    }

    @Test fun automaticFailuresStayQuietAndManualRateLimitsAreVisible() = runBlocking {
        var attempts = 0
        val controller = UpdateController(this, { throw UpdateRateLimitException() }, { attempts++ })
        controller.request(manual = false)
        yield()
        assertNull(controller.notice)
        controller.request(manual = true)
        yield()
        assertEquals(UpdateProblem.RATE_LIMIT, controller.notice?.problem)
        assertEquals(2, attempts)
    }

    @Test fun cacheWriteFailureDoesNotDisableManualChecking() = runBlocking {
        val controller = UpdateController(this, { UpdateResult.UpToDate }, { throw IOException("Disk") })
        controller.request(manual = true)
        yield()
        assertEquals(UpdateResult.UpToDate, controller.notice?.result)
        assertFalse(controller.checking)
    }

    @Test fun knownUpdateSurvivesDismissingThePromptAndClearsAfterLatestResult() = runBlocking {
        var result: UpdateResult = available
        val controller = UpdateController(this, { result }, {})
        assertNull(controller.availableUpdate)
        controller.request(manual = false)
        yield()
        controller.dismiss()
        assertNull(controller.notice)
        assertEquals(available.update, controller.availableUpdate)
        controller.showAvailableUpdate()
        assertEquals(available, controller.notice?.result)
        controller.applyChannel(includePrereleases = false)
        assertNull(controller.availableUpdate)
        assertNull(controller.notice)
        controller.request(manual = true)
        yield()
        assertEquals(available.update, controller.availableUpdate)
        result = UpdateResult.UpToDate
        controller.request(manual = true)
        yield()
        assertNull(controller.availableUpdate)
    }
}
