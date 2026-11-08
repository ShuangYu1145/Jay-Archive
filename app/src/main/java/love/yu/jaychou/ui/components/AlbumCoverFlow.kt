package love.yu.jaychou.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.RoundedCornerShape
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import love.yu.jaychou.R
import love.yu.jaychou.data.Album
import love.yu.jaychou.data.AppText
import love.yu.jaychou.ui.animation.AlbumMotionSource
import love.yu.jaychou.ui.animation.AlbumOrigin
import love.yu.jaychou.ui.designsystem.Text
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * A bounded, virtualized 3D shelf. Only the visible neighborhood is composed. Fractional
 * position is read during placement/drawing, so frames do not recompose the artwork.
 * Native scroll arbitration owns drag, fling and click animations (including cancellation).
 */
@Composable
fun AlbumCoverFlow(
    albums: List<Album>,
    onAlbum: (Album, AlbumOrigin) -> Unit,
    modifier: Modifier = Modifier,
    onSelectedAlbum: (Album) -> Unit = {},
) {
    var savedId by rememberSaveable { mutableStateOf<String?>(null) }
    val ids = remember(albums) { albums.map { it.id } }
    // Reconcile by identity after filtering/reordering, cancelling obsolete animations.
    val state = remember(ids) {
        CoverFlowScrollState(albums.size, ids.indexOf(savedId).coerceAtLeast(0))
    }
    val scope = rememberCoroutineScope()
    val selectionCallback by rememberUpdatedState(onSelectedAlbum)
    val currentAlbums by rememberUpdatedState(albums)
    LaunchedEffect(state) {
        snapshotFlow { state.selectedIndex }.distinctUntilChanged().collect { index ->
            savedId = currentAlbums.getOrNull(index)?.id
        }
    }
    LaunchedEffect(state, albums) {
        snapshotFlow { state.selectedIndex.takeUnless { state.scroll.isScrollInProgress } }
            .distinctUntilChanged().collect { index ->
                index?.let { currentAlbums.getOrNull(it) }?.let(selectionCallback)
            }
    }
    val selectedIndex by remember(state) { derivedStateOf { state.selectedIndex } }
    val selected = albums.getOrNull(selectedIndex)
    val selectedLabel = stringResource(R.string.ui_cover_flow_selected)
    val openLabel = stringResource(R.string.ui_cover_flow_open)
    val previousLabel = stringResource(R.string.ui_cover_flow_previous)
    val nextLabel = stringResource(R.string.ui_cover_flow_next)
    val colors = ArchiveUiTheme.colorScheme
    val openActions = remember(state) { mutableMapOf<String, () -> Unit>() }
    BoxWithConstraints(modifier.background(Brush.verticalGradient(listOf(
        lerp(colors.background, colors.surfaceContainerHighest, .3f), colors.background)))
        .testTag("album-cover-flow")) {
        val compact = maxWidth > maxHeight
        val coverSize = minOf(maxWidth * .64f,
            (maxHeight - if (compact) 100.dp else 160.dp).coerceAtLeast(1.dp) * .85f,
            440.dp).coerceAtLeast(1.dp)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(coverSize * 1.2f).clipToBounds()
            .testTag("cover-flow-shelf")) {
            val density = LocalDensity.current
            val coverPx = with(density) { coverSize.roundToPx() }.coerceAtLeast(1)
            val stride = coverPx * .30f
            SideEffect { state.stride = stride.coerceAtLeast(1f) }
            val radius = ceil(with(density) { maxWidth.toPx() } / stride.coerceAtLeast(1f) / 2f)
                .toInt().coerceIn(3, 12) + 2
            val anchor by remember(state) { derivedStateOf { state.selectedIndex } }
            val range = maxOf(0, anchor - radius)..minOf(albums.lastIndex, anchor + radius)
            val fling = remember(state) { CoverFlowFling(state) }
            Layout(
                modifier = Modifier.fillMaxSize()
                    .scrollable(state.scroll, Orientation.Horizontal, reverseDirection = true,
                        flingBehavior = fling, enabled = albums.size > 1)
                    .pointerInput(state, range, coverPx) {
                        val geometry = CdCaseGeometry()
                        detectTapGestures { tap ->
                            // Hit the projected solid, not its unrotated rectangular layout slot.
                            val index = range.sortedBy { abs(it - state.position) }.firstOrNull { i ->
                                val d = i - state.position
                                geometry.update(coverPx.toFloat(), caseYaw(d), casePitch(d), caseScale(d))
                                geometry.contains(tap.x - (size.width - coverPx) / 2f - caseTranslation(d, coverPx.toFloat()),
                                    tap.y - (size.height - coverPx) / 2f)
                            }
                            if (index != null) {
                                if (index == state.selectedIndex && !state.scroll.isScrollInProgress)
                                    openActions[albums[index].id]?.invoke()
                                else scope.launch { state.select(index) }
                            }
                        }
                    }
                    .semantics {
                        collectionInfo = CollectionInfo(1, albums.size)
                        horizontalScrollAxisRange = ScrollAxisRange(
                            value = { state.position }, maxValue = { albums.lastIndex.coerceAtLeast(0).toFloat() })
                        customActions = listOf(
                            CustomAccessibilityAction(previousLabel) {
                                scope.launch { state.select(selectedIndex - 1) }; selectedIndex > 0
                            },
                            CustomAccessibilityAction(nextLabel) {
                                scope.launch { state.select(selectedIndex + 1) }; selectedIndex < albums.lastIndex
                            },
                        )
                    },
                content = {
                    for (index in range) key(albums[index].id) {
                        val album = albums[index]
                        AlbumMotionSource(album, onAlbum) { capture, coverCapture, open ->
                            SideEffect { openActions[album.id] = open }
                            DisposableEffect(album.id) { onDispose { openActions.remove(album.id) } }
                            CdCaseArtwork(album.title, album.year, { index - state.position }, capture.fillMaxSize()) {
                                CatalogImage(
                                    album.coverUrl,
                                    AppText.get(R.string.ui_album_artwork_source, album.title, album.sourceLabel),
                                    coverCapture.fillMaxSize().clip(RoundedCornerShape(1.dp))
                                        .testTag("cover-flow-${album.id}")
                                        .semantics {
                                            this.selected = index == selectedIndex
                                            role = Role.Button
                                            if (index == selectedIndex) stateDescription = selectedLabel
                                            onClick(if (index == selectedIndex) openLabel else album.title) {
                                                if (index == state.selectedIndex && !state.scroll.isScrollInProgress) open()
                                                else scope.launch { state.select(index) }
                                                true
                                            }
                                        },
                                    contentScale = ContentScale.Crop, crossfade = false,
                                )
                            }
                        }
                    }
                },
            ) { measurables, constraints ->
                val covers = measurables.map { it.measure(Constraints.fixed(coverPx, coverPx)) }
                layout(constraints.maxWidth, constraints.maxHeight) {
                    covers.forEachIndexed { child, cover ->
                        val distance = range.first + child - state.position
                        val depth = abs(distance)
                        cover.placeWithLayer(
                            (constraints.maxWidth - cover.width) / 2,
                            (constraints.maxHeight - cover.height) / 2,
                            zIndex = -depth,
                        ) {
                            translationX = caseTranslation(distance, coverPx.toFloat())
                            alpha = (1f - .035f * depth).coerceIn(.55f, 1f)
                            clip = false
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = if (compact) 6.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 7.dp)) {
            if (selected != null) {
                Text(selected.title, Modifier.testTag("cover-flow-title"), color = colors.onBackground,
                    fontSize = if (compact) 20.sp else 25.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                // This catalog's album artist is Jay Chou; track credits can include guest artists.
                Text(AppText.fromOriginal("周杰伦"), color = colors.onBackgroundVariant, fontSize = 15.sp)
                Text(listOfNotNull(selected.year.takeIf { it.length == 4 && it.all(Char::isDigit) },
                    "${selectedIndex + 1} / ${albums.size}").joinToString("  ·  "),
                    color = colors.onBackgroundVariant, fontSize = 12.sp,
                    modifier = Modifier.semantics { stateDescription = selectedLabel })
            } else {
                Text(stringResource(R.string.ui_cover_flow_empty), color = colors.onBackgroundVariant)
            }
        }
    }
    }
}

internal class CoverFlowScrollState(val count: Int, initialIndex: Int = 0) {
    var position by mutableFloatStateOf(initialIndex.coerceIn(0, (count - 1).coerceAtLeast(0)).toFloat())
        private set
    var stride = 1f
    val selectedIndex get() = position.roundToInt().coerceIn(0, (count - 1).coerceAtLeast(0))
    val scroll = ScrollableState { delta ->
        val before = position
        position = (before + delta / stride).coerceIn(0f, (count - 1).coerceAtLeast(0).toFloat())
        (position - before) * stride
    }

    suspend fun select(index: Int) {
        if (count == 0) return
        scroll.scroll(MutatePriority.Default) { settle(index.coerceIn(0, count - 1), 0f) }
    }

    suspend fun ScrollScope.settle(index: Int, velocity: Float) {
        // Animate album units so resizing the viewport during a snap keeps its target centered.
        animate(position, index.toFloat(), initialVelocity = velocity / stride,
            animationSpec = spring(dampingRatio = 1f, stiffness = 260f, visibilityThreshold = .001f)) { value, _ ->
            scrollBy((value - position) * stride)
        }
        scrollBy((index - position) * stride)
    }
}

private class CoverFlowFling(private val state: CoverFlowScrollState) : FlingBehavior {
    private val decay = exponentialDecay<Float>(frictionMultiplier = 1.5f)
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        if (state.count <= 1) return initialVelocity
        if ((state.position == 0f && initialVelocity < 0f) ||
            (state.position == (state.count - 1).toFloat() && initialVelocity > 0f)) return initialVelocity
        val projected = decay.calculateTargetValue(state.position * state.stride, initialVelocity)
        val target = (projected / state.stride).roundToInt().coerceIn(0, state.count - 1)
        with(state) { settle(target, initialVelocity) }
        return 0f
    }
}
