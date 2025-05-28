package com.example.todoschedule.ui.study

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    paddingValues: PaddingValues = PaddingValues(),
    viewModel: StudyViewModel
) {
    val stats by viewModel.studyStat.collectAsStateWithLifecycle()
    val studyPlans by viewModel.studyPlans.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val showStudyMenu by viewModel.showStudyMenu.collectAsStateWithLifecycle()
    val selectedPlan by viewModel.selectedPlan.collectAsStateWithLifecycle()
    val selectedStat by viewModel.selectedStat.collectAsStateWithLifecycle()
    val selectedStatFilteredRecords by viewModel.selectedStatRecords.collectAsStateWithLifecycle()
    val showTimerSettings by viewModel.showTimerSettingsModal.collectAsStateWithLifecycle()
    val showTaskSelection by viewModel.showTaskSelectionModal.collectAsStateWithLifecycle()
    val showFocusHistory by viewModel.showFocusHistoryModal.collectAsStateWithLifecycle()
    val focusHistoryList by viewModel.focusHistory.collectAsStateWithLifecycle()
    val showAddEditPlanModal by viewModel.showAddEditPlanModal.collectAsStateWithLifecycle()
    val editingPlan by viewModel.editingPlan.collectAsStateWithLifecycle()
    val showDeletePlanConfirmation by viewModel.showDeletePlanConfirmation.collectAsStateWithLifecycle()
    val planToDelete by viewModel.planToDelete.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("学习", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                actions = {
                    IconButton(onClick = { viewModel.toggleStudyMenu() }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showStudyMenu,
                        onDismissRequest = { viewModel.toggleStudyMenu(false) }
                    ) {
                        DropdownMenuItem(
                            text = { Text("专注历史") },
                            onClick = {
                                viewModel.toggleStudyMenu(false)
                                viewModel.showFocusHistoryModal(true)
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("专注设置") },
                            onClick = {
                                viewModel.showTimerSettingsModal(true)
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAddStudyPlanClicked() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(16.dp)
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加学习计划")
            }
        },
        content = { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    PomodoroTimer(
                        timerState = timerState,
                        onTaskSelect = { viewModel.showTaskSelectionModal(true) },
                        onTimerAction = viewModel::onTimerAction
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "学习统计",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stats.forEach { stat ->
                            StatCard(
                                stat = stat,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.selectStat(stat) }
                            )
                        }
                    }
                }
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "今日学习计划",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (studyPlans.isEmpty()) { // 新增：检查列表是否为空
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "暂无学习计划，点击右下角按钮添加",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                studyPlans.forEach { plan ->
                                    StudyPlanCard(
                                        plan = plan,
                                        onClick = { viewModel.selectPlan(plan) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
    selectedPlan?.let { plan ->
        PlanDetailModal(
            plan = plan,
            onDismiss = { viewModel.selectPlan(null) },
            onStartStudy = {
                viewModel.startStudyFromPlan(plan)
            },
            onEdit = { viewModel.onEditPlanClicked(plan) },
            onDelete = { viewModel.onDeletePlanClicked(plan) }
        )
    }
    selectedStat?.let { stat ->
        StatDetailModal(
            stat = stat,
            relevantRecords = selectedStatFilteredRecords,
            onDismiss = { viewModel.selectStat(null) }
        )
    }
    if (showTimerSettings) {
        TimerSettingsModal(
            onDismiss = { viewModel.showTimerSettingsModal(false) },
            onSave = { newTimerSettings ->
                viewModel.saveTimerSettings(newTimerSettings)
            },
            timerState = timerState
        )
    }
    if (showTaskSelection) {
        val availableTasks by viewModel.availableTasks.collectAsStateWithLifecycle()
        TaskSelectionModal(
            tasks = availableTasks,
            onDismiss = { viewModel.showTaskSelectionModal(false) },
            onTaskSelected = { taskTitle ->
                viewModel.selectTaskForTimer(taskTitle)
            },
            onAddNewTask = { viewModel.onAddNewTaskClickedFromStudy() }
        )
    }

    if (showFocusHistory) {
        FocusHistoryModal(
            history = focusHistoryList,
            onDismiss = { viewModel.showFocusHistoryModal(false) }
        )
    }

    if (showAddEditPlanModal) {
        AddEditStudyPlanModal(
            editingPlan = editingPlan,
            onDismiss = { viewModel.onDismissAddEditPlanModal() },
            onSave = { title, startTime, endTime, description, subject, location ->
                viewModel.onSaveStudyPlan(title, startTime, endTime, description, subject, location)
            }
        )
    }

    if (showDeletePlanConfirmation && planToDelete != null) {
        DeleteConfirmationDialog(
            planTitle = planToDelete!!.title,
            onConfirm = { viewModel.confirmDeletePlan() },
            onDismiss = { viewModel.onDismissDeletePlanConfirmation() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusHistoryModal(
    history: List<FocusSessionRecord>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "专注历史记录",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无专注历史记录",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(history.size) { index ->
                        val record = history[index]
                        FocusHistoryItem(record)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun FocusHistoryItem(record: FocusSessionRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.taskName ?: "无特定任务",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = record.timeRangeDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "时长: ${record.durationDisplay}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "状态: ${
                        when (record.sessionStatus) {
                            FocusSessionRecord.SessionStatus.COMPLETED -> "已完成"
                            FocusSessionRecord.SessionStatus.INTERRUPTED -> "已中断"
                            FocusSessionRecord.SessionStatus.RESET -> "已重置"
                        }
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PomodoroTimer(
    timerState: TimerState,
    onTaskSelect: () -> Unit,
    onTimerAction: (TimerControlAction) -> Unit
) {
    val statusText = when {
        timerState.isRunning -> "专注中..."
        timerState.isPaused -> "已暂停"
        timerState.isInitial -> "准备开始"
        timerState.remainingTime == 0 -> "已完成"
        else -> "已停止"
    }
    val elapsedRatio by animateFloatAsState(
        targetValue = if (timerState.isInitial && timerState.remainingTime == timerState.totalTimeSeconds) 0f
        else ((timerState.totalTimeSeconds - timerState.remainingTime).toFloat() / timerState.totalTimeSeconds.toFloat()),
        animationSpec = tween(
            durationMillis = if (timerState.isRunning && timerState.remainingTime > 0) 1000 else 300,
            easing = LinearEasing
        ),
        label = "progressAnimation"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {

                val progressTrackColor = MaterialTheme.colorScheme.primary
                val progressIndicatorColor = MaterialTheme.colorScheme.primaryContainer
                val innerCircleBackgroundColor = MaterialTheme.colorScheme.surface
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasSize = size.minDimension
                    val center = Offset(canvasSize / 2, canvasSize / 2)
                    val radius = canvasSize / 2
                    val innerCircleRadius = 100.dp.toPx()
                    drawCircle(
                        color = progressTrackColor,
                        radius = radius,
                        center = center
                    )
                    if (elapsedRatio > 0f || (!timerState.isInitial && !timerState.isPaused)) {
                        drawArc(
                            color = progressIndicatorColor,
                            startAngle = -90f,
                            sweepAngle = -360f * elapsedRatio.coerceAtLeast(-360f),
                            useCenter = true,
                            topLeft = Offset.Zero,
                            size = Size(canvasSize, canvasSize)
                        )
                    }
                    drawCircle(
                        color = innerCircleBackgroundColor,
                        radius = innerCircleRadius,
                        center = center
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "%02d:%02d".format(
                            timerState.remainingTime / 60,
                            timerState.remainingTime % 60
                        ),
                        style = MaterialTheme.typography.displayMedium,
                        fontSize = 48.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onTaskSelect,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text(
                    text = timerState.selectedTask ?: "选择任务",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Select task"
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val mainButtonText = when {
                    timerState.isRunning -> "暂停"
                    timerState.isPaused -> "继续"
                    timerState.isInitial -> "开始"
                    timerState.remainingTime == 0 -> "重新开始"
                    else -> "开始"
                }
                Button(
                    onClick = {
                        when {
                            timerState.isRunning -> onTimerAction(TimerControlAction.PAUSE)
                            timerState.isPaused -> onTimerAction(TimerControlAction.RESUME)
                            timerState.isInitial -> onTimerAction(TimerControlAction.START_INITIAL)
                            timerState.remainingTime == 0 -> onTimerAction(TimerControlAction.RESTART_COMPLETED)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(text = mainButtonText)
                }
                Button(
                    onClick = {
                        onTimerAction(TimerControlAction.RESET)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    enabled = !timerState.isInitial || timerState.remainingTime != timerState.totalTimeSeconds || timerState.isRunning || timerState.isPaused
                ) {
                    Text("重置")
                }
            }
        }
    }
}

@Composable
fun StatCard(
    stat: StudyStat,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = stat.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stat.displayValue,
                style = MaterialTheme.typography.titleLarge,
                color = when (stat.type) {
                    StatType.DAILY -> MaterialTheme.colorScheme.primary
                    StatType.WEEKLY -> MaterialTheme.colorScheme.secondary
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatDetailModal(
    stat: StudyStat,
    relevantRecords: List<FocusSessionRecord>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (stat.type == StatType.DAILY) "今日专注统计" else "本周专注统计",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stat.displayValue,
                    style = MaterialTheme.typography.displaySmall,
                    color = when (stat.type) {
                        StatType.DAILY -> MaterialTheme.colorScheme.primary
                        StatType.WEEKLY -> MaterialTheme.colorScheme.secondary
                    }
                )
            }
            Text(
                text = "专注记录",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (relevantRecords.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "暂无相关专注记录",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(relevantRecords.size) { index ->
                        val record = relevantRecords[index]
                        FocusHistoryItem(record = record)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StudyPlanCard(plan: StudyPlan, onClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "截止" + plan.timeDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = plan.description.takeIf { !it.isNullOrBlank() } ?: "暂无任务内容",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailModal(
    plan: StudyPlan,
    onDismiss: () -> Unit,
    onStartStudy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            DetailRow(icon = Icons.Default.AccessTime, text = "截止时间: ${plan.timeDisplay}")
            plan.location?.let {
                DetailRow(icon = Icons.Filled.Star, text = "地点: $it")
            }
            DetailRow(icon = Icons.Default.Lightbulb, text = "科目: ${plan.subject}")
            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = "任务内容",
                    modifier = Modifier.padding(top = 4.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "任务内容: ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = plan.description.takeIf { !it.isNullOrBlank() } ?: "暂无任务内容",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = onEdit)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "编辑",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = onStartStudy)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Start",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "开始学习",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = onDelete)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "删除",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DetailRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun DeleteConfirmationDialog(
    planTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认删除") },
        text = { Text("你确定要删除学习计划\"$planTitle\"吗？此操作无法撤销") },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditStudyPlanModal(
    editingPlan: StudyPlan?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        description: String?,
        subject: String,
        location: String?
    ) -> Unit
) {
    val isEditing = editingPlan != null
    var title by remember(editingPlan) { mutableStateOf(editingPlan?.title ?: "") }
    var description by remember(editingPlan) { mutableStateOf(editingPlan?.description ?: "") }
    var subject by remember(editingPlan) { mutableStateOf(editingPlan?.subject ?: "") }
    var location by remember(editingPlan) { mutableStateOf(editingPlan?.location ?: "") }

    var startDate by remember(editingPlan) { mutableStateOf(editingPlan?.startTime?.date) }
    var startTime by remember(editingPlan) { mutableStateOf(editingPlan?.startTime?.time) }
    var endDate by remember(editingPlan) { mutableStateOf(editingPlan?.endTime?.date) }
    var endTime by remember(editingPlan) { mutableStateOf(editingPlan?.endTime?.time) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val startDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = editingPlan?.startTime?.toInstant(TimeZone.currentSystemDefault())
            ?.toEpochMilliseconds(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= Clock.System.now()
                    .toEpochMilliseconds() - (24 * 60 * 60 * 1000)
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year >= Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault()).year - 1
            }
        }
    )
    val startTimePickerState = rememberTimePickerState(
        initialHour = editingPlan?.startTime?.hour ?: LocalTime(0, 0).hour,
        initialMinute = editingPlan?.startTime?.minute ?: LocalTime(0, 0).minute,
        is24Hour = true
    )
    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = editingPlan?.endTime?.toInstant(TimeZone.currentSystemDefault())
            ?.toEpochMilliseconds(),
        selectableDates = startDatePickerState.selectableDates
    )
    val endTimePickerState = rememberTimePickerState(
        initialHour = editingPlan?.endTime?.hour ?: LocalTime(0, 0).hour,
        initialMinute = editingPlan?.endTime?.minute ?: LocalTime(0, 0).minute,
        is24Hour = true
    )

    var titleError by remember { mutableStateOf<String?>(null) }
    var subjectError by remember { mutableStateOf<String?>(null) }
    var startDateTimeError by remember { mutableStateOf<String?>(null) }
    var endDateTimeError by remember { mutableStateOf<String?>(null) }

    fun validateAndSave() {
        titleError = if (title.isBlank()) "标题不能为空" else null
        subjectError = if (subject.isBlank()) "科目不能为空" else null
        startDateTimeError =
            if (startDate == null || startTime == null) "请选择完整的开始日期和时间" else null
        endDateTimeError =
            if (endDate == null || endTime == null) "请选择完整的结束日期和时间" else null

        var finalStartTime: LocalDateTime? = null
        var finalEndTime: LocalDateTime? = null

        if (startDate != null && startTime != null) {
            finalStartTime = startDate!!.atTime(startTime!!)
        }
        if (endDate != null && endTime != null) {
            finalEndTime = endDate!!.atTime(endTime!!)
        }

        if (finalStartTime != null && finalEndTime != null && finalEndTime < finalStartTime) {
            endDateTimeError = (endDateTimeError ?: "") + "结束时间不能早于开始时间。"
        }

        if (titleError == null && subjectError == null && startDateTimeError == null && endDateTimeError == null && finalStartTime != null && finalEndTime != null) {
            onSave(
                title,
                finalStartTime,
                finalEndTime,
                description.ifBlank { null },
                subject,
                location.ifBlank { null })
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isEditing) "编辑学习计划" else "添加学习计划",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; titleError = null },
                label = { Text("标题*") },
                isError = titleError != null,
                supportingText = { titleError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it; subjectError = null },
                label = { Text("科目*") },
                isError = subjectError != null,
                supportingText = { subjectError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            Text("开始时间*", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val startDateButtonEnabled = true
                Button(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = startDateButtonEnabled)
                ) {
                    Text(startDate.formatForDisplay())
                }

                val startTimeButtonEnabled = startDate != null
                Button(
                    onClick = { showStartTimePicker = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = startTimeButtonEnabled),
                    enabled = startDate != null // 只有选了日期才能选时间
                ) {
                    Text(startTime.formatForDisplay())
                }
            }
            if (startDateTimeError != null) {
                Text(
                    startDateTimeError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text("结束时间*", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val endDateButtonEnabled = true
                Button(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = endDateButtonEnabled)
                ) {
                    Text(endDate.formatForDisplay())
                }

                val endTimeButtonEnabled = endDate != null
                Button(
                    onClick = { showEndTimePicker = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = endTimeButtonEnabled),
                    enabled = endDate != null
                ) {
                    Text(endTime.formatForDisplay())
                }
            }
            if (endDateTimeError != null) {
                Text(
                    endDateTimeError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("描述 (可选)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("地点 (可选)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("取消")
                }
                Button(onClick = { validateAndSave() }) {
                    Text(if (isEditing) "保存更改" else "添加计划")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startDatePickerState.selectedDateMillis?.let { millis ->
                            startDate = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.currentSystemDefault()).date
                            startDateTimeError = null
                        }
                        showStartDatePicker = false
                        if (startDate != null && startTime == null) {
                            showStartTimePicker = true
                        }
                    }
                ) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }
    if (showStartTimePicker) {
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("选择开始时间") },
            text = {
                TimePicker(state = startTimePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        startTime =
                            LocalTime(startTimePickerState.hour, startTimePickerState.minute)
                        startDateTimeError = null
                        showStartTimePicker = false
                    }
                ) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text("取消") }
            }
        )
    }
    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        endDatePickerState.selectedDateMillis?.let { millis ->
                            endDate = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.currentSystemDefault()).date
                            endDateTimeError = null
                        }
                        showEndDatePicker = false
                        if (endDate != null && endTime == null) {
                            showEndTimePicker = true
                        }
                    }
                ) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }
    if (showEndTimePicker) {
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("选择结束时间") },
            text = { TimePicker(state = endTimePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        endTime = LocalTime(endTimePickerState.hour, endTimePickerState.minute)
                        endDateTimeError = null
                        showEndTimePicker = false
                    }
                ) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) { Text("取消") }
            }
        )
    }
}

fun LocalDate?.formatForDisplay(): String {
    return this?.toJavaLocalDate()?.format(DateTimeFormatter.ISO_LOCAL_DATE)
        ?: "选择日期"
}

fun LocalTime?.formatForDisplay(): String {
    return this?.toJavaLocalTime()?.format(DateTimeFormatter.ofPattern("HH:mm"))
        ?: "选择时间"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerSettingsModal(
    onDismiss: () -> Unit,
    onSave: (TimerState) -> Unit,
    timerState: TimerState
) {
    var focusDuration by remember { mutableStateOf(timerState.focusDurationMinutes.toString()) }
    var shortBreak by remember { mutableStateOf(timerState.shortBreakMinutes.toString()) }
    var longBreak by remember { mutableStateOf(timerState.longBreakMinutes.toString()) }
    var longBreakInterval by remember { mutableStateOf(timerState.longBreakInterval.toString()) }
    var autoStart by remember { mutableStateOf(timerState.autoStartNextRound) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "专注设置",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NumberInput(
                    label = "专注时长（分钟）",
                    value = focusDuration,
                    onValueChange = { focusDuration = it },
                    min = 1,
                    max = 120
                )
                NumberInput(
                    label = "短休息时长（分钟）",
                    value = shortBreak,
                    onValueChange = { shortBreak = it },
                    min = 1,
                    max = 30
                )
                NumberInput(
                    label = "长休息时长（分钟）",
                    value = longBreak,
                    onValueChange = { longBreak = it },
                    min = 5,
                    max = 60
                )
                NumberInput(
                    label = "长休息间隔（次数）",
                    value = longBreakInterval,
                    onValueChange = { longBreakInterval = it },
                    min = 1,
                    max = 10
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = autoStart,
                        onCheckedChange = { autoStart = it }
                    )
                    Text(
                        text = "启用自动开始下一轮专注/休息",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        val fdStr = focusDuration
                        val sbStr = shortBreak
                        val lbStr = longBreak
                        val lbiStr = longBreakInterval
                        val fdNum = fdStr.toIntOrNull()
                        val sbNum = sbStr.toIntOrNull()
                        val lbNum = lbStr.toIntOrNull()
                        val lbiNum = lbiStr.toIntOrNull()
                        val isFdValid = fdNum != null && fdNum in 1..120
                        val isSbValid = sbNum != null && sbNum in 1..30
                        val isLbValid = lbNum != null && lbNum in 5..60
                        val isLbiValid = lbiNum != null && lbiNum in 1..10
                        if (isFdValid && isSbValid && isLbValid && isLbiValid) {
                            val newSettings = TimerState(
                                focusDurationMinutes = fdNum,
                                shortBreakMinutes = lbNum,
                                longBreakMinutes = lbNum,
                                longBreakInterval = lbiNum,
                                autoStartNextRound = autoStart
                            )
                            onSave(newSettings)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("保存")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun NumberInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    min: Int,
    max: Int
) {
    val currentError: String? = remember(value, min, max) {
        when {
            value.isEmpty() -> "此字段不能为空"
            value.toIntOrNull() == null -> "请输入有效的数字"
            value.toInt() !in min..max -> "请输入 $min~$max 之间的数字"
            else -> null
        }
    }
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                    onValueChange(newValue)
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number
            ),
            isError = currentError != null,
            modifier = Modifier.fillMaxWidth(),
            supportingText = {
                if (currentError != null) {
                    Text(currentError)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSelectionModal(
    tasks: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onTaskSelected: (String) -> Unit,
    onAddNewTask: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "选择任务",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column {
                tasks.forEach { (title, time) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTaskSelected(title) }
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = time,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onAddNewTask() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(8.dp))
                Text("创建新任务")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}