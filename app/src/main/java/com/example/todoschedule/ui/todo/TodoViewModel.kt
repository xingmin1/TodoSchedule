package com.example.todoschedule.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * 待办页面ViewModel
 */
@HiltViewModel
class TodoViewModel @Inject constructor() : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow<TodoUiState>(TodoUiState.Loading)
    val uiState: StateFlow<TodoUiState> = _uiState.asStateFlow()

    // 选中日期
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // 假数据
    private val mockTodos = listOf(
        TodoUiModel(
            id = 1,
            title = "完成数学作业",
            description = "第三章习题1-10",
            isCompleted = false,
            dueDate = LocalDate.now()
        ),
        TodoUiModel(
            id = 2,
            title = "预习英语课程",
            description = "阅读第四章并做笔记",
            isCompleted = true,
            dueDate = LocalDate.now().plusDays(1)
        ),
        TodoUiModel(
            id = 3,
            title = "小组会议",
            description = "讨论项目进展",
            isCompleted = false,
            dueDate = LocalDate.now().plusDays(2)
        )
    )

    init {
        loadTodos()
    }

    /**
     * 加载待办事项
     */
    private fun loadTodos() {
        viewModelScope.launch {
            // 模拟加载延迟
            kotlinx.coroutines.delay(500)

            // 过滤选中日期的待办事项
            val todosForSelectedDate = mockTodos.filter { todo ->
                todo.dueDate?.isEqual(_selectedDate.value) ?: false
            }

            _uiState.value = if (todosForSelectedDate.isEmpty()) {
                TodoUiState.Empty
            } else {
                TodoUiState.Success(todosForSelectedDate)
            }
        }
    }

    /**
     * 选择日期
     */
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        loadTodos()
    }

    /**
     * 切换待办事项完成状态
     */
    fun toggleTodoComplete(todoId: Int) {
        viewModelScope.launch {
            val updatedTodos = (uiState.value as? TodoUiState.Success)?.todos?.map { todo ->
                if (todo.id == todoId) {
                    todo.copy(isCompleted = !todo.isCompleted)
                } else {
                    todo
                }
            } ?: return@launch

            if (updatedTodos.isEmpty()) {
                _uiState.value = TodoUiState.Empty
            } else {
                _uiState.value = TodoUiState.Success(updatedTodos)
            }
        }
    }

    /**
     * 删除待办事项
     */
    fun deleteTodo(todoId: Int) {
        viewModelScope.launch {
            val updatedTodos = (uiState.value as? TodoUiState.Success)?.todos?.filter {
                it.id != todoId
            } ?: return@launch

            if (updatedTodos.isEmpty()) {
                _uiState.value = TodoUiState.Empty
            } else {
                _uiState.value = TodoUiState.Success(updatedTodos)
            }
        }
    }
} 