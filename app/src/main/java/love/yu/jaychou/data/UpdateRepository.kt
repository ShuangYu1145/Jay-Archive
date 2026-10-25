package love.yu.jaychou.data

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class AppUpdate(
    val versionName: String,
    val versionCode: Int?,
    val notes: String,
    val downloadUrl: String,
    val releaseUrl: String,
    val prerelease: Boolean,
)

sealed interface UpdateResult {
    data class Available(val update: AppUpdate) : UpdateResult
    data object UpToDate : UpdateResult
    data object NoCompatibleRelease : UpdateResult
}

class UpdateRateLimitException : IOException("GitHub API rate limit")

/** Public release metadata only. APK downloads are handed to the user's browser. */
class UpdateRepository(private val fetch: suspend (String) -> String = ::fetchUpdateJson) {
    companion object {
        const val releasesUrl = "https://api.github.com/repos/ShuangYu1145/Jay-Archive/releases?per_page=20"
        private const val downloadPrefix = "https://github.com/ShuangYu1145/Jay-Archive/releases/download/"
        private const val releasePrefix = "https://github.com/ShuangYu1145/Jay-Archive/releases/tag/"
    }

    suspend fun check(
        installedCode: Int, installedName: String, debug: Boolean,
        includePrereleases: Boolean, sdk: Int,
    ): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val releases = JSONArray(fetch(releasesUrl))
            val updates = mutableListOf<AppUpdate>()
            var compatible = false
            for (index in 0 until releases.length()) {
                currentCoroutineContext().ensureActive()
                val release = releases.getJSONObject(index)
                if (release.optBoolean("draft") || (!includePrereleases && release.optBoolean("prerelease"))) continue
                val releaseUrl = release.optString("html_url")
                if (!releaseUrl.startsWith(releasePrefix)) continue
                val assets = release.optJSONArray("assets") ?: continue
                val apkName = if (debug) "app-debug.apk" else "app-release.apk"
                val apk = findAsset(assets, apkName) ?: continue
                val apkUrl = apk.optString("browser_download_url")
                if (!apkUrl.startsWith(downloadPrefix)) continue
                val metadataAsset = findAsset(assets, "update.json")
                val metadata = metadataAsset?.let {
                    val url = it.optString("browser_download_url")
                    if (!url.startsWith(downloadPrefix)) throw IOException("Invalid metadata URL")
                    JSONObject(fetch(url))
                }
                val code = metadata?.getLong("versionCode")?.also {
                    if (it !in 1..Int.MAX_VALUE.toLong()) throw IOException("Invalid version code")
                }?.toInt()
                val name = metadata?.optString("versionName")?.takeIf { it.isNotBlank() }
                    ?: release.optString("tag_name").removePrefix("v").removePrefix("V")
                val minSdk = metadata?.optInt("minSdk", 26) ?: 26
                if (minSdk <= 0) throw IOException("Invalid minSdk")
                if (sdk < minSdk) continue
                val comparison = compareUpdateVersions(name, installedName)
                if (code == null && comparison == null) continue
                compatible = true
                val newer = if (code != null) code > installedCode else comparison!! > 0
                if (!newer) continue
                updates += AppUpdate(
                    name, code,
                    release.optString("body").takeUnless { it == "null" }.orEmpty().take(20_000),
                    apkUrl, releaseUrl, release.optBoolean("prerelease"),
                )
            }
            currentCoroutineContext().ensureActive()
            val latest = updates.maxWithOrNull { first, second ->
                if (first.versionCode != null && second.versionCode != null)
                    first.versionCode.compareTo(second.versionCode)
                else compareUpdateVersions(first.versionName, second.versionName) ?: 0
            }
            when {
                latest != null -> UpdateResult.Available(latest)
                compatible -> UpdateResult.UpToDate
                else -> UpdateResult.NoCompatibleRelease
            }
        } catch (error: JSONException) {
            throw IOException("Invalid update response", error)
        }
    }

    private fun findAsset(assets: JSONArray, name: String): JSONObject? =
        (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull {
            it.optString("name") == name && it.optString("state", "uploaded") == "uploaded"
        }
}

/** Numeric segments, zero padding, SemVer prerelease order; never compare version strings lexically. */
internal fun compareUpdateVersions(first: String, second: String): Int? {
    val pattern = Regex("^[vV]?(\\d+(?:\\.\\d+)*)(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?$")
    fun parse(value: String): Pair<List<Long>, List<String>>? {
        val match = pattern.matchEntire(value.trim()) ?: return null
        val numbers = match.groupValues[1].split('.').map { it.toLongOrNull() ?: return null }
        val suffix = match.groupValues[2].takeIf { it.isNotEmpty() }?.split('.').orEmpty()
        return numbers to suffix
    }
    val a = parse(first) ?: return null
    val b = parse(second) ?: return null
    for (index in 0 until maxOf(a.first.size, b.first.size)) {
        val comparison = a.first.getOrElse(index) { 0L }.compareTo(b.first.getOrElse(index) { 0L })
        if (comparison != 0) return comparison
    }
    if (a.second.isEmpty() || b.second.isEmpty()) return when {
        a.second.isEmpty() && b.second.isEmpty() -> 0
        a.second.isEmpty() -> 1
        else -> -1
    }
    for (index in 0 until minOf(a.second.size, b.second.size)) {
        val x = a.second[index]
        val y = b.second[index]
        val xn = x.toLongOrNull()
        val yn = y.toLongOrNull()
        val comparison = when {
            xn != null && yn != null -> xn.compareTo(yn)
            xn != null -> -1
            yn != null -> 1
            else -> x.compareTo(y)
        }
        if (comparison != 0) return comparison
    }
    return a.second.size.compareTo(b.second.size)
}

internal fun shouldAutomaticallyCheckUpdates(enabled: Boolean, lastCheck: Long, now: Long): Boolean =
    enabled && (lastCheck <= 0L || now < lastCheck || now - lastCheck >= 24L * 60 * 60 * 1000)

private suspend fun fetchUpdateJson(address: String): String = withContext(Dispatchers.IO) {
    currentCoroutineContext().ensureActive()
    val uri = try { URI(address) } catch (error: java.net.URISyntaxException) { throw IOException("Invalid URL", error) }
    if (uri.scheme != "https") throw IOException("HTTPS required")
    val connection = uri.toURL().openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "Jay-Archive-Update-Checker")
        if (uri.host == "api.github.com") connection.setRequestProperty("X-GitHub-Api-Version", "2026-03-10")
        val status = connection.responseCode
        if (status == 429 || (status == 403 && connection.getHeaderField("X-RateLimit-Remaining") == "0"))
            throw UpdateRateLimitException()
        if (status !in 200..299) throw IOException("GitHub HTTP $status")
        val output = ByteArrayOutputStream()
        connection.inputStream.use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = input.read(buffer)
                if (read < 0) break
                if (output.size() + read > 2 * 1024 * 1024) throw IOException("Update response too large")
                output.write(buffer, 0, read)
            }
        }
        currentCoroutineContext().ensureActive()
        output.toString(Charsets.UTF_8.name())
    } finally { connection.disconnect() }
}
