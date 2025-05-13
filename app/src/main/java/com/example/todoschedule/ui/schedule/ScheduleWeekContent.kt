package com.example.todoschedule.ui.schedule

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.domain.model.TimeSlot
import com.example.todoschedule.domain.utils.CalendarUtils
import com.example.todoschedule.ui.navigation.NavigationState
import com.example.todoschedule.ui.theme.ColorSchemeEnum
import com.example.todoschedule.ui.utils.ColorUtils
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import java.util.LinkedList
import kotlin.math.roundToInt

// 课表的小时高度和时间轴宽度常量
private val HOUR_HEIGHT = 60.dp
private val TIME_AXIS_WIDTH = 48.dp
private const val GRID_START_HOUR = 0
private const val GRID_END_HOUR = 24

/**
 * 周视图主内容 Composable
 * @param navigationState 导航状态
 * @param defaultTableId 默认课表ID
 * @param currentWeek 当前周数
 * @param weekDates 当前周的日期列表
 * @param weekTimeSlots 当前周的所有日程
 * @param onWeekChange 切换周的回调
 * @param onTimeSlotClick 日程点击回调
 */
@Composable
fun ScheduleWeekContent(
    navigationState: NavigationState,
    defaultTableId: Int?,
    currentWeek: Int,
    weekDates: List<LocalDate>,
    weekTimeSlots: List<TimeSlot>,
    onWeekChange: (Int) -> Unit,
    onTimeSlotClick: (TimeSlot) -> Unit
) {
    // Pager状态
    val pagerState = rememberPagerState(initialPage = currentWeek - 1) { CalendarUtils.MAX_WEEKS }
    // 监听Pager滑动，回调外部更新当前周
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page + 1 != currentWeek) onWeekChange(page + 1)
        }
    }
    // 外部currentWeek变化时，Pager自动跳转
    LaunchedEffect(currentWeek) {
        if (pagerState.currentPage != currentWeek - 1) {
            pagerState.scrollToPage(currentWeek - 1)
        }
    }
    // 计算每周的日期
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val availableWidth = constraints.maxWidth
        val dayWidth = with(density) { (availableWidth.toDp() - TIME_AXIS_WIDTH) / 7 }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            val thisWeek = page + 1
            val dates = CalendarUtils.getWeekDates(thisWeek, weekDates.firstOrNull())
            val slots = if (thisWeek == currentWeek) weekTimeSlots else emptyList()
            WeekSchedulePage(
                weekDates = dates,
                timeSlots = slots,
                dayWidth = dayWidth,
                navigationState = navigationState,
                defaultTableId = defaultTableId,
                onTimeSlotClick = onTimeSlotClick
            )
        }
    }
}

/**
 * 单周课表页面布局
 */
@Composable
fun WeekSchedulePage(
    weekDates: List<LocalDate>,
    timeSlots: List<TimeSlot>,
    dayWidth: Dp,
    navigationState: NavigationState,
    defaultTableId: Int?,
    onTimeSlotClick: (TimeSlot) -> Unit
) {
    val hourHeight = HOUR_HEIGHT
    val gridStartHour = GRID_START_HOUR
    val gridEndHour = GRID_END_HOUR
    val totalHours = gridEndHour - gridStartHour
    val totalGridHeight = (totalHours * hourHeight.value).dp
    val verticalScrollState = rememberScrollState()
    Column(Modifier.fillMaxSize()) {
        WeekHeader(weekDates = weekDates, dayWidth = dayWidth)
        Row(
            Modifier.weight(1f).fillMaxWidth()
        ) {
            // 时间轴
            Column(
                Modifier.width(TIME_AXIS_WIDTH).verticalScroll(verticalScrollState)
            ) {
                for (hour in gridStartHour until gridEndHour) {
                    Box(
                        Modifier.height(hourHeight).fillMaxWidth(),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Text(
                            "${hour}:00",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 8.dp, top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            // 课表网格
            Box(
                Modifier.weight(1f).verticalScroll(verticalScrollState).height(totalGridHeight)
            ) {
                // 背景分割线
                for (hour in gridStartHour until gridEndHour) {
                    Divider(
                        Modifier.offset(y = hourHeight * (hour - gridStartHour)),
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp
                    )
                }
                // 事件卡片布局（重叠处理）
                val eventSlots = calculateWeekEventPositions(timeSlots)
                eventSlots.forEach { (event, slotInfo) ->
                    val top = hourHeight * (slotInfo.startMinutes / 60f)
                    val height = hourHeight * ((slotInfo.endMinutes - slotInfo.startMinutes) / 60f).coerceAtLeast(0.5f)
                    val hasOverlap = slotInfo.maxColumns > 1
                    val density = LocalDensity.current
                    val horizontalOffset: Dp
                    val cardWidth: Dp
                    if (hasOverlap) {
                        val marginPx = with(density) { 4.dp.toPx() }
                        val gapPx = with(density) { 2.dp.toPx() }
                        val dayWidthPx = with(density) { dayWidth.toPx() }
                        val totalGap = gapPx * (slotInfo.maxColumns - 1)
                        val availableWidth = dayWidthPx - 2 * marginPx - totalGap
                        val cardWidthPx = availableWidth / slotInfo.maxColumns
                        val offsetPx = marginPx + (cardWidthPx + gapPx) * slotInfo.column
                        horizontalOffset = with(density) { offsetPx.toDp() }
                        cardWidth = with(density) { cardWidthPx.toDp() }
                    } else {
                        horizontalOffset = 4.dp
                        cardWidth = dayWidth - 8.dp
                    }
                    TimeSlotCard(
                        event = event,
                        modifier = Modifier
                            .width(cardWidth)
                            .offset(x = horizontalOffset, y = top)
                            .height(height)
                            .padding(vertical = 1.dp),
                        navigationState = navigationState,
                        defaultTableId = defaultTableId,
                        onClick = { onTimeSlotClick(event) }
                    )
                }
                // 当前时间指示器
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                if (weekDates.contains(now.date)) {
                    val dayIndex = weekDates.indexOf(now.date)
                    val nowMinutes = (now.hour - gridStartHour) * 60 + now.minute
                    val nowOffset = hourHeight * (nowMinutes / 60f)
                    Row(
                        Modifier.offset(x = dayWidth * dayIndex, y = nowOffset - 1.dp).height(2.dp)
                    ) {
                        Box(
                            Modifier.size(8.dp).offset(x = (-3).dp).background(MaterialTheme.colorScheme.error, CircleShape)
                        )
                        Box(
                            Modifier.weight(1f).height(2.dp).background(MaterialTheme.colorScheme.error)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 周头部，显示星期和日期
 */
@Composable
fun WeekHeader(weekDates: List<LocalDate>, dayWidth: Dp) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Spacer(Modifier.width(TIME_AXIS_WIDTH))
        weekDates.forEach { date ->
            val isToday = date == today
            val dayOfWeekName = date.dayOfWeek.getChineseWeekName()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(dayWidth).padding(bottom = 4.dp)
            ) {
                Text(
                    text = dayOfWeekName.substring(0, 2),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(top = 2.dp).size(24.dp).clip(CircleShape)
                        .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent)
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * 事件卡片，风格与日/月视图一致
 */
@Composable
fun TimeSlotCard(
    event: TimeSlot,
    modifier: Modifier = Modifier,
    navigationState: NavigationState,
    defaultTableId: Int?,
    onClick: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val (backgroundColor, contentColor) = ColorUtils.calculateTimeSlotColors(
        event.displayColor,
        event.scheduleId,
        isDarkTheme,
        MaterialTheme.colorScheme
    )
    val startInstant = Instant.fromEpochMilliseconds(event.startTime)
    val endInstant = Instant.fromEpochMilliseconds(event.endTime)
    val startTimeLocal = startInstant.toLocalDateTime(TimeZone.currentSystemDefault())
    val endTimeLocal = endInstant.toLocalDateTime(TimeZone.currentSystemDefault())
    val title = event.displayTitle ?: "无标题"
    val details = if (event.scheduleType == ScheduleType.ORDINARY) {
        event.displaySubtitle
    } else {
        if (!event.displaySubtitle.isNullOrBlank()) {
            "@${event.displaySubtitle}"
        } else {
            "${startTimeLocal.format(timeFormatter)} - ${endTimeLocal.format(timeFormatter)}"
        }
    }
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!details.isNullOrBlank()) {
                Text(
                    details,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    fontSize = 10.sp,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * 计算一周内所有事件的重叠布局信息，借鉴日视图算法
 */
data class WeekEventSlotInfo(
    val dayOfWeek: Int,
    val startMinutes: Int,
    val endMinutes: Int,
    val column: Int = 0,
    val maxColumns: Int = 1
)

fun calculateWeekEventPositions(events: List<TimeSlot>): List<Pair<TimeSlot, WeekEventSlotInfo>> {
    if (events.isEmpty()) return emptyList()
    val minDurationMinutes = 20
    val eventWithTimes = events.map { event ->
        val start = Instant.fromEpochMilliseconds(event.startTime).toLocalDateTime(TimeZone.currentSystemDefault())
        val end = Instant.fromEpochMilliseconds(event.endTime).toLocalDateTime(TimeZone.currentSystemDefault())
        val dayOfWeek = start.dayOfWeek.isoDayNumber
        val startMinutes = (start.hour - GRID_START_HOUR) * 60 + start.minute
        val originalEndMinutes = (end.hour - GRID_START_HOUR) * 60 + end.minute
        val durationMinutes = originalEndMinutes - startMinutes
        val endMinutes = if (durationMinutes < minDurationMinutes) startMinutes + minDurationMinutes else originalEndMinutes
        Triple(event, dayOfWeek, WeekEventSlotInfo(dayOfWeek, startMinutes, endMinutes))
    }
    val eventsByDay = eventWithTimes.groupBy { it.second }
    val result = mutableListOf<Pair<TimeSlot, WeekEventSlotInfo>>()
    eventsByDay.forEach { (_, dayEvents) ->
        if (dayEvents.size == 1) {
            val (event, _, slotInfo) = dayEvents[0]
            result.add(event to slotInfo.copy(column = 0, maxColumns = 1))
            return@forEach
        }
        val sortedEvents = dayEvents.sortedBy { it.third.startMinutes }
        val size = sortedEvents.size
        val overlapMatrix = Array(size) { BooleanArray(size) { false } }
        for (i in 0 until size) {
            for (j in i + 1 until size) {
                val a = sortedEvents[i].third
                val b = sortedEvents[j].third
                val buffer = 1
                if (a.startMinutes < (b.endMinutes - buffer) && (a.endMinutes - buffer) > b.startMinutes) {
                    overlapMatrix[i][j] = true
                    overlapMatrix[j][i] = true
                }
            }
        }
        val visited = BooleanArray(size) { false }
        val overlapGroups = mutableListOf<List<Int>>()
        for (i in 0 until size) {
            if (!visited[i]) {
                val group = mutableListOf<Int>()
                val queue = LinkedList<Int>()
                queue.add(i)
                visited[i] = true
                while (queue.isNotEmpty()) {
                    val current = queue.remove()
                    group.add(current)
                    for (j in 0 until size) {
                        if (!visited[j] && overlapMatrix[current][j]) {
                            queue.add(j)
                            visited[j] = true
                        }
                    }
                }
                overlapGroups.add(group)
            }
        }
        for (group in overlapGroups) {
            if (group.size == 1) {
                val idx = group[0]
                val (event, _, slotInfo) = sortedEvents[idx]
                result.add(event to slotInfo.copy(column = 0, maxColumns = 1))
                continue
            }
            val groupEventIndices = group.sortedBy { sortedEvents[it].third.startMinutes }
            val columnEndTimes = mutableListOf<Int>()
            val eventToColumn = mutableMapOf<Int, Int>()
            for (idx in groupEventIndices) {
                val (_, _, slotInfo) = sortedEvents[idx]
                var column = -1
                for (c in columnEndTimes.indices) {
                    if (slotInfo.startMinutes >= columnEndTimes[c]) {
                        column = c
                        break
                    }
                }
                if (column == -1) {
                    column = columnEndTimes.size
                    columnEndTimes.add(slotInfo.endMinutes)
                } else {
                    columnEndTimes[column] = slotInfo.endMinutes
                }
                eventToColumn[idx] = column
            }
            val maxColumns = columnEndTimes.size
            for (idx in group) {
                val (event, _, slotInfo) = sortedEvents[idx]
                val column = eventToColumn[idx] ?: 0
                val updatedSlotInfo = slotInfo.copy(column = column, maxColumns = maxColumns)
                result.add(event to updatedSlotInfo)
            }
        }
    }
    return result
}

