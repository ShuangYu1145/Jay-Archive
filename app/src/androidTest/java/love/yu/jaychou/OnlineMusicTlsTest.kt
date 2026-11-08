package love.yu.jaychou

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import love.yu.jaychou.online.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in probe: no credentials or user confirmation; creates and cancels an anonymous QR. */
class OnlineMusicTlsTest {
    @Test fun certificateVerificationDefaultsOnAndSurvivesStoreRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = "tls-test-" + java.util.UUID.randomUUID()
        val isolated = object : android.content.ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int) = super.getSharedPreferences(name + suffix, mode)
        }
        try {
            val store = DeviceMusicAccountStore(isolated)
            assertTrue(store.certificateVerificationEnabled())
            store.setCertificateVerificationEnabled(false)
            assertFalse(DeviceMusicAccountStore(isolated).certificateVerificationEnabled())
            store.setCertificateVerificationEnabled(true)
            assertTrue(DeviceMusicAccountStore(isolated).certificateVerificationEnabled())
        } finally { context.deleteSharedPreferences("online_music_accounts" + suffix) }
    }

    @Test fun tunnelCertificateWorksForItsDomainAndDoesNotDisableIpVerification() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue(arguments.getString("musicTlsProbe") == "true")
        val base = requireNotNull(arguments.getString("musicTlsProbeBase"))
        val ipBase = requireNotNull(arguments.getString("musicTlsProbeIpBase"))
        var targetBase = base
        var verify = true
        val accounts = object : MusicAccountStore {
            override fun endpoint(provider: MusicProvider) = targetBase
            override fun setEndpoint(provider: MusicProvider, endpoint: String) { }
            override fun credential(provider: MusicProvider) = ""
            override fun setCredential(provider: MusicProvider, credential: String) { error("Probe must not save any sign-in") }
            override fun certificateVerificationEnabled() = verify
            override fun setCertificateVerificationEnabled(enabled: Boolean) { verify = enabled }
        }
        val originalFactory = javax.net.ssl.HttpsURLConnection.getDefaultSSLSocketFactory()
        val originalVerifier = javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier()
        val transport = HttpsMusicApiTransport(accounts::certificateVerificationEnabled)
        val channels = transport.request(base, "/login/channels", emptyMap(), "", MusicProvider.QQ)
        assertTrue(channels.getJSONObject("data").getJSONArray("channels").length() > 0)
        val repository = OnlineMusicRepository(accounts)
        val qr = repository.startLogin(MusicProvider.QQ, "qq")
        try {
            assertTrue(qr.key.isNotBlank())
            assertTrue(qr.image.startsWith("data:image/"))
            val bytes = Base64.decode(qr.image.substringAfter(','), Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            assertNotNull("The actual QR payload must decode on Android", bitmap)
            assertTrue(bitmap.width > 20 && bitmap.height > 20)
            bitmap.recycle()
        } finally { repository.cancelLogin(MusicProvider.QQ, qr.key) }
        try {
            transport.request(ipBase, "/login/channels", emptyMap(), "", MusicProvider.QQ)
            fail("A DNS-only certificate must not authenticate the IP address")
        } catch (failure: OnlineMusicException) {
            assertEquals(OnlineMusicException.Reason.TLS, failure.reason)
        }
        accounts.setCertificateVerificationEnabled(false)
        val unverified = transport.request(ipBase, "/login/channels", emptyMap(), "", MusicProvider.QQ)
        assertTrue(unverified.getJSONObject("data").getJSONArray("channels").length() > 0)
        targetBase = ipBase
        // The repository's default transport must read the same saved setting as the UI.
        repository.cancelLogin(MusicProvider.QQ, "tls-probe-no-active-qr")
        assertSame(originalFactory, javax.net.ssl.HttpsURLConnection.getDefaultSSLSocketFactory())
        assertSame(originalVerifier, javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier())
        accounts.setCertificateVerificationEnabled(true)
        try {
            transport.request(ipBase, "/login/channels", emptyMap(), "", MusicProvider.QQ)
            fail("Turning verification back on must reject the IP again, using this same transport")
        } catch (failure: OnlineMusicException) { assertEquals(OnlineMusicException.Reason.TLS, failure.reason) }
    }
}
