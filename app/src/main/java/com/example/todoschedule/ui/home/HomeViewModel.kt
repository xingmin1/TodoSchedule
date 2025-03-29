package com.example.todoschedule.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.domain.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/**
 * 首页视图模型
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val courseRepository: CourseRepository
    // 这里需要注入TodoRepository，但目前尚未创建
    // private val todoRepository: TodoRepository
) : ViewModel() {

    // 今日待办列表
    var todoItems by mutableStateOf<List<HomeTodoItem>>(emptyList())
        private set

    // 今日课程列表
    private val _todayCourses = MutableStateFlow<List<HomeCourseItem>>(emptyList())
    val todayCourses: StateFlow<List<HomeCourseItem>> = _todayCourses.asStateFlow()

    init {
        // 加载今日待办（后续需要实现TodoRepository后再完善）
        loadTodoItems()
        
        // 加载今日课程
        viewModelScope.launch {
            // 此处应从CourseRepository获取今日课程
            // 目前使用模拟数据
            loadTodayCourses()
        }
    }
    
    /**
     * 加载今日待办事项
     */
    private fun loadTodoItems() {
        // 模拟待办数据，后续需要从TodoRepository获取
        todoItems = listOf(
            HomeTodoItem(
                id = 1,
                title = "计算机网络作业",
                description = "完成第五章习题",
                isCompleted = false,
                dueTime = LocalTime.of(18, 0)
            ),
            HomeTodoItem(
                id = 2,
                title = "数据结构实验报告",
                description = "提交排序算法分析",
                isCompleted = true,
                dueTime = LocalTime.of(23, 59)
            ),
            HomeTodoItem(
                id = 3,
                title = "小组会议",
                description = "讨论期末项目进度",
                isCompleted = false,
                dueTime = LocalTime.of(16, 30)
            )
        )
    }
    
    /**
     * 加载今日课程
     */
    private fun loadTodayCourses() {
        // 此处应从CourseRepository获取今日课程
        // 目前使用模拟数据
        _todayCourses.value = listOf(
            HomeCourseItem(
                id = 1,
                name = "计算机网络",
                location = "教学楼A404",
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(9, 40)
            ),
            HomeCourseItem(
                id = 2,
                name = "数据结构",
                location = "教学楼B302",
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 40)
            ),
            HomeCourseItem(
                id = 3,
                name = "操作系统",
                location = "教学楼C201",
                startTime = LocalTime.of(14, 0),
                endTime = LocalTime.of(15, 40)
            )
        )
    }
}

/**
 * 首页待办项UI模型
 */
data class HomeTodoItem(
    val id: Int,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val dueTime: LocalTime? = null
)

/**
 * 首页课程项UI模型
 */
data class HomeCourseItem(
    val id: Int,
    val name: String,
    val location: String,
    val startTime: LocalTime,
    val endTime: LocalTime
) 