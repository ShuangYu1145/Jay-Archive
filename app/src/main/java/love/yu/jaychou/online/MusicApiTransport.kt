package love.yu.jaychou.online

import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

fun interface MusicApiTransport {
    suspend fun request(base: String, path: String, params: Map<String, String>, credential: String, provider: MusicProvider): JSONObject
}

class HttpsMusicApiTransport(private val verifyCertificates: () -> Boolean = { true }) : MusicApiTransport {
    override suspend fun request(base: String, path: String, params: Map<String, String>, credential: String, provider: MusicProvider): JSONObject = withContext(Dispatchers.IO) {
        val post = provider == MusicProvider.NETEASE || path == "/login/refresh"
        val query = if (post) "" else params.entries.joinToString("&", prefix = "?") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}" }
        val connection = URI(serviceAddress(base) + path + query).toURL().openConnection() as HttpURLConnection
        try {
            if (connection is javax.net.ssl.HttpsURLConnection) configureMusicApiTls(connection, verifyCertificates())
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 70_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Cache-Control", "no-store")
            if (provider == MusicProvider.QQ && credential.isNotBlank()) {
                val token = credential.split(';').firstOrNull { it.trim().startsWith("qqmusic_session=") }
                    ?.substringAfter('=')?.trim().orEmpty()
                if (token.isBlank()) throw OnlineMusicException(OnlineMusicException.Reason.AUTH)
                connection.setRequestProperty("X-QQ-Session", token)
            }
            if (post) {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                val body = JSONObject(params).put("timestamp", System.currentTimeMillis())
                if (provider == MusicProvider.NETEASE && credential.isNotBlank()) body.put("cookie", credential)
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            if (status == 401 || status == 403) throw OnlineMusicException(OnlineMusicException.Reason.AUTH)
            if (status !in 200..299) throw OnlineMusicException(OnlineMusicException.Reason.SERVICE)
            // A bounded response avoids accepting a login page or an unbounded proxy response.
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > 4 * 1024 * 1024) throw OnlineMusicException(OnlineMusicException.Reason.INVALID_RESPONSE)
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            try { JSONObject(String(bytes, Charsets.UTF_8)) }
            catch (_: org.json.JSONException) { throw OnlineMusicException(OnlineMusicException.Reason.INVALID_RESPONSE) }
        } catch (known: OnlineMusicException) { throw known }
        catch (failure: java.io.IOException) { throw OnlineMusicException(musicConnectionFailure(failure)) }
        finally { connection.disconnect() }
    }
}

/** Certificate and hostname failures need different guidance from a disconnected network. */
internal fun musicConnectionFailure(failure: java.io.IOException): OnlineMusicException.Reason =
    if (generateSequence<Throwable>(failure) { it.cause }.take(12).any { it is javax.net.ssl.SSLException })
        OnlineMusicException.Reason.TLS else OnlineMusicException.Reason.NETWORK
