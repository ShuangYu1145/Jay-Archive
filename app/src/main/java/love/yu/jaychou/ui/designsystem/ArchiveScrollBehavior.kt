package love.yu.jaychou.ui.designsystem

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection

@OptIn(ExperimentalMaterial3Api::class)
class ArchiveScrollBehavior internal constructor(
    internal val miuix: top.yukonga.miuix.kmp.basic.ScrollBehavior? = null,
    internal val material: TopAppBarScrollBehavior? = null,
) {
    val nestedScrollConnection: NestedScrollConnection
        get() = material?.nestedScrollConnection ?: requireNotNull(miuix).nestedScrollConnection
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberArchiveScrollBehavior(): ArchiveScrollBehavior = if (ArchiveUiTheme.isMaterial) {
    val behavior = androidx.compose.material3.TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    remember(behavior) { ArchiveScrollBehavior(material = behavior) }
} else {
    val behavior = top.yukonga.miuix.kmp.basic.MiuixScrollBehavior()
    remember(behavior) { ArchiveScrollBehavior(miuix = behavior) }
}
