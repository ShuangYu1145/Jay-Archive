# 多语言维护

应用支持 `zh-Hans`、`zh-Hant`、`en`、`ja`、`ko`，默认跟随系统；没有受支持的系统语言时使用简体中文。

## 界面文案

编辑 `scripts/localization/ui.json`，然后运行：

```powershell
python scripts/localization/generate.py
```

脚本生成各语言的 Android 字符串资源，以及原始资料文案的查找表。不要直接编辑生成文件。参数占位符必须在所有语言中保持一致。带数量的条目使用 `countArg` 指定数量参数位置，`enOne` 保存英文单数形式。

Compose 页面与事件回调通过 `AppText` 读取当前配置下的 Android 资源。`ArchiveTheme` 刷新资源配置；固定枚举值、分类键和筛选状态不使用翻译后的文字。

语言选择使用 AndroidX AppCompat 的应用语言机制。Android 13 及以上与系统的应用语言设置互通，旧系统通过 AppCompat 自动保存语言。不要再在 DataStore 中保存第二份语言偏好。

## 专辑资料

基础目录仍在 `app/src/main/assets/albums.json` 与 `extended-albums.json`。ID、发行日期、曲序、分类、图片、链接等事实字段只在基础目录维护。

各语言的显示内容保存在 `app/src/main/assets/i18n/<语言>.json`：

```json
{
  "albums": {
    "专辑ID": {
      "title": "显示名称",
      "summary": "简介",
      "dateNote": "版本和日期说明",
      "sourceLabel": "来源名称",
      "aliases": ["可选的搜索别名"],
      "tracks": {
        "1": { "title": "曲名", "aliases": ["可选别名"] }
      }
    }
  }
}
```

曲目键是整个发行版本的 `Track.number`，不是每张碟片从头开始的曲序。翻译缺失、空白或为 null 时，按字段回退到简体中文，再回退到基础目录。新增作品时应补齐五种语言；完整性测试会检查遗漏。

使用已提供的官方英文名；没有可靠译名的作品保留原名。繁体作品名需要人工校对，不能直接采用简繁转换结果，例如「髮如雪」「驚嘆號」「菊花台」。其他常见写法可以放入 `aliases`。

`originalTitle` 保留基础目录的原始名称，在线歌词匹配始终使用它。搜索同时索引原名、已有英文名及所有翻译包中的名称和别名。歌词正文及服务自带的翻译不随界面语言自动翻译。

## 验证

`AlbumLocalizationTest` 检查五语覆盖、逐字段回退、资料不变、语言映射与跨语言搜索。`AppLanguageTest` 在实际 Activity 中验证五语切换、详情页恢复、数量单复数，并在模拟器的应用外部文件目录 `language-checks` 保存截图。

全量单元测试另有既存的 `CatalogTest` 数据断言失败，分别涉及封面资源与扩展目录分类数量；本次未修改这些基础资料。
