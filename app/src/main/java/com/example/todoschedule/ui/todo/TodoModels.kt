package com.example.todoschedule.ui.todo

import java.time.LocalDate

/**
 * 待办UI模型
 */
data class TodoUiModel(
    val id: Int,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val dueDate: LocalDate? = null,
    val priority: TodoPriority = TodoPriority.NORMAL,
    val category: String? = null
)

/**
 * 待办优先级
 */
enum class TodoPriority {
    LOW,
    NORMAL,
    HIGH
}

/**
 * 待办UI状态
 */
sealed class TodoUiState {
    object Loading : TodoUiState()
    object Empty : TodoUiState()
    data class Success(val todos: List<TodoUiModel>) : TodoUiState()
    data class Error(val message: String) : TodoUiState()
} 