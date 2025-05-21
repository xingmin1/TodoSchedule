package com.example.todoschedule.ui.schedule

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.domain.model.TimeSlot
import com.example.todoschedule.ui.navigation.NavigationState
import com.example.todoschedule.ui.utils.ColorUtils
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

// 定义左侧时间轴的宽度常量
private val TIME_AXIS_WIDTH = 48.dp // 稍微增加宽度以容纳节次和时间

// 定义课表的小时高度
private val HOUR_HEIGHT = 60.dp

// 定义网格显示的小时范围
private const val GRID_START_HOUR = 0 // 从 0 点开始
private const val GRID_END_HOUR = 24 // 到 24 点结束 (显示 0:00 到 23:00 的标签)


/**
 * 日视图主内容 Composable
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun DayScheduleContent(
    anchorDate: kotlinx.datetime.LocalDate,
    onDateChange: (kotlinx.datetime.LocalDate) -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel(),
    navigationState: NavigationState,
    defaultTableId: Int?
) {
    val pageCount = 1000 // A large number for pseudo-infinite scrolling
    val initialPage = pageCount / 2 // Start in the middle
    val systemToday = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val targetPage = initialPage + anchorDate.daysUntil(systemToday) * -1

    key(anchorDate) {
        val pagerState = rememberPagerState(initialPage = targetPage) { pageCount }
        // Get all time slots and filter for the current date in pager
        val allTimeSlots by viewModel.allTimeSlots.collectAsState()
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 2
        ) { page ->
            val currentDate = systemToday.plus(page - initialPage, DateTimeUnit.DAY)
            if (currentDate != anchorDate && pagerState.currentPage == page) {
                onDateChange(currentDate)
            }
            val slotsForDay = remember(allTimeSlots, currentDate) {
                allTimeSlots.filter { slot ->
                    val slotDate = Instant.fromEpochMilliseconds(slot.startTime)
                        .toLocalDateTime(TimeZone.currentSystemDefault()).date
                    slotDate == currentDate
                }.sortedBy { it.startTime }
            }
            val allDayEvents = slotsForDay.filter { it.isAllDayEvent() }
            val timedEvents = slotsForDay.filter { !it.isAllDayEvent() }
            Column(Modifier.fillMaxSize()) {
                if (allDayEvents.isNotEmpty()) {
                    AllDayEventRow(
                        events = allDayEvents,
                        navigationState = navigationState,
                        defaultTableId = defaultTableId
                    )
                }
                DayTimeline(
                    date = currentDate,
                    events = timedEvents,
                    navigationState = navigationState,
                    defaultTableId = defaultTableId
                )
            }
        }
    }
}

// Helper extension for TimeSlot (consider placing in a more general location if used elsewhere)
fun TimeSlot.isAllDayEvent(): Boolean {
    // A common definition for an all-day event might be one where startTime equals endTime,
    // or it spans a full 24-hour period starting at midnight.
    // For now, let's assume if it was marked with head and no specific different start/end time in a day context,
    // or if startTime and endTime are identical (which often signals a placeholder for an all-day concept).
    // This might need refinement based on how OrdinarySchedule allDay is mapped to TimeSlot.
    if (this.scheduleType == ScheduleType.ORDINARY) {
        // Heuristic: if start and end are same, or if it's a placeholder from an all-day OrdinarySchedule
        // This needs a clear definition from how OrdinarySchedule(isAllDay=true) creates its TimeSlots.
        // A simple check for now:
        return startTime == endTime // This is often how simple all-day tasks are marked
    }
    return false // Courses are typically not all-day in this manner
}


@Composable
fun AllDayEventRow(
    events: List<TimeSlot>,
    navigationState: NavigationState,
    defaultTableId: Int?
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        events.forEach { event ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable {
                        handleTimeSlotClick(event, navigationState, defaultTableId)
                    }
            ) {
                Text(
                    text = event.displayTitle ?: "无标题",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun DayTimeline(
    date: LocalDate,
    events: List<TimeSlot>, // Should only contain timed events for this specific date
    navigationState: NavigationState,
    defaultTableId: Int?
) {
    val hourHeight = HOUR_HEIGHT
    val gridStartHour = GRID_START_HOUR
    val gridEndHour = GRID_END_HOUR
    val totalGridHeight = ((gridEndHour - gridStartHour) * hourHeight.value).dp
    val verticalScrollState = rememberScrollState()
    val isDarkTheme = isSystemInDarkTheme()

    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .width(TIME_AXIS_WIDTH)
                    .verticalScroll(verticalScrollState)
            ) {
                for (hour in gridStartHour until gridEndHour) {
                    Box(
                        Modifier
                            .height(hourHeight)
                            .fillMaxWidth(),
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
            Box(
                Modifier
                    .weight(1f)
                    .verticalScroll(verticalScrollState)
                    .height(totalGridHeight) // Important for scroll content size
            ) {
                for (hour in gridStartHour until gridEndHour) {
                    Divider(
                        Modifier.offset(y = hourHeight * (hour - gridStartHour)),
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp
                    )
                }

                val eventSlotsWithPositionInfo = calculateEventPositions(events, date)
                eventSlotsWithPositionInfo.forEach { (event, slotInfo) ->
                    val top = hourHeight * (slotInfo.startMinutes / 60f)
                    val height =
                        hourHeight * ((slotInfo.endMinutes - slotInfo.startMinutes) / 60f).coerceAtLeast(
                            0.5f
                        )
                    val hasOverlap = slotInfo.maxColumns > 1
                    val density = LocalDensity.current
                    val horizontalOffset: Dp
                    val cardWidth: Dp

                    if (hasOverlap) {
                        val containerWidthPx = with(LocalDensity.current) {
                            // Approximate width of the day column area
                            (LocalConfiguration.current.screenWidthDp.dp - TIME_AXIS_WIDTH).toPx() * 0.95f // factor for safety
                        }
                        val marginPx = with(density) { 4.dp.toPx() }  // Reduced margin for day view
                        val gapPx = with(density) { 2.dp.toPx() }     // Reduced gap
                        val totalGapWidth = gapPx * (slotInfo.maxColumns - 1)
                        val availableWidth = containerWidthPx - 2 * marginPx - totalGapWidth
                        val cardWidthPx =
                            if (slotInfo.maxColumns > 0) availableWidth / slotInfo.maxColumns else availableWidth
                        val offsetPx = marginPx + (cardWidthPx + gapPx) * slotInfo.column
                        horizontalOffset = with(density) { offsetPx.toDp() }
                        cardWidth = with(density) { cardWidthPx.toDp() }
                    } else {
                        horizontalOffset = 4.dp // Reduced margin for non-overlapping
                        cardWidth = with(density) {
                            (LocalConfiguration.current.screenWidthDp.dp - TIME_AXIS_WIDTH - 8.dp) // Adjusted for margins
                        }
                    }

                    val (backgroundColor, contentColor) = ColorUtils.calculateTimeSlotColors(
                        event.displayColor,
                        event.scheduleId,
                        isDarkTheme,
                        MaterialTheme.colorScheme
                    )
                    val startTime = Instant.fromEpochMilliseconds(event.startTime)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                    val endTime = Instant.fromEpochMilliseconds(event.endTime)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                    val timeString =
                        "${startTime.format(timeFormatter)} - ${endTime.format(timeFormatter)}"

                    Card(
                        modifier = Modifier
                            .width(cardWidth)
                            .offset(x = horizontalOffset, y = top)
                            .height(height)
                            .padding(horizontal = 1.dp, vertical = 1.dp)
                            .clickable {
                                handleTimeSlotClick(
                                    event,
                                    navigationState,
                                    defaultTableId
                                )
                            },
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = backgroundColor,
                            contentColor = contentColor
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                event.displayTitle ?: "无标题",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (height > 50.dp) {
                                Text(
                                    timeString,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    fontSize = 10.sp,
                                    color = contentColor.copy(alpha = 0.7f)
                                )
                            }
                            if (!event.displaySubtitle.isNullOrBlank() && height > 70.dp) {
                                Text(
                                    event.displaySubtitle!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                if (now.date == date) {
                    val nowMinutes = (now.hour - gridStartHour) * 60 + now.minute
                    val nowOffset = hourHeight * (nowMinutes / 60f)
                    Row(
                        Modifier
                            .offset(y = nowOffset - 1.dp)
                            .fillMaxWidth()
                            .height(2.dp)
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .offset(x = (-3).dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape)
                        )
                        Box(
                            Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }
                }
            }
        }
    }
}

private fun handleTimeSlotClick(
    timeSlot: TimeSlot,
    navigationState: NavigationState,
    defaultTableId: Int?
) {
    Log.d(
        "DaySchedule",
        "Clicked on: Type=${timeSlot.scheduleType}, ID=${timeSlot.scheduleId}, Title='${timeSlot.displayTitle}'"
    )
    when (timeSlot.scheduleType) {
        ScheduleType.COURSE -> {
            // Use the first default table ID if multiple are present, or if a specific active table ID is available
            val tableIdToUse = defaultTableId ?: AppConstants.Ids.INVALID_TABLE_ID
            if (tableIdToUse != AppConstants.Ids.INVALID_TABLE_ID) {
                navigationState.navigateToCourseDetail(
                    tableId = tableIdToUse,
                    courseId = timeSlot.scheduleId
                )
            } else {
                Log.w("DaySchedule", "Cannot navigate to course detail, table ID is invalid.")
            }
        }

        ScheduleType.ORDINARY -> {
            navigationState.navigateToOrdinaryScheduleDetail(timeSlot.scheduleId)
        }

        else -> {
            Log.w(
                "DaySchedule",
                "Navigation not implemented for schedule type: ${timeSlot.scheduleType}"
            )
        }
    }
}