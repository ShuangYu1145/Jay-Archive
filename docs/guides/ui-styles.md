# 三种界面风格

在「设置 → 主题风格 → 界面风格」选择 **Miuix、COUI（ColorOS）或 Material Design 3**。默认使用 Miuix，切换后即时生效并保留当前页面，无须重启。

## 风格与组件

| 风格 | 组件与表现 |
| :-- | :-- |
| Miuix | Miuix 0.9.3 主题与组件，支持高斯或渐变顶部模糊，以及可选玻璃导航。 |
| COUI（ColorOS） | COUI 1.1.0 主题与组件，采用超椭圆表面、分组设置、圆形操作按钮与分体悬浮底栏。 |
| Material Design 3 | AndroidX Material 3 1.4.0 主题与组件，提供原生导航、卡片、设置控件与动态配色。 |

三种风格共用专辑、演唱会、对外作曲、搜索与歌词业务流程，均可在应用支持的 Android 设备上选择。

## COUI 顶栏与材质

- 介绍页的专辑、演唱会与作曲标签固定在列表上方，滚动或左右切页时保持位置，操作区域只显示当前分区的按钮。
- 工具栏按钮与非液态玻璃的 COUI 悬浮底栏共用磨砂表面、描边和阴影；小顶栏为按钮阴影保留绘制空间。
- 开启模糊后，介绍页和专辑详情页使用向下渐隐的顶部模糊；绘制范围延伸到顶栏下方，保持内容位置与点击区域不变。
- 搜索页的按钮单独采样背景，避免把包含按钮自身的页面图层作为采样源。

实现与兼容说明见 [COUI 风格接入](coui-ui.md)。

## 设置保存

深浅色、动态配色、自定义颜色与专辑布局按风格独立保存。切回原风格时恢复原来的设置。

- Miuix 保留原有外观键。
- COUI 使用 `coui_` 前缀。
- Material 3 使用 `material3_` 前缀。
- `ui_style` 只记录当前选择，修改外观时只写入当前风格对应的字段。

悬浮底栏、液态玻璃、背景模糊、自定义背景、语言、个人资料、专辑分类、歌词导入和预测性返回等设置或数据共用。Material 3 的设置页不提供背景模糊开关；Miuix 和 COUI 提供该开关，Miuix 另可选择顶部高斯／渐变模糊。

## 导航与系统兼容

三种风格均支持标准底栏、悬浮底栏与可选液态玻璃。关闭液态玻璃时使用各自风格的导航；COUI 悬浮底栏由两侧圆形入口和中间胶囊组成。宽屏可使用对应风格的侧栏导航。

真实背景模糊与液态玻璃仅在 Android 13+ 创建采样与 shader 对象；旧系统保留实色表面。动态配色要求 Android 12+。

## 代码入口

- `ui/theme/ArchiveTheme.kt`：选择风格主题；`CouiArchiveTheme.kt` 与 `MaterialArchiveTheme.kt` 分别处理对应配色。
- `ui/designsystem/`：共享页面的组件与主题适配，业务页面共用同一数据与交互流程。
- `ui/components/ArchiveScaffold.kt`：顶栏、顶部安全区与分区操作。
- `ui/navigation/`：标准底栏、悬浮底栏、液态玻璃与侧栏导航。
- `ui/effects/`：背景采样、COUI 磨砂表面与顶部渐变模糊。
- `data/SettingsRepository.kt`：风格选择与外观设置的持久化。

相关回归测试包括 `UiStyleSwitchTest`、`UiStylePersistenceTest`、`CouiToolbarLayoutTest` 与 `AlbumSearchNavigationTest`。
