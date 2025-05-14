package com.example.todoschedule.ui.schedule

import android.util.Log
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.todoschedule.domain.utils.CalendarUtils
import com.example.todoschedule.ui.navigation.NavigationState
import com.example.todoschedule.ui.utils.ColorUtils
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
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
@Composable
fun DayScheduleContent(
    viewModel: ScheduleViewModel = hiltViewModel(),
    navigationState: NavigationState,
    defaultTableId: Int?
) {
    // Pager配置
    val pageCount = 1000
    val initialPage = pageCount / 2
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val pagerState = rememberPagerState(initialPage = initialPage) { pageCount }
    val coroutineScope = rememberCoroutineScope()

    // 当前页对应的日期
    val currentPage = pagerState.currentPage
    val currentDate =
        remember(currentPage) { today.plus(currentPage - initialPage, DateTimeUnit.DAY) }

    // 监听currentDayDate变化，自动滚动到对应日期
    val currentDayDate by viewModel.currentDayDate.collectAsState()

    LaunchedEffect(currentDayDate) {
        currentDayDate?.let { targetDate ->
            // 计算目标日期与今天相差的天数
            val daysDiff = CalendarUtils.calculateDaysBetween(today, targetDate)
            val targetPage = initialPage + daysDiff
            // 只有当目标页码和当前页码不一致时才跳转，避免死循环
            if (targetPage in 0 until pageCount && targetPage != pagerState.currentPage) {
                pagerState.scrollToPage(targetPage)
            }
        }
    }

    // 将当前页面的日期反馈给ViewModel
    LaunchedEffect(currentDate) {
        // 延迟更新，避免过于频繁的状态更新
        try {
            viewModel.updateCurrentDayDate(currentDate)
        } catch (e: Exception) {
            Log.e("DayScheduleContent", "Error updating current day date", e)
        }
    }

    // 获取当前日期的所有日程（课程+普通日程）
    val allTimeSlots by viewModel.allTimeSlots.collectAsState()
    val slotsForDay = remember(allTimeSlots, currentDate) {
        allTimeSlots.filter {
            val slotDate = Instant.fromEpochMilliseconds(it.startTime)
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            slotDate == currentDate
        }.sortedBy { it.startTime }
    }
    // 全天事件与普通事件分离
    val allDayEvents = slotsForDay.filter { it.startTime == it.endTime }
    val timedEvents = slotsForDay.filter { it.startTime != it.endTime }

    Column(Modifier.fillMaxSize()) {
        // 头部区域
        // DayViewHeader(
        //     date = currentDate,
        //     onPrevDay = { coroutineScope.launch { pagerState.animateScrollToPage(currentPage - 1) } },
        //     onNextDay = { coroutineScope.launch { pagerState.animateScrollToPage(currentPage + 1) } },
        //     onToday = { coroutineScope.launch { pagerState.animateScrollToPage(initialPage) } },
        // )
        // 全天事件区
        if (allDayEvents.isNotEmpty()) {
            AllDayEventRow(
                events = allDayEvents,
                navigationState = navigationState,
                defaultTableId = defaultTableId
            )
        }
        // Pager内容
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val date = today.plus(page - initialPage, DateTimeUnit.DAY)
            val slots = slotsForDay.filter {
                val slotDate = Instant.fromEpochMilliseconds(it.startTime)
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date
                slotDate == date
            }.sortedBy { it.startTime }
            val allDay = slots.filter { it.startTime == it.endTime }
            val timed = slots.filter { it.startTime != it.endTime }
            DayTimeline(
                date = date,
                events = timed,
                allDayEvents = allDay,
                navigationState = navigationState,
                defaultTableId = defaultTableId
            )
        }
    }
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
                        // 处理全天事件点击
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
    events: List<TimeSlot>,
    allDayEvents: List<TimeSlot>,
    navigationState: NavigationState,
    defaultTableId: Int?
) {
    // 时间轴参数
    val hourHeight = HOUR_HEIGHT
    val gridStartHour = GRID_START_HOUR
    val gridEndHour = GRID_END_HOUR
    val totalHours = gridEndHour - gridStartHour
    val totalGridHeight = (totalHours * hourHeight.value).dp
    val verticalScrollState = rememberScrollState()
    val isDarkTheme = isSystemInDarkTheme()

    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            // 时间刻度（与事件区共用verticalScrollState，实现同步滚动）
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
            // 事件区
            Box(
                Modifier
                    .weight(1f)
                    .verticalScroll(verticalScrollState)
                    .height(totalGridHeight)
            ) {
                // 背景分割线
                for (hour in gridStartHour until gridEndHour) {
                    Divider(
                        Modifier.offset(y = hourHeight * (hour - gridStartHour)),
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp
                    )
                }

                // 事件卡片布局（优化重叠处理）
                val eventSlots = calculateEventPositions(events, date)
                eventSlots.forEach { (event, slotInfo) ->
                    // 计算位置和尺寸
                    val top = hourHeight * (slotInfo.startMinutes / 60f)
                    val height =
                        hourHeight * ((slotInfo.endMinutes - slotInfo.startMinutes) / 60f).coerceAtLeast(
                            0.5f
                        )

                    // 计算宽度和位置
                    val hasOverlap = slotInfo.maxColumns > 1
                    val density = LocalDensity.current

                    // 为重叠事件计算宽度和偏移
                    val horizontalOffset: Dp
                    val cardWidth: Dp

                    if (hasOverlap) {
                        // 重叠事件组布局参数
                        val containerWidth = with(LocalDensity.current) {
                            LocalConfiguration.current.screenWidthDp.dp.toPx() * 0.9f
                        }
                        val marginPx = with(density) { 8.dp.toPx() }  // 两侧边距
                        val gapPx = with(density) { 4.dp.toPx() }     // 卡片间隔

                        // 计算单个卡片宽度（像素）
                        val totalGapWidth = gapPx * (slotInfo.maxColumns - 1)
                        val availableWidth = containerWidth - 2 * marginPx - totalGapWidth
                        val cardWidthPx = availableWidth / slotInfo.maxColumns

                        // 计算水平偏移（像素）
                        val offsetPx = marginPx + (cardWidthPx + gapPx) * slotInfo.column

                        // 转换回Dp单位
                        horizontalOffset = with(density) { offsetPx.toDp() }
                        cardWidth = with(density) { cardWidthPx.toDp() }

                        Log.d(
                            "DaySchedule",
                            "重叠卡片 ${event.displayTitle} - 列:${slotInfo.column}/${slotInfo.maxColumns}, " +
                                    "宽度:${cardWidthPx}px, 偏移:${offsetPx}px"
                        )
                    } else {
                        // 非重叠事件使用接近全宽但留出边距
                        horizontalOffset = 8.dp
                        cardWidth = with(density) {
                            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                            screenWidth - 16.dp  // 两侧各留8dp边距
                        }
                    }

                    // 计算颜色
                    val (backgroundColor, contentColor) = ColorUtils.calculateTimeSlotColors(
                        event.displayColor,
                        event.scheduleId,
                        isDarkTheme,
                        MaterialTheme.colorScheme
                    )

                    // 时间格式化
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
                            .padding(horizontal = 1.dp, vertical = 1.dp) // 减少内边距
                            .clickable {
                                // 处理事件卡片点击，导航到对应详情页
                                handleTimeSlotClick(event, navigationState, defaultTableId)
                            },
                        shape = RoundedCornerShape(6.dp), // 减小圆角
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
                            // 标题
                            Text(
                                event.displayTitle ?: "无标题",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // 时间
                            if (height > 50.dp) {
                                Text(
                                    timeString,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    fontSize = 10.sp,
                                    color = contentColor.copy(alpha = 0.7f)
                                )
                            }

                            // 地点/详情
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

                // 当前时间指示器（红线，最后渲染，确保在最上层）
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                if (now.date == date) {
                    val nowMinutes = (now.hour - gridStartHour) * 60 + now.minute
                    val nowOffset = hourHeight * (nowMinutes / 60f)

                    // 时间线指示器
                    Row(
                        Modifier
                            .offset(y = nowOffset - 1.dp)
                            .fillMaxWidth()
                            .height(2.dp)
                    ) {
                        // 左侧圆点
                        Box(
                            Modifier
                                .size(8.dp)
                                .offset(x = (-3).dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape)
                        )
                        // 线条
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

/**
 * 处理时间槽点击事件，根据类型导航到对应详情页
 */
private fun handleTimeSlotClick(
    timeSlot: TimeSlot,
    navigationState: NavigationState,
    defaultTableId: Int?
) {
    Log.d(
        "DaySchedule",
        "点击日程: Type=${timeSlot.scheduleType}, ID=${timeSlot.scheduleId}, 标题='${timeSlot.displayTitle}'"
    )

    when (timeSlot.scheduleType) {
        ScheduleType.COURSE -> {
            if (defaultTableId != null && defaultTableId != AppConstants.Ids.INVALID_TABLE_ID) {
                Log.i(
                    "DaySchedule",
                    "导航到课程详情: tableId=$defaultTableId, courseId=${timeSlot.scheduleId}"
                )
                navigationState.navigateToCourseDetail(
                    tableId = defaultTableId,
                    courseId = timeSlot.scheduleId
                )
            } else {
                Log.w(
                    "DaySchedule",
                    "无法导航到课程详情，默认课表ID无效或为空"
                )
            }
        }

        ScheduleType.ORDINARY -> {
            Log.i(
                "DaySchedule",
                "导航到普通日程详情: scheduleId=${timeSlot.scheduleId}"
            )
            navigationState.navigateToOrdinaryScheduleDetail(timeSlot.scheduleId)
        }

        else -> {
            Log.w(
                "DaySchedule",
                "未实现该日程类型的导航: ${timeSlot.scheduleType}"
            )
        }
    }
}