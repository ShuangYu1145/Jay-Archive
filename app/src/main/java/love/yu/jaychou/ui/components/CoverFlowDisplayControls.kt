package love.yu.jaychou.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import love.yu.jaychou.R
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import love.yu.jaychou.ui.designsystem.Card
import love.yu.jaychou.ui.designsystem.IconButton
import love.yu.jaychou.ui.designsystem.Text
import love.yu.jaychou.ui.designsystem.TextButton

/** Kept on the shelf even when both surrounding app bars are hidden. */
@Composable
internal fun CoverFlowDisplayControls(headerHidden: Boolean, navigationHidden: Boolean,
                                     onHeaderHidden: (Boolean) -> Unit, onNavigationHidden: (Boolean) -> Unit,
                                     modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val visibility = remember { MutableTransitionState(false) }
    LaunchedEffect(expanded) { visibility.targetState = expanded }
    val colors = ArchiveUiTheme.colorScheme
    val label = stringResource(R.string.ui_cover_flow_display)
    Box(modifier) {
        val buttonModifier = Modifier.size(48.dp)
            .semantics { contentDescription = label }.testTag("cover-flow-display")
        val icon: @Composable () -> Unit = {
            Canvas(Modifier.size(22.dp)) {
                val w = size.width; val h = size.height; val length = w * .3f
                for ((x, y) in listOf(0f to 0f, w to 0f, w to h, 0f to h)) {
                    drawLine(colors.onBackground, Offset(x, y), Offset(if (x == 0f) length else w - length, y),
                        strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(colors.onBackground, Offset(x, y), Offset(x, if (y == 0f) length else h - length),
                        strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                }
            }
        }
        if (ArchiveUiTheme.isCoui) {
            love.yu.jaychou.ui.designsystem.CouiToolbarButton(
                { expanded = !expanded }, buttonModifier, content = icon)
        } else SectionHeaderActions(singleButton = true) {
            IconButton(onClick = { expanded = !expanded }, modifier = buttonModifier, content = icon)
        }
        if (visibility.currentState || visibility.targetState) Popup(
            alignment = Alignment.TopEnd, onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = true)) {
            AnimatedVisibility(visibleState = visibility,
                enter = fadeIn(tween(140)) + scaleIn(spring(dampingRatio = .9f, stiffness = 600f),
                    initialScale = .86f, transformOrigin = TransformOrigin(1f, 0f)),
                exit = fadeOut(tween(120)) + scaleOut(tween(160), targetScale = .92f,
                    transformOrigin = TransformOrigin(1f, 0f))) {
            Card(Modifier.widthIn(max = 300.dp).padding(12.dp).testTag("cover-flow-display-panel"),
                insideMargin = PaddingValues(12.dp)) {
                Text(label, Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                SettingSwitch(stringResource(R.string.ui_cover_flow_show_header), "", !headerHidden,
                    { onHeaderHidden(!it) }, true)
                SettingSwitch(stringResource(R.string.ui_cover_flow_show_navigation), "", !navigationHidden,
                    { onNavigationHidden(!it) }, true)
                TextButton(stringResource(R.string.ui_ok), { expanded = false },
                    Modifier.align(Alignment.End).testTag("cover-flow-display-done"))
            }
            }
        }
    }
}
