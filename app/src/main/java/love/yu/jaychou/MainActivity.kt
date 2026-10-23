package love.yu.jaychou

import android.content.res.Configuration
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.*
import love.yu.jaychou.navigation.ArchiveApp

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_JayChou)
        super.onCreate(savedInstanceState)
        AppText.update(resources)
        // Keep the system's icon launch window until the first real app frame is ready.
        var contentReady = false
        val content = findViewById<android.view.View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (!contentReady) return false
                content.viewTreeObserver.removeOnPreDrawListener(this)
                return true
            }
        })
        enableEdgeToEdge()
        val systemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !systemDark
            isAppearanceLightNavigationBars = !systemDark
        }
        if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false

        val repository = SettingsRepository(applicationContext)
        lifecycleScope.launch {
            // Prepare first-frame data independently of Compose's frame clock.
            val initialSettings = repository.settings.first()
            val data = withContext(Dispatchers.IO) { AlbumRepository.load(this@MainActivity) }
            setContent {
                val current by remember { repository.settings }.collectAsStateWithLifecycle(initialValue = initialSettings)

                Box(Modifier.fillMaxSize()) {
                    // Do not draw the wrong theme before persisted preferences have loaded.
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
                                    error = AppText.get(R.string.ui_unable_to_save_settings_please_try_again)
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
                            onAlbumCategories = { save { repository.setAlbumCategories(it) } },
                            onCustomColor = { save { repository.setCustomColor(it) } },
                            error = error, onDismissError = { error = null },
                        )
                    }
                }
            }
            contentReady = true
        }
    }
}
