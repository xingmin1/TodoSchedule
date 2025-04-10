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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.domain.model.TimeSlot
import com.example.todoschedule.domain.utils.CalendarUtils
import com.example.todoschedule.ui.navigation.NavigationState
import com.example.todoschedule.ui.schedule.model.ScheduleUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

// 用于格式化时间的 Formatter (移到顶层)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

// 定义左侧时间轴的宽度常量
private val TIME_AXIS_WIDTH = 48.dp // 稍微增加宽度以容纳节次和时间

// 定义课表的小时高度
private val HOUR_HEIGHT = 60.dp

// 定义网格显示的小时范围
private const val GRID_START_HOUR = 0 // 从 0 点开始
private const val GRID_END_HOUR = 24 // 到 24 点结束 (显示 0:00 到 23:00 的标签)

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
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
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

    // --- 使用 Scaffold --- //
    Scaffold(
        topBar = {
            // --- 顶部栏 --- //
            TopAppBar(
                modifier = Modifier.fillMaxWidth(),
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
                        IconButton(onClick = {
                            navigationState.navigateToAddCourse(
                                defaultTableId ?: AppConstants.Ids.INVALID_TABLE_ID
                            )
                        }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "添加",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = {
                            navigationState.navigateSchoolSelectorScreen(
                                defaultTableId ?: AppConstants.Ids.INVALID_TABLE_ID
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
        },
        bottomBar = {
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
        },
        floatingActionButton = {
            // --- 浮动按钮 --- //
            FloatingActionButton(
                onClick = { navigationState.navigateToAddEditOrdinarySchedule(null) },
                // Modifier.align is not needed here as Scaffold handles placement
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加普通日程"
                )
            }
        },
        containerColor = backgroundColor // Set background color for the Scaffold itself
    ) { innerPadding -> // Content lambda provides padding
        // --- 内容区域 --- //
        Box(
            modifier = Modifier
                .padding(innerPadding) // Apply padding from Scaffold
                .fillMaxSize() // Content should fill the space provided by Scaffold
        ) {
            when (uiState) {
                is ScheduleUiState.Loading -> LoadingScreen()
                is ScheduleUiState.Success -> {
                    // 调用新的 ScheduleContent
                    ScheduleContent(
                        currentWeek = currentWeek,
                        timeSlotsForCurrentWeek = displayableTimeSlots, // 来自 ViewModel
                        onWeekChange = viewModel::updateCurrentWeek,
                        onTimeSlotClick = {
                            // 复用之前的点击逻辑
                            Log.d(
                                "ScheduleScreen",
                                "Clicked on TimeSlot: Type=${it.scheduleType}, ScheduleID=${it.scheduleId}, Head='${it.head}'"
                            )
                            when (it.scheduleType) {
                                ScheduleType.COURSE -> {
                                    val currentTableId = defaultTableId
                                    if (currentTableId != null && currentTableId != AppConstants.Ids.INVALID_TABLE_ID) {
                                        Log.i(
                                            "ScheduleScreen",
                                            "Navigating to Course Detail: tableId=$currentTableId, courseId=${it.scheduleId}"
                                        )
                                        navigationState.navigateToCourseDetail(
                                            tableId = currentTableId,
                                            courseId = it.scheduleId
                                        )
                                    } else {
                                        Log.w(
                                            "ScheduleScreen",
                                            "Cannot navigate to course detail, defaultTableId is invalid or null."
                                        )
                                    }
                                }

                                ScheduleType.ORDINARY -> {
                                    Log.i(
                                        "ScheduleScreen",
                                        "Navigating to Ordinary Schedule Detail for scheduleId=${it.scheduleId}"
                                    )
                                    navigationState.navigateToOrdinaryScheduleDetail(it.scheduleId)
                                }

                                else -> {
                                    Log.w(
                                        "ScheduleScreen",
                                        "Navigation not implemented for ScheduleType: ${it.scheduleType}"
                                    )
                                }
                            }
                        }
                    )
                }

                is ScheduleUiState.Error -> ErrorScreen((uiState as ScheduleUiState.Error).message)
                is ScheduleUiState.Empty -> EmptyScheduleScreen() // 假设有这个状态
                is ScheduleUiState.NoTableSelected -> NoTableSelectedScreen(
                    navigationState = navigationState,
                    onNavigateToImport = {
                        navigationState.navigateSchoolSelectorScreen(
                            // 传递无效ID而不是null
                            AppConstants.Ids.INVALID_TABLE_ID
                        )
                    }
                )
            }
        } // End of Content Box
    } // End of Scaffold
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
 * 新的课表内容区域，使用 HorizontalPager 实现周切换。
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScheduleContent(
    currentWeek: Int,
    timeSlotsForCurrentWeek: List<TimeSlot>,
    onWeekChange: (Int) -> Unit,
    onTimeSlotClick: (TimeSlot) -> Unit
) {
    // --- Pager State --- //
    val pagerState = rememberPagerState(initialPage = currentWeek - 1) {
        CalendarUtils.MAX_WEEKS // 总页数，对应最大周数
    }

    // --- Side Effects for Synchronization --- //
    // 1. Pager 滑动 -> 通知 ViewModel 更新 currentWeek
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                Log.d(
                    "ScheduleContent",
                    "Pager changed to page $page, notifying ViewModel (filter removed)"
                )
                onWeekChange(page + 1) // 页码从0开始，周数从1开始
            }
    }

    // 2. ViewModel 更新 currentWeek -> Pager 滚动到对应页面
    LaunchedEffect(currentWeek) {
        if (currentWeek - 1 != pagerState.currentPage) {
            Log.d("ScheduleContent", "currentWeek changed to $currentWeek, scrolling Pager")
            try {
                pagerState.animateScrollToPage(currentWeek - 1)
            } catch (e: Exception) {
                Log.e("ScheduleContent", "Error scrolling Pager", e)
                // 如果动画滚动失败，尝试立即跳转
                try {
                    pagerState.scrollToPage(currentWeek - 1)
                } catch (scrollError: Exception) {
                    Log.e("ScheduleContent", "Error jumping Pager", scrollError)
                }
            }
        }
    }

    // --- Layout Calculation --- //
    var dayWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 在 BoxWithConstraints 中获取可用宽度并计算 dayWidth
        val availableWidth = constraints.maxWidth
        val calculatedDayWidth = with(density) {
            (availableWidth.toDp() - TIME_AXIS_WIDTH) / 7
        }
        // 确保 dayWidth 只在计算有效时更新
        if (calculatedDayWidth > 0.dp) {
            dayWidth = calculatedDayWidth
        }

        // --- Horizontal Pager --- //
        if (dayWidth > 0.dp) { // 仅在 dayWidth 有效时渲染 Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
                // beyondBoundsPageCount = 1 // 预加载相邻页面
            ) { page ->
                val pageWeek = page + 1
                // 获取该页对应的周日期 (临时方案)
                // TODO: 理想情况下应从 ViewModel 获取或传入
                val pageWeekDates = remember(pageWeek) { // 根据 pageWeek 缓存计算结果
                    // 注意: 这里需要知道课表起始日期，暂时无法从 ViewModel 直接获取
                    // 临时使用 null，CalendarUtils 会用默认值
                    CalendarUtils.getWeekDates(pageWeek, null)
                }
                Log.d("ScheduleContent", "Pager rendering page $page (week $pageWeek)")

                // 获取 ViewModel 当前认为的周数 (传递给 ScheduleContent 的状态)
                val currentViewModelWeek = currentWeek
                // 获取 ViewModel 当前周对应的数据 (传递给 ScheduleContent 的状态)
                val slotsFromViewModel = timeSlotsForCurrentWeek

                // 只有当 Pager 的页面周数 与 ViewModel 的当前周数 一致时，才传递数据
                // 否则传递空列表，避免显示陈旧数据
                val slotsForThisPage = if (pageWeek == currentViewModelWeek) {
                    slotsFromViewModel
                } else {
                    emptyList()
                }

                // 调用单周视图
                WeekSchedulePage(
                    weekDates = pageWeekDates,
                    timeSlots = slotsForThisPage, // 使用条件判断后的数据
                    dayWidth = dayWidth,
                    onTimeSlotClick = onTimeSlotClick
                )
            }
        } else {
            // 可选：在 dayWidth 计算完成前显示加载指示器或占位符
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Calculating layout...")
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
    modifier: Modifier = Modifier // Implicitly contains size from Layout
) {
    // --- Get display info ---
    val startInstant = Instant.fromEpochMilliseconds(timeSlot.startTime)
    val endInstant = Instant.fromEpochMilliseconds(timeSlot.endTime)
    val startTimeLocal = startInstant.toLocalDateTime(TimeZone.currentSystemDefault())
    val endTimeLocal = endInstant.toLocalDateTime(TimeZone.currentSystemDefault())

    val title = timeSlot.displayTitle ?: "无标题"
    val timeString =
        "${startTimeLocal.format(timeFormatter)} - ${endTimeLocal.format(timeFormatter)}"
    val details = if (!timeSlot.displaySubtitle.isNullOrBlank()) {
        "@${timeSlot.displaySubtitle}"
    } else {
        timeString
    }

    // --- Colors (Adaptive & Low Saturation/Brightness) ---
    val isDarkTheme = isSystemInDarkTheme()
    // 使用新的自适应颜色生成逻辑，忽略 timeSlot.displayColor
    val backgroundColor = remember(timeSlot.scheduleId, isDarkTheme) {
        // 使用 scheduleId 作为种子，确保同一课程颜色稳定
        generateAdaptiveCourseColor(timeSlot.scheduleId, isDarkTheme)
    }
    val contentColor = remember(backgroundColor) {
        // 基于背景亮度决定内容颜色，保证对比度
        if (backgroundColor.calculateLuminance() < 0.5f) Color.White.copy(alpha = 0.95f) // Dark background -> Light text
        else Color.Black.copy(alpha = 0.87f) // Light background -> Dark text
    }

    // --- Render Card ---
    Card(
        // Apply modifier passed by Layout (contains size)
        modifier = modifier
            .fillMaxSize() // Fill the size given by Layout
            .padding(horizontal = 2.dp, vertical = 1.dp) // Internal padding
            .clickable(onClick = onTimeSlotClick),
        shape = RoundedCornerShape(4.dp), // Slightly smaller radius? Adjust as needed
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        // Column for content, padding adjusted for the Card's internal padding
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 2.dp), // Padding for text inside the card
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 13.sp
            )
            Text(
                text = details,
                fontSize = 9.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = contentColor.copy(alpha = 0.8f),
                lineHeight = 11.sp
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
 * 辅助函数：根据输入种子 (seed) 和主题模式生成一个低饱和度、低亮度的自适应颜色。
 * 使用输入的 hashcode 作为随机数种子，确保相同输入的颜色是固定的。
 *
 * @param seed 用于生成颜色的种子 (例如课程 ID)。
 * @param isDarkTheme 当前是否为暗色主题。
 * @return 生成的 Compose Color 对象。
 */
fun generateAdaptiveCourseColor(seed: Any, isDarkTheme: Boolean): Color {
    val random = Random(seed.hashCode())
    val hue = random.nextFloat() * 360f // 色相: 0-360 度，随机选择

    // 调整饱和度和亮度以获得柔和且适应主题的颜色
    val saturation = random.nextFloat() * 0.2f + 0.3f // 饱和度: 限制在 0.3 - 0.5 之间，更低饱和度

    val lightness = if (isDarkTheme) {
        // 暗色主题：亮度稍低，但不要太暗难以区分
        random.nextFloat() * 0.2f + 0.4f // 亮度: 限制在 0.4 - 0.6
    } else {
        // 亮色主题：亮度稍高，但仍保持柔和感
        random.nextFloat() * 0.2f + 0.65f // 亮度: 限制在 0.65 - 0.85
    }

    // --- HSL 转换为 RGB (与 generatePastelColor 类似) ---
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

    return Color(
        red = (r + m).coerceIn(0f, 1f),
        green = (g + m).coerceIn(0f, 1f),
        blue = (b + m).coerceIn(0f, 1f)
    )
}

/**
 * Color 的扩展函数：计算颜色的相对亮度 (Luminance)。
 * 返回值在 0 (纯黑) 到 1 (纯白) 之间。
 * 基于 W3C 的 WCAG (Web Content Accessibility Guidelines) 定义。
 *
 * @return 颜色的相对亮度。
 */
fun Color.calculateLuminance(): Float {
    // sRGB color space component values adjusted for gamma correction
    fun adjust(component: Float): Float {
        return if (component <= 0.03928f) {
            component / 12.92f
        } else {
            ((component + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
        }
    }

    val r = adjust(red)
    val g = adjust(green)
    val b = adjust(blue)

    // Calculate luminance using the standard formula
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
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
            timeSlots.forEach { timeSlot ->
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
        require(timeSlotMeasurables.size == timeSlots.size) { "Number of TimeSlotItems does not match number of timeSlots" }

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


        timeSlotMeasurables.forEachIndexed { index, measurable ->
            val timeSlot = timeSlots[index]

            // --- Calculate Position and Size in Pixels --- 
            val startInstant = Instant.fromEpochMilliseconds(timeSlot.startTime)
            val endInstant = Instant.fromEpochMilliseconds(timeSlot.endTime)
            val startTimeLocal = startInstant.toLocalDateTime(TimeZone.currentSystemDefault())
            val endTimeLocal = endInstant.toLocalDateTime(TimeZone.currentSystemDefault())
            val dayOfWeek = startTimeLocal.dayOfWeek.isoDayNumber // 1=Mon, 7=Sun

            // Y Offset (Top edge of the slot)
            val startMinutesPastGridStart =
                (startTimeLocal.hour - gridStartHour) * 60 + startTimeLocal.minute
            val yOffsetPx = (startMinutesPastGridStart / 60.0 * hourHeightPx).toFloat()

            // Height (Calculate full slot height first)
            val durationMillis = max(1, timeSlot.endTime - timeSlot.startTime)
            val durationMinutes = durationMillis / (1000.0 * 60.0)
            val fullSlotHeightPx = (durationMinutes / 60.0 * hourHeightPx).toFloat()
            // Actual measurable height = full height - internal padding
            val itemHeightPx = (fullSlotHeightPx - itemVerticalPaddingPx).coerceAtLeast(1f)

            // X Offset (Left edge of the slot)
            val xOffsetPx = ((dayOfWeek - 1) * dayWidthPx).toFloat()

            // Width (Calculate full slot width first)
            val fullSlotWidthPx = dayWidthPx
            // Actual measurable width = full width - internal padding
            val itemWidthPx = (fullSlotWidthPx - itemHorizontalPaddingPx).coerceAtLeast(1f)

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

