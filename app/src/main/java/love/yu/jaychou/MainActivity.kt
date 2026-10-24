package love.yu.jaychou

import love.yu.jaychou.ui.animation.JayLaunchAnimation
import love.yu.jaychou.ui.theme.ArchiveTheme
import love.yu.jaychou.ui.theme.isDark
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
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import love.yu.jaychou.data.*
import love.yu.jaychou.navigation.ArchiveApp

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_JayChou)
        super.onCreate(savedInstanceState)
        AppText.update(resources)
        var launchAnimationReady by mutableStateOf(false)
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            splashScreen.setOnExitAnimationListener { splash ->
                // Show the handwriting as soon as content is drawn, without a second icon transition.
                splash.remove()
                launchAnimationReady = true
            }
        }
        // Warm starts and some OEMs omit the splash exit callback, so also watch real frames.
        var contentReady = false
        val content = findViewById<android.view.View>(android.R.id.content)
        if (android.os.Build.VERSION.SDK_INT >= 29 && content.isHardwareAccelerated) {
            content.viewTreeObserver.registerFrameCommitCallback {
                content.post { launchAnimationReady = true }
            }
        }
        content.viewTreeObserver.addOnDrawListener(object : ViewTreeObserver.OnDrawListener {
            private var scheduled = false
            override fun onDraw() {
                if (scheduled) return
                scheduled = true
                content.post {
                    content.viewTreeObserver.removeOnDrawListener(this)
                    // Also support software rendering and devices that omit frame commits.
                    content.postOnAnimation { launchAnimationReady = true }
                }
            }
        })
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
            val catalog = async(Dispatchers.IO) { AlbumRepository.load(this@MainActivity) }
            val initialSettings = repository.settings.first()
            val data = catalog.await()
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
                        JayLaunchAnimation(playOnLaunch = savedInstanceState == null, ready = launchAnimationReady) {
                            ArchiveApp(
                                catalog = data, settings = current,
                                onTheme = { save { repository.setTheme(it) } },
                                onUiStyle = { save { repository.setUiStyle(it) } },
                                onMonet = { save { repository.setMonet(it) } },
                                onBlur = { save { repository.setBlur(it) } },
                                onLiquidGlass = { save { repository.setLiquidGlass(it) } },
                                onFloatingBottomBar = { save { repository.setFloatingBottomBar(it) } },
                                onPredictive = { save { repository.setPredictiveBack(it) } },
                                onAlbumLayout = { save { repository.setAlbumLayout(it) } },
                                onAlbumCategories = { save { repository.setAlbumCategories(it) } },
                                onCustomColor = { save { repository.setCustomColor(it) } },
                                error = error, onDismissError = { error = null },
                            )
                        }
                    }
                }
            }
            contentReady = true
        }
    }
}
