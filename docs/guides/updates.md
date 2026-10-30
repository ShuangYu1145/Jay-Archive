# 发布应用更新

应用从 `ShuangYu1145/Jay-Archive` 的 GitHub Releases 读取最近 20 个发布。默认包含 Beta，用户可在设置中关闭测试版更新或自动检查。

## 每次发布

1. 修改 `app/build.gradle.kts`：递增 `versionCode`，设置供用户查看的 `versionName`。
2. 构建并使用原有签名签署安装包，保持包名 `love.yu.jaychou` 不变。
3. 新建 GitHub Release，标签建议使用 `0.5`、`v0.5` 或 `0.5-beta.1`。测试版勾选 Pre-release。
4. 上传 `app-release.apk`；如提供调试版，同时上传 `app-debug.apk`。检查器按当前构建类型选择，避免将 Debug 用户引导至不同签名的 Release 包。
5. 上传构建自动生成的 `app/build/outputs/updates/update.json`，并在 Release 正文填写更新日志。也可运行 `gradlew.bat :app:generateUpdateMetadata` 单独生成。

`update.json` 与这次发布的 APK 必须来自同一次构建，例如：

```json
{
  "versionCode": 6,
  "versionName": "0.5",
  "minSdk": 26
}
```

检查器优先比较 `versionCode`。为兼容已发布的 0.2～0.4，没有 `update.json` 的旧 Release 会按标签的数字版本比较；正式版本高于相同数字的 `-beta.1` 等预发布版本。无法解析的标签必须提供 `update.json`。请勿只替换同名旧版本的 APK 而不递增版本。

## 应用内行为

- 设置页保留手动检查入口；成功、无适配发布、网络失败和限流分别提示。关于页仅在已检测到新版时显示“应用更新”，点击可重新打开更新详情，关闭提示不会隐藏该入口。
- 自动检查在启动进入前台后运行，每 24 小时最多尝试一次；关闭开关会取消待执行的自动检查。手动检查始终可用。
- 自动检查未发现更新或连接失败时保持安静，仅发现新版时提示。提示中也可关闭自动检查。
- 下载按钮使用所选 Release APK 附件的 HTTPS 地址交给浏览器，不在应用中保存 GitHub Token，也不申请安装权限。
- 当前没有兼容渠道、对应构建类型的 APK 或满足系统要求的发布时，不会显示“已是最新版本”。

代码只读取 GitHub，不会自动创建 Release、上传文件或修改仓库。首次上线更新功能后，请按上面的步骤手动发布，后续用户才能收到新版提示。
