package com.example.todoschedule.ui.schedule

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.ui.navigation.NavigationState
import com.example.todoschedule.ui.schedule.model.ScheduleUiState
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale


/**
 * 课程表屏幕 Composable 函数。
 *
 * 负责展示课程表界面，包括顶部应用栏、
 * 课程/日程网格、时间轴以及添加/导入按钮。
 * 它根据 ViewModel 提供的状态 (uiState) 显示不同的界面内容（加载中、成功、空、错误、未选择课表）。
 *
 * @param navigationState 用于处理导航事件的对象。
 * @param paddingValues 用于处理 Scaffold 的 padding 参数。
 * @param viewModel ScheduleViewModel 的实例，用于获取界面状态和数据。
 */
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navigationState: NavigationState,
    paddingValues: PaddingValues,
    viewModel: ScheduleViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit
) {
    // --- 状态收集 ---
    val uiState by viewModel.uiState.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val anchorDate by viewModel.anchorDate.collectAsState()
    val defaultTableIds by viewModel.defaultTableIdState.collectAsState()
    val currentActiveTable by viewModel.currentActiveTable.collectAsState()

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }

    // For MonthView's Pager state, driven by anchorDate
    var currentDisplayYearMonth by remember { mutableStateOf(YearMonth.from(anchorDate.toJavaLocalDate())) }
    LaunchedEffect(anchorDate) {
        currentDisplayYearMonth = YearMonth.from(anchorDate.toJavaLocalDate())
    }

    // --- 顶部栏 --- //
    Scaffold(
        topBar = {
            val topBarDateText: String
            val topBarWeekInfo: String?

            when (viewMode) {
                ScheduleViewMode.MONTH -> {
                    topBarDateText = "${currentDisplayYearMonth.year}/${currentDisplayYearMonth.monthValue.toString().padStart(2, '0')}"
                    topBarWeekInfo = null
                }
                ScheduleViewMode.DAY -> {
                    topBarDateText = "${anchorDate.year}/${anchorDate.monthNumber.toString().padStart(2, '0')}/${anchorDate.dayOfMonth.toString().padStart(2, '0')}"
                    topBarWeekInfo = anchorDate.dayOfWeek.getChineseWeekName()
                }
                ScheduleViewMode.WEEK -> {
                    val monday = anchorDate.minus((anchorDate.dayOfWeek.isoDayNumber - 1).toLong(), DateTimeUnit.DAY)
                    val sunday = monday.plus(6, DateTimeUnit.DAY)
                    topBarDateText = "${monday.monthNumber}/${monday.dayOfMonth} - ${sunday.monthNumber}/${sunday.dayOfMonth}"
                    // Week info for week view might show the month or be empty
                    val monthName = monday.month.getDisplayName(TextStyle.FULL, Locale.CHINA)
                    topBarWeekInfo = if (monday.monthNumber != sunday.monthNumber) {
                        "${monday.month.getDisplayName(TextStyle.SHORT, Locale.CHINA)} - ${sunday.month.getDisplayName(TextStyle.SHORT, Locale.CHINA)}"
                    } else {
                        monthName
                    }
                }
            }

            ScheduleTopBar(
                viewMode = viewMode,
                formattedDate = topBarDateText,
                weekInfoText = topBarWeekInfo,
                onGoToToday = {
                    viewModel.goToToday() // Universal go to today
                },
                onAdd = { showQuickAddDialog = true },
                onDownload = {
                    navigationState.navigateSchoolSelectorScreen(
                        defaultTableIds.firstOrNull() 
                            ?: AppConstants.Ids.INVALID_TABLE_ID
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
                onClick = { showQuickAddDialog = true },
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
                    bottom = 0.dp 
                )
        ) {
            if (currentActiveTable == null && defaultTableIds.isEmpty()) { 
                NoTableBanner(
                    onCreateTable = { navigationState.navigateToCreateEditTable() },
                    onImportTable = { navigationState.navigateSchoolSelectorScreen(AppConstants.Ids.INVALID_TABLE_ID) }
                )
            }
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
                                viewModel = viewModel, // Passes the whole viewModel
                                onTimeSlotClick = { /* TODO: Handle TimeSlot click */ }
                            )
                            is ScheduleUiState.Error -> ErrorScreen((uiState as ScheduleUiState.Error).message)
                            is ScheduleUiState.Empty -> EmptyScheduleScreen()
                            is ScheduleUiState.NoTableSelected -> NoTableSelectedScreen(
                                navigationState = navigationState,
                                onNavigateToImport = {
                                    navigationState.navigateSchoolSelectorScreen(AppConstants.Ids.INVALID_TABLE_ID)
                                }
                            )
                        }
                    }
                    ScheduleViewMode.MONTH -> {
                        ScheduleMonthContent(
                            navigationState = navigationState,
                            defaultTableId = defaultTableIds.firstOrNull(),
                            viewModel = viewModel, // Passes the whole viewModel
                            paddingValues = innerPadding, 
                            initialYearMonth = currentDisplayYearMonth, // Driven by anchorDate
                            onYearMonthChange = { yearMonth ->
                                // When month pager changes, update the anchorDate
                                val firstDayOfMonth = LocalDate(yearMonth.year, yearMonth.monthValue, 1)
                                viewModel.setAnchorDate(firstDayOfMonth)
                            }
                        )
                    }
                    ScheduleViewMode.DAY -> {
                        DayScheduleContent(
                            viewModel = viewModel, // Passes the whole viewModel
                            navigationState = navigationState,
                            defaultTableId = defaultTableIds.firstOrNull()
                        )
                    }
                }
            }
        }
    }

    if (showQuickAddDialog) {
        QuickAddScheduleDialog(
            onDismiss = { showQuickAddDialog = false },
            viewModel = hiltViewModel<QuickAddScheduleViewModel>()
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            // TODO: BottomSheet content composable here
        }
    }
}




























