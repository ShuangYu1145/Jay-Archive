package love.yu.jaychou.ui.screens.home

import love.yu.jaychou.ui.components.QuietText
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import love.yu.jaychou.R
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.designsystem.*
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import love.yu.jaychou.ui.designsystem.WindowBottomSheet

@Composable
fun HomeProfileButton(profile: AppSettings) {
    var editing by rememberSaveable { mutableStateOf(false) }
    val name = profile.userName.ifBlank { AppText.get(R.string.ui_profile_default_name) }
    Row(Modifier.clip(CircleShape).clickable(role = Role.Button,
        onClickLabel = AppText.get(R.string.ui_edit_profile), onClick = { editing = true })
        .testTag("home-profile").heightIn(min = 48.dp).padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ProfileAvatar(name, profile.userAvatarPath, Modifier.size(40.dp))
        Text(name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 120.dp))
    }
    ProfileSheet(editing, profile, onDismiss = { editing = false })
}

@Composable
private fun ProfileAvatar(name: String, path: String?, modifier: Modifier = Modifier) {
    val colors = ArchiveUiTheme.colorScheme
    Box(modifier.clip(CircleShape).background(colors.primary.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase(), color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        if (path != null) AsyncImage(File(path), contentDescription = null,
            modifier = Modifier.fillMaxSize().testTag("profile-avatar-image"), contentScale = ContentScale.Crop)
    }
}

@Composable
private fun ProfileSheet(show: Boolean, profile: AppSettings, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    val colors = ArchiveUiTheme.colorScheme
    var name by rememberSaveable(show) { mutableStateOf(profile.userName) }
    var avatar by rememberSaveable(show) { mutableStateOf(profile.userAvatarPath) }
    var busy by remember { mutableStateOf(false) }
    var error by remember(show) { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            error = null
            try {
                val imported = UserAvatarStore.import(context, uri)
                if (avatar != profile.userAvatarPath) UserAvatarStore.remove(context, avatar)
                avatar = imported
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = AppText.get(R.string.ui_avatar_import_failed)
            } finally { busy = false }
        }
    }
    val dismiss: () -> Unit = {
        if (!busy) {
            val discarded = avatar.takeIf { it != profile.userAvatarPath }
            scope.launch { UserAvatarStore.remove(context, discarded) }
            onDismiss()
        }
    }
    WindowBottomSheet(show, title = AppText.get(R.string.ui_edit_profile), onDismissRequest = dismiss,
        startAction = { TextButton(AppText.get(R.string.ui_cancel), enabled = !busy, onClick = dismiss) },
        endAction = {
            TextButton(AppText.get(R.string.ui_apply), enabled = !busy, modifier = Modifier.testTag("profile-save"), onClick = {
                scope.launch {
                    busy = true
                    error = null
                    try {
                        repository.setProfile(name, avatar)
                        if (profile.userAvatarPath != avatar) UserAvatarStore.remove(context, profile.userAvatarPath)
                        onDismiss()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        error = AppText.get(R.string.ui_unable_to_save_settings_please_try_again)
                    } finally { busy = false }
                }
            })
        },
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().testTag("profile-sheet"),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ProfileAvatar(name.ifBlank { AppText.get(R.string.ui_profile_default_name) }, avatar, Modifier.size(88.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(AppText.get(R.string.ui_choose_avatar), enabled = !busy,
                    modifier = Modifier.testTag("profile-choose-avatar"), onClick = { picker.launch(arrayOf("image/*")) })
                if (avatar != null) TextButton(AppText.get(R.string.ui_remove_avatar), enabled = !busy,
                    modifier = Modifier.testTag("profile-remove-avatar"), onClick = {
                        val discarded = avatar.takeIf { it != profile.userAvatarPath }
                        scope.launch { UserAvatarStore.remove(context, discarded) }
                        avatar = null
                    })
            }
            val nameLabel = AppText.get(R.string.ui_profile_name)
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(nameLabel, fontSize = 14.sp, color = colors.onSurfaceContainerVariant)
                BasicTextField(value = name, onValueChange = { name = it.replace("\n", "").take(30) },
                    enabled = !busy, singleLine = true,
                    textStyle = ArchiveUiTheme.textStyles.body1.copy(color = colors.onBackground),
                    cursorBrush = SolidColor(colors.primary),
                    modifier = Modifier.fillMaxWidth().background(colors.surfaceContainerHigh, CircleShape)
                        .padding(horizontal = 20.dp, vertical = 16.dp).testTag("profile-name-input")
                        .semantics { contentDescription = nameLabel },
                    decorationBox = { input ->
                        Box {
                            if (name.isEmpty()) Text(AppText.get(R.string.ui_profile_default_name), color = colors.onSurfaceContainerVariant)
                            input()
                        }
                    })
            }
            QuietText(AppText.get(R.string.ui_profile_local_note))
            if (busy) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            error?.let { Text(it, color = colors.primary, fontSize = 13.sp) }
        }
    }
}
