package com.example.todoschedule.ui.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import com.example.todoschedule.domain.model.TableTimeConfig
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
                title = { Text("课表设置: ${uiState.table?.tableName ?: ""}") },
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
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showAddTimeConfigDialog() },
                    icon = { Icon(Icons.Default.Add, "添加时间配置") },
                    text = { Text("添加时间配置") }
                )
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.table == null) {
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
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
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("保存开学日期与周数")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
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

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "时间配置方案",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            if (uiState.timeConfigs.isEmpty()) {
                                Text(
                                    text = "暂无时间配置，请添加",
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    uiState.timeConfigs.forEach { config ->
                                        TimeConfigItem(
                                            config = config,
                                            onItemClick = {
                                                navigationState.navigateToTimeNodesSettings(config.id.toInt())
                                            },
                                            onEditClick = {
                                                viewModel.showEditTimeConfigNameDialog(config.id, config.name)
                                            },
                                            onDeleteClick = {
                                                viewModel.showDeleteTimeConfigDialog(config.id, config.name)
                                            },
                                            onSetDefaultClick = {
                                                viewModel.setDefaultTimeConfig(config.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(80.dp)) 
                }
            }
        }
    }

    when (val dialogState = uiState.dialogState) {
        is SingleTableSettingsViewModel.DialogState.AddTimeConfig -> {
            AddEditTimeConfigNameDialog(
                isEdit = false,
                currentName = "",
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { newName -> viewModel.addTimeConfig(newName) }
            )
        }
        is SingleTableSettingsViewModel.DialogState.EditTimeConfigName -> {
            AddEditTimeConfigNameDialog(
                isEdit = true,
                currentName = dialogState.currentName,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { newName -> viewModel.updateTimeConfigName(dialogState.configId, newName) }
            )
        }
        is SingleTableSettingsViewModel.DialogState.DeleteTimeConfig -> {
            DeleteTimeConfigConfirmationDialog(
                configName = dialogState.configName,
                onConfirm = { viewModel.deleteTimeConfig(dialogState.configId) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        is SingleTableSettingsViewModel.DialogState.None -> Unit
    }
}

@Composable
private fun TimeConfigItem(
    config: TableTimeConfig,
    onItemClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSetDefaultClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = config.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (config.isDefault) {
                    Text(
                        text = "(默认)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (!config.isDefault) {
                IconButton(onClick = onSetDefaultClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.CheckCircleOutline, "设为默认", tint = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, "编辑名称")
            }
            if (!config.isDefault) {
                 IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun AddEditTimeConfigNameDialog(
    isEdit: Boolean,
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "编辑名称" else "添加时间配置") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("配置名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) {
                Text("确定")
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
private fun DeleteTimeConfigConfirmationDialog(
    configName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除时间配置") },
        text = { Text("确定要删除时间配置 \"$configName\" 吗？其下的所有节次信息也将被删除。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
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

@Composable
private fun TotalWeeksSlider(
    currentWeeks: Int,
    onWeeksChange: (Int) -> Unit
) {
    var sliderValue by remember { mutableStateOf(currentWeeks.toFloat()) }
    var textFieldValue by remember { mutableStateOf(currentWeeks.toString()) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
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

private fun formatDate(date: LocalDate): String {
    val javaDate = date.toJavaLocalDate()
    return javaDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
} 