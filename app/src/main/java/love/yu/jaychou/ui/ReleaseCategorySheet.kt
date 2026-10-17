package love.yu.jaychou.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import love.yu.jaychou.data.*
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.preference.CheckboxLocation
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ReleaseCategorySheet(show: Boolean, selected: List<String>, catalog: AlbumCatalog,
                         onDismiss: () -> Unit, onApply: (List<String>) -> Unit) {
    var draft by rememberSaveable(show) { mutableStateOf(selected) }
    WindowBottomSheet(show = show, title = "专辑分类", onDismissRequest = onDismiss,
        startAction = { TextButton("取消", onClick = onDismiss) },
        endAction = { TextButton("确定", enabled = draft.isNotEmpty(), onClick = { onApply(draft); onDismiss() }) },
    ) {
        LazyColumn(Modifier.fillMaxWidth().testTag("category-sheet"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item("scope") { QuietText("可多选，至少保留一项。演唱会项指已发行唱片，非巡演场次。") }
            ReleaseCategory.entries.forEach { category ->
                item(category.name) {
                    Card(insideMargin = PaddingValues(0.dp), cornerRadius = 20.dp,
                        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainerHigh)) {
                        CheckboxPreference(
                            title = category.label,
                            summary = "${catalog.albums.count { it.category == category }} 张",
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
