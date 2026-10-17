package love.yu.jaychou.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import love.yu.jaychou.BuildConfig
import love.yu.jaychou.R
import love.yu.jaychou.data.*
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun AboutScreen(
    state: LazyListState, bottom: Dp,
) {
    val behavior = MiuixScrollBehavior()
    ArchiveScaffold("关于", behavior = behavior) { padding ->
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize().nestedScroll(behavior.nestedScrollConnection).testTag("about-list"),
            contentPadding = PaddingValues(12.dp, padding.calculateTopPadding() + 8.dp, 12.dp, bottom),
        ) {
            item("identity") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(painterResource(R.drawable.feature_graphic), "杰作应用图标：蓝色唱片档案", Modifier.size(80.dp))
                    Text("杰作", style = MiuixTheme.textStyles.title1)
                    Text("JAY ARCHIVE", fontSize = 12.sp, letterSpacing = 3.sp, color = MiuixTheme.colorScheme.primary)
                    QuietText("版本 Beta ${BuildConfig.VERSION_NAME}")
                    Text("一本关于周杰伦与音乐作品的口袋档案。\n按年代翻阅作品，让熟悉的旋律有迹可循。",
                        style = MiuixTheme.textStyles.body2, textAlign = TextAlign.Center,
                        color = MiuixTheme.colorScheme.onBackgroundVariant)
                }
            }
            item("about-title") { PreferenceTitle("关于") }
            item("about") {
                PreferenceCard {
                    BasicComponent(
                        title = "ShuangYu",
                        startAction = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MiuixTheme.colorScheme.surfaceContainerHighest),
                            ) {
                                AsyncImage(
                                    model = "https://q1.qlogo.cn/g?b=qq&nk=1106388837&s=100",
                                    contentDescription = "ShuangYu",
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                )
                            }
                        },
                        endActions = { PreferenceValue("Developer") })
                }
            }

            item("project-title") { PreferenceTitle("项目与来源") }
            item("project") {
                PreferenceCard {
                    LinkRow("Miuix UI", "开源组件库 · Apache-2.0 · 0.9.3", "https://github.com/compose-miuix-ui/miuix")
                    LinkRow("官方专辑资料", "名称、发行信息、曲序与封面", AlbumRepository.albumSource)
                    LinkRow("周杰伦介绍与图片", "杰威尔音乐 · QQ音乐", AlbumRepository.artistSource)
                    LinkRow("本项目开源代码", "GitHub", "https://github.com/ShuangYu1145/Jay-Archive")

                }
            }

            item { PreferenceTitle(text = "测试版本") }
            item {
                Card(
                    insideMargin = PaddingValues(15.dp),
                    pressFeedbackType = PressFeedbackType.Tilt,
                    showIndication = false,
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "⚠️说明",
                            style = MiuixTheme.textStyles.title2
                        )
                        // 标题与副标题之间的间距（可选）
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "本项目处于测试阶段 无法保证所有功能正常"
                        )

                        Text(
                            text = "歌曲信息，封面，歌词均来自于网络，如有侵权，请联系删除。"
                        )
                    }
                }
            }

            item("footer") {
                Text("献给我们的青春。", Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    textAlign = TextAlign.Center, style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onBackgroundVariant)
            }
        }
    }
}

@Composable
private fun LinkRow(title: String, subtitle: String, url: String) {
    val uri = LocalUriHandler.current
    ArrowPreference(title = title, summary = subtitle, onClick = { uri.openUri(url) })
}

@Composable
fun SourceLink(title: String, url: String) {
    val uri = LocalUriHandler.current
    Text(title, Modifier.clickable(role = Role.Button, onClickLabel = "打开资料来源") { uri.openUri(url) }
        .padding(vertical = 12.dp), fontSize = 13.sp, color = MiuixTheme.colorScheme.primary)
}
