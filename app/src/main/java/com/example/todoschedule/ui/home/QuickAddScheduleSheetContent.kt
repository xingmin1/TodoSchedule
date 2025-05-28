package com.example.todoschedule.ui.home

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.ui.ordinaryschedule.TimePickerDialog
import com.example.todoschedule.ui.ordinaryschedule.formatDate
import com.example.todoschedule.ui.ordinaryschedule.formatTime
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/**
 * 快速添加日程/课程的底部弹出内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddScheduleSheetContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuickAddScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // 错误提示
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { errorMessage ->
            viewModel.consumeErrorMessage()
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "添加日程/课程",
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "关闭")
            }
        }

        // 分类标签页
        var selectedTabIndex by remember { mutableIntStateOf(0) }
        TabRow(selectedTabIndex = selectedTabIndex) {
            ScheduleCategory.values().forEachIndexed { index, category ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = {
                        selectedTabIndex = index
                        viewModel.onCategoryChange(category)
                    },
                    text = {
                        Text(
                            when (category) {
                                ScheduleCategory.COURSE -> "课程"
                                ScheduleCategory.HOMEWORK -> "作业"
                                ScheduleCategory.EXAM -> "考试"
                                ScheduleCategory.ONLINE_CLASS -> "网课"
                                ScheduleCategory.REVIEW -> "复习"
                                ScheduleCategory.ORDiNARY -> "普通日程"
                            }
                        )
                    }
                )
            }
        }

        // 输入区域
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题输入
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        when (uiState.selectedCategory) {
                            ScheduleCategory.COURSE -> "输入课程名称"
                            ScheduleCategory.HOMEWORK -> "输入作业内容"
                            ScheduleCategory.EXAM -> "输入考试科目"
                            ScheduleCategory.ONLINE_CLASS -> "输入网课名称"
                            ScheduleCategory.REVIEW -> "输入复习内容"
                            ScheduleCategory.ORDiNARY -> "记些重要的事情吧~"
                        }
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // 日期和时间选择
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 日期选择
                OutlinedButton(
                    onClick = { viewModel.showDatePicker(true) },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "选择日期",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = uiState.selectedDate.formatDate(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // 时间选择
                OutlinedButton(
                    onClick = { viewModel.showStartTimePicker(true) },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = "选择时间",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = uiState.startTime?.formatTime() ?: "时间",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // 根据选中的类别显示不同的输入字段
            when (uiState.selectedCategory) {
                ScheduleCategory.COURSE -> {
                    // 课程特有字段
                    OutlinedTextField(
                        value = uiState.location,
                        onValueChange = viewModel::onLocationChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("上课地点") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.teacher,
                        onValueChange = viewModel::onTeacherChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("任课教师") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = if (uiState.credit > 0) uiState.credit.toString() else "",
                        onValueChange = {
                            viewModel.onCreditChange(it.toFloatOrNull() ?: 0f)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("学分") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                    OutlinedTextField(
                        value = uiState.weekRange,
                        onValueChange = viewModel::onWeekRangeChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("周次范围 (例如: 1-16)") },
                        singleLine = true
                    )
                }

                ScheduleCategory.HOMEWORK, ScheduleCategory.EXAM -> {
                    // 作业和考试特有字段
                    OutlinedTextField(
                        value = uiState.location,
                        onValueChange = viewModel::onLocationChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("地点") },
                        singleLine = true
                    )
                }

                else -> {
                    // 其他类型的通用字段
                    if (uiState.selectedCategory != ScheduleCategory.ORDiNARY) {
                        OutlinedTextField(
                            value = uiState.location,
                            onValueChange = viewModel::onLocationChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("地点") },
                            singleLine = true
                        )
                    }
                }
            }

            // 提醒和重复选项
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = uiState.isNotify,
                        onCheckedChange = viewModel::onNotifyChange
                    )
                    Text("提醒")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = uiState.isRepeat,
                        onCheckedChange = viewModel::onRepeatChange
                    )
                    Text("重复")
                }
            }
        }

        // 保存按钮
        FilledTonalButton(
            onClick = {
                viewModel.saveSchedule()
                if (uiState.errorMessage == null) {
                    onDismiss()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("保存")
        }
    }

    // 日期选择器对话框
    if (uiState.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDate.atStartOfDayIn(
                kotlinx.datetime.TimeZone.currentSystemDefault()
            ).toEpochMilliseconds()
        )

        DatePickerDialog(
            onDismissRequest = { viewModel.showDatePicker(false) },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.onDateSelected(
                            kotlinx.datetime.Instant.fromEpochMilliseconds(it)
                                .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
                        )
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDatePicker(false) }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // 时间选择器对话框
    if (uiState.showStartTimePicker) {
        val timePickerState = rememberTimePickerState()

        TimePickerDialog(
            onDismissRequest = { viewModel.showStartTimePicker(false) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onTimeSelected(
                        kotlinx.datetime.LocalTime(
                            timePickerState.hour,
                            timePickerState.minute
                        )
                    )
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showStartTimePicker(false) }) { Text("取消") }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickAddScheduleSheetContentPreview() {
    MaterialTheme {
        QuickAddScheduleSheetContent(
            onDismiss = {}
        )
    }
}
