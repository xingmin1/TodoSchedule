package com.example.todoschedule.ui.table

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditTableScreen(
    onNavigateBack: () -> Unit,
    onTableSaved: () -> Unit,
    viewModel: CreateEditTableViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val datePickerState = rememberDatePickerState()

    // 处理保存成功事件
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onTableSaved() // 调用导航回调
            viewModel.consumeSavedEvent() // 消耗事件避免重复导航
        }
    }

    // 显示错误信息
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { errorMsg ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = errorMsg,
                    duration = SnackbarDuration.Short
                )
                viewModel.consumeErrorMessage()
            }
        }
    }

    // 日期选择器对话框
    if (uiState.showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { viewModel.dismissStartDatePicker() },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val selectedDate =
                            kotlinx.datetime.Instant.fromEpochMilliseconds(selectedMillis)
                                .toLocalDateTime(kotlinx.datetime.TimeZone.UTC).date
                        viewModel.onStartDateSelected(selectedDate)
                    } else {
                        viewModel.onStartDateSelected(null) // Or handle no selection
                    }
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissStartDatePicker() }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "编辑课表" else "创建新课表") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
            ExtendedFloatingActionButton(
                onClick = {
                    if (!uiState.isLoading) viewModel.saveTable()
                },
                containerColor = if (uiState.isLoading)
                    MaterialTheme.colorScheme.surfaceVariant
                else
                    MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (uiState.isLoading)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onPrimaryContainer,
                icon = {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                text = { Text(text = "保存") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.tableName,
                onValueChange = viewModel::onTableNameChange,
                label = { Text("课表名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.startDate?.toString() ?: "", // 显示选中的日期
                onValueChange = { /* ReadOnly */ },
                label = { Text("学期开始日期") },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { viewModel.showStartDatePicker() }) {
                        Icon(Icons.Default.DateRange, contentDescription = "选择日期")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.totalWeeks,
                onValueChange = viewModel::onTotalWeeksChange,
                label = { Text("总周数") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 学期信息
            OutlinedTextField(
                value = uiState.terms ?: "",
                onValueChange = viewModel::onTermsChange,
                label = { Text("学期信息 (可选)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 背景颜色 (简单占位，实际需要颜色选择器)
            OutlinedTextField(
                value = uiState.background ?: "",
                onValueChange = viewModel::onBackgroundChange,
                label = { Text("背景颜色 (可选，例如 #FFFFFF)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
                // TODO: 添加颜色预览或按钮触发颜色选择器
            )

            Spacer(modifier = Modifier.weight(1f)) // 将保存按钮推到底部 (如果不用 FAB)
        }
    }
} 