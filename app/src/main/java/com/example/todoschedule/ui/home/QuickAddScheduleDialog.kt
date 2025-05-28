@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.todoschedule.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
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
                .padding(bottom = 120.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 96.dp)
                            .nestedScroll(remember { object : NestedScrollConnection {} })
                    ) {
                        // 类型选择区
                        GroupTitle("类型选择")
                        GroupCard {
                            CategorySelector(
                                selectedCategory = uiState.selectedCategory,
                                onCategorySelected = viewModel::onCategoryChange
                            )
                        }
                        // 颜色选择区
                        GroupTitle("颜色选择")
                        GroupCard {
                            ColorPickerSection(
                                selectedColor = uiState.selectedColor,
                                onColorSelected = viewModel::onColorChange
                            )
                        }
                        // 基础信息
                        GroupTitle("基础信息")
                        GroupCard {
                            OutlinedTextField(
                                value = uiState.title,
                                onValueChange = viewModel::onTitleChange,
                                label = { Text("标题", fontWeight = FontWeight.Medium) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bringIntoViewRequester(titleBringIntoViewRequester)
                                    .onFocusChanged { isTitleFocused.value = it.isFocused },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    errorBorderColor = MaterialTheme.colorScheme.error
                                ),
                                isError = titleError != null,
                                supportingText = { FieldError(titleError) }
                            )
                            // 仅普通日程类型显示详情输入框
                            if (uiState.selectedCategory == ScheduleCategory.ORDiNARY) {
                                OutlinedTextField(
                                    value = uiState.detail,
                                    onValueChange = viewModel::onDetailChange,
                                    label = { Text("详情", fontWeight = FontWeight.Medium) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    singleLine = false,
                                    maxLines = 3,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                            }
                        }
                        // 时间与地点
                        GroupTitle("时间与地点")
                        GroupCard {
                            OutlinedTextField(
                                value = uiState.selectedDate.toString(),
                                onValueChange = {},
                                label = { Text("日期", fontWeight = FontWeight.Medium) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bringIntoViewRequester(dateBringIntoViewRequester)
                                    .onFocusChanged { isDateFocused.value = it.isFocused },
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.showDatePicker(true) }) {
                                        Icon(Icons.Default.CalendarToday, "选择日期")
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                            FieldError(dateError)
                        }
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
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
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
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
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
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        // 课程信息
                        if (uiState.selectedCategory == ScheduleCategory.COURSE) {
                            GroupTitle("课程信息")
                            GroupCard {
                                OutlinedTextField(
                                    value = uiState.teacher,
                                    onValueChange = viewModel::onTeacherChange,
                                    label = { Text("教师", fontWeight = FontWeight.Medium) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .bringIntoViewRequester(teacherBringIntoViewRequester)
                                        .onFocusChanged { isTeacherFocused.value = it.isFocused },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                                OutlinedTextField(
                                    value = uiState.credit.toString(),
                                    onValueChange = { value ->
                                        value.toFloatOrNull()?.let { viewModel.onCreditChange(it) }
                                    },
                                    label = { Text("学分", fontWeight = FontWeight.Medium) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                        .bringIntoViewRequester(creditBringIntoViewRequester)
                                        .onFocusChanged { isCreditFocused.value = it.isFocused },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                                OutlinedTextField(
                                    value = uiState.weekRange,
                                    onValueChange = viewModel::onWeekRangeChange,
                                    label = {
                                        Text(
                                            "周次范围 (如1-16)",
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    isError = weekRangeError != null,
                                    supportingText = { FieldError(weekRangeError) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                        .bringIntoViewRequester(weekRangeBringIntoViewRequester)
                                        .onFocusChanged { isWeekRangeFocused.value = it.isFocused },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                                // 节次输入优化：下拉选择+即时校验（使用 ExposedDropdownMenuBox）
                                val nodeOptions = (1..12).toList()
                                val stepOptions = (1..6).toList()
                                val startNodeError =
                                    if (uiState.startNode == null) "请选择开始节次" else null
                                val stepError = if (uiState.step == null) "请选择节数" else null
                                // 开始节次
                                var startNodeExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = startNodeExpanded,
                                    onExpandedChange = { startNodeExpanded = !startNodeExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = uiState.startNode?.toString() ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = {
                                            Text(
                                                "开始节次",
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(
                                                expanded = startNodeExpanded
                                            )
                                        },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        isError = startNodeError != null,
                                        supportingText = { FieldError(startNodeError) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                            errorBorderColor = MaterialTheme.colorScheme.error
                                        )
                                    )
                                    ExposedDropdownMenu(
                                        expanded = startNodeExpanded,
                                        onDismissRequest = { startNodeExpanded = false }
                                    ) {
                                        nodeOptions.forEach { node ->
                                            DropdownMenuItem(
                                                text = { Text("第${node}节") },
                                                onClick = {
                                                    viewModel.onStartNodeChange(node)
                                                    startNodeExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                // 节数
                                var stepExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = stepExpanded,
                                    onExpandedChange = { stepExpanded = !stepExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = uiState.step?.toString() ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("节数", fontWeight = FontWeight.Medium) },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(
                                                expanded = stepExpanded
                                            )
                                        },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                            .padding(top = 16.dp),
                                        isError = stepError != null,
                                        supportingText = { FieldError(stepError) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                            errorBorderColor = MaterialTheme.colorScheme.error
                                        )
                                    )
                                    ExposedDropdownMenu(
                                        expanded = stepExpanded,
                                        onDismissRequest = { stepExpanded = false }
                                    ) {
                                        stepOptions.forEach { step ->
                                            DropdownMenuItem(
                                                text = { Text("${step}节") },
                                                onClick = {
                                                    viewModel.onStepChange(step)
                                                    stepExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 提醒和重复选项
                        GroupTitle("提醒设置")
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
                        // 提醒时间自定义（优化版，支持更多提醒方式和美化UI）
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
                    }
                    // 底部按钮区美化
                    Surface(
                        tonalElevation = 8.dp,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) { Text("取消", color = MaterialTheme.colorScheme.primary) }
                            Button(
                                onClick = { viewModel.saveSchedule(); if (uiState.errorMessage == null && canSave) onDismiss() },
                                shape = RoundedCornerShape(24.dp),
                                enabled = canSave
                            ) { Text("保存") }
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
 * 类型选择区（横向滚动美化）
 */
@Composable
private fun CategorySelector(
    selectedCategory: ScheduleCategory,
    onCategorySelected: (ScheduleCategory) -> Unit
) {
    val categories = listOf(
        ScheduleCategory.COURSE to "课程",
        ScheduleCategory.HOMEWORK to "作业",
        ScheduleCategory.EXAM to "考试",
        ScheduleCategory.ONLINE_CLASS to "网课",
        ScheduleCategory.REVIEW to "复习",
        ScheduleCategory.ORDiNARY to "普通日程"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { (type, label) ->
            FilterChip(
                selected = selectedCategory == type,
                onClick = { onCategorySelected(type) },
                label = {
                    Text(
                        label,
                        fontWeight = if (selectedCategory == type) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

/**
 * 分组标题（加粗）
 */
@Composable
private fun GroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
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
 * 颜色选择区块（调色盘样式，支持丰富颜色选择）
 */
@Composable
private fun ColorPickerSection(
    selectedColor: ColorSchemeEnum,
    onColorSelected: (ColorSchemeEnum) -> Unit
) {
    // 仅展示常用且显示效果好的主题色
    val colorList = listOf(
        ColorSchemeEnum.PRIMARY, // 主题主色
        ColorSchemeEnum.SECONDARY, // 辅助色
        ColorSchemeEnum.TERTIARY, // 第三色
        ColorSchemeEnum.ERROR, // 错误色
        ColorSchemeEnum.SURFACE, // 表面色
        ColorSchemeEnum.SURFACEVARIANT, // 表面变体
        ColorSchemeEnum.OUTLINE // 轮廓色
    )
    val columns = 4
    val rows = (colorList.size + columns - 1) / columns
    var showColorPicker by remember { mutableStateOf(false) }
    var selectedCustomColor by remember { mutableStateOf<Color?>(null) }
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        // 调色盘网格
        for (row in 0 until rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    if (index < colorList.size) {
                        val colorEnum = colorList[index]
                        val color = colorEnum.toColor(MaterialTheme.colorScheme)
                        // 圆形色块，选中高亮
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColor == colorEnum) 4.dp else 2.dp,
                                    color = if (selectedColor == colorEnum) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { onColorSelected(colorEnum) }
                        )
                    } else {
                        // 占位，保持网格对齐
                        Box(modifier = Modifier.size(36.dp))
                    }
                }
            }
        }
        // "自定义"按钮，弹出色板
        Row(modifier = Modifier.padding(top = 4.dp)) {
            IconButton(
                onClick = { showColorPicker = true },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, Color.LightGray, CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "自定义颜色",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "自定义颜色",
                modifier = Modifier.align(Alignment.CenterVertically),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    // 弹出自定义色板，支持更细致的色彩选择
    if (showColorPicker) {
        val hueSteps = 12
        val lightnessSteps = 6
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("调色盘选择") },
            text = {
                Column {
                    Text("点击选择任意颜色：")
                    Column {
                        for (l in 0 until lightnessSteps) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (h in 0 until hueSteps) {
                                    val color = Color.hsl(
                                        h * 360f / hueSteps,
                                        0.7f,
                                        0.35f + l * 0.1f // 明度递增
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (selectedCustomColor == color) 3.dp else 1.dp,
                                                color = if (selectedCustomColor == color) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedCustomColor = color }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedCustomColor?.let {
                        onColorSelected(ColorSchemeEnum.Fixed(it))
                    }
                    showColorPicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showColorPicker = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 提醒时间自定义区块（优化版，支持更多提醒方式和美化UI）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotifyTimeSection(
    isNotify: Boolean,
    notifyTime: Int,
    onNotifyTimeChange: (Int) -> Unit
) {
    // 预设提醒选项，单位：分钟，-1为前一天，-2为整点
    val options = listOf(0, 5, 10, 30, 60, 120, 1440, -1, -2)
    val optionLabels = mapOf(
        0 to "准时",
        5 to "提前5分钟",
        10 to "提前10分钟",
        30 to "提前30分钟",
        60 to "提前1小时",
        120 to "提前2小时",
        1440 to "提前1天",
        -1 to "前一天晚上8点",
        -2 to "整点提醒"
    )
    var showCustomDialog by remember { mutableStateOf(false) }
    var customMinute by remember { mutableStateOf(15) }
    AnimatedVisibility(visible = isNotify) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Text(
                "提醒方式：",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                options.forEach { min ->
                    FilterChip(
                        selected = notifyTime == min,
                        onClick = { onNotifyTimeChange(min) },
                        label = { Text(optionLabels[min] ?: "提前${min}分钟") }
                    )
                }
                // 自定义分钟数
                FilterChip(
                    selected = notifyTime > 0 && notifyTime !in options,
                    onClick = { showCustomDialog = true },
                    label = { Text("自定义") }
                )
            }
        }
    }
    // 自定义分钟数弹窗
    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("自定义提醒时间") },
            text = {
                Column {
                    Text("请输入提前提醒的分钟数：")
                    OutlinedTextField(
                        value = customMinute.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { customMinute = it } },
                        label = { Text("分钟数") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (customMinute > 0) onNotifyTimeChange(customMinute)
                    showCustomDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text("取消") }
            }
        )
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

/** 分组区块美化 */
@Composable
private fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
} 