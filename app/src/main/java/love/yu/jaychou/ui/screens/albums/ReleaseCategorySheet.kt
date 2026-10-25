package love.yu.jaychou.ui.screens.albums

import love.yu.jaychou.ui.components.QuietText
import love.yu.jaychou.data.AppText
import love.yu.jaychou.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.designsystem.*
import love.yu.jaychou.ui.designsystem.WindowBottomSheet
import love.yu.jaychou.ui.designsystem.CheckboxPreference
import top.yukonga.miuix.kmp.preference.CheckboxLocation
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Ok

@Composable
fun ReleaseCategorySheet(show: Boolean, selected: List<String>, catalog: AlbumCatalog,
                         onDismiss: () -> Unit, onApply: (List<String>) -> Unit) {
    var draft by rememberSaveable(show) { mutableStateOf(selected) }
    WindowBottomSheet(show = show, title = AppText.get(R.string.ui_album_categories), onDismissRequest = onDismiss,
        startAction = { SheetActionButton(MiuixIcons.Close, AppText.get(R.string.ui_cancel), onClick = onDismiss,
            modifier = Modifier.testTag("category-cancel")) },
        endAction = { SheetActionButton(MiuixIcons.Ok, AppText.get(R.string.ui_confirm), enabled = draft.isNotEmpty(),
            onClick = { onApply(draft); onDismiss() }, modifier = Modifier.testTag("category-apply")) },
        expanded = true,
    ) {
        LazyColumn(Modifier.fillMaxWidth().testTag("category-sheet"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item("scope") { QuietText(AppText.get(R.string.ui_select_one_or_more_categories_live_albums_are_released_recording)) }
            ReleaseCategory.entries.forEach { category ->
                item(category.name) {
                    Card(insideMargin = PaddingValues(0.dp),
                        colors = CardDefaults.defaultColors(color = ArchiveUiTheme.colorScheme.surfaceContainerHigh)) {
                        CheckboxPreference(
                            title = category.label,
                            summary = AppText.get(R.string.ui_releases_202, catalog.albums.count { it.category == category }),
                            checkboxLocation = CheckboxLocation.End,
                            checked = category.name in draft,
                            onCheckedChange = { checked ->
                                draft = if (checked) (draft + category.name).distinct() else draft - category.name
                            },
                            modifier = Modifier.testTag("category-${category.name}"),
                        )
                    }
                }
            }
        }
    }
}
