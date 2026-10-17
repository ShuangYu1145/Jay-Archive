package love.yu.jaychou.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String) { SYSTEM("跟随系统"), LIGHT("浅色"), DARK("深色") }

enum class AlbumLayout(val label: String, val description: String) {
    LIST("横向列表", "小封面在左，专辑信息在右"),
    GRID("双列网格", "两列卡片并排，快速浏览全部专辑"),
    LARGE("大封面卡片", "单列大封面，逐张欣赏专辑视觉"),
}

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val monet: Boolean = false,
    val blur: Boolean = true,
    val predictiveBack: Boolean = true,
    val albumLayout: AlbumLayout = AlbumLayout.GRID,
    val customColor: Int? = null,
)

private val Context.settingsDataStore by preferencesDataStore("appearance")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsDataStore
    private val themeKey = stringPreferencesKey("theme")
    private val monetKey = booleanPreferencesKey("monet")
    private val blurKey = booleanPreferencesKey("blur")
    private val predictiveKey = booleanPreferencesKey("predictive_back")
    private val albumLayoutKey = stringPreferencesKey("album_layout")
    private val customColorKey = intPreferencesKey("custom_color")

    val settings = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { prefs ->
        AppSettings(
            theme = ThemeMode.entries.find { it.name == prefs[themeKey] } ?: ThemeMode.SYSTEM,
            monet = prefs[monetKey] ?: false,
            blur = prefs[blurKey] ?: true,
            predictiveBack = prefs[predictiveKey] ?: true,
            albumLayout = AlbumLayout.entries.find { it.name == prefs[albumLayoutKey] } ?: AlbumLayout.GRID,
            customColor = prefs[customColorKey],
        )
    }

    // Update only the changed field, so quick independent changes cannot overwrite each other.
    suspend fun setTheme(value: ThemeMode) { store.edit { it[themeKey] = value.name } }
    suspend fun setMonet(value: Boolean) { store.edit { it[monetKey] = value } }
    suspend fun setBlur(value: Boolean) { store.edit { it[blurKey] = value } }
    suspend fun setPredictiveBack(value: Boolean) { store.edit { it[predictiveKey] = value } }
    suspend fun setAlbumLayout(value: AlbumLayout) { store.edit { it[albumLayoutKey] = value.name } }
    suspend fun setCustomColor(value: Int?) {
        store.edit { if (value == null) it.remove(customColorKey) else it[customColorKey] = value or 0xFF000000.toInt() }
    }
}
