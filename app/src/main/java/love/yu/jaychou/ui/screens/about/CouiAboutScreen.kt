package love.yu.jaychou.ui.screens.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
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
import love.yu.jaychou.ui.components.ArchiveScaffold
import love.yu.jaychou.ui.designsystem.*
import love.yu.jaychou.ui.updates.LocalUpdateController

/** ColorOS's grouped about layout, with application information in place of device specs. */
@Composable
internal fun CouiAboutScreen(state: LazyListState, bottom: Dp, active: Boolean = true, onDeveloperTap: () -> Unit) {
    val versionVisible by remember(state) { derivedStateOf { state.firstVisibleItemIndex == 0 } }
    ArchiveScaffold(AppText.get(R.string.ui_about), small = true) { padding ->
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize().testTag("about-list"),
            contentPadding = PaddingValues(start = CouiLayout.PageInset, end = CouiLayout.PageInset,
                top = padding.calculateTopPadding() + 16.dp, bottom = bottom),
            verticalArrangement = Arrangement.spacedBy(CouiLayout.GroupSpacing),
        ) {
            item("identity") { CouiVersionCard(active && versionVisible) }
            item("information") {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CouiInfoCard(Modifier.weight(1f).fillMaxHeight(), AppText.get(R.string.ui_jay_archive), "JAY ARCHIVE") {
                        Image(painterResource(R.drawable.feature_graphic), null,
                            Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)))
                    }
                    CouiInfoCard(Modifier.weight(1f).fillMaxHeight().testTag("about-developer-card"),
                        AppText.get(R.string.ui_developer), "ShuangYu", onDeveloperTap) {
                        AsyncImage("https://q1.qlogo.cn/g?b=qq&nk=1106388837&s=100", "ShuangYu",
                            Modifier.size(28.dp).clip(RoundedCornerShape(14.dp)).testTag("about-developer-avatar"),
                            contentScale = ContentScale.Crop)
                    }
                }
            }
            item("project") {
                CouiAboutCard {
                    CouiSourceRow("COUI", "ColorOS UI", "https://github.com/suqi8/coui")
                    PreferenceDivider()
                    CouiSourceRow(AppText.get(R.string.ui_project_source_code), "GitHub", "https://github.com/ShuangYu1145/Jay-Archive")
                    PreferenceDivider()
                    CouiSourceRow(AppText.get(R.string.ui_official_album_information), "Apple Music", AlbumRepository.albumSource)
                    PreferenceDivider()
                    CouiSourceRow(AppText.get(R.string.ui_jay_chou_biography_and_photos),
                        AppText.get(R.string.ui_jvr_music_qq_music), AlbumRepository.artistSource)
                    PreferenceDivider()
                    CouiSourceRow("Miuix UI", "Apache 2.0", "https://github.com/compose-miuix-ui/miuix")
                }
            }
            item("notice") {
                CouiAboutCard {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(AppText.get(R.string.ui_notice), fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        Text(AppText.get(R.string.ui_this_project_is_in_beta_some_features_may_not_work_as_expected),
                            color = ArchiveUiTheme.colorScheme.onBackgroundVariant, fontSize = 14.sp)
                        Text(AppText.get(R.string.ui_song_information_artwork_and_lyrics_come_from_online_sources_ple),
                            color = ArchiveUiTheme.colorScheme.onBackgroundVariant, fontSize = 14.sp)
                    }
                }
            }
            item("support") {
                CouiAboutCard {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(AppText.get(R.string.ui_support_motivation), Modifier.fillMaxWidth(), fontSize = 16.sp)
                        Spacer(Modifier.height(16.dp))
                        AsyncImage("file:///android_asset/help.png", AppText.get(R.string.ui_support_qr),
                            Modifier.widthIn(max = 320.dp).fillMaxWidth().aspectRatio(1f).testTag("about-support-qr"),
                            contentScale = ContentScale.Fit)
                    }
                }
            }
            item("footer") {
                Text(AppText.get(R.string.ui_for_the_music_of_our_youth),
                    Modifier.fillMaxWidth().padding(vertical = 12.dp), textAlign = TextAlign.Center,
                    fontSize = 13.sp, color = ArchiveUiTheme.colorScheme.onBackgroundVariant)
            }
        }
    }
}

@Composable
private fun CouiAboutCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), cornerRadius = CouiLayout.CardRadius, content = content)
}

@Composable
private fun CouiInfoCard(modifier: Modifier, label: String, value: String,
                         onClick: (() -> Unit)? = null, icon: @Composable () -> Unit) {
    Card(modifier, cornerRadius = CouiLayout.CardRadius, onClick = onClick) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            icon()
            Spacer(Modifier.height(4.dp))
            Text(label, color = ArchiveUiTheme.colorScheme.onBackgroundVariant, fontSize = 14.sp)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CouiSourceRow(title: String, value: String, url: String) {
    val uri = LocalUriHandler.current
    Row(Modifier.fillMaxWidth().heightIn(min = CouiLayout.RowMinHeight).clickable { uri.openUri(url) }.padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, Modifier.weight(1f), fontSize = 16.sp)
        Text(value, Modifier.widthIn(max = 112.dp), fontSize = 14.sp, textAlign = TextAlign.End,
            color = ArchiveUiTheme.colorScheme.onBackgroundVariant)
        Text("›", fontSize = 22.sp, color = ArchiveUiTheme.colorScheme.onBackgroundVariant)
    }
}

@Composable
private fun CouiVersionCard(active: Boolean) {
    val controller = LocalUpdateController.current
    val update = controller?.availableUpdate
    val colors = ArchiveUiTheme.colorScheme
    val titleBrush = rememberAboutTitleBrush()
    val checking = controller?.checking == true
    val status = when {
        checking -> AppText.get(R.string.ui_update_checking)
        update != null -> AppText.get(R.string.ui_update_available, update.versionName)
        else -> AppText.get(R.string.ui_update_latest)
    }
    BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
        .background(colors.background)
        .border(.5.dp, colors.onBackground.copy(alpha = .06f), RoundedCornerShape(22.dp))
        .testTag("about-identity")
        .clickable(enabled = controller != null && !checking, role = Role.Button, onClickLabel = status) {
            controller?.let {
                if (update != null) it.showAvailableUpdate() else it.request(manual = true)
            }
        }) {
        val heroHeight = (maxWidth * .85f).coerceIn(300.dp, 380.dp)
        val titleSize = (maxWidth.value * .14f).coerceIn(40f, 56f).sp
        AboutBackground(active = active, modifier = Modifier.matchParentSize())
        // A minimum height keeps the compact version-card proportions while allowing large text to grow.
        Column(Modifier.fillMaxWidth().heightIn(min = heroHeight).padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween) {
            Text("JAY ARCHIVE", color = colors.onBackgroundVariant, fontSize = 12.sp, letterSpacing = 3.sp)
            Column(Modifier.fillMaxWidth().padding(vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicText(AppText.get(R.string.ui_jay_archive),
                    modifier = Modifier.fillMaxWidth(),
                    style = ArchiveUiTheme.textStyles.title1.copy(
                        brush = titleBrush, fontSize = titleSize, lineHeight = titleSize * 1.2f,
                        fontWeight = FontWeight.Medium, textAlign = TextAlign.Center))
                Text(BuildConfig.VERSION_NAME, color = colors.onBackgroundVariant, fontSize = 17.sp)
            }
            Row(Modifier.widthIn(max = 280.dp).clip(CircleShape)
                .background(colors.surfaceContainer.copy(alpha = .7f))
                .heightIn(min = 48.dp).padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag("check-updates").semantics { liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (checking) CircularProgressIndicator(Modifier.size(18.dp), size = 18.dp)
                Text(status, Modifier.weight(1f, fill = false), color = colors.onBackground,
                    fontSize = 14.sp, textAlign = TextAlign.Center)
                if (update != null && !checking) Text("›", color = colors.onBackgroundVariant, fontSize = 20.sp)
            }
        }
    }
}
