package com.example.todoschedule.ui.ordinaryschedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.data.database.converter.ReminderType
import com.example.todoschedule.data.database.converter.ScheduleStatus
import com.example.todoschedule.domain.model.OrdinarySchedule
import com.example.todoschedule.domain.model.TimeSlot
import com.example.todoschedule.ui.navigation.NavigationState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// TODO: Create OrdinaryScheduleDetailViewModel to fetch details

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdinaryScheduleDetailScreen(
    // scheduleId is implicitly handled by the ViewModel via SavedStateHandle
    navigationState: NavigationState,
    viewModel: OrdinaryScheduleDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // 控制删除确认对话框的显示状态
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    // 监听 ViewModel 事件
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is OrdinaryScheduleDetailEvent.NavigateBack -> {
                    navigationState.navigateBack()
                }

                is OrdinaryScheduleDetailEvent.ShowError -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(event.message)
                    }
                    // 如果错误是因为找不到日程，可能也需要返回
                    if (event.message.contains("未找到 ID")) {
                        navigationState.navigateBack()
                    }
                }
            }
        }
    }

    // 获取状态颜色
    val statusColor = getStatusColor(uiState)
    val onStatusColor = getOnStatusColor(statusColor)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // 添加 SnackbarHost
        topBar = {
            TopAppBar(
                title = {
                    val titleText = when (val state = uiState) {
                        is OrdinaryScheduleDetailUiState.Success -> state.schedule.title
                        else -> "日程详情"
                    }
                    Text(
                        text = titleText,
                        color = onStatusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigationState.navigateBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = onStatusColor
                        )
                    }
                },
                actions = {
                    if (uiState is OrdinaryScheduleDetailUiState.Success) {
                        // 编辑按钮
                        IconButton(onClick = {
                            val scheduleId =
                                (uiState as OrdinaryScheduleDetailUiState.Success).schedule.id
                            navigationState.navigateToAddEditOrdinarySchedule(scheduleId)
                        }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "编辑",
                                tint = onStatusColor
                            )
                        }
                        // 删除按钮
                        IconButton(onClick = {
                            showDeleteConfirmationDialog = true /* 显示确认对话框 */
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = onStatusColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = statusColor
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopStart
        ) {
            when (val state = uiState) {
                is OrdinaryScheduleDetailUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is OrdinaryScheduleDetailUiState.Success -> {
                    ScheduleDetailsContent(
                        schedule = state.schedule,
                        statusColor = statusColor
                    )
                }

                is OrdinaryScheduleDetailUiState.Error -> {
                    // 错误状态已通过 Snackbar 显示，这里可以留空或显示通用错误提示
                    // ErrorContent(
                    //     message = state.message,
                    //     modifier = Modifier.align(Alignment.Center)
                    // )
                }
            }

            // 删除确认对话框
            if (showDeleteConfirmationDialog) {
                DeleteConfirmationDialog(
                    onConfirm = {
                        viewModel.deleteSchedule()
                        showDeleteConfirmationDialog = false
                    },
                    onDismiss = { showDeleteConfirmationDialog = false }
                )
            }
        }
    }
}

// 提取获取状态颜色的逻辑
@Composable
private fun getStatusColor(uiState: OrdinaryScheduleDetailUiState): Color {
    return when {
        uiState is OrdinaryScheduleDetailUiState.Success -> {
            val schedule = uiState.schedule
            val color = schedule.color?.toColor(MaterialTheme.colorScheme)
            when {
                color != null -> color
                schedule.status == ScheduleStatus.TODO -> MaterialTheme.colorScheme.tertiary
                schedule.status == ScheduleStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                schedule.status == ScheduleStatus.DONE -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.surface
            }
        }

        else -> MaterialTheme.colorScheme.surface
    }
}

// 提取获取状态栏前景色的逻辑
@Composable
private fun getOnStatusColor(statusColor: Color): Color {
    return if (statusColor == MaterialTheme.colorScheme.surface) {
        MaterialTheme.colorScheme.onSurface
    } else if (statusColor.luminance() > 0.5f) {
        Color.Black.copy(alpha = 0.87f) // 浅色背景用深色文字
    } else {
        Color.White // 深色背景用浅色文字
    }
}

@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认删除") },
        text = { Text("确定要删除这个日程吗？此操作无法撤销。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ErrorContent(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PriorityHigh,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ScheduleDetailsContent(
    schedule: OrdinarySchedule,
    statusColor: Color
) {
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
    }

    val colorScheme = MaterialTheme.colorScheme

    // 计算完成进度
    val completedTimeSlots = schedule.timeSlots.count { it.isCompleted }
    val totalTimeSlots = schedule.timeSlots.size
    val completionProgress = if (totalTimeSlots > 0) {
        completedTimeSlots.toFloat() / totalTimeSlots
    } else {
        0f
    }

    // 使用动画进度
    val animatedProgress by animateFloatAsState(
        targetValue = completionProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "progressAnimation"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 头部卡片
        item {
            HeadingCard(
                schedule = schedule,
                statusColor = statusColor,
                colorScheme = colorScheme
            )
        }

        // 完成进度指示器（仅在有时间段时显示）
        if (totalTimeSlots > 0) {
            item {
                ProgressCard(
                    completedTimeSlots = completedTimeSlots,
                    totalTimeSlots = totalTimeSlots,
                    progress = animatedProgress
                )
            }
        }

        // 描述部分
        if (!schedule.description.isNullOrBlank()) {
            item {
                SectionCard(title = "描述") {
                    Text(
                        text = schedule.description,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // 详细信息卡片
        item {
            DetailsCard(schedule = schedule)
        }

        // 时间段部分
        if (schedule.timeSlots.isNotEmpty()) {
            item {
                TimeSlotsSectionCard(
                    timeSlots = schedule.timeSlots,
                    dateTimeFormatter = dateTimeFormatter
                )
            }
        }

        // 底部间距
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeadingCard(
    schedule: OrdinarySchedule,
    statusColor: Color,
    colorScheme: androidx.compose.material3.ColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 状态栏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(statusColor)
            )

            // 标题和状态
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = schedule.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // 颜色标识
                    schedule.color?.let { colorEnum ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    color = colorEnum.toColor(colorScheme)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 状态标签
                Surface(
                    color = when (schedule.status) {
                        ScheduleStatus.TODO -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                        ScheduleStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ScheduleStatus.DONE -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (schedule.status) {
                                        ScheduleStatus.TODO -> MaterialTheme.colorScheme.tertiary
                                        ScheduleStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                                        ScheduleStatus.DONE -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.outline
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (schedule.status) {
                                ScheduleStatus.TODO -> "待办"
                                ScheduleStatus.IN_PROGRESS -> "进行中"
                                ScheduleStatus.DONE -> "已完成"
                                else -> "未知状态"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = when (schedule.status) {
                                ScheduleStatus.TODO -> MaterialTheme.colorScheme.tertiary
                                ScheduleStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                                ScheduleStatus.DONE -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(
    completedTimeSlots: Int,
    totalTimeSlots: Int,
    progress: Float
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "完成进度",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "已完成 $completedTimeSlots 个时间段 (共 $totalTimeSlots 个)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailsCard(schedule: OrdinarySchedule) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "基本信息",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // 地点信息
            if (!schedule.location.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = schedule.location,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // 类别信息
            if (!schedule.category.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = schedule.category,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // 全天事件
            if (schedule.isAllDay) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WatchLater,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "全天事件",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeSlotsSectionCard(
    timeSlots: List<TimeSlot>,
    dateTimeFormatter: DateTimeFormatter
) {
    var expanded by remember { mutableStateOf(true) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 可折叠标题行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "时间段 (${timeSlots.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // 可展开/折叠内容
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    timeSlots.forEachIndexed { index, timeSlot ->
                        TimeSlotItem(
                            timeSlot = timeSlot,
                            dateTimeFormatter = dateTimeFormatter
                        )

                        // 分隔线（除了最后一项）
                        if (index < timeSlots.size - 1) {
                            Divider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeSlotItem(
    timeSlot: TimeSlot,
    dateTimeFormatter: DateTimeFormatter
) {
    val startInstant = Instant.ofEpochMilli(timeSlot.startTime)
    val endInstant = Instant.ofEpochMilli(timeSlot.endTime)

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // 主要信息（始终显示）
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp)
        ) {
            // 完成状态指示
            Box(
                modifier = Modifier
                    .size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (timeSlot.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (timeSlot.isCompleted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 标题/头部（如果有）
                if (!timeSlot.head.isNullOrBlank()) {
                    Text(
                        text = timeSlot.head,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // 时间
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${dateTimeFormatter.format(startInstant)} → ${
                            dateTimeFormatter.format(
                                endInstant
                            )
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 展开/折叠按钮
            if (timeSlot.priority != null || timeSlot.reminderType != null || timeSlot.isRepeated) {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 扩展信息（仅在展开时显示）
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 4.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 优先级
                timeSlot.priority?.let { priority ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PriorityHigh,
                            contentDescription = null,
                            tint = when (priority) {
                                1 -> MaterialTheme.colorScheme.tertiary
                                2 -> MaterialTheme.colorScheme.primary
                                3 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.outline
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "优先级: ${
                                when (priority) {
                                    1 -> "低"
                                    2 -> "中"
                                    3 -> "高"
                                    else -> "默认"
                                }
                            }",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // 提醒信息
                if (timeSlot.reminderType != null && timeSlot.reminderType != ReminderType.NONE) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (timeSlot.reminderType) {
                                ReminderType.NOTIFICATION -> Icons.Default.Notifications
                                ReminderType.ALARM -> Icons.Default.Alarm
                                else -> Icons.Default.NotificationsNone
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "提醒方式: ${
                                when (timeSlot.reminderType) {
                                    ReminderType.NOTIFICATION -> "通知提醒"
                                    ReminderType.ALARM -> "闹钟提醒"
                                    else -> "无提醒"
                                }
                            }${
                                timeSlot.reminderOffset?.let {
                                    " (提前 ${it / 60000} 分钟)"
                                } ?: ""
                            }",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // 重复信息
                if (timeSlot.isRepeated) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "重复: ${timeSlot.repeatPattern ?: "未知重复模式"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            content()
        }
    }
} 