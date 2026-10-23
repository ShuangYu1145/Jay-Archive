package love.yu.jaychou.ui

import love.yu.jaychou.data.AppText
import love.yu.jaychou.R
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.window.WindowBottomSheet

@Composable
fun CustomColorSheet(show: Boolean, current: Int?, dismiss: () -> Unit, apply: (Int?) -> Unit) {
    var draft by rememberSaveable(show, current) { mutableIntStateOf(current ?: 0xFF4979F5.toInt()) }
    WindowBottomSheet(show, title = AppText.get(R.string.ui_custom_theme_color), onDismissRequest = dismiss,
        startAction = { TextButton(AppText.get(R.string.ui_cancel), onClick = dismiss) },
        endAction = { TextButton(AppText.get(R.string.ui_apply), onClick = { apply(draft); dismiss() }) },
    ) {
        Column(Modifier.fillMaxWidth().testTag("color-sheet"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            QuietText(AppText.get(R.string.ui_used_when_monet_is_off_miuix_generates_light_and_dark_palettes_f))
            ColorPicker(Color(draft), onColorChanged = { draft = it.toArgb() }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(AppText.get(R.string.ui_blue) to 0xFF4979F5, AppText.get(R.string.ui_purple) to 0xFF8456CF, AppText.get(R.string.ui_green) to 0xFF2D8B70, AppText.get(R.string.ui_orange) to 0xFFBD6535).forEachIndexed { index, (name, color) ->
                    TextButton(name, onClick = { draft = color.toInt() }, modifier = Modifier.testTag("color-preset-$index"))
                }
            }
            TextButton(AppText.get(R.string.ui_restore_default_blue), onClick = { apply(null); dismiss() }, modifier = Modifier.fillMaxWidth())
        }
    }
}
