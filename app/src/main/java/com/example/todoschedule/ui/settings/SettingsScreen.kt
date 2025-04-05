package com.example.todoschedule.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.BuildConfig

/**
 * 设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val darkTheme by viewModel.darkTheme.collectAsState()
    val materialYou by viewModel.materialYou.collectAsState()
    val firstDayOfWeek by viewModel.firstDayOfWeek.collectAsState()
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsCategory(title = "外观") {
                SettingsSwitchItem(
                    icon = Icons.Default.Palette,
                    title = "深色模式",
                    subtitle = "使用深色主题",
                    checked = darkTheme,
                    onCheckedChange = viewModel::updateDarkTheme
                )

                SettingsSwitchItem(
                    icon = Icons.Default.Palette,
                    title = "Material You",
                    subtitle = "使用系统动态颜色",
                    checked = materialYou,
                    onCheckedChange = viewModel::updateMaterialYou
                )
            }

            SettingsCategory(title = "日历") {
                SettingsClickableItem(
                    icon = Icons.Default.DateRange,
                    title = "一周起始日",
                    subtitle = if (firstDayOfWeek == 1) "周一" else "周日",
                    onClick = { viewModel.toggleFirstDayOfWeek() }
                )

                SettingsClickableItem(
                    icon = Icons.Default.DateRange,
                    title = "开学日期",
                    subtitle = "设置学期开始时间",
                    onClick = { /* 显示日期选择器 */ }
                )

                SettingsClickableItem(
                    icon = Icons.Default.Schedule,
                    title = "节次设置",
                    subtitle = "自定义课程时间",
                    onClick = { /* 导航至时间设置页面 */ }
                )
            }

            SettingsCategory(title = "通知") {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "课程提醒",
                    subtitle = "在上课前提醒您",
                    checked = false,
                    onCheckedChange = { /* 更新通知设置 */ }
                )
            }

            SettingsCategory(title = "关于") {
                SettingsClickableItem(
                    icon = Icons.Default.Info,
                    title = "关于应用",
                    subtitle = "版本 1.0.0",
                    onClick = { /* 显示关于对话框 */ }
                )
            }

            // ----- 用户账户 ----- (新类别)
            SettingsCategory(title = "账户") {
                // TODO: 添加修改密码、绑定手机/邮箱等入口

                // 登出按钮
                SettingsClickableItem(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    title = "退出登录",
                    onClick = { showLogoutConfirmDialog = true }
                )
            }

            // 开发者选项
            if (BuildConfig.DEBUG) {
                DeveloperOptions(viewModel)
            }
        }
    }

    // 登出确认对话框
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("确认退出") },
            text = { Text("您确定要退出当前账号吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirmDialog = false
                    viewModel.logout()
                    onLogout()
                }) {
                    Text("退出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 设置类别
 */
@Composable
fun SettingsCategory(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

/**
 * 可点击的设置项
 */
@Composable
fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
    )
}

/**
 * 带开关的设置项
 */
@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )

                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) }
        )
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
    )
}

/**
 * 清空数据库确认对话框
 */
@Composable
fun ClearDatabaseConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("警告") },
        text = { Text("确定要清空所有数据吗？此操作不可撤销。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确定", color = MaterialTheme.colorScheme.error)
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
 * 加载对话框
 */
@Composable
fun LoadingDialog() {
    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

/**
 * 开发者选项部分
 */
@Composable
fun DeveloperOptions(viewModel: SettingsViewModel) {
    val databaseOperation by viewModel.databaseOperation.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }

    // 处理数据库操作状态
    when (val currentState = databaseOperation) {
        is DatabaseOperation.Loading -> {
            LoadingDialog()
        }

        is DatabaseOperation.Success -> {
            val message = currentState.message
            val context = LocalContext.current
            LaunchedEffect(message) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                viewModel.resetDatabaseOperation()
            }
        }

        is DatabaseOperation.Error -> {
            val message = currentState.message
            val context = LocalContext.current
            LaunchedEffect(message) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                viewModel.resetDatabaseOperation()
            }
        }

        else -> { /* Idle state, do nothing */
        }
    }

    // 显示确认对话框
    if (showConfirmDialog) {
        ClearDatabaseConfirmDialog(
            onConfirm = {
                showConfirmDialog = false
                viewModel.clearDatabase()
            },
            onDismiss = { showConfirmDialog = false }
        )
    }

    SettingsCategory(title = "开发者选项") {
        SettingsClickableItem(
            icon = Icons.Default.DeleteForever,
            title = "清空数据库",
            subtitle = "删除所有数据（仅开发环境可用）",
            onClick = { showConfirmDialog = true }
        )
    }
}

/**
 * 数据库操作状态处理
 */
@Composable
fun DatabaseOperationHandler(viewModel: SettingsViewModel) {
    // Implementation of the new method
} 