package love.yu.jaychou.data

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

enum class AppLanguage(val tag: String, val nativeName: String) {
    SYSTEM("", ""),
    SIMPLIFIED("zh-Hans", "简体中文"),
    TRADITIONAL("zh-Hant", "繁體中文"),
    ENGLISH("en", "English"),
    JAPANESE("ja", "日本語"),
    KOREAN("ko", "한국어");

    fun select() = AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))

    companion object {
        fun current(): AppLanguage {
            val locale = AppCompatDelegate.getApplicationLocales()[0] ?: return SYSTEM
            return entries.firstOrNull { it.tag == contentTag(locale) } ?: SYSTEM
        }

        fun contentTag(locale: Locale): String = when (locale.language) {
            "zh" -> when (locale.script) {
                "Hant" -> "zh-Hant"
                "Hans" -> "zh-Hans"
                else -> if (locale.country in listOf("TW", "HK", "MO")) "zh-Hant" else "zh-Hans"
            }
            "en", "ja", "ko" -> locale.language
            else -> "zh-Hans"
        }

        fun contentTag(locales: android.os.LocaleList): String =
            (0 until locales.size()).map { locales[it] }
                .firstOrNull { it.language in listOf("zh", "en", "ja", "ko") }
                ?.let(::contentTag) ?: "zh-Hans"
    }
}

/** Resource access for event callbacks and non-Compose formatting as well as the UI.
 * Refreshed from the current Activity configuration; no Activity reference is retained.
 */
object AppText {
    @Volatile private var currentResources: Resources? = null
    fun update(resources: Resources) { currentResources = resources }
    val locale: Locale get() = currentResources?.configuration?.locales
        ?.let { Locale.forLanguageTag(AppLanguage.contentTag(it)) } ?: Locale.SIMPLIFIED_CHINESE
    fun get(@StringRes id: Int, vararg args: Any): String {
        val resources = requireNotNull(currentResources) { "AppText must be initialized with the current configuration" }
        val quantity = quantityTextKeys[id]
        return if (quantity != null) {
            val count = (args[quantity.second] as Number).toInt()
            resources.getQuantityString(quantity.first, count, *args)
        } else resources.getString(id, *args)
    }

    fun quantity(@androidx.annotation.PluralsRes id: Int, count: Int): String =
        requireNotNull(currentResources).getQuantityString(id, count, count)

    /** Raw values from source APIs stay usable without an Android context in catalogue tests. */
    fun fromOriginal(value: String, vararg args: Any): String {
        val key = originalTextKeys[value]
        val resources = currentResources
        return if (key != null && resources != null) get(key, *args)
        else if (args.isNotEmpty()) String.format(locale, value, *args) else value
    }
}
