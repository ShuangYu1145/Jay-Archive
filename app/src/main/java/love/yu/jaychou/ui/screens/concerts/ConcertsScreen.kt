package love.yu.jaychou.ui.screens.concerts

import love.yu.jaychou.ui.components.ArchiveScaffold
import love.yu.jaychou.ui.components.BackButton
import love.yu.jaychou.ui.components.QuietText
import love.yu.jaychou.ui.components.SectionTitle
import love.yu.jaychou.ui.components.SourceLink
import love.yu.jaychou.data.AppText
import love.yu.jaychou.R
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import love.yu.jaychou.data.*
import love.yu.jaychou.ui.designsystem.*
import love.yu.jaychou.ui.designsystem.ArchiveUiTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun ConcertsScreen(bottom: Dp, onDetailVisibility: (Boolean) -> Unit, sectionHeader: Boolean = false) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val behavior = rememberArchiveScrollBehavior()
    var loadAttempt by remember { mutableIntStateOf(0) }
    var snapshot by remember { mutableStateOf<ConcertSnapshot?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshNote by remember { mutableStateOf<String?>(null) }
    var mode by rememberSaveable { mutableStateOf("upcoming") }
    var country by rememberSaveable { mutableStateOf("all") }
    var city by rememberSaveable { mutableStateOf("all_cities") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailConcert by remember { mutableStateOf<Concert?>(null) }
    var now by remember { mutableStateOf(ZonedDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = ZonedDateTime.now()
        }
    }

    LaunchedEffect(loadAttempt) {
        loading = true
        error = null
        try {
            snapshot = ConcertRepository.load()
            refreshNote = null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = AppText.get(R.string.ui_unable_to_connect_to_the_official_schedule_check_your_connection)
        } finally {
            loading = false
        }
    }

    val selected = snapshot?.concerts?.firstOrNull { it.id == selectedId }
    LaunchedEffect(selectedId, selected, snapshot) {
        if (selectedId != null && snapshot != null && selected == null) selectedId = null
        if (selected != null) detailConcert = selected
        onDetailVisibility(selected != null)
    }
    DisposableEffect(Unit) {
        onDispose { onDetailVisibility(false) }
    }
    BackHandler(enabled = selected != null) { selectedId = null }

    val refreshNow: () -> Unit = {
        val base = snapshot
        if (base == null) {
            loadAttempt++
        } else if (!refreshing) {
            scope.launch {
                refreshing = true
                try {
                    snapshot = ConcertRepository.load()
                    refreshNote = null
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    refreshNote = AppText.get(R.string.ui_update_failed_the_loaded_schedule_is_still_available_please_try)
                } finally {
                    refreshing = false
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        ArchiveScaffold(
            AppText.get(R.string.ui_concerts),
            behavior = behavior,
            sectionHeader = sectionHeader,
            actions = {
                if (sectionHeader) {
                    Box(
                        Modifier.fillMaxWidth().height(48.dp)
                            .clickable(enabled = !loading && !refreshing, onClick = refreshNow)
                            .testTag("concerts-refresh"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            AppText.get(when {
                                refreshing -> R.string.ui_updating
                                loading -> R.string.ui_loading
                                else -> R.string.ui_refresh
                            }),
                            fontSize = 14.sp, maxLines = 1,
                            color = if (loading || refreshing) ArchiveUiTheme.colorScheme.primary
                                else ArchiveUiTheme.colorScheme.onBackground,
                        )
                    }
                } else if (refreshing) {
                    Text(AppText.get(R.string.ui_updating), modifier = Modifier.padding(horizontal = 12.dp),
                        fontSize = 13.sp, color = ArchiveUiTheme.colorScheme.primary)
                } else {
                    TextButton(if (loading) AppText.get(R.string.ui_loading) else AppText.get(R.string.ui_refresh), onClick = { if (!loading) refreshNow() }, modifier = Modifier.testTag("concerts-refresh"))
                }
            },
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
                    .then(if (sectionHeader) Modifier else Modifier.nestedScroll(behavior.nestedScrollConnection))
                    .testTag("concerts-list"),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 12.dp,
                    bottom = bottom + 20.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item("concert-intro") {
                    ConcertIntro(
                        updated = snapshot?.updatedLabel,
                        refreshing = refreshing,
                        refreshNote = refreshNote,
                    )
                }

                if (loading) {
                    item("concert-loading") {
                        Card(insideMargin = PaddingValues(22.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                CircularProgressIndicator()
                                Column {
                                    Text(AppText.get(R.string.ui_loading_concert_schedule), fontSize = 16.sp)
                                    Text(AppText.get(R.string.ui_connecting_to_the_official_jvr_schedule_please_wait), fontSize = 12.sp,
                                        color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
                                }
                            }
                        }
                    }
                }

                if (error != null) {
                    item("concert-error") {
                        Card(insideMargin = PaddingValues(22.dp)) {
                            Text(AppText.get(R.string.ui_schedule_unavailable), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Text(error!!, fontSize = 14.sp, lineHeight = 21.sp,
                                color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = { loadAttempt++ }, modifier = Modifier.testTag("concerts-retry")) {
                                Text(AppText.get(R.string.ui_retry))
                            }
                        }
                    }
                }

                val data = snapshot
                if (data != null) {
                    val next = data.next(now)
                    if (next != null) {
                        item("concert-featured") {
                            FeaturedConcertCard(next, now) { detailConcert = next; selectedId = next.id }
                        }
                    } else if (!loading) {
                        item("concert-none") {
                            Card(insideMargin = PaddingValues(22.dp)) {
                                Text(AppText.get(R.string.ui_no_announced_shows), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(6.dp))
                                Text(AppText.get(R.string.ui_new_shows_will_appear_after_you_refresh_once_they_are_officially), fontSize = 13.sp,
                                    color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
                            }
                        }
                    }

                    item("concert-calendar-title") {
                        Column(Modifier.padding(top = 4.dp)) {
                            SectionTitle(AppText.get(R.string.ui_tour_calendar), AppText.get(R.string.ui_announced_shows_by_date_tap_for_details))
                            Spacer(Modifier.height(10.dp))
                            FilterRow(
                                title = AppText.get(R.string.ui_shows),
                                labels = listOf("upcoming", "all", "ended"),
                                selected = mode,
                                tagPrefix = "concert-mode",
                            ) { mode = it }
                            Spacer(Modifier.height(8.dp))
                            FilterRow(
                                title = AppText.get(R.string.ui_country_region),
                                labels = listOf("all") + data.countries,
                                selected = country,
                                tagPrefix = "concert-country",
                            ) { value ->
                                country = value
                                city = "all_cities"
                            }
                            val cities = if (country == "all") emptyList() else data.cities(country)
                            if (cities.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                FilterRow(
                                    title = AppText.get(R.string.ui_city),
                                    labels = listOf("all_cities") + cities,
                                    selected = city,
                                    tagPrefix = "concert-city",
                                ) { city = it }
                            }
                        }
                    }

                    val filtered = data.concerts
                        .filter {
                            val status = it.status(now)
                            when (mode) {
                                "upcoming" -> status != ShowStatus.ENDED && status != ShowStatus.CANCELLED
                                "ended" -> status == ShowStatus.ENDED || status == ShowStatus.CANCELLED
                                else -> true
                            }
                        }
                        .filter { country == "all" || it.venue.country == country }
                        .filter { city == "all_cities" || it.venue.city == city }
                    if (filtered.isEmpty()) {
                        item("concert-filter-empty") {
                            Card(insideMargin = PaddingValues(22.dp)) {
                                Text(AppText.get(R.string.ui_no_shows_match_these_filters), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(6.dp))
                                Text(AppText.get(R.string.ui_try_another_country_or_city_filter), fontSize = 13.sp,
                                    color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
                            }
                        }
                    } else {
                        var lastMonth: YearMonth? = null
                        filtered.forEach { concert ->
                            val month = YearMonth.from(concert.date)
                            if (month != lastMonth) {
                                item("concert-month-$month") {
                                    MonthHeader(month, filtered.count { YearMonth.from(it.date) == month })
                                }
                                lastMonth = month
                            }
                            item("concert-${concert.id}") {
                                Box(Modifier.animateItem()) {
                                    ConcertRow(concert, now) { detailConcert = concert; selectedId = concert.id }
                                }
                            }
                        }
                    }

                    item("concert-source") {
                        Column(Modifier.padding(top = 6.dp)) {
                            QuietText(AppText.get(R.string.ui_schedules_are_read_from_jvr_announcements_from_the_past_six_mont))
                            SourceLink(AppText.get(R.string.ui_official_jvr_schedule), ConcertRepository.source)
                            CompactSourceLink(AppText.get(R.string.ui_live_photos_jvr_music), concertPhotoSource)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selected != null,
            enter = fadeIn(tween(180)) + scaleIn(initialScale = .96f, animationSpec = tween(220)),
            exit = fadeOut(tween(150)) + scaleOut(targetScale = .98f, animationSpec = tween(180)),
            modifier = Modifier.fillMaxSize().zIndex(4f),
        ) {
            detailConcert?.let { concert ->
                ConcertDetailScreen(
                    concert = concert,
                    updatedLabel = snapshot?.updatedLabel ?: concert.updatedLabel,
                    now = now,
                    onBack = { selectedId = null },
                )
            }
        }
    }
}

@Composable
private fun ConcertIntro(updated: String?, refreshing: Boolean, refreshNote: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ConcertPhoto(Modifier.fillMaxWidth().height(200.dp))
        Text("JAY CHOU · CARNIVAL II", fontSize = 11.sp, letterSpacing = 1.8.sp,
            fontWeight = FontWeight.SemiBold, color = ArchiveUiTheme.colorScheme.primary)
        Text(AppText.get(R.string.ui_on_tour), fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp,
            modifier = Modifier.semantics { heading() })
        QuietText(AppText.get(R.string.ui_a_different_city_another_story_explore_the_officially_announced))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(AppText.get(R.string.ui_updated, updated ?: AppText.get(R.string.ui_loading_250)), fontSize = 12.sp,
                color = ArchiveUiTheme.colorScheme.onBackgroundVariant)
            if (refreshing) {
                Text(AppText.get(R.string.ui_checking_the_official_jvr_schedule), fontSize = 12.sp,
                    color = ArchiveUiTheme.colorScheme.primary)
            }
        }
        if (refreshNote != null) {
            Text(refreshNote, fontSize = 12.sp, lineHeight = 18.sp,
                color = ArchiveUiTheme.colorScheme.onBackgroundVariant)
        }
    }
}

@Composable
private fun FeaturedConcertCard(concert: Concert, now: ZonedDateTime, onClick: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val status = concert.status(now)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("concert-featured"),
        insideMargin = PaddingValues(22.dp),
        pressFeedbackType = PressFeedbackType.Sink,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(AppText.get(R.string.ui_next_stop), fontSize = 12.sp, letterSpacing = 1.4.sp,
                fontWeight = FontWeight.SemiBold, color = ArchiveUiTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            ConcertStatusChip(concert, now)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${concert.theme} · ${AppText.fromOriginal(concert.venue.city)}", fontSize = 29.sp, lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(AppText.fromOriginal(concert.tourName), fontSize = 13.sp, lineHeight = 19.sp,
                    color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(concert.countdown(now), fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = ArchiveUiTheme.colorScheme.primary)
                Text(concert.date.format(DateTimeFormatter.ofPattern(AppText.get(R.string.ui_mmm_d), AppText.locale)),
                    fontSize = 12.sp, color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("${concert.dateLabel} · ${concert.timeLabel}", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text("${AppText.fromOriginal(concert.venue.name)} · ${AppText.fromOriginal(concert.venue.country)}", fontSize = 13.sp, lineHeight = 20.sp,
            color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant, maxLines = 2,
            overflow = TextOverflow.Ellipsis)
        Text(concert.venue.address, fontSize = 12.sp, lineHeight = 18.sp,
            color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant, maxLines = 2,
            overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(AppText.get(R.string.ui_show_details), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = ArchiveUiTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            if (concert.ticketUrl != null &&
                (status == ShowStatus.UPCOMING || status == ShowStatus.TODAY) &&
                concert.ticketState != TicketState.PENDING &&
                concert.ticketState != TicketState.SOLD_OUT
            ) {
                Text(
                    AppText.get(R.string.ui_tickets, concert.ticketPlatform ?: AppText.get(R.string.ui_ticket_platform)),
                    modifier = Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button) {
                        uriHandler.openUri(concert.ticketUrl)
                    }.padding(vertical = 4.dp, horizontal = 2.dp),
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = ArchiveUiTheme.colorScheme.primary,
                )
            } else if (concert.priceRange != null) {
                Text(AppText.get(R.string.ui_prices_announced), fontSize = 11.sp, color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
            }
        }
    }
}

@Composable
private fun ConcertRow(concert: Concert, now: ZonedDateTime, onClick: () -> Unit) {
    val status = concert.status(now)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().animateContentSize().testTag("concert-row-${concert.id}"),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        pressFeedbackType = PressFeedbackType.Sink,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.width(52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(concert.date.month.getDisplayName(TextStyle.SHORT, AppText.locale), fontSize = 11.sp,
                    color = ArchiveUiTheme.colorScheme.primary)
                Text(concert.date.dayOfMonth.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(concert.date.dayOfWeek.getDisplayName(TextStyle.SHORT, AppText.locale),
                    fontSize = 10.sp, color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(AppText.fromOriginal(concert.venue.city), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(" · ${concert.theme}", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
                }
                Text(AppText.fromOriginal(concert.venue.name), fontSize = 13.sp, lineHeight = 19.sp,
                    color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${concertTime(concert, now)} · ${AppText.fromOriginal(concert.venue.country)}", fontSize = 12.sp,
                    color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                ConcertStatusChip(concert, now)
                if (status == ShowStatus.UPCOMING || status == ShowStatus.TODAY) {
                    Text(concert.countdown(now), fontSize = 11.sp,
                        color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
                }
            }
        }
    }
}

@Composable
private fun ConcertStatusChip(concert: Concert, now: ZonedDateTime) {
    val label = concert.ticketLabel(now)
    val colors = when {
        concert.status(now) in listOf(ShowStatus.ENDED, ShowStatus.CANCELLED) ->
            ArchiveUiTheme.colorScheme.surfaceContainerHigh to ArchiveUiTheme.colorScheme.onSurfaceContainerVariant
        concert.ticketState == TicketState.ON_SALE -> ArchiveUiTheme.colorScheme.primaryContainer to ArchiveUiTheme.colorScheme.onPrimaryContainer
        concert.ticketState == TicketState.SOLD_OUT -> ArchiveUiTheme.colorScheme.errorContainer to ArchiveUiTheme.colorScheme.onErrorContainer
        else -> ArchiveUiTheme.colorScheme.secondaryContainer to ArchiveUiTheme.colorScheme.onSecondaryContainer
    }
    Card(
        insideMargin = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
        colors = CardDefaults.defaultColors(color = colors.first, contentColor = colors.second),
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FilterRow(
    title: String,
    labels: List<String>,
    selected: String,
    tagPrefix: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, fontSize = 12.sp, color = ArchiveUiTheme.colorScheme.onBackgroundVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(labels.size, key = { labels[it] }) { index ->
                val label = labels[index]
                val active = label == selected
                val background by animateColorAsState(
                    if (active) ArchiveUiTheme.colorScheme.primaryContainer
                    else ArchiveUiTheme.colorScheme.surfaceContainerHigh, label = "filterBackground")
                Card(
                    onClick = { onSelect(label) },
                    insideMargin = PaddingValues(horizontal = 13.dp, vertical = 7.dp),
                    colors = CardDefaults.defaultColors(
                        color = background,
                        contentColor = if (active) ArchiveUiTheme.colorScheme.onPrimaryContainer
                        else ArchiveUiTheme.colorScheme.onSurfaceContainerVariant,
                    ),
                    pressFeedbackType = PressFeedbackType.Sink,
                    modifier = Modifier.testTag("$tagPrefix-$label"),
                ) {
                    Text(concertFilterLabel(label), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, count: Int) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(AppText.get(R.string.ui_calendar_month_year, month.year, month.monthValue), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text(AppText.get(R.string.ui_shows_91, count), fontSize = 12.sp, color = ArchiveUiTheme.colorScheme.onBackgroundVariant)
    }
}

@Composable
private fun ConcertDetailScreen(
    concert: Concert,
    updatedLabel: String,
    now: ZonedDateTime,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val status = concert.status(now)
    val timeText = concertTime(concert, now)
    ArchiveScaffold(
        AppText.get(R.string.ui_show_details_92), small = true,
        navigationIcon = { BackButton(onBack, AppText.get(R.string.ui_back_to_concerts)) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("concert-detail"),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top = padding.calculateTopPadding() + 10.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item("concert-detail-photo") {
                ConcertPhoto(Modifier.fillMaxWidth().height(240.dp))
                CompactSourceLink(AppText.get(R.string.ui_official_tour_photo_jvr_music), concertPhotoSource)
            }
            item("concert-detail-hero") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("CARNIVAL II · LIVE", fontSize = 11.sp, letterSpacing = 1.6.sp,
                            fontWeight = FontWeight.SemiBold, color = ArchiveUiTheme.colorScheme.primary)
                        Spacer(Modifier.weight(1f))
                        ConcertStatusChip(concert, now)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("${concert.theme} · ${AppText.fromOriginal(concert.venue.city)}", fontSize = 34.sp,
                        lineHeight = 42.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() })
                    Text(AppText.fromOriginal(concert.tourName), fontSize = 14.sp, lineHeight = 21.sp,
                        color = ArchiveUiTheme.colorScheme.onBackgroundVariant)
                    Spacer(Modifier.height(2.dp))
                    Text(concert.dateLabel, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("$timeText · ${concert.zoneLabel}", fontSize = 13.sp,
                        color = ArchiveUiTheme.colorScheme.onBackgroundVariant)
                    Text(concert.countdown(now), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = ArchiveUiTheme.colorScheme.primary)
                }
            }

            item("concert-detail-place") {
                DetailCard(AppText.get(R.string.ui_time_and_venue)) {
                    DetailValue(AppText.get(R.string.ui_date), concert.dateLabel)
                    DetailValue(AppText.get(R.string.ui_local_time), timeText)
                    DetailValue(AppText.get(R.string.ui_time_zone), concert.zoneLabel)
                    DetailValue(AppText.get(R.string.ui_venue), "${AppText.fromOriginal(concert.venue.name)} · ${AppText.fromOriginal(concert.venue.country)}")
                    DetailValue(AppText.get(R.string.ui_address), concert.venue.address.ifBlank { AppText.get(R.string.ui_not_provided_in_the_official_schedule) })
                    if (concert.venue.latitude != null && concert.venue.longitude != null) {
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = {
                            val latitude = concert.venue.latitude
                            val longitude = concert.venue.longitude
                            val label = Uri.encode(concert.venue.name)
                            val geo = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($label)")
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, geo))
                            } catch (_: ActivityNotFoundException) {
                                runCatching {
                                    uriHandler.openUri(
                                        "https://www.openstreetmap.org/?mlat=$latitude&mlon=$longitude" +
                                            "#map=16/$latitude/$longitude",
                                    )
                                }
                            }
                        }) { Text(AppText.get(R.string.ui_open_map)) }
                    }
                }
            }

            item("concert-detail-ticket") {
                DetailCard(AppText.get(R.string.ui_ticket_information)) {
                    DetailValue(AppText.get(R.string.ui_ticket_status), concert.ticketLabel(now))
                    concert.ticketPlatform?.let { DetailValue(AppText.get(R.string.ui_ticket_platform), it) }
                    concert.saleStartsAt?.let {
                        DetailValue(AppText.get(R.string.ui_sales_start), formatDateTime(it, concert))
                    }
                    concert.priceRange?.let {
                        DetailValue(AppText.get(R.string.ui_price_range), it)
                        Text(AppText.get(R.string.ui_shown_only_when_officially_announced_the_final_price_is_determin),
                            fontSize = 11.sp, lineHeight = 17.sp,
                            color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
                    }
                    Spacer(Modifier.height(14.dp))
                    when {
                        status == ShowStatus.ENDED || status == ShowStatus.CANCELLED ->
                            Text(AppText.get(R.string.ui_this_show_has_ended_ticket_links_are_closed), fontSize = 13.sp,
                                color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
                        concert.ticketState == TicketState.PENDING ->
                            Text(AppText.get(R.string.ui_ticket_sales_have_not_started_check_the_ticket_platform_after_sa), fontSize = 13.sp,
                                color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
                        concert.ticketState == TicketState.SOLD_OUT ->
                            Text(AppText.get(R.string.ui_this_show_is_sold_out_check_official_announcements_for_extra_dat), fontSize = 13.sp,
                                color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
                        concert.ticketUrl != null ->
                            Button(onClick = { uriHandler.openUri(concert.ticketUrl) },
                                modifier = Modifier.testTag("concert-ticket")) {
                                Text(
                                    if (concert.ticketState == TicketState.ON_SALE) {
                                        AppText.get(R.string.ui_open, concert.ticketPlatform ?: AppText.get(R.string.ui_ticket_platform))
                                    } else {
                                        AppText.get(R.string.ui_view_ticket_platform)
                                    },
                                )
                            }
                        else ->
                            Text(AppText.get(R.string.ui_the_official_schedule_has_no_ticket_link_see_the_official_source), fontSize = 13.sp,
                                color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
                    }
                    concert.note?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it, fontSize = 12.sp, lineHeight = 19.sp,
                            color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
                    }
                }
            }

            item("concert-detail-source") {
                DetailCard(AppText.get(R.string.ui_organizer_and_sources)) {
                    DetailValue(AppText.get(R.string.ui_organizer), concert.organizer ?: AppText.get(R.string.ui_organizer_not_listed_in_public_sources))
                    val sources = buildList {
                        addAll(concert.sourceUrls)
                        concert.officialUrl?.let { add(it) }
                    }.distinctBy(::concertSourceLabel)
                    if (sources.isNotEmpty()) {
                        Text(AppText.get(R.string.ui_sources), fontSize = 12.sp, color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
                        sources.forEach { CompactSourceLink(concertSourceLabel(it), it) }
                    }
                    DetailValue(AppText.get(R.string.ui_last_updated), updatedLabel)
                    DetailValue(AppText.get(R.string.ui_data_verified), concert.updatedLabel)
                    Text(AppText.get(R.string.ui_show_times_prices_and_ticket_availability_may_change_follow_the),
                        fontSize = 12.sp, lineHeight = 19.sp,
                        color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
                }
            }

            item("concert-detail-note") {
                QuietText(AppText.get(R.string.ui_this_page_collects_public_event_information_it_does_not_offer_ti))
            }
        }
    }
}

@Composable
private fun CompactSourceLink(label: String, url: String) {
    val uriHandler = LocalUriHandler.current
    Text(
        label,
        modifier = Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button,
            onClickLabel = AppText.get(R.string.ui_open_source)) { uriHandler.openUri(url) }.padding(vertical = 2.dp),
        fontSize = 13.sp,
        color = ArchiveUiTheme.colorScheme.primary,
    )
}

@Composable
private fun DetailCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), insideMargin = PaddingValues(20.dp)) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(13.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun DetailValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, fontSize = 12.sp, color = ArchiveUiTheme.colorScheme.onSurfaceContainerVariant)
        Text(value, fontSize = 15.sp, lineHeight = 23.sp)
    }
}

private fun concertTime(concert: Concert, now: ZonedDateTime): String =
    if (concert.startTime == null && concert.status(now) == ShowStatus.ENDED) AppText.get(R.string.ui_time_not_listed)
    else concert.timeLabel

private fun formatDateTime(value: ZonedDateTime, concert: Concert): String =
    value.withZoneSameInstant(concert.venue.timeZone)
        .format(DateTimeFormatter.ofPattern(AppText.get(R.string.ui_yyyy_mm_dd_hh_mm), AppText.locale)) +
        " · ${concert.venue.timeZone.id}"

private fun concertSourceLabel(url: String): String = when {
    "jvrmusic.com" in url -> AppText.get(R.string.ui_jvr_music_official_schedule)
    "wikipedia.org" in url -> AppText.get(R.string.ui_wikipedia)
    "ticketmaster" in url -> "Ticketmaster"
    "bookmyshow" in url -> "BookMyShow"
    "hk01.com" in url -> AppText.get(R.string.ui_hk01)
    "sina.com.cn" in url -> AppText.get(R.string.ui_sina_hainan_tourism_radio)
    "8world.com" in url -> AppText.get(R.string.ui_8world)
    "orientaldaily.com.my" in url -> AppText.get(R.string.ui_oriental_daily)
    "damai.cn" in url -> AppText.get(R.string.ui_damai)
    else -> runCatching { Uri.parse(url).host }.getOrNull() ?: AppText.get(R.string.ui_source)
}

private const val concertPhotoSource = "https://www.jvrmusic.com/news/detail/2041391444102615040"
private const val concertPhotoUrl = "https://www.jvrmusic.com/upload/2041392808765558784/mI76_0456n1.jpg"

@Composable
private fun ConcertPhoto(modifier: Modifier = Modifier) {
    Box(modifier.background(ArchiveUiTheme.colorScheme.surfaceContainerHigh)) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(concertPhotoUrl).crossfade(350).build(),
            contentDescription = AppText.get(R.string.ui_official_photo_from_the_carnival_ii_tour_in_hangzhou),
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
            loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } },
            error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(AppText.get(R.string.ui_live_photo_unavailable), fontSize = 13.sp) } },
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .65f)))))
        Text(AppText.get(R.string.ui_carnival_ii_on_tour), color = Color.White, fontSize = 11.sp, letterSpacing = 1.4.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp))
    }
}

private fun concertFilterLabel(key: String): String = when (key) {
    "upcoming" -> AppText.get(R.string.ui_upcoming)
    "ended" -> AppText.get(R.string.ui_ended)
    "all" -> AppText.get(R.string.ui_all)
    "all_cities" -> AppText.get(R.string.ui_all_cities)
    else -> AppText.fromOriginal(key)
}
