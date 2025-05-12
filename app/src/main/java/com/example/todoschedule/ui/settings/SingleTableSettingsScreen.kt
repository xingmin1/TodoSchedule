package com.example.todoschedule.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.ui.navigation.NavigationState
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * 单个课表设置屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleTableSettingsScreen(
    navigationState: NavigationState,
    viewModel: SingleTableSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // 显示成功消息
    LaunchedEffect(uiState.success) {
        uiState.success?.let { successMessage ->
            scope.launch {
                snackbarHostState.showSnackbar(successMessage)
                viewModel.clearSuccessMessage()
            }
        }
    }
    
    // 显示错误消息
    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMessage ->
            scope.launch {
                snackbarHostState.showSnackbar(errorMessage)
                viewModel.clearErrorMessage()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("课表设置") },
                navigationIcon = {
                    IconButton(onClick = { navigationState.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading) {
                FloatingActionButton(
                    onClick = {
                        viewModel.saveTableSettings {
                            // 保存成功后的回调
                        }
                    }
                ) {
                    Icon(Icons.Default.Save, contentDescription = "保存")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                // 显示加载指示器
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.table == null) {
                // 显示错误信息
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "无法加载课表信息",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                // 显示表单
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // 课表基本信息卡片
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "基本信息",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            Text(
                                text = "课表名称",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.table?.tableName ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 开学日期设置卡片
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "开学日期设置",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            StartDatePicker(
                                currentDate = uiState.startDate,
                                onDateSelected = { newDate ->
                                    viewModel.updateStartDate(newDate)
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 总周数设置卡片
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "学期周数设置",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            TotalWeeksSlider(
                                currentWeeks = uiState.totalWeeks,
                                onWeeksChange = { weeks ->
                                    viewModel.updateTotalWeeks(weeks)
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 保存按钮
                    Button(
                        onClick = {
                            viewModel.saveTableSettings {
                                // 保存成功后的回调
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("保存设置")
                        }
                    }
                    
                    // 底部间距
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

/**
 * 开学日期选择器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartDatePicker(
    currentDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val formattedDate = currentDate?.let { formatDate(it) } ?: "未设置"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "当前开学日期",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        
        IconButton(onClick = { showDatePicker = true }) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "选择日期",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
    
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentDate?.toJavaLocalDate()?.atStartOfDay()
                ?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val instant = java.time.Instant.ofEpochMilli(millis)
                            val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                            onDateSelected(localDate.let { 
                                LocalDate(it.year, it.monthValue, it.dayOfMonth)
                            })
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * 总周数滑块
 */
@Composable
private fun TotalWeeksSlider(
    currentWeeks: Int,
    onWeeksChange: (Int) -> Unit
) {
    var sliderValue by remember { mutableStateOf(currentWeeks.toFloat()) }
    var textFieldValue by remember { mutableStateOf(currentWeeks.toString()) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // 周数显示和输入
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "总周数",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { input ->
                    // 仅允许数字输入
                    if (input.isEmpty() || input.all { it.isDigit() }) {
                        textFieldValue = input
                        input.toIntOrNull()?.let { value ->
                            if (value in 1..30) {
                                sliderValue = value.toFloat()
                                onWeeksChange(value)
                            }
                        }
                    }
                },
                modifier = Modifier.width(80.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                singleLine = true
            )
            
            Text(
                text = "周",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 周数滑块
        Slider(
            value = sliderValue,
            onValueChange = { value ->
                sliderValue = value
                val intValue = value.toInt()
                textFieldValue = intValue.toString()
                onWeeksChange(intValue)
            },
            valueRange = 1f..30f,
            steps = 28,
            modifier = Modifier.fillMaxWidth()
        )
        
        // 滑块范围指示
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "1周",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "30周",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 格式化日期
 */
private fun formatDate(date: LocalDate): String {
    val javaDate = date.toJavaLocalDate()
    return javaDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
} 