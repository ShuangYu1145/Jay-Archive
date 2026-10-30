package love.yu.jaychou.ui.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared metrics for ColorOS-style grouped lists and their content insets. */
internal object CouiLayout {
    val ActionSize = 56.dp
    val PageInset = 16.dp
    val CardRadius = 20.dp
    val GroupSpacing = 16.dp
    val RowMinHeight = 52.dp
    val RowPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
}

/** Only between actual options: never above the first or below the last row. */
@Composable
internal fun PreferenceDivider() {
    if (ArchiveUiTheme.isCoui) {
        HorizontalDivider(Modifier.padding(horizontal = CouiLayout.PageInset))
    }
}

/** Descriptions span the row instead of being squeezed beside a selected value. */
@Composable
internal fun CouiPreferenceSummary(summary: String) {
    Text(summary, fontSize = 13.sp, color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
}
