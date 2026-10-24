# 两套界面风格

在「设置 → 主题风格 → 界面风格」切换 Miuix 和 Material Design 3。新安装及原有安装默认使用 Miuix。

两种风格使用各自原生的主题、导航栏、顶栏、卡片、按钮、设置控件和弹窗。Material Design 3 使用 AndroidX Material 3 1.4.0，Miuix 保留现有 0.9.3 组件及玻璃效果。

## 设置隔离

深浅色、动态配色、自定义颜色、专辑布局按风格独立保存。切回原风格时恢复原来的设置。Material 3 默认启用设备支持的动态配色；Miuix 保留原有默认值及既有存储键。

Miuix 的悬浮栏、液态玻璃、背景模糊开关仅影响 Miuix。Material 3 使用原生导航栏和表面色，不创建 Miuix 的 backdrop 或 shader。

语言、资料、专辑分类、歌词导入以及预测性返回等内容与功能设置共用。切换时保留当前页面，歌词页继续不显示主打星标。

## 代码边界

- `ui/theme/ArchiveTheme.kt` 选择主题；`MaterialArchiveTheme.kt` 处理 Material 配色。
- `ui/designsystem/` 为共享页面提供原生组件与主题 token 的适配边界。Miuix 和 Material 分支各自调用原生库，不复制专辑、演唱会或歌词业务流程。
- `ui/navigation/ArchiveBottomNavigationBar.kt` 为 Material 使用标准 NavigationBar，为 Miuix 保留原有导航模式。
- `SettingsRepository` 保留原有 Miuix 外观键，并以 `material3_` 前缀保存 Material 外观。`ui_style` 仅记录当前选择；每次修改只写当前风格对应的字段。

`UiStyleSwitchTest` 验证双向切换、当前页面保留、两套配置独立持久保存，以及 Material 分类、搜索、专辑详情和歌词阅读。
