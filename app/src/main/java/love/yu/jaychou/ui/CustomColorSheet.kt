package love.yu.jaychou.ui

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
    WindowBottomSheet(show, title = "自定义主题色", onDismissRequest = dismiss,
        startAction = { TextButton("取消", onClick = dismiss) },
        endAction = { TextButton("应用", onClick = { apply(draft); dismiss() }) },
    ) {
        Column(Modifier.fillMaxWidth().testTag("color-sheet"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            QuietText("关闭 Monet 时使用此颜色；深浅色均由 Miuix 根据种子色生成。")
            ColorPicker(Color(draft), onColorChanged = { draft = it.toArgb() }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("蓝" to 0xFF4979F5, "紫" to 0xFF8456CF, "绿" to 0xFF2D8B70, "橙" to 0xFFBD6535).forEachIndexed { index, (name, color) ->
                    TextButton(name, onClick = { draft = color.toInt() }, modifier = Modifier.testTag("color-preset-$index"))
                }
            }
            TextButton("恢复预设蓝色", onClick = { apply(null); dismiss() }, modifier = Modifier.fillMaxWidth())
        }
    }
}
