package com.example.todoschedule.ui.task

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.core.extensions.toJavaCalendar
import com.example.todoschedule.ui.components.CalendarPermissionTextProvider
import com.example.todoschedule.ui.components.PermissionDialog
import com.example.todoschedule.ui.navigation.NavigationState
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

/**
 * 任务日历同步界面
 * 用于将任务同步到设备日历
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCalendarSyncScreen(
    filter: String = "all",
    navigationState: NavigationState,
    viewModel: TaskCalendarViewModel = hiltViewModel()
) {
    val syncUiState by viewModel.uiState.collectAsState()
    val hasPermissions by viewModel.hasCalendarPermissions.collectAsState()
    val context = LocalContext.current

    // 任务筛选类型
    val taskFilterOptions = listOf("today", "week", "all", "custom")
    val taskFilterLabels = listOf("今日任务", "本周任务", "全部任务", "自定义选择")

    var selectedOption by remember { mutableStateOf(filter) }

    // 权限请求状态
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionActionTriggered by remember { mutableStateOf(false) } // 跟踪权限请求是否已被触发
    var needsPermissionRequest by remember { mutableStateOf(false) } // 标记是否需要请求权限

    // 首次加载时强制检查权限
    LaunchedEffect(Unit) {
        viewModel.checkCalendarPermissions()
        // 如果没有权限，立即请求
        if (!viewModel.hasCalendarPermissions.value) {
            needsPermissionRequest = true
        }
    }

    // 权限请求启动器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val allGranted = permissions.values.all { it }
            viewModel.onPermissionsResult(allGranted)
            if (!allGranted) {
                // 如果权限仍然未被授予，显示解释对话框
                showPermissionDialog = true
            }
            permissionActionTriggered = false // 重置触发器
            needsPermissionRequest = false // 重置请求标记
        }
    )

    // 使用 LaunchedEffect 检查初始权限状态，并在权限变化时重新加载
    LaunchedEffect(hasPermissions, selectedOption) {
        if (hasPermissions) {
            // 有权限，加载数据
            viewModel.loadTasks(selectedOption)
            viewModel.autoSelectCalendar()
            // 非自定义模式下强制默认全选
            if (selectedOption != "custom") {
                // 添加短延迟确保任务加载完成
                delay(100)
                viewModel.selectAllTasks()
            }
        }
    }

    // 当标记需要请求权限且尚未触发时，立即发起请求
    LaunchedEffect(needsPermissionRequest) {
        if (needsPermissionRequest && !permissionActionTriggered && !hasPermissions) {
            permissionActionTriggered = true
            permissionLauncher.launch(viewModel.getCalendarPermissions())
        }
    }

    // 显示权限对话框 (仅在权限请求被拒绝后显示)
    if (showPermissionDialog) {
        PermissionDialog(
            permissionTextProvider = CalendarPermissionTextProvider(),
            // 这里的 isPermanentlyDeclined 逻辑可以根据需要进一步完善
            // 简单处理：如果对话框显示了，就认为不是永久拒绝（因为是首次拒绝或后续拒绝）
            isPermanentlyDeclined = false,
            onDismiss = { showPermissionDialog = false },
            onOkClick = {
                showPermissionDialog = false
                // 用户点击OK再次请求权限
                permissionActionTriggered = true // 重新标记触发
                needsPermissionRequest = true // 重新标记需要请求
                permissionLauncher.launch(viewModel.getCalendarPermissions())
            },
            onGoToAppSettingsClick = {
                showPermissionDialog = false
                // 跳转到应用设置页面
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("将任务同步到系统日历") },
                navigationIcon = {
                    IconButton(onClick = { navigationState.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 根据权限状态显示不同内容
            if (!hasPermissions) {
                // 如果权限正在请求或等待处理，可以显示加载指示器或空白
                if (permissionActionTriggered || needsPermissionRequest) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                        // 或者显示一个提示文本，如 "正在检查权限..."
                    }
                } else if (!showPermissionDialog) { // 仅在对话框未显示时显示按钮
                    // 如果没有权限且未在请求，显示获取权限按钮
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("此功能需要日历访问权限才能工作")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                // 点击按钮触发权限请求
                                permissionActionTriggered = true
                                needsPermissionRequest = true
                            }
                        ) {
                            Text("授予权限")
                        }
                    }
                }
            } else {
                // 有权限时显示日历同步界面
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // 选项卡 - 使用更美观的下拉选择框
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "选择要同步的任务范围:",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 下拉选择框
                        var expanded by remember { mutableStateOf(false) }
                        val currentFilterLabel =
                            taskFilterLabels[taskFilterOptions.indexOf(selectedOption)]

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = currentFilterLabel,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                taskFilterOptions.forEachIndexed { index, option ->
                                    DropdownMenuItem(
                                        text = { Text(taskFilterLabels[index]) },
                                        onClick = {
                                            selectedOption = option
                                            expanded = false
                                            // 非自定义模式强制重新加载任务并自动全选
                                            viewModel.loadTasks(option)
                                            if (option != "custom") {
                                                viewModel.selectAllTasks()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 任务列表标题 和 全选/取消全选按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "任务列表 (可勾选):",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row {
                            TextButton(onClick = { viewModel.selectAllTasks() }) {
                                Text("全选")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { viewModel.deselectAllTasks() }) {
                                Text("取消全选")
                            }
                        }
                    }
                    Text(
                        "(共 ${syncUiState.tasks.size} 个任务，已选择 ${syncUiState.selectedTaskIds.size} 个)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(
                            items = syncUiState.tasks,
                            // 确保每次迭代时真正使用唯一键
                            key = { task -> task.id }
                        ) { task ->
                            TaskSyncItem(
                                task = task,
                                isSelected = syncUiState.selectedTaskIds.contains(task.id),
                                // 直接绑定切换逻辑，不再受 selectedOption 限制
                                onToggleSelection = { viewModel.toggleTaskSelection(task.id) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 同步按钮
                    Button(
                        onClick = { viewModel.syncSelectedTasksToCalendar() },
                        enabled = syncUiState.selectedTaskIds.isNotEmpty() && !syncUiState.isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        if (syncUiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("同步选中的${syncUiState.selectedTaskIds.size}个任务到系统日历")
                    }

                    // 同步结果消息
                    syncUiState.syncResultMessage?.let { message ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (message.contains("成功")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskSyncItem(
    task: TaskItemUiModel,
    isSelected: Boolean,
    onToggleSelection: () -> Unit
    // 移除 isSelectable 参数
) {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val formattedTime = remember(task) {
        val calendar = task.startTime.toJavaCalendar()
        formatter.format(calendar.time)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onToggleSelection() } // 整行可点击
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 始终显示 Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() } // 点击 Checkbox 也可以触发
            )
            Spacer(modifier = Modifier.width(12.dp)) // Checkbox 和文本的间距

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "时间: $formattedTime",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = task.timeDescription,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
} 