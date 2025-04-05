package com.example.todoschedule.ui.schedule

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.model.CourseNode
import com.example.todoschedule.domain.model.TimeSlot
import com.example.todoschedule.ui.navigation.NavigationState
import com.example.todoschedule.ui.schedule.model.ScheduleUiState
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max

/**
 * 课程表屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navigationState: NavigationState,
    onNavigateToSettings: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    // 状态
    val uiState by viewModel.uiState.collectAsState()
    val currentWeek by viewModel.currentWeek.collectAsState()
    val weekDates by viewModel.weekDates.collectAsState()
    val displayableTimeSlots by viewModel.displayableTimeSlots.collectAsState()
    val defaultTableId by viewModel.defaultTableIdState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("第${currentWeek}周") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // 上一周
                    IconButton(onClick = { viewModel.previousWeek() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上一周")
                    }

                    // 回到当前周
                    IconButton(onClick = { viewModel.goToCurrentWeek() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "回到本周")
                    }

                    // 下一周
                    IconButton(onClick = { viewModel.nextWeek() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下一周")
                    }

                    // 设置
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            // 只在 Success 状态下显示 FAB
            if (uiState is ScheduleUiState.Success) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 下载按钮 (选择学校)
                    FloatingActionButton(
                        onClick = { navigationState.navigateSchoolSelectorScreen(defaultTableId) },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "选择学校"
                        )
                    }

                    // 添加日程按钮
                    FloatingActionButton(
                        onClick = { navigationState.navigateToAddEditOrdinarySchedule() },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ) {
                        Icon(
                            Icons.Default.Event,
                            contentDescription = "添加日程"
                        )
                    }

                    // 添加课程按钮
                    FloatingActionButton(
                        onClick = { navigationState.navigateToAddCourse(defaultTableId) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加课程")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                ScheduleUiState.Loading -> LoadingScreen()
                ScheduleUiState.Success -> {
                    // 成功状态下，检查时间槽是否为空
                    if (displayableTimeSlots.isEmpty()) {
                        // TODO: 区分是真的这周没课/日程，还是因为没有默认课表？
                        // 目前 _defaultTableIdState 的逻辑应保证 Success 时 tableId 有效
                        // 所以这里是真 · 空状态
                        EmptyScheduleScreen()
                    } else {
                        ScheduleContent(
                            tableId = defaultTableId,
                            weekDates = weekDates,
                            timeSlots = displayableTimeSlots,
                            onTimeSlotClick = { timeSlot ->
                                Log.d(
                                    "ScheduleScreen",
                                    "Clicked on TimeSlot: Type=${timeSlot.scheduleType}, ScheduleID=${timeSlot.scheduleId}, Head='${timeSlot.head}'"
                                )
                                when (timeSlot.scheduleType) {
                                    ScheduleType.COURSE -> {
                                        if (defaultTableId != AppConstants.Ids.INVALID_TABLE_ID) {
                                            Log.i(
                                                "ScheduleScreen",
                                                "Navigating to Course Detail: tableId=$defaultTableId, courseId=${timeSlot.scheduleId}"
                                            )
                                            navigationState.navigateToCourseDetail(
                                                tableId = defaultTableId,
                                                courseId = timeSlot.scheduleId
                                            )
                                        } else {
                                            Log.w(
                                                "ScheduleScreen",
                                                "Cannot navigate to course detail, defaultTableId is invalid."
                                            )
                                        }
                                    }

                                    ScheduleType.ORDINARY -> {
                                        Log.i(
                                            "ScheduleScreen",
                                            "Navigating to Ordinary Schedule Detail for scheduleId=${timeSlot.scheduleId}"
                                        )
                                        navigationState.navigateToOrdinaryScheduleDetail(timeSlot.scheduleId)
                                    }

                                    else -> {
                                        Log.w(
                                            "ScheduleScreen",
                                            "Navigation not implemented for ScheduleType: ${timeSlot.scheduleType}"
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
                is ScheduleUiState.Error -> ErrorScreen((uiState as ScheduleUiState.Error).message)
                // 处理新的 NoTableSelected 状态
                ScheduleUiState.NoTableSelected -> {
                    NoTableSelectedScreen(
                        navigationState = navigationState,
                        onNavigateToImport = {
                            navigationState.navigateSchoolSelectorScreen(
                                AppConstants.Ids.INVALID_TABLE_ID
                            )
                        }
                    )
                }

                ScheduleUiState.Empty -> EmptyScheduleScreen() // 将 Empty 状态映射到空屏幕
            }
        }
    }
}

/**
 * 加载中屏幕
 */
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "加载中...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * 空课表屏幕
 */
@Composable
fun EmptyScheduleScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "没有课程",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击右下角的 + 按钮添加课程",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 错误屏幕
 */
@Composable
fun ErrorScreen(errorMessage: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "出错了",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 没有选中/没有默认课表的提示屏幕
 */
@Composable
fun NoTableSelectedScreen(
    navigationState: NavigationState,
    onNavigateToImport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("您还没有选择或创建课表", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("请先创建新课表或从教务系统导入", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNavigateToImport) {
            Text("从教务系统导入课表")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navigationState.navigateToCreateEditTable() }) {
            Text("手动创建新课表")
        }
        // TODO: 添加选择现有课表的按钮 (如果允许多课表)
    }
}

/**
 * 课表内容
 */
@Composable
fun ScheduleContent(
    tableId: Int,
    weekDates: List<LocalDate>,
    timeSlots: List<TimeSlot>,
    onTimeSlotClick: (TimeSlot) -> Unit
) {
    val scrollState = rememberScrollState()
    val maxNodes = 12 // TODO: 从配置或计算得出最大节数 (对应小时数?)
    val hourHeight = 60.dp // 每小时的高度
    val totalHeight = (maxNodes * hourHeight.value).dp

    // State to hold the calculated width for a day column
    var dayWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // 星期和日期标题行
        WeekHeader(weekDates = weekDates)

        // 时间轴和课程/日程格子
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
                // Calculate dayWidth once the layout is measured
                .onGloballyPositioned { coordinates ->
                    with(density) {
                        // Calculate width available for 7 days after the time axis
                        val availableWidth = coordinates.size.width.toDp() - TIME_AXIS_WIDTH
                        // Ensure positive width before calculating dayWidth
                        if (availableWidth > 0.dp) {
                            dayWidth = availableWidth / 7
                        }
                    }
                }
        ) {
            // 时间轴 (左侧)
            TimeAxis(maxNodes = maxNodes, hourHeight = hourHeight)

            // 绘制背景网格线 (需要 dayWidth)
            if (dayWidth > 0.dp) { // Only draw grid if dayWidth is calculated
                WeekGrid(maxNodes = maxNodes, hourHeight = hourHeight, dayWidth = dayWidth)
            }

            // 放置 TimeSlot Items (需要 dayWidth)
            if (dayWidth > 0.dp) { // Only place items if dayWidth is calculated
                timeSlots.forEach { timeSlot ->
                    TimeSlotItem(
                        timeSlot = timeSlot,
                        hourHeight = hourHeight,
                        dayWidth = dayWidth,
                        gridStartHour = 8, // Assume grid starts at 8 AM
                        onTimeSlotClick = { onTimeSlotClick(timeSlot) },
                    )
                }
            }
        }
    }
}

/**
 * 周日期头部
 */
@Composable
fun WeekHeader(weekDates: List<LocalDate>) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        // 时间列占位
        item {
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .padding(horizontal = 2.dp)
            )
        }

        // 日期项
        items(weekDates) { date ->
            val isToday = date == today
            // 使用 DayOfWeek 枚举值获取星期几
            val dayOfWeekName = when (date.dayOfWeek) {
                DayOfWeek.MONDAY -> "周一"
                DayOfWeek.TUESDAY -> "周二"
                DayOfWeek.WEDNESDAY -> "周三"
                DayOfWeek.THURSDAY -> "周四"
                DayOfWeek.FRIDAY -> "周五"
                DayOfWeek.SATURDAY -> "周六"
                DayOfWeek.SUNDAY -> "周日"
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 2.dp),
            ) {
                // 星期
                Text(
                    text = dayOfWeekName.substring(0, 2),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )

                // 日期
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
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
}

/**
 * 时间轴
 */
@Composable
fun TimeAxis(maxNodes: Int, hourHeight: Dp) {
    val times = listOf(
        "8:00", "9:00", "10:00", "11:00", "12:00",
        "13:00", "14:00", "15:00", "16:00", "17:00",
        "18:00", "19:00", "20:00"
    )

    Column(
        modifier = Modifier
            .width(30.dp)
            .padding(end = 2.dp)
    ) {
        times.forEachIndexed { index, time ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(60.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = (index + 1).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 绘制背景网格线
 */
@Composable
fun WeekGrid(maxNodes: Int, hourHeight: Dp, dayWidth: Dp) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Horizontal lines
        (0..maxNodes).forEach { node -> // Start from 0 for the top line
            Divider(
                color = Color.LightGray.copy(alpha = 0.3f),
                thickness = 0.5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .offset(y = (node * hourHeight.value).dp)
            )
        }
        // Vertical lines (after time axis)
        (0..7).forEach { dayIndex -> // Draw 8 lines for 7 columns
            Divider(
                color = Color.LightGray.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(0.5.dp)
                    .align(Alignment.TopStart)
                    // Offset starts from TIME_AXIS_WIDTH
                    .offset(x = TIME_AXIS_WIDTH + (dayIndex * dayWidth.value).dp)
            )
        }
    }
}

// Formatter for displaying time in TimeSlotItem
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * 显示单个时间槽的 Composable
 */
@Composable
fun TimeSlotItem(
    timeSlot: TimeSlot,
    hourHeight: Dp,
    dayWidth: Dp,
    gridStartHour: Int,
    onTimeSlotClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // 1. 计算 TimeSlot 的位置和大小
    val startInstant = Instant.fromEpochMilliseconds(timeSlot.startTime)
    val endInstant = Instant.fromEpochMilliseconds(timeSlot.endTime)
    val startTimeLocal = startInstant.toLocalDateTime(TimeZone.currentSystemDefault())
    val endTimeLocal = endInstant.toLocalDateTime(TimeZone.currentSystemDefault())
    val dayOfWeek = startTimeLocal.dayOfWeek.isoDayNumber
    val startHourFraction = startTimeLocal.hour + startTimeLocal.minute / 60.0
    val durationMillis = max(1, timeSlot.endTime - timeSlot.startTime)
    val durationHours = durationMillis / (1000.0 * 60 * 60)
    val topOffset = ((startHourFraction - gridStartHour) * hourHeight.value).dp
    val itemHeight = (durationHours * hourHeight.value).dp
    val leftOffset = TIME_AXIS_WIDTH + ((dayOfWeek - 1) * dayWidth.value).dp
    val itemWidth = dayWidth - 4.dp
    val finalHeight = max(1f, itemHeight.value - 4).dp
    val finalWidth = max(1f, itemWidth.value).dp

    // 2. 获取显示信息和颜色 (使用新字段)
    val title = timeSlot.displayTitle // 使用 displayTitle
    // 格式化时间 + 地点/副标题
    val timeString =
        "${startTimeLocal.format(timeFormatter)} - ${endTimeLocal.format(timeFormatter)}"
    val details = if (!timeSlot.displaySubtitle.isNullOrBlank()) {
        "$timeString\n@${timeSlot.displaySubtitle}" // 如果有副标题，换行显示
    } else {
        timeString
    }

    // 解析背景颜色，提供默认值
    val backgroundColor = remember(timeSlot.displayColor) { // Remember based on color string
        try {
            timeSlot.displayColor?.let { Color(it.toColorInt()) }
                ?: fallbackColor(timeSlot.scheduleType)
        } catch (e: Exception) {
            Log.w("TimeSlotItem", "Invalid color string: ${timeSlot.displayColor}, using fallback.")
            fallbackColor(timeSlot.scheduleType)
        }
    }
    // 根据背景色决定内容颜色以确保可读性
    val contentColor = remember(backgroundColor) {
        if (backgroundColor.isDark()) Color.White else Color.Black.copy(alpha = 0.87f)
    }

    // Prevent item from drawing outside the grid boundaries (optional but good practice)
    if (topOffset < 0.dp || leftOffset < TIME_AXIS_WIDTH || dayWidth <= 0.dp) {
        // Log error or skip rendering if position calculation is invalid
        Log.e(
            "TimeSlotItem",
            "Invalid position calculated for TimeSlot ${timeSlot.id}, skipping render."
        )
        return // Don't draw if position is invalid
    }

    Card(
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .offset(x = leftOffset, y = topOffset)
            .width(finalWidth)
            .height(finalHeight)
            .clickable(onClick = onTimeSlotClick),
        shape = RoundedCornerShape(6.dp),
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
            verticalArrangement = Arrangement.Top,
        ) {
            // 显示标题
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 2, // 允许标题最多两行
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            // 显示详情 (时间 +/- 地点)
            Text(
                text = details,
                fontSize = 9.sp,
                maxLines = 2, // 允许详情两行 (时间+地点)
                overflow = TextOverflow.Ellipsis,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

private fun LocalDateTime.format(formatter: DateTimeFormatter): String {
    return this.toJavaLocalDateTime().format(formatter)
}

// 辅助函数：根据类型提供备用颜色
private fun fallbackColor(type: ScheduleType): Color {
    return when (type) {
        ScheduleType.COURSE -> Color(0xFF03A9F4) // 蓝色
        ScheduleType.ORDINARY -> Color(0xFF4CAF50) // 绿色
        ScheduleType.HOMEWORK -> Color(0xFFFF9800) // 橙色
        ScheduleType.EXAM -> Color(0xFFF44336) // 红色
        else -> Color.LightGray
    }
}

// 辅助函数：判断颜色是否深色 (可能需要调整阈值)
fun Color.isDark(): Boolean {
    val darkness = 1 - (0.299 * red + 0.587 * green + 0.114 * blue) * alpha
    return darkness >= 0.5 // 阈值可以调整
}

// 假设的时间轴宽度常量
private val TIME_AXIS_WIDTH = 40.dp

@Preview(showBackground = true)
@Composable
fun PreviewScheduleScreen() {
    val viewModel = hiltViewModel<ScheduleViewModel>()
    val navController = rememberNavController()
    val navigationState = remember(navController) {
        NavigationState(navController)
    }
    ScheduleScreen(navigationState, {}, viewModel)
}

@Preview(showBackground = true)
@Composable
fun PreviewScheduleContent() {
    // 示例数据
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val weekDates = List(7) { index -> today.plus(DatePeriod(days = index)) }

    val weekCourses = listOf<Course>(
        Course(
            id = 1,
            courseName = "数学",
            nodes = listOf(
                CourseNode(
                    day = 1, startNode = 1, step = 2, room = "教室A", startWeek = 1, endWeek = 16,
                    weekType = 1, teacher = "张老师"
                ),
                CourseNode(
                    day = 4, startNode = 1, step = 2, room = "教室A", startWeek = 1, endWeek = 16,
                    weekType = 1, teacher = "张老师"
                )
            ),
            color = "#FF5733"
        ),
        Course(
            id = 2,
            courseName = "英语",
            nodes = listOf(
                CourseNode(
                    day = 1,
                    startNode = 3,
                    step = 2,
                    room = "教室B",
                    startWeek = 1,
                    endWeek = 16,
                    weekType = 1,
                    teacher = "李老师"
                )
            ),
            color = "#33FF57"
        ),
        Course(
            id = 3,
            courseName = "物理",
            nodes = listOf(
                CourseNode(
                    day = 1,
                    startNode = 5,
                    step = 2,
                    room = "教室C",
                    startWeek = 1,
                    endWeek = 16,
                    weekType = 1,
                    teacher = "王老师"
                )
            ),
            color = "#3357FF"
        ),
        Course(
            id = 4,
            courseName = "化学",
            nodes = listOf(
                CourseNode(
                    day = 1,
                    startNode = 7,
                    step = 2,
                    room = "教室D",
                    startWeek = 1,
                    endWeek = 16,
                    weekType = 1,
                    teacher = "赵老师"
                )
            ),
            color = "#FF33A1"
        ),
        Course(
            id = 5,
            courseName = "生物",
            nodes = listOf(
                CourseNode(
                    day = 1,
                    startNode = 9,
                    step = 2,
                    room = "教室E",
                    startWeek = 1,
                    endWeek = 16,
                    weekType = 1,
                    teacher = "孙老师"
                )
            ),
            color = "#A133FF"
        ),
        Course(
            id = 6,
            courseName = "地理",
            nodes = listOf(
                CourseNode(
                    day = 1,
                    startNode = 11,
                    step = 2,
                    room = "教室F",
                    startWeek = 1,
                    endWeek = 16,
                    weekType = 1,
                    teacher = "周老师"
                ),
                CourseNode(
                    day = 5,
                    startNode = 11,
                    step = 2,
                    room = "教室F",
                    startWeek = 1,
                    endWeek = 16,
                    weekType = 1,
                    teacher = "周老师"
                )
            ),
            color = "#FFA133"
        ),
        Course(
            id = 7,
            courseName = "历史",
            nodes = listOf(
                CourseNode(
                    day = 2,
                    startNode = 1,
                    step = 2,
                    room = "教室G",
                    startWeek = 1,
                    endWeek = 16,
                    weekType = 1,
                    teacher = "吴老师"
                )
            ),
            color = "#33A1FF"
        ),
        Course(
            id = 8,
            courseName = "政治",
            nodes = listOf(
                CourseNode(
                    day = 2,
                    startNode = 3,
                    step = 2,
                    room = "教室H",
                    startWeek = 1,
                    endWeek = 16,
                    weekType = 1,
                    teacher = "钱老师"
                )
            ),
            color = "#A1FF33"
        ),
        Course(
            id = 9,
            courseName = "音乐",
            nodes = listOf(
                CourseNode(
                    day = 2,
                    startNode = 5,
                    step = 2,
                    room = "教室I",
                    startWeek = 1,
                    endWeek = 16,
                    weekType = 1,
                    teacher = "孙老师"
                )
            ),
            color = "#FF33A1"
        ),
        Course(
            id = 10,
            courseName = "美术",
            nodes = listOf(
                CourseNode(
                    day = 2,
                    startNode = 7,
                    step = 2,
                    room = "教室J",
                    startWeek = 1,
                    endWeek = 16,
                    weekType = 1,
                    teacher = "周老师"
                )
            ),
            color = "#33A1FF"
        ),
        Course(
            id = 11,
            courseName = "信息技术",
            nodes = listOf(
                CourseNode(
                    day = 2,
                    startNode = 9,
                    step = 2,
                    room = "教室K",
                    startWeek = 1,
                    endWeek = 16,
                    weekType = 1,
                    teacher = "钱老师"
                )
            ),
            color = "#A1FF33"
        ),
        Course(
            id = 12,
            courseName = "体育",
            nodes = listOf(
                CourseNode(
                    day = 3,
                    startNode = 1,
                    step = 2,
                    room = "教室L",
                    startWeek = 1,
                    endWeek = 16,
                    weekType = 1,
                    teacher = "孙老师"
                )
            ),
            color = "#33FF57"
        ),
    )
    ScheduleContent(
        tableId = 0,
        weekDates = weekDates,
        timeSlots = weekCourses.flatMap { course ->
            course.nodes.map { node ->
                TimeSlot(
                    id = course.id,
                    scheduleType = ScheduleType.COURSE,
                    scheduleId = course.id,
                    head = "${course.courseName} - ${node.room}",
                    startTime = System.currentTimeMillis() + node.startNode * 3600000L, // 使用毫秒时间戳
                    endTime = System.currentTimeMillis() + (node.startNode + node.step) * 3600000L,
                    userId = 1 // 添加userId参数
                )

            }
        },
        onTimeSlotClick = { timeSlot ->
            Log.d(
                "PreviewScheduleContent",
                "Clicked on TimeSlot: ${timeSlot.id}, Type: ${timeSlot.scheduleType}, ScheduleID: ${timeSlot.scheduleId}"
            )
        }
    )
}

