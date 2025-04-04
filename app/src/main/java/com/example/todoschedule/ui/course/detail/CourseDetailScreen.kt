package com.example.todoschedule.ui.course.detail

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * 课程详情页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    tableId: Int,
    courseId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Int, Int) -> Unit,
    viewModel: CourseDetailViewModel = hiltViewModel()
) {
    // 加载课程数据
    LaunchedEffect(courseId) {
        viewModel.loadCourse(courseId)
    }

    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("课程详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // 编辑按钮
                    IconButton(onClick = { onNavigateToEdit(tableId, courseId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑课程")
                    }

                    // 删除按钮
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除课程",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is CourseDetailUiState.Loading -> {
                    // 加载中
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is CourseDetailUiState.Success -> {
                    // 显示课程详情
                    val course = (uiState as CourseDetailUiState.Success).course
                    CourseDetail(course = course)
                }

                is CourseDetailUiState.Error -> {
                    // 显示错误信息
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (uiState as CourseDetailUiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is CourseDetailUiState.Deleted -> {
                    // 课程已删除，返回上一页
                    LaunchedEffect(Unit) {
                        onNavigateBack()
                    }
                }
            }
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除课程") },
            text = { Text("确定要删除这门课程吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCourse()
                        showDeleteDialog = false
                    }
                ) {
                    Text(
                        "删除",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 课程详情内容
 */
@Composable
private fun CourseDetail(course: CourseDetailModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 课程颜色和名称
        val backgroundColor = try {
            Color(course.color.toColorInt())
        } catch (_: Exception) {
            MaterialTheme.colorScheme.primary
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = course.courseName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isColorDark(backgroundColor)) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (course.teacher != null) {
                    Text(
                        text = "教师：${course.teacher}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isColorDark(backgroundColor)) Color.White.copy(alpha = 0.8f) else Color.Black.copy(
                            alpha = 0.8f
                        )
                    )
                }

                if (course.credit != null) {
                    Text(
                        text = "学分：${course.credit}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isColorDark(backgroundColor)) Color.White.copy(alpha = 0.8f) else Color.Black.copy(
                            alpha = 0.8f
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 上课节点列表
        Text(
            text = "上课时间",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        course.nodes.forEach { node ->
            NodeCard(node = node)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * 节点卡片
 */
@Composable
private fun NodeCard(node: CourseNodeDetailModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val dayText = when (node.day) {
                1 -> "周一"
                2 -> "周二"
                3 -> "周三"
                4 -> "周四"
                5 -> "周五"
                6 -> "周六"
                7 -> "周日"
                else -> "未知"
            }

            val weekTypeText = when (node.weekType) {
                0 -> "全部周"
                1 -> "单周"
                2 -> "双周"
                else -> "未知"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 星期指示器
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayText.substring(1, 2),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.padding(horizontal = 8.dp))

                Column {
                    Text(
                        text = "$dayText 第${node.startNode}-${node.startNode + node.step - 1}节",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "第${node.startWeek}-${node.endWeek}周 ($weekTypeText)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (node.room != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "教室：${node.room}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (node.teacher != null) {
                        Text(
                            text = "教师：${node.teacher}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 判断颜色是否为深色
 */
private fun isColorDark(color: Color): Boolean {
    val darkness = 1 - (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
    return darkness > 0.5
} 