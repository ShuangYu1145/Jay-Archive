# COUI 风格接入

## 入口与持久化

- `data/SettingsRepository.kt`：复用 `UiStyle` 和唯一的 `appearance` DataStore；`ui_style` 仍保存枚举名称，选择顺序为 Miuix / COUI（ColorOS）/ Material Design 3。
- 现有 Miuix 无前缀键和 Material 的 `material3_` 键保持兼容；COUI 使用 `coui_` 前缀保存主题、动态色、自定义色和专辑布局。其他业务设置继续共享。
- `SettingsScreen` 继续使用原有风格下拉入口。`MainActivity` 原有 Flow 收集路径触发全局重组，`ArchiveTheme` 原有 movable content 保留页面状态，无须重启。

## 原生组件

依赖锁定为官方 Maven Central 发布的 `io.github.suqi8.coui.kmp:coui-ui:1.1.0` 和 `coui-preference:1.1.0`。

- `CouiArchiveTheme` 提供官方 `COUITheme`；使用库的颜色、字形和动态色支持。
- 在现有 `ui/designsystem` 封装中增加 COUI 分支，保持调用接口；Card 颜色和按压反馈类型在边界转换，不让业务页面依赖第三套类型。
- Scaffold、大小顶栏、滚动行为、卡片、按钮、文字、图标、设置项、开关、复选框、下拉菜单、颜色选择器、底部抽屉、底部导航和侧栏均调用官方组件。
- 歌词顶栏和滑块、更新弹窗、关于页彩蛋弹窗补充原生 COUI 分支。
- COUI 超椭圆表面、按压反馈、菜单和弹窗动画由官方组件提供。顶栏和导航栏背景模糊复用项目已有的背景采样，Android 13 以下保留实色降级。Miuix、COUI 和 Material Design 3 均支持悬浮导航和液态玻璃开关；COUI 的非玻璃悬浮栏使用官方组件，液态玻璃复用现有实现并读取当前风格的主题。

## 官方资料与兼容性

- 源码：https://github.com/suqi8/coui
- 文档：https://suqi8.github.io/coui/
- 核对源码提交：`e4e915ca67ea0f2689c746822dcc5fda1f84e817`
- 官方源码 Kotlin 2.4.10 / Compose Multiplatform 1.11.1 / minSdk 23；本项目 Kotlin 2.4.20 / AndroidX Compose 1.11.2 / minSdk 26。保留项目原有工具链，已通过依赖元数据检查、重复类检查和 debug 应用及测试 APK 构建。

## 验证

- `UiStylePersistenceTest`：旧名称、旧键、未知值回退、三套配置键隔离。
- `UiStyleSwitchTest`：三风格即时切换并保留当前页、真实 DataStore 写入及 Repository 重建读取、COUI 与 Material 的分类筛选—搜索—专辑—歌词流程。
- `AdaptiveNavigationTest`：三风格手机及平板/横屏导航。
- 现有 `UiStyle.entries` 测试自动覆盖第三种风格，包括简介子页、作品页、歌词明暗主题和阅读设置。
- `UpdateFlowTest`：增加 COUI 原生更新弹窗验证。

完整单元测试中，`CatalogTest` 的两项现有数据校验失败：扩展发行分类数量与预期不符，以及专辑封面文件大小断言失败。本次未修改相关目录数据、封面或断言。

最终验证结果（2026-10-30）：debug 应用与测试 APK 构建通过；新增持久化单元测试 2/2 通过；Pixel 10 Pro Android 17 模拟器主流程 16/16 通过，补充 COUI 颜色选择器及关于页弹窗交互测试 1/1 通过。已检查 COUI 设置页、歌词页、颜色选择器及弹窗截图。完整单元测试 53 项中 51 项通过，另 2 项为上文记录的目录数据校验失败。

弹窗布局修复：COUI 的 WindowDialog 自定义 content 默认无内边距；短提示改用内置 summary，长更新正文增加水平内边距并在剩余空间滚动，按钮统一使用官方 DialogButtonBar，关于页输入弹窗同步适配。已分别在真实竖屏和横屏窗口验证浅/深色、最新版本提示、网络失败提示及长更新说明（共 12 个场景）；更新流程和关于页交互测试 3 项通过。模拟器尺寸在验证后恢复。

COUI 导航扩展：设置中开放原有悬浮底栏和液态玻璃开关，继续使用同一 DataStore 的既有设置。非玻璃悬浮栏使用官方 COUI FloatingNavigationBar；液态玻璃、按压拖动和背景采样复用原实现，文字、图标及颜色读取当前风格主题。Android 13 以下保留原生悬浮栏降级。Debug 构建通过；导航模式/模糊/开关持久化、玻璃拖动及全局风格切换共 12 项界面测试通过（持续动画的全局开关测试使用受控时钟单独复测通过）。

Material Design 3 导航扩展：三种风格均开放原有悬浮底部栏和液态玻璃开关，继续使用既有 DataStore 设置。Material 非玻璃悬浮栏使用 Material NavigationBar/NavigationBarItem，保留原生配色和选中反馈；液态玻璃使用当前 Material 主题。标准底栏保留原生不透明表面。Debug 构建通过，三种风格模式及持久化、全局开关、风格切换共 9 项界面回归测试通过（Material 模式测试按标准栏的原生实色行为调整后单独复测通过）。
