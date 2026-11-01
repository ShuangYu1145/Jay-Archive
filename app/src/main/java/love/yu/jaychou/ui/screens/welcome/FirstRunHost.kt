package love.yu.jaychou.ui.screens.welcome

import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import kotlin.math.hypot
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import love.yu.jaychou.R
import love.yu.jaychou.data.*

/** Do not compose network-backed pages or the update checker until setup has been saved. */
@Composable
internal fun FirstRunHost(
    settings: AppSettings,
    onComplete: suspend (UiStyle, ThemeMode) -> Unit,
    content: @Composable () -> Unit,
) {
    var welcomeVisible by remember { mutableStateOf(!settings.onboardingCompleted) }
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var checkBounds by remember { mutableStateOf<Rect?>(null) }
    var hostOrigin by remember { mutableStateOf(Offset.Zero) }
    val reveal = remember { Animatable(0f) }
    val revealing = settings.onboardingCompleted
    LaunchedEffect(revealing) {
        if (revealing && welcomeVisible) {
            // Allow the saved theme and first home layout to reach the renderer before revealing it.
            withFrameNanos { }
            reveal.animateTo(1f, tween(680, easing = FastOutSlowInEasing))
            welcomeVisible = false
        }
    }
    Box(Modifier.fillMaxSize().onGloballyPositioned { hostOrigin = it.positionInRoot() }) {
        if (welcomeVisible) WelcomeScreen(settings, saving || revealing, error, onReadyBounds = { checkBounds = it }) { style, theme ->
            if (!saving) {
                saving = true
                error = null
                scope.launch {
                    try { onComplete(style, theme) }
                    catch (e: CancellationException) { throw e }
                    catch (_: java.io.IOException) {
                        error = AppText.get(R.string.ui_unable_to_save_settings_please_try_again)
                    } finally { saving = false }
                }
            }
        }
        if (revealing) {
            val path = remember { Path() }
            Box(Modifier.fillMaxSize().then(if (welcomeVisible) Modifier.testTag("welcome-home-reveal").drawWithContent {
                val center = checkBounds?.center?.minus(hostOrigin) ?: Offset(size.width / 2, size.height / 2)
                val endRadius = hypot(max(center.x, size.width - center.x), max(center.y, size.height - center.y))
                val radius = endRadius * reveal.value
                path.reset()
                path.addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
                clipPath(path) { this@drawWithContent.drawContent() }
            } else Modifier)) { content() }
            // A clipped drawing still has full-size hit targets. Block input until the reveal finishes.
            if (welcomeVisible) Box(Modifier.fillMaxSize().pointerInput(Unit) {
                awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
            })
            BackHandler(welcomeVisible) { }
        }
    }
}
