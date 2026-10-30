package love.yu.jaychou.ui.screens.lyrics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yu.jaychou.R
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.designsystem.*
import java.util.Locale

@Composable
internal fun LyricsReadingSheet(
    show: Boolean,
    settings: LyricsReadingSettings,
    translationAvailable: Boolean,
    canCopy: Boolean,
    onChange: (LyricsReadingSettings, Boolean) -> Unit,
    onCommit: () -> Unit,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onSearch: () -> Unit,
) {
    WindowBottomSheet(show, AppText.get(R.string.ui_reader_settings), onDismiss,
        endAction = { TextButton(AppText.get(R.string.ui_reader_done), onDismiss) },
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding()
            .padding(bottom = 16.dp).testTag("lyrics-reading-sheet"), verticalArrangement = Arrangement.spacedBy(if (ArchiveUiTheme.isCoui) 12.dp else 18.dp)) {
            Text(AppText.get(R.string.ui_reader_preview), Modifier.fillMaxWidth(),
                fontSize = settings.fontSize.sp, lineHeight = (settings.fontSize * settings.lineSpacing).sp,
                textAlign = settings.alignment.textAlign(), color = ArchiveUiTheme.colorScheme.onSurface)
            ReaderControlGroup {
                ReaderSlider(AppText.get(R.string.ui_reader_font_size), "${settings.fontSize.toInt()} sp",
                    settings.fontSize, 18f..32f, 13, "reader-font-size",
                    { onChange(settings.copy(fontSize = it), false) }, onCommit)
                PreferenceDivider()
                ReaderSlider(AppText.get(R.string.ui_reader_line_spacing), String.format(Locale.ROOT, "%.1f×", settings.lineSpacing),
                    settings.lineSpacing, 1.2f..2.2f, 9, "reader-line-spacing",
                    { onChange(settings.copy(lineSpacing = it), false) }, onCommit)
            }
            ReaderChoices(AppText.get(R.string.ui_reader_alignment),
                listOf(R.string.ui_reader_align_left, R.string.ui_reader_align_center, R.string.ui_reader_align_right),
                settings.alignment.ordinal, "reader-align") {
                onChange(settings.copy(alignment = LyricsAlignment.entries[it]), true)
            }
            ReaderChoices(AppText.get(R.string.ui_reader_background),
                listOf(R.string.ui_reader_background_system, R.string.ui_reader_background_album, R.string.ui_reader_background_plain),
                settings.background.ordinal, "reader-background") {
                onChange(settings.copy(background = LyricsBackground.entries[it]), true)
            }
            ReaderControlGroup {
                if (translationAvailable) {
                    SwitchPreference(
                        title = AppText.get(R.string.ui_bilingual), summary = AppText.get(R.string.ui_toggle_lyric_translations),
                        checked = settings.showTranslation,
                        onCheckedChange = { onChange(settings.copy(showTranslation = it), true) },
                        modifier = Modifier.testTag("reader-translation"),
                    )
                    PreferenceDivider()
                }
                SwitchPreference(title = AppText.get(R.string.ui_reader_immersive),
                    summary = AppText.get(R.string.ui_reader_immersive_hint), checked = settings.immersive,
                    onCheckedChange = { onChange(settings.copy(immersive = it), true) },
                    modifier = Modifier.testTag("reader-immersive"))
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(AppText.get(R.string.ui_reader_copy_all), onCopy, enabled = canCopy,
                    modifier = Modifier.testTag("reader-copy-all"))
                TextButton(AppText.get(R.string.ui_reader_search), onSearch, enabled = canCopy,
                    modifier = Modifier.testTag("reader-search"))
            }
            TextButton(AppText.get(R.string.ui_reader_reset),
                { onChange(LyricsReadingSettings(), true) }, Modifier.fillMaxWidth().testTag("reader-reset"))
        }
    }
}

@Composable
private fun ReaderSlider(label: String, valueLabel: String, value: Float,
                         range: ClosedFloatingPointRange<Float>, steps: Int, tag: String,
                         onChange: (Float) -> Unit, onFinished: () -> Unit) {
    Column(if (ArchiveUiTheme.isCoui) Modifier.padding(horizontal = 16.dp, vertical = 10.dp) else Modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(valueLabel, fontSize = 14.sp, color = ArchiveUiTheme.colorScheme.onBackgroundVariant)
        }
        val modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .semantics { contentDescription = label }.testTag(tag)
        if (ArchiveUiTheme.isMaterial) androidx.compose.material3.Slider(value, onChange,
            modifier = modifier, valueRange = range, steps = steps, onValueChangeFinished = onFinished)
        else if (ArchiveUiTheme.isCoui) io.github.suqi8.coui.kmp.basic.Slider(value, onChange,
            modifier = modifier, valueRange = range, steps = steps, onValueChangeFinished = onFinished)
        else top.yukonga.miuix.kmp.basic.Slider(value, onChange,
            modifier = modifier, valueRange = range, steps = steps, onValueChangeFinished = onFinished)
    }
}

@Composable
private fun ReaderChoices(label: String, options: List<Int>, selectedIndex: Int, tag: String, onSelect: (Int) -> Unit) {
    val colors = ArchiveUiTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { index, resource ->
                Card(onClick = { onSelect(index) }, cornerRadius = 16.dp,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("$tag-$index")
                        .semantics { selected = index == selectedIndex; role = Role.RadioButton },
                    insideMargin = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
                    colors = CardDefaults.defaultColors(
                        if (index == selectedIndex) colors.primaryContainer else colors.surfaceContainerHigh,
                        if (index == selectedIndex) colors.onPrimaryContainer else colors.onSurfaceContainer),
                ) { Text(AppText.get(resource), fontSize = 14.sp) }
            }
        }
    }
}

internal fun LyricsAlignment.textAlign() = when (this) {
    LyricsAlignment.LEFT -> TextAlign.Left
    LyricsAlignment.CENTER -> TextAlign.Center
    LyricsAlignment.RIGHT -> TextAlign.Right
}

/** Keep related COUI controls in a single surface, as in ColorOS sound settings. */
@Composable
private fun ReaderControlGroup(content: @Composable ColumnScope.() -> Unit) {
    if (ArchiveUiTheme.isCoui) Card(content = content)
    else Column(verticalArrangement = Arrangement.spacedBy(18.dp), content = content)
}
