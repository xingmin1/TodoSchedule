package com.example.todoschedule.ui.course.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.ui.course.add.CourseNodeUiState
import com.example.todoschedule.ui.theme.courseColors
import kotlinx.coroutines.flow.collectLatest

/** 编辑课程页面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCourseScreen(
    courseId: Int,
    onNavigateBack: () -> Unit,
    onCourseUpdated: () -> Unit,
    viewModel: EditCourseViewModel = hiltViewModel()
) {
    // 加载课程数据
    LaunchedEffect(courseId) { viewModel.loadCourse(courseId) }

    val uiState by viewModel.uiState.collectAsState()
    val courseName by viewModel.courseName.collectAsState()
    val color by viewModel.color.collectAsState()
    val room by viewModel.room.collectAsState()
    val teacher by viewModel.teacher.collectAsState()
    val credit by viewModel.credit.collectAsState()
    val courseCode by viewModel.courseCode.collectAsState()
    val courseNodes by viewModel.courseNodes.collectAsState()

    var showNodeDialog by remember { mutableStateOf(false) }
    var nodeBeingEdited by remember { mutableStateOf<CourseNodeUiState?>(null) }
    var nodeEditIndex by remember { mutableStateOf(-1) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is EditCourseEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                EditCourseEvent.CourseUpdated -> {
                    onCourseUpdated()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑课程") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor =
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.updateCourse() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Default.Check, contentDescription = "保存") }
        }
    ) { innerPadding ->
        when (uiState) {
            EditCourseUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            EditCourseUiState.Success -> {
                // 表单内容
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 课程基本信息
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "基本信息", style = MaterialTheme.typography.titleMedium)

                            OutlinedTextField(
                                value = courseName,
                                onValueChange = { viewModel.updateCourseName(it) },
                                label = { Text("课程名称*") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(text = "选择颜色")

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(courseColors) { courseColor ->
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Color(courseColor.toColorInt())
                                                )
                                                .border(
                                                    width = 2.dp,
                                                    color =
                                                        if (color == courseColor
                                                        )
                                                            MaterialTheme
                                                                .colorScheme
                                                                .primary
                                                        else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    viewModel.updateColor(courseColor)
                                                }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = room,
                                onValueChange = { viewModel.updateRoom(it) },
                                label = { Text("教室") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = teacher,
                                onValueChange = { viewModel.updateTeacher(it) },
                                label = { Text("教师") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = credit,
                                    onValueChange = { viewModel.updateCredit(it) },
                                    label = { Text("学分") },
                                    keyboardOptions =
                                        KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = courseCode,
                                    onValueChange = { viewModel.updateCourseCode(it) },
                                    label = { Text("课程代码") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 课程节点
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "上课时间节点",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                IconButton(
                                    onClick = {
                                        nodeBeingEdited = null
                                        nodeEditIndex = -1
                                        showNodeDialog = true
                                    }
                                ) { Icon(Icons.Default.Add, contentDescription = "添加节点") }
                            }

                            if (courseNodes.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "请添加上课时间",
                                        color =
                                            MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = 0.6f
                                            ),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    courseNodes.forEachIndexed { index, node ->
                                        CourseNodeItem(
                                            node = node,
                                            onEdit = {
                                                nodeBeingEdited = node
                                                nodeEditIndex = index
                                                showNodeDialog = true
                                            },
                                            onDelete = { viewModel.deleteCourseNode(index) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            EditCourseUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { Text("加载失败，请返回重试", color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    if (showNodeDialog) {
        CourseNodeDialog(
            node = nodeBeingEdited,
            onDismiss = { showNodeDialog = false },
            onConfirm = { node ->
                if (nodeEditIndex >= 0) {
                    viewModel.updateCourseNode(nodeEditIndex, node)
                } else {
                    viewModel.addCourseNode(node)
                }
                showNodeDialog = false
            }
        )
    }
}

/** 课程节点项 */
@Composable
fun CourseNodeItem(node: CourseNodeUiState, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val day =
                    when (node.day) {
                        1 -> "周一"
                        2 -> "周二"
                        3 -> "周三"
                        4 -> "周四"
                        5 -> "周五"
                        6 -> "周六"
                        7 -> "周日"
                        else -> "未知"
                    }

                val weekType =
                    when (node.weekType) {
                        0 -> "全部"
                        1 -> "单周"
                        2 -> "双周"
                        else -> "未知"
                    }

                Text(text = "$day 第${node.startNode}-${node.startNode + node.step - 1}节")

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "第${node.startWeek}-${node.endWeek}周 ($weekType)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                if (node.room.isNotEmpty() || node.teacher.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))

                    val info = buildString {
                        if (node.room.isNotEmpty()) {
                            append("@${node.room}")
                        }
                        if (node.teacher.isNotEmpty()) {
                            if (isNotEmpty()) append(" ")
                            append(node.teacher)
                        }
                    }

                    Text(
                        text = info,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "编辑",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDelete) {
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

/** 课程节点对话框 */
@Composable
fun CourseNodeDialog(
    node: CourseNodeUiState?,
    onDismiss: () -> Unit,
    onConfirm: (CourseNodeUiState) -> Unit
) {
    val isNewNode = node == null

    var day by remember { mutableStateOf(node?.day ?: 1) }
    var startNode by remember { mutableStateOf("${node?.startNode ?: 1}") }
    var step by remember { mutableStateOf("${node?.step ?: 2}") }
    var startWeek by remember { mutableStateOf("${node?.startWeek ?: 1}") }
    var endWeek by remember { mutableStateOf("${node?.endWeek ?: 16}") }
    var weekType by remember { mutableStateOf(node?.weekType ?: AppConstants.WeekTypes.ALL) }
    var room by remember { mutableStateOf(node?.room ?: "") }
    var teacher by remember { mutableStateOf(node?.teacher ?: "") }

    var dayError by remember { mutableStateOf(false) }
    var startNodeError by remember { mutableStateOf(false) }
    var stepError by remember { mutableStateOf(false) }
    var startWeekError by remember { mutableStateOf(false) }
    var endWeekError by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        dayError = day < 1 || day > 7
        startNodeError = startNode.toIntOrNull() == null || startNode.toInt() < 1
        stepError = step.toIntOrNull() == null || step.toInt() < 1
        startWeekError = startWeek.toIntOrNull() == null || startWeek.toInt() < 1
        endWeekError =
            endWeek.toIntOrNull() == null ||
                    endWeek.toInt() < (startWeek.toIntOrNull() ?: AppConstants.Ids.INVALID_ID)

        return !dayError && !startNodeError && !stepError && !startWeekError && !endWeekError
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNewNode) "添加课程节点" else "编辑课程节点") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("星期几")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 1..7) {
                        val dayText =
                            when (i) {
                                1 -> "一"
                                2 -> "二"
                                3 -> "三"
                                4 -> "四"
                                5 -> "五"
                                6 -> "六"
                                7 -> "日"
                                else -> ""
                            }

                        Box(
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (day == i)
                                            MaterialTheme.colorScheme
                                                .primary
                                        else
                                            MaterialTheme.colorScheme
                                                .surfaceVariant
                                    )
                                    .clickable { day = i },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayText,
                                color =
                                    if (day == i) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startNode,
                        onValueChange = { startNode = it },
                        label = { Text("开始节次") },
                        keyboardOptions =
                            KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = startNodeError,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = step,
                        onValueChange = { step = it },
                        label = { Text("节数") },
                        keyboardOptions =
                            KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = stepError,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startWeek,
                        onValueChange = { startWeek = it },
                        label = { Text("开始周") },
                        keyboardOptions =
                            KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = startWeekError,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = endWeek,
                        onValueChange = { endWeek = it },
                        label = { Text("结束周") },
                        keyboardOptions =
                            KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = endWeekError,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("周类型")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val weekTypes = listOf("全部", "单周", "双周")

                    weekTypes.forEachIndexed { index, type ->
                        Button(
                            onClick = { weekType = index },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = type,
                                color =
                                    if (weekType == index)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.6f
                                        )
                            )
                        }
                    }
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("教室(可选)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("教师(可选)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (validate()) {
                        onConfirm(
                            CourseNodeUiState(
                                day = day,
                                startNode = startNode.toInt(),
                                step = step.toInt(),
                                startWeek = startWeek.toInt(),
                                endWeek = endWeek.toInt(),
                                weekType = weekType,
                                room = room,
                                teacher = teacher
                            )
                        )
                    }
                }
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
