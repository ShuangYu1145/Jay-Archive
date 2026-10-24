package love.yu.jaychou.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import love.yu.jaychou.ui.designsystem.Text

/** Modest, consistent rounding for album art at every navigation endpoint. */
val AlbumCoverShape = RoundedCornerShape(12.dp)

/** The track title remains unchanged for search, matching and accessibility. */
@Composable
fun LeadTrackStar(modifier: Modifier = Modifier) {
    Text("🌟", modifier.clearAndSetSemantics { }, fontSize = 16.sp)
}
