package love.yu.jaychou

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.*
import love.yu.jaychou.navigation.ArchiveApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = SettingsRepository(applicationContext)
        setContent {
            val settings by remember { repository.settings }.collectAsStateWithLifecycle(initialValue = null)
            val catalog by produceState<AlbumCatalog?>(initialValue = null) {
                value = withContext(Dispatchers.IO) { AlbumRepository.load(applicationContext) }
            }
            val current = settings
            val data = catalog
            // Do not draw the wrong theme before persisted preferences have loaded.
            if (current != null && data != null) {
                ArchiveTheme(current) {
                    val dark = current.isDark()
                    val view = LocalView.current
                    SideEffect {
                        WindowCompat.getInsetsController(window, view).apply {
                            isAppearanceLightStatusBars = !dark
                            isAppearanceLightNavigationBars = !dark
                        }
                        if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
                    }
                    val scope = rememberCoroutineScope()
                    var error by remember { mutableStateOf<String?>(null) }
                    fun save(action: suspend () -> Unit) {
                        scope.launch {
                            try { action() } catch (_: java.io.IOException) {
                                error = "设置保存失败，请重试。"
                            }
                        }
                    }
                    ArchiveApp(
                        catalog = data, settings = current,
                        onTheme = { save { repository.setTheme(it) } },
                        onMonet = { save { repository.setMonet(it) } },
                        onBlur = { save { repository.setBlur(it) } },
                        onPredictive = { save { repository.setPredictiveBack(it) } },
                        onAlbumLayout = { save { repository.setAlbumLayout(it) } },
                        onCustomColor = { save { repository.setCustomColor(it) } },
                        error = error, onDismissError = { error = null },
                    )
                }
            }
        }
    }
}
