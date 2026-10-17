package love.yu.jaychou.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalArchiveBlur = staticCompositionLocalOf { false }

/** Separate sibling capture and header: never sample the blurred bar into itself. */
@Composable
fun ArchiveScaffold(
    title: String,
    behavior: ScrollBehavior? = null,
    small: Boolean = false,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val background = MiuixTheme.colorScheme.background
    val scene: @Composable (Modifier, Modifier) -> Unit = { capture, header ->
        Scaffold(
            modifier = Modifier.fillMaxSize().clipToBounds(),
            containerColor = background, contentWindowInsets = WindowInsets(0),
            topBar = {
                val bar = header.testTag("top-bar-$title")
                if (small) SmallTopAppBar(title, modifier = bar, color = Color.Transparent,
                    navigationIcon = navigationIcon, actions = actions, defaultWindowInsetsPadding = true)
                else TopAppBar(title, modifier = bar, color = Color.Transparent,
                    scrollBehavior = behavior, titlePadding = 24.dp,
                    navigationIcon = navigationIcon, actions = actions, defaultWindowInsetsPadding = true)
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().clipToBounds().then(capture).background(background)) { content(padding) }
        }
    }
    if (Build.VERSION.SDK_INT >= 33 && LocalArchiveBlur.current) HeaderGlassLayer(background, scene)
    else scene(Modifier, Modifier.background(background))
}
