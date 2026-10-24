package love.yu.jaychou.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import love.yu.jaychou.R
import love.yu.jaychou.data.AppText

@Composable
fun ColorPicker(color: Color, onColorChanged: (Color) -> Unit, modifier: Modifier = Modifier) {
    if (!ArchiveUiTheme.isMaterial) {
        top.yukonga.miuix.kmp.basic.ColorPicker(color, onColorChanged = onColorChanged, modifier = modifier)
        return
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().height(48.dp).background(color, RoundedCornerShape(16.dp)))
        androidx.compose.material3.Text("#%06X".format(color.toArgb() and 0xFFFFFF),
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        listOf(R.string.ui_color_red, R.string.ui_color_green, R.string.ui_color_blue).forEachIndexed { index, label ->
            val name = AppText.get(label)
            val value = when (index) { 0 -> color.red; 1 -> color.green; else -> color.blue }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.Text(name, Modifier.widthIn(min = 40.dp))
                androidx.compose.material3.Slider(value, { next ->
                    onColorChanged(when (index) {
                        0 -> color.copy(red = next)
                        1 -> color.copy(green = next)
                        else -> color.copy(blue = next)
                    })
                }, Modifier.weight(1f).semantics { contentDescription = name })
            }
        }
    }
}
