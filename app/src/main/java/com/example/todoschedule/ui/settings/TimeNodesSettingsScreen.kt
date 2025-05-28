package com.example.todoschedule.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.domain.model.TableTimeConfigNode
import com.example.todoschedule.ui.navigation.NavigationState
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalTime
import java.time.format.DateTimeFormatter

/**
 * 时间节点设置屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeNodesSettingsScreen(
    navigationState: NavigationState,
    viewModel: TimeNodesSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 显示成功/错误消息
    LaunchedEffect(uiState.success) {
        uiState.success?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearSuccessMessage()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("配置节次: ${uiState.configName}") },
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
            FloatingActionButton(onClick = { viewModel.showAddNodeDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "添加节次")
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
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.nodes.isEmpty()) {
                Text(
                    text = "暂无节次，点击右下角按钮添加",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.nodes, key = { it.id }) { node ->
                        TimeNodeItem(
                            node = node,
                            onEditClick = { viewModel.showEditNodeDialog(node) },
                            onDeleteClick = { viewModel.showDeleteNodeDialog(node.id, node.name) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) } // FAB spacing
                }
            }
        }
    }

    // 对话框处理
    when (val dialogState = uiState.dialogState) {
        is TimeNodesSettingsViewModel.DialogState.AddOrEditNode -> {
            AddEditNodeDialog(
                dialogState = dialogState,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { viewModel.saveNode(dialogState) },
                onUpdateState = { viewModel.updateDialogState(it) }
            )
        }

        is TimeNodesSettingsViewModel.DialogState.DeleteNode -> {
            DeleteNodeConfirmationDialog(
                nodeName = dialogState.nodeName,
                onConfirm = { viewModel.deleteNode(dialogState.nodeId) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is TimeNodesSettingsViewModel.DialogState.None -> Unit // Do nothing
    }
}

/**
 * 时间节点列表项
 */
@Composable
private fun TimeNodeItem(
    node: TableTimeConfigNode,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "第${node.node}节: ${node.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${
                        node.startTime.toJavaLocalTime().format(timeFormatter)
                    } - ${node.endTime.toJavaLocalTime().format(timeFormatter)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * 添加或编辑节次对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditNodeDialog(
    dialogState: TimeNodesSettingsViewModel.DialogState.AddOrEditNode,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onUpdateState: (TimeNodesSettingsViewModel.DialogState.AddOrEditNode) -> Unit
) {
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (dialogState.nodeToEdit == null) "添加节次" else "编辑节次") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dialogState.nodeName,
                    onValueChange = { onUpdateState(dialogState.copy(nodeName = it)) },
                    label = { Text("节次名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 显示节次编号 (只读)
                Text(
                    text = "节次编号: ${dialogState.nodeNumber}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "开始时间: ${
                            dialogState.startTime.toJavaLocalTime().format(timeFormatter)
                        }"
                    )
                    TextButton(onClick = { showStartTimePicker = true }) {
                        Text("选择")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("结束时间: ${dialogState.endTime.toJavaLocalTime().format(timeFormatter)}")
                    TextButton(onClick = { showEndTimePicker = true }) {
                        Text("选择")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    // 开始时间选择器
    if (showStartTimePicker) {
        val state = rememberTimePickerState(
            initialHour = dialogState.startTime.hour,
            initialMinute = dialogState.startTime.minute,
            is24Hour = true
        )
        TimePickerDialog(
            onCancel = { showStartTimePicker = false },
            onConfirm = {
                onUpdateState(dialogState.copy(startTime = LocalTime(state.hour, state.minute)))
                showStartTimePicker = false
            }) {
            TimePicker(state = state)
        }
    }

    // 结束时间选择器
    if (showEndTimePicker) {
        val state = rememberTimePickerState(
            initialHour = dialogState.endTime.hour,
            initialMinute = dialogState.endTime.minute,
            is24Hour = true
        )
        TimePickerDialog(
            onCancel = { showEndTimePicker = false },
            onConfirm = {
                onUpdateState(dialogState.copy(endTime = LocalTime(state.hour, state.minute)))
                showEndTimePicker = false
            }) {
            TimePicker(state = state)
        }
    }
}

// TimePickerDialog Composable (需要自行实现或使用库)
@Composable
fun TimePickerDialog(
    title: String = "Select Time",
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    toggle: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                ),
        ) {
            toggle()
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = title,
                    style = MaterialTheme.typography.labelMedium
                )
                content()
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onCancel) { Text("Cancel") }
                    TextButton(onClick = onConfirm) { Text("OK") }
                }
            }
        }
    }
}


/**
 * 删除节次确认对话框
 */
@Composable
private fun DeleteNodeConfirmationDialog(
    nodeName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除节次") },
        text = { Text("确定要删除节次 \"$nodeName\" 吗？") },
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