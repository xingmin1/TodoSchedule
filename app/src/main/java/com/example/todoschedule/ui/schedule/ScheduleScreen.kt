package com.example.todoschedule.ui.schedule

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.unit.times
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.model.CourseNode
import com.example.todoschedule.domain.model.TimeSlot
import com.example.todoschedule.ui.navigation.NavigationState
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
import kotlin.random.Random

/**
 * 课程表屏幕 Composable 函数。
 *
 * 负责展示周视图的课程表界面，包括顶部应用栏（周数切换、设置入口）、
 * 课程/日程网格、时间轴以及添加/导入按钮。
 * 它根据 ViewModel 提供的状态 (uiState) 显示不同的界面内容（加载中、成功、空、错误、未选择课表）。
 *
 * @param navigationState 用于处理导航事件的对象。
 * @param onNavigateToSettings 当点击设置图标时触发的回调。
 * @param viewModel ScheduleViewModel 的实例，用于获取界面状态和数据。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navigationState: NavigationState,
    onNavigateToSettings: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    // --- 主题颜色定义 --- //
    val backgroundColor = MaterialTheme.colorScheme.background // 背景色
    val headerColor = MaterialTheme.colorScheme.primaryContainer // 头部背景色
    val textPrimaryColor = MaterialTheme.colorScheme.onBackground // 主要文字颜色
    val textSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant // 次要文字颜色
    val dividerColor = MaterialTheme.colorScheme.outlineVariant // 分割线颜色

    // --- 状态收集 --- //
    // 从 ViewModel 收集各种界面状态
    val uiState by viewModel.uiState.collectAsState() // 整体界面状态 (加载中, 成功, 错误等)
    val currentWeek by viewModel.currentWeek.collectAsState() // 当前显示的周数
    val weekDates by viewModel.weekDates.collectAsState() // 当前周的所有日期 (LocalDate列表)
    val displayableTimeSlots by viewModel.displayableTimeSlots.collectAsState() // 需要在课表上显示的时间槽 (课程/日程) 列表
    val defaultTableId by viewModel.defaultTableIdState.collectAsState() // 当前使用的课表 ID

    // --- 主布局 --- //
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- 顶部栏 --- //
            TopAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor),
                title = {
                    val currentDate = Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
                    val formattedDate = "${currentDate.year}/${
                        currentDate.monthNumber.toString().padStart(2, '0')
                    }/${currentDate.dayOfMonth.toString().padStart(2, '0')}"

                    Column {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Text(
                            text = "第${currentWeek}周 ${currentDate.dayOfWeek.getChineseWeekName()}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                },
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        IconButton(onClick = { navigationState.navigateToAddCourse(defaultTableId) }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "添加",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = {
                            navigationState.navigateSchoolSelectorScreen(
                                defaultTableId
                            )
                        }) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "下载",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = { /* TODO: 分享 */ }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "分享",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = { /* TODO: 更多选项 */ }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = headerColor,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )

            // --- 课表内容 --- //
            ScheduleContent(
                tableId = 0,
                weekDates = weekDates,
                timeSlots = displayableTimeSlots,
                onTimeSlotClick = { timeSlot ->
                    // 处理时间槽点击事件
                    Log.d(
                        "ScheduleScreen",
                        "Clicked on TimeSlot: Type=${timeSlot.scheduleType}, ScheduleID=${timeSlot.scheduleId}, Head='${timeSlot.head}'"
                    )
                    // 根据时间槽类型进行不同的导航
                    when (timeSlot.scheduleType) {
                        ScheduleType.COURSE -> {
                            // 如果是课程，导航到课程详情页
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
                            // 如果是普通日程，导航到日程详情页
                            Log.i(
                                "ScheduleScreen",
                                "Navigating to Ordinary Schedule Detail for scheduleId=${timeSlot.scheduleId}"
                            )
                            navigationState.navigateToOrdinaryScheduleDetail(timeSlot.scheduleId)
                        }

                        else -> {
                            // 其他类型暂未实现导航
                            Log.w(
                                "ScheduleScreen",
                                "Navigation not implemented for ScheduleType: ${timeSlot.scheduleType}"
                            )
                        }
                    }
                }
            )

            // --- 底部导航栏 --- //
            NavigationBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                val items = listOf(
                    Triple("首页", Icons.Default.Home, "home"),
                    Triple("课表", Icons.Default.DateRange, "schedule"),
                    Triple("任务", Icons.AutoMirrored.Filled.Assignment, "task"),
                    Triple("学习", Icons.Default.School, "study"),
                    Triple("我的", Icons.Default.Person, "profile")
                )

                items.forEach { (title, icon, route) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = title) },
                        label = { Text(title) },
                        selected = route == "schedule",
                        onClick = { /* TODO: 导航 */ },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                alpha = 0.6f
                            ),
                            unselectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                alpha = 0.6f
                            ),
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }

        // --- 浮动按钮 --- //
        FloatingActionButton(
            onClick = { navigationState.navigateToAddEditOrdinarySchedule(null) },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomEnd),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加普通日程"
            )
        }
    }
}

/**
 * 加载中状态显示的 Composable。
 * 居中显示一个圆形进度指示器和"加载中..."文本。
 */
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(), // 填充整个父容器
        contentAlignment = Alignment.Center // 内容居中对齐
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally // 列内元素水平居中
        ) {
            CircularProgressIndicator( // 圆形进度条
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary // 使用主题色
            )
            Spacer(modifier = Modifier.height(16.dp)) // 添加垂直间距
            Text(
                text = "加载中...",
                style = MaterialTheme.typography.bodyLarge // 使用预定义的文本样式
            )
        }
    }
}

/**
 * 课表为空状态显示的 Composable。
 * 居中显示提示信息，引导用户添加课程。
 */
@Composable
fun EmptyScheduleScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp) // 添加内边距
        ) {
            Text(
                text = "没有课程",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary // 使用主题强调色
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击右下角的 + 按钮添加课程",
                textAlign = TextAlign.Center, // 文本居中对齐
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant // 使用次要文本颜色
            )
        }
    }
}

/**
 * 加载出错状态显示的 Composable。
 * 居中显示错误提示信息。
 *
 * @param errorMessage 要显示的错误消息文本。
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
                color = MaterialTheme.colorScheme.error // 使用错误状态颜色
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage, // 显示具体的错误信息
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 未选择任何课表时显示的 Composable。
 * 提供创建新课表或导入课表的入口。
 *
 * @param navigationState 用于处理导航事件的对象。
 * @param onNavigateToImport 当点击"从教务系统导入"按钮时触发的回调。
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
        horizontalAlignment = Alignment.CenterHorizontally, // 水平居中
        verticalArrangement = Arrangement.Center // 垂直居中
    ) {
        Text("您还没有选择或创建课表", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("请先创建新课表或从教务系统导入", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNavigateToImport) { // 导入按钮
            Text("从教务系统导入课表")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navigationState.navigateToCreateEditTable() }) { // 手动创建按钮
            Text("手动创建新课表")
        }
        // TODO: 添加选择现有课表的按钮 (如果允许多课表)
    }
}

/**
 * 课表核心内容区域 Composable。
 * 负责绘制周视图的头部（星期、日期）、时间轴、背景网格以及放置所有的 TimeSlotItem。
 *
 * @param tableId 当前课表的 ID。
 * @param weekDates 当前周的日期列表 (LocalDate)。
 * @param timeSlots 需要显示的时间槽 (课程/日程) 列表。
 * @param onTimeSlotClick 时间槽被点击时的回调函数。
 */
@Composable
fun ScheduleContent(
    tableId: Int,
    weekDates: List<LocalDate>,
    timeSlots: List<TimeSlot>,
    onTimeSlotClick: (TimeSlot) -> Unit
) {
    // --- 基础布局参数 --- //
    val scrollState = rememberScrollState() // 垂直滚动状态
    // 定义课表显示的时间范围 (小时)
    val gridStartHour = 8 // 网格开始时间 (例如 8 点)
    val gridEndHour = 21 // 网格结束时间 (例如 21 点，显示到 21:00 的标签)
    val totalHours = gridEndHour - gridStartHour + 1 // 总共需要显示的小时标签数
    val hourHeight = 60.dp // 每个小时在垂直方向上占据的高度
    // 计算网格的总高度 (基于实际显示的小时数)
    val totalGridHeight = ((totalHours - 1) * hourHeight.value).dp

    // --- 动态计算参数 --- //
    // State 用于存储计算后得到的每天的宽度
    var dayWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current // 获取当前屏幕密度，用于像素和 dp 转换

    // --- 主体布局 (垂直滚动) --- //
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState) // 使整个内容区域可垂直滚动
    ) {

        // --- 周日期标题行 --- //
        // 仅在 dayWidth 计算完成后显示标题行
        if (dayWidth > 0.dp) {
            WeekHeader(weekDates = weekDates, dayWidth = dayWidth)
        }
        // --- 时间轴和课表网格容器 --- //
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalGridHeight) // 设置固定高度，由总小时数和小时高度决定
                // 在布局完成后获取实际宽度，并计算 dayWidth
                .onGloballyPositioned { coordinates ->
                    with(density) {
                        // 计算除去左侧时间轴后，可用于分配给 7 天的宽度
                        val availableWidth = coordinates.size.width.toDp() - TIME_AXIS_WIDTH
                        // 确保可用宽度大于 0 才进行计算
                        if (availableWidth > 0.dp) {
                            dayWidth = availableWidth / 7 // 平均分配给 7 天
                        }
                    }
                }
        ) {
            // --- 时间轴 (左侧) --- //
            TimeAxis(
                startHour = gridStartHour, // 网格开始小时
                endHour = gridEndHour, // 网格结束小时
                hourHeight = hourHeight // 每小时高度
            )

            // --- 背景网格线 --- //
            // 仅在 dayWidth 计算完成后绘制网格 (dayWidth > 0.dp)
            if (dayWidth > 0.dp) {
                WeekGrid(
                    startHour = gridStartHour,
                    endHour = gridEndHour,
                    hourHeight = hourHeight,
                    dayWidth = dayWidth // 传递计算好的日宽度
                )
            }

            // --- 放置时间槽项目 (课程/日程) --- //
            // 仅在 dayWidth 计算完成后放置项目
            if (dayWidth > 0.dp) {
                timeSlots.forEach { timeSlot ->
                    // TODO: 确认 TimeSlot 的 startTime 和 endTime 是否精确反映了 45 分钟的课时安排。
                    // ViewModel/数据层中从 CourseNode (startNode, step) 到 TimeSlot (startTime, endTime) 的转换
                    // 需要使用实际的学校作息时间（包括休息时间），而不仅仅是假设节点直接映射到小时。
                    // 正确的时间对于精确的视觉对齐和持续时间至关重要。

                    // 过滤掉不在当前显示时间范围 (gridStartHour 到 gridEndHour) 内的时间槽
                    val startTimeLocal = Instant.fromEpochMilliseconds(timeSlot.startTime)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                    if (startTimeLocal.hour >= gridStartHour && startTimeLocal.hour < gridEndHour) { // 注意: 结束小时不包含，因为只显示到 21:00 的标签
                        TimeSlotItem(
                            timeSlot = timeSlot, // 时间槽数据
                            hourHeight = hourHeight, // 小时高度，用于计算垂直位置和高度
                            dayWidth = dayWidth, // 日宽度，用于计算水平位置和宽度
                            gridStartHour = gridStartHour, // 网格开始小时，用于计算垂直偏移
                            onTimeSlotClick = { onTimeSlotClick(timeSlot) }, // 点击事件回调
                        )
                    }
                }
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

    // 使用 LazyRow 实现水平滚动 (虽然通常不需要，但保持结构一致性)
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp) // 设置内边距
    ) {
        // --- 左侧时间轴占位 --- //
        item {
            Box(
                modifier = Modifier
                    .width(TIME_AXIS_WIDTH) // 宽度与时间轴保持一致
                    .padding(horizontal = 2.dp)
            )
        }

        // --- 循环显示每天的日期和星期 --- //
        items(weekDates) { date ->
            val isToday = date == today // 判断是否是今天
            // 将 DayOfWeek 枚举转换为中文星期名称
            val dayOfWeekName = when (date.dayOfWeek) {
                DayOfWeek.MONDAY -> "周一"
                DayOfWeek.TUESDAY -> "周二"
                DayOfWeek.WEDNESDAY -> "周三"
                DayOfWeek.THURSDAY -> "周四"
                DayOfWeek.FRIDAY -> "周五"
                DayOfWeek.SATURDAY -> "周六"
                DayOfWeek.SUNDAY -> "周日"
            }

            // --- 单个日期的显示列 --- //
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, // 水平居中
                modifier = Modifier
                    .padding(horizontal = 2.dp) // 列之间的水平间距
                    .width(dayWidth) // 使用传入的 dayWidth 确保与网格对齐
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
 * 显示左侧时间轴的 Composable。
 * 包含从 `startHour` 到 `endHour` 的时间标签，并显示节次。
 *
 * @param startHour 时间轴开始的小时 (包含)。
 * @param endHour 时间轴结束的小时 (不包含，但会显示 startHour 到 endHour-1 的标签)。
 * @param hourHeight 每个小时标签占据的高度。
 */
@Composable
fun TimeAxis(startHour: Int, endHour: Int, hourHeight: Dp) {
    Column(
        modifier = Modifier
            .width(TIME_AXIS_WIDTH) // 使用统一定义的时间轴宽度
            .padding(end = 4.dp) // 右侧留出一点间距
            .fillMaxHeight() // 填充父容器的高度
    ) {
        // 遍历从 startHour 到 endHour-1 的所有小时
        (startHour until endHour).forEachIndexed { index, hour ->
            val nodeNumber = index + 1 // 计算对应的节次 (从 1 开始)
            val timeString = String.format("%d:00", hour) // 格式化小时字符串，例如 "8:00"

            // --- 单个时间标签容器 --- //
            Box(
                contentAlignment = Alignment.TopCenter, // 内容顶部居中对齐
                modifier = Modifier
                    .height(hourHeight) // 设置固定高度
                    .fillMaxWidth()
                    .padding(top = 2.dp) // 顶部留出一点内边距
            ) {
                // 使用 Column 垂直排列节次和时间
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // --- 显示节次 --- //
                    Text(
                        text = nodeNumber.toString(),
                        style = MaterialTheme.typography.labelMedium, // 使用中等标签样式
                        fontSize = 11.sp, // 调整字体大小
                        color = MaterialTheme.colorScheme.onSurfaceVariant // 使用次要文本颜色
                    )
                    // --- 显示时间 --- //
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall, // 使用小标签样式
                        fontSize = 9.sp, // 调整字体大小
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) // 更淡的颜色
                    )
                }
            }
        }
    }
}

/**
 * 绘制课表背景网格线的 Composable。
 *
 * @param startHour 网格开始的小时。
 * @param endHour 网格结束的小时。
 * @param hourHeight 网格中每小时的高度。
 * @param dayWidth 网格中每天的宽度。
 */
@Composable
fun WeekGrid(startHour: Int, endHour: Int, hourHeight: Dp, dayWidth: Dp) {
    val totalHourSlots = endHour - startHour // 计算总共有多少个小时间隔
    Box(modifier = Modifier.fillMaxSize()) {
        // --- 绘制水平线 --- //
        // 绘制从顶部开始的 totalHourSlots + 1 条线 (包含底部边界线)
        (0..totalHourSlots).forEach { hourIndex ->
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart) // 对齐到左上角
                    .offset( // 计算偏移量
                        x = TIME_AXIS_WIDTH, // 水平方向从时间轴右侧开始
                        y = (hourIndex * hourHeight.value).dp // 垂直方向根据小时索引计算
                    )
                    .width(7 * dayWidth), // 线条宽度覆盖 7 天
                thickness = 0.6.dp, // 设置线条粗细
                color = Color.LightGray.copy(alpha = 0.4f) // 设置颜色和透明度
            )
        }
        // --- 绘制垂直线 --- //
        // 绘制 8 条线来分割 7 天的列
        (0..7).forEach { dayIndex ->
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxHeight() // 线条高度填充整个网格
                    .width(0.6.dp) // 设置线条宽度
                    .align(Alignment.TopStart)
                    // 计算水平偏移量
                    .offset(x = TIME_AXIS_WIDTH + (dayIndex * dayWidth.value).dp),
                color = Color.LightGray.copy(alpha = 0.4f)
            )
        }
    }
}

// 用于在 TimeSlotItem 中格式化时间的 Formatter
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * 显示单个时间槽 (课程或日程) 的 Composable。
 * 它是一个 Card，根据时间槽的开始/结束时间、星期几来计算其在网格中的位置和大小。
 *
 * @param timeSlot 要显示的时间槽数据对象。
 * @param hourHeight 网格中每小时的高度，用于计算垂直位置和大小。
 * @param dayWidth 网格中每天的宽度，用于计算水平位置和大小。
 * @param gridStartHour 网格开始的小时，用于计算垂直偏移。
 * @param onTimeSlotClick 时间槽被点击时的回调。
 * @param modifier Modifier，允许外部自定义。
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
    val density = LocalDensity.current // 获取屏幕密度

    // --- 1. 计算 TimeSlot 的位置和大小 --- //
    // 将毫秒时间戳转换为 LocalDateTime 对象
    val startInstant = Instant.fromEpochMilliseconds(timeSlot.startTime)
    val endInstant = Instant.fromEpochMilliseconds(timeSlot.endTime)
    val startTimeLocal = startInstant.toLocalDateTime(TimeZone.currentSystemDefault())
    val endTimeLocal = endInstant.toLocalDateTime(TimeZone.currentSystemDefault())
    val dayOfWeek = startTimeLocal.dayOfWeek.isoDayNumber // 获取星期几 (1=周一, 7=周日)

    // 计算垂直偏移 (Y 轴位置)
    // 计算开始时间相对于网格开始小时的总分钟数
    val startMinutesPastGridStart =
        (startTimeLocal.hour - gridStartHour) * 60 + startTimeLocal.minute
    // 将分钟数转换为相对于网格顶部的小时数比例，再乘以每小时高度
    val topOffset = (startMinutesPastGridStart / 60.0 * hourHeight.value).dp

    // 计算高度
    val durationMillis = max(1, timeSlot.endTime - timeSlot.startTime) // 确保时长至少为 1ms，避免除零
    val durationMinutes = durationMillis / (1000.0 * 60) // 计算总分钟时长
    // 将分钟时长转换为小时数比例，再乘以每小时高度
    val itemHeight = (durationMinutes / 60.0 * hourHeight.value).dp

    // 计算水平偏移 (X 轴位置) 和宽度
    // 确保 dayWidth 是正数
    val safeDayWidth = max(0.dp.value, dayWidth.value).dp
    // X 偏移 = 时间轴宽度 + (星期几 - 1) * 每天宽度
    val leftOffset = TIME_AXIS_WIDTH + ((dayOfWeek - 1) * safeDayWidth.value).dp
    // 宽度 = 每天宽度 - 左右两边的固定内边距 (例如 4dp)
    val itemWidth = safeDayWidth - 4.dp

    // 调整最终尺寸并应用内边距
    val verticalPadding = 2.dp // 在时间槽上下边缘留出的内边距
    // 最终高度 = 计算高度 - 上下内边距，同时确保最小高度 (例如小时高度的 10%)
    val finalHeight = maxOf(hourHeight * 0.1f, itemHeight - verticalPadding * 2)
    // 最终宽度 = 计算宽度，同时确保最小宽度 (例如 1dp)
    val finalWidth = maxOf(1.dp, itemWidth)

    // --- 2. 获取显示信息和颜色 --- //
    // 获取标题，如果为空则显示默认值
    val title = timeSlot.displayTitle ?: "无标题"
    // 格式化时间范围字符串
    val timeString =
        "${startTimeLocal.format(timeFormatter)} - ${endTimeLocal.format(timeFormatter)}"
    // 决定详情文本：优先显示副标题 (地点)，否则显示时间范围
    val details = if (!timeSlot.displaySubtitle.isNullOrBlank()) {
        "@${timeSlot.displaySubtitle}" // 时间信息已由位置体现
    } else {
        timeString
    }

    // 生成柔和的背景色 (基于标题或 ID，确保颜色稳定)
    val backgroundColor = remember(timeSlot.id, title) {
        generatePastelColor(title.ifBlank { timeSlot.id.toString() }) // 使用标题，若为空则用 ID 作种子
    }
    // 根据背景色亮度决定内容 (文字) 颜色，以保证可读性
    val contentColor = remember(backgroundColor) {
        if (backgroundColor.isDark()) Color.White.copy(alpha = 0.95f) else Color.Black.copy(alpha = 0.87f)
    }

    // --- 安全检查 (可选但推荐) --- //
    // 防止计算出的位置无效导致绘制到网格外部
    if (topOffset < 0.dp || leftOffset < TIME_AXIS_WIDTH || safeDayWidth <= 0.dp) {
        // 记录错误日志或跳过渲染
        Log.e(
            "TimeSlotItem",
            "Invalid position calculated for TimeSlot ${timeSlot.id}, skipping render."
        )
        return // 如果位置无效，则不绘制此 TimeSlotItem
    }

    // --- 绘制 Card 作为时间槽 --- //
    Card(
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = verticalPadding) // 应用外部和垂直内边距
            .offset(x = leftOffset, y = topOffset) // 应用计算好的位置偏移
            .width(finalWidth) // 设置计算好的宽度
            .height(finalHeight) // 设置计算好的高度
            .clickable(onClick = onTimeSlotClick), // 使 Card 可点击
        shape = RoundedCornerShape(6.dp), // 设置圆角
        colors = CardDefaults.cardColors( // 设置 Card 颜色
            containerColor = backgroundColor, // 背景色
            contentColor = contentColor // 内容 (文字) 颜色
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) // 设置阴影
    ) {
        // --- Card 内部内容布局 --- //
        Column(
            modifier = Modifier
                .fillMaxSize() // 填充整个 Card
                .padding(horizontal = 4.dp, vertical = 3.dp), // Card 内部的内边距
            verticalArrangement = Arrangement.SpaceBetween // 垂直方向上将标题和详情分开，尽可能填充空间
        ) {
            // --- 显示标题 --- //
            Text(
                text = title,
                fontWeight = FontWeight.Bold, // 加粗
                fontSize = 11.sp, // 字体大小
                maxLines = 3, // 最多显示 3 行
                overflow = TextOverflow.Ellipsis, // 超出部分显示省略号
                lineHeight = 13.sp // 调整行高，使文本更紧凑
            )
            // Spacer(modifier = Modifier.height(2.dp)) // 使用 SpaceBetween 后不再需要固定间距
            // --- 显示详情 (地点或时间) --- //
            Text(
                text = details,
                fontSize = 9.sp,
                maxLines = 2, // 最多显示 2 行
                overflow = TextOverflow.Ellipsis,
                color = contentColor.copy(alpha = 0.8f), // 使用稍透明的颜色
                lineHeight = 11.sp // 调整行高
            )
        }
    }
}

/**
 * LocalDateTime 的扩展函数，用于方便地格式化时间。
 *
 * @param formatter DateTimeFormatter 实例。
 * @return 格式化后的时间字符串。
 */
private fun LocalDateTime.format(formatter: DateTimeFormatter): String {
    // 将 kotlinx.datetime.LocalDateTime 转换为 java.time.LocalDateTime 再格式化
    return this.toJavaLocalDateTime().format(formatter)
}

/**
 * 辅助函数：根据时间槽类型提供备用的回退颜色。
 * (当前主要由 generatePastelColor 生成颜色，此函数可能较少用到)
 *
 * @param type ScheduleType 枚举值。
 * @return 对应的 Color 对象。
 */
private fun fallbackColor(type: ScheduleType): Color {
    return when (type) {
        ScheduleType.COURSE -> Color(0xFF03A9F4) // 课程: 蓝色
        ScheduleType.ORDINARY -> Color(0xFF4CAF50) // 普通日程: 绿色
        ScheduleType.HOMEWORK -> Color(0xFFFF9800) // 作业: 橙色
        ScheduleType.EXAM -> Color(0xFFF44336) // 考试: 红色
        else -> Color.LightGray // 其他: 浅灰色
    }
}

/**
 * Color 的扩展函数：判断颜色是否偏深色。
 * 用于决定内容 (文字) 应该使用浅色还是深色以保证对比度。
 *
 * @return 如果颜色偏深，返回 true，否则返回 false。
 */
fun Color.isDark(): Boolean {
    // 使用 YIQ 公式近似计算亮度
    // val darkness = 1 - (0.299 * red + 0.587 * green + 0.114 * blue)
    // 使用 Compose 内置的 luminance() 可能更准确 (如果需要)
    // val luminance = ColorUtils.calculateLuminance(this.toArgb())
    // 简单的亮度计算 (基于 RGB 平均值，可能不太准确，但简单)
    val darkness = 1 - (red + green + blue) / 3 * alpha // 考虑透明度
    return darkness >= 0.55 // 亮度阈值，可以根据需要调整
}

/**
 * 辅助函数：根据输入字符串 (seed) 生成一个柔和的、低饱和度的颜色 (Pastel Color)。
 * 使用输入的 hashcode 作为随机数种子，确保相同输入的颜色是固定的。
 *
 * @param seed 用于生成颜色的字符串种子 (例如课程标题或 ID)。
 * @return 生成的 Color 对象。
 */
fun generatePastelColor(seed: String): Color {
    // 使用 seed 的 hashcode 初始化伪随机数生成器，确保结果可预测
    val random = Random(seed.hashCode())
    // HSL 颜色模型: Hue (色相), Saturation (饱和度), Lightness (亮度)
    val hue = random.nextFloat() * 360f // 色相: 0-360 度，随机选择
    val saturation = random.nextFloat() * 0.2f + 0.5f // 饱和度: 限制在 0.5 - 0.7 之间，营造柔和感
    val lightness = random.nextFloat() * 0.15f + 0.8f // 亮度: 限制在 0.8 - 0.95 之间，确保颜色较浅

    // --- HSL 转换为 RGB --- //
    // 注意: Android 的 Color 类不直接支持 HSL，这里使用简化的转换逻辑。
    // 更精确的转换可以使用 android.graphics.Color.HSVToColor 或相关库。
    val c = (1f - kotlin.math.abs(2 * lightness - 1)) * saturation
    val x = c * (1f - kotlin.math.abs((hue / 60f) % 2 - 1))
    val m = lightness - c / 2f

    val (r, g, b) = when {
        hue < 60 -> Triple(c, x, 0f)
        hue < 120 -> Triple(x, c, 0f)
        hue < 180 -> Triple(0f, c, x)
        hue < 240 -> Triple(0f, x, c)
        hue < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    // 创建 Compose Color 对象，确保 RGB 值在 0-1 范围内
    return Color(
        red = (r + m).coerceIn(0f, 1f),
        green = (g + m).coerceIn(0f, 1f),
        blue = (b + m).coerceIn(0f, 1f)
    )
}

// 定义左侧时间轴的宽度常量
private val TIME_AXIS_WIDTH = 48.dp // 稍微增加宽度以容纳节次和时间

/**
 * 用于在 Android Studio 预览中显示 ScheduleScreen 的 Composable。
 */
@Preview(showBackground = true)
@Composable
fun PreviewScheduleScreen() {
    // 获取 Hilt 注入的 ViewModel (仅限预览)
    val viewModel = hiltViewModel<ScheduleViewModel>()
    // 创建虚拟的 NavController 和 NavigationState
    val navController = rememberNavController()
    val navigationState = remember(navController) {
        NavigationState(navController)
    }
    // 调用 ScheduleScreen 进行预览
    ScheduleScreen(navigationState, {}, viewModel)
}

/**
 * 用于在 Android Studio 预览中显示 ScheduleContent 的 Composable。
 * 使用固定的示例数据进行预览。
 */
@Preview(showBackground = true)
@Composable
fun PreviewScheduleContent() {
    // --- 创建示例数据 --- //
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val weekDates = List(7) { index -> today.plus(DatePeriod(days = index)) } // 生成本周日期

    // 定义示例课程列表
    val weekCourses = listOf<Course>(
        // ... (保留部分示例课程数据)
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
                // --- 创建 TimeSlot 对象 --- //
                TimeSlot(
                    id = course.id, // 使用 Course ID 作为基础 ID (可能需要更唯一的 ID)
                    scheduleType = ScheduleType.COURSE, // 类型为课程
                    scheduleId = course.id, // 关联的课程 ID
                    // 使用 ViewModel 中定义的字段
                    displayTitle = course.courseName, // 主标题：课程名
                    displaySubtitle = node.room, // 副标题：教室
                    displayColor = null, // 颜色由 generatePastelColor 根据标题生成
                    // TODO: 这里的 startTime 和 endTime 计算逻辑是【占位符】，【极其不准确】。
                    // 它错误地假设 startNode 直接映射到小时。
                    // 【必须】替换为根据学校实际的 45 分钟课时表（例如，第 1 节=8:00，第 3 节=9:50 等）
                    // 和 node.step（持续节数）来计算【精确】的开始和结束毫秒时间戳的逻辑。
                    startTime = System.currentTimeMillis() + node.startNode * 3600000L, // 错误的占位符
                    endTime = System.currentTimeMillis() + (node.startNode + node.step) * 3600000L, // 错误的占位符
                    userId = 1 // 示例用户 ID
                )
            }
        },
        onTimeSlotClick = { timeSlot -> // 预览中的点击事件处理 (仅打印日志)
            Log.d(
                "PreviewScheduleContent",
                "Clicked on TimeSlot: ${timeSlot.id}, Type: ${timeSlot.scheduleType}, ScheduleID: ${timeSlot.scheduleId}"
            )
        }
    )
}

/**
 * DayOfWeek 的扩展函数，将星期转换为中文表示
 */
private fun DayOfWeek.getChineseWeekName(): String {
    return when (this) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }
}

