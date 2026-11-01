package love.yu.jaychou.ui.screens.welcome

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import love.yu.jaychou.R
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.designsystem.*
import love.yu.jaychou.ui.theme.ArchiveTheme
import love.yu.jaychou.ui.theme.isDark
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Ok

@Composable
internal fun WelcomeScreen(
    settings: AppSettings,
    saving: Boolean,
    error: String?,
    onReadyBounds: (Rect) -> Unit = {},
    onFinish: (UiStyle, ThemeMode) -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var accepted by rememberSaveable { mutableStateOf(false) }
    var policy by rememberSaveable { mutableStateOf(false) }
    var style by rememberSaveable { mutableStateOf(settings.uiStyle) }
    var theme by rememberSaveable { mutableStateOf(settings.theme) }
    val preview = settings.copy(uiStyle = style, theme = theme)
    BackHandler(step > 0 || policy) {
        if (!saving) { if (policy) policy = false else step-- }
    }
    ArchiveTheme(preview) {
        val view = LocalView.current
        val dark = preview.isDark()
        SideEffect {
            (view.context as? android.app.Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
        Column(Modifier.fillMaxSize().background(ArchiveUiTheme.colorScheme.background)
            .safeDrawingPadding().testTag("welcome-screen"), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.widthIn(max = 600.dp).fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ui_jay_archive), fontSize = 19.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                if (!policy) Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier
                    .semantics { contentDescription = "${step + 1} / 4" }) {
                    repeat(4) { index ->
                        val color by animateColorAsState(if (index == step) ArchiveUiTheme.colorScheme.primary
                            else ArchiveUiTheme.colorScheme.surfaceContainerHigh, label = "welcome-step-color")
                        Box(Modifier.size(if (index == step) 18.dp else 6.dp, 6.dp).clip(CircleShape).background(color))
                    }
                }
            }
            AnimatedContent(targetState = if (policy) 4 else step, modifier = Modifier.weight(1f).widthIn(max = 600.dp).fillMaxWidth(),
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(240)) + slideInHorizontally(tween(360)) { it / 7 * direction }) togetherWith
                        (fadeOut(tween(180)) + slideOutHorizontally(tween(300)) { -it / 7 * direction })
                }, label = "welcome-page") { page ->
                BoxWithConstraints(Modifier.fillMaxSize().testTag("welcome-step-$page")) {
                    val leadingSpace = if (page == 0 || page == 3) maxHeight * .16f else 24.dp
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(leadingSpace))
                        when (page) {
                            0 -> {
                                Image(painterResource(R.drawable.splash_icon), null, Modifier.size(124.dp))
                                Spacer(Modifier.height(28.dp))
                                WelcomeHeading(R.string.welcome_title, R.string.welcome_subtitle)
                                Spacer(Modifier.height(32.dp))
                                WelcomeInfoCard(R.string.welcome_features)
                            }
                            1 -> {
                                Icon(MiuixIcons.Lock, null, Modifier.size(70.dp), ArchiveUiTheme.colorScheme.primary)
                                Spacer(Modifier.height(26.dp))
                                WelcomeHeading(R.string.welcome_statement, R.string.welcome_statement_subtitle)
                                Spacer(Modifier.height(26.dp))
                                Card(Modifier.fillMaxWidth(), cornerRadius = 22.dp, insideMargin = PaddingValues(20.dp)) {
                                    Text(stringResource(R.string.welcome_statement_summary), fontSize = 16.sp, lineHeight = 26.sp,
                                        color = ArchiveUiTheme.colorScheme.onBackgroundVariant)
                                    TextButton(stringResource(R.string.welcome_read_details), { policy = true }, Modifier.testTag("welcome-policy").padding(top = 10.dp))
                                }
                                Spacer(Modifier.height(18.dp))
                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .toggleable(accepted, role = Role.Checkbox, onValueChange = { accepted = it })
                                    .testTag("welcome-agreement").padding(vertical = 16.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(Modifier.size(28.dp).clip(CircleShape)
                                        .background(if (accepted) ArchiveUiTheme.colorScheme.primary else ArchiveUiTheme.colorScheme.surfaceContainerHigh),
                                        contentAlignment = Alignment.Center) {
                                        if (accepted) Icon(MiuixIcons.Ok, null, Modifier.size(20.dp), Color.White)
                                    }
                                    Text(stringResource(R.string.welcome_agreement), fontSize = 16.sp, lineHeight = 25.sp)
                                }
                            }
                            2 -> {
                                Image(painterResource(R.drawable.splash_icon), null, Modifier.size(80.dp))
                                Spacer(Modifier.height(20.dp))
                                WelcomeHeading(R.string.welcome_appearance, R.string.welcome_appearance_subtitle)
                                Spacer(Modifier.height(24.dp))
                                Column(Modifier.selectableGroup()) { UiStyle.entries.forEach { choice ->
                                    WelcomeChoice(choice.label, stringResource(when (choice) {
                                        UiStyle.MIUIX -> R.string.welcome_miuix
                                        UiStyle.COUI -> R.string.welcome_coui
                                        UiStyle.MATERIAL3 -> R.string.welcome_material
                                    }), choice == style, "welcome-style-${choice.name}") { style = choice }
                                    Spacer(Modifier.height(10.dp))
                                } }
                                Spacer(Modifier.height(12.dp))
                                Text(stringResource(R.string.welcome_color_mode), fontSize = 13.sp,
                                    color = ArchiveUiTheme.colorScheme.onBackgroundVariant, modifier = Modifier.align(Alignment.Start))
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ThemeMode.entries.forEach { choice ->
                                        Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                            .background(if (theme == choice) ArchiveUiTheme.colorScheme.primaryContainer else ArchiveUiTheme.colorScheme.surfaceContainer)
                                            .selectable(theme == choice, role = Role.RadioButton, onClick = { theme = choice })
                                            .testTag("welcome-theme-${choice.name}").padding(vertical = 14.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center) {
                                            Text(choice.label, fontSize = 14.sp, textAlign = TextAlign.Center,
                                                color = if (theme == choice) ArchiveUiTheme.colorScheme.onPrimaryContainer else ArchiveUiTheme.colorScheme.onSurfaceContainer)
                                        }
                                    }
                                }
                            }
                            3 -> {
                                Box(Modifier.size(100.dp).onGloballyPositioned { onReadyBounds(it.boundsInRoot()) }
                                    .testTag("welcome-ready-check").clip(CircleShape).background(ArchiveUiTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                    Icon(MiuixIcons.Ok, null, Modifier.size(54.dp), ArchiveUiTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.height(28.dp))
                                WelcomeHeading(R.string.welcome_ready, R.string.welcome_ready_subtitle)
                                Spacer(Modifier.height(30.dp))
                                WelcomeInfoCard(R.string.welcome_ready_note)
                            }
                            4 -> {
                                WelcomeHeading(R.string.welcome_statement, R.string.welcome_policy_subtitle)
                                Spacer(Modifier.height(24.dp))
                                WelcomeInfoCard(R.string.welcome_policy_local)
                                Spacer(Modifier.height(14.dp))
                                WelcomeInfoCard(R.string.welcome_policy_network)
                                Spacer(Modifier.height(14.dp))
                                WelcomeInfoCard(R.string.welcome_policy_scope)
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
            Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
                if (error != null) Text(error, Modifier.padding(bottom = 12.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    fontSize = 13.sp, color = ArchiveUiTheme.colorScheme.onBackgroundVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (step > 0 && !policy) WelcomeAction(stringResource(R.string.welcome_previous), false, !saving,
                        Modifier.weight(1f).testTag("welcome-previous")) { step-- }
                    WelcomeAction(stringResource(when {
                        policy -> R.string.welcome_back
                        saving -> R.string.welcome_saving
                        step == 0 -> R.string.welcome_begin
                        step == 3 -> R.string.welcome_enter
                        else -> R.string.welcome_next
                    }), true, !saving && (policy || step != 1 || accepted), Modifier.weight(1f).testTag("welcome-next")) {
                        if (!saving && (policy || step != 1 || accepted)) {
                            when { policy -> policy = false; step == 3 -> if (accepted) onFinish(style, theme); else -> step++ }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeHeading(title: Int, subtitle: Int) {
    Text(stringResource(title), fontSize = 32.sp, fontWeight = FontWeight.Medium, lineHeight = 42.sp,
        textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(10.dp))
    Text(stringResource(subtitle), fontSize = 15.sp, lineHeight = 24.sp, textAlign = TextAlign.Center,
        color = ArchiveUiTheme.colorScheme.onBackgroundVariant)
}

@Composable
private fun WelcomeInfoCard(text: Int) {
    Card(Modifier.fillMaxWidth(), cornerRadius = 22.dp, insideMargin = PaddingValues(20.dp)) {
        Text(stringResource(text), fontSize = 16.sp, lineHeight = 27.sp, color = ArchiveUiTheme.colorScheme.onBackgroundVariant)
    }
}

@Composable
private fun WelcomeChoice(title: String, summary: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(if (ArchiveUiTheme.isMaterial) 16.dp else 20.dp)
    Row(Modifier.fillMaxWidth().clip(shape)
        .background(if (selected) ArchiveUiTheme.colorScheme.primaryContainer else ArchiveUiTheme.colorScheme.surfaceContainer)
        .then(if (selected) Modifier.border(1.dp, ArchiveUiTheme.colorScheme.primary.copy(alpha = .5f), shape) else Modifier)
        .selectable(selected, role = Role.RadioButton, onClick = onClick).testTag(tag).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Medium,
                color = if (selected) ArchiveUiTheme.colorScheme.onPrimaryContainer else ArchiveUiTheme.colorScheme.onSurfaceContainer)
            Spacer(Modifier.height(5.dp))
            Text(summary, fontSize = 13.sp, lineHeight = 20.sp,
                color = if (selected) ArchiveUiTheme.colorScheme.onPrimaryContainer.copy(alpha = .85f) else ArchiveUiTheme.colorScheme.onBackgroundVariant)
        }
        if (selected) Icon(MiuixIcons.Ok, null, Modifier.padding(start = 10.dp).size(22.dp), ArchiveUiTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun WelcomeAction(label: String, primary: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    // COUI's high container can be white even in dark mode; use the matched surface pair.
    val color = if (primary) ArchiveUiTheme.colorScheme.primary else ArchiveUiTheme.colorScheme.surfaceContainer
    val foreground = if (!primary) ArchiveUiTheme.colorScheme.onSurfaceContainer
        else if (color.luminance() > .45f) Color.Black else Color.White
    Box(modifier.clip(RoundedCornerShape(if (ArchiveUiTheme.isMaterial) 28.dp else 18.dp))
        .background(color.copy(alpha = if (enabled) 1f else .4f))
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
        Text(label, color = foreground.copy(alpha = if (enabled) 1f else .5f), fontSize = 18.sp, textAlign = TextAlign.Center)
    }
}
