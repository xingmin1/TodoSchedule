package com.example.todoschedule.ui.task

import androidx.compose.ui.graphics.Color
import com.example.todoschedule.data.database.converter.ScheduleStatus
import com.example.todoschedule.ui.theme.ColorSchemeEnum
import kotlinx.datetime.LocalDateTime

/**
 * TaskScreen 的 UI 状态
 */
data class TaskUiState(
    val isLoading: Boolean = true,
    val tasks: List<TaskItemUiModel> = emptyList(), // 统一的任务列表
    val selectedFilter: TaskFilter = TaskFilter.ALL, // 当前选中的筛选器
    val searchTerm: String = "", // 搜索词
    val errorMessage: String? = null // 错误信息
)

/**
 * 任务筛选类型
 */
enum class TaskFilter {
    ALL, TODAY, WEEK, COMPLETED
}

/**
 * 统一的任务项 UI 模型
 */
sealed class TaskItemUiModel {
    abstract val id: String // 使用 String 避免类型冲突 (e.g., "course_1", "schedule_5")
    abstract val title: String
    abstract val timeDescription: String // e.g., "截止时间：今天 22:00", "第 1-2 节"
    abstract val priorityTag: PriorityTag? // 优先级或状态标签
    abstract val isCompleted: Boolean // 是否已完成 (用于视觉效果和筛选)
    abstract val originalId: Int // 原始 ID，用于导航
    abstract val itemType: TaskItemType // 原始类型，用于导航判断
    abstract val startTime: LocalDateTime // 开始时间
    abstract val endTime: LocalDateTime // 结束时间

    /** 普通日程 */
    data class OrdinaryTask(
        override val id: String,
        override val title: String,
        override val timeDescription: String,
        override val priorityTag: PriorityTag?,
        override val isCompleted: Boolean,
        override val originalId: Int,
        override val startTime: LocalDateTime,
        override val endTime: LocalDateTime,
        val status: ScheduleStatus?, // 添加状态以备后用
        val location: String? = null,
    ) : TaskItemUiModel() {
        override val itemType: TaskItemType = TaskItemType.ORDINARY_SCHEDULE
    }

    /** 课程 */
    data class CourseTask(
        override val id: String,
        override val title: String, // 课程名
        override val timeDescription: String, // 节次和教室
        val courseColor: ColorSchemeEnum?, // 课程颜色
        override val originalId: Int, // Course ID
        override val startTime: LocalDateTime, // 开始时间
        override val endTime: LocalDateTime, // 结束时间
        val courseNodeId: Int? = null, // 可选的 CourseNode ID
        val tableId: Int, // 所属课表ID
    ) : TaskItemUiModel() {
        override val priorityTag: PriorityTag? = null // 课程通常没有优先级标签
        override val isCompleted: Boolean = false // 课程没有完成状态
        override val itemType: TaskItemType = TaskItemType.COURSE
    }
}

/**
 * 优先级/状态标签模型
 */
data class PriorityTag(
    val text: String,
    val backgroundColor: Color,
    val textColor: Color
)

/**
 * 原始任务项类型
 */
enum class TaskItemType {
    ORDINARY_SCHEDULE, COURSE
} 