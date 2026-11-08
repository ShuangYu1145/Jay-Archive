# 本地 QQ 音乐 API

使用 Node.js 24+ 和 `@yakult-green-tea/qq-music-api` 3.3.6，支持二维码登录、用户资料、搜索、本人歌单、喜欢列表和登录态播放链接。接口兼容杰作现有 QQ 音乐适配器。

```powershell
cd E:\A.Android\JayChou\services\music-api
npm ci
npm run qq:background
```

默认 API 根地址：`http://127.0.0.1:3200`；接口浏览器：`http://127.0.0.1:3200/explorer`。

```powershell
npm start             # 前台启动，Ctrl+C 停止
npm run qq:background # 后台启动，Windows 不弹出终端窗口
npm run qq:status     # 检查服务状态
npm run qq:stop       # 停止本项目启动的服务
```

后台启动会检查已有服务，重复执行不会再启动一个实例。日志在 `.local-qq/`。

## 配置与数据

按需复制 `.env.qq.local.example` 为 `.env.qq.local`，修改 `QQ_API_PORT`。默认仅监听本机；需要同一局域网手机访问时，将 `QQ_API_HOST` 改为 `0.0.0.0`，然后通过电脑的局域网 IP 访问。

首次启动自动生成会话加密密钥，保存在 `.auth-state/qq-local.key`；设备身份和加密会话保存在同一目录下的 `qq-local-device.json`、`qq-local-sessions.json`。保留这些文件才能跨重启保留登录。密钥、会话、日志和本机配置均已加入 Git 忽略规则，启动和状态命令不会输出凭据。

## 登录与调用

1. `GET /login/qr/key?channel=qq` 获取 `data.unikey`，也可选 `channel=wechat`。
2. `GET /login/qr/create?key=...` 获取 `data.qrimg`，使用对应手机 App 扫码。
3. 每两秒调用 `GET /login/qr/check?key=...`。801 等待扫码，802 等待手机确认，803 成功，800 失效。
4. 从成功响应的 `cookie` 中提取 `qqmusic_session` 的值，后续通过 `X-QQ-Session` 请求头发送裸 token。

常用接口：

| 请求 | 用途 |
| --- | --- |
| `GET /login/channels` | 可用登录通道 |
| `GET /login/status` | 账号状态 |
| `GET /user/detail` | 用户资料 |
| `GET /search?keyword=晴天&type=song&page=1&limit=20` | 搜索歌曲 |
| `GET /user/playlist` | 本人歌单 |
| `GET /user/playlist-detail?tid=歌单ID&offset=0&limit=100` | 本人歌单歌曲 |
| `GET /user/liked-songs?offset=0&limit=100` | 喜欢列表 |
| `GET /getMusicPlay/歌曲MID?quality=best` | 当前账号可用播放链接 |
| `GET /getLyric?songmid=歌曲MID` | 歌词 |

该本地服务提供 HTTP。杰作 Android 应用目前只接受 HTTPS 服务地址，手机正式接入可使用此目录已有的 Caddy/Compose 配置，见 [在线音乐接入说明](../../docs/guides/online-music.md)。本机 API 调试可直接使用接口浏览器。

上游协议说明：[QQ Music API](https://github.com/yakult-green-tea/qq-music-api/blob/main/docs/README.md)。

## 浏览器能打开，但 Android 无法连接

先核对 HTTPS 证书与访问域名。樱花穿透的自动 TLS 证书可能为自签名，并只匹配节点域名。当前隧道应使用 `https://frp-oil.com:23745`，而不是节点 IP；应用需安装包含此域名证书信任配置的新构建。浏览器手动放行证书的设置不会传递到应用。详见[穿透 TLS 说明](../../docs/guides/online-music.md#樱花穿透自动-tls)。

也可在新版应用的「在线 → 音乐服务」关闭「验证服务证书」，用于临时调试自签名或域名不匹配的 API。默认开启，关闭时会跳过证书链与主机名校验，仅作用于音乐 API；调试完成后可直接重新开启。
