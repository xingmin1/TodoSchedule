package com.example.todoschedule.ui.schedule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.ui.navigation.NavigationState
import com.example.todoschedule.ui.schedule.model.ScheduleUiState
import kotlinx.datetime.toLocalDateTime


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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navigationState: NavigationState,
    onNavigateToSettings: () -> Unit,
    paddingValues: PaddingValues,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    // --- 状态收集 ---
    val uiState by viewModel.uiState.collectAsState()
    val currentWeek by viewModel.currentWeek.collectAsState()
//    val weekDates by viewModel.weekDates.collectAsState()
//    val displayableTimeSlots by viewModel.displayableTimeSlots.collectAsState()
    val defaultTableId by viewModel.defaultTableIdState.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val currentDayDate by viewModel.currentDayDate.collectAsState()
    val currentTableState by viewModel.currentTablesState.collectAsState()
//    val startDate = currentTableState?.startDate
//    val currentDate by viewModel.currentDate.collectAsState()
    val currentTable by viewModel.currentTable.collectAsState()
//    val allTables by viewModel.allTables.collectAsState()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
//    var lastTableId by remember { mutableStateOf<Int?>(null) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var currentYearMonth by remember { mutableStateOf(java.time.YearMonth.now()) }

    // --- 顶部栏 ---
    Scaffold(
        topBar = {
            ScheduleTopBar(
                viewMode = viewMode,
                formattedDate = when (viewMode) {
                    ScheduleViewMode.MONTH -> "${currentYearMonth.year}/${
                        currentYearMonth.monthValue.toString().padStart(2, '0')
                    }"

                    ScheduleViewMode.DAY -> currentDayDate?.let {
                        "${it.year}/${
                            it.monthNumber.toString().padStart(2, '0')
                        }/${it.dayOfMonth.toString().padStart(2, '0')}"
                    } ?: "--/--/--"

                    else -> {
                        val today = kotlinx.datetime.Clock.System.now()
                            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
                        "${today.year}/${
                            today.monthNumber.toString().padStart(2, '0')
                        }/${today.dayOfMonth.toString().padStart(2, '0')}"
                    }
                },
                weekInfoText = when (viewMode) {
                    ScheduleViewMode.DAY -> currentDayDate?.dayOfWeek?.getChineseWeekName() ?: ""
                    ScheduleViewMode.WEEK -> "第${currentWeek}周 " + (currentDayDate?.dayOfWeek?.getChineseWeekName()
                        ?: "")

                    else -> null
                },
                onGoToToday = {
                    when (viewMode) {
                        ScheduleViewMode.WEEK -> viewModel.goToCurrentWeek()
                        ScheduleViewMode.MONTH -> {
                            val today = kotlinx.datetime.Clock.System.now()
                                .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
                            currentYearMonth = java.time.YearMonth.of(today.year, today.monthNumber)
                        }

                        ScheduleViewMode.DAY -> {
                            val today = kotlinx.datetime.Clock.System.now()
                                .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
                            viewModel.updateCurrentDayDate(today)
                        }
                    }
                },
                onAdd = { showDialog = true },
                onDownload = {
                    navigationState.navigateSchoolSelectorScreen(
                        defaultTableId
                            ?: com.example.todoschedule.core.constants.AppConstants.Ids.INVALID_TABLE_ID
                    )
                },
                onShare = { /* TODO: 分享逻辑 */ },
                onChangeViewMode = { viewModel.setViewMode(it) },
                showMoreMenu = showMoreMenu,
                onShowMoreMenu = { showMoreMenu = true },
                onDismissMoreMenu = { showMoreMenu = false }
            )
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
        },
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    start = innerPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = innerPadding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    bottom = paddingValues.calculateBottomPadding()
                )
        ) {
            // 多课表无数据提示
            if (currentTable == null) {
                NoTableBanner(
                    onCreateTable = { navigationState.navigateToCreateEditTable() },
                    onImportTable = { navigationState.navigateSchoolSelectorScreen(com.example.todoschedule.core.constants.AppConstants.Ids.INVALID_TABLE_ID) }
                )
            }
            // 内容分发
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (viewMode) {
                    ScheduleViewMode.WEEK -> {
                        when (uiState) {
                            is ScheduleUiState.Loading -> LoadingScreen()
                            is ScheduleUiState.Success -> ScheduleWeekContent(
                                viewModel = viewModel,
                                onTimeSlotClick = { /* 处理点击 */ }
                            )

                            is ScheduleUiState.Error -> ErrorScreen((uiState as ScheduleUiState.Error).message)
                            is ScheduleUiState.Empty -> EmptyScheduleScreen()
                            is ScheduleUiState.NoTableSelected -> NoTableSelectedScreen(
                                navigationState = navigationState,
                                onNavigateToImport = {
                                    navigationState.navigateSchoolSelectorScreen(com.example.todoschedule.core.constants.AppConstants.Ids.INVALID_TABLE_ID)
                                }
                            )
                        }
                    }

                    ScheduleViewMode.MONTH -> {
                        ScheduleMonthContent(
                            navigationState = navigationState,
                            defaultTableId = defaultTableId,
                            viewModel = viewModel,
                            paddingValues = paddingValues,
                            initialYearMonth = currentYearMonth,
                            onYearMonthChange = { currentYearMonth = it }
                        )
                    }

                    ScheduleViewMode.DAY -> {
                        DayScheduleContent(
                            viewModel = viewModel,
                            navigationState = navigationState,
                            defaultTableId = defaultTableId
                        )
                    }
                }
            }
        }
    }
    // --- 弹窗 ---
    if (showDialog) {
        QuickAddScheduleDialog(
            onDismiss = { showDialog = false },
            viewModel = hiltViewModel<QuickAddScheduleViewModel>()
        )
    }
    // --- BottomSheet ---
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            // 这里调用你的底部弹窗内容组件
        }
    }
}




























