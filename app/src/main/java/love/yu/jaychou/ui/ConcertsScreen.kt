package love.yu.jaychou.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun ConcertsScreen(bottom: Dp, onDetailVisibility: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val behavior = MiuixScrollBehavior()
    var loadAttempt by remember { mutableIntStateOf(0) }
    var snapshot by remember { mutableStateOf<ConcertSnapshot?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshNote by remember { mutableStateOf<String?>(null) }
    var mode by rememberSaveable { mutableStateOf("即将举行") }
    var country by rememberSaveable { mutableStateOf("全部") }
    var city by rememberSaveable { mutableStateOf("全部城市") }
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
            val loaded = ConcertRepository.load(context)
            snapshot = loaded
            loading = false
            if (ConcertRepository.isStale(loaded, ZonedDateTime.now())) {
                refreshing = true
                try {
                    snapshot = ConcertRepository.refresh(context, loaded)
                    refreshNote = null
                } catch (_: Exception) {
                    refreshNote = "JVR 官方行程暂时无法更新，正在显示离线快照。"
                } finally {
                    refreshing = false
                }
            }
        } catch (_: Exception) {
            loading = false
            error = "演唱会日程加载失败，请稍后重试。"
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
                    snapshot = ConcertRepository.refresh(context, base)
                    refreshNote = null
                } catch (_: Exception) {
                    refreshNote = "JVR 官方行程暂时无法更新，正在显示离线快照。"
                } finally {
                    refreshing = false
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        ArchiveScaffold(
            "演唱会",
            behavior = behavior,
            actions = {
                if (refreshing) {
                    Text("更新中…", modifier = Modifier.padding(horizontal = 12.dp),
                        fontSize = 13.sp, color = MiuixTheme.colorScheme.primary)
                } else {
                    TextButton("刷新", onClick = refreshNow, modifier = Modifier.testTag("concerts-refresh"))
                }
            },
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().nestedScroll(behavior.nestedScrollConnection)
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
                        Card(cornerRadius = 24.dp, insideMargin = PaddingValues(22.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                CircularProgressIndicator()
                                Column {
                                    Text("正在读取演唱会日程…", fontSize = 16.sp)
                                    Text("随后会向 JVR 官方行程核对最新场次。", fontSize = 12.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
                                }
                            }
                        }
                    }
                }

                if (error != null) {
                    item("concert-error") {
                        Card(cornerRadius = 24.dp, insideMargin = PaddingValues(22.dp)) {
                            Text("日程暂时不可用", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Text(error!!, fontSize = 14.sp, lineHeight = 21.sp,
                                color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = { loadAttempt++ }, modifier = Modifier.testTag("concerts-retry")) {
                                Text("重试")
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
                            Card(cornerRadius = 24.dp, insideMargin = PaddingValues(22.dp)) {
                                Text("暂无已公布场次", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(6.dp))
                                Text("官方公开新的巡演场次后，可在刷新后自动显示。", fontSize = 13.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
                            }
                        }
                    }

                    item("concert-calendar-title") {
                        Column(Modifier.padding(top = 4.dp)) {
                            SectionTitle("巡演日历", "按日期排列已公布场次 · 点击查看演出详情")
                            Spacer(Modifier.height(10.dp))
                            FilterRow(
                                title = "场次",
                                labels = listOf("即将举行", "全部", "已结束"),
                                selected = mode,
                                tagPrefix = "concert-mode",
                            ) { mode = it }
                            Spacer(Modifier.height(8.dp))
                            FilterRow(
                                title = "国家 / 地区",
                                labels = listOf("全部") + data.countries,
                                selected = country,
                                tagPrefix = "concert-country",
                            ) { value ->
                                country = value
                                city = "全部城市"
                            }
                            val cities = if (country == "全部") emptyList() else data.cities(country)
                            if (cities.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                FilterRow(
                                    title = "城市",
                                    labels = listOf("全部城市") + cities,
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
                                "即将举行" -> status != ShowStatus.ENDED && status != ShowStatus.CANCELLED
                                "已结束" -> status == ShowStatus.ENDED || status == ShowStatus.CANCELLED
                                else -> true
                            }
                        }
                        .filter { country == "全部" || it.venue.country == country }
                        .filter { city == "全部城市" || it.venue.city == city }
                    if (filtered.isEmpty()) {
                        item("concert-filter-empty") {
                            Card(cornerRadius = 24.dp, insideMargin = PaddingValues(22.dp)) {
                                Text("没有符合筛选的场次", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(6.dp))
                                Text("切换国家或城市筛选条件后再试。", fontSize = 13.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
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
                                ConcertRow(concert, now) { detailConcert = concert; selectedId = concert.id }
                            }
                        }
                    }

                    item("concert-source") {
                        Column(Modifier.padding(top = 6.dp)) {
                            QuietText("日程来源：JVR 官方行程、维基百科与各站票务公告。演出时间、票价与售票状态可能调整，以主办方及票务平台为准。")
                            SourceLink("JVR 官方行程", ConcertRepository.source)
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
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("JAY CHOU · CARNIVAL II", fontSize = 11.sp, letterSpacing = 1.8.sp,
            fontWeight = FontWeight.SemiBold, color = MiuixTheme.colorScheme.primary)
        Text("巡演现场", fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp,
            modifier = Modifier.semantics { heading() })
        QuietText("一城一名的嘉年华 II 世界巡回演唱会。最近场次、巡演日历与票务入口来自公开来源。")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("更新于 ${updated ?: "加载中"}", fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant)
            if (refreshing) {
                Text("· 正在核对 JVR 官方行程", fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.primary)
            }
        }
        if (refreshNote != null) {
            Text(refreshNote, fontSize = 12.sp, lineHeight = 18.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant)
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
        cornerRadius = 30.dp,
        insideMargin = PaddingValues(22.dp),
        pressFeedbackType = PressFeedbackType.Sink,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("最近演唱会", fontSize = 12.sp, letterSpacing = 1.4.sp,
                fontWeight = FontWeight.SemiBold, color = MiuixTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            ConcertStatusChip(concert, now)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${concert.theme} · ${concert.venue.city}", fontSize = 29.sp, lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(concert.tourName, fontSize = 13.sp, lineHeight = 19.sp,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(concert.countdown(now), fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.primary)
                Text(concert.date.format(DateTimeFormatter.ofPattern("M月d日", Locale.SIMPLIFIED_CHINESE)),
                    fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("${concert.dateLabel} · ${concert.timeLabel}", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text("${concert.venue.name} · ${concert.venue.country}", fontSize = 13.sp, lineHeight = 20.sp,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant, maxLines = 2,
            overflow = TextOverflow.Ellipsis)
        Text(concert.venue.address, fontSize = 12.sp, lineHeight = 18.sp,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant, maxLines = 2,
            overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("查看场次详情", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            if (concert.ticketUrl != null &&
                (status == ShowStatus.UPCOMING || status == ShowStatus.TODAY) &&
                concert.ticketState != TicketState.PENDING &&
                concert.ticketState != TicketState.SOLD_OUT
            ) {
                Text(
                    "购票 · ${concert.ticketPlatform ?: "票务平台"}",
                    modifier = Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button) {
                        uriHandler.openUri(concert.ticketUrl)
                    }.padding(vertical = 4.dp, horizontal = 2.dp),
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.primary,
                )
            } else if (concert.priceRange != null) {
                Text("票价已公布", fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
            }
        }
    }
}

@Composable
private fun ConcertRow(concert: Concert, now: ZonedDateTime, onClick: () -> Unit) {
    val status = concert.status(now)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("concert-row-${concert.id}"),
        cornerRadius = 22.dp,
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        pressFeedbackType = PressFeedbackType.Sink,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.width(52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${concert.date.monthValue}月", fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.primary)
                Text(concert.date.dayOfMonth.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(concert.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.SIMPLIFIED_CHINESE),
                    fontSize = 10.sp, color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(concert.venue.city, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(" · ${concert.theme}", fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
                }
                Text(concert.venue.name, fontSize = 13.sp, lineHeight = 19.sp,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${concertTime(concert, now)} · ${concert.venue.country}", fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                ConcertStatusChip(concert, now)
                if (status == ShowStatus.UPCOMING || status == ShowStatus.TODAY) {
                    Text(concert.countdown(now), fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
                }
            }
        }
    }
}

@Composable
private fun ConcertStatusChip(concert: Concert, now: ZonedDateTime) {
    val label = concert.ticketLabel(now)
    val colors = when (label) {
        "售票中" -> MiuixTheme.colorScheme.primaryContainer to MiuixTheme.colorScheme.onPrimaryContainer
        "待开票", "即将举行", "今天开演" ->
            MiuixTheme.colorScheme.secondaryContainer to MiuixTheme.colorScheme.onSecondaryContainer
        "已售罄" -> MiuixTheme.colorScheme.errorContainer to MiuixTheme.colorScheme.onErrorContainer
        else -> MiuixTheme.colorScheme.surfaceContainerHigh to MiuixTheme.colorScheme.onSurfaceContainerVariant
    }
    Card(
        cornerRadius = 50.dp,
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
        Text(title, fontSize = 12.sp, color = MiuixTheme.colorScheme.onBackgroundVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(labels.size, key = { labels[it] }) { index ->
                val label = labels[index]
                val active = label == selected
                Card(
                    onClick = { onSelect(label) },
                    cornerRadius = 50.dp,
                    insideMargin = PaddingValues(horizontal = 13.dp, vertical = 7.dp),
                    colors = CardDefaults.defaultColors(
                        color = if (active) MiuixTheme.colorScheme.primaryContainer
                        else MiuixTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (active) MiuixTheme.colorScheme.onPrimaryContainer
                        else MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    ),
                    pressFeedbackType = PressFeedbackType.Sink,
                    modifier = Modifier.testTag("$tagPrefix-$label"),
                ) {
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, count: Int) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text("${month.year} 年 ${month.monthValue} 月", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text("$count 场", fontSize = 12.sp, color = MiuixTheme.colorScheme.onBackgroundVariant)
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
        "演出详情", small = true,
        navigationIcon = { BackButton(onBack, "返回演唱会") },
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
            item("concert-detail-hero") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("CARNIVAL II · LIVE", fontSize = 11.sp, letterSpacing = 1.6.sp,
                            fontWeight = FontWeight.SemiBold, color = MiuixTheme.colorScheme.primary)
                        Spacer(Modifier.weight(1f))
                        ConcertStatusChip(concert, now)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("${concert.theme} · ${concert.venue.city}", fontSize = 34.sp,
                        lineHeight = 42.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() })
                    Text(concert.tourName, fontSize = 14.sp, lineHeight = 21.sp,
                        color = MiuixTheme.colorScheme.onBackgroundVariant)
                    Spacer(Modifier.height(2.dp))
                    Text(concert.dateLabel, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("$timeText · ${concert.zoneLabel}", fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onBackgroundVariant)
                    Text(concert.countdown(now), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.primary)
                }
            }

            item("concert-detail-place") {
                DetailCard("时间与地点") {
                    DetailValue("日期", concert.dateLabel)
                    DetailValue("当地时间", timeText)
                    DetailValue("时区", concert.zoneLabel)
                    DetailValue("场馆", "${concert.venue.name} · ${concert.venue.country}")
                    DetailValue("地址", concert.venue.address.ifBlank { "地址待公布" })
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
                        }) { Text("地图导航") }
                    }
                }
            }

            item("concert-detail-ticket") {
                DetailCard("票务信息") {
                    DetailValue("售票状态", concert.ticketLabel(now))
                    concert.ticketPlatform?.let { DetailValue("票务平台", it) }
                    concert.saleStartsAt?.let {
                        DetailValue("开售时间", formatDateTime(it, concert))
                    }
                    concert.priceRange?.let {
                        DetailValue("票价区间", it)
                        Text("仅在公开来源明确公布票档时显示；实际价格以平台结算页为准。",
                            fontSize = 11.sp, lineHeight = 17.sp,
                            color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
                    }
                    Spacer(Modifier.height(14.dp))
                    when {
                        status == ShowStatus.ENDED || status == ShowStatus.CANCELLED ->
                            Text("演出已结束，票务入口已关闭。", fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
                        concert.ticketState == TicketState.PENDING ->
                            Text("官方尚未开票；开票后可在票务平台购票。", fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
                        concert.ticketState == TicketState.SOLD_OUT ->
                            Text("该场次已售罄；请留意官方后续加场或回流票公告。", fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
                        concert.ticketUrl != null ->
                            Button(onClick = { uriHandler.openUri(concert.ticketUrl) },
                                modifier = Modifier.testTag("concert-ticket")) {
                                Text(
                                    if (concert.ticketState == TicketState.ON_SALE) {
                                        "前往 ${concert.ticketPlatform ?: "票务平台"}"
                                    } else {
                                        "查看票务平台"
                                    },
                                )
                            }
                        else ->
                            Text("官方票务入口尚未公布。", fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
                    }
                    concert.note?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it, fontSize = 12.sp, lineHeight = 19.sp,
                            color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
                    }
                }
            }

            item("concert-detail-source") {
                DetailCard("主办与来源") {
                    DetailValue("主办方", concert.organizer ?: "公开来源未标注主办方")
                    val sources = buildList {
                        addAll(concert.sourceUrls)
                        concert.officialUrl?.let { add(it) }
                    }.distinctBy(::concertSourceLabel)
                    if (sources.isNotEmpty()) {
                        Text("信息来源", fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
                        sources.forEach { CompactSourceLink(concertSourceLabel(it), it) }
                    }
                    DetailValue("最后更新", updatedLabel)
                    DetailValue("数据核对", concert.updatedLabel)
                    Text("演出时间、票价与售票状态可能调整；请以主办方与票务平台的最终公告为准。",
                        fontSize = 12.sp, lineHeight = 19.sp,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
                }
            }

            item("concert-detail-note") {
                QuietText("本页只整理公开演出信息，不提供购票代理、加价票或保证入场服务。")
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
            onClickLabel = "打开来源") { uriHandler.openUri(url) }.padding(vertical = 2.dp),
        fontSize = 13.sp,
        color = MiuixTheme.colorScheme.primary,
    )
}

@Composable
private fun DetailCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), cornerRadius = 24.dp, insideMargin = PaddingValues(20.dp)) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(13.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun DetailValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceContainerVariant)
        Text(value, fontSize = 15.sp, lineHeight = 23.sp)
    }
}

private fun concertTime(concert: Concert, now: ZonedDateTime): String =
    if (concert.startTime == null && concert.status(now) == ShowStatus.ENDED) "时间未收录"
    else concert.timeLabel

private fun formatDateTime(value: ZonedDateTime, concert: Concert): String =
    value.withZoneSameInstant(concert.venue.timeZone)
        .format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.SIMPLIFIED_CHINESE)) +
        " · ${concert.venue.timeZone.id}"

private fun concertSourceLabel(url: String): String = when {
    "jvrmusic.com" in url -> "JVR Music 官方行程"
    "wikipedia.org" in url -> "维基百科"
    "ticketmaster" in url -> "Ticketmaster"
    "bookmyshow" in url -> "BookMyShow"
    "hk01.com" in url -> "香港01"
    "sina.com.cn" in url -> "新浪 / 海南旅游广播"
    "8world.com" in url -> "8视界"
    "orientaldaily.com.my" in url -> "东方日报"
    "damai.cn" in url -> "大麦"
    else -> runCatching { Uri.parse(url).host }.getOrNull() ?: "来源"
}
