package love.yu.jaychou.ui

import android.os.Build
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
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsScreen(
    settings: AppSettings, state: LazyListState, bottom: Dp,
    onTheme: (ThemeMode) -> Unit, onMonet: (Boolean) -> Unit,
    onBlur: (Boolean) -> Unit, onPredictive: (Boolean) -> Unit,
    onAlbumLayout: (AlbumLayout) -> Unit,
    onCustomColor: (Int?) -> Unit,
) {
    var colorPicker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val behavior = MiuixScrollBehavior()
    ArchiveScaffold("设置", behavior = behavior) { padding ->
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize().nestedScroll(behavior.nestedScrollConnection).testTag("settings-list"),
            contentPadding = PaddingValues(12.dp, padding.calculateTopPadding() + 8.dp, 12.dp, bottom),
        ) {
            item("album-title") { PreferenceTitle("专辑浏览") }
            item("album-layout") {
                PreferenceCard {
                    OverlayDropdownPreference(
                        title = "专辑列表样式", summary = settings.albumLayout.description,
                        items = AlbumLayout.entries.map { it.label },
                        selectedIndex = settings.albumLayout.ordinal,
                        onSelectedIndexChange = { onAlbumLayout(AlbumLayout.entries[it]) },
                        modifier = Modifier.testTag("album-layout"),
                    )
                }
            }
            item("style-title") { PreferenceTitle("主题风格") }
            item("style") {
                PreferenceCard {
                    BasicComponent(title = "UI 引擎", endActions = { PreferenceValue("Miuix") })
                    BasicComponent(title = "悬浮底栏", summary = "沿底栏左右拖动，松手切换页面",
                        endActions = { PreferenceValue("已启用") })
                }
            }
            item("miuix-title") { PreferenceTitle("Miuix") }
            item("theme") {
                PreferenceCard {
                    OverlayDropdownPreference(
                        title = "主题模式", items = ThemeMode.entries.map { it.label },
                        selectedIndex = settings.theme.ordinal,
                        onSelectedIndexChange = { onTheme(ThemeMode.entries[it]) },
                        modifier = Modifier.testTag("theme-mode"),
                    )
                    SettingSwitch("启用 Monet 动态配色",
                        if (Build.VERSION.SDK_INT >= 31) "使用系统壁纸配色" else "Android 12 起支持；当前使用应用主题色",
                        settings.monet, onMonet, Build.VERSION.SDK_INT >= 31)
                    SettingSwitch("启用模糊",
                        if (Build.VERSION.SDK_INT >= 33) "模糊所有顶部栏与悬浮底栏后方的内容" else "需要 Android 13；当前使用实色材质",
                        settings.blur, onBlur, Build.VERSION.SDK_INT >= 33)
                    ArrowPreference(title = "自定义颜色",
                        summary = if (settings.monet && Build.VERSION.SDK_INT >= 31) "Monet 已开启，当前优先使用系统颜色" else "选择种子色，自动生成深浅色 Miuix 调色板",
                        endActions = { PreferenceValue(settings.customColor?.let { "#%06X".format(it and 0xFFFFFF) } ?: "预设蓝色") },
                        onClick = { colorPicker = true }, modifier = Modifier.testTag("custom-color"))
                }
            }
            item("motion-title") { PreferenceTitle("动画与手势") }
            item("motion") {
                PreferenceCard {
                    BasicComponent(title = "页面切换", summary = "横向滑动与底栏拖动联动", endActions = { PreferenceValue("滑动") })
                    BasicComponent(title = "专辑展开", summary = "从卡片展开，返回时收回原位", endActions = { PreferenceValue("缩放") })
                    SettingSwitch("预测性返回手势",
                        if (Build.VERSION.SDK_INT >= 34) "滑动预览列表，取消后留在详情" else "Android 14 起支持进度；当前使用常规返回",
                        settings.predictiveBack, onPredictive, Build.VERSION.SDK_INT >= 34)
                }
            }
        }
    }
    CustomColorSheet(colorPicker, settings.customColor, { colorPicker = false }, onCustomColor)
}
