package com.example.todoschedule.ui.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuBoxScope.menuAnchor
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.ui.navigation.NavigationState

/**
 * 添加课程页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseScreen(
    tableId: Int,
    navigationState: NavigationState,
    viewModel: AddCourseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加课程") },
                navigationIcon = {
                    IconButton(onClick = { navigationState.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    viewModel.saveCourse()
                    navigationState.navigateBack() 
                }
            ) {
                Icon(Icons.Default.Check, contentDescription = "保存")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 课程名称输入
            OutlinedTextField(
                value = uiState.courseName,
                onValueChange = { viewModel.updateCourseName(it) },
                label = { Text("课程名称") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                }
            )
            
            // 教师名称输入
            OutlinedTextField(
                value = uiState.teacherName,
                onValueChange = { viewModel.updateTeacherName(it) },
                label = { Text("教师姓名") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                }
            )
            
            // 教室位置输入
            OutlinedTextField(
                value = uiState.location,
                onValueChange = { viewModel.updateLocation(it) },
                label = { Text("教室位置") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                }
            )
            
            // 课程时间设置
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "上课时间",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    // 周次选择
                    WeekSelector(
                        selectedWeeks = uiState.selectedWeeks,
                        onWeekSelected = { week, selected ->
                            viewModel.updateSelectedWeek(week, selected)
                        }
                    )
                    
                    // 星期选择
                    DayOfWeekSelector(
                        selectedDay = uiState.dayOfWeek,
                        onDaySelected = { viewModel.updateDayOfWeek(it) }
                    )
                    
                    // 开始结束节数
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StartEndPeriodSelector(
                            startPeriod = uiState.startPeriod,
                            endPeriod = uiState.endPeriod,
                            onStartPeriodChanged = { viewModel.updateStartPeriod(it) },
                            onEndPeriodChanged = { viewModel.updateEndPeriod(it) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 周次选择器
 */
@Composable
fun WeekSelector(
    selectedWeeks: List<Int>,
    onWeekSelected: (Int, Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "选择周次",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            WeekRangeButton(text = "1-4周", onClick = {
                (1..4).forEach { onWeekSelected(it, true) }
            })
            
            WeekRangeButton(text = "5-8周", onClick = {
                (5..8).forEach { onWeekSelected(it, true) }
            })
            
            WeekRangeButton(text = "9-12周", onClick = {
                (9..12).forEach { onWeekSelected(it, true) }
            })
            
            WeekRangeButton(text = "13-16周", onClick = {
                (13..16).forEach { onWeekSelected(it, true) }
            })
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 单周选择
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 1..8) {
                WeekCheckItem(
                    week = i,
                    isSelected = selectedWeeks.contains(i),
                    onCheckedChange = { selected -> onWeekSelected(i, selected) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 9..16) {
                WeekCheckItem(
                    week = i,
                    isSelected = selectedWeeks.contains(i),
                    onCheckedChange = { selected -> onWeekSelected(i, selected) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 周次范围按钮
 */
@Composable
fun WeekRangeButton(
    text: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
    ) {
        Text(text)
    }
}

/**
 * 周次选择项
 */
@Composable
fun WeekCheckItem(
    week: Int,
    isSelected: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(36.dp),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$week",
                style = MaterialTheme.typography.bodySmall
            )
            
            Checkbox(
                checked = isSelected,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

/**
 * 星期选择器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayOfWeekSelector(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit
) {
    val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "选择星期",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = days[selectedDay - 1],
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                days.forEachIndexed { index, day ->
                    DropdownMenuItem(
                        text = { Text(day) },
                        onClick = {
                            onDaySelected(index + 1)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * 开始结束节数选择器
 */
@Composable
fun StartEndPeriodSelector(
    startPeriod: Int,
    endPeriod: Int,
    onStartPeriodChanged: (Int) -> Unit,
    onEndPeriodChanged: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 开始节数
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "开始节数",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            OutlinedTextField(
                value = startPeriod.toString(),
                onValueChange = { 
                    val value = it.toIntOrNull() ?: return@OutlinedTextField
                    if (value in 1..12) onStartPeriodChanged(value)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // 结束节数
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "结束节数",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            OutlinedTextField(
                value = endPeriod.toString(),
                onValueChange = { 
                    val value = it.toIntOrNull() ?: return@OutlinedTextField
                    if (value in startPeriod..12) onEndPeriodChanged(value)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
} 