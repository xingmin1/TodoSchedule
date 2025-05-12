package com.example.todoschedule.ui.schedule

import android.annotation.SuppressLint
import android.util.Log
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.unit.times
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.domain.model.TimeSlot
import com.example.todoschedule.domain.utils.CalendarUtils
import com.example.todoschedule.ui.navigation.NavigationState
import com.example.todoschedule.ui.schedule.model.ScheduleUiState
import com.example.todoschedule.ui.theme.ColorSchemeEnum
import com.example.todoschedule.ui.utils.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random
import androidx.compose.ui.platform.LocalConfiguration
import java.util.*

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
 * 月历显示模式
 */
enum class MonthCalendarMode { FULL, MEDIUM, MINI }

/**
 * 课程表屏幕 Composable 函数。
 *
 * 负责展示周视图的课程表界面，包括顶部应用栏（周数切换、设置入口）、
 * 课程/日程网格、时间轴以及添加/导入按钮。
 * 它根据 ViewModel 提供的状态 (uiState) 显示不同的界面内容（加载中、成功、空、错误、未选择课表）。
 *
 * @param navigationState 用于处理导航事件的对象。
 * @param onNavigateToSettings 当点击设置图标时触发的回调。
 * @param paddingValues 用于处理 Scaffold 的 padding 参数。
 * @param viewModel ScheduleViewModel 的实例，用于获取界面状态和数据。
 */
@OptIn(ExperimentalMaterial3Api::class) // Opt-in for experimental ModalBottomSheetState
@Composable
fun ScheduleScreen(
    navigationState: NavigationState,
    onNavigateToSettings: () -> Unit,
    paddingValues: PaddingValues,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    // --- 主题颜色定义 --- //
    val backgroundColor = MaterialTheme.colorScheme.background // 背景色
    val headerColor = MaterialTheme.colorScheme.primaryContainer // 头部背景色
    val textPrimaryColor = MaterialTheme.colorScheme.onBackground // 主要文字颜色
    val textSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant // 次要文字颜色
    val dividerColor = MaterialTheme.colorScheme.outlineVariant // 分割线颜色

    // --- 状态收集 --- //
    val uiState by viewModel.uiState.collectAsState() // 整体界面状态 (加载中, 成功, 错误等)
    val currentWeek by viewModel.currentWeek.collectAsState() // 当前显示的周数
    val weekDates by viewModel.weekDates.collectAsState() // 当前周的所有日期 (LocalDate列表)
    val displayableTimeSlots by viewModel.displayableTimeSlots.collectAsState() // 需要在课表上显示的时间槽 (课程/日程) 列表
    val defaultTableId by viewModel.defaultTableIdState.collectAsState() // 当前使用的课表 ID
    val viewMode by viewModel.viewMode.collectAsState() // 监听视图模式
    val currentDayDate by viewModel.currentDayDate.collectAsState() // 日视图当前日期

    // --- 新增: 月视图当前年月状态 ---
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }

    // --- 新增: BottomSheet 状态 ---
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    // --- 新增: Dialog 状态 ---
    var showDialog by remember { mutableStateOf(false) }

    // --- 新增: 更多菜单状态 ---
    var showMoreMenu by remember { mutableStateOf(false) }

    // --- Root layout: Box to allow FAB placement over content ---
    // 将Scaffold包裹原来的内容，并在外部添加ModalBottomSheet
    Scaffold(
        // TopAppBar 和 SnackbarHost 保持不变
        topBar = {
            // --- TopAppBar Replacement (Similar to HomeScreen) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding() // 添加状态栏 padding
                    // .background(headerColor) // Apply background if desired
                    .padding(horizontal = 16.dp, vertical = 8.dp), // Adjust padding
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title part (Date and Week info)
                val currentDate = Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                // --- 优化：根据视图模式动态显示日期 ---
                val formattedDate = when (viewMode) {
                    ScheduleViewMode.MONTH -> "${currentYearMonth.year}/${
                        currentYearMonth.monthValue.toString().padStart(2, '0')
                    }"

                    ScheduleViewMode.DAY -> {
                        currentDayDate?.let {
                            "${it.year}/${
                                it.monthNumber.toString().padStart(2, '0')
                            }/${it.dayOfMonth.toString().padStart(2, '0')}"
                        } ?: "${currentDate.year}/${
                            currentDate.monthNumber.toString().padStart(2, '0')
                        }/${currentDate.dayOfMonth.toString().padStart(2, '0')}"
                    }

                    else -> "${currentDate.year}/${
                        currentDate.monthNumber.toString().padStart(2, '0')
                    }/${currentDate.dayOfMonth.toString().padStart(2, '0')}"
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.titleLarge // Adjust style as needed
                        // color = MaterialTheme.colorScheme.onPrimaryContainer // Adjust color if needed
                    )
                    if (viewMode != ScheduleViewMode.MONTH) {
                    Text(
                            text = when (viewMode) {
                                ScheduleViewMode.DAY -> {
                                    currentDayDate?.let {
                                        "${it.dayOfWeek.getChineseWeekName()}"
                                    } ?: "${currentDate.dayOfWeek.getChineseWeekName()}"
                                }

                                else -> "第${currentWeek}周 ${currentDate.dayOfWeek.getChineseWeekName()}"
                            },
                        style = MaterialTheme.typography.bodyMedium // Adjust style as needed
                        // color = MaterialTheme.colorScheme.onPrimaryContainer // Adjust color if needed
                    )
                    }
                }

                // Actions part
                Row(
                    horizontalArrangement = Arrangement
                        .Absolute.Right,
                    verticalAlignment = Alignment.CenterVertically,
                    // modifier = Modifier.padding(end = 16.dp) // Padding handled by outer Row
                ) {
                    // 提前获取coroutineScope，而不是在onClick中调用rememberCoroutineScope
                    val coroutineScope = rememberCoroutineScope()

                    // 回到今天按钮
                    IconButton(
                        onClick = {
                            when (viewMode) {
                                ScheduleViewMode.WEEK -> {
                                    Log.d("ScheduleScreen", "回到今天按钮点击 - 周视图模式")
                                    val startDate = viewModel.currentTableState.value?.startDate
                                    if (startDate != null) {
                                        val today = Clock.System.now()
                                            .toLocalDateTime(TimeZone.currentSystemDefault())
                                            .date
                                        val currentWeekNumber =
                                            CalendarUtils.getCurrentWeek(startDate)
                                        Log.d(
                                            "ScheduleScreen",
                                            "计算得到当前周次: $currentWeekNumber, 课表起始日期: $startDate"
                                        )

                                        // 直接调用ViewModel的方法，让它处理状态更新和滚动
                                        viewModel.goToCurrentWeek()
                                        Log.d("ScheduleScreen", "成功调用goToCurrentWeek()方法")
                                    } else {
                                        Log.w(
                                            "ScheduleScreen",
                                            "课表起始日期为空，使用默认goToCurrentWeek()方法"
                                        )
                                        viewModel.goToCurrentWeek()
                                    }
                                }

                                ScheduleViewMode.MONTH -> {
                                    val today = Clock.System.now()
                                        .toLocalDateTime(TimeZone.currentSystemDefault())
                                        .date
                                    currentYearMonth = YearMonth.of(today.year, today.monthNumber)
                                }

                                ScheduleViewMode.DAY -> {
                                    // 在日视图显示今天日期
                                    val today = Clock.System.now()
                                        .toLocalDateTime(TimeZone.currentSystemDefault())
                                        .date
                                    // 更新ViewModel状态
                                    viewModel.updateCurrentDayDate(today)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "回到今天",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = {
                        navigationState.navigateToAddCourse(
                            defaultTableId ?: AppConstants.Ids.INVALID_TABLE_ID
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加",
                            // tint = MaterialTheme.colorScheme.onPrimaryContainer // Adjust tint
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
                            // tint = MaterialTheme.colorScheme.onPrimaryContainer // Adjust tint
                        )
                    }
                    IconButton(onClick = { /* TODO: 分享 */ }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "分享",
                            // tint = MaterialTheme.colorScheme.onPrimaryContainer // Adjust tint
                        )
                    }
                    // --- 新增: 更多菜单 ---
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多",
                            )
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("周视图") },
                                onClick = {
                                    viewModel.setViewMode(ScheduleViewMode.WEEK)
                                    showMoreMenu = false
                                },
                                enabled = viewMode != ScheduleViewMode.WEEK
                            )
                            DropdownMenuItem(
                                text = { Text("月视图") },
                                onClick = {
                                    viewModel.setViewMode(ScheduleViewMode.MONTH)
                                    showMoreMenu = false
                                },
                                enabled = viewMode != ScheduleViewMode.MONTH
                            )
                            DropdownMenuItem(
                                text = { Text("日视图") },
                                onClick = {
                                    viewModel.setViewMode(ScheduleViewMode.DAY)
                                    showMoreMenu = false
                                },
                                enabled = viewMode != ScheduleViewMode.DAY
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier
                    .padding(16.dp)
                    .padding(bottom = paddingValues.calculateBottomPadding()),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加日程/课程"
                )
            }
        }
    ) { innerPadding -> // Scaffold 提供内边距
        // --- Main Content Column ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    start = innerPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = innerPadding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    // 组合内部 Scaffold 和外部传入的 paddingValues 的底部 padding
                    bottom = paddingValues.calculateBottomPadding()
                ) // 应用Scaffold提供的内边距，并确保底部不与导航栏重叠
                .background(backgroundColor) // 应用背景色
        ) {
            // --- TopAppBar 占位符或者实际内容 ---
            // Spacer(modifier = Modifier.height(8.dp)) // 这个Spacer可能不再需要，因为TopAppBar已在Scaffold中定义

            // --- 内容区域 (The when block) --- //
            Box(
                modifier = Modifier
                    .weight(1f) // 占据剩余空间
                    .fillMaxWidth()
            ) {
                // --- 新增: 根据视图模式切换内容 ---
                when (viewMode) {
                    ScheduleViewMode.WEEK -> {
                        when (uiState) {
                            is ScheduleUiState.Loading -> LoadingScreen()
                            is ScheduleUiState.Success -> {
                                ScheduleContent(
                                    currentWeek = currentWeek,
                                    timeSlotsForCurrentWeek = displayableTimeSlots,
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
                            is ScheduleUiState.Empty -> EmptyScheduleScreen()
                            is ScheduleUiState.NoTableSelected -> NoTableSelectedScreen(
                                navigationState = navigationState,
                                onNavigateToImport = {
                                    navigationState.navigateSchoolSelectorScreen(
                                        AppConstants.Ids.INVALID_TABLE_ID
                                    )
                                }
                            )
                        }
                    }

                    ScheduleViewMode.MONTH -> {
                        // --- 优化：传递 currentYearMonth 和回调 ---
                        MonthSchedulePager(
                            navigationState = navigationState,
                            defaultTableId = defaultTableId,
                            viewModel = viewModel,
                            paddingValues = paddingValues,
                            initialYearMonth = currentYearMonth,
                            onYearMonthChange = { currentYearMonth = it }
                        )
                    }

                    ScheduleViewMode.DAY -> {
                        // --- 占位：日视图 ---
                        DayScheduleContent(
                            viewModel = viewModel,
                            navigationState = navigationState,
                            defaultTableId = defaultTableId
                        )
                    }
                }
            } // End Content 'when' Box
        } // End Main Content Column
    } // End Scaffold

    // --- ModalBottomSheet ---
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            QuickAddScheduleSheetContent(
                onDismiss = { showBottomSheet = false }
            )
        }
    }

    // 显示添加日程/课程对话框
    if (showDialog) {
        QuickAddScheduleDialog(
            onDismiss = { showDialog = false },
            viewModel = hiltViewModel()
        )
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
        try {
        if (currentWeek - 1 != pagerState.currentPage) {
                Log.d(
                    "ScheduleContent",
                    "ViewModel 更新 currentWeek: $currentWeek, 当前页面: ${pagerState.currentPage}"
                )
                val targetPage = currentWeek - 1
                val currentPage = pagerState.currentPage
                val distance = abs(targetPage - currentPage)

                Log.d("ScheduleContent", "计算跳转距离: $distance 周")

                if (distance > 3) {
                    // 距离较大时直接跳转，避免动画问题
                    Log.d("ScheduleContent", "检测到大距离跳转 ($distance 周), 使用直接跳转")
                    try {
                        // 使用withContext(Dispatchers.Main)确保在主线程上执行UI操作
                        withContext(Dispatchers.Main) {
                            pagerState.scrollToPage(targetPage)
                        }
                        Log.d("ScheduleContent", "直接跳转到第 $targetPage 页成功")
            } catch (e: Exception) {
                        Log.e("ScheduleContent", "直接跳转失败，尝试备用方案", e)
                        // 如果scrollToPage失败，尝试animateScrollToPage作为备用方案
                        pagerState.animateScrollToPage(targetPage)
                    }
                } else {
                    // 距离较小时使用动画
                    Log.d("ScheduleContent", "检测到小距离跳转 ($distance 周), 使用动画滚动")
                    pagerState.animateScrollToPage(targetPage)
                }
            }
        } catch (e: Exception) {
            Log.e("ScheduleContent", "滚动过程中发生错误", e)
            // 如果任何滚动操作失败，尝试立即跳转
            try {
                Log.d("ScheduleContent", "尝试最后的直接跳转恢复方案")
                    pagerState.scrollToPage(currentWeek - 1)
                } catch (scrollError: Exception) {
                Log.e("ScheduleContent", "最终恢复方案也失败", scrollError)
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
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1, // 预加载相邻页面，提高滑动流畅度
                pageSpacing = 0.dp // 确保页面间无间隙
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
 * 计算周视图事件的位置，处理同一天内重叠的事件
 * 参考日视图的重叠处理方式
 */
@OptIn(ExperimentalStdlibApi::class)
private fun calculateWeekViewEventPositions(
    events: List<TimeSlot>,
    gridStartHour: Int
): List<Pair<TimeSlot, WeekViewSlotInfo>> {
    if (events.isEmpty()) return emptyList()
    
    // 1. 转换事件为带有时间信息的对象
    val eventWithTimes = events.map { event ->
        val start = Instant.fromEpochMilliseconds(event.startTime)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val end = Instant.fromEpochMilliseconds(event.endTime)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            
        val dayOfWeek = start.dayOfWeek.isoDayNumber
        val startMinutes = (start.hour - gridStartHour) * 60 + start.minute
        val endMinutes = (end.hour - gridStartHour) * 60 + end.minute
        
        Triple(event, dayOfWeek, WeekViewSlotInfo(dayOfWeek, startMinutes, endMinutes))
    }
    
    // 2. 按天分组
    val eventsByDay = eventWithTimes.groupBy { it.second }
    
    // 3. 处理每天内部的重叠事件
    val result = mutableListOf<Pair<TimeSlot, WeekViewSlotInfo>>()
    
    eventsByDay.forEach { (day, dayEvents) ->
        // 如果该天只有一个事件，不需要处理重叠
        if (dayEvents.size == 1) {
            val (event, _, slotInfo) = dayEvents[0]
            result.add(event to slotInfo)
            return@forEach
        }
        
        // 处理重叠：找出同一天内时间上重叠的事件
        val sortedDayEvents = dayEvents.sortedBy { it.third.startMinutes }
        
        // 构建重叠矩阵
        val overlapMatrix = Array(sortedDayEvents.size) { BooleanArray(sortedDayEvents.size) { false } }
        
        for (i in sortedDayEvents.indices) {
            for (j in i + 1 until sortedDayEvents.size) {
                val a = sortedDayEvents[i].third
                val b = sortedDayEvents[j].third
                
                // 检查是否重叠 (与日视图使用相同的判断逻辑)
                val buffer = 1 // 1分钟缓冲区
                if (a.startMinutes < (b.endMinutes - buffer) && (a.endMinutes - buffer) > b.startMinutes) {
                    overlapMatrix[i][j] = true
                    overlapMatrix[j][i] = true
                    Log.d("WeekSchedule", "同一天内事件重叠: Day=$day, 事件${i}和事件${j}")
                }
            }
        }
        
        // 构建重叠组 (连通分量)
        val visited = BooleanArray(sortedDayEvents.size) { false }
        val overlapGroups = mutableListOf<List<Int>>()
        
        for (i in sortedDayEvents.indices) {
            if (!visited[i]) {
                val group = mutableListOf<Int>()
                val queue = LinkedList<Int>()
                queue.add(i)
                visited[i] = true
                
                while (queue.isNotEmpty()) {
                    val current = queue.remove()
                    group.add(current)
                    
                    for (j in sortedDayEvents.indices) {
                        if (!visited[j] && overlapMatrix[current][j]) {
                            queue.add(j)
                            visited[j] = true
                        }
                    }
                }
                
                overlapGroups.add(group)
                Log.d("WeekSchedule", "找到重叠组: 天=$day, 组大小=${group.size}")
            }
        }
        
        // 为每个重叠组进行列分配
        overlapGroups.forEach { group ->
            // 简单情况：组内只有一个事件
            if (group.size == 1) {
                val idx = group[0]
                val (event, _, slotInfo) = sortedDayEvents[idx]
                result.add(event to slotInfo)
                return@forEach
            }
            
            // 为组内事件分配列
            val columnEndTimes = mutableListOf<Int>()
            
            group.forEach { idx ->
                val (event, _, slotInfo) = sortedDayEvents[idx]
                
                // 寻找可用列
                var assignedColumn = -1
                
                for (col in columnEndTimes.indices) {
                    if (slotInfo.startMinutes >= columnEndTimes[col]) {
                        assignedColumn = col
                        break
                    }
                }
                
                // 如果没有找到可用列，创建新列
                if (assignedColumn == -1) {
                    assignedColumn = columnEndTimes.size
                    columnEndTimes.add(slotInfo.endMinutes)
                } else {
                    // 更新列结束时间
                    columnEndTimes[assignedColumn] = slotInfo.endMinutes
                }
                
                // 更新位置信息
                val updatedSlotInfo = slotInfo.copy(
                    column = assignedColumn,
                    maxColumns = columnEndTimes.size
                )
                
                result.add(event to updatedSlotInfo)
            }
        }
    }
    
    return result
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
        
        Log.d("WeekSchedule", "事件 ${event.displayTitle} - 原始持续时间: ${durationMinutes}分钟, 用于计算重叠的持续时间: ${endMinutes - startMinutes}分钟")
        
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
            for (j in i+1 until size) {
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
                Log.d("WeekSchedule", "重叠项目布局: 列=${slotInfo.column}/${slotInfo.maxColumns}, 事件='${timeSlot.displayTitle}'")
                
                // 可用宽度 = 日宽度 - 两侧边距 - 列间隙宽度总和
                val availableWidth = dayWidthPx - (2 * marginPx) - (gapPx * (slotInfo.maxColumns - 1))
                
                // 确保每列宽度合理
                val columnWidth = (availableWidth / slotInfo.maxColumns).coerceAtLeast(20f) // 确保最小宽度
                
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
                Log.d("WeekSchedule", "重叠项目详细 - ID: ${timeSlot.id}, 标题: '${timeSlot.displayTitle}', " +
                        "列: ${slotInfo.column}/${slotInfo.maxColumns}, 宽度: ${itemWidthPx}px, 偏移: ${xOffsetPx - baseXOffset}px")
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

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

/**
 * 月视图 Composable：顶部大号年月、星期标题行、支持大/中/小月历切换
 */
@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalStdlibApi::class
)
@Composable
fun MonthSchedulePager(
    navigationState: NavigationState,
    defaultTableId: Int?,
    viewModel: ScheduleViewModel,
    paddingValues: PaddingValues,
    initialYearMonth: YearMonth,
    onYearMonthChange: (YearMonth) -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = 500,
        pageCount = { 1000 }
    )
    val baseYearMonth = remember { YearMonth.now() }
    val currentPage = pagerState.currentPage
    val currentYearMonth = remember(currentPage) {
        baseYearMonth.plusMonths((currentPage - 500).toLong())
    }
    // 监听外部指定的年月，自动滚动到对应月份
    LaunchedEffect(initialYearMonth) {
        if (initialYearMonth != currentYearMonth) {
            // 计算目标月份与基准月份的差距
            val monthsDiff = YearMonth.of(
                initialYearMonth.year,
                initialYearMonth.monthValue
            ).toEpochMonth() - baseYearMonth.toEpochMonth()
            // 计算目标页码
            val targetPage = 500 + monthsDiff.toInt()
            // 如果目标页码在有效范围内且与当前页不同，则滚动到目标页
            if (targetPage in 0 until 1000 && targetPage != pagerState.currentPage) {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }
    // 统一管理大/小格子模式
    var isLargeMode by remember { mutableStateOf(false) }
    LaunchedEffect(currentYearMonth) {
        onYearMonthChange(currentYearMonth)
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1, // 预加载相邻页面，提高滑动流畅度
        pageSpacing = 0.dp // 确保页面间无间隙
    ) { page ->
        val yearMonth = baseYearMonth.plusMonths((page - 500).toLong())
        MonthSchedulePage(
            navigationState = navigationState,
            defaultTableId = defaultTableId,
            viewModel = viewModel,
            yearMonth = yearMonth,
            isLargeMode = isLargeMode,
            onLargeModeChange = { isLargeMode = it }
        )
    }
}

/**
 * 将 YearMonth 转换为从 1970 年 1 月算起的月份数
 */
private fun YearMonth.toEpochMonth(): Long {
    return year.toLong() * 12L + monthValue - 1
}

@Composable
fun MonthSchedulePage(
    navigationState: NavigationState,
    defaultTableId: Int?,
    viewModel: ScheduleViewModel,
    yearMonth: YearMonth,
    isLargeMode: Boolean,
    onLargeModeChange: (Boolean) -> Unit
) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    var selectedDate by remember { mutableStateOf(today) }
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
    val monthTimeSlots by viewModel.getDisplayableTimeSlotsForMonth(year, month).collectAsState()
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
                                                .clickable { selectedDate = date }
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

/**
 * 占位：日视图 Composable
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
            try {
                // 计算目标日期与今天相差的天数
                val daysDiff = CalendarUtils.calculateDaysBetween(today, targetDate)
                // 计算目标页码
                val targetPage = initialPage + daysDiff

                // 如果目标页码在有效范围内且与当前页不同，则滚动到目标页
                if (targetPage in 0 until pageCount && targetPage != pagerState.currentPage) {
                    // 使用协程上下文中的launch，避免在LaunchedEffect中直接调用挂起函数
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(targetPage)
                    }
                }
            } catch (e: Exception) {
                Log.e("DayScheduleContent", "Error scrolling to target date", e)
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

    // 获取当前日期的TimeSlot
    val daySlots by viewModel.getDisplayableTimeSlotsForMonth(
        currentDate.year,
        currentDate.monthNumber
    ).collectAsState()
    val slotsForDay = remember(daySlots, currentDate) {
        daySlots.filter {
            val slotDate = Instant.fromEpochMilliseconds(it.startTime)
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            slotDate == currentDate
        }.sortedBy { it.startTime }
    }
    // 全天事件与普通事件分离（此处简单分离，实际可根据业务调整）
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
            val slots = daySlots.filter {
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
fun DayViewHeader(
    date: LocalDate,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(8.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevDay) { Icon(Icons.Default.ArrowBack, "前一天") }
        Text(
            "${date.year}年${date.monthNumber}月${date.dayOfMonth}日，${date.dayOfWeek.getChineseWeekName()}",
            Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
        IconButton(onClick = onNextDay) { Icon(Icons.Default.ArrowForward, "后一天") }
        IconButton(onClick = onToday) { Icon(Icons.Default.Home, "今天") }
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

                // 当前时间指示器
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
                        
                        Log.d("DaySchedule", "重叠卡片 ${event.displayTitle} - 列:${slotInfo.column}/${slotInfo.maxColumns}, " +
                                "宽度:${cardWidthPx}px, 偏移:${offsetPx}px")
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
            }
        }
    }
}

/**
 * 事件在时间轴上的位置信息
 */
data class EventSlotInfo(
    val startMinutes: Int,
    val endMinutes: Int,
    val column: Int = 0,
    val maxColumns: Int = 1
)

/**
 * 计算事件位置，处理重叠事件
 * 使用广度优先搜索(BFS)建立连通分量，确保所有直接或间接重叠的事件都被分到同一组
 * 使用贪心算法进行列分配，优先填充已有列位置，减少总列数
 */
@OptIn(ExperimentalStdlibApi::class)
fun calculateEventPositions(
    events: List<TimeSlot>,
    date: LocalDate
): List<Pair<TimeSlot, EventSlotInfo>> {
    if (events.isEmpty()) return emptyList()
    
    Log.d("DaySchedule", "计算${events.size}个事件的位置")
    
    // 设置最小时间段为20分钟（用于计算重叠）
    val minDurationMinutes = 20

    // 按开始时间排序
    val sortedEvents = events.sortedBy { it.startTime }

    // 转换为带位置信息的列表
    val eventSlots = sortedEvents.map { event ->
        val start = Instant.fromEpochMilliseconds(event.startTime)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val end = Instant.fromEpochMilliseconds(event.endTime)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        val gridStartHour = GRID_START_HOUR
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

        Log.d("DaySchedule", "事件 ${event.displayTitle} - 原始持续时间: ${durationMinutes}分钟, 用于计算重叠的持续时间: ${endMinutes - startMinutes}分钟")

        event to EventSlotInfo(startMinutes, endMinutes)
    }.toMutableList()

    // 步骤1: 计算所有重叠区间（任意两个事件间的重叠）
    val overlapMatrix = Array(eventSlots.size) { BooleanArray(eventSlots.size) { false } }
    
    for (i in eventSlots.indices) {
        for (j in i + 1 until eventSlots.size) {
            if (isOverlapping(eventSlots[i].second, eventSlots[j].second)) {
                overlapMatrix[i][j] = true
                overlapMatrix[j][i] = true
                Log.d("DaySchedule", "事件 ${i}(${eventSlots[i].first.displayTitle}) 和 ${j}(${eventSlots[j].first.displayTitle}) 重叠")
            }
        }
    }
    
    // 步骤2: 使用广度优先搜索(BFS)构建重叠组（连通分量）
    val visited = BooleanArray(eventSlots.size) { false }
    val overlapGroups = mutableListOf<List<Int>>()
    
    for (i in eventSlots.indices) {
        if (!visited[i]) {
            val group = mutableListOf<Int>()
            val queue = ArrayDeque<Int>()
            queue.addLast(i)
            visited[i] = true
            
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                group.add(current)
                
                for (j in eventSlots.indices) {
                    if (!visited[j] && overlapMatrix[current][j]) {
                        queue.addLast(j)
                        visited[j] = true
                    }
                }
            }
            
            overlapGroups.add(group)
            Log.d("DaySchedule", "重叠组 ${overlapGroups.size}: ${group.map { eventSlots[it].first.displayTitle }}")
        }
    }
    
    // 步骤3: 对每个重叠组，使用贪心算法为事件分配最佳列位置
    val result = eventSlots.toMutableList()
    
    for ((groupIndex, group) in overlapGroups.withIndex()) {
        // 特殊情况：只有一个事件的组不需要特殊处理
        if (group.size == 1) {
            val index = group[0]
            val (event, slotInfo) = result[index]
            result[index] = event to slotInfo.copy(column = 0, maxColumns = 1)
            continue
        }
        
        // 按开始时间排序组内事件，确保先开始的事件优先分配列
        val sortedIndices = group.sortedBy { result[it].second.startMinutes }
        
        // 列占用跟踪：每列存储已分配事件的结束时间
        val columnEndTimes = mutableListOf<Int>()
        
        // 为组内每个事件贪心分配列
        for (eventIndex in sortedIndices) {
            val (event, slotInfo) = result[eventIndex]
            
            // 寻找最早可用的列
            var assignedColumn = -1
            
            for (col in columnEndTimes.indices) {
                if (slotInfo.startMinutes >= columnEndTimes[col]) {
                    assignedColumn = col
                    break
                }
            }
            
            // 如果没有找到可用列，创建新列
            if (assignedColumn == -1) {
                assignedColumn = columnEndTimes.size
                columnEndTimes.add(slotInfo.endMinutes)
            } else {
                // 更新现有列的结束时间
                columnEndTimes[assignedColumn] = slotInfo.endMinutes
            }
            
            // 更新事件的列信息
            result[eventIndex] = event to slotInfo.copy(
                column = assignedColumn,
                maxColumns = columnEndTimes.size
            )
            
            Log.d("DaySchedule", "为事件 ${event.displayTitle} 分配列 $assignedColumn/${columnEndTimes.size}")
        }
        
        // 最后确认该组的所有事件使用相同的maxColumns值
        val finalMaxColumns = columnEndTimes.size
        for (eventIndex in group) {
            val (event, slotInfo) = result[eventIndex]
            result[eventIndex] = event to slotInfo.copy(maxColumns = finalMaxColumns)
        }
        
        Log.d("DaySchedule", "重叠组 $groupIndex 使用 $finalMaxColumns 列显示")
    }

    return result
}

/**
 * 检查两个时间段是否重叠
 */
fun isOverlapping(a: EventSlotInfo, b: EventSlotInfo): Boolean {
    // 添加1分钟的缓冲区，避免时间刚好接壤的事件被判定为重叠
    val buffer = 1 // 1分钟缓冲区
    return a.startMinutes < (b.endMinutes - buffer) && (a.endMinutes - buffer) > b.startMinutes
}

