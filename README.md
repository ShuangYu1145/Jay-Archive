<div align="center">

<img src="docs/banner.png" alt="杰作 · Jay Archive" width="100%" />

**一本关于周杰伦与音乐作品的口袋档案**

按年代翻阅作品，让熟悉的旋律有迹可循。

![Version](https://img.shields.io/badge/version-0.3%20Beta-2E7CFB?style=flat-square)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/about/versions/oreo)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.11.2-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Miuix](https://img.shields.io/badge/Miuix-0.9.3-FA5AA9?style=flat-square)](https://github.com/compose-miuix-ui/miuix/tree/v0.9.3)
[![License](https://img.shields.io/badge/license-Apache--2.0-2E7CFB?style=flat-square)](LICENSE)

[界面预览](#界面预览) · [启动动画](#启动动画) · [功能一览](#功能一览) · [构建与运行](#构建与运行) · [更新日志](#更新日志)

</div>

杰作（Jay Archive）是一款原生 Android 粉丝档案应用，以 Kotlin 与 Jetpack Compose 构建，提供 **Miuix / Material Design 3** 两套界面风格。从专辑封面、完整曲序到歌词阅读和演唱会行程，把作品与现场放进同一本口袋档案。

| 录音室专辑 | 录音室曲目 | 扩展发行版本 | 界面语言 |
| :--: | :--: | :--: | :--: |
| **16 张** | **175 首** | **26 个 · 229 首曲目** | **5 种** |

> 专辑目录可离线浏览。在线图片、歌词获取与官方行程需要网络；本应用不提供音乐播放或下载。

## 界面预览

2026 年 10 月 24 日实机截图，展示 Miuix 风格下的首页、专辑目录与关于页。点击可查看原图。

<table align="center">
  <tr>
    <td align="center" width="33%">
      <a href="docs/screenshots/2026-10-24-home.jpg"><img src="docs/screenshots/2026-10-24-home.jpg" width="260" alt="首页：个人资料、周杰伦介绍与作品统计" /></a>
    </td>
    <td align="center" width="33%">
      <a href="docs/screenshots/2026-10-24-albums.jpg"><img src="docs/screenshots/2026-10-24-albums.jpg" width="260" alt="介绍页：专辑与演唱会分区、双列专辑网格、搜索与筛选" /></a>
    </td>
    <td align="center" width="33%">
      <a href="docs/screenshots/2026-10-24-about.jpg"><img src="docs/screenshots/2026-10-24-about.jpg" width="260" alt="关于页：渐变背景、应用图标、Beta 0.3 版本与项目来源" /></a>
    </td>
  </tr>
  <tr>
    <td align="center"><b>首页</b><br /><sub>人物介绍 · 作品概览 · 个人资料</sub></td>
    <td align="center"><b>介绍</b><br /><sub>专辑目录 · 演唱会 · 搜索筛选</sub></td>
    <td align="center"><b>关于</b><br /><sub>应用信息 · 开发者 · 项目与来源</sub></td>
  </tr>
</table>

## 启动动画

「Jay」从第一笔写到收笔线，再淡入首页。手写笔迹跟随当前主题色，深浅主题均可呈现；已完成的动画不会因 Activity 重建而重新播放。

<div align="center">
  <a href="docs/media/jay-launch.mp4"><img src="docs/media/jay-launch.gif" width="260" alt="启动动画录屏：逐笔书写 Jay，绘制收笔线后淡入首页" /></a>
  <p><sub>实机录屏 · 约 2.74 秒 · GIF 循环预览</sub></p>
</div>

[查看 MP4 原始录屏](docs/media/jay-launch.mp4)

## 功能一览

### 作品与现场

- **完整专辑目录**：收录 16 张录音室专辑及 26 个演唱会、EP、单曲与原声等扩展发行版本，保留发行日期、曲序、封面、版本说明和来源链接。
- **多种浏览方式**：横向列表、双列网格、大封面卡片三种布局；支持多选发行分类与跨语言名称、别名搜索，记忆列表位置及筛选状态。
- **主打曲目标记**：在专辑曲目中标记已核对的主打歌，标记依据见[主打曲目说明](docs/lead-tracks.md)。
- **歌词阅读**：按歌曲与专辑匹配在线歌词，支持原文 / 双语显示，也可导入本地 LRC、TXT 文件。
- **演唱会行程**：「介绍」内切换专辑与演唱会，联网读取 JVR 官方公布的行程，提供最近场次倒计时、日历与国家 / 城市 / 状态筛选。连接失败可重试，未公布的信息保留待确认状态。

### 外观与体验

- **两套原生界面**：在设置中切换 Miuix / Material Design 3，深浅色、动态配色、自定义颜色与专辑布局按风格独立保存。详见[界面风格说明](docs/ui-styles.md)。
- **四个底部入口**：首页、介绍、设置、关于；专辑与演唱会集中在「介绍」，分区切换后保留各自的浏览状态。
- **可选导航样式**：Miuix 支持普通底栏、悬浮底栏及液态玻璃效果；Material Design 3 使用原生 Material 导航栏。
- **连续的页面动效**：手写启动动画、可拖动胶囊、专辑卡片原位展开与详情返回过渡。
- **个人资料**：点击首页右上角修改名字和头像；头像保存在应用私有目录，重启后仍可使用。
- **五种语言**：简体中文、繁体中文、英语、日语、韩语，默认跟随系统，也可单独设置。详见[多语言维护说明](docs/localization.md)。

### 系统适配

| 能力 | 生效条件 | 其他情况 |
| :-- | :-- | :-- |
| Miuix 背景模糊与液态玻璃 | Android 13 / API 33+，启用对应设置 | 使用普通表面与导航样式 |
| Monet 动态配色 | Android 12 / API 31+，启用动态配色 | 使用自定义颜色或预设配色 |
| 应用内预测性返回 | Android 14 / API 34+，手势导航，启用对应设置 | 使用常规返回过渡 |
| 离线目录与本地歌词 | Android 8.0 / API 26+ | 无需联网 |

<details>
<summary>动效与兼容实现</summary>

- 卡片展开复用封面的位置与图层，不在主线程截取 Bitmap；滚动与选择状态通过可保存状态恢复。
- 启动时并行读取设置和专辑目录，首帧就绪后移除系统图标页，随后播放手写动画；缺失绘制信号时有超时回退。
- Miuix 的模糊与液态玻璃调用隔离在 `ui/effects/`，仅在 API 33+ 且设置允许时创建。Material 风格不创建 Miuix 的背景采样或着色器。
- Manifest 仅对 `top.yukonga.miuix.kmp.blur` 声明 `tools:overrideLibrary`，低版本设备通过 SDK 检查走降级路径。

</details>

## 构建与运行

### 环境

- 支持 **AGP 9.4.1** 的 Android Studio。
- **JDK 25**：Gradle Daemon 版本由 `gradle/gradle-daemon-jvm.properties` 固定，可自动查找或下载；应用 Java 字节码目标为 17。
- **Android SDK Platform 37.0**、**Build Tools 36.0.0**。
- Android 8.0 / API 26 及以上的设备或模拟器。

在 `local.properties` 中配置本机 `sdk.dir`，同步项目后运行 `app` 模块。首次构建需要访问 Google Maven、Maven Central 与 Gradle 分发站点。

```bash
./gradlew :app:assembleDebug                    # 构建调试 APK
./gradlew :app:assembleRelease                  # 构建发布 APK
./gradlew :app:testDebugUnitTest :app:lintDebug  # 单元测试与静态检查
./gradlew :app:connectedDebugAndroidTest        # 设备端测试
```

Windows 下将 `./gradlew` 替换为 `.\gradlew.bat`。调试安装包位于 `app/build/outputs/apk/debug/app-debug.apk`；正式发布签名需自行配置。

### 技术栈

| 项目 | 当前配置 |
| :-- | :-- |
| Gradle / Android Gradle Plugin | 9.6.0 / 9.4.1 |
| Kotlin / Compose Compiler | 2.4.20 / 2.4.20 |
| Compose UI、Foundation、Animation | 1.11.2 |
| Miuix UI、Blur、Preference | 0.9.3 |
| Material Design 3 | 1.4.0 |
| Activity / Lifecycle Compose | 1.12.4 / 2.10.0 |
| Preferences DataStore / Coil | 1.2.1 / 2.7.0 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 26 |

依赖版本集中在 [`gradle/libs.versions.toml`](gradle/libs.versions.toml)。工程采用 AGP 内置 Kotlin 支持，根构建脚本将 Kotlin 版本与 Compose Compiler 对齐。

<details>
<summary>工程结构</summary>

```text
app/src/main/
├─ assets/                      # 专辑目录、封面与五语资料
├─ java/love/yu/jaychou/
│  ├─ MainActivity.kt           # 启动、首帧准备与系统栏
│  ├─ data/                     # 专辑、歌词、行程与设置
│  ├─ navigation/ArchiveApp.kt  # 页面导航、详情与预测返回
│  └─ ui/
│     ├─ screens/               # 首页、介绍、专辑、演唱会、歌词、设置、关于
│     ├─ designsystem/          # Miuix / Material 3 控件适配
│     ├─ components/            # 跨页面组件
│     ├─ navigation/            # 普通、悬浮与可拖动底栏
│     ├─ animation/             # Jay 手写启动与卡片展开
│     ├─ effects/               # 背景采样、模糊与液态玻璃
│     └─ theme/                 # 两套主题与配色
└─ res/                        # 界面资源与多语言文案

app/src/test/                  # 目录、曲序、语言与歌词等单元测试
app/src/androidTest/           # 导航、启动、设置、语言与状态恢复测试
docs/                          # 截图、录屏与维护说明
scripts/                       # 资料抓取、本地化与交付辅助脚本
```

</details>

## 资料来源与使用说明

- **专辑资料**：目录随包核对日期为 **2026-10-17**，保留 JVR、Apple Music 等来源链接与版本说明。不同发行版本的曲序、日期分开记录。
- **图片**：通过 Coil 加载在线封面与人物照，并提供随包素材作为加载中和失败时的回退；失败时可点击重试。
- **歌词**：在线获取使用[落月 API 的 QQ 音乐歌词接口](https://doc.vkeys.cn/v2/%E9%9F%B3%E4%B9%90%E6%A8%A1%E5%9D%97/QQ%E9%9F%B3%E4%B9%90/8-lyric.html)。匹配会区分录音室与现场版本；未预装商业歌曲歌词，也不保证每首歌曲均能匹配。
- **演唱会**：以 [JVR 官方行程](https://www.jvrmusic.com/activity/list/1150822038412333056)及主办方、票务平台公告为准。未明确公布的时间、场馆或票务信息不补写推测值。
- **项目定位**：本项目为非官方粉丝档案，与周杰伦本人、杰威尔音乐及任何唱片公司均无关联。封面、人物照片、歌词及商标的版权归各自权利人所有。

## 更新日志

### 0.3 Beta

- 新增 Miuix / Material Design 3 双界面风格，两套外观偏好独立保存。
- 底部导航整理为首页、介绍、设置、关于；专辑与演唱会在「介绍」内切换。
- 新增 Jay 逐笔手写启动动画，优化软件图标阶段的等待与退出衔接。
- 首页支持自定义名字和头像，关于页更新渐变背景与应用信息。
- 支持五种界面语言、跨语言搜索及已核对的主打曲目标记。
- Miuix 底栏支持普通、悬浮与液态玻璃样式，并保留低版本降级。

<details>
<summary>0.2 Beta · 界面重做与档案扩展</summary>

- 使用 Kotlin、Jetpack Compose 与 Miuix 重做界面。
- 扩充专辑与发行版本，提供三种目录布局和多选分类筛选。
- 加入卡片原位展开、背景模糊、自定义配色与预测性返回。
- 提供在线歌词、双语阅读及本地歌词导入。
- 加入演唱会行程、最近场次倒计时与日历筛选。

</details>

## 许可与致谢

代码以 [Apache License 2.0](LICENSE) 开源。感谢 [Miuix UI](https://github.com/compose-miuix-ui/miuix)、[Jetpack Compose](https://developer.android.com/jetpack/compose)、[Material Design 3](https://developer.android.com/develop/ui/compose/designsystems/material3)、[Coil](https://github.com/coil-kt/coil) 及落月 API 提供的组件与服务。第三方素材与组件的许可见项目内对应声明。

<div align="center">

**从 2000 年，听到今天。**

<sub>Made with ❤️ for Jay Chou's music · 非官方项目</sub>

</div>
