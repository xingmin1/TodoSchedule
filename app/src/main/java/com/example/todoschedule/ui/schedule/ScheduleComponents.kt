package com.example.todoschedule.ui.schedule

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.todoschedule.ui.navigation.NavigationState


/**
 * 通用顶部栏组件，支持日期、周数、模式切换、常用操作按钮
 * @param viewMode 当前视图模式
 * @param formattedDate 顶部显示的日期字符串
 * @param weekInfoText 顶部显示的周信息字符串
 * @param onGoToToday 回到今天按钮回调
 * @param onAdd 添加按钮回调
 * @param onDownload 下载按钮回调
 * @param onShare 分享按钮回调
 * @param onChangeViewMode 切换视图模式回调
 * @param onShowMoreMenu 显示更多菜单回调
 * @param showMoreMenu 是否显示更多菜单
 * @param onDismissMoreMenu 关闭更多菜单回调
 */
@Composable
fun ScheduleTopBar(
    viewMode: ScheduleViewMode,
    formattedDate: String,
    weekInfoText: String?,
    onGoToToday: () -> Unit,
    onAdd: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onChangeViewMode: (ScheduleViewMode) -> Unit,
    showMoreMenu: Boolean,
    onShowMoreMenu: () -> Unit,
    onDismissMoreMenu: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 日期与周信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.titleLarge
            )
            if (weekInfoText != null) {
                Text(
                    text = weekInfoText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        // 操作按钮区
        Row(
            horizontalArrangement = Arrangement.Absolute.Right,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onGoToToday) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "回到今天",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onAdd) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加"
                )
            }
            IconButton(onClick = onDownload) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "下载"
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "分享"
                )
            }
            // 更多菜单
            Box {
                IconButton(onClick = onShowMoreMenu) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多"
                    )
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = onDismissMoreMenu,
                    offset = DpOffset(x = 0.dp, y = 8.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("周视图") },
                        onClick = { onChangeViewMode(ScheduleViewMode.WEEK); onDismissMoreMenu() },
                        enabled = viewMode != ScheduleViewMode.WEEK
                    )
                    DropdownMenuItem(
                        text = { Text("月视图") },
                        onClick = { onChangeViewMode(ScheduleViewMode.MONTH); onDismissMoreMenu() },
                        enabled = viewMode != ScheduleViewMode.MONTH
                    )
                    DropdownMenuItem(
                        text = { Text("日视图") },
                        onClick = { onChangeViewMode(ScheduleViewMode.DAY); onDismissMoreMenu() },
                        enabled = viewMode != ScheduleViewMode.DAY
                    )
                }
            }
        }
    }
}

/**
 * 多课表无数据提示条组件
 * @param onCreateTable 新建课表回调
 * @param onImportTable 导入课表回调
 */
@Composable
fun NoTableBanner(
    onCreateTable: () -> Unit,
    onImportTable: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "该日期无课表数据，请新建或导入课表",
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onCreateTable,
                modifier = Modifier.padding(start = 8.dp)
            ) { Text("新建课表") }
            Button(
                onClick = onImportTable,
                modifier = Modifier.padding(start = 8.dp)
            ) { Text("导入课表") }
        }
    }
}

/**
 * 未选择任何课表时显示的 Composable。
 * 提供创建新课表或导入课表的入口。
 *
 * @param navigationState 用于处理导航事件的对象。
 * @param onNavigateToImport 当点击"从教务系统导入"按钮时触发的回调。
 */
@Composable
fun NoTableSelectedScreen(
    navigationState: NavigationState,
    onNavigateToImport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, // 水平居中
        verticalArrangement = Arrangement.Center // 垂直居中
    ) {
        Text("您还没有选择或创建课表", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("请先创建新课表或从教务系统导入", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNavigateToImport) { // 导入按钮
            Text("从教务系统导入课表")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navigationState.navigateToCreateEditTable() }) { // 手动创建按钮
            Text("手动创建新课表")
        }
        // TODO: 添加选择现有课表的按钮 (如果允许多课表)
    }
}

/**
 * 加载中状态组件
 */
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(), // 填充整个父容器
        contentAlignment = Alignment.Center // 内容居中对齐
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally // 列内元素水平居中
        ) {
            CircularProgressIndicator( // 圆形进度条
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary // 使用主题色
            )
            Spacer(modifier = Modifier.height(16.dp)) // 添加垂直间距
            Text(
                text = "加载中...",
                style = MaterialTheme.typography.bodyLarge // 使用预定义的文本样式
            )
        }
    }
}

/**
 * 课表为空状态组件
 */
@Composable
fun EmptyScheduleScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "没有课程",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击右下角的 + 按钮添加课程",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 错误状态组件
 */
@Composable
fun ErrorScreen(errorMessage: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "出错了",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
