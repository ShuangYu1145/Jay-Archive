<div align="center">

<img src="docs/banner.png" alt="杰作 · Jay Archive" width="100%" />

<br />

**一本关于周杰伦与音乐作品的口袋档案** · 原生 Android 应用

[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/about/versions/oreo)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.11.2-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Miuix](https://img.shields.io/badge/Miuix%20UI-0.9.3-FA5AA9?style=flat-square)](https://github.com/compose-miuix-ui/miuix/tree/v0.9.3)
![Version](https://img.shields.io/badge/version-0.2%20Beta-2E7CFB?style=flat-square)
[![License](https://img.shields.io/github/license/ShuangYu1145/Jay-Archive?style=flat-square&color=2E7CFB)](LICENSE)
[![Stars](https://img.shields.io/github/stars/ShuangYu1145/Jay-Archive?style=flat-square&color=FA5AA9)](https://github.com/ShuangYu1145/Jay-Archive/stargazers)
[![Last commit](https://img.shields.io/github/last-commit/ShuangYu1145/Jay-Archive?style=flat-square&color=7F52FF)](https://github.com/ShuangYu1145/Jay-Archive/commits)

</div>

以周杰伦及其录音室专辑为主题的原生 Android 应用。**Kotlin + Jetpack Compose + Miuix** 重写界面，收录 16 张录音室专辑、175 首曲目的完整目录，另附 26 个已核实的扩展发行版本。三种目录布局、多选分类、歌词阅读、玻璃材质与卡片原位展开动画均已可用。

> 本项目是**离线可读的档案应用**，不提供音乐播放或下载。

## 📱 界面预览

<table align="center">
  <tr>
    <td align="center" width="25%">
      <img src="docs/screenshots/01-home.jpg" width="210" alt="首页" />
      <br />
      <sub><b>首页</b> · 作品概览</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/02-albums-list.jpg" width="210" alt="专辑 · 横向列表" />
      <br />
      <sub><b>专辑</b> · 横向列表</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/03-albums-grid.jpg" width="210" alt="专辑 · 双列网格" />
      <br />
      <sub><b>专辑</b> · 双列网格</sub>
    </td>
    <td align="center" width="25%">
      <img src="docs/screenshots/04-about.jpg" width="210" alt="关于" />
      <br />
      <sub><b>关于</b> · 项目与来源</sub>
    </td>
  </tr>
</table>

## ✨ 功能特性

<table>
  <tr>
    <td width="50%" valign="top">
      <b>📚 完整录音室目录</b><br />
      16 张录音室专辑、175 首曲目，逐专辑保存发行日期、完整曲序、封面与官方来源链接。
    </td>
    <td width="50%" valign="top">
      <b>🗃 扩展发行档案</b><br />
      另有 26 个已核实的演唱会、EP、单曲与原声发行（229 首曲目），版本差异在详情中逐条注明。
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <b>🪟 三种目录布局</b><br />
      横向列表 / 双列网格 / 大封面卡片，随时切换；每种「分类 × 布局」分别记忆滚动位置。
    </td>
    <td width="50%" valign="top">
      <b>🔍 多选分类筛选</b><br />
      原生 Miuix 底部面板，按录音室、演唱会、EP、单曲、原声自由组合，至少保留一项，选择持久保存。
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <b>🎧 歌词阅读</b><br />
      按需调用落月 API 的 QQ 音乐歌词接口，自动匹配周杰伦版本；单卡片呈现原文与翻译，支持离线导入 LRC / TXT。
    </td>
    <td width="50%" valign="top">
      <b>🎨 主题与配色</b><br />
      深浅色跟随系统，支持 Monet 动态取色（Android 12+）与自定义种子色，自动生成 Miuix 调色板。
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <b>🎤 巡演现场</b><br />
      最近场次倒计时、巡演日历按国家/城市/状态筛选；详情包含当地时间与时区、场馆地址、地图导航、票务入口与来源。
    </td>
    <td width="50%" valign="top">
      <b>🔄 官方行程刷新</b><br />
      启动时向 JVR 官方行程核对最新场次，离线时使用核对快照；不虚构票价、开票时间或售票状态。
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <b>🫧 真实采样模糊</b><br />
      Miuix Blur 直接采样顶栏与悬浮底栏后方的滚动内容，低版本或关闭开关时自动降级。
    </td>
    <td width="50%" valign="top">
      <b>↩️ 预测性返回</b><br />
      Android 14+ 手势逐帧预览详情关闭过程，取消即恢复当前详情，支持 Activity 重建。
    </td>
  </tr>
</table>

## 🫧 视觉与交互

- **悬浮胶囊底栏** —— 五个一级页面共用 `HorizontalPager`。点按使用弹簧动画切换，也可直接横滑；沿底栏左右拖动时，选中胶囊跟随手指并轻微伸缩，进度同步给 Pager，松手吸附到最近页面。
- **卡片原位展开** —— 点击专辑时记录封面在窗口中的真实矩形与 `GraphicsLayer`，详情面板从该矩形弹簧展开，原卡片图层渐变为详情，底层页面轻微后退、底栏淡出；返回时反向收回，**不在主线程截取 Bitmap**。
- **材质链路** —— `rememberLayerBackdrop` → 页面内容的 `layerBackdrop` → 底栏的 `textureBlur`。捕获层与底栏互为兄弟节点，避免把底栏自身拍进背景；半径 24dp，模糊输出上用常规绘制叠加半透明色层、渐变高光边线与阴影，保证前景文字清晰。
- **系统 inset** —— 页面宿主只处理横向安全区并裁切窗口边界，顶部系统 inset 由顶栏单独消费；底栏距两侧 28dp、距系统导航区上边缘 12dp，宽屏最大 460dp。
- **实测动效** —— Compose 时钟驱动，遵循系统动画时长缩放；真正的背景模糊限 API 33+，页面与卡片动画在 API 26+ 可用。拖动胶囊与卡片展开是本项目的自定义 Compose 动效，视觉参考 iOS 式可拖动胶囊与 Android 桌面应用展开，不声称调用系统专有动画。

### 能力与降级

| 能力 | 生效条件 | 降级方式 |
| :-- | :-- | :-- |
| Miuix 背景模糊 | API 33+、模糊开关开启、硬件加速正常 | API 26–32 或关闭开关：实色顶栏与胶囊，保留高光、阴影与选中态 |
| Monet 系统动态配色 | API 31+、Monet 开关开启 | 关闭或系统过旧：已保存的自定义色；未自定义时使用预设蓝色 |
| 应用内预测性返回进度 | API 34+、手势导航、设置开关开启 | 按钮返回或关闭开关：常规详情退出过渡 |
| 系统返回桌面预测动画 | 系统、启动器与手势设置支持 | 系统普通返回 |

`miuix-blur` 是 Miuix 仓库本体的独立模块，v0.9.3 Android 模块声明 `minSdk = 33`（高斯模糊、噪声与高光使用 `RuntimeShader`）。为支持 API 26，Manifest **仅对 `top.yukonga.miuix.kmp.blur`** 声明 `tools:overrideLibrary`，所有调用隔离在 `ui/GlassLayer.kt`，入口带 `@RequiresApi(33)` 且外层先检查 SDK 与开关——旧系统不会创建任何 backdrop、highlight 或 shader 对象。

## 🚀 快速开始

**环境要求**

- Android Studio（支持 AGP **9.4.1**）
- **JDK 25** 作为 Gradle Daemon JVM（`gradle/gradle-daemon-jvm.properties` 已固定版本，可自动查找或下载）
- **Android SDK Platform 37.0** 与 **Build Tools 36.0.0**
- 设备或模拟器 **Android 8.0 / API 26** 及以上

**构建**

```bash
# 1. 在 local.properties 中配置 sdk.dir（该文件不属于源码）
# 2. 同步后运行 app 模块

./gradlew :app:assembleRelease                                 # 打包 APK
./gradlew :app:assembleDebug                                   # 打包 Debug APK
./gradlew :app:testDebugUnitTest :app:lintDebug                # 单元测试与静态检查
./gradlew :app:connectedDebugAndroidTest                       # 设备端验收测试
```

Windows 下把 `./gradlew` 换成 `.\gradlew.bat`。首次同步需访问 Google Maven、Maven Central 与 Gradle 分发站点。

产物位于 `app/build/outputs/apk/debug/app-debug.apk`。这是可安装的 Debug 签名包，正式发布签名由项目所有者配置。

## 🧱 技术栈

<details>
<summary><b>固定版本与配置</b></summary>

<br />

| 项目 | 固定版本 / 配置 |
| :-- | :-- |
| Gradle Wrapper | 9.6.0，保留分发 SHA-256 校验 |
| Android Gradle Plugin | 9.4.1，使用内置 Kotlin 支持 |
| Kotlin / Compose Compiler | 2.4.20 / 2.4.20 |
| AndroidX Compose UI、Foundation、Animation | 1.11.2 |
| Miuix UI / Blur / Preference | 0.9.3 / 0.9.3 / 0.9.3 |
| Activity Compose | 1.12.4 |
| Lifecycle Compose | 2.10.0 |
| Preferences DataStore | 1.2.1 |
| Coil Compose | 2.7.0 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 26 |
| Build Tools / Java 字节码 | 36.0.0 / 17 |

为复现构建，固定采用已核实并实际编译的 Miuix **v0.9.3**，而不是跟随 `main`。上游该标签使用 Kotlin 2.4.0 与 Compose Multiplatform 1.11.1；本工程使用 AndroidX Compose 1.11.2 与更新的同系列 Kotlin 2.4.20，编译时已验证兼容。根 `build.gradle.kts` 显式提升 AGP 内置 KGP 版本以与 Compose Compiler 对齐，不再应用 `org.jetbrains.kotlin.android` 插件。

```kotlin
implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.3")
implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.3")
implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.3")
```

版本集中在 `gradle/libs.versions.toml`。实际使用 Miuix 的 `MiuixTheme`、`ThemeController`、`ColorSchemeMode`、`Scaffold`、`TopAppBar`、`SmallTopAppBar`、`MiuixScrollBehavior`、`Card`、`SmallTitle`、`BasicComponent`、`Text`、`Button`，以及独立 Preference 模块的 `OverlayDropdownPreference`、`SwitchPreference`、`ArrowPreference`，API 均与 v0.9.3 发布源码对照。

</details>

## 🗂 工程结构

<details>
<summary><b>目录与关键文件</b></summary>

<br />

```text
JayChou/
├─ build.gradle.kts                   # AGP、内置 Kotlin 版本对齐
├─ settings.gradle.kts                # Google Maven / Maven Central
├─ gradle/libs.versions.toml          # 固定依赖版本
├─ gradle/wrapper/                    # 可直接运行的 Wrapper
├─ app/build.gradle.kts
├─ app/src/main/AndroidManifest.xml
├─ app/src/main/assets/
│  ├─ albums.json                     # 16 张录音室专辑，来源 URL、原始时间戳、完整曲序
│  ├─ extended-albums.json            # 26 个扩展发行版本，含碟片号与数字版来源
│  └─ jay-portrait.jpg                # 官方人物宣传照
├─ app/src/main/java/love/yu/jaychou/
│  ├─ MainActivity.kt                 # 入口、读取设置、edge-to-edge、系统栏
│  ├─ data/Album.kt                   # 专辑/曲目模型、目录解析与简介
│  ├─ data/LyricsRepository.kt        # 在线候选、私有目录导入与 LRC 解析
│  ├─ data/SettingsRepository.kt      # DataStore 设置持久化
│  ├─ navigation/ArchiveApp.kt        # 一级页面、详情导航及预测返回
│  └─ ui/
│     ├─ ArchiveTheme.kt              # Miuix 深浅色、跟随系统、Monet
│     ├─ HomeScreen.kt
│     ├─ AlbumScreens.kt              # 横向列表、双列网格、大卡片与详情
│     ├─ AlbumMotion.kt               # 卡片原位图层与展开预览
│     ├─ ArchiveScaffold.kt           # 全页面顶栏、模糊采样与顶部安全区
│     ├─ LyricsScreen.kt              # 版本选择、静态阅读、导入与失败重试
│     ├─ ReleaseCategorySheet.kt      # Miuix 多选发行分类面板
│     ├─ CustomColorSheet.kt          # Miuix 颜色选择器
│     ├─ DraggableNavigationBar.kt    # 胶囊拖动、伸缩、高光与选页
│     ├─ AboutScreen.kt               # 应用身份与项目资料
│     ├─ SettingsScreen.kt            # 独立设置页、布局选择与外观/手势
│     ├─ PreferenceComponents.kt      # 共用原生 Miuix 设置组件
│     ├─ Components.kt                # 图片加载状态、来源链接、矢量图标
│     └─ GlassLayer.kt                # API 33+ 真正的背景采样模糊
├─ app/src/test/                      # 专辑完整性、原始曲序、日期转换验证
├─ app/src/androidTest/               # 导航、重建、设置与预测返回验收
├─ docs/                              # README 横幅与界面截图
├─ scripts/fetch_catalog.py           # 官方来源抓取辅助，不参与 Gradle 构建
├─ scripts/fetch_extended_catalog.py  # 扩展数字版目录抓取与曲序核对
└─ scripts/package_delivery.py        # 源码交付打包辅助
```

滚动状态位于导航宿主，使用可保存的 `rememberLazyListState` / `rememberLazyGridState`，不随一级页面卸载而丢失。专辑 key 为官方条目 ID，曲目 key 为专辑 ID + 原始序号，因此重建、预测返回与分类切换后都能回到原位置。

</details>

## 📚 资料与说明

- 目录、封面与日期取自官方数字版元数据（Apple Music 等），逐条保存 `sourceUrl` 与原始时间戳，核对日期为 **2026-10-17**。日期存在来源分歧时在详情中明确说明，不默默拼接不同版本的曲目。
- 封面与人物照通过 Coil 联网加载，显示加载状态；失败时显示占位素材与「离线图片 · 点此重试」。文字与完整目录本身完全离线可用。
- 在线歌词来自[落月 API](https://doc.vkeys.cn/v2/%E9%9F%B3%E4%B9%90%E6%A8%A1%E5%9D%97/QQ%E9%9F%B3%E4%B9%90/8-lyric.html)的 QQ 音乐歌词接口：先按歌名与专辑匹配周杰伦版本，再读取原文与翻译；匹配会区分现场与录音室版本，请自行核对。**未预装任何商业歌曲歌词**，也不承诺所有歌曲都有在线歌词。
- 演唱会日期与场馆来自 [JVR 官方行程](https://www.jvrmusic.com/activity/list/1150822038412333056)、[维基百科](https://zh.wikipedia.org/wiki/%E5%98%89%E5%B9%B4%E8%8F%AFII%E4%B8%96%E7%95%8C%E5%B7%A1%E8%BF%B4%E6%BC%94%E5%94%B1%E6%9C%83)及各站票务公告；应用启动时会核对 JVR 的当月官方行程，网络不可用时使用随包快照。票价与售票状态仅在来源明确公布时显示，**最终以主办方和票务平台为准**。
- 本项目为**非官方粉丝档案**，与周杰伦本人、杰威尔音乐及任何唱片公司均无关联。所有专辑封面、人物照片、歌词与商标版权归各自权利人所有，仅作资料展示用途。

## 📝 更新日志

<details>
<summary><b>0.2 Beta</b> —— 界面重做与档案扩展</summary>

<br />

- 五页悬浮导航：首页、专辑、演唱会、设置、关于。演唱会页已接入最近场次、巡演日历与演出详情，不再留空。
- 专辑页「分类」使用原生 Miuix `WindowBottomSheet` 与 `CheckboxPreference`，支持多选，取消不生效、确定才应用，至少保留一项；分类及滚动状态可在 Activity 重建后恢复。
- 保留横向列表 / 双列网格 / 大封面卡片三种布局，通过设置选择并持久化；统一顶部说明与大标题的 24dp 起点。
- 修复卡片展开时椭圆一帧：投影圆角按 X/Y 各自缩放补偿，横向卡片与窄曲目行也维持屏幕上的圆角矩形；歌词页从曲目行按同一动画展开。
- 所有一级页、专辑详情与歌词页统一顶栏：用 Miuix Blur 采样实际滚动内容，顶栏与状态栏区域为高不透明度的模糊材质；API 26–32 或关闭模糊时实色降级。
- 设置新增「自定义颜色」：原生 Miuix `ColorPicker` 任选种子色，另有快捷色与恢复预设；Monet 开启且系统支持时优先使用壁纸调色板。
- 修复专辑详情缓慢起手拖动时偶尔不能滚动：移除详情父容器对 Final 阶段触摸事件的无条件消费，改为详情后方的独立全屏触摸隔离层，展开与收回动画期间也覆盖卡片之外区域。参考 [Compose 事件命中与传播规则](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures#event-dispatching)。
- 歌词获取改为落月 API 的 QQ 音乐搜索 + 歌词接口，按标题、歌手、专辑及 Live 状态评分匹配；歌词页重做为单一 Miuix 卡片，支持原文/双语切换、加载与失败状态，并保留本地导入与删除。
- 演唱会页新增最近场次倒计时、按国家/城市/状态筛选的巡演日历，以及带当地时间、时区、场馆地址、地图导航、票务与来源的演出详情；启动时自动核对 JVR 官方行程，网络不可用时回退到核对快照。
- 新增图标启动动画：系统启动窗口与 Compose 启动画面使用同一枚圆形应用图标，等待设置与专辑目录就绪后再淡入主界面，避免首帧空白或页面突然卡顿。

</details>

## ⚖️ 许可与致谢

代码以 [Apache License 2.0](LICENSE) 开源。界面组件来自 [Miuix UI](https://github.com/compose-miuix-ui/miuix)（Apache-2.0），在线歌词服务来自 [落月 API](https://doc.vkeys.cn/v2/%E9%9F%B3%E4%B9%90%E6%A8%A1%E5%9D%97/QQ%E9%9F%B3%E4%B9%90/8-lyric.html)（QQ 音乐），构建于 [Jetpack Compose](https://developer.android.com/jetpack/compose) 之上。

<div align="center">

<br />

**如果这份档案对你有帮助，欢迎点一个 ⭐**

<sub>Made with ❤️ for Jay Chou's music · 非官方项目</sub>

</div>
