package love.yu.jaychou.online

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

interface MusicAccountStore {
    fun endpoint(provider: MusicProvider): String
    fun setEndpoint(provider: MusicProvider, endpoint: String)
    fun credential(provider: MusicProvider): String
    fun setCredential(provider: MusicProvider, credential: String)
    fun certificateVerificationEnabled(): Boolean = true
    fun setCertificateVerificationEnabled(enabled: Boolean) { }
    fun pendingLogin(provider: MusicProvider): PendingMusicLogin? = null
    fun setPendingLogin(provider: MusicProvider, login: PendingMusicLogin?) { }
    fun selectedProvider(): MusicProvider = MusicProvider.QQ
    fun setSelectedProvider(provider: MusicProvider) { }
}

/** AES-GCM key stays in Android Keystore; this preference file is excluded from backup. */
class DeviceMusicAccountStore(context: Context) : MusicAccountStore {
    private val preferences = context.getSharedPreferences("online_music_accounts", Context.MODE_PRIVATE)
    private val alias = "jaychou_online_music_v1"
    override fun selectedProvider() = runCatching {
        MusicProvider.valueOf(preferences.getString("selected_provider", MusicProvider.QQ.name).orEmpty())
    }.getOrDefault(MusicProvider.QQ)
    override fun setSelectedProvider(provider: MusicProvider) { preferences.edit().putString("selected_provider", provider.name).apply() }
    override fun certificateVerificationEnabled() = preferences.getBoolean("verify_service_certificates", true)
    override fun setCertificateVerificationEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("verify_service_certificates", enabled).apply()
    }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    override fun endpoint(provider: MusicProvider) = preferences.getString("${provider.name}_endpoint", "").orEmpty()
    override fun setEndpoint(provider: MusicProvider, endpoint: String) {
        val normalized = serviceAddress(endpoint)
        if (normalized != endpoint(provider)) {
            preferences.edit().putString("${provider.name}_endpoint", normalized).remove("${provider.name}_session")
                .remove("${provider.name}_pending_login").apply()
        }
    }
    private fun readEncrypted(provider: MusicProvider, slot: String): String {
        val stored = preferences.getString("${provider.name}_$slot", null) ?: return ""
        return try {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            cipher.updateAAD((if (slot == "session") provider.name else "${provider.name}:$slot").toByteArray())
            String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
        } catch (_: Exception) {
            preferences.edit().remove("${provider.name}_$slot").apply()
            ""
        }
    }
    private fun writeEncrypted(provider: MusicProvider, slot: String, value: String) {
        if (value.isBlank()) { preferences.edit().remove("${provider.name}_$slot").apply(); return }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD((if (slot == "session") provider.name else "${provider.name}:$slot").toByteArray())
        preferences.edit().putString("${provider.name}_$slot",
            Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)).apply()
    }
    override fun credential(provider: MusicProvider) = readEncrypted(provider, "session")
    override fun setCredential(provider: MusicProvider, credential: String) = writeEncrypted(provider, "session", credential)
    override fun pendingLogin(provider: MusicProvider): PendingMusicLogin? {
        val stored = readEncrypted(provider, "pending_login")
        if (stored.isBlank()) return null
        return try {
            val item = JSONObject(stored)
            PendingMusicLogin(LoginQr(item.getString("key"), item.getString("image")), item.getString("channel"),
                item.getString("endpoint"), item.optInt("code", 801), item.optString("credential"))
        } catch (_: Exception) { setPendingLogin(provider, null); null }
    }
    override fun setPendingLogin(provider: MusicProvider, login: PendingMusicLogin?) {
        val stored = login?.let {
            JSONObject().put("key", it.qr.key).put("image", it.qr.image).put("channel", it.channel)
                .put("endpoint", it.endpoint).put("code", it.code).put("credential", it.credential).toString()
        }.orEmpty()
        writeEncrypted(provider, "pending_login", stored)
    }
}
