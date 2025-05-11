@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.todoschedule.ui.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.todoschedule.ui.components.DatePickerDialog
import com.example.todoschedule.ui.components.TimePickerDialog
import com.example.todoschedule.ui.theme.ColorSchemeEnum

/**
 * 快速添加日程/课程对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddScheduleDialog(
    onDismiss: () -> Unit,
    viewModel: QuickAddScheduleViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    // --- 实时校验逻辑 ---
    val titleError = if (uiState.title.isBlank()) "标题不能为空" else null
    val dateError = null // 日期一般不为空
    val startTime = uiState.startTime
    val endTime = uiState.endTime
    val startTimeError = if (startTime == null) "请选择开始时间" else null
    val endTimeError =
        if (endTime != null && startTime != null && endTime <= startTime) "结束时间需晚于开始时间" else null
    val weekRangeError =
        if (uiState.selectedCategory == ScheduleCategory.COURSE && uiState.weekRange.isBlank()) "请输入周次范围" else null
    val canSave =
        titleError == null && startTimeError == null && endTimeError == null && weekRangeError == null

    // bringIntoViewRequester + focus 状态
    val titleBringIntoViewRequester = remember { BringIntoViewRequester() }
    val isTitleFocused = remember { mutableStateOf(false) }
    LaunchedEffect(isTitleFocused.value) {
        if (isTitleFocused.value) {
            titleBringIntoViewRequester.bringIntoView()
        }
    }
    val dateBringIntoViewRequester = remember { BringIntoViewRequester() }
    val isDateFocused = remember { mutableStateOf(false) }
    LaunchedEffect(isDateFocused.value) {
        if (isDateFocused.value) {
            dateBringIntoViewRequester.bringIntoView()
        }
    }
    val startTimeBringIntoViewRequester = remember { BringIntoViewRequester() }
    val isStartTimeFocused = remember { mutableStateOf(false) }
    LaunchedEffect(isStartTimeFocused.value) {
        if (isStartTimeFocused.value) {
            startTimeBringIntoViewRequester.bringIntoView()
        }
    }
    val endTimeBringIntoViewRequester = remember { BringIntoViewRequester() }
    val isEndTimeFocused = remember { mutableStateOf(false) }
    LaunchedEffect(isEndTimeFocused.value) {
        if (isEndTimeFocused.value) {
            endTimeBringIntoViewRequester.bringIntoView()
        }
    }
    val locationBringIntoViewRequester = remember { BringIntoViewRequester() }
    val isLocationFocused = remember { mutableStateOf(false) }
    LaunchedEffect(isLocationFocused.value) {
        if (isLocationFocused.value) {
            locationBringIntoViewRequester.bringIntoView()
        }
    }
    val teacherBringIntoViewRequester = remember { BringIntoViewRequester() }
    val isTeacherFocused = remember { mutableStateOf(false) }
    LaunchedEffect(isTeacherFocused.value) {
        if (isTeacherFocused.value) {
            teacherBringIntoViewRequester.bringIntoView()
        }
    }
    val creditBringIntoViewRequester = remember { BringIntoViewRequester() }
    val isCreditFocused = remember { mutableStateOf(false) }
    LaunchedEffect(isCreditFocused.value) {
        if (isCreditFocused.value) {
            creditBringIntoViewRequester.bringIntoView()
        }
    }
    val weekRangeBringIntoViewRequester = remember { BringIntoViewRequester() }
    val isWeekRangeFocused = remember { mutableStateOf(false) }
    LaunchedEffect(isWeekRangeFocused.value) {
        if (isWeekRangeFocused.value) {
            weekRangeBringIntoViewRequester.bringIntoView()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        // 外层 Box 控制整体居中和底部间距
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp), // 增大底部空间，使对话框悬浮更高
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(16.dp),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 64.dp)
                            .nestedScroll(remember { object : NestedScrollConnection {} })
                    ) {
                        // 标题
                        Text(
                            text = "添加日程/课程",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // 类型选择
                        SectionTitle("类型选择")
                        CategorySelector(
                            selectedCategory = uiState.selectedCategory,
                            onCategorySelected = viewModel::onCategoryChange
                        )

                        // 颜色选择
                        SectionTitle("颜色选择")
                        ColorPickerSection(
                            selectedColor = uiState.selectedColor,
                            onColorSelected = viewModel::onColorChange
                        )

                        // 基础信息
                        SectionTitle("基础信息")
                        OutlinedTextField(
                            value = uiState.title,
                            onValueChange = viewModel::onTitleChange,
                            label = { Text("标题") },
                            isError = titleError != null,
                            supportingText = { FieldError(titleError) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewRequester(titleBringIntoViewRequester)
                                .onFocusChanged { isTitleFocused.value = it.isFocused },
                            singleLine = true
                        )

                        // 时间地点
                        SectionTitle("时间与地点")
                        OutlinedTextField(
                            value = uiState.selectedDate.toString(),
                            onValueChange = {},
                            label = { Text("日期") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewRequester(dateBringIntoViewRequester)
                                .onFocusChanged { isDateFocused.value = it.isFocused },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { viewModel.showDatePicker(true) }) {
                                    Icon(Icons.Default.CalendarToday, "选择日期")
                                }
                            }
                        )
                        FieldError(dateError)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = startTime?.toString() ?: "",
                                onValueChange = {},
                                label = { Text("开始时间") },
                                isError = startTimeError != null,
                                supportingText = { FieldError(startTimeError) },
                                modifier = Modifier
                                    .weight(1f)
                                    .bringIntoViewRequester(startTimeBringIntoViewRequester)
                                    .onFocusChanged { isStartTimeFocused.value = it.isFocused },
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.showStartTimePicker(true) }) {
                                        Icon(Icons.Default.Schedule, "选择时间")
                                    }
                                }
                            )
                            OutlinedTextField(
                                value = endTime?.toString() ?: "",
                                onValueChange = {},
                                label = { Text("结束时间") },
                                isError = endTimeError != null,
                                supportingText = { FieldError(endTimeError) },
                                modifier = Modifier
                                    .weight(1f)
                                    .bringIntoViewRequester(endTimeBringIntoViewRequester)
                                    .onFocusChanged { isEndTimeFocused.value = it.isFocused },
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.showEndTimePicker(true) }) {
                                        Icon(Icons.Default.Schedule, "选择时间")
                                    }
                                }
                            )
                        }

                        OutlinedTextField(
                            value = uiState.location,
                            onValueChange = viewModel::onLocationChange,
                            label = { Text("地点") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .bringIntoViewRequester(locationBringIntoViewRequester)
                                .onFocusChanged { isLocationFocused.value = it.isFocused },
                            singleLine = true
                        )

                        // 课程特有字段
                        if (uiState.selectedCategory == ScheduleCategory.COURSE) {
                            SectionTitle("课程信息")
                            OutlinedTextField(
                                value = uiState.teacher,
                                onValueChange = viewModel::onTeacherChange,
                                label = { Text("教师") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bringIntoViewRequester(teacherBringIntoViewRequester)
                                    .onFocusChanged { isTeacherFocused.value = it.isFocused },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = uiState.credit.toString(),
                                onValueChange = { value ->
                                    value.toFloatOrNull()?.let { viewModel.onCreditChange(it) }
                                },
                                label = { Text("学分") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                                    .bringIntoViewRequester(creditBringIntoViewRequester)
                                    .onFocusChanged { isCreditFocused.value = it.isFocused },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = uiState.weekRange,
                                onValueChange = viewModel::onWeekRangeChange,
                                label = { Text("周次范围 (例如: 1-16)") },
                                isError = weekRangeError != null,
                                supportingText = { FieldError(weekRangeError) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                                    .bringIntoViewRequester(weekRangeBringIntoViewRequester)
                                    .onFocusChanged { isWeekRangeFocused.value = it.isFocused },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = uiState.startNode?.toString() ?: "",
                                onValueChange = { value ->
                                    value.toIntOrNull()?.let { viewModel.onStartNodeChange(it) }
                                },
                                label = { Text("开始节次 (如1)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = uiState.step?.toString() ?: "",
                                onValueChange = { value ->
                                    value.toIntOrNull()?.let { viewModel.onStepChange(it) }
                                },
                                label = { Text("节数 (如2)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                singleLine = true
                            )
                        }

                        // 提醒和重复选项
                        SectionTitle("提醒设置")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = uiState.isNotify,
                                    onCheckedChange = viewModel::onNotifyChange
                                )
                                Text("提醒")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = uiState.isRepeat,
                                    onCheckedChange = viewModel::onRepeatChange
                                )
                                Text("重复")
                            }
                        }
                        // 提醒时间自定义
                        NotifyTimeSection(
                            isNotify = uiState.isNotify,
                            notifyTime = uiState.notifyTime,
                            onNotifyTimeChange = viewModel::onNotifyTimeChange
                        )
                        // 重复规则自定义
                        RepeatRuleSection(
                            isRepeat = uiState.isRepeat,
                            repeatRule = uiState.repeatRule,
                            onRepeatRuleChange = viewModel::onRepeatRuleChange
                        )

                        // 全局错误信息
                        uiState.errorMessage?.let { errorMsg ->
                            Text(
                                text = errorMsg,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    // 底部按钮区固定
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.saveSchedule()
                                if (uiState.errorMessage == null && canSave) {
                                    onDismiss()
                                }
                            },
                            enabled = canSave
                        ) {
                            Text("保存")
                        }
                    }
                }
            }
        }

        // 日期选择器
        if (uiState.showDatePicker) {
            DatePickerDialog(
                onDateSelected = viewModel::onDateSelected,
                onDismiss = { viewModel.showDatePicker(false) }
            )
        }

        // 开始时间选择器
        if (uiState.showStartTimePicker) {
            TimePickerDialog(
                onTimeSelected = viewModel::onTimeSelected,
                onDismiss = { viewModel.showStartTimePicker(false) }
            )
        }
        // 结束时间选择器
        if (uiState.showEndTimePicker) {
            TimePickerDialog(
                onTimeSelected = viewModel::onEndTimeSelected,
                onDismiss = { viewModel.showEndTimePicker(false) }
            )
        }
    }
}

/**
 * 类型选择器
 */
@Composable
private fun CategorySelector(
    selectedCategory: ScheduleCategory,
    onCategorySelected: (ScheduleCategory) -> Unit
) {
    Column {
        Text(
            text = "类型",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == ScheduleCategory.COURSE,
                onClick = { onCategorySelected(ScheduleCategory.COURSE) },
                label = { Text("课程") },
                leadingIcon = {
                    if (selectedCategory == ScheduleCategory.COURSE) {
                        Icon(Icons.Default.Check, null)
                    }
                }
            )
            FilterChip(
                selected = selectedCategory == ScheduleCategory.HOMEWORK,
                onClick = { onCategorySelected(ScheduleCategory.HOMEWORK) },
                label = { Text("作业") },
                leadingIcon = {
                    if (selectedCategory == ScheduleCategory.HOMEWORK) {
                        Icon(Icons.Default.Check, null)
                    }
                }
            )
            FilterChip(
                selected = selectedCategory == ScheduleCategory.EXAM,
                onClick = { onCategorySelected(ScheduleCategory.EXAM) },
                label = { Text("考试") },
                leadingIcon = {
                    if (selectedCategory == ScheduleCategory.EXAM) {
                        Icon(Icons.Default.Check, null)
                    }
                }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == ScheduleCategory.ONLINE_CLASS,
                onClick = { onCategorySelected(ScheduleCategory.ONLINE_CLASS) },
                label = { Text("网课") },
                leadingIcon = {
                    if (selectedCategory == ScheduleCategory.ONLINE_CLASS) {
                        Icon(Icons.Default.Check, null)
                    }
                }
            )
            FilterChip(
                selected = selectedCategory == ScheduleCategory.REVIEW,
                onClick = { onCategorySelected(ScheduleCategory.REVIEW) },
                label = { Text("复习") },
                leadingIcon = {
                    if (selectedCategory == ScheduleCategory.REVIEW) {
                        Icon(Icons.Default.Check, null)
                    }
                }
            )
            FilterChip(
                selected = selectedCategory == ScheduleCategory.OTHER,
                onClick = { onCategorySelected(ScheduleCategory.OTHER) },
                label = { Text("其他") },
                leadingIcon = {
                    if (selectedCategory == ScheduleCategory.OTHER) {
                        Icon(Icons.Default.Check, null)
                    }
                }
            )
        }
    }
}

/**
 * 分组标题组件
 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

/**
 * 字段错误提示组件
 */
@Composable
private fun FieldError(text: String?) {
    if (!text.isNullOrBlank()) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
        )
    }
}

/**
 * 颜色选择区块
 */
@Composable
private fun ColorPickerSection(
    selectedColor: ColorSchemeEnum,
    onColorSelected: (ColorSchemeEnum) -> Unit
) {
    val colorList = ColorSchemeEnum.getColorSchemeEnumList()
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        colorList.forEach { colorEnum ->
            val color = when (colorEnum) {
                is ColorSchemeEnum.Fixed -> colorEnum.color
                else -> colorEnum.toColor(MaterialTheme.colorScheme)
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (colorEnum == selectedColor) 3.dp else 1.dp,
                        color = if (colorEnum == selectedColor) Color.Black else Color.Gray,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(colorEnum) }
            )
        }
    }
}

/**
 * 提醒时间自定义区块
 */
@Composable
private fun NotifyTimeSection(
    isNotify: Boolean,
    notifyTime: Int,
    onNotifyTimeChange: (Int) -> Unit
) {
    val options = listOf(0, 5, 10, 30, 60) // 单位：分钟
    AnimatedVisibility(visible = isNotify) {
        Row(modifier = Modifier.padding(top = 8.dp)) {
            Text("提醒时间：")
            options.forEach { min ->
                FilterChip(
                    selected = notifyTime == min,
                    onClick = { onNotifyTimeChange(min) },
                    label = { Text(if (min == 0) "准时" else "提前${min}分钟") }
                )
            }
        }
    }
}

/**
 * 重复规则自定义区块
 */
@Composable
private fun RepeatRuleSection(
    isRepeat: Boolean,
    repeatRule: String,
    onRepeatRuleChange: (String) -> Unit
) {
    val rules = listOf("每天", "每周", "每月", "工作日", "自定义")
    AnimatedVisibility(visible = isRepeat) {
        Row(modifier = Modifier.padding(top = 8.dp)) {
            Text("重复：")
            rules.forEach { rule ->
                FilterChip(
                    selected = repeatRule == rule,
                    onClick = { onRepeatRuleChange(rule) },
                    label = { Text(rule) }
                )
            }
        }
    }
} 