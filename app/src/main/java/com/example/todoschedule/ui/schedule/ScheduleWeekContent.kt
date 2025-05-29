package com.example.todoschedule.ui.schedule

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
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
import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.domain.model.TimeSlot
import com.example.todoschedule.ui.theme.ColorSchemeEnum
import com.example.todoschedule.ui.utils.ColorUtils
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
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
    anchorDate: kotlinx.datetime.LocalDate,
    onDateChange: (kotlinx.datetime.LocalDate) -> Unit,
    viewModel: ScheduleViewModel,
    onTimeSlotClick: (TimeSlot) -> Unit
) {
    val allTimeSlots by viewModel.allTimeSlots.collectAsState()
    val pageCount = 1000
    val initialPage = pageCount / 2
    val systemToday = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val systemMonday =
        systemToday.minus((systemToday.dayOfWeek.isoDayNumber - 1).toLong(), DateTimeUnit.DAY)
    val anchorMonday =
        anchorDate.minus((anchorDate.dayOfWeek.isoDayNumber - 1).toLong(), DateTimeUnit.DAY)
    val weeksDiff = anchorMonday.daysUntil(systemMonday) * -1 / 7
    val targetPage = initialPage + weeksDiff

    // 1. PagerState 只初始化一次，避免重组导致的卡顿
    val pagerState = rememberPagerState(initialPage = targetPage) { pageCount }

    // 2. 当前页面对应的周一日期，直接由 page 索引推导
    val currentMonday = remember(pagerState.currentPage) {
        systemMonday.plus((pagerState.currentPage - initialPage) * 7, DateTimeUnit.DAY)
    }

    // 3. 监听 anchorDate 变化，外部跳转时自动 scrollToPage
    LaunchedEffect(anchorDate) {
        val anchorMonday =
            anchorDate.minus((anchorDate.dayOfWeek.isoDayNumber - 1).toLong(), DateTimeUnit.DAY)
        val newPage = initialPage + (anchorMonday.daysUntil(systemMonday) * -1) / 7
        android.util.Log.d(
            "ScheduleWeekContent",
            "anchorDate: $anchorDate, anchorMonday: $anchorMonday, newPage: $newPage"
        )
        if (pagerState.currentPage != newPage) {
            pagerState.scrollToPage(newPage)
        }
    }

    // 4. 只在 currentMonday 变化时回调 onDateChange，避免死循环
    LaunchedEffect(currentMonday) {
        if (currentMonday != anchorDate) {
            onDateChange(currentMonday)
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val dayColumnWidth = (screenWidthDp - TIME_AXIS_WIDTH) / 7
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        val mondayOfWeek = systemMonday.plus((page - initialPage) * 7, DateTimeUnit.DAY)
        val weekDates = List(7) { i -> mondayOfWeek.plus(i.toLong(), DateTimeUnit.DAY) }
        val weekTimeSlots = allTimeSlots.filter { slot ->
            val slotDate = Instant.fromEpochMilliseconds(slot.startTime)
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            weekDates.contains(slotDate)
        }
        WeekSchedulePage(
            weekDates = weekDates,
            timeSlots = weekTimeSlots,
            dayWidth = dayColumnWidth,
            onTimeSlotClick = onTimeSlotClick
        )
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

    // 统一重叠处理逻辑，分别对每一天计算重叠，补充 dayOfWeek 信息
    val processedSlots = timeSlots.groupBy { slot ->
        val start = Instant.fromEpochMilliseconds(slot.startTime)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        start.date
    }.flatMap { (date, slots) ->
        calculateEventPositions(slots, date).map { (slot, info) ->
            val start = Instant.fromEpochMilliseconds(slot.startTime)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val dayOfWeek = start.dayOfWeek.isoDayNumber
            slot to WeekViewSlotInfo(
                dayOfWeek = dayOfWeek,
                startMinutes = info.startMinutes,
                endMinutes = info.endMinutes,
                column = info.column,
                maxColumns = info.maxColumns
            )
        }
    }

    Layout(
        modifier = modifier.height(totalGridHeight),
        content = {
            // 1. 背景网格
            WeekGrid(
                startHour = gridStartHour,
                endHour = gridEndHour,
                hourHeight = hourHeight,
                dayWidth = dayWidth,
                modifier = Modifier.fillMaxSize()
            )
            // 2. 课表卡片
            processedSlots.forEach { (timeSlot, slotInfo) ->
                TimeSlotItem(
                    timeSlot = timeSlot,
                    onTimeSlotClick = { onTimeSlotClick(timeSlot) }
                )
            }
        }
    ) { measurables, constraints ->
        val gridMeasurable = measurables.firstOrNull()
        val timeSlotMeasurables = measurables.drop(1)
        val layoutWidth = constraints.maxWidth
        val layoutHeight = with(density) { totalGridHeight.toPx() }.roundToInt()
        val gridPlaceable = gridMeasurable?.measure(
            Constraints.fixed(layoutWidth, layoutHeight)
        )
        val placeablesWithCoords = mutableListOf<Pair<Placeable, IntOffset>>()
        val hourHeightPx = with(density) { hourHeight.toPx() }
        val dayWidthPx = with(density) { dayWidth.toPx() }
        val itemHorizontalPadding = 4.dp
        val itemVerticalPadding = 2.dp
        val itemHorizontalPaddingPx = with(density) { itemHorizontalPadding.toPx() }
        val itemVerticalPaddingPx = with(density) { itemVerticalPadding.toPx() }
        val minDurationMinutes = 20
        val minHeightPx = (minDurationMinutes / 60.0f * hourHeightPx).toFloat()
        timeSlotMeasurables.forEachIndexed { index, measurable ->
            val (timeSlot, slotInfo) = processedSlots[index]
            val yOffsetPx = (slotInfo.startMinutes / 60.0 * hourHeightPx).toFloat()
            val durationMinutes = slotInfo.endMinutes - slotInfo.startMinutes
            val fullSlotHeightPx = if (durationMinutes <= minDurationMinutes) {
                minHeightPx
            } else {
                (durationMinutes / 60.0 * hourHeightPx).toFloat()
            }
            val itemHeightPx = (fullSlotHeightPx - itemVerticalPaddingPx).coerceAtLeast(minHeightPx)
            val hasOverlap = slotInfo.maxColumns > 1
            val dayOfWeek = slotInfo.dayOfWeek
            val baseXOffset = ((dayOfWeek - 1) * dayWidthPx).toFloat()
            // 优化重叠事件的宽度和偏移，避免宽度为0
            val (itemWidthPx, xOffsetPx) = if (hasOverlap) {
                val gapPx = 1f // 列间距
                val marginPx = 1f // 两侧边距
                val availableWidth =
                    dayWidthPx - (2 * marginPx) - (gapPx * (slotInfo.maxColumns - 1))
                val columnWidth = (availableWidth / slotInfo.maxColumns).coerceAtLeast(20f)
                val columnOffset = marginPx + (columnWidth + gapPx) * slotInfo.column
                val width = (columnWidth - itemHorizontalPaddingPx).coerceAtLeast(15f)
                val offset = baseXOffset + columnOffset
                Pair(width, offset)
            } else {
                val margin = 4f
                val width = (dayWidthPx - (2 * margin) - itemHorizontalPaddingPx).coerceAtLeast(20f)
                val offset = baseXOffset + margin
                Pair(width, offset)
            }
            // 调试日志
            if (hasOverlap) {
                android.util.Log.d(
                    "WeekSchedule",
                    "重叠项目: '${timeSlot.displayTitle}', 列: ${slotInfo.column}/${slotInfo.maxColumns}, 宽度: ${itemWidthPx}px, 偏移: ${xOffsetPx - baseXOffset}px"
                )
            }
            if (itemHeightPx <= 0 || itemWidthPx <= 0 || yOffsetPx < 0 || xOffsetPx < 0 || yOffsetPx + itemHeightPx > layoutHeight + hourHeightPx) {
                android.util.Log.w(
                    "ScheduleGridWithTimeSlots",
                    "跳过无效布局: ${timeSlot.id}"
                )
            } else {
                try {
                    val itemConstraints = Constraints.fixed(
                        width = itemWidthPx.roundToInt(),
                        height = itemHeightPx.roundToInt()
                    )
                    val placeable = measurable.measure(itemConstraints)
                    val placeX = (xOffsetPx + itemHorizontalPaddingPx / 2).roundToInt()
                    val placeY = (yOffsetPx + itemVerticalPaddingPx / 2).roundToInt()
                    placeablesWithCoords.add(placeable to IntOffset(placeX, placeY))
                } catch (e: Exception) {
                    android.util.Log.e(
                        "ScheduleGridWithTimeSlots",
                        "测量TimeSlot出错: ${timeSlot.id}",
                        e
                    )
                }
            }
        }
        layout(layoutWidth, layoutHeight) {
            gridPlaceable?.placeRelative(0, 0)
            placeablesWithCoords.forEach { (placeable, coords) ->
                placeable.placeRelative(coords.x, coords.y)
            }
        }
    }
}

