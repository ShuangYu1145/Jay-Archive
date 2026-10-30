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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import love.yu.jaychou.BuildConfig
import love.yu.jaychou.R
import love.yu.jaychou.data.AlbumRepository
import love.yu.jaychou.data.AppText
import love.yu.jaychou.ui.components.AlbumCoverShape
import love.yu.jaychou.ui.components.CatalogImage
import love.yu.jaychou.ui.components.PreferenceCard
import love.yu.jaychou.ui.components.PreferenceTitle
import love.yu.jaychou.ui.components.PreferenceValue
import love.yu.jaychou.ui.components.QuietText
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
    var developerCardTaps by remember { mutableIntStateOf(0) }
    var showEasterEgg by rememberSaveable { mutableStateOf(false) }
    if (ArchiveUiTheme.isCoui) {
        CouiAboutScreen(state, bottom, active) {
            developerCardTaps++
            if (developerCardTaps == 4) {
                developerCardTaps = 0
                showEasterEgg = true
            }
        }
        AboutEasterEggDialog(showEasterEgg && active) { showEasterEgg = false }
        return
    }
    val availableUpdate = love.yu.jaychou.ui.updates.LocalUpdateController.current?.availableUpdate
    val background = ArchiveUiTheme.pageBackground
    val density = LocalDensity.current
    AboutTopEdge {
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
        if (love.yu.jaychou.ui.effects.LocalCustomBackgroundPath.current == null) AboutBackground(
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
                Card(
                    modifier = Modifier.testTag("about-developer-card"),
                    onClick = {
                        developerCardTaps++
                        if (developerCardTaps == 4) {
                            developerCardTaps = 0
                            showEasterEgg = true
                        }
                    },
                ) {
                    BasicComponent(
                        title = "ShuangYu",
                        startAction = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(ArchiveUiTheme.colorScheme.surfaceContainerHighest)
                                    .testTag("about-developer-avatar"),
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

            if (availableUpdate != null) {
                item("updates-title") { PreferenceTitle(AppText.get(R.string.ui_update_settings)) }
                item("updates") {
                    PreferenceCard { love.yu.jaychou.ui.updates.AvailableUpdateRow() }
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

            item { PreferenceTitle(text = "你们的支持是我最大的动力") }
            item {
                Card(
                    insideMargin = PaddingValues(15.dp),
                    pressFeedbackType = PressFeedbackType.Tilt,
                    showIndication = false,
                ) {
                  Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                      AsyncImage(
                          model = "file:///android_asset/help.png",
                          contentDescription = "支持作者二维码",
                          contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                          modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth()
                              .aspectRatio(1171f / 1171f)
                              .testTag("about-support-qr"),
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
    AboutEasterEggDialog(show = showEasterEgg && active, onDismiss = { showEasterEgg = false })
}

@Composable
private fun AboutEasterEggDialog(show: Boolean, onDismiss: () -> Unit) {
    var code by rememberSaveable(show) { mutableStateOf("") }
    var message by rememberSaveable(show) { mutableStateOf<String?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val onCodeChange: (String) -> Unit = { value ->
        code = value
        message = when (code) {
            "0118" -> "周杰伦牛逼"
            "0302" -> "诶哟，你怎么知道作者的生日的"
            "1008" -> "庭庭，不管怎样，我会永远爱你的"
            "庭庭" -> "永远爱你哟"
            "度蜜月卡" -> "蜜蜜蜜蜜"
            "同性恋" -> "我理解你可能出于信仰、传统或对陌生事物的不安而持有不同看法，你可以保留自己的道德或宗教观点；但请把“不认同”停留在个人信念层面，而不是变成对身边人的羞辱、排斥、歧视或要求剥夺他们平等生活、工作和相爱的权利。同性恋者和你一样，是努力生活、爱家人、希望被尊重的普通人，他们相爱的方式并没有伤害你。社会可以有不同的声音，但文明底线是不因一个人爱谁而否定他的人格与尊严。你可以不祝福，但请别伤害；如果愿意，先听一个真实的人讲讲他的生活，很多敌意会变成理解。"
            else -> null
        }
        if (message != null) keyboard?.hide()
    }
    val dismissLabel = if (message == null) AppText.get(R.string.ui_cancel) else AppText.get(R.string.ui_confirm)
    if (ArchiveUiTheme.isMaterial) {
        if (show) androidx.compose.material3.AlertDialog(
            modifier = Modifier.testTag("about-easter-egg-dialog"),
            onDismissRequest = onDismiss,
            containerColor = if (ArchiveUiTheme.colorScheme.background.luminance() < .5f) Color.Black
                else androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp,
            title = { androidx.compose.material3.Text(message ?: "彩蛋") },
            text = if (message == null) {
                {
                    androidx.compose.material3.OutlinedTextField(
                        value = code,
                        onValueChange = onCodeChange,
                        label = { androidx.compose.material3.Text("输入彩蛋口令") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("about-easter-egg-input"),
                    )
                }
            } else null,
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("about-easter-egg-dismiss"),
                ) { androidx.compose.material3.Text(dismissLabel) }
            },
        )
        return
    }
    if (ArchiveUiTheme.isCoui) {
        io.github.suqi8.coui.kmp.window.WindowDialog(show = show, title = "彩蛋", summary = message,
            modifier = Modifier.testTag("about-easter-egg-dialog"), onDismissRequest = onDismiss) {
            Column {
                if (message == null) Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                    io.github.suqi8.coui.kmp.basic.TextField(
                        value = code, onValueChange = onCodeChange, label = "输入彩蛋口令", singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("about-easter-egg-input"))
                }
                io.github.suqi8.coui.kmp.layout.DialogButtonBar(
                    modifier = Modifier.testTag("about-easter-egg-dismiss"), negative = null,
                    positive = io.github.suqi8.coui.kmp.layout.DialogButtonBarAction(
                        text = dismissLabel, onClick = onDismiss),
                )
            }
        }
        return
    }
    CenteredEasterEggDialog(
        show = show,
        title = message ?: "彩蛋",
        onDismissRequest = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (message == null) {
                top.yukonga.miuix.kmp.basic.TextField(
                    value = code,
                    onValueChange = onCodeChange,
                    label = "输入彩蛋口令",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("about-easter-egg-input"),
                )
            }
            top.yukonga.miuix.kmp.basic.TextButton(
                text = dismissLabel,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().testTag("about-easter-egg-dismiss"),
            )
        }
    }
}

@Composable
private fun CenteredEasterEggDialog(
    show: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (!show) return
    val colors = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
    // Miuix WindowDialog aligns to the bottom on phones. A platform dialog host
    // centers the same native Miuix surface and controls at every window size.
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        top.yukonga.miuix.kmp.basic.Card(
            modifier = Modifier.padding(horizontal = 24.dp).widthIn(max = 420.dp)
                .fillMaxWidth().testTag("about-easter-egg-dialog"),
            cornerRadius = 32.dp,
            insideMargin = PaddingValues(24.dp),
            colors = top.yukonga.miuix.kmp.basic.CardDefaults.defaultColors(
                color = if (colors.background.luminance() < .5f) Color.Black
                    else top.yukonga.miuix.kmp.layout.DialogDefaults.backgroundColor(),
            ),
        ) {
            top.yukonga.miuix.kmp.basic.Text(
                text = title,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            content()
        }
    }
}

@Composable
private fun AboutIdentity(modifier: Modifier) {
    val titleBrush = rememberAboutTitleBrush()
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

/** The about page has no toolbar; sample its scrolling content for the top edge. */
@Composable
private fun AboutTopEdge(content: @Composable () -> Unit) {
    val progressive = android.os.Build.VERSION.SDK_INT >= 33 &&
        !ArchiveUiTheme.isMaterial && !ArchiveUiTheme.isCoui &&
        love.yu.jaychou.ui.components.LocalArchiveBlur.current &&
        love.yu.jaychou.ui.effects.LocalTopAppBarBlur.current == love.yu.jaychou.data.TopAppBarBlur.PROGRESSIVE
    if (progressive) {
        love.yu.jaychou.ui.effects.HeaderGlassLayer(
            ArchiveUiTheme.colorScheme.background, progressive = true,
            content = { capture, header ->
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().then(capture)) { content() }
                    Box(header.fillMaxWidth().statusBarsPadding().height(74.dp)
                        .testTag("about-progressive-header"))
                }
            },
        )
    } else content()
}
