package love.yu.jaychou

import java.io.File
import java.io.IOException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import javax.xml.parsers.DocumentBuilderFactory
import love.yu.jaychou.online.OnlineMusicException
import love.yu.jaychou.online.musicConnectionFailure
import org.junit.Assert.*
import org.junit.Test

class OnlineMusicTlsTest {
    @Test fun disablingChecksIsConnectionScopedAndNewVerifiedConnectionsKeepTheirDefaults() {
        val originalFactory = javax.net.ssl.HttpsURLConnection.getDefaultSSLSocketFactory()
        val originalVerifier = javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier()
        val unverified = java.net.URI("https://fixture.example.com").toURL().openConnection() as javax.net.ssl.HttpsURLConnection
        love.yu.jaychou.online.configureMusicApiTls(unverified, verify = false)
        assertNotSame(originalFactory, unverified.sslSocketFactory)
        assertNotSame(originalVerifier, unverified.hostnameVerifier)
        assertEquals("close", unverified.getRequestProperty("Connection"))
        assertSame(originalFactory, javax.net.ssl.HttpsURLConnection.getDefaultSSLSocketFactory())
        assertSame(originalVerifier, javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier())
        val verified = java.net.URI("https://fixture.example.com").toURL().openConnection() as javax.net.ssl.HttpsURLConnection
        love.yu.jaychou.online.configureMusicApiTls(verified, verify = true)
        assertSame(originalFactory, verified.sslSocketFactory)
        assertSame(originalVerifier, verified.hostnameVerifier)
        unverified.disconnect(); verified.disconnect()
    }

    @Test fun certificateAndHostnameFailuresAreNotPresentedAsOfflineErrors() {
        assertEquals(OnlineMusicException.Reason.TLS, musicConnectionFailure(SSLHandshakeException("Untrusted certificate")))
        assertEquals(OnlineMusicException.Reason.TLS, musicConnectionFailure(SSLPeerUnverifiedException("Hostname mismatch")))
        assertEquals(OnlineMusicException.Reason.TLS, musicConnectionFailure(IOException("wrapped", SSLHandshakeException("Invalid chain"))))
        assertEquals(OnlineMusicException.Reason.NETWORK, musicConnectionFailure(java.net.ConnectException("Connection refused")))
    }

    @Test fun tunnelCertificateIsNotTrustedGloballyOrForSubdomains() {
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/xml/network_security_config.xml")).documentElement
        val base = root.getElementsByTagName("base-config").item(0) as org.w3c.dom.Element
        assertEquals("false", base.getAttribute("cleartextTrafficPermitted"))
        val baseAnchors = base.getElementsByTagName("certificates")
        assertEquals(1, baseAnchors.length)
        assertEquals("system", (baseAnchors.item(0) as org.w3c.dom.Element).getAttribute("src"))
        val domainConfig = root.getElementsByTagName("domain-config").item(0) as org.w3c.dom.Element
        val domain = domainConfig.getElementsByTagName("domain").item(0) as org.w3c.dom.Element
        assertEquals("frp-oil.com", domain.textContent.trim())
        assertEquals("false", domain.getAttribute("includeSubdomains"))
        val certificate = File("src/main/res/raw/sakura_frp_qq_2026.pem").inputStream().use {
            java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(it) as java.security.cert.X509Certificate
        }
        certificate.checkValidity()
        certificate.verify(certificate.publicKey)
        assertTrue(certificate.subjectAlternativeNames.any { it[0] == 2 && it[1] == "frp-oil.com" })
        assertFalse(certificate.subjectAlternativeNames.any { it[0] == 7 })
    }
}
