package love.yu.jaychou.ui.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import love.yu.jaychou.ui.effects.backgroundCardBlur
import love.yu.jaychou.ui.effects.backgroundCardBlurEnabled
import love.yu.jaychou.ui.effects.BackgroundBlurSurfaceAlpha

/** Native controls selected at the presentation boundary; screen/data state is shared. */
@Composable
fun Text(
    text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified, fontWeight: FontWeight? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified, textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified, overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE, minLines: Int = 1,
    style: TextStyle = ArchiveUiTheme.textStyles.main,
) {
    if (ArchiveUiTheme.isMaterial) androidx.compose.material3.Text(
        text, modifier, color = color, fontSize = fontSize, fontWeight = fontWeight,
        letterSpacing = letterSpacing, textAlign = textAlign, lineHeight = lineHeight,
        overflow = overflow, maxLines = maxLines, minLines = minLines, style = style,
    ) else top.yukonga.miuix.kmp.basic.Text(
        text, modifier, color = color, fontSize = fontSize, fontWeight = fontWeight,
        letterSpacing = letterSpacing, textAlign = textAlign, lineHeight = lineHeight,
        overflow = overflow, maxLines = maxLines, minLines = minLines, style = style,
    )
}

object CardDefaults {
    @Composable
    fun defaultColors(
        color: Color = ArchiveUiTheme.colorScheme.surfaceContainer,
        contentColor: Color = ArchiveUiTheme.colorScheme.onSurfaceContainer,
    ) = CardColors(color, contentColor)
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = if (ArchiveUiTheme.isMaterial) 12.dp else top.yukonga.miuix.kmp.basic.CardDefaults.CornerRadius,
    insideMargin: PaddingValues = PaddingValues(0.dp),
    colors: CardColors = CardDefaults.defaultColors(),
    pressFeedbackType: PressFeedbackType = PressFeedbackType.None,
    showIndication: Boolean = false, onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val blurCards = backgroundCardBlurEnabled()
    val cardModifier = if (blurCards) modifier.backgroundCardBlur(RoundedCornerShape(cornerRadius)) else modifier
    // The native surface owns its tint even when the backdrop isn't ready or cannot draw.
    val cardColors = if (blurCards) CardColors(colors.color.copy(alpha = BackgroundBlurSurfaceAlpha), colors.contentColor) else colors
    if (ArchiveUiTheme.isMaterial) {
        val palette = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = cardColors.color, contentColor = cardColors.contentColor)
        if (onClick == null) androidx.compose.material3.Card(
            modifier = cardModifier, shape = RoundedCornerShape(cornerRadius), colors = palette,
        ) { Column(Modifier.padding(insideMargin), content = content) }
        else androidx.compose.material3.Card(
            onClick = onClick, modifier = cardModifier, shape = RoundedCornerShape(cornerRadius), colors = palette,
        ) { Column(Modifier.padding(insideMargin), content = content) }
    } else top.yukonga.miuix.kmp.basic.Card(
        cardModifier, cornerRadius, insideMargin, cardColors, pressFeedbackType, showIndication,
        onClick = onClick, content = content,
    )
}

@Composable
fun Button(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
           content: @Composable RowScope.() -> Unit) {
    if (ArchiveUiTheme.isMaterial) androidx.compose.material3.Button(onClick, modifier, enabled, content = content)
    else top.yukonga.miuix.kmp.basic.Button(onClick, modifier, enabled, content = content)
}

@Composable
fun TextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    if (ArchiveUiTheme.isMaterial) androidx.compose.material3.TextButton(onClick, modifier, enabled) {
        androidx.compose.material3.Text(text)
    } else top.yukonga.miuix.kmp.basic.TextButton(text, onClick = onClick, modifier = modifier, enabled = enabled)
}

@Composable
fun Icon(imageVector: ImageVector, contentDescription: String?, modifier: Modifier = Modifier,
         tint: Color = ArchiveUiTheme.colorScheme.onBackground) {
    if (ArchiveUiTheme.isMaterial) androidx.compose.material3.Icon(imageVector, contentDescription, modifier, tint)
    else top.yukonga.miuix.kmp.basic.Icon(imageVector, contentDescription, modifier, tint)
}

@Composable
fun IconButton(onClick: () -> Unit, modifier: Modifier = Modifier,
               backgroundColor: Color = Color.Transparent, minWidth: Dp = 48.dp, minHeight: Dp = 48.dp,
               content: @Composable () -> Unit) {
    if (ArchiveUiTheme.isMaterial) androidx.compose.material3.IconButton(onClick, modifier, content = content)
    else top.yukonga.miuix.kmp.basic.IconButton(onClick = onClick, modifier = modifier,
        backgroundColor = backgroundColor, minWidth = minWidth, minHeight = minHeight, content = content)
}

@Composable
fun SmallTitle(text: String, modifier: Modifier = Modifier,
               insideMargin: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
    if (ArchiveUiTheme.isMaterial) androidx.compose.material3.Text(text,
        modifier.padding(insideMargin), color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
    else top.yukonga.miuix.kmp.basic.SmallTitle(text, modifier, insideMargin = insideMargin)
}

@Composable
fun HorizontalDivider(modifier: Modifier = Modifier, color: Color = ArchiveUiTheme.colorScheme.dividerLine) {
    if (ArchiveUiTheme.isMaterial) androidx.compose.material3.HorizontalDivider(modifier, color = color)
    else top.yukonga.miuix.kmp.basic.HorizontalDivider(modifier, color = color)
}

@Composable
fun CircularProgressIndicator(modifier: Modifier = Modifier, size: Dp = if (ArchiveUiTheme.isMaterial) 40.dp else 30.dp) {
    if (ArchiveUiTheme.isMaterial) androidx.compose.material3.CircularProgressIndicator(modifier.size(size), strokeWidth = 3.dp)
    else top.yukonga.miuix.kmp.basic.CircularProgressIndicator(modifier = modifier, size = size)
}
