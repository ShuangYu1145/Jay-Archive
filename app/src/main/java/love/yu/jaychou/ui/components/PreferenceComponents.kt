package love.yu.jaychou.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import love.yu.jaychou.ui.designsystem.*
import love.yu.jaychou.ui.designsystem.SwitchPreference
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme

@Composable
internal fun PreferenceTitle(text: String) {
    SmallTitle(text, Modifier.padding(top = 18.dp).semantics { heading() },
        insideMargin = PaddingValues(start = 16.dp, top = 8.dp, bottom = 10.dp))
}

@Composable
internal fun PreferenceCard(content: @Composable ColumnScope.() -> Unit) {
    Card(insideMargin = PaddingValues(0.dp), content = content)
}

@Composable
internal fun PreferenceValue(value: String) {
    Text(value, style = ArchiveUiTheme.textStyles.body1, color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
}

@Composable
internal fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean) {
    SwitchPreference(title = title, summary = subtitle, checked = checked, onCheckedChange = onChange, enabled = enabled,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = title
            toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
            if (!enabled) disabled()
        })
}

