package love.yu.jaychou.ui.screens.settings

import love.yu.jaychou.ui.components.ArchiveScaffold
import love.yu.jaychou.ui.components.PreferenceCard
import love.yu.jaychou.ui.components.PreferenceTitle
import love.yu.jaychou.ui.components.PreferenceValue
import love.yu.jaychou.ui.components.SettingSwitch
import love.yu.jaychou.data.AppText
import love.yu.jaychou.R
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.designsystem.*
import love.yu.jaychou.ui.designsystem.OverlayDropdownPreference
import love.yu.jaychou.ui.designsystem.ArrowPreference
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme

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
) {
    var colorPicker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val customColorVisible = settings.uiStyle != UiStyle.MIUIX || !settings.monet
    LaunchedEffect(customColorVisible) {
        if (!customColorVisible) colorPicker = false
    }
    val behavior = rememberArchiveScrollBehavior()
    ArchiveScaffold(AppText.get(R.string.ui_settings), behavior = behavior) { padding ->
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize().nestedScroll(behavior.nestedScrollConnection).testTag("settings-list"),
            contentPadding = PaddingValues(12.dp, padding.calculateTopPadding() + 8.dp, 12.dp, bottom),
        ) {
            item("language") {
                PreferenceCard {
                    OverlayDropdownPreference(
                        title = AppText.fromOriginal("语言 / Language"),
                        summary = AppText.fromOriginal("选择应用显示语言"),
                        items = AppLanguage.entries.map {
                            if (it == AppLanguage.SYSTEM) AppText.fromOriginal("跟随系统") else it.nativeName
                        },
                        selectedIndex = AppLanguage.current().ordinal,
                        onSelectedIndexChange = { AppLanguage.entries[it].select() },
                        modifier = Modifier.testTag("app-language"),
                    )
                }
            }
            item("album-title") { PreferenceTitle(AppText.get(R.string.ui_album_browsing)) }

            // 第一个卡片保持不变
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

            item("style-title") { PreferenceTitle(AppText.get(R.string.ui_appearance)) }
            item("style") {
                PreferenceCard {
                    OverlayDropdownPreference(
                        title = AppText.get(R.string.ui_interface_style),
                        summary = AppText.get(R.string.ui_interface_style_summary),
                        items = UiStyle.entries.map { it.label }, selectedIndex = settings.uiStyle.ordinal,
                        onSelectedIndexChange = { onUiStyle(UiStyle.entries[it]) },
                        modifier = Modifier.testTag("ui-style"),
                    )
                    if (settings.uiStyle == UiStyle.MIUIX) {
                    SettingSwitch(AppText.get(R.string.ui_floating_navigation_bar),
                        AppText.get(R.string.ui_floating_bottom_bar_summary),
                        settings.floatingBottomBar, onFloatingBottomBar, true)
                    AnimatedSetting(visible = settings.floatingBottomBar) {
                        SettingSwitch(AppText.get(R.string.ui_liquid_glass),
                            if (Build.VERSION.SDK_INT >= 33) AppText.get(R.string.ui_liquid_glass_summary)
                            else AppText.get(R.string.ui_requires_android_13_using_solid_surfaces_on_this_device),
                            settings.liquidGlass, onLiquidGlass, Build.VERSION.SDK_INT >= 33)
                    }
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
                    SettingSwitch(AppText.get(R.string.ui_enable_monet_dynamic_colors),
                        if (Build.VERSION.SDK_INT >= 31) AppText.get(R.string.ui_use_colors_from_your_wallpaper) else AppText.get(R.string.ui_requires_android_12_using_the_app_theme_color_on_this_device),
                        settings.monet, onMonet, Build.VERSION.SDK_INT >= 31)
                    if (settings.uiStyle == UiStyle.MIUIX) SettingSwitch(AppText.get(R.string.ui_enable_blur),
                        if (Build.VERSION.SDK_INT >= 33) AppText.get(R.string.ui_blur_content_behind_top_bars_and_the_floating_navigation_bar) else AppText.get(R.string.ui_requires_android_13_using_solid_surfaces_on_this_device),
                        settings.blur, onBlur, Build.VERSION.SDK_INT >= 33)
                    AnimatedSetting(visible = customColorVisible) {
                        ArrowPreference(title = AppText.get(R.string.ui_custom_color),
                            summary = if (settings.monet && Build.VERSION.SDK_INT >= 31) AppText.get(R.string.ui_monet_is_enabled_system_colors_take_priority) else AppText.get(R.string.ui_choose_seed_color_for_current_style),
                            endActions = { PreferenceValue(settings.customColor?.let { "#%06X".format(it and 0xFFFFFF) } ?: AppText.get(if (settings.uiStyle == UiStyle.MATERIAL3) R.string.ui_default_material_color else R.string.ui_default_blue)) },
                            onClick = { colorPicker = true }, modifier = Modifier.testTag("custom-color"))
                    }
                }
            }

            item("background-title") { PreferenceTitle(AppText.get(R.string.ui_custom_background)) }
            item("background") { BackgroundPreferences(settings) }

            item("updates-title") { PreferenceTitle(AppText.get(R.string.ui_update_settings)) }
            item("updates") { love.yu.jaychou.ui.updates.UpdatePreferences(settings) }

            item("motion-title") { PreferenceTitle(AppText.get(R.string.ui_animations_and_gestures)) }
            item("motion") {
                PreferenceCard {
                    BasicComponent(title = AppText.get(R.string.ui_page_transitions), summary = AppText.get(R.string.ui_swipe_pages_or_drag_the_navigation_bar), endActions = { PreferenceValue(AppText.get(R.string.ui_swipe)) })
                    BasicComponent(title = AppText.get(R.string.ui_album_transition), summary = AppText.get(R.string.ui_artwork_moves_independently_as_details_appear_background_blur_in), endActions = { PreferenceValue(AppText.get(R.string.ui_zoom)) })
                    SettingSwitch(AppText.get(R.string.ui_predictive_back_gesture),
                        if (Build.VERSION.SDK_INT >= 34) AppText.get(R.string.ui_preview_the_list_while_swiping_cancel_to_stay_on_details) else AppText.get(R.string.ui_requires_android_14_for_progress_previews_using_standard_back_na),
                        settings.predictiveBack, onPredictive, Build.VERSION.SDK_INT >= 34)
                }
            }
        }
    }
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
