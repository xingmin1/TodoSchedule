package com.example.todoschedule.ui.schedule

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.domain.model.TimeSlot
import com.example.todoschedule.ui.theme.ColorSchemeEnum
import com.example.todoschedule.ui.utils.ColorUtils
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.util.LinkedList
import kotlin.math.abs
import kotlin.math.roundToInt


// 课表的小时高度和时间轴宽度常量
private val HOUR_HEIGHT = 60.dp
private val TIME_AXIS_WIDTH = 48.dp
private const val GRID_START_HOUR = 0
private const val GRID_END_HOUR = 24

/**
 * 周视图主内容 Composable
 * 以日期为主，渲染连续7天的日历周视图
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScheduleWeekContent(
    viewModel: ScheduleViewModel,
    onTimeSlotClick: (TimeSlot) -> Unit
) {
    val anchorDate by viewModel.anchorDate.collectAsState()
    val allTimeSlots by viewModel.allTimeSlots.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val dayColumnWidth = (screenWidthDp - TIME_AXIS_WIDTH) / 7

    // Calculate the 7 days of the week based on the anchorDate
    val mondayOfAnchorWeek = anchorDate.minus((anchorDate.dayOfWeek.isoDayNumber - 1).toLong(),  DateTimeUnit.DAY)
    val weekDates = remember(mondayOfAnchorWeek) { // Recalculate if the Monday changes
        List(7) { i -> mondayOfAnchorWeek.plus(i.toLong(), DateTimeUnit.DAY) }
    }

    // Filter allTimeSlots for the current weekDates and group by date
    val weekTimeSlotsMap = remember(allTimeSlots, weekDates) {
        weekDates.associateWith { dateInWeek ->
            allTimeSlots.filter { slot ->
                val slotDate = Instant.fromEpochMilliseconds(slot.startTime)
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date
                slotDate == dateInWeek
            }
        }
    }

    // 支持左右滑动切换周
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(anchorDate) { // Re-key when anchorDate changes
                detectDragGestures {
                    change, dragAmount ->
                    change.consume()
                    if (abs(dragAmount.x) > 80 && abs(dragAmount.x) > abs(dragAmount.y)) {
                        val daysToShift = if (dragAmount.x < 0) 7 else -7
                        val newAnchorDate = anchorDate.plus(daysToShift.toLong(), DateTimeUnit.DAY)
                        coroutineScope.launch { viewModel.setAnchorDate(newAnchorDate) }
                    }
                }
            }
    ) {
        val scrollState = rememberScrollState()
        Column(Modifier.fillMaxSize()) {
            // 头部：显示7天日期
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Spacer(Modifier.width(TIME_AXIS_WIDTH)) // Placeholder for time axis alignment
                weekDates.forEach { date ->
                    val isToday = date == today
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = date.dayOfWeek.getChineseWeekName().substring(0, 2),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(24.dp)
                                .clip(CircleShape)
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
            // 主体区整体可滚动
            Box(
                Modifier
                    .weight(1f)
                    .verticalScroll(scrollState) // Entire content area below header is scrollable
            ) {
                Row(Modifier.fillMaxSize()) {
                    // 左侧时间轴
                    Column(
                        Modifier.width(TIME_AXIS_WIDTH)
                    ) {
                        for (hour in GRID_START_HOUR until GRID_END_HOUR) {
                            Box(
                                Modifier
                                    .height(HOUR_HEIGHT)
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
                    // 右侧7天日程区
                    weekDates.forEach { date ->
                        val slotsForThisDay = weekTimeSlotsMap[date] ?: emptyList()
                        Box(
                            Modifier
                                .width(dayColumnWidth) // Use calculated dayColumnWidth
                                .fillMaxHeight()
                        ) {
                            DayColumnSchedule(
                                date = date,
                                slots = slotsForThisDay,
                                onTimeSlotClick = onTimeSlotClick,
                                columnWidth = dayColumnWidth
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 单天的日程列，纵向为时间轴，卡片按时间段排列，重叠处理参考日视图
 */
@Composable
fun DayColumnSchedule(
    date: LocalDate, // Date for this column
    slots: List<TimeSlot>,
    onTimeSlotClick: (TimeSlot) -> Unit,
    columnWidth: Dp
) {
    // 复用日视图的重叠处理算法 (from ScheduleOverlapUtils.kt)
    val eventSlotsWithPositionInfo = calculateEventPositions(slots, date)
    val hourHeight = HOUR_HEIGHT
    val minDurationMinutes = 20 // Minimum duration for display to ensure visibility

    Box(Modifier.fillMaxSize()) { // This Box represents a single day column
        // 背景分割线 (hourly grid lines)
        for (hour in GRID_START_HOUR until GRID_END_HOUR) {
            Box(
                Modifier
                    .offset(y = hourHeight * (hour - GRID_START_HOUR))
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }

        // 日程卡片
        eventSlotsWithPositionInfo.forEach { (event, slotInfo) ->
            val topOffset = hourHeight * (slotInfo.startMinutes / 60f)
            // Ensure minimum height for very short or zero-duration events
            val eventDurationMinutes = (slotInfo.endMinutes - slotInfo.startMinutes).coerceAtLeast(minDurationMinutes)
            val cardHeight = hourHeight * (eventDurationMinutes / 60f)

            // Overlap handling: calculate width and horizontal offset within the day column
            val hasOverlap = slotInfo.maxColumns > 1
            val margin = 1.dp // Reduced margin for tighter packing in week view columns
            val gap = 1.dp    // Reduced gap

            val availableWidthForCards = columnWidth - (2 * margin) - ((slotInfo.maxColumns - 1) * gap)
            val individualCardWidth = if (slotInfo.maxColumns > 0) availableWidthForCards / slotInfo.maxColumns else availableWidthForCards

            val horizontalCardOffset = margin + (individualCardWidth + gap) * slotInfo.column

            // Colors
            val isDarkTheme = isSystemInDarkTheme()
            val (backgroundColor, contentColor) = ColorUtils.calculateTimeSlotColors(
                event.displayColor,
                event.scheduleId,
                isDarkTheme,
                MaterialTheme.colorScheme
            )

            // Time formatting
            val startTime = Instant.fromEpochMilliseconds(event.startTime)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val endTime = Instant.fromEpochMilliseconds(event.endTime)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val timeString = "${startTime.hour}:${startTime.minute.toString().padStart(2, '0')}" +
                    " - ${endTime.hour}:${endTime.minute.toString().padStart(2, '0')}"

            Card(
                modifier = Modifier
                    .width(individualCardWidth)
                    .offset(x = horizontalCardOffset, y = topOffset)
                    .height(cardHeight)
                    .padding(horizontal = 0.5.dp, vertical = 0.5.dp) // Minimal padding around card
                    .clickable { onTimeSlotClick(event) },
                shape = RoundedCornerShape(2.dp), // Sharper corners for dense view
                colors = CardDefaults.cardColors(
                    containerColor = backgroundColor,
                    contentColor = contentColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 2.dp) // Inner padding for content
                ) {
                    Text(
                        event.displayTitle ?: "无标题",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp // Smaller font for week view items
                    )
                    if (cardHeight > 40.dp) { // Show time only if enough space
                        Text(
                            timeString,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            fontSize = 8.sp, // Even smaller font for time
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                    if (!event.displaySubtitle.isNullOrBlank() && cardHeight > 55.dp) { // Show subtitle if space
                        Text(
                            event.displaySubtitle!!,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 8.sp,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单周的课表页面布局
 *
 * @param weekDates 当前周的日期列表 (LocalDate)。
 * @param timeSlots 当前周需要显示的时间槽 (课程/日程) 列表。
 * @param dayWidth 网格中每天的宽度。
 * @param onTimeSlotClick 时间槽被点击时的回调函数。
 */
@Composable
fun WeekSchedulePage(
    weekDates: List<LocalDate>,
    timeSlots: List<TimeSlot>,
    dayWidth: Dp,
    onTimeSlotClick: (TimeSlot) -> Unit
) {
    // --- 常量和状态 --- //
    // 使用文件顶部的常量
    val hourHeight = HOUR_HEIGHT
    val gridStartHour = GRID_START_HOUR
    val gridEndHour = GRID_END_HOUR
    val totalHours = gridEndHour - gridStartHour // 总小时数 (0-23共24个)
    val totalGridHeight = (totalHours * hourHeight.value).dp // 网格总高度

    val verticalScrollState = rememberScrollState()

    // --- 整体布局: 固定的头部 + 可滚动的内容 --- //
    Column(modifier = Modifier.fillMaxSize()) {
        // 1. 周日期头部 (固定)
        WeekHeader(weekDates = weekDates, dayWidth = dayWidth)

        // 2. 时间轴和网格内容 (可垂直滚动，通过 Row 分开时间轴和网格)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds() // 添加裁剪以防止 TimeAxis 绘制到 Header 上
        ) {
            // a) 左侧时间轴 (与网格同步滚动，通过 scrollState 实现)
            TimeAxis(
                startHour = gridStartHour,
                endHour = gridEndHour,
                hourHeight = hourHeight,
                scrollState = verticalScrollState, // 传递共享的 ScrollState
                modifier = Modifier
                    // 不再需要 offset 和 background
                    .width(TIME_AXIS_WIDTH)
            )

            // b) 右侧课表网格和时间槽 (垂直滚动主体)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(verticalScrollState)
            ) {
                // 在滚动区域内放置网格层
                ScheduleGridWithTimeSlots(
                    timeSlots = timeSlots,
                    dayWidth = dayWidth,
                    hourHeight = hourHeight,
                    gridStartHour = gridStartHour,
                    gridEndHour = gridEndHour,
                    totalGridHeight = totalGridHeight,
                    onTimeSlotClick = onTimeSlotClick,
                    modifier = Modifier.fillMaxWidth() // Layout will determine height internally
                )
            }
        }
    }
}

/**
 * 显示周日期头部的 Composable。
 * 包括左侧时间轴的占位和右侧 7 天的星期与日期。
 *
 * @param weekDates 当前周的日期列表 (LocalDate)。
 * @param dayWidth 每天的宽度，用于确保与网格对齐。
 */
@Composable
fun WeekHeader(
    weekDates: List<LocalDate>,
    dayWidth: Dp
) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date // 获取今天的日期

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp) // 设置内边距
        // .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        // --- 左侧时间轴占位 --- //
        Spacer(modifier = Modifier.width(TIME_AXIS_WIDTH))

        // --- 循环显示每天的日期和星期 --- //
        weekDates.forEach { date ->
            val isToday = date == today // 判断是否是今天
            // 将 DayOfWeek 枚举转换为中文星期名称
            val dayOfWeekName = date.dayOfWeek.getChineseWeekName()

            // --- 单个日期的显示列 --- //
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, // 水平居中
                modifier = Modifier
                    // .padding(horizontal = 1.dp) // 列之间的水平间距调整
                    .width(dayWidth) // 使用传入的 dayWidth 确保与网格对齐
                    .padding(bottom = 4.dp)
            ) {
                // --- 显示星期 --- //
                Text(
                    text = dayOfWeekName.substring(0, 2), // 只显示前两个字，例如 "周一"
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, // 今天加粗显示
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface // 今天使用主题色
                )

                // --- 显示日期 (圆形背景) --- //
                Box(
                    contentAlignment = Alignment.Center, // 内容居中
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(24.dp) // 固定大小
                        .clip(CircleShape) // 裁剪为圆形
                        // 今天使用主题色背景，否则透明
                        .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent)
                ) {
                    Text(
                        text = date.dayOfMonth.toString(), // 显示几号
                        style = MaterialTheme.typography.bodySmall,
                        // 根据背景色选择文字颜色
                        color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * 显示单个时间槽 (课程或日程) 的 Composable。
 * **重要:** 此 Composable 不再计算自己的位置或大小。
 * 它仅根据父级 `Layout` 提供的 `modifier` (隐含大小) 渲染内容。
 *
 * @param timeSlot 要显示的时间槽数据对象。
 * @param onTimeSlotClick 时间槽被点击时的回调。
 * @param modifier Modifier 由父级 `Layout` 提供，包含大小信息。
 */
@Composable
fun TimeSlotItem(
    timeSlot: TimeSlot,
    onTimeSlotClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // --- Get display info ---
    val startInstant = Instant.fromEpochMilliseconds(timeSlot.startTime)
    val endInstant = Instant.fromEpochMilliseconds(timeSlot.endTime)
    val startTimeLocal = startInstant.toLocalDateTime(TimeZone.currentSystemDefault())
    val endTimeLocal = endInstant.toLocalDateTime(TimeZone.currentSystemDefault())

    val title = timeSlot.displayTitle ?: "无标题"
    val details = if (timeSlot.scheduleType == ScheduleType.ORDINARY) {
        timeSlot.displaySubtitle // 只显示地点，不显示时间
    } else {
        if (!timeSlot.displaySubtitle.isNullOrBlank()) {
            "@${timeSlot.displaySubtitle}"
        } else {
            "${startTimeLocal.format(timeFormatter)} - ${endTimeLocal.format(timeFormatter)}"
        }
    }

    // 判断是否为时间点
    val isTimePoint = timeSlot.startTime == timeSlot.endTime

    // --- Colors ---
    val isDarkTheme = isSystemInDarkTheme()
    val (backgroundColor, contentColor) = ColorUtils.calculateTimeSlotColors(
        timeSlot.displayColor,
        timeSlot.scheduleId,
        isDarkTheme,
        MaterialTheme.colorScheme
    )

    if (isTimePoint) {
        val (_, contentColor) = ColorUtils.calculateTimeSlotColors(
            ColorSchemeEnum.SURFACECONTAINER,
            timeSlot.scheduleId,
            isDarkTheme,
            MaterialTheme.colorScheme
        )
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
                .clickable(onClick = onTimeSlotClick)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 2.dp),
                color = contentColor
            )
            HorizontalDivider(
                thickness = 2.dp,
                color = backgroundColor
            )
        }
    } else {
        // 普通时间段显示为卡片样式
        Card(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 1.dp)
                .clickable(onClick = onTimeSlotClick),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor,
                contentColor = contentColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.sp,
                    color = contentColor,
                )
                if (!details.isNullOrBlank()) {
                    Text(
                        text = details,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = contentColor.copy(alpha = 0.8f),
                        lineHeight = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * 周视图位置信息数据类，包含位置和尺寸信息
 */
private data class WeekViewSlotInfo(
    val dayOfWeek: Int, // ISO周几 (1-7, 周一到周日)
    val startMinutes: Int, // 距离网格开始的分钟数
    val endMinutes: Int, // 距离网格开始的结束分钟数
    val column: Int = 0, // 在同一天内的列索引
    val maxColumns: Int = 1 // 同一天内的最大列数
)

/**
 * 新的课表内容区域，使用 HorizontalPager 实现周切换。
 */
@Composable
fun ScheduleGridWithTimeSlots(
    timeSlots: List<TimeSlot>,
    dayWidth: Dp,
    hourHeight: Dp,
    gridStartHour: Int,
    gridEndHour: Int,
    totalGridHeight: Dp,
    onTimeSlotClick: (TimeSlot) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // 处理同一天内事件的重叠情况 - 使用增强版算法
    val processedSlots = calculateWeekViewEventPositionsEnhanced(timeSlots, gridStartHour)

    Layout(
        modifier = modifier.height(totalGridHeight), // Apply fixed height
        content = {
            // 1. Background Grid (Must be the first item)
            WeekGrid(
                startHour = gridStartHour,
                endHour = gridEndHour,
                hourHeight = hourHeight,
                dayWidth = dayWidth,
                modifier = Modifier.fillMaxSize() // Grid fills the Layout bounds
            )

            // 2. TimeSlot Items (Order matters!)
            processedSlots.forEach { (timeSlot, slotInfo) ->
                TimeSlotItem(
                    timeSlot = timeSlot,
                    // Pass only necessary data, size/position handled by Layout
                    onTimeSlotClick = { onTimeSlotClick(timeSlot) }
                )
            }
        }
    ) { measurables, constraints ->
        val gridMeasurable = measurables.firstOrNull()
        val timeSlotMeasurables = measurables.drop(1)

        require(gridMeasurable != null) { "WeekGrid must be the first child of ScheduleGridWithTimeSlots" }
        require(timeSlotMeasurables.size == processedSlots.size) { "Number of TimeSlotItems does not match number of processed slots" }

        val layoutWidth = constraints.maxWidth
        val layoutHeight = with(density) { totalGridHeight.toPx() }.roundToInt()

        // Measure grid to fill the layout space
        val gridPlaceable = gridMeasurable.measure(
            Constraints.fixed(layoutWidth, layoutHeight)
        )

        val placeablesWithCoords = mutableListOf<Pair<Placeable, IntOffset>>()

        // Pre-calculate pixel values
        val hourHeightPx = with(density) { hourHeight.toPx() }
        val dayWidthPx = with(density) { dayWidth.toPx() }
        // Define paddings used INSIDE TimeSlotItem for size calculation adjustment
        val itemHorizontalPadding = 4.dp // 2.dp horizontal padding on each side *inside* the card
        val itemVerticalPadding = 2.dp   // 1.dp vertical padding on top/bottom *inside* the card
        val itemHorizontalPaddingPx = with(density) { itemHorizontalPadding.toPx() }
        val itemVerticalPaddingPx = with(density) { itemVerticalPadding.toPx() }

        // 计算20分钟对应的像素高度（最小高度）
        val minDurationMinutes = 20
        val minHeightPx = (minDurationMinutes / 60.0f * hourHeightPx).toFloat()

        timeSlotMeasurables.forEachIndexed { index, measurable ->
            val (timeSlot, slotInfo) = processedSlots[index]

            // Y Offset (Top edge of the slot)
            val yOffsetPx = (slotInfo.startMinutes / 60.0 * hourHeightPx).toFloat()

            // Height (Calculate full slot height first)
            val durationMinutes = slotInfo.endMinutes - slotInfo.startMinutes

            // 时间点或短于20分钟的事件使用最小高度
            val fullSlotHeightPx = if (durationMinutes <= minDurationMinutes) {
                minHeightPx
            } else {
                (durationMinutes / 60.0 * hourHeightPx).toFloat()
            }

            // Actual measurable height = full height - internal padding
            val itemHeightPx = (fullSlotHeightPx - itemVerticalPaddingPx).coerceAtLeast(minHeightPx)

            // 计算水平位置和宽度（考虑重叠）
            val hasOverlap = slotInfo.maxColumns > 1
            val dayOfWeek = slotInfo.dayOfWeek

            // 计算x偏移和宽度
            val baseXOffset = ((dayOfWeek - 1) * dayWidthPx).toFloat()

            // 计算实际宽度和偏移
            val (itemWidthPx, xOffsetPx) = if (hasOverlap) {
                // 重叠事件使用更合理的宽度和间距
                val gapPx = 1f // 增大列间距，改善视觉效果
                val marginPx = 1f // 增大边距，避免贴边

                // 记录调试信息
                Log.d(
                    "WeekSchedule",
                    "重叠项目布局: 列=${slotInfo.column}/${slotInfo.maxColumns}, 事件='${timeSlot.displayTitle}'"
                )

                // 可用宽度 = 日宽度 - 两侧边距 - 列间隙宽度总和
                val availableWidth =
                    dayWidthPx - (2 * marginPx) - (gapPx * (slotInfo.maxColumns - 1))

                // 确保每列宽度合理
                val columnWidth =
                    (availableWidth / slotInfo.maxColumns).coerceAtLeast(20f) // 确保最小宽度

                // 计算该事件的水平偏移
                val columnOffset = marginPx + (columnWidth + gapPx) * slotInfo.column

                // 最终宽度和偏移
                val width = (columnWidth - itemHorizontalPaddingPx).coerceAtLeast(15f) // 确保最小宽度
                val offset = baseXOffset + columnOffset

                // 确保不超出日宽度
                val maxRightEdge = dayWidthPx - marginPx
                val rightEdge = offset + width + itemHorizontalPaddingPx
                val finalOffset = if (rightEdge > maxRightEdge) {
                    // 如果超出右边界，适当左移
                    offset - (rightEdge - maxRightEdge)
                } else offset

                Pair(width, finalOffset.coerceAtLeast(baseXOffset + marginPx)) // 确保不会偏移到前一天
            } else {
                // 无重叠时使用接近全宽 (但留出更多边距)
                val margin = 4f // 增大边距，提升美观度
                val width = (dayWidthPx - (2 * margin) - itemHorizontalPaddingPx).coerceAtLeast(20f)
                val offset = baseXOffset + margin

                Pair(width, offset)
            }

            // 添加详细日志，帮助调试
            if (hasOverlap) {
                Log.d(
                    "WeekSchedule",
                    "重叠项目详细 - ID: ${timeSlot.id}, 标题: '${timeSlot.displayTitle}', " +
                            "列: ${slotInfo.column}/${slotInfo.maxColumns}, 宽度: ${itemWidthPx}px, 偏移: ${xOffsetPx - baseXOffset}px"
                )
            }

            // Validate before measuring
            if (itemHeightPx <= 0 || itemWidthPx <= 0 || yOffsetPx < 0 || xOffsetPx < 0 || yOffsetPx + itemHeightPx > layoutHeight + hourHeightPx /* Allow some overflow? */) {
                Log.w(
                    "ScheduleGridWithTimeSlots",
                    "Skipping invalid TimeSlot layout: ${timeSlot.id}"
                )
            } else {
                try {
                    // Measure the TimeSlotItem with the calculated size (excluding padding)
                    val itemConstraints = Constraints.fixed(
                        width = itemWidthPx.roundToInt(),
                        height = itemHeightPx.roundToInt()
                    )
                    val placeable = measurable.measure(itemConstraints)

                    // Calculate placement coordinates (top-left corner)
                    // Add half the internal padding to the offset to center the content area
                    val placeX = (xOffsetPx + itemHorizontalPaddingPx / 2).roundToInt()
                    val placeY = (yOffsetPx + itemVerticalPaddingPx / 2).roundToInt()

                    placeablesWithCoords.add(placeable to IntOffset(placeX, placeY))
                } catch (e: Exception) {
                    Log.e("ScheduleGridWithTimeSlots", "Error measuring TimeSlot ${timeSlot.id}", e)
                }
            }
        }

        // Set the size of the Layout composable itself
        layout(layoutWidth, layoutHeight) {
            // Place the grid background first
            gridPlaceable.placeRelative(0, 0)

            // Place the time slots on top
            placeablesWithCoords.forEach { (placeable, coords) ->
                placeable.placeRelative(coords.x, coords.y)
            }
        }
    }
}

/**
 * 增强版：计算周视图事件的位置，提供更优的重叠事件处理
 * 使用优化的算法确保重叠事件分配到合适的列，并且显示效果更佳
 */
private fun calculateWeekViewEventPositionsEnhanced(
    events: List<TimeSlot>,
    gridStartHour: Int
): List<Pair<TimeSlot, WeekViewSlotInfo>> {
    if (events.isEmpty()) return emptyList()

    // 设置最小时间段为20分钟（用于计算重叠）
    val minDurationMinutes = 20

    // 转换事件为带有日期和时间信息的对象
    val eventWithTimes = events.map { event ->
        val start = Instant.fromEpochMilliseconds(event.startTime)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val end = Instant.fromEpochMilliseconds(event.endTime)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        val dayOfWeek = start.dayOfWeek.isoDayNumber
        val startMinutes = (start.hour - gridStartHour) * 60 + start.minute

        // 计算原始的结束分钟数
        val originalEndMinutes = (end.hour - gridStartHour) * 60 + end.minute

        // 应用最小持续时间规则：时间点日程和短于minDurationMinutes的日程都以minDurationMinutes计算重叠
        val durationMinutes = originalEndMinutes - startMinutes
        val endMinutes = if (durationMinutes < minDurationMinutes) {
            startMinutes + minDurationMinutes
        } else {
            originalEndMinutes
        }

        Log.d(
            "WeekSchedule",
            "事件 ${event.displayTitle} - 原始持续时间: ${durationMinutes}分钟, 用于计算重叠的持续时间: ${endMinutes - startMinutes}分钟"
        )

        Triple(event, dayOfWeek, WeekViewSlotInfo(dayOfWeek, startMinutes, endMinutes))
    }

    // 按天分组
    val eventsByDay = eventWithTimes.groupBy { it.second }

    // 处理结果列表
    val result = mutableListOf<Pair<TimeSlot, WeekViewSlotInfo>>()

    // 处理每天的事件
    eventsByDay.forEach { (day, dayEvents) ->
        // 只有一个事件时不需要处理重叠
        if (dayEvents.size == 1) {
            val (event, _, slotInfo) = dayEvents[0]
            result.add(event to slotInfo.copy(column = 0, maxColumns = 1))
            return@forEach
        }

        // 按开始时间排序
        val sortedEvents = dayEvents.sortedBy { it.third.startMinutes }

        // 构建重叠矩阵
        val size = sortedEvents.size
        val overlapMatrix = Array(size) { BooleanArray(size) { false } }

        for (i in 0 until size) {
            for (j in i + 1 until size) {
                val a = sortedEvents[i].third
                val b = sortedEvents[j].third

                // 检查是否重叠（添加1分钟缓冲区）
                val buffer = 1
                if (a.startMinutes < (b.endMinutes - buffer) && (a.endMinutes - buffer) > b.startMinutes) {
                    overlapMatrix[i][j] = true
                    overlapMatrix[j][i] = true
                }
            }
        }

        // 找出重叠组（连通分量）
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

        // 处理每个重叠组
        for (group in overlapGroups) {
            // 单个事件直接处理
            if (group.size == 1) {
                val idx = group[0]
                val (event, _, slotInfo) = sortedEvents[idx]
                result.add(event to slotInfo.copy(column = 0, maxColumns = 1))
                continue
            }

            // 按开始时间排序重叠组中的事件
            val groupEventIndices = group.sortedBy { sortedEvents[it].third.startMinutes }

            // 记录每列的结束时间
            val columnEndTimes = mutableListOf<Int>()
            // 记录每个事件被分配的列
            val eventToColumn = mutableMapOf<Int, Int>()

            // 为每个事件分配列
            for (idx in groupEventIndices) {
                val (_, _, slotInfo) = sortedEvents[idx]

                // 尝试找到一个可用的列
                var column = -1
                for (c in columnEndTimes.indices) {
                    if (slotInfo.startMinutes >= columnEndTimes[c]) {
                        column = c
                        break
                    }
                }

                // 如果没找到可用列，创建新列
                if (column == -1) {
                    column = columnEndTimes.size
                    columnEndTimes.add(slotInfo.endMinutes)
                } else {
                    // 更新列的结束时间
                    columnEndTimes[column] = slotInfo.endMinutes
                }

                // 记录事件的列位置
                eventToColumn[idx] = column
            }

            // 确定最大列数
            val maxColumns = columnEndTimes.size

            // 为组内每个事件生成最终位置信息
            for (idx in group) {
                val (event, _, slotInfo) = sortedEvents[idx]
                val column = eventToColumn[idx] ?: 0

                // 创建更新的位置信息
                val updatedSlotInfo = slotInfo.copy(
                    column = column,
                    maxColumns = maxColumns
                )

                // 添加到结果
                result.add(event to updatedSlotInfo)
            }
        }
    }

    return result
}

