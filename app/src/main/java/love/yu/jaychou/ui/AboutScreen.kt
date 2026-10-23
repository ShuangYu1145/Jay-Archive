package love.yu.jaychou.ui

import love.yu.jaychou.data.AppText
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
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
    ArchiveScaffold(AppText.get(R.string.ui_about), behavior = behavior) { padding ->
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize().nestedScroll(behavior.nestedScrollConnection).testTag("about-list"),
            contentPadding = PaddingValues(12.dp, padding.calculateTopPadding() + 8.dp, 12.dp, bottom),
        ) {
            item("identity") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(painterResource(R.drawable.feature_graphic), AppText.get(R.string.ui_jay_archive_icon_blue_record_archive), Modifier.size(118.dp))
                    Text(AppText.get(R.string.ui_jay_archive), style = MiuixTheme.textStyles.title1)
                    Text("JAY ARCHIVE", fontSize = 12.sp, letterSpacing = 3.sp, color = MiuixTheme.colorScheme.primary)
                    QuietText(AppText.get(R.string.ui_beta_version, BuildConfig.VERSION_NAME))
                    Text(AppText.get(R.string.ui_a_pocket_archive_of_jay_chou_and_his_music_nexplore_his_releases),
                        style = MiuixTheme.textStyles.body2, textAlign = TextAlign.Center,
                        color = MiuixTheme.colorScheme.onBackgroundVariant)
                }
            }
            item("about-title") { PreferenceTitle(AppText.get(R.string.ui_about)) }
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
                        endActions = { PreferenceValue(AppText.get(R.string.ui_developer)) })
                }
            }

            item("project-title") { PreferenceTitle(AppText.get(R.string.ui_project_and_sources)) }
            item("project") {
                PreferenceCard {
                    LinkRow("Miuix UI", AppText.get(R.string.ui_open_source_ui_library_apache_2_0_0_9_3), "https://github.com/compose-miuix-ui/miuix")
                    LinkRow(AppText.get(R.string.ui_official_album_information), AppText.get(R.string.ui_titles_release_information_track_order_and_artwork), AlbumRepository.albumSource)
                    LinkRow(AppText.get(R.string.ui_jay_chou_biography_and_photos), AppText.get(R.string.ui_jvr_music_qq_music), AlbumRepository.artistSource)
                    LinkRow(AppText.get(R.string.ui_project_source_code), "GitHub", "https://github.com/ShuangYu1145/Jay-Archive")

                }
            }

            item { PreferenceTitle(text = AppText.get(R.string.ui_beta_release)) }
            item {
                Card(
                    insideMargin = PaddingValues(15.dp),
                    pressFeedbackType = PressFeedbackType.Tilt,
                    showIndication = false,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = AppText.get(R.string.ui_notice),
                            style = MiuixTheme.textStyles.title2
                        )
                        // 标题与副标题之间的间距（可选）
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = AppText.get(R.string.ui_this_project_is_in_beta_some_features_may_not_work_as_expected)
                        )

                        Text(
                            text = AppText.get(R.string.ui_song_information_artwork_and_lyrics_come_from_online_sources_ple)
                        )
                    }
                }
            }

            item("footer") {
                Text(AppText.get(R.string.ui_for_the_music_of_our_youth), Modifier.fillMaxWidth().padding(vertical = 28.dp),
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
    Text(title, Modifier.clickable(role = Role.Button, onClickLabel = AppText.get(R.string.ui_open_source_reference)) { uri.openUri(url) }
        .padding(vertical = 12.dp), fontSize = 13.sp, color = MiuixTheme.colorScheme.primary)
}
