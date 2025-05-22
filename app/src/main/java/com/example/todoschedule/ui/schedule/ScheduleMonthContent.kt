package com.example.todoschedule.ui.schedule

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.ui.navigation.NavigationState
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import java.time.YearMonth


/**
 * 月视图主内容 Composable
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalStdlibApi::class
)
@Composable
fun ScheduleMonthContent(
    initialYearMonth: java.time.YearMonth,
    onYearMonthChange: (java.time.YearMonth) -> Unit,
    navigationState: NavigationState,
    defaultTableId: Int?,
    viewModel: ScheduleViewModel,
    paddingValues: PaddingValues,
    isLargeMode: Boolean,
    onLargeModeChange: (Boolean) -> Unit,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val pageCount = 1000
    val initialPage = pageCount / 2
    val baseYearMonth = remember { java.time.YearMonth.now() }
    val monthsDiff = baseYearMonth.until(initialYearMonth, java.time.temporal.ChronoUnit.MONTHS)
    val targetPage = initialPage + monthsDiff.toInt()

    // 1. PagerState 只初始化一次，避免重组导致的卡顿
    val pagerState = rememberPagerState(initialPage = targetPage) { pageCount }

    // 2. 当前页面对应的 YearMonth，直接由 page 索引推导
    val currentYearMonth = remember(pagerState.currentPage) {
        baseYearMonth.plusMonths((pagerState.currentPage - initialPage).toLong())
    }

    // 3. 监听 initialYearMonth 变化，外部跳转时自动 scrollToPage
    LaunchedEffect(initialYearMonth) {
        val newPage = initialPage + baseYearMonth.until(initialYearMonth, java.time.temporal.ChronoUnit.MONTHS).toInt()
        if (pagerState.currentPage != newPage) {
            pagerState.scrollToPage(newPage)
        }
    }

    // 4. 只在 currentYearMonth 变化时回调 onYearMonthChange，避免死循环
    LaunchedEffect(currentYearMonth) {
        if (currentYearMonth != initialYearMonth) {
            onYearMonthChange(currentYearMonth)
        }
    }

    // 横向滑动分页，滑动时页面内容直接由 page 索引决定，保证流畅
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1, // 预加载相邻页面，提高滑动流畅度
        pageSpacing = 0.dp // 确保页面间无间隙
    ) { page ->
        val yearMonth = baseYearMonth.plusMonths((page - initialPage).toLong())
        MonthSchedulePage(
            navigationState = navigationState,
            defaultTableId = defaultTableId,
            viewModel = viewModel,
            yearMonth = yearMonth,
            isLargeMode = isLargeMode,
            onLargeModeChange = onLargeModeChange,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected
        )
    }
}

// 其它相关Composable和辅助函数
// ...请根据原文件内容继续迁移...

/**
 * 将 YearMonth 转换为从 1970 年 1 月算起的月份数
 */
private fun YearMonth.toEpochMonth(): Long {
    return year.toLong() * 12L + monthValue - 1
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun MonthSchedulePage(
    navigationState: NavigationState,
    defaultTableId: Int?,
    viewModel: ScheduleViewModel,
    yearMonth: YearMonth,
    isLargeMode: Boolean,
    onLargeModeChange: (Boolean) -> Unit,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val year = yearMonth.year
    val month = yearMonth.monthValue
    val firstDayOfMonth = LocalDate(year, month, 1)
    val daysInMonth = firstDayOfMonth.month.length(isLeapYear(firstDayOfMonth.year))
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.isoDayNumber
    val leadingEmptyDays = (firstDayOfWeek - 1).coerceAtLeast(0)
    val totalGridCount = 42
    val monthDates = List(daysInMonth) { i -> LocalDate(year, month, i + 1) }
    val prevMonth = if (month == 1) LocalDate(year - 1, 12, 1) else LocalDate(year, month - 1, 1)
    val prevMonthDays = prevMonth.month.length(isLeapYear(prevMonth.year))
    val prevMonthDates = List(leadingEmptyDays) { i ->
        LocalDate(
            prevMonth.year,
            prevMonth.month,
            prevMonthDays - leadingEmptyDays + i + 1
        )
    }
    val trailingEmptyDays = totalGridCount - leadingEmptyDays - daysInMonth
    val nextMonth = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
    val nextMonthDates =
        List(trailingEmptyDays) { i -> LocalDate(nextMonth.year, nextMonth.month, i + 1) }
    val calendarGrid = prevMonthDates + monthDates + nextMonthDates
    // 获取本月所有日程（课程+普通日程）
    val allTimeSlots by viewModel.allTimeSlots.collectAsState()
    val monthTimeSlots = remember(allTimeSlots, year, month) {
        allTimeSlots.filter {
            val slotDate = Instant.fromEpochMilliseconds(it.startTime)
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            slotDate.year == year && slotDate.monthNumber == month
        }
    }
    // 按日期分组，便于日历格子渲染
    val daySlotMap = remember(monthTimeSlots) {
        monthTimeSlots.groupBy {
            Instant.fromEpochMilliseconds(it.startTime)
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
        }
    }
    val density = LocalDensity.current
    val switchThresholdPx = with(density) { 100.dp.toPx() }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { w ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = w,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(isLargeMode) {
                    awaitPointerEventScope {
                        while (true) {
                            val down =
                                awaitPointerEvent().changes.firstOrNull { it.pressed } ?: continue
                            val startY = down.position.y
                            var drag = down
                            var consumed = false
                            while (drag.pressed) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.pressed } ?: break
                                val dy = change.position.y - startY
                                if (!consumed && kotlin.math.abs(dy) > switchThresholdPx) {
                                    if (dy > 0 && !isLargeMode) onLargeModeChange(true)
                                    else if (dy < 0 && isLargeMode) onLargeModeChange(false)
                                    consumed = true
                                    break
                                }
                                drag = change
                            }
                        }
                    }
                }
        ) {
            val scrollState = rememberScrollState()
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                for (row in 0 until 6) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        for (col in 0 until 7) {
                            val index = row * 7 + col
                            val date = calendarGrid.getOrNull(index)
                            if (date == null) {
                                Box(Modifier.weight(1f))
                            } else {
                                val isThisMonth = date.monthNumber == month && date.year == year
                                val isToday = date == today
                                val isSelected = date == selectedDate
                                val hasEvent = (daySlotMap[date]?.isNotEmpty() == true)
                                val bgColor by animateColorAsState(
                                    targetValue = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
                                        else -> Color.Transparent
                                    }, label = "date-bg"
                                )
                                val textColor = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isThisMonth -> MaterialTheme.colorScheme.onSurface
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                }
                                val cellHeight = if (isLargeMode) 100.dp else 44.dp
                                Surface(
                                    shape = RoundedCornerShape(if (isLargeMode) 16.dp else 12.dp),
                                    color = bgColor,
                                    tonalElevation = if (isSelected) 3.dp else 0.dp,
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .height(cellHeight)
                                        .weight(1f)
                                        .clickable { onDateSelected(date) }
                                ) {
                                    Column(
                                        Modifier
                                            .fillMaxSize()
                                            .padding(
                                                top = if (isLargeMode) 8.dp else 4.dp,
                                                bottom = 2.dp
                                            ),
                                        verticalArrangement = Arrangement.Top,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            style = if (isLargeMode) MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ) else MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = textColor
                                        )
                                        if (isLargeMode) {
                                            val slots = (daySlotMap[date]
                                                ?: emptyList()).sortedBy { it.startTime }
                                            if (slots.isNotEmpty()) {
                                                Spacer(Modifier.height(2.dp))
                                                Column(
                                                    Modifier.fillMaxWidth(),
                                                    verticalArrangement = Arrangement.Center,
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    if (slots.size == 1) {
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                                            tonalElevation = 1.dp,
                                                            modifier = Modifier
                                                                .padding(vertical = 2.dp)
                                                                .height(20.dp)
                                                        ) {
                                                            Text(
                                                                text = slots[0].displayTitle?.take(6)
                                                                    ?: "",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                maxLines = 1,
                                                                modifier = Modifier.padding(
                                                                    horizontal = 6.dp,
                                                                    vertical = 2.dp
                                                                )
                                                            )
                                                        }
                                                    } else if (slots.size == 2) {
                                                        for (i in 0..1) {
                                                            Surface(
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                                tonalElevation = 1.dp,
                                                                modifier = Modifier
                                                                    .padding(vertical = 2.dp)
                                                                    .height(20.dp)
                                                            ) {
                                                                Text(
                                                                    text = slots[i].displayTitle?.take(
                                                                        6
                                                                    ) ?: "",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                    maxLines = 1,
                                                                    modifier = Modifier.padding(
                                                                        horizontal = 6.dp,
                                                                        vertical = 2.dp
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    } else if (slots.size > 2) {
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                                            tonalElevation = 1.dp,
                                                            modifier = Modifier
                                                                .padding(vertical = 2.dp)
                                                                .height(20.dp)
                                                        ) {
                                                            Text(
                                                                text = slots[0].displayTitle?.take(6)
                                                                    ?: "",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                maxLines = 1,
                                                                modifier = Modifier.padding(
                                                                    horizontal = 6.dp,
                                                                    vertical = 2.dp
                                                                )
                                                            )
                                                        }
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = MaterialTheme.colorScheme.primary,
                                                            tonalElevation = 1.dp,
                                                            modifier = Modifier
                                                                .padding(vertical = 2.dp)
                                                                .height(20.dp)
                                                        ) {
                                                            Text(
                                                                text = "+${slots.size - 1}",
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontWeight = FontWeight.Bold
                                                                ),
                                                                color = MaterialTheme.colorScheme.onPrimary,
                                                                maxLines = 1,
                                                                modifier = Modifier.padding(
                                                                    horizontal = 6.dp,
                                                                    vertical = 2.dp
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            if (hasEvent) {
                                                Spacer(Modifier.height(2.dp))
                                                Box(
                                                    Modifier
                                                        .size(6.dp)
                                                        .background(
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                            shape = CircleShape
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (!isLargeMode) {
                    AnimatedVisibility(
                        visible = !isLargeMode,
                        enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }) + fadeOut()
                    ) {
                        Spacer(Modifier.height(0.dp))
                        Surface(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(20.dp),
                            tonalElevation = 4.dp
                        ) {
                            val slots =
                                (daySlotMap[selectedDate] ?: emptyList()).sortedBy { it.startTime }
                            Column(Modifier.padding(20.dp)) {
                                Text(
                                    "${selectedDate.year}年${selectedDate.monthNumber}月${selectedDate.dayOfMonth}日 日程安排",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(Modifier.height(6.dp))
                                if (slots.isEmpty()) {
                                    Text("暂无日程安排", style = MaterialTheme.typography.bodyLarge)
                                } else {
                                    slots.forEach { slot ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            tonalElevation = 1.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clickable {
                                                    when (slot.scheduleType) {
                                                        ScheduleType.COURSE -> {
                                                            val currentTableId = defaultTableId
                                                            if (currentTableId != null && currentTableId != AppConstants.Ids.INVALID_TABLE_ID) {
                                                                navigationState.navigateToCourseDetail(
                                                                    tableId = currentTableId,
                                                                    courseId = slot.scheduleId
                                                                )
                                                            }
                                                        }

                                                        ScheduleType.ORDINARY -> {
                                                            navigationState.navigateToOrdinaryScheduleDetail(
                                                                slot.scheduleId
                                                            )
                                                        }

                                                        else -> {}
                                                    }
                                                }
                                        ) {
                                            Column(Modifier.padding(12.dp)) {
                                                Text(
                                                    slot.displayTitle ?: "无标题",
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                if (!slot.displaySubtitle.isNullOrBlank()) {
                                                    Text(
                                                        slot.displaySubtitle!!,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}