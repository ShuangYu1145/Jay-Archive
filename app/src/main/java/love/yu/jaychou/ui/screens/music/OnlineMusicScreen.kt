package love.yu.jaychou.ui.screens.music

import android.util.Base64
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import love.yu.jaychou.R
import love.yu.jaychou.data.AppText
import love.yu.jaychou.online.*
import love.yu.jaychou.ui.components.ArchiveScaffold
import love.yu.jaychou.ui.components.CatalogImage
import love.yu.jaychou.ui.components.QuietText
import love.yu.jaychou.ui.components.SectionTitle
import love.yu.jaychou.ui.designsystem.*

internal fun providerLabel(provider: MusicProvider) = AppText.get(if (provider == MusicProvider.QQ) R.string.online_qq else R.string.online_netease)
internal fun onlineError(reason: OnlineMusicException.Reason) = AppText.get(when (reason) {
    OnlineMusicException.Reason.NOT_CONFIGURED -> R.string.online_not_configured
    OnlineMusicException.Reason.INVALID_ADDRESS -> R.string.online_invalid_address
    OnlineMusicException.Reason.AUTH -> R.string.online_auth_error
    OnlineMusicException.Reason.RESTRICTED -> R.string.online_restricted
    OnlineMusicException.Reason.NO_MATCH -> R.string.online_no_match
    OnlineMusicException.Reason.NETWORK -> R.string.online_network_error
    OnlineMusicException.Reason.TLS -> R.string.online_tls_error
    else -> R.string.online_service_error
})

@Composable
fun OnlineMusicScreen(state: OnlineMusicState, bottom: Dp, active: Boolean) {
    val colors = ArchiveUiTheme.colorScheme
    var showConfiguration by rememberSaveable { mutableStateOf(false) }
    var showLogin by rememberSaveable { mutableStateOf(state.qr != null) }
    val channel = state.loginChannel
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val saveScope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var requestedSave by remember { mutableStateOf<Pair<LoginQr, MusicProvider>?>(null) }
    fun saveQr(request: Pair<LoginQr, MusicProvider>) {
        if (saving) return
        saving = true
        saveScope.launch {
            try {
                LoginQrGallery.save(context.applicationContext, qrImageBytes(request.first.image)
                    ?: throw java.io.IOException("Invalid QR image"), request.second)
                Toast.makeText(context, AppText.get(R.string.online_qr_saved), Toast.LENGTH_LONG).show()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { Toast.makeText(context, AppText.get(R.string.online_qr_save_failed), Toast.LENGTH_LONG).show() }
            finally { saving = false }
        }
    }
    val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val request = requestedSave
        requestedSave = null
        if (granted && request != null) saveQr(request)
        else Toast.makeText(context, AppText.get(R.string.online_qr_save_permission), Toast.LENGTH_LONG).show()
    }
    LaunchedEffect(Unit) { state.refreshAccount() }
    LaunchedEffect(active, showLogin, channel, state.provider, state.verifyServiceCertificates) {
        if (!active || (!showLogin && state.qr == null)) { state.pauseLogin(); return@LaunchedEffect }
        // Keep checking while the Activity is backgrounded for QQ/WeChat scanning.
        // Destroying it stops this worker, but the encrypted pending login survives.
        lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
            state.startLogin(channel)
            try { awaitCancellation() } finally { state.pauseLogin() }
        }
    }
    LaunchedEffect(state.qrCode, state.users[state.provider]) {
        if (state.qrCode == 803 && state.users[state.provider] != null) showLogin = false
    }
    ArchiveScaffold(AppText.get(R.string.online_title), actions = {
        TextButton(AppText.get(R.string.online_services), { showConfiguration = true }, modifier = Modifier.testTag("online-services"))
    }) { padding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxWidth().testTag("online-list").imePadding(),
            contentPadding = PaddingValues(20.dp, padding.calculateTopPadding() + 12.dp, 20.dp, bottom),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item("providers") {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MusicProvider.entries.forEach { source ->
                        Card(Modifier.weight(1f).fillMaxHeight().semantics { selected = state.provider == source }.testTag("online-provider-${source.name}"), cornerRadius = 18.dp,
                            colors = CardDefaults.defaultColors(if (state.provider == source) colors.primaryContainer else colors.surfaceContainer),
                            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 18.dp), onClick = { state.select(source) }) {
                            Text(providerLabel(source), fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                color = if (state.provider == source) colors.onPrimaryContainer else colors.onSurfaceContainer)
                        }
                    }
                }
            }
            item("search") {
                Card(Modifier.fillMaxWidth(), cornerRadius = 18.dp, insideMargin = PaddingValues(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OnlineInput(state.query, { state.query = it }, AppText.get(R.string.online_search_hint),
                            Modifier.weight(1f).testTag("online-search-input"), onSearch = { keyboard?.hide(); state.search() })
                        TextButton(AppText.get(R.string.online_search), { keyboard?.hide(); state.search() },
                            enabled = state.query.isNotBlank(), modifier = Modifier.testTag("online-search"))
                    }
                }
            }
            item("account") {
                val user = state.users[state.provider]
                Card(Modifier.fillMaxWidth().testTag("online-account"), insideMargin = PaddingValues(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (user?.avatar?.isNotBlank() == true) CatalogImage(user.avatar, user.nickname,
                            Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)), showStatus = false)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(user?.nickname?.ifBlank { user.id } ?: AppText.get(R.string.online_account_title), fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            QuietText(if (user != null) providerLabel(state.provider) + " · " + user.id else AppText.get(R.string.online_account_hint))
                        }
                        if (state.accountLoading) CircularProgressIndicator(size = 24.dp)
                        else TextButton(AppText.get(if (user != null) R.string.online_logout else R.string.online_login),
                            { if (user != null) state.logout() else if (state.repository.accounts.endpoint(state.provider).isBlank()) showConfiguration = true else showLogin = true },
                            modifier = Modifier.testTag("online-login"))
                    }
                    if (user != null) {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Row(
                            Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TextButton(AppText.get(R.string.online_liked), state::liked, modifier = Modifier.testTag("online-liked"))
                            TextButton(AppText.get(R.string.online_refresh), state::refreshAccount, enabled = !state.accountLoading)
                        }
                    }
                }
            }
            if (state.selectedPlaylist != null || state.searchedQuery.isNotBlank()) {
                item("songs-title") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            SectionTitle(state.selectedPlaylist?.let { if (it.liked) AppText.get(R.string.online_liked) else it.name }
                                ?: AppText.get(R.string.online_results))
                        }
                        TextButton(AppText.get(R.string.online_library), state::library)
                    }
                }
                if (state.songs.isNotEmpty()) item("play-all") {
                    Button({ state.play() }, modifier = Modifier.testTag("online-play-all")) { Text(AppText.get(R.string.online_play_all)) }
                }
                itemsIndexed(state.songs, key = { _, song -> song.key }) { index, song ->
                    Card(Modifier.fillMaxWidth().testTag("online-song-${song.id}"), cornerRadius = 18.dp,
                        insideMargin = PaddingValues(14.dp), onClick = { state.play(index) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            CatalogImage(song.cover, song.title, Modifier.size(52.dp).clip(RoundedCornerShape(9.dp)), showStatus = false)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(song.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist + " · " + song.album, fontSize = 12.sp, color = colors.onSurfaceContainerVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(AppText.get(R.string.music_play), fontSize = 12.sp, color = colors.primary)
                        }
                    }
                }
                if (state.more) item("more") { TextButton(AppText.get(R.string.online_more), { state.load(true) }, enabled = !state.loading) }
                if (!state.loading && state.songs.isEmpty()) item("empty") { QuietText(AppText.get(R.string.online_empty)) }
            } else {
                item("library-title") { SectionTitle(AppText.get(R.string.online_playlists), providerLabel(state.provider)) }
                items(state.playlists, key = { it.id }) { playlist ->
                    Card(Modifier.fillMaxWidth().testTag("online-playlist-${playlist.id}"), insideMargin = PaddingValues(14.dp), onClick = { state.open(playlist) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            CatalogImage(playlist.cover, playlist.name, Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)), showStatus = false)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(playlist.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                QuietText(AppText.get(R.string.ui_tracks, playlist.count))
                            }
                        }
                    }
                }
                if (state.playlists.isEmpty() && !state.accountLoading) item("welcome") {
                    Card(Modifier.fillMaxWidth(), insideMargin = PaddingValues(22.dp)) {
                        Text(AppText.get(R.string.online_welcome), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        QuietText(AppText.get(R.string.online_welcome_hint))
                    }
                }
            }
            if (state.loading) item("loading") { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }
        }
    }
    WindowBottomSheet(showLogin && active, AppText.get(R.string.online_login), { showLogin = false; state.pauseLogin() }, expanded = true) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.provider == MusicProvider.QQ) Row {
                TextButton(AppText.get(R.string.online_scan_qq), { state.startLogin("qq") })
                TextButton(AppText.get(R.string.online_scan_wechat), { state.startLogin("wechat") })
            }
            Text(AppText.get(if (state.provider == MusicProvider.NETEASE) R.string.online_scan_netease else if (channel == "wechat") R.string.online_scan_wechat_hint else R.string.online_scan_qq_hint),
                color = colors.onSurfaceContainerVariant)
            if (state.loginLoading) CircularProgressIndicator()
            state.qr?.let { qr ->
                val image = remember(qr.image) { qrImageBytes(qr.image) }
                var imageFailed by remember(qr.key) { mutableStateOf(false) }
                if (image != null) AsyncImage(ImageRequest.Builder(LocalContext.current).data(image).decoderFactory(SvgDecoder.Factory()).build(),
                    AppText.get(R.string.online_qr), Modifier.size(224.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).padding(12.dp).testTag("online-qr"),
                    onError = { imageFailed = true })
                else Text(onlineError(OnlineMusicException.Reason.INVALID_RESPONSE))
                if (imageFailed) {
                    Text(onlineError(OnlineMusicException.Reason.INVALID_RESPONSE))
                    TextButton(AppText.get(R.string.online_refresh_qr), { state.refreshLogin(channel) })
                }
                Text(AppText.get(when (state.qrCode) { 800 -> R.string.online_qr_expired; 802 -> R.string.online_qr_scanned; 803 -> R.string.online_qr_confirmed; else -> R.string.online_qr_waiting }))
                QuietText(AppText.get(R.string.online_qr_keep_hint))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(AppText.get(if (saving) R.string.online_qr_saving else R.string.online_qr_save), {
                        val request = qr to state.provider
                        if (Build.VERSION.SDK_INT <= 28 && ContextCompat.checkSelfPermission(context,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            requestedSave = request
                            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else saveQr(request)
                    }, enabled = !saving && image != null && !imageFailed && state.qrCode in 801..802,
                        modifier = Modifier.testTag("online-save-qr"))
                    TextButton(AppText.get(R.string.online_refresh_qr), { state.refreshLogin(channel) },
                        enabled = !state.loginLoading, modifier = Modifier.testTag("online-refresh-qr"))
                }
            }
            state.loginError?.let { Text(onlineError(it)) }
            if (state.loginError != null) TextButton(AppText.get(R.string.music_retry), { state.startLogin(channel) })
        }
    }
    OnlineServiceSheet(showConfiguration, state) { showConfiguration = false }
}

internal fun qrImageBytes(value: String): ByteArray? {
    if (value.length > 1_500_000 || !value.startsWith("data:image/") || !value.substringBefore(',').endsWith(";base64")) return null
    return runCatching { Base64.decode(value.substringAfter(','), Base64.DEFAULT) }.getOrNull()
}

@Composable
private fun OnlineServiceSheet(show: Boolean, state: OnlineMusicState, onClose: () -> Unit) {
    if (!show) return
    var netease by remember { mutableStateOf(state.repository.accounts.endpoint(MusicProvider.NETEASE)) }
    var qq by remember { mutableStateOf(state.repository.accounts.endpoint(MusicProvider.QQ)) }
    WindowBottomSheet(true, AppText.get(R.string.online_services), onClose, expanded = true) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            QuietText(AppText.get(R.string.online_services_hint))
            Text(providerLabel(MusicProvider.NETEASE), fontWeight = FontWeight.SemiBold)
            ServiceInput(netease, { netease = it }, "https://music.example.com/netease", "online-netease-endpoint")
            Text(providerLabel(MusicProvider.QQ), fontWeight = FontWeight.SemiBold)
            ServiceInput(qq, { qq = it }, "https://music.example.com/qq", "online-qq-endpoint")
            Card(Modifier.fillMaxWidth()) {
                SwitchPreference(AppText.get(R.string.online_verify_certificates),
                    AppText.get(if (state.verifyServiceCertificates) R.string.online_verify_certificates_on else R.string.online_verify_certificates_off),
                    checked = state.verifyServiceCertificates, onCheckedChange = state::setCertificateVerification,
                    modifier = Modifier.testTag("online-verify-certificates"))
            }
            Button({ if (state.configure(netease, qq)) onClose() }, Modifier.fillMaxWidth().testTag("online-save-services")) { Text(AppText.get(R.string.online_save)) }
        }
    }
}

@Composable
private fun ServiceInput(value: String, onValue: (String) -> Unit, hint: String, tag: String) {
    Card(Modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) { OnlineInput(value, onValue, hint, Modifier.fillMaxWidth().testTag(tag)) }
}

@Composable
private fun OnlineInput(value: String, onValue: (String) -> Unit, hint: String, modifier: Modifier, onSearch: (() -> Unit)? = null) {
    val colors = ArchiveUiTheme.colorScheme
    BasicTextField(value, onValue, modifier.heightIn(min = 40.dp).semantics { contentDescription = hint },
        singleLine = true, textStyle = TextStyle(color = colors.onSurfaceContainer, fontSize = 16.sp), cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = if (onSearch != null) ImeAction.Search else ImeAction.Done),
        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }), decorationBox = { input ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(hint, color = colors.onSurfaceContainerVariant, fontSize = 14.sp)
                input()
            }
        })
}
