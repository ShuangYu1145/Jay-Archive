package love.yu.jaychou.online

import android.annotation.SuppressLint
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/** Explicit user opt-out for configured music API connections only; defaults remain untouched. */
@SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
private object UnverifiedMusicApiTls {
    val socketFactory by lazy {
        val trust = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) { }
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) { }
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), SecureRandom()) }.socketFactory
    }
    val hostnameVerifier = HostnameVerifier { _, _ -> true }
}

internal fun configureMusicApiTls(connection: HttpsURLConnection, verify: Boolean) {
    if (verify) return
    connection.sslSocketFactory = UnverifiedMusicApiTls.socketFactory
    connection.hostnameVerifier = UnverifiedMusicApiTls.hostnameVerifier
    // Do not retain an unverified keep-alive connection after the user restores verification.
    connection.setRequestProperty("Connection", "close")
}
