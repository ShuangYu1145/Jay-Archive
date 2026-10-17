# 杰作 · Jay Archive

以周杰伦及其录音室专辑为主题的原生 Android 应用。Kotlin + Jetpack Compose + Miuix，包名 `love.yu.jaychou`，版本 `0.2`（Beta）。包含首页、专辑、空白演唱会、设置、关于五页，以及专辑详情和歌词阅读页。默认完整收录 16 张录音室专辑（175 条曲目），扩展目录另含 33 个已核实发行版本；三种专辑布局与多选分类均可用。不提供音乐播放或下载。

### 0.2 新增与修复

- 五页悬浮导航：首页、专辑、演唱会、设置、关于。演唱会页按要求仅保留标题与导航，内容为空，未虚构演出。
- 专辑页右上角“分类”使用原生 Miuix `WindowBottomSheet`、`CheckboxPreference`；支持录音室 CD、演唱会 CD、EP、单曲、原声与其他多选，取消不生效、确定才应用，至少选择一项。默认录音室，分类及其滚动状态可在 Activity 重建后恢复。
- 保留横向列表 / 双列网格 / 大封面卡片，通过设置选择并持久化。各“分类组合 × 布局”分别保存滚动位置。统一顶部说明与大标题 24dp 起点，以及网格标题两行的固定行高和日期基线。
- 修复卡片展开时椭圆一帧：投影圆角按 X/Y 各自缩放补偿，横向卡片和窄曲目行也维持屏幕上的圆角矩形。歌词页从点击曲目行按同一动画展开，逐级返回保留专辑曲目位置，支持重建与预测返回取消。
- 所有一级页、专辑详情和歌词页统一顶部栏：使用 Miuix Blur 捕获实际滚动内容，顶栏与状态栏区域为采样模糊加高不透明度材质；API 26–32 或关闭模糊时实色降级。顶部系统 inset 由顶栏单独处理，页面宿主仅处理横向安全区并裁切窗口边界。
- 设置新增“自定义颜色”：原生 Miuix `ColorPicker` 可任选种子色，另有快捷色与恢复预设。Miuix 生成深浅色调色板，API 26+ 可用，选择持久化。Monet 开启且系统支持时优先使用壁纸调色板，关闭后恢复已选自定义色。
- 歌词为按需读取的 LRCLIB 社区版本，显示多个版本供核对；可离线导入 LRC/TXT，支持加载/失败/无结果/纯音乐及删除本地文件。未预装商业歌曲歌词，不承诺所有歌曲都有在线歌词。
- 扩展 6 张演唱会录音、4 张 EP、21 张单曲及 2 张原声/合作发行，来自 Apple Music 台湾数字版元数据；日期、曲序、碟片号、封面与来源逐条保存，版本差异在详情说明。不是将巡演活动当作唱片，亦不是所有地区发行的穷尽清单。

### 1.1.1 修复

修复专辑详情缓慢起手拖动时偶尔不能滚动：移除详情父容器对 Final 阶段触摸事件的无条件消费，改为位于详情后方的独立全屏触摸隔离层。该层通过同级节点命中测试阻挡底层网格，详情列表、返回按钮和来源链接继续自行处理手势；展开和收回动画期间也覆盖卡片之外的区域。参考 [Compose 事件命中与传播规则](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures#event-dispatching)。新增真实触摸回归覆盖缓慢起手、上下连续滚动、取消后继续滚动、重新打开与预测返回取消后滚动。

## 导入与运行

1. 使用支持 AGP 9.4.1 的 Android Studio 打开本目录。
2. Gradle Daemon 使用 **JDK 25**：工程的 `gradle/gradle-daemon-jvm.properties` 已固定版本 25，可自动查找或下载。本机实际构建为 Microsoft OpenJDK 25.0.1，启动 Wrapper 的 Java 为 Zulu 21.0.12；Android Studio 中可直接选择 JDK 25。SDK Manager 安装 **Android SDK Platform 37.0** 与 **Build Tools 36.0.0**。Android 设备最低为 **Android 8.0 / API 26**。
3. 配置本机 `local.properties` 的 `sdk.dir`；不要将其他机器的 SDK 路径直接用于自己的机器。此文件不属于源码交付包。
4. Gradle Sync 后运行 `app`。首次同步需访问 Google Maven、Maven Central 及 Gradle 分发站点。

Windows 命令：

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

APK：`app/build/outputs/apk/debug/app-debug.apk`。这是可安装的 Debug 签名包，正式发布签名由项目所有者配置。

## 已核实的依赖与配置

| 项目 | 固定版本 / 配置 |
| --- | --- |
| Gradle Wrapper | 9.6.0，保留分发 SHA-256 校验 |
| Android Gradle Plugin | 9.4.1，使用内置 Kotlin 支持 |
| Kotlin / Compose compiler | 2.4.20 / 2.4.20 |
| AndroidX Compose UI、Foundation、Animation | 1.11.2 |
| Miuix UI / Blur / Preference | 0.9.3 / 0.9.3 / 0.9.3 |
| Activity Compose | 1.12.4 |
| Lifecycle Compose | 2.10.0 |
| Preferences DataStore | 1.2.1 |
| Coil Compose | 2.7.0 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 26 |
| Build Tools / Java 字节码 | 36.0.0 / 17 |
| Gradle Daemon JVM | JDK 25（实测 Microsoft 25.0.1） |

为复现构建，固定采用已核实并实际编译的 Miuix **v0.9.3**，不是跟随 `main` 或声称使用最新版。上游该标签使用 Kotlin 2.4.0、Compose Multiplatform 1.11.1、compile SDK 37；本工程使用 AndroidX Compose 1.11.2 与更新的同系列 Kotlin 2.4.20，编译时已验证兼容。根 `build.gradle.kts` 显式提升 AGP 内置 KGP 版本，与 Compose compiler 对齐，不再应用 `org.jetbrains.kotlin.android` 插件。

来源：[Miuix v0.9.3 源码](https://github.com/compose-miuix-ui/miuix/tree/v0.9.3)、[上游版本目录](https://github.com/compose-miuix-ui/miuix/blob/v0.9.3/gradle/libs.versions.toml)、[上游 SDK 配置](https://github.com/compose-miuix-ui/miuix/blob/v0.9.3/build-plugins/src/main/kotlin/BuildConfig.kt)。

实际 Android 依赖坐标：

```kotlin
implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.3")
implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.3")
implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.3")
```

版本集中在 `gradle/libs.versions.toml`。实际使用 Miuix 的 `MiuixTheme`、`ThemeController`、`ColorSchemeMode`、`Scaffold`、`TopAppBar`、`SmallTopAppBar`、`MiuixScrollBehavior`、`Card`、`SmallTitle`、`BasicComponent`、`Text`、`Button`，以及独立 Preference 模块的 `OverlayDropdownPreference`、`SwitchPreference`、`ArrowPreference`。API 与 v0.9.3 发布源码对照；没有把新版文档中的接口名直接当成旧版接口。

## 页面与卡片动画

- 五个一级页面共用 Compose `HorizontalPager`。点击底栏使用弹簧动画横向切换，也可直接横滑页面；页面自身的纵向列表状态独立保存。
- 底栏可从任意选项开始左右拖动。选中胶囊跟随手指并轻微伸缩，越过选项有轻触反馈；拖动位置同步给 Pager，因此页面跟随移动，松手吸附到最近页面。系统取消触摸时恢复拖动前页面。无障碍点击与普通点按仍然可用。
- 专辑点击记录卡片在窗口中的实际矩形和 Compose `GraphicsLayer` 渲染层。详情面板从该矩形弹簧展开，原卡片图层渐变为详情，底层页面轻微后退、底栏淡出；返回时反向收回。没有在主线程截取 Bitmap。
- 首页代表作品卡片也采用同一过渡；返回仍在首页原位置。源页面在详情下面保留，不能在展开或关闭过程中误触底层列表。重建后不持有已失效的图层；直接恢复完整详情，返回使用已保存的相对起点。
- 预测返回继续支持进度和取消。完成手势后接续收回动画，取消则恢复当前详情。动画使用 Compose 时钟，遵循系统动画时长缩放；真正的背景模糊仍限 API 33+，页面与卡片动画可在 API 26+ 使用。

滑动/拖动和容器展开是本项目的自定义 Compose 动效，视觉参考 iOS 式可拖动胶囊与 Android 桌面应用展开，不声称调用或完整复制系统专有动画。Miuix 原生组件负责标题折叠、列表行按压、开关和下拉菜单动效。

风格依据：官方[最佳实践](https://compose-miuix-ui.github.io/miuix/zh_CN/guide/best-practices)及同一固定标签的[SettingsPage 示例](https://github.com/compose-miuix-ui/miuix/blob/v0.9.3/example/shared/src/commonMain/kotlin/SettingsPage.kt)、[Preference 模块源码](https://github.com/compose-miuix-ui/miuix/tree/v0.9.3/miuix-preference)。该最佳实践页面是示例项目索引，不是逐条强制规范。

## 模糊、玻璃、配色与返回手势

| 能力 | 生效条件 | 降级方式 |
| --- | --- | --- |
| Miuix 背景模糊 | API 33+、模糊开关开启、正常硬件加速渲染 | API 26–32 或关闭开关：实色顶栏与胶囊，保留高光、阴影和选中状态 |
| Monet 系统动态配色 | API 31+、Monet 开关开启 | 关闭或系统过旧：已保存的自定义色；未自定义时使用预设蓝色 |
| 应用内预测性返回进度 | API 34+、启用手势导航、设置开关开启 | 按钮返回、API 26–33 或关闭开关：常规详情退出过渡 |
| 系统返回桌面预测动画 | 系统、启动器与手势设置支持；Manifest 已启用 | 系统普通返回 |

`miuix-blur` **属于该仓库本体**，是可独立依赖的模块，并非另一个同名插件。其 v0.9.3 Android 模块声明 `minSdk = 33`，原因是高斯模糊、噪声及高光使用 RuntimeShader。参见[模块构建配置](https://github.com/compose-miuix-ui/miuix/blob/v0.9.3/miuix-blur/build.gradle.kts)与[官方 Blur 指南](https://github.com/compose-miuix-ui/miuix/blob/v0.9.3/docs/guide/blur.md)。

为了支持 API 26，Manifest **仅对 `top.yukonga.miuix.kmp.blur`** 声明 `tools:overrideLibrary`。所有该模块调用隔离在 `ui/GlassLayer.kt`，入口带 `@RequiresApi(33)`，外层先检查 SDK 与开关。旧系统不会创建任何 backdrop、highlight 或 shader 对象。这个覆盖是显式的集成选择，不会把 RuntimeShader 变成低版本可用的 API；Android 10 的安装启动与导航测试已验证隔离路径。

玻璃实现由 `rememberLayerBackdrop` → 页面内容的 `Modifier.layerBackdrop` → 底栏的 `Modifier.textureBlur` 组成。捕获层与底栏是兄弟节点，防止捕获底栏自身；捕获节点内绘制不透明页面底色，避免卡片间隙产生透明颜色伪影。半径为 24dp，模糊输出上用标准 Compose 绘制半透明色层、渐变高光边线及阴影，保持前景文字图标清晰。材质叠层使用常规绘制，避开运行验证中出现黑底的复合 shader 路径；背景采样和高斯模糊仍由 Miuix Blur 实际执行。选中胶囊独立绘制。**不是对底栏本身调用 `Modifier.blur()`，也不是仅使用透明背景。**

外观用 Miuix `ThemeController` 实现。启用 Monet 且 API 31+ 时选择 `MonetSystem` / `MonetLight` / `MonetDark`，`keyColor` 保持空值，让上游读取系统配色；关闭 Monet 后，有自定义色时用该种子生成配色，没有则使用预设主题。主题、Monet、模糊、预测性返回开关、专辑布局及自定义颜色通过 DataStore 持久化，首次读取完成后才显示应用，避免主题闪变。读写按字段更新；写入失败有可关闭的错误提示。

预测性返回使用 AndroidX `PredictiveBackHandler` 收集 `BackEventCompat.progress`，详情随进度缩放、位移、圆角变化，下面保留来源页面。成功结束才清空详情 ID并接续收回；取消手势时捕获 `CancellationException`，动画恢复至详情初始状态。低版本用 `BackHandler`。[Android 官方说明](https://developer.android.com/develop/ui/compose/system/predictive-back)。Android 13 已提供系统返回框架，但连续的应用内手势进度从 Android 14 起使用；Android 13/14 的系统预测动画还可能受开发者选项影响。

## 工程结构

```text
JayChou/
├─ build.gradle.kts                 # AGP、内置 Kotlin 版本对齐
├─ settings.gradle.kts              # Google Maven / Maven Central
├─ gradle/libs.versions.toml         # 固定依赖版本
├─ gradle/wrapper/                  # 可直接运行的 Wrapper
├─ app/build.gradle.kts
├─ app/src/main/AndroidManifest.xml
├─ app/src/main/assets/
│  ├─ albums.json                   # 16 张专辑，逐专辑来源 URL、原始时间戳、完整曲序
│  ├─ extended-albums.json          # 33 个扩展发行版本，含碟片号与数字版来源
│  ├─ covers/                       # 共 49 张封面的离线副本
│  └─ jay-portrait.jpg              # 官方人物宣传照离线副本
├─ app/src/main/java/love/yu/jaychou/
│  ├─ MainActivity.kt               # 入口、读取设置、edge-to-edge、系统栏
│  ├─ data/Album.kt                 # 专辑/曲目模型、目录解析与简介
│  ├─ data/LyricsRepository.kt       # 在线候选、私有目录导入与 LRC 解析
│  ├─ data/SettingsRepository.kt    # DataStore 设置持久化
│  ├─ navigation/ArchiveApp.kt      # 一级页面、详情导航及预测返回
│  └─ ui/
│     ├─ ArchiveTheme.kt            # Miuix 深浅色、跟随系统、Monet
│     ├─ HomeScreen.kt
│     ├─ AlbumScreens.kt            # 横向列表、双列网格、大卡片与详情
│     ├─ AlbumMotion.kt             # 卡片原位图层与展开预览
│     ├─ ArchiveScaffold.kt         # 全页面顶栏、模糊采样与顶部安全区
│     ├─ LyricsScreen.kt            # 版本选择、静态阅读、导入与失败重试
│     ├─ ReleaseCategorySheet.kt    # Miuix 多选发行分类面板
│     ├─ CustomColorSheet.kt        # Miuix 颜色选择器
│     ├─ DraggableNavigationBar.kt  # 胶囊拖动、伸缩、高光与选页
│     ├─ AboutScreen.kt             # 应用身份与项目资料
│     ├─ SettingsScreen.kt          # 独立设置页、布局选择与外观/手势
│     ├─ PreferenceComponents.kt    # 共用原生 Miuix 设置组件
│     ├─ Components.kt              # 图片加载状态、来源链接、自定义矢量图标
│     └─ GlassLayer.kt              # API 33+ 真正的背景采样模糊
├─ app/src/test/                    # 专辑完整性、原始曲序、日期转换验证
├─ app/src/androidTest/             # 导航、重建、设置与预测返回验收
├─ scripts/fetch_catalog.py         # 官方来源抓取辅助，不参与 Gradle 构建
├─ scripts/fetch_extended_catalog.py # 扩展数字版目录抓取与曲序核对
├─ SOURCES.md
└─ ACCEPTANCE.md
```

滚动状态位于导航宿主，使用可保存的 `rememberLazyListState` / `rememberLazyGridState`，不随一级页面的内容卸载而丢失。详情独立保存滚动位置；返回显示来源页面原位置。一级页面由 `rememberPagerState` 保存，详情 ID 与卡片相对矩形用 `rememberSaveable` 保存，支持 Activity 重建。专辑 key 为官方条目 ID，曲目 key 为专辑 ID + 原始序号。

页面宿主仅在横向使用 `safeDrawing`；顶栏包含并消费状态栏/顶部系统 inset，内容起始留白由实际顶栏高度计算，底栏距两边 28dp，距系统导航区上边缘 12dp；宽屏底栏最大 460dp。页面底部留白按**实际测得的底栏高度 + 系统导航区 + 间距**计算，支持较大字号。底栏上方滚动内容持续被捕获，最后一项可完整滚到导航栏之上。

## 界面与资料约定

- 独立设置页按此前参考图的 Miuix 系统设置风格组织：浅灰底、12dp 卡片外边距、低强调分组标题、24dp 平滑圆角卡片、主题下拉选择及整行开关。使用原生 Preference 行，而不是用自绘行模仿开关组件。保留居中应用身份与资料说明；没有复制参考应用的作者、版本或更新链接。
- 自定义部分：悬浮胶囊外壳、材质色层与高光边线、可拖动选中项、矢量导航图标、首页主视觉、专辑的三种目录布局、卡片容器展开及预测返回变换。主题、卡片、文本、标题、下拉选择、底部分类面板、复选框、颜色选择器和设置开关使用 Miuix；真正的背景采样模糊使用上游 Blur 模块。
- 网络图片通过 Coil 加载，显示加载状态；失败时显示随包素材与“离线图片 · 点此重试”。文字、完整目录、封面与人物照都能离线使用。图片说明标注来源，详情和关于页提供可点击来源链接。
- 专辑、资料、封面均见 [SOURCES.md](SOURCES.md)。录音室目录沿用工作区已有的简体显示名称；新增目录保留 Apple Music 条目字形；介绍为简短事实性概述。日期存在来源分歧时在详情明确说明，不默默拼接不同版本曲目。
- 关于页保留已有 ShuangYu 作者、头像与 Beta 标识；“本项目开源代码”当前仅为 GitHub 首页，发布前仍需填写本应用的实际仓库地址。

验收步骤、实际运行结果和仍需真机确认的内容见 [ACCEPTANCE.md](ACCEPTANCE.md)。
