package com.example.todoschedule.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.ui.home.model.HomeUiState
import com.example.todoschedule.ui.navigation.NavigationState
import com.example.todoschedule.ui.study.StudyStat
import com.example.todoschedule.ui.study.StudyViewModel
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import com.example.todoschedule.ui.study.StatType as StudyModelStatType

/**
 * 首页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigationState: NavigationState,
    paddingValues: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel(),
    studyViewModel: StudyViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val todayCourses by viewModel.todayCourses.collectAsState()
    val todaySchedules by viewModel.todayOrdinarySchedules.collectAsState()
    val courseCount by viewModel.todayCourses.map { it.size }.collectAsState(initial = 0)
    val scheduleCount by viewModel.todayOrdinarySchedules.map { it.size }
        .collectAsState(initial = 0)
    val defaultTableId by viewModel.defaultTableIdState.collectAsState()

    var showQuickAddDialog by remember { mutableStateOf(false) }

    val studyStats by studyViewModel.studyStat.collectAsState()
    val todayFocusStat: StudyStat? =
        studyStats.find { it.type == StudyModelStatType.DAILY && it.title.contains("今日专注") }
    val weeklyFocusStat =
        studyStats.find { it.type == StudyModelStatType.WEEKLY && it.title.contains("本周专注") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("首页", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickAddDialog = true },
                modifier = Modifier
                    .padding(16.dp)
                    .padding(bottom = paddingValues.calculateBottomPadding()),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加日程/课程"
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (uiState) {
                is HomeUiState.Loading -> LoadingScreen()
                is HomeUiState.Empty -> EmptyHomeScreen()
                is HomeUiState.Error -> ErrorScreen((uiState as HomeUiState.Error).message)
                is HomeUiState.NoTableSelected -> NotableSelectedScreen(
                    navigationState = navigationState,
                    onNavigateToImport = {
                        navigationState.navigateSchoolSelectorScreen(
                            UUID.randomUUID() // 使用随机ID创建临时课表
                        )
                    }
                )

                is HomeUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = paddingValues.calculateBottomPadding()),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        item {
                            TodayOverviewSection(
                                courseCount = courseCount,
                                scheduleCount = scheduleCount,
                                todayFocusDisplay = todayFocusStat?.displayValue ?: "0小时",
                                onCourseClick = { navigationState.navigateToSchedule() },
                                onTaskClick = { navigationState.navigateToTask() },
                                onStudyClick = { navigationState.navigateToStudy() }
                            )
                        }
                        item {
                            TodayCoursesSection(
                                courses = todayCourses,
                                onCourseClick = { course ->
                                    navigationState.navigateToCourseDetail(
                                        tableId = defaultTableId,
                                        courseId = course.id
                                    )
                                },
                                onViewAllClick = { navigationState.navigateToSchedule() }
                            )
                        }
                        item {
                            PendingTasksSection(
                                tasks = todaySchedules,
                                onTaskClick = { task ->
                                    navigationState.navigateToOrdinaryScheduleDetail(task.id)
                                },
                                onViewAllClick = { navigationState.navigateToTask() },
                                onToggleComplete = { task ->
                                    viewModel.toggleComplete(task)
                                }
                            )
                        }

                        val displayWeeklyStat = weeklyFocusStat ?: StudyStat(
                            title = "本周专注",
                            value = 0f,
                            type = StudyModelStatType.WEEKLY
                        )

                        item {
                            StudyStatisticsSection(
                                studyStat = displayWeeklyStat,
                                onClick = { navigationState.navigateToStudy() }
                            )
                        }

//                        weeklyFocusStat?.let { stat ->
//                            item {
//                                StudyStatisticsSection(
//                                    studyStat = stat,
//                                    onClick = { navigationState.navigateToStudy() }
//                                )
//                            }
//                        }
                    }
                }
            }
        }
    }

    if (showQuickAddDialog) {
        QuickAddScheduleDialog(
            onDismiss = { showQuickAddDialog = false },
            viewModel = hiltViewModel<QuickAddScheduleViewModel>()
        )
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "加载中...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun EmptyHomeScreen() {
    val currentDate = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
    val formattedDate = "${currentDate.year}/${
        currentDate.monthNumber.toString().padStart(2, '0')
    }/${currentDate.dayOfMonth.toString().padStart(2, '0')}"

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "今日无日程",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$formattedDate ${currentDate.dayOfWeek.getChineseWeekName()}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "点击右下角按钮添加日程",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun DayOfWeek.getChineseWeekName(): String {
    return when (this) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }
}

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

@Composable
fun NotableSelectedScreen(
    navigationState: NavigationState,
    onNavigateToImport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "您还没有选择或创建课表",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "请先创建新课表或从教务系统导入",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNavigateToImport) {
            Text("从教务系统导入课表")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navigationState.navigateToCreateEditTable() }) {
            Text("手动创建新课表")
        }
    }
}

@Composable
fun TodayOverviewSection(
    courseCount: Int,
    scheduleCount: Int,
    todayFocusDisplay: String,
    onCourseClick: () -> Unit,
    onTaskClick: () -> Unit,
    onStudyClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "今日概览",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OverviewCard(
                title = "今日课程",
                value = courseCount.toString(),
                color = MaterialTheme.colorScheme.primary,
                onClick = onCourseClick
            )
            OverviewCard(
                title = "待办任务",
                value = scheduleCount.toString(),
                color = MaterialTheme.colorScheme.tertiary,
                onClick = onTaskClick
            )
            OverviewCard(
                title = "专注时长",
                value = todayFocusDisplay,
                color = MaterialTheme.colorScheme.secondary,
                onClick = onStudyClick
            )
        }
    }
}

@Composable
fun OverviewCard(
    title: String,
    value: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}


@Composable
fun TodayCoursesSection(
    courses: List<HomeCourseUiModel>,
    onCourseClick: (HomeCourseUiModel) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "今日课程",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "查看全部",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onViewAllClick() }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (courses.isEmpty()) {
            Text(
                text = "今日暂无课程",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                courses.forEach { course ->
                    CourseListItem(
                        course = course,
                        onClick = { onCourseClick(course) }
                    )
                }
            }
        }
    }
}

@Composable
fun CourseListItem(
    course: HomeCourseUiModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "第${course.startNode}-${course.endNode}节  ${course.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = course.timeDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun PendingTasksSection(
    tasks: List<HomeScheduleUiModel>,
    onTaskClick: (HomeScheduleUiModel) -> Unit,
    onViewAllClick: () -> Unit,
    onToggleComplete: (HomeScheduleUiModel) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "待办任务",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "查看全部",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onViewAllClick() }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (tasks.isEmpty()) {
            Text(
                text = "今日暂无待办",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tasks.forEach { task ->
                    TaskListItem(
                        task = task,
                        onClick = { onTaskClick(task) },
                        onToggleComplete = { onToggleComplete(task) }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskListItem(
    task: HomeScheduleUiModel,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() },
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 12.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (task.isCompleted) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                task.description?.let { description ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = task.timeDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun StudyStatisticsSection(
    studyStat: StudyStat,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "学习统计",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = studyStat.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = studyStat.displayValue,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { (studyStat.value / 20f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}