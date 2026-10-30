package love.yu.jaychou.ui.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import love.yu.jaychou.R
import love.yu.jaychou.data.AppText
import top.yukonga.miuix.kmp.preference.CheckboxLocation

@Composable
fun BasicComponent(
    title: String, modifier: Modifier = Modifier, summary: String? = null,
    startAction: (@Composable () -> Unit)? = null,
    endActions: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null, enabled: Boolean = true,
) {
    if (ArchiveUiTheme.isMaterial) {
        androidx.compose.material3.ListItem(
            headlineContent = { androidx.compose.material3.Text(title) },
            supportingContent = summary?.let { { androidx.compose.material3.Text(it) } },
            leadingContent = startAction,
            trailingContent = endActions?.let { { Row(content = it) } },
            modifier = modifier.then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
                .then(if (enabled) Modifier else Modifier.alpha(.38f)),
            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    } else if (ArchiveUiTheme.isCoui) io.github.suqi8.coui.kmp.basic.BasicComponent(
        title = title, modifier = modifier.heightIn(min = CouiLayout.RowMinHeight),
        summary = summary.takeIf { endActions == null }, startAction = startAction,
        bottomAction = summary?.takeIf { endActions != null }?.let { { CouiPreferenceSummary(it) } },
        insideMargin = CouiLayout.RowPadding,
        endActions = endActions, onClick = onClick, enabled = enabled)
    else top.yukonga.miuix.kmp.basic.BasicComponent(
        title = title, modifier = modifier, summary = summary, startAction = startAction,
        endActions = endActions, onClick = onClick, enabled = enabled)
}

@Composable
fun ArrowPreference(title: String, modifier: Modifier = Modifier, summary: String? = null,
                    endActions: @Composable RowScope.() -> Unit = {}, onClick: (() -> Unit)? = null) {
    if (ArchiveUiTheme.isMaterial) BasicComponent(title, modifier, summary,
        endActions = { endActions(); Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant) }, onClick = onClick)
    else if (ArchiveUiTheme.isCoui) io.github.suqi8.coui.kmp.preference.ArrowPreference(title,
        modifier.heightIn(min = CouiLayout.RowMinHeight),
        bottomAction = summary?.let { { CouiPreferenceSummary(it) } },
        insideMargin = CouiLayout.RowPadding, endActions = endActions, onClick = onClick)
    else top.yukonga.miuix.kmp.preference.ArrowPreference(title, modifier, summary = summary,
        endActions = endActions, onClick = onClick)
}

@Composable
fun SwitchPreference(title: String, summary: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit,
                     enabled: Boolean = true, modifier: Modifier = Modifier) {
    if (ArchiveUiTheme.isMaterial) BasicComponent(title,
        modifier.toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange), summary,
        endActions = { androidx.compose.material3.Switch(checked, onCheckedChange = null, enabled = enabled) },
        enabled = enabled)
    else if (ArchiveUiTheme.isCoui) io.github.suqi8.coui.kmp.preference.SwitchPreference(title = title, summary = summary,
        checked = checked, onCheckedChange = onCheckedChange, enabled = enabled,
        insideMargin = CouiLayout.RowPadding, modifier = modifier.heightIn(min = CouiLayout.RowMinHeight))
    else top.yukonga.miuix.kmp.preference.SwitchPreference(title = title, summary = summary,
        checked = checked, onCheckedChange = onCheckedChange, enabled = enabled, modifier = modifier)
}

@Composable
fun CheckboxPreference(title: String, summary: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit,
                       checkboxLocation: CheckboxLocation = CheckboxLocation.End, modifier: Modifier = Modifier) {
    if (ArchiveUiTheme.isMaterial) BasicComponent(title,
        modifier.toggleable(checked, role = Role.Checkbox, onValueChange = onCheckedChange), summary,
        endActions = { androidx.compose.material3.Checkbox(checked, onCheckedChange = null) })
    else if (ArchiveUiTheme.isCoui) io.github.suqi8.coui.kmp.preference.CheckboxPreference(title = title, summary = summary,
        checked = checked, onCheckedChange = onCheckedChange, insideMargin = CouiLayout.RowPadding,
        checkboxLocation = io.github.suqi8.coui.kmp.preference.CheckboxLocation.valueOf(checkboxLocation.name),
        modifier = modifier.heightIn(min = CouiLayout.RowMinHeight))
    else top.yukonga.miuix.kmp.preference.CheckboxPreference(title = title, summary = summary,
        checked = checked, onCheckedChange = onCheckedChange, checkboxLocation = checkboxLocation, modifier = modifier)
}

@Composable
fun OverlayDropdownPreference(title: String, items: List<String>, selectedIndex: Int,
                              onSelectedIndexChange: (Int) -> Unit, modifier: Modifier = Modifier,
                              summary: String? = null) {
    if (ArchiveUiTheme.isCoui) {
        io.github.suqi8.coui.kmp.preference.OverlayDropdownPreference(title = title, items = items,
            selectedIndex = selectedIndex, onSelectedIndexChange = onSelectedIndexChange,
            modifier = modifier.heightIn(min = CouiLayout.RowMinHeight),
            insideMargin = CouiLayout.RowPadding,
            bottomAction = summary?.let { { CouiPreferenceSummary(it) } })
        return
    }
    if (!ArchiveUiTheme.isMaterial) {
        top.yukonga.miuix.kmp.preference.OverlayDropdownPreference(title = title, items = items,
            selectedIndex = selectedIndex, onSelectedIndexChange = onSelectedIndexChange, modifier = modifier, summary = summary)
        return
    }
    var expanded by rememberSaveable { mutableStateOf(false) }
    BasicComponent(title, modifier.semantics { stateDescription = items.getOrElse(selectedIndex) { "" } }, summary,
        endActions = { Text(items.getOrElse(selectedIndex) { "" }, color = MaterialTheme.colorScheme.primary) },
        onClick = { expanded = true })
    if (expanded) androidx.compose.material3.AlertDialog(
        onDismissRequest = { expanded = false },
        title = { androidx.compose.material3.Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
                items.forEachIndexed { index, item ->
                    Row(Modifier.fillMaxWidth().selectable(index == selectedIndex, role = Role.RadioButton,
                        onClick = { expanded = false; onSelectedIndexChange(index) })
                        .padding(vertical = 12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        androidx.compose.material3.RadioButton(index == selectedIndex, onClick = null)
                        androidx.compose.material3.Text(item)
                    }
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { expanded = false }) {
            androidx.compose.material3.Text(AppText.get(R.string.ui_cancel))
        } },
    )
}
