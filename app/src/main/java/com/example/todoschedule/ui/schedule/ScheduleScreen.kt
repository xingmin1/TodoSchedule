package com.example.todoschedule.ui.schedule

import android.annotation.SuppressLint
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.model.CourseNode
import com.example.todoschedule.ui.navigation.NavigationState
import com.example.todoschedule.ui.schedule.model.ScheduleUiState
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import androidx.core.graphics.toColorInt

/**
 * 课程表屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navigationState: NavigationState,
    onNavigateToSettings: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    // 状态
    val uiState by viewModel.uiState.collectAsState()
    val currentWeek by viewModel.currentWeek.collectAsState()
    val weekDates by viewModel.weekDates.collectAsState()
    val weekCourses by viewModel.weekCourses.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("第${currentWeek}周") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // 上一周
                    IconButton(onClick = { viewModel.previousWeek() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上一周")
                    }
                    
                    // 回到当前周
                    IconButton(onClick = { viewModel.goToCurrentWeek() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "回到本周")
                    }
                    
                    // 下一周
                    IconButton(onClick = { viewModel.nextWeek() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下一周")
                    }
                    
                    // 设置
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navigationState.navigateToAddCourse() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加课程")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                ScheduleUiState.Loading -> LoadingScreen()
                is ScheduleUiState.Success -> {
                    if (weekCourses.isEmpty()) {
                        EmptyScheduleScreen()
                    } else {
                        ScheduleContent(
                            weekDates = weekDates,
                            weekCourses = weekCourses,
                            onCourseClick = { navigationState.navigateToCourseDetail(it) }
                        )
                    }
                }
                is ScheduleUiState.Error -> ErrorScreen((uiState as ScheduleUiState.Error).message)
                ScheduleUiState.Empty -> TODO()
            }
        }
    }
}

/**
 * 加载中屏幕
 */
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

/**
 * 空课表屏幕
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
 * 错误屏幕
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

/**
 * 课表内容
 */
@Composable
fun ScheduleContent(
    weekDates: List<LocalDate>,
    weekCourses: List<Course>,
    onCourseClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 周日期头部
        WeekHeader(weekDates = weekDates)
        
        // 课程表格
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            // 时间列
            TimeColumn()
            
            // 课程网格
            CourseGrid(
                courses = weekCourses,
                onCourseClick = onCourseClick
            )
        }
    }
}

/**
 * 周日期头部
 */
@Composable
fun WeekHeader(weekDates: List<LocalDate>) {
    val today = LocalDate.now()
    
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        // 时间列占位
        item {
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .padding(horizontal = 2.dp)
            )
        }
        
        // 日期项
        items(weekDates) { date ->
            val isToday = date == today
            val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 2.dp),
            ) {
                // 星期
                Text(
                    text = dayOfWeek,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                
                // 日期
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent)
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * 时间列
 */
@Composable
fun TimeColumn() {
    val times = listOf(
        "8:00", "9:00", "10:00", "11:00", "12:00",
        "13:00", "14:00", "15:00", "16:00", "17:00",
        "18:00", "19:00", "20:00"
    )
    
    Column(
        modifier = Modifier
            .width(30.dp)
            .padding(end = 2.dp)
    ) {
        times.forEachIndexed { index, time ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(60.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = (index + 1).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 课程网格
 */
@Composable
fun CourseGrid(
    courses: List<Course>,
    onCourseClick: (Int) -> Unit
) {
    // 为每一天创建一列
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 创建7列（周一到周日）
        for (day in 1..7) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 过滤出当天的课程节点
                val nodesToday = courses.flatMap { course ->
                    course.nodes
                        .filter { it.day == day }
                        .map { node -> Pair(course, node) }
                }
                
                // 用于跟踪已渲染的时间槽
                val renderedSlots = mutableSetOf<Int>()
                
                // 遍历每个时间槽 (1-12)
                for (slot in 1..12) {
                    // 检查是否已经渲染过这个时间槽
                    if (slot in renderedSlots) continue
                    
                    // 查找在这个时间槽开始的课程
                    val courseAtSlot = nodesToday.find { (_, node) -> node.startNode == slot }
                    
                    if (courseAtSlot != null) {
                        // 有课程，渲染课程卡片
                        val (course, node) = courseAtSlot
                        CourseCard(
                            course = course,
                            node = node,
                            onClick = { onCourseClick(course.id) }
                        )
                        
                        // 标记课程占用的所有时间槽为已渲染
                        for (i in node.startNode until node.startNode + node.step) {
                            renderedSlots.add(i)
                        }
                    } else {
                        // 没有课程，渲染空时间槽
                        EmptyTimeSlot()
                        renderedSlots.add(slot)
                    }
                }
            }
        }
    }
}

/**
 * 空时间槽
 */
@Composable
fun EmptyTimeSlot() {
    Box(
        modifier = Modifier
            .height(60.dp)
            .padding(1.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            )
    )
}

/**
 * 课程卡片
 */
@SuppressLint("UseKtx")
@Composable
fun CourseCard(
    course: Course,
    node: CourseNode,
    onClick: () -> Unit
) {
    val cardColor = try {
        Color(course.color.toColorInt())
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }
    
    val textColor = if (isColorDark(cardColor)) {
        Color.White
    } else {
        Color.Black
    }
    
    Card(
        modifier = Modifier
            .height(60.dp * node.step)
            .padding(1.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // 课程名称
            Text(
                text = course.courseName,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // 如果有足够的空间，显示教室
            if (node.step >= 2 && !node.room.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = node.room,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 判断颜色是否为深色
 */
fun isColorDark(color: Color): Boolean {
    val darkness = 1 - (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
    return darkness > 0.5
}

@Preview(showBackground = true)
@Composable
fun PreviewScheduleScreen() {
    val viewModel = hiltViewModel<ScheduleViewModel>()
    val navController = rememberNavController()
    val navigationState = remember(navController) {
        NavigationState(navController)
    }
    ScheduleScreen(navigationState, {}, viewModel)
}

@Preview(showBackground = true)
@Composable
fun PreviewScheduleContent() {
    // 示例数据
    val monday = LocalDate.of(2025, 3, 31)
    val weekDates = listOf(
        monday,
        monday.plusDays(1),
        monday.plusDays(2),
        monday.plusDays(3),
        monday.plusDays(4),
        monday.plusDays(5),
        monday.plusDays(6)
    )
    val weekCourses = listOf<Course>(
        Course(
            id = 1,
            courseName = "数学",
            nodes = listOf(CourseNode(
                day = 1, startNode = 1, step = 2, room = "教室A", startWeek = 1, endWeek = 16,
                weekType = 1, teacher = "张老师"
            ),
            CourseNode(
                day = 4, startNode = 1, step = 2, room = "教室A", startWeek = 1, endWeek = 16,
                weekType = 1, teacher = "张老师"
            )
            ),
            color = "#FF5733"
        ),
        Course(
            id = 2,
            courseName = "英语",
            nodes = listOf(CourseNode(day = 1, startNode = 3, step = 2, room = "教室B", startWeek = 1, endWeek = 16, weekType = 1, teacher = "李老师")),
            color = "#33FF57"
        ),
        Course(
            id = 3,
            courseName = "物理",
            nodes = listOf(CourseNode(day = 1, startNode = 5, step = 2, room = "教室C", startWeek = 1, endWeek = 16, weekType = 1, teacher = "王老师")),
            color = "#3357FF"
        ),
        Course(
            id = 4,
            courseName = "化学",
            nodes = listOf(CourseNode(day = 1, startNode = 7, step = 2, room = "教室D", startWeek = 1, endWeek = 16, weekType = 1, teacher = "赵老师")),
            color = "#FF33A1"
        ),
        Course(
            id = 5,
            courseName = "生物",
            nodes = listOf(CourseNode(day = 1, startNode = 9, step = 2, room = "教室E", startWeek = 1, endWeek = 16, weekType = 1, teacher = "孙老师")),
            color = "#A133FF"
        ),
        Course(
            id = 6,
            courseName = "地理",
            nodes = listOf(
                CourseNode(day = 1, startNode = 11, step = 2, room = "教室F", startWeek = 1, endWeek = 16, weekType = 1, teacher = "周老师"),
                CourseNode(day = 5, startNode = 11, step = 2, room = "教室F", startWeek = 1, endWeek = 16, weekType = 1, teacher = "周老师")
            ),
            color = "#FFA133"
        ),
        Course(
            id = 7,
            courseName = "历史",
            nodes = listOf(CourseNode(day = 2, startNode = 1, step = 2, room = "教室G", startWeek = 1, endWeek = 16, weekType = 1, teacher = "吴老师")),
            color = "#33A1FF"
        ),
        Course(
            id = 8,
            courseName = "政治",
            nodes = listOf(CourseNode(day = 2, startNode = 3, step = 2, room = "教室H", startWeek = 1, endWeek = 16, weekType = 1, teacher = "钱老师")),
            color = "#A1FF33"
        ),
        Course(
            id = 9,
            courseName = "音乐",
            nodes = listOf(CourseNode(day = 2, startNode = 5, step = 2, room = "教室I", startWeek = 1, endWeek = 16, weekType = 1, teacher = "孙老师")),
            color = "#FF33A1"
        ),
        Course(
            id = 10,
            courseName = "美术",
            nodes = listOf(CourseNode(day = 2, startNode = 7, step = 2, room = "教室J", startWeek = 1, endWeek = 16, weekType = 1, teacher = "周老师")),
            color = "#33A1FF"
        ),
        Course(
            id = 11,
            courseName = "信息技术",
            nodes = listOf(CourseNode(day = 2, startNode = 9, step = 2, room = "教室K", startWeek = 1, endWeek = 16, weekType = 1, teacher = "钱老师")),
            color = "#A1FF33"
        ),
        Course(
            id = 12,
            courseName = "体育",
            nodes = listOf(CourseNode(day = 3, startNode = 1, step = 2, room = "教室L", startWeek = 1, endWeek = 16, weekType = 1, teacher = "孙老师")),
            color = "#33FF57"
        ),
    )
    ScheduleContent(
        weekDates = weekDates,
        weekCourses = weekCourses,
    ) { }
}

