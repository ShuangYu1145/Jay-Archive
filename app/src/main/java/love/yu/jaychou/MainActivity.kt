package love.yu.jaychou

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.*
import love.yu.jaychou.navigation.ArchiveApp

class MainActivity : ComponentActivity() {
    companion object {
        private var splashShownInProcess = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val systemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !systemDark
            isAppearanceLightNavigationBars = !systemDark
        }
        if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false

        val repository = SettingsRepository(applicationContext)
        setContent {
            val settings by remember { repository.settings }.collectAsStateWithLifecycle(initialValue = null)
            val catalog by produceState<AlbumCatalog?>(initialValue = null) {
                value = withContext(Dispatchers.IO) { AlbumRepository.load(applicationContext) }
            }
            var minimumSplashElapsed by remember { mutableStateOf(splashShownInProcess) }
            LaunchedEffect(Unit) {
                if (!splashShownInProcess) {
                    delay(700)
                    splashShownInProcess = true
                    minimumSplashElapsed = true
                }
            }

            val current = settings
            val data = catalog
            val ready = current != null && data != null
            val showSplash = !ready || !minimumSplashElapsed

            Box(Modifier.fillMaxSize()) {
                // Do not draw the wrong theme before persisted preferences have loaded.
                if (ready) {
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
                AnimatedVisibility(
                    visible = showSplash,
                    enter = EnterTransition.None,
                    exit = fadeOut(tween(200)),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    JaySplashScreen()
                }
            }
        }
    }
}
