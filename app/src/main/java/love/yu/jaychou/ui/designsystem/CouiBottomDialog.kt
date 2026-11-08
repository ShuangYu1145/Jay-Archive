package love.yu.jaychou.ui.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.layout.DialogButtonBarAction
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.window.WindowDialog

/** Floating ColorOS panel; the native host retains its dim, IME and back-gesture behavior. */
@Composable
internal fun CouiBottomDialog(
    show: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    actions: @Composable () -> Unit,
) {
    WindowDialog(
        show = show, onDismissRequest = onDismissRequest,
        modifier = modifier.heightIn(max = LocalWindowInfo.current.containerDpSize.height * .85f),
        backgroundColor = ArchiveUiTheme.colorScheme.surfaceContainer,
        outsideMargin = DpSize(16.dp, 12.dp),
        cornerRadius = 40.dp,
        largeScreen = false,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 28.dp, bottom = 16.dp)) {
            // Only the body scrolls, leaving the capsule actions accessible in short windows.
            Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp)) {
                Text(title, Modifier.fillMaxWidth(), fontSize = 20.sp, lineHeight = 28.sp,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                summary?.let {
                    Text(it, Modifier.fillMaxWidth().padding(top = 8.dp), fontSize = 15.sp, lineHeight = 22.sp,
                        color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant, textAlign = TextAlign.Center)
                }
                if (content != null) Column(Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
            }
            Spacer(Modifier.height(24.dp))
            actions()
        }
    }
}

/** Short actions sit side by side; longer labels stack with the cancel action last. */
@Composable
internal fun CouiDialogActions(
    positive: DialogButtonBarAction,
    negative: DialogButtonBarAction? = null,
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
    destructive: Boolean = false,
    positiveModifier: Modifier = Modifier,
    negativeModifier: Modifier = Modifier,
) {
    val dark = ArchiveUiTheme.colorScheme.background.luminance() < .5f
    val labelStyle = COUITheme.textStyles.button.copy(
        fontSize = 18.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
    )
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val fill = if (dark) Color(0xFF363638) else Color(0xFFF0F1F2)
    val confirmColor = if (destructive) {
        if (dark) Color(0xFFFF625B) else Color(0xFFD93025)
    } else ArchiveUiTheme.colorScheme.primary
    val button: @Composable (DialogButtonBarAction, Boolean, Modifier) -> Unit = { action, confirm, buttonModifier ->
        TextButton(
            text = action.text, onClick = action.onClick, enabled = action.enabled,
            modifier = buttonModifier, minHeight = 48.dp, cornerRadius = 100.dp,
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 10.dp), textStyle = labelStyle,
            colors = ButtonDefaults.textButtonColors(color = fill,
                textColor = if (confirm) confirmColor else ArchiveUiTheme.colorScheme.onSurfaceContainer),
        )
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cellWidthPx = with(density) { ((maxWidth - 8.dp) / 2 - 32.dp).toPx() }
        val vertical = stacked || (negative != null && listOf(positive, negative).any {
            textMeasurer.measure(it.text, labelStyle, softWrap = false, maxLines = 1).size.width > cellWidthPx
        })
        if (vertical) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                button(positive, true, positiveModifier.fillMaxWidth())
                negative?.let { button(it, false, negativeModifier.fillMaxWidth()) }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                negative?.let { button(it, false, negativeModifier.weight(1f)) }
                button(positive, true, positiveModifier.weight(1f))
            }
        }
    }
}
