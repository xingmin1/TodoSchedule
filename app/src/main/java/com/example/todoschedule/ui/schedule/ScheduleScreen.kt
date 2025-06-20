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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.domain.model.TimeSlot
import com.example.todoschedule.ui.navigation.NavigationState
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import java.time.YearMonth
import java.util.UUID


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
    val isMonthLargeMode by viewModel.isMonthLargeMode.collectAsState()

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }

    // 切换视图时直接调用 ViewModel
    fun handleViewModeChange(newMode: ScheduleViewMode) {
        viewModel.setViewMode(newMode)
    }

    // 顶部栏日期展示用中文
    fun getChineseWeekName(dayOfWeek: kotlinx.datetime.DayOfWeek): String =
        when (dayOfWeek) {
            kotlinx.datetime.DayOfWeek.MONDAY -> "周一"
            kotlinx.datetime.DayOfWeek.TUESDAY -> "周二"
            kotlinx.datetime.DayOfWeek.WEDNESDAY -> "周三"
            kotlinx.datetime.DayOfWeek.THURSDAY -> "周四"
            kotlinx.datetime.DayOfWeek.FRIDAY -> "周五"
            kotlinx.datetime.DayOfWeek.SATURDAY -> "周六"
            kotlinx.datetime.DayOfWeek.SUNDAY -> "周日"
        }

    fun getChineseMonthName(month: kotlinx.datetime.Month): String =
        when (month) {
            kotlinx.datetime.Month.JANUARY -> "一月"
            kotlinx.datetime.Month.FEBRUARY -> "二月"
            kotlinx.datetime.Month.MARCH -> "三月"
            kotlinx.datetime.Month.APRIL -> "四月"
            kotlinx.datetime.Month.MAY -> "五月"
            kotlinx.datetime.Month.JUNE -> "六月"
            kotlinx.datetime.Month.JULY -> "七月"
            kotlinx.datetime.Month.AUGUST -> "八月"
            kotlinx.datetime.Month.SEPTEMBER -> "九月"
            kotlinx.datetime.Month.OCTOBER -> "十月"
            kotlinx.datetime.Month.NOVEMBER -> "十一月"
            kotlinx.datetime.Month.DECEMBER -> "十二月"
        }

    // 统一的日程卡片点击跳转逻辑，供各视图调用
    fun handleTimeSlotClick(
        timeSlot: TimeSlot,
        navigationState: NavigationState,
        tableId: UUID?
    ) {
        when (timeSlot.scheduleType) {
            ScheduleType.COURSE -> {
                val tableIdToUse = tableId ?: AppConstants.Ids.INVALID_TABLE_ID
                if (tableIdToUse != AppConstants.Ids.INVALID_TABLE_ID) {
                    navigationState.navigateToCourseDetail(
                        tableId = tableIdToUse,
                        courseId = timeSlot.scheduleId
                    )
                }
            }

            ScheduleType.ORDINARY -> {
                navigationState.navigateToOrdinaryScheduleDetail(timeSlot.scheduleId)
            }

            else -> { /* 其他类型暂不处理 */
            }
        }
    }

    // --- 顶部栏 --- //
    Scaffold(
        topBar = {
            val topBarDateText: String
            val topBarWeekInfo: String?

            when (viewMode) {
                ScheduleViewMode.MONTH -> {
                    topBarDateText =
                        "${anchorDate.year}/${anchorDate.monthNumber.toString().padStart(2, '0')}"
                    topBarWeekInfo = null
                }

                ScheduleViewMode.DAY -> {
                    topBarDateText = "${anchorDate.year}/${
                        anchorDate.monthNumber.toString().padStart(2, '0')
                    }/${anchorDate.dayOfMonth.toString().padStart(2, '0')}"
                    topBarWeekInfo = getChineseWeekName(anchorDate.dayOfWeek)
                }

                ScheduleViewMode.WEEK -> {
                    val monday = anchorDate.minus(
                        (anchorDate.dayOfWeek.isoDayNumber - 1).toLong(),
                        DateTimeUnit.DAY
                    )
                    val sunday = monday.plus(6, DateTimeUnit.DAY)
                    topBarDateText =
                        "${monday.monthNumber}/${monday.dayOfMonth} - ${sunday.monthNumber}/${sunday.dayOfMonth}"
                    val monthName = getChineseMonthName(monday.month)
                    topBarWeekInfo = if (monday.monthNumber != sunday.monthNumber) {
                        "${getChineseMonthName(monday.month)} - ${getChineseMonthName(sunday.month)}"
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
                            ?: UUID.randomUUID()
                    )
                },
                onShare = { /* TODO: 分享逻辑 */ },
                onChangeViewMode = { handleViewModeChange(it) },
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
                    onImportTable = { navigationState.navigateSchoolSelectorScreen(UUID.randomUUID()) }
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (viewMode) {
                    ScheduleViewMode.WEEK -> {
                        ScheduleWeekContent(
                            anchorDate = anchorDate,
                            onDateChange = { viewModel.setAnchorDate(it) },
                            viewModel = viewModel,
                            onTimeSlotClick = { timeSlot ->
                                val tableId =
                                    currentActiveTable?.id ?: defaultTableIds.firstOrNull()
                                handleTimeSlotClick(timeSlot, navigationState, tableId)
                            }
                        )
                    }

                    ScheduleViewMode.MONTH -> {
                        ScheduleMonthContent(
                            initialYearMonth = YearMonth.of(
                                anchorDate.year,
                                anchorDate.monthNumber
                            ),
                            onYearMonthChange = { newYearMonth ->
                                // 尝试保留当前 anchorDate 的"日"，如果新月没有该日，则用新月最后一天
                                val day = anchorDate.dayOfMonth.coerceAtMost(
                                    YearMonth.of(newYearMonth.year, newYearMonth.monthValue)
                                        .lengthOfMonth()
                                )
                                viewModel.setAnchorDate(
                                    kotlinx.datetime.LocalDate(
                                        newYearMonth.year,
                                        newYearMonth.monthValue,
                                        day
                                    )
                                )
                            },
                            navigationState = navigationState,
                            defaultTableId = defaultTableIds.firstOrNull(),
                            viewModel = viewModel,
                            paddingValues = innerPadding,
                            isLargeMode = isMonthLargeMode,
                            onLargeModeChange = { viewModel.setMonthLargeMode(it) },
                            selectedDate = anchorDate,
                            onDateSelected = { viewModel.setAnchorDate(it) }
                        )
                    }

                    ScheduleViewMode.DAY -> {
                        DayScheduleContent(
                            anchorDate = anchorDate,
                            onDateChange = { viewModel.setAnchorDate(it) },
                            viewModel = viewModel,
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




























