package com.example.todoschedule.ui.task

import android.util.Log
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.todoschedule.domain.utils.CalendarUtils
import com.example.todoschedule.ui.navigation.NavigationState
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Main Composable for the Task Screen
@Composable
fun TaskScreen(
    paddingValues: PaddingValues,
    viewModel: TaskViewModel = hiltViewModel(),
    navigationState: NavigationState // Need navigationState for actions
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSearch by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) } // For the main task menu

    Scaffold(
        topBar = {
            TaskTopAppBar(
                showSearch = showSearch,
                onSearchClick = { showSearch = !showSearch },
                onMenuClick = { showMenu = !showMenu }, // Toggle menu visibility
                onSearchQueryChanged = { viewModel.updateSearchTerm(it) },
                onCloseSearch = {
                    showSearch = false
                    viewModel.updateSearchTerm("") // Clear search on close
                },
                searchText = uiState.searchTerm
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navigationState.navigateToAddEditOrdinarySchedule() }, // Navigate to add task screen
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加任务", tint = Color.White)
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        modifier = Modifier.padding(paddingValues) // Apply padding from the outer Scaffold
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Apply padding from this Scaffold
                .background(MaterialTheme.colorScheme.background) // Use theme background
        ) {
            // Filter Tabs
            TaskFilterTabs(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { viewModel.updateFilter(it) }
            )

            // Task List
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("错误: ${uiState.errorMessage}", color = MaterialTheme.colorScheme.error)
                }
            } else {
                TaskList(
                    tasks = uiState.tasks,
                    onTaskClick = { task ->
                        // Navigate based on task type
                        when (task.itemType) {
                            TaskItemType.ORDINARY_SCHEDULE -> {
                                navigationState.navigateToOrdinaryScheduleDetail(task.originalId)
                            }

                            TaskItemType.COURSE -> {
                                if (task is TaskItemUiModel.CourseTask) {
                                    navigationState.navigateToCourseDetail(
                                        task.tableId,
                                        task.originalId
                                    )
                                }
                            }
                        }
                    },
                    onToggleComplete = { task ->
                        if (task is TaskItemUiModel.OrdinaryTask) {
                            viewModel.toggleTaskComplete(task)
                        }
                        // CourseTask completion is not handled
                    }
                )
            }

            // --- Task Menu (Dropdown) ---
            // TODO: Implement the actual dropdown menu logic later
            // This is just a placeholder to show where it would go conceptually
            if (showMenu) {
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                    // modifier = Modifier.align(Alignment.End) // Adjust position
                ) {
                    DropdownMenuItem(
                        text = { Text("排序方式") },
                        onClick = { /* Handle sort */ showMenu = false })
                    DropdownMenuItem(
                        text = { Text("筛选任务") },
                        onClick = { /* Handle filter */ showMenu = false })
                    DropdownMenuItem(
                        text = { Text("批量编辑") },
                        onClick = { /* Handle bulk edit */ showMenu = false })
                    DropdownMenuItem(
                        text = { Text("任务设置") },
                        onClick = { /* Handle settings */ showMenu = false })
                }
            }
        }
    }
}

// Top App Bar for Task Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTopAppBar(
    showSearch: Boolean,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onCloseSearch: () -> Unit,
    searchText: String
) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) { // Use surface variant for slight contrast
        TopAppBar(
            title = { if (!showSearch) Text("任务", fontWeight = FontWeight.SemiBold) },
            actions = {
                // Search Icon or Search Input
                if (!showSearch) {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索任务")
                    }
                }
                // Menu Icon
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "更多选项")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent, // Make TopAppBar transparent
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // AnimatedVisibility can be used for smoother transition later
        if (showSearch) {
            OutlinedTextField( // Use OutlinedTextField for better styling
                value = searchText,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索任务...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = onCloseSearch) {
                        Icon(Icons.Filled.Close, contentDescription = "关闭搜索")
                    }
                },
                shape = CircleShape, // Rounded shape like in the design
                singleLine = true,
                colors = TextFieldDefaults.colors( // Customize colors
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent, // Hide indicator
                    unfocusedIndicatorColor = Color.Transparent, // Hide indicator
                    disabledIndicatorColor = Color.Transparent // Hide indicator
                )
            )
        }
    }
}

// Filter Tabs Composable
@Composable
fun TaskFilterTabs(
    selectedFilter: TaskFilter,
    onFilterSelected: (TaskFilter) -> Unit
) {
    val filters = TaskFilter.entries.toTypedArray()
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface) // Background for the tab area
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            val isSelected = filter == selectedFilter
            Button(
                onClick = { onFilterSelected(filter) },
                shape = CircleShape, // Use CircleShape for rounded buttons
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ), // Adjust padding
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp) // Remove shadow
            ) {
                Text(
                    text = when (filter) {
                        TaskFilter.ALL -> "全部"
                        TaskFilter.TODAY -> "今日"
                        TaskFilter.WEEK -> "本周"
                        TaskFilter.COMPLETED -> "已完成"
                    },
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

// Task List Composable using LazyColumn
@Composable
fun TaskList(
    tasks: List<TaskItemUiModel>,
    onTaskClick: (TaskItemUiModel) -> Unit,
    onToggleComplete: (TaskItemUiModel) -> Unit
) {
    // Group tasks (Simplified grouping for now - Today, Week, Completed, Others)
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val weekDates =
        CalendarUtils.getWeekDates(CalendarUtils.getCurrentWeek()) // TODO: Needs table start date
    val weekStart = weekDates.first()
    val weekEnd = weekDates.last()
    Log.d("TaskList", "Week Start: $weekStart, Week End: $weekEnd")

    val groupedTasks = tasks.groupBy { task ->
        when (task) {
            is TaskItemUiModel.OrdinaryTask -> {
                if (task.isCompleted) TaskGroup.COMPLETED
                else {
                    val startTime = task.startTime
                    val endTime = task.endTime

                    when {
                        startTime == today || endTime == today -> TaskGroup.TODAY
                        startTime.date in weekStart..weekEnd || endTime.date in weekStart..weekEnd
                            -> TaskGroup.WEEK

                        else -> {
                            Log.d(
                                "TaskList",
                                "Task not today or this week: ${task.title} - $startTime"
                            )
                            TaskGroup.OTHER
                        } // For tasks not today, not this week, not completed
                    }
                }
            }

            is TaskItemUiModel.CourseTask -> {
                val nodeDay =
                    task.id.substringAfter("course_").substringAfter("_").substringBefore("_")
                        .toIntOrNull() ?: -1
                val courseDate = if (nodeDay in 1..7) weekDates.getOrNull(nodeDay - 1) else null
                when {
                    courseDate == today -> TaskGroup.TODAY
                    courseDate != null -> TaskGroup.WEEK // Courses in the current week
                    else -> TaskGroup.OTHER // Courses not today or this week (shouldn't happen often with current logic)
                }
            }
        }
    }.map {
        it.key to it.value.sortedBy { task ->
            task.startTime
        }
    }.toMap()

    // Define the order of groups
    val groupOrder = listOf(TaskGroup.TODAY, TaskGroup.WEEK, TaskGroup.OTHER, TaskGroup.COMPLETED)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp) // Space between groups
    ) {
        groupOrder.forEach { group ->
            val groupTasks = groupedTasks[group]
            if (!groupTasks.isNullOrEmpty()) {
                item {
                    TaskGroupHeader(group.title, groupTasks.size)
                }
                items(groupTasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        onClick = { onTaskClick(task) },
                        onToggleComplete = { onToggleComplete(task) }
                    )
                }
            }
        }
    }
}

enum class TaskGroup(val title: String) {
    TODAY("今日任务"),
    WEEK("本周任务"),
    COMPLETED("已完成"),
    OTHER("其他任务") // Fallback group
}


@Composable
fun TaskGroupHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp), // Space below header
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall, // Adjust style as needed
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = count.toString(), // Display count, potentially completed/total later
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// Individual Task Item Composable
@Composable
fun TaskItem(
    task: TaskItemUiModel,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp), // More rounded corners
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Subtle shadow
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // Use surface color
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp) // Adjust padding
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Checkbox and Text Content
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Show Checkbox only for Ordinary Tasks
                if (task is TaskItemUiModel.OrdinaryTask) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onToggleComplete() },
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 12.dp), // Add padding after checkbox
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                } else if (task is TaskItemUiModel.CourseTask) {
                    // Placeholder for course icon or color indicator?
                    Spacer(modifier = Modifier.width(36.dp)) // Keep alignment consistent
                }

                // Task Title and Subtitle
                Column(modifier = Modifier.padding(end = 8.dp)) { // Add padding before tag
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.timeDescription,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Priority Tag (Optional)
            task.priorityTag?.let { tag ->
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(tag.backgroundColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = tag.text,
                        color = tag.textColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}