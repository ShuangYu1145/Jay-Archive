package love.yu.jaychou.ui.screens.settings
import love.yu.jaychou.ui.effects.materialCardSurface

import love.yu.jaychou.ui.components.ArchiveScaffold
import love.yu.jaychou.ui.components.PreferenceCard
import love.yu.jaychou.ui.components.PreferenceTitle
import love.yu.jaychou.ui.components.PreferenceValue
import love.yu.jaychou.ui.components.SettingSwitch
import love.yu.jaychou.data.AppText
import love.yu.jaychou.R
import android.os.Build
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.rememberLazyListState
import love.yu.jaychou.ui.components.ArchiveGlyph
import love.yu.jaychou.ui.components.Glyph
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Translate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.designsystem.*
import love.yu.jaychou.ui.designsystem.OverlayDropdownPreference
import love.yu.jaychou.ui.designsystem.ArrowPreference
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import love.yu.jaychou.ui.designsystem.couiContentColumn
import love.yu.jaychou.ui.designsystem.rememberCouiWindowMetrics

@Composable
fun SettingsScreen(
    settings: AppSettings, state: LazyListState, bottom: Dp,
    onTheme: (ThemeMode) -> Unit, onMonet: (Boolean) -> Unit,
    onBlur: (Boolean) -> Unit, onPredictive: (Boolean) -> Unit,
    onAlbumLayout: (AlbumLayout) -> Unit,
    onCustomColor: (Int?) -> Unit,
    onLiquidGlass: (Boolean) -> Unit = {},
    onFloatingBottomBar: (Boolean) -> Unit = {},
    onUiStyle: (UiStyle) -> Unit = {},
    onTopAppBarBlur: (love.yu.jaychou.data.TopAppBarBlur) -> Unit = {},
    onDetailVisibility: (Boolean) -> Unit = {},
    active: Boolean = true,
) {
    val settingsContext = androidx.compose.ui.platform.LocalContext.current
    val repository = remember(settingsContext) { SettingsRepository(settingsContext) }
    val settingsScope = rememberCoroutineScope()
    var page by rememberSaveable { mutableStateOf("main") }
    val themeState = rememberLazyListState()
    val motionState = rememberLazyListState()
    val aboutState = rememberLazyListState()
    val visibilityCallback by rememberUpdatedState(onDetailVisibility)
    val themeTitle = AppText.get(R.string.ui_theme_settings)
    val motionTitle = AppText.get(R.string.ui_animations_and_gestures)
    var colorPicker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val customColorVisible = settings.uiStyle == UiStyle.MATERIAL3 || !settings.monet
    LaunchedEffect(customColorVisible) {
        if (!customColorVisible) colorPicker = false
    }
    val screen: @Composable (String) -> Unit = { currentPage ->
        val behavior = rememberArchiveScrollBehavior()
        val coui = ArchiveUiTheme.isCoui
        val metrics = rememberCouiWindowMetrics()
        ArchiveScaffold(when (currentPage) { "theme" -> themeTitle; "motion" -> motionTitle; else -> AppText.get(R.string.ui_settings) },
            behavior = behavior.takeIf { currentPage == "main" }, small = currentPage != "main",
            navigationIcon = { if (currentPage != "main") Box(Modifier.testTag("settings-back")) {
                love.yu.jaychou.ui.components.BackButton({ page = "main" }, AppText.get(R.string.ui_settings_back))
            } },
            contentMaxWidth = if (coui) metrics.formMaxWidth else null) { padding ->
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
            LazyColumn(
                state = when (currentPage) { "theme" -> themeState; "motion" -> motionState; else -> state },
                modifier = Modifier.couiContentColumn(if (coui) metrics.formMaxWidth else null)
                    .then(if (currentPage == "main") Modifier.nestedScroll(behavior.nestedScrollConnection) else Modifier)
                    .testTag("settings-list"),
                contentPadding = PaddingValues(
                    start = if (coui) metrics.pageInset else 12.dp,
                    end = if (coui) metrics.pageInset else 12.dp,
                    top = padding.calculateTopPadding() + 8.dp, bottom = if (currentPage != "main") WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp else bottom),
            ) {
                if (currentPage == "main") {
                    item("appearance-title") { PreferenceTitle(AppText.get(R.string.ui_appearance)) }
                    item("appearance") {
                        PreferenceCard {
                            BasicComponent(title = themeTitle, modifier = Modifier.testTag("theme-settings"),
                                startAction = { Icon(MiuixIcons.Theme, null, Modifier.size(28.dp)) },
                                endActions = { Icon(MiuixIcons.ChevronForward, null, Modifier.size(20.dp), tint = ArchiveUiTheme.colorScheme.onBackgroundVariant) },
                                onClick = { page = "theme" })
                            PreferenceDivider()
                            OverlayDropdownPreference(
                                title = AppText.get(R.string.ui_settings_language),
                                items = AppLanguage.entries.map {
                                    if (it == AppLanguage.SYSTEM) AppText.fromOriginal("跟随系统") else it.nativeName
                                }, selectedIndex = AppLanguage.current().ordinal,
                                onSelectedIndexChange = { AppLanguage.entries[it].select() },
                                startAction = { Icon(MiuixIcons.Translate, null, Modifier.size(28.dp)) },
                                modifier = Modifier.testTag("app-language"),
                            )
                            PreferenceDivider()
                            BasicComponent(title = motionTitle, modifier = Modifier.testTag("motion-settings"),
                                startAction = { ArchiveGlyph(Glyph.GESTURE, Modifier.size(28.dp)) },
                                endActions = { Icon(MiuixIcons.ChevronForward, null, Modifier.size(20.dp), tint = ArchiveUiTheme.colorScheme.onBackgroundVariant) },
                                onClick = { page = "motion" })
                        }
                    }
                    item("updates-title") { PreferenceTitle(AppText.get(R.string.ui_update_settings)) }
                    item("updates") { love.yu.jaychou.ui.updates.UpdatePreferences(settings) }
                    item("about") {
                        Column(Modifier.padding(top = if (coui) metrics.groupSpacing else 16.dp)) {
                            PreferenceCard {
                                BasicComponent(title = AppText.get(R.string.ui_about), modifier = Modifier.testTag("about-settings"),
                                    startAction = { Icon(MiuixIcons.Info, null, Modifier.size(28.dp)) },
                                    endActions = { Icon(MiuixIcons.ChevronForward, null, Modifier.size(20.dp), tint = ArchiveUiTheme.colorScheme.onBackgroundVariant) },
                                    onClick = { page = "about" })
                            }
                        }
                    }
                }
                if (currentPage == "theme") {
                    item("style-title") { PreferenceTitle(AppText.get(R.string.ui_interface_style)) }
                    item("style") {
                        PreferenceCard {
                            if (ArchiveUiTheme.isMaterial) {
                                listOf(UiStyle.MATERIAL3, UiStyle.MIUIX, UiStyle.COUI).forEach { style ->
                                    val selected = settings.uiStyle == style
                                    androidx.compose.material3.ListItem(
                                        headlineContent = { androidx.compose.material3.Text(style.label,
                                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium) },
                                        supportingContent = { androidx.compose.material3.Text(when (style) {
                                            UiStyle.MATERIAL3 -> "Google Material 3"
                                            UiStyle.MIUIX -> "HyperOS"
                                            UiStyle.COUI -> "ColorOS"
                                        }) },
                                        trailingContent = { androidx.compose.material3.RadioButton(selected, onClick = null) },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                            .materialCardSurface(androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                                if (selected) androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                                                else androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .testTag("ui-style-${style.name}")
                                            .selectable(selected, role = androidx.compose.ui.semantics.Role.RadioButton,
                                                onClick = { onUiStyle(style) }),
                                        colors = androidx.compose.material3.ListItemDefaults.colors(
                                            containerColor = androidx.compose.ui.graphics.Color.Transparent),
                                    )
                                }
                            } else {
                            OverlayDropdownPreference(
                                title = AppText.get(R.string.ui_interface_style),
                                summary = AppText.get(R.string.ui_interface_style_summary),
                                items = UiStyle.entries.map { it.label }, selectedIndex = settings.uiStyle.ordinal,
                                onSelectedIndexChange = { onUiStyle(UiStyle.entries[it]) },
                                modifier = Modifier.testTag("ui-style"),
                            )
                            }
                            PreferenceDivider()
                            SettingSwitch(AppText.get(R.string.ui_floating_navigation_bar),
                                AppText.get(R.string.ui_floating_bottom_bar_summary),
                                settings.floatingBottomBar, onFloatingBottomBar, true)
                            AnimatedSetting(visible = settings.floatingBottomBar) {
                                PreferenceDivider()
                                SettingSwitch(AppText.get(R.string.ui_liquid_glass),
                                    if (Build.VERSION.SDK_INT >= 33) AppText.get(R.string.ui_liquid_glass_summary)
                                    else AppText.get(R.string.ui_requires_android_13_using_solid_surfaces_on_this_device),
                                    settings.liquidGlass, onLiquidGlass, Build.VERSION.SDK_INT >= 33)
                            }
                        }
                    }

                    item("miuix-title") { PreferenceTitle(settings.uiStyle.label) }
                    item("theme") {
                        PreferenceCard {
                            OverlayDropdownPreference(
                                title = AppText.get(R.string.ui_theme_mode), items = ThemeMode.entries.map { it.label },
                                selectedIndex = settings.theme.ordinal,
                                onSelectedIndexChange = { onTheme(ThemeMode.entries[it]) },
                                modifier = Modifier.testTag("theme-mode"),
                            )
                            PreferenceDivider()
                            SettingSwitch(AppText.get(R.string.ui_enable_monet_dynamic_colors),
                                if (Build.VERSION.SDK_INT >= 31) AppText.get(R.string.ui_use_colors_from_your_wallpaper) else AppText.get(R.string.ui_requires_android_12_using_the_app_theme_color_on_this_device),
                                settings.monet, onMonet, Build.VERSION.SDK_INT >= 31)
                            PreferenceDivider()
                            SettingSwitch(AppText.get(R.string.ui_enable_blur),
                                if (Build.VERSION.SDK_INT >= 33) AppText.get(R.string.ui_blur_content_behind_top_bars_and_the_floating_navigation_bar) else AppText.get(R.string.ui_requires_android_13_using_solid_surfaces_on_this_device),
                                settings.blur, onBlur, Build.VERSION.SDK_INT >= 33)
                            if (settings.uiStyle != UiStyle.COUI && settings.blur && Build.VERSION.SDK_INT >= 33) {
                                OverlayDropdownPreference(
                                    title = AppText.get(R.string.ui_top_app_bar_blur),
                                    items = listOf("Gaussian", "Progressive"),
                                    summary = AppText.get(R.string.ui_top_app_bar_blur_summary),
                                    selectedIndex = settings.topAppBarBlur.ordinal,
                                    onSelectedIndexChange = { onTopAppBarBlur(TopAppBarBlur.entries[it]) },
                                    modifier = Modifier.testTag("top-app-bar-blur"),
                                )
                            }
                            AnimatedSetting(visible = customColorVisible) {
                                PreferenceDivider()
                                ArrowPreference(title = AppText.get(R.string.ui_custom_color),
                                    summary = if (settings.monet && Build.VERSION.SDK_INT >= 31) AppText.get(R.string.ui_monet_is_enabled_system_colors_take_priority) else AppText.get(R.string.ui_choose_seed_color_for_current_style),
                                    endActions = { PreferenceValue(settings.customColor?.let { "#%06X".format(it and 0xFFFFFF) } ?: AppText.get(if (settings.uiStyle == UiStyle.MATERIAL3) R.string.ui_default_material_color else R.string.ui_default_blue)) },
                                    onClick = { colorPicker = true }, modifier = Modifier.testTag("custom-color"))
                            }
                        }
                    }

                    item("album-title") { PreferenceTitle(AppText.get(R.string.ui_album_browsing)) }

                    item("album-layout") {
                        PreferenceCard {
                            OverlayDropdownPreference(
                                title = AppText.get(R.string.ui_album_layout), summary = settings.albumLayout.description,
                                items = AlbumLayout.entries.map { it.label },
                                selectedIndex = settings.albumLayout.ordinal,
                                onSelectedIndexChange = { onAlbumLayout(AlbumLayout.entries[it]) },
                                modifier = Modifier.testTag("album-layout"),
                            )
                        }
                    }

                    item("background-title") { PreferenceTitle(AppText.get(R.string.ui_custom_background)) }
                    item("background") { BackgroundPreferences(settings) }

                }
                if (currentPage == "motion") {
                    item("motion-title") { PreferenceTitle(AppText.get(R.string.ui_animations_and_gestures)) }
                    item("motion") {
                        PreferenceCard {
                            BasicComponent(title = AppText.get(R.string.ui_page_transitions), summary = AppText.get(R.string.ui_swipe_pages_or_drag_the_navigation_bar), endActions = { PreferenceValue(AppText.get(R.string.ui_swipe)) })
                            PreferenceDivider()
                            BasicComponent(title = AppText.get(R.string.ui_album_transition), summary = AppText.get(R.string.ui_artwork_moves_independently_as_details_appear_background_blur_in), endActions = { PreferenceValue(AppText.get(R.string.ui_zoom)) })
                            PreferenceDivider()
                            OverlayDropdownPreference(
                                title = androidx.compose.ui.res.stringResource(R.string.ui_predictive_animation),
                                items = listOf(androidx.compose.ui.res.stringResource(R.string.ui_animation_none), "AOSP", "Miuix",
                                    androidx.compose.ui.res.stringResource(R.string.ui_zoom), androidx.compose.ui.res.stringResource(R.string.ui_animation_classic)),
                                selectedIndex = settings.predictiveBackAnimation.ordinal,
                                onSelectedIndexChange = { index -> settingsScope.launch {
                                    repository.setPredictiveBackAnimation(PredictiveBackAnimation.entries[index])
                                } }, modifier = Modifier.testTag("predictive-back-animation"),
                            )
                            PreferenceDivider()
                            OverlayDropdownPreference(
                                title = androidx.compose.ui.res.stringResource(R.string.ui_predictive_exit_direction),
                                items = listOf(
                                    androidx.compose.ui.res.stringResource(R.string.ui_exit_follow_gesture),
                                    androidx.compose.ui.res.stringResource(R.string.ui_exit_always_right),
                                    androidx.compose.ui.res.stringResource(R.string.ui_exit_always_left),
                                ), selectedIndex = settings.predictiveBackExitDirection.ordinal,
                                onSelectedIndexChange = { index -> settingsScope.launch {
                                    repository.setPredictiveBackExitDirection(PredictiveBackExitDirection.entries[index])
                                } }, modifier = Modifier.testTag("predictive-back-exit-direction"),
                            )
                            PreferenceDivider()
                            SettingSwitch(AppText.get(R.string.ui_predictive_back_gesture),
                                if (Build.VERSION.SDK_INT >= 34) AppText.get(R.string.ui_preview_the_list_while_swiping_cancel_to_stay_on_details) else AppText.get(R.string.ui_requires_android_14_for_progress_previews_using_standard_back_na),
                                settings.predictiveBack, onPredictive, Build.VERSION.SDK_INT >= 34)
                        }
                    }
                }
            }
            }
        }
    }
    love.yu.jaychou.ui.animation.SecondaryPageHost(
        page = page.takeUnless { it == "main" }, onBack = { page = "main" },
        onVisibilityChanged = { visibilityCallback(it) },
        enabled = active,
        root = { screen("main") }, content = {
            if (it == "about") Box(Modifier.fillMaxSize().testTag("page-ABOUT")) {
                love.yu.jaychou.ui.screens.about.AboutScreen(aboutState,
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
                    active = active && page == "about", onBack = { page = "main" })
            } else screen(it)
        },
    )
    CustomColorSheet(colorPicker && customColorVisible, settings.customColor, { colorPicker = false }, onCustomColor)
}

@Composable
private fun AnimatedSetting(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(280)) + slideInVertically(tween(280)) { -it / 2 } + fadeIn(tween(180)),
        exit = shrinkVertically(tween(220)) + slideOutVertically(tween(220)) { -it / 2 } + fadeOut(tween(150)),
    ) {
        content()
    }
}
