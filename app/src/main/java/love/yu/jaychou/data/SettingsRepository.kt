package love.yu.jaychou.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

enum class ThemeMode(private val sourceLabel: String) {
    SYSTEM("跟随系统"), LIGHT("浅色"), DARK("深色");
    val label: String get() = AppText.fromOriginal(sourceLabel)
}

enum class UiStyle(val label: String) {
    MIUIX("Miuix"), MATERIAL3("Material Design 3");

    companion object {
        fun fromStored(value: String?): UiStyle = entries.find { it.name == value } ?: MIUIX
    }
}

/** Existing Miuix keys stay compatible; Material owns a separate appearance profile. */
internal fun appearanceKey(style: UiStyle, name: String): String =
    if (style == UiStyle.MIUIX) name else "material3_$name"

enum class AlbumLayout(private val sourceLabel: String, private val sourceDescription: String) {
    LIST("横向列表", "小封面在左，专辑信息在右"),
    GRID("双列网格", "两列卡片并排，快速浏览全部专辑"),
    LARGE("大封面卡片", "单列大封面，逐张欣赏专辑视觉");
    val label: String get() = AppText.fromOriginal(sourceLabel)
    val description: String get() = AppText.fromOriginal(sourceDescription)
}

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val monet: Boolean = false,
    val blur: Boolean = true,
    val predictiveBack: Boolean = true,
    val albumLayout: AlbumLayout = AlbumLayout.GRID,
    val customColor: Int? = null,
    val albumCategories: List<String> = listOf(ReleaseCategory.STUDIO.name),
    val userName: String = "",
    val userAvatarPath: String? = null,
    val liquidGlass: Boolean = true,
    val floatingBottomBar: Boolean = true,
    val uiStyle: UiStyle = UiStyle.MIUIX,
    val customBackgroundPath: String? = null,
    val backgroundCardBlur: Boolean = false,
    val autoCheckUpdates: Boolean = true,
    val includePrereleaseUpdates: Boolean = true,
    val lastUpdateCheckAt: Long = 0L,
)

private val Context.settingsDataStore by preferencesDataStore("appearance")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsDataStore
    private val blurKey = booleanPreferencesKey("blur")
    private val liquidGlassKey = booleanPreferencesKey("liquid_glass")
    private val floatingBottomBarKey = booleanPreferencesKey("floating_bottom_bar")
    private val predictiveKey = booleanPreferencesKey("predictive_back")
    private val albumCategoriesKey = stringSetPreferencesKey("album_categories")
    private val userNameKey = stringPreferencesKey("user_name")
    private val userAvatarKey = stringPreferencesKey("user_avatar")
    private val uiStyleKey = stringPreferencesKey("ui_style")
    private val customBackgroundKey = stringPreferencesKey("custom_background")
    private val backgroundCardBlurKey = booleanPreferencesKey("background_card_blur")
    private val autoCheckUpdatesKey = booleanPreferencesKey("auto_check_updates")
    private val prereleaseUpdatesKey = booleanPreferencesKey("prerelease_updates")
    private val lastUpdateCheckKey = longPreferencesKey("last_update_check")

    val lyricsReadingSettings = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map(LyricsReadingKeys::decode)

    suspend fun setLyricsReadingSettings(value: LyricsReadingSettings) {
        val normalized = value.normalized()
        store.edit {
            it[LyricsReadingKeys.font] = normalized.fontSize
            it[LyricsReadingKeys.spacing] = normalized.lineSpacing
            it[LyricsReadingKeys.alignment] = normalized.alignment.name
            it[LyricsReadingKeys.background] = normalized.background.name
            it[LyricsReadingKeys.immersive] = normalized.immersive
            it[LyricsReadingKeys.translation] = normalized.showTranslation
        }
    }

    val settings = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { prefs ->
        val style = UiStyle.fromStored(prefs[uiStyleKey])
        AppSettings(
            uiStyle = style,
            theme = ThemeMode.entries.find { it.name == prefs[stringPreferencesKey(appearanceKey(style, "theme"))] } ?: ThemeMode.SYSTEM,
            monet = prefs[booleanPreferencesKey(appearanceKey(style, "monet"))] ?: (style == UiStyle.MATERIAL3),
            blur = prefs[blurKey] ?: true,
            liquidGlass = prefs[liquidGlassKey] ?: true,
            floatingBottomBar = prefs[floatingBottomBarKey] ?: true,
            predictiveBack = prefs[predictiveKey] ?: true,
            albumLayout = AlbumLayout.entries.find { it.name == prefs[stringPreferencesKey(appearanceKey(style, "album_layout"))] } ?: AlbumLayout.GRID,
            customColor = prefs[intPreferencesKey(appearanceKey(style, "custom_color"))],
            albumCategories = normalizeAlbumCategories(prefs[albumCategoriesKey].orEmpty()),
            userName = prefs[userNameKey].orEmpty(),
            userAvatarPath = prefs[userAvatarKey],
            customBackgroundPath = prefs[customBackgroundKey],
            backgroundCardBlur = prefs[backgroundCardBlurKey] ?: false,
            autoCheckUpdates = prefs[autoCheckUpdatesKey] ?: true,
            includePrereleaseUpdates = prefs[prereleaseUpdatesKey] ?: true,
            lastUpdateCheckAt = prefs[lastUpdateCheckKey] ?: 0L,
        )
    }

    // Update only the changed field, so quick independent changes cannot overwrite each other.
    suspend fun setUiStyle(value: UiStyle) { store.edit { it[uiStyleKey] = value.name } }
    suspend fun setTheme(value: ThemeMode) {
        store.edit { it[stringPreferencesKey(appearanceKey(UiStyle.fromStored(it[uiStyleKey]), "theme"))] = value.name }
    }
    suspend fun setMonet(value: Boolean) {
        store.edit { it[booleanPreferencesKey(appearanceKey(UiStyle.fromStored(it[uiStyleKey]), "monet"))] = value }
    }
    suspend fun setBlur(value: Boolean) { store.edit { it[blurKey] = value } }
    suspend fun setAutoCheckUpdates(value: Boolean) { store.edit { it[autoCheckUpdatesKey] = value } }
    suspend fun setIncludePrereleaseUpdates(value: Boolean) {
        store.edit { it[prereleaseUpdatesKey] = value; it.remove(lastUpdateCheckKey) }
    }
    suspend fun recordUpdateCheck(timeMillis: Long) { store.edit { it[lastUpdateCheckKey] = timeMillis } }
    suspend fun setBackgroundCardBlur(value: Boolean) { store.edit { it[backgroundCardBlurKey] = value } }
    suspend fun setCustomBackground(path: String?) {
        store.edit {
            if (path == null) it.remove(customBackgroundKey) else it[customBackgroundKey] = path
        }
    }
    suspend fun setLiquidGlass(value: Boolean) { store.edit { it[liquidGlassKey] = value } }
    suspend fun setFloatingBottomBar(value: Boolean) { store.edit { it[floatingBottomBarKey] = value } }
    suspend fun setPredictiveBack(value: Boolean) { store.edit { it[predictiveKey] = value } }
    suspend fun setAlbumLayout(value: AlbumLayout) {
        store.edit { it[stringPreferencesKey(appearanceKey(UiStyle.fromStored(it[uiStyleKey]), "album_layout"))] = value.name }
    }
    suspend fun setAlbumCategories(value: List<String>) {
        store.edit { it[albumCategoriesKey] = normalizeAlbumCategories(value).toSet() }
    }
    suspend fun setCustomColor(value: Int?) {
        store.edit {
            val key = intPreferencesKey(appearanceKey(UiStyle.fromStored(it[uiStyleKey]), "custom_color"))
            if (value == null) it.remove(key) else it[key] = value or 0xFF000000.toInt()
        }
    }
    suspend fun setProfile(name: String, avatarPath: String?) {
        store.edit {
            it[userNameKey] = name.trim().take(30)
            if (avatarPath == null) it.remove(userAvatarKey) else it[userAvatarKey] = avatarPath
        }
    }
}

internal fun normalizeAlbumCategories(value: Collection<String>): List<String> =
    ReleaseCategory.entries.map { it.name }.filter { it in value }
        .ifEmpty { listOf(ReleaseCategory.STUDIO.name) }
