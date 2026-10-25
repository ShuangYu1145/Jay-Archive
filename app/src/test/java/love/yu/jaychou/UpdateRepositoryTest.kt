package love.yu.jaychou

import java.io.IOException
import kotlinx.coroutines.runBlocking
import love.yu.jaychou.data.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class UpdateRepositoryTest {
    private val root = "https://github.com/ShuangYu1145/Jay-Archive/releases/"
    private fun release(tag: String, beta: Boolean = false, draft: Boolean = false,
                        apk: String = "app-release.apk", metadata: Boolean = false): JSONObject = JSONObject().apply {
        put("tag_name", tag); put("html_url", "${root}tag/$tag")
        put("draft", draft); put("prerelease", beta); put("body", "Changes for $tag")
        put("assets", JSONArray().apply {
            put(JSONObject().put("name", apk).put("browser_download_url", "${root}download/$tag/$apk"))
            if (metadata) put(JSONObject().put("name", "update.json")
                .put("browser_download_url", "${root}download/$tag/update.json"))
        })
    }
    private suspend fun check(vararg releases: JSONObject, beta: Boolean = true, debug: Boolean = false,
                              code: Int = 5, name: String = "0.4", sdk: Int = 37,
                              metadata: String = """{"versionCode":6,"versionName":"0.5","minSdk":26}"""): UpdateResult {
        val list = JSONArray(releases.toList()).toString()
        return UpdateRepository { if (it == UpdateRepository.releasesUrl) list else metadata }
            .check(code, name, debug, beta, sdk)
    }

    @Test fun existingPublishedBetasWorkWithoutMetadata() = runBlocking {
        val fixture = requireNotNull(javaClass.getResource("/github-releases.json")).readText()
        var requests = 0
        val repo = UpdateRepository { requests++; fixture }
        assertEquals(UpdateResult.UpToDate, repo.check(5, "0.4", false, true, 37))
        val update = repo.check(4, "0.3", false, true, 37) as UpdateResult.Available
        assertEquals("0.4", update.update.versionName)
        assertTrue(update.update.downloadUrl.endsWith("/0.4/app-release.apk"))
        assertEquals(2, requests)
    }

    @Test fun prereleasesAndDraftsRespectTheChannel() = runBlocking {
        val beta = release("0.6-beta.1", beta = true)
        val draft = release("9.0", draft = true)
        val stable = release("0.5")
        assertEquals("0.5", (check(beta, draft, stable, beta = false) as UpdateResult.Available).update.versionName)
        assertEquals("0.6-beta.1", (check(beta, draft, stable) as UpdateResult.Available).update.versionName)
        assertEquals(UpdateResult.NoCompatibleRelease, check(beta, beta = false))
    }

    @Test fun comparesNumericVersionsAndSelectsHighestRatherThanFirst() = runBlocking {
        assertEquals("0.10", (check(release("0.9"), release("0.10")) as UpdateResult.Available).update.versionName)
        assertEquals(0, compareUpdateVersions("v0.4", "0.4.0"))
        assertTrue(compareUpdateVersions("1.0-rc.10", "1.0-rc.2")!! > 0)
        assertTrue(compareUpdateVersions("1.0", "1.0-rc.10")!! > 0)
        assertEquals(0, compareUpdateVersions("1.0+build.2", "1.0+build.1"))
        assertNull(compareUpdateVersions("latest", "0.4"))
    }

    @Test fun metadataCodeIsAuthoritativeIncludingSameVersionName() = runBlocking {
        assertTrue(check(release("0.4", metadata = true),
            metadata = """{"versionCode":6,"versionName":"0.4","minSdk":26}""") is UpdateResult.Available)
        assertEquals(UpdateResult.UpToDate, check(release("9.0", metadata = true),
            metadata = """{"versionCode":5,"versionName":"9.0","minSdk":26}"""))
    }

    @Test fun choosesOnlyTheMatchingBuildAndSupportedSdk() = runBlocking {
        assertEquals(UpdateResult.NoCompatibleRelease, check(release("0.5", apk = "app-debug.apk")))
        assertEquals(UpdateResult.NoCompatibleRelease, check(release("0.5"), debug = true))
        val debug = check(release("0.5", apk = "app-debug.apk"), debug = true) as UpdateResult.Available
        assertTrue(debug.update.downloadUrl.endsWith("app-debug.apk"))
        assertEquals(UpdateResult.NoCompatibleRelease, check(release("0.5", metadata = true), sdk = 26,
            metadata = """{"versionCode":6,"versionName":"0.5","minSdk":33}"""))
    }

    @Test fun emptyOrUnparseableReleasesAreNotReportedAsUpToDate() = runBlocking {
        assertEquals(UpdateResult.NoCompatibleRelease, check())
        assertEquals(UpdateResult.NoCompatibleRelease, check(release("latest")))
    }

    @Test fun rejectsExternalDownloadAddresses() = runBlocking {
        val item = release("0.5")
        item.getJSONArray("assets").getJSONObject(0).put("browser_download_url", "https://example.com/app-release.apk")
        assertEquals(UpdateResult.NoCompatibleRelease, check(item))
    }

    @Test fun malformedResponseAndMetadataFailInsteadOfPretendingLatest() = runBlocking {
        for (response in listOf("not json", "{}")) {
            try {
                UpdateRepository { response }.check(5, "0.4", false, true, 37)
                fail("Expected malformed response to fail")
            } catch (_: IOException) { }
        }
        try {
            check(release("0.5", metadata = true), metadata = """{"versionCode":-1}""")
            fail("Expected invalid metadata to fail")
        } catch (_: IOException) { }
    }

    @Test fun automaticallyChecksAtMostDailyAndAllowsClockRecovery() {
        val day = 24L * 60 * 60 * 1000
        assertFalse(shouldAutomaticallyCheckUpdates(false, 0, day))
        assertTrue(shouldAutomaticallyCheckUpdates(true, 0, day))
        assertFalse(shouldAutomaticallyCheckUpdates(true, day, day * 2 - 1))
        assertTrue(shouldAutomaticallyCheckUpdates(true, day, day * 2))
        assertTrue(shouldAutomaticallyCheckUpdates(true, day, day - 1))
    }
}
