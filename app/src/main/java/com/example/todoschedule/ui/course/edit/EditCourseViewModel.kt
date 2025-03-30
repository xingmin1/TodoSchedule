package com.example.todoschedule.ui.course.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.model.CourseNode
import com.example.todoschedule.domain.repository.CourseRepository
import com.example.todoschedule.ui.course.add.CourseNodeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 编辑课程事件
 */
sealed class EditCourseEvent {
    data class ShowError(val message: String) : EditCourseEvent()
    object CourseUpdated : EditCourseEvent()
}

/**
 * 编辑课程UI状态
 */
enum class EditCourseUiState {
    Loading,
    Success,
    Error
}

/**
 * 编辑课程ViewModel
 */
@HiltViewModel
class EditCourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow(EditCourseUiState.Loading)
    val uiState: StateFlow<EditCourseUiState> = _uiState

    // 课程ID
    private var courseId = 0

    // 课程基本信息
    private val _courseName = MutableStateFlow("")
    val courseName = _courseName.asStateFlow()

    private val _color = MutableStateFlow("#F44336")
    val color = _color.asStateFlow()

    private val _room = MutableStateFlow("")
    val room = _room.asStateFlow()

    private val _teacher = MutableStateFlow("")
    val teacher = _teacher.asStateFlow()

    private val _credit = MutableStateFlow("")
    val credit = _credit.asStateFlow()

    private val _courseCode = MutableStateFlow("")
    val courseCode = _courseCode.asStateFlow()

    // 课程节点
    private val _courseNodes = MutableStateFlow<List<CourseNodeUiState>>(emptyList())
    val courseNodes = _courseNodes.asStateFlow()

    // 事件通道
    private val _events = Channel<EditCourseEvent>()
    val events = _events.receiveAsFlow()

    /**
     * 加载课程数据
     */
    fun loadCourse(id: Int) {
        courseId = id
        _uiState.value = EditCourseUiState.Loading

        viewModelScope.launch {
            try {
                val course = courseRepository.getCourseById(id)
                if (course != null) {
                    // 更新表单数据
                    _courseName.value = course.courseName
                    _color.value = course.color
                    _room.value = course.room ?: ""
                    _teacher.value = course.teacher ?: ""
                    _credit.value = course.credit?.toString() ?: ""
                    _courseCode.value = course.courseCode ?: ""

                    // 更新课程节点
                    _courseNodes.value = course.nodes.map { node ->
                        CourseNodeUiState(
                            day = node.day,
                            startNode = node.startNode,
                            step = node.step,
                            startWeek = node.startWeek,
                            endWeek = node.endWeek,
                            weekType = node.weekType,
                            room = node.room ?: "",
                            teacher = node.teacher ?: ""
                        )
                    }

                    _uiState.value = EditCourseUiState.Success
                } else {
                    _uiState.value = EditCourseUiState.Error
                    _events.send(EditCourseEvent.ShowError("找不到课程"))
                }
            } catch (e: Exception) {
                _uiState.value = EditCourseUiState.Error
                _events.send(EditCourseEvent.ShowError(e.message ?: "加载课程失败"))
            }
        }
    }

    /**
     * 更新课程
     */
    fun updateCourse() {
        if (!validateForm()) {
            viewModelScope.launch {
                _events.send(EditCourseEvent.ShowError("请填写必要的课程信息"))
            }
            return
        }

        viewModelScope.launch {
            try {
                val course = Course(
                    id = courseId,
                    courseName = _courseName.value,
                    color = _color.value,
                    room = _room.value.takeIf { it.isNotEmpty() },
                    teacher = _teacher.value.takeIf { it.isNotEmpty() },
                    credit = _credit.value.takeIf { it.isNotEmpty() }?.toFloatOrNull(),
                    courseCode = _courseCode.value.takeIf { it.isNotEmpty() },
                    nodes = _courseNodes.value.map { it.toDomain() }
                )

                TODO()
                // courseRepository.updateCourse(course)
                _events.send(EditCourseEvent.CourseUpdated)
            } catch (e: Exception) {
                _events.send(EditCourseEvent.ShowError(e.message ?: "更新课程失败"))
            }
        }
    }

    /**
     * 更新课程名称
     */
    fun updateCourseName(name: String) {
        _courseName.value = name
    }

    /**
     * 更新课程颜色
     */
    fun updateColor(newColor: String) {
        _color.value = newColor
    }

    /**
     * 更新教室
     */
    fun updateRoom(newRoom: String) {
        _room.value = newRoom
    }

    /**
     * 更新教师
     */
    fun updateTeacher(newTeacher: String) {
        _teacher.value = newTeacher
    }

    /**
     * 更新学分
     */
    fun updateCredit(newCredit: String) {
        _credit.value = newCredit
    }

    /**
     * 更新课程代码
     */
    fun updateCourseCode(newCode: String) {
        _courseCode.value = newCode
    }

    /**
     * 添加课程节点
     */
    fun addCourseNode(node: CourseNodeUiState) {
        _courseNodes.value = _courseNodes.value + node
    }

    /**
     * 更新课程节点
     */
    fun updateCourseNode(index: Int, node: CourseNodeUiState) {
        val nodes = _courseNodes.value.toMutableList()
        if (index in nodes.indices) {
            nodes[index] = node
            _courseNodes.value = nodes
        }
    }

    /**
     * 删除课程节点
     */
    fun deleteCourseNode(index: Int) {
        val nodes = _courseNodes.value.toMutableList()
        if (index in nodes.indices) {
            nodes.removeAt(index)
            _courseNodes.value = nodes
        }
    }

    /**
     * 验证表单
     */
    private fun validateForm(): Boolean {
        return _courseName.value.isNotEmpty() && _courseNodes.value.isNotEmpty()
    }

    /**
     * 将CourseNodeUiState转换为CourseNode领域模型
     */
    private fun CourseNodeUiState.toDomain(): CourseNode {
        return CourseNode(
            day = day,
            startNode = startNode,
            step = step,
            startWeek = startWeek,
            endWeek = endWeek,
            weekType = weekType,
            room = room.takeIf { it.isNotEmpty() },
            teacher = teacher.takeIf { it.isNotEmpty() }
        )
    }
} 