package love.yu.jaychou.ui.screens.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import love.yu.jaychou.BuildConfig
import love.yu.jaychou.R
import love.yu.jaychou.data.AlbumRepository
import love.yu.jaychou.data.AppText
import love.yu.jaychou.ui.components.PreferenceCard
import love.yu.jaychou.ui.components.PreferenceTitle
import love.yu.jaychou.ui.components.PreferenceValue
import love.yu.jaychou.ui.designsystem.BasicComponent
import love.yu.jaychou.ui.designsystem.Card
import love.yu.jaychou.ui.designsystem.Text
import love.yu.jaychou.ui.designsystem.ArrowPreference
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun AboutScreen(
    state: LazyListState, bottom: Dp, active: Boolean = true,
) {
    val background = ArchiveUiTheme.colorScheme.background
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize().background(background).clipToBounds()) {
        val heroHeight = (maxHeight * .60f).coerceAtLeast(300.dp * density.fontScale)
        val heroPx = with(density) { heroHeight.toPx() }
        val fadeDistance = with(density) { 389.dp.toPx() }
        val backgroundVisible by remember(state, fadeDistance) {
            derivedStateOf {
                state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset < fadeDistance
            }
        }
        val heroVisible by remember(state, heroPx) {
            derivedStateOf {
                state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset < heroPx * .60f
            }
        }
        AboutBackground(
            active = active && backgroundVisible,
            modifier = Modifier.fillMaxWidth().height(heroHeight + 180.dp).graphicsLayer {
                val scroll = if (state.firstVisibleItemIndex == 0) state.firstVisibleItemScrollOffset.toFloat() else heroPx
                alpha = (1f - scroll / fadeDistance).coerceIn(0f, 1f)
                translationY = -scroll * .12f
            },
        )
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize().testTag("about-list"),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = bottom),
        ) {
            item("identity") { Spacer(Modifier.height(heroHeight)) }
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
                                    .background(ArchiveUiTheme.colorScheme.surfaceContainerHighest),
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
                            style = ArchiveUiTheme.textStyles.title2
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
                    textAlign = TextAlign.Center, style = ArchiveUiTheme.textStyles.footnote1,
                    color = ArchiveUiTheme.colorScheme.onBackgroundVariant)
            }
        }
        if (heroVisible) AboutIdentity(
            Modifier.fillMaxWidth().height(heroHeight).statusBarsPadding()
                .padding(horizontal = 28.dp)
                .graphicsLayer {
                    val progress = ((state.firstVisibleItemScrollOffset - heroPx * .25f) / (heroPx * .35f)).coerceIn(0f, 1f)
                    alpha = 1f - progress
                    scaleX = 1f - progress * .10f
                    scaleY = scaleX
                },
        )
    }
}

@Composable
private fun AboutIdentity(modifier: Modifier) {
    val dark = ArchiveUiTheme.colorScheme.background.luminance() < .5f
    val titleBrush = remember(dark) {
        Brush.horizontalGradient(
            if (dark) listOf(Color(0xFFB9CBFF), Color(0xFFF2B6DA))
            else listOf(Color(0xFF234EAB), Color(0xFF8C326B)),
        )
    }
    Column(modifier.testTag("about-identity"), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Image(painterResource(R.drawable.feature_graphic),
            AppText.get(R.string.ui_jay_archive_icon_blue_record_archive), Modifier.size(100.dp))
        Spacer(Modifier.height(18.dp))
        BasicText(AppText.get(R.string.ui_jay_archive),
            modifier = Modifier.semantics { heading() },
            style = ArchiveUiTheme.textStyles.title1.copy(
                brush = titleBrush, fontSize = 38.sp, lineHeight = 48.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            ))
        Text("JAY ARCHIVE", Modifier.padding(top = 4.dp), fontSize = 13.sp, letterSpacing = 3.sp,
            color = ArchiveUiTheme.colorScheme.onBackgroundVariant)
        Text(AppText.get(R.string.ui_beta_version, BuildConfig.VERSION_NAME),
            Modifier.padding(top = 14.dp), fontSize = 15.sp,
            color = ArchiveUiTheme.colorScheme.onBackgroundVariant)
        Text(AppText.get(R.string.ui_a_pocket_archive_of_jay_chou_and_his_music_nexplore_his_releases),
            Modifier.padding(top = 14.dp), style = ArchiveUiTheme.textStyles.footnote1,
            textAlign = TextAlign.Center, color = ArchiveUiTheme.colorScheme.onBackgroundVariant)
    }
}

@Composable
private fun LinkRow(title: String, subtitle: String, url: String) {
    val uri = LocalUriHandler.current
    ArrowPreference(title = title, summary = subtitle, onClick = { uri.openUri(url) })
}
