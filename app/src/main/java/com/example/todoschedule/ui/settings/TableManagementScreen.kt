package com.example.todoschedule.ui.settings

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.domain.model.Table
import com.example.todoschedule.ui.navigation.AppRoutes
import com.example.todoschedule.ui.navigation.NavigationState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * 课表管理屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableManagementScreen(
    navigationState: NavigationState,
    viewModel: TableManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 记录上一次刷新状态以检测变化
    var wasRefreshing by remember { mutableStateOf(false) }

    // 处理下拉刷新指示器的动画状态
    LaunchedEffect(isRefreshing) {
        Log.d("TableManagementScreen", "LaunchedEffect(isRefreshing) triggered: $isRefreshing")

        if (isRefreshing) {
            Log.d("TableManagementScreen", "正在刷新，显示指示器")
            // 无需手动调用 animateToThreshold，Modifier.pullToRefresh 会处理
            wasRefreshing = true
        } else if (wasRefreshing) {
            Log.d("TableManagementScreen", "刷新完成，隐藏指示器")
            // 无需手动调用 animateToHidden，Modifier.pullToRefresh 会处理
            wasRefreshing = false
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = {
                    // onRefresh 回调会在用户拉过阈值并释放后触发
                    Log.d("TableManagementScreen", "onRefresh 回调被调用 (用户触发)")
                    viewModel.refreshTables()
                }
            ),
        topBar = {
            TopAppBar(
                title = { Text("课表管理") },
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
            ExtendedFloatingActionButton(
                onClick = {
                    // 导航到创建新课表页面
                    navigationState.navController.navigate(AppRoutes.CreateEditTable.createRoute())
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = "添加") },
                text = { Text("添加课表") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 主内容
            if (uiState.isLoading && uiState.tables.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.tables.isEmpty()) {
                EmptyTableList(
                    onAddClick = {
                        navigationState.navController.navigate(AppRoutes.CreateEditTable.createRoute())
                    }
                )
            } else {
                TableList(
                    tables = uiState.tables,
                    onTableSettingsClick = { tableId ->
                        navigationState.navController.navigate(
                            AppRoutes.SingleTableSettings.createRoute(tableId)
                        )
                    },
                    onTableEditClick = { tableId ->
                        navigationState.navController.navigate(
                            AppRoutes.CreateEditTable.createRoute(tableId)
                        )
                    },
                    onTableDeleteClick = { tableId, tableName ->
                        scope.launch {
                            viewModel.deleteTable(
                                tableId = tableId,
                                onSuccess = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("课表 \"$tableName\" 已删除")
                                    }
                                },
                                onError = { error ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(error)
                                    }
                                }
                            )
                        }
                    }
                )
            }

            // 顶部进度指示器
            if (isRefreshing) {
                // 显示不确定进度条
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (pullRefreshState.distanceFraction > 0) {
                // 仅在有拉动距离时显示进度条
                LinearProgressIndicator(
                    progress = { pullRefreshState.distanceFraction },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // 错误提示
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            scope.launch {
                snackbarHostState.showSnackbar(error)
            }
        }
    }
}

/**
 * 课表列表
 */
@Composable
private fun TableList(
    tables: List<Table>,
    onTableSettingsClick: (Int) -> Unit,
    onTableEditClick: (Int) -> Unit,
    onTableDeleteClick: (Int, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tables) { table ->
            TableItem(
                table = table,
                onSettingsClick = { onTableSettingsClick(table.id) },
                onEditClick = { onTableEditClick(table.id) },
                onDeleteClick = { onTableDeleteClick(table.id, table.tableName) }
            )
        }
        // 底部间距
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

/**
 * 课表项
 */
@Composable
private fun TableItem(
    table: Table,
    onSettingsClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 课表名称和背景色指示
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                Color(
                                    if (table.background.isNotEmpty()) table.background.toInt() else "#3F51B5".toColorInt()
                                )
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = table.tableName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 操作按钮
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "设置",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = { showDeleteConfirmation = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
            )

            // 课表详情
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "开学日期",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDate(table.startDate),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Column {
                    Text(
                        text = "总周数",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${table.totalWeeks} 周",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        DeleteConfirmationDialog(
            tableName = table.tableName,
            onConfirm = {
                onDeleteClick()
                showDeleteConfirmation = false
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }
}

/**
 * 删除确认对话框
 */
@Composable
private fun DeleteConfirmationDialog(
    tableName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除课表") },
        text = { Text("确定要删除课表 \"$tableName\" 吗？此操作不可撤销。") },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
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

/**
 * 空课表列表
 */
@Composable
private fun EmptyTableList(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "暂无课表",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击下方按钮添加新课表",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(
            onClick = onAddClick
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加")
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加课表")
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