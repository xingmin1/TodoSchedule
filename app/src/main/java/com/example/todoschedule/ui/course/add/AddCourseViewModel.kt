package com.example.todoschedule.ui.course.add

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.model.CourseNode
import com.example.todoschedule.domain.model.Table
import com.example.todoschedule.domain.repository.CourseRepository
import com.example.todoschedule.domain.repository.TableRepository
import com.example.todoschedule.domain.use_case.auth.GetLoginUserIdFlowUseCase
import com.example.todoschedule.ui.theme.ColorSchemeEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * 添加课程ViewModel
 */
@HiltViewModel
class AddCourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val tableRepository: TableRepository,
    private val getLoginUserIdFlowUseCase: GetLoginUserIdFlowUseCase
) : ViewModel() {

    // 课程名称
    private val _courseName = MutableStateFlow("")
    val courseName: StateFlow<String> = _courseName.asStateFlow()

    // 课程颜色
    private val _color = MutableStateFlow<ColorSchemeEnum>(AppConstants.DEFAULT_COURSE_COLOR)
    val color: StateFlow<ColorSchemeEnum> = _color.asStateFlow()

    // 教室
    private val _room = MutableStateFlow("")
    val room: StateFlow<String> = _room.asStateFlow()

    // 教师
    private val _teacher = MutableStateFlow("")
    val teacher: StateFlow<String> = _teacher.asStateFlow()

    // 学分
    private val _credit = MutableStateFlow("")
    val credit: StateFlow<String> = _credit.asStateFlow()

    // 课程代码
    private val _courseCode = MutableStateFlow("")
    val courseCode: StateFlow<String> = _courseCode.asStateFlow()

    // 上课时间节点
    private val _courseNodes = MutableStateFlow<List<CourseNodeUiState>>(emptyList())
    val courseNodes: StateFlow<List<CourseNodeUiState>> = _courseNodes.asStateFlow()

    // 保存状态
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    /**
     * 重置所有信息
     */
    fun resetAllFields() {
        _courseName.value = ""
        _color.value = AppConstants.DEFAULT_COURSE_COLOR
        _room.value = ""
        _teacher.value = ""
        _credit.value = ""
        _courseCode.value = ""
        _courseNodes.value = emptyList()
        resetSaveState()
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
    fun updateColor(color: ColorSchemeEnum) {
        _color.value = color
    }

    /**
     * 更新教室
     */
    fun updateRoom(room: String) {
        _room.value = room
    }

    /**
     * 更新教师
     */
    fun updateTeacher(teacher: String) {
        _teacher.value = teacher
    }

    /**
     * 更新学分
     */
    fun updateCredit(credit: String) {
        _credit.value = credit
    }

    /**
     * 更新课程代码
     */
    fun updateCourseCode(code: String) {
        _courseCode.value = code
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
     * 删除全部课程节点
     */
    fun deleteAllCourseNodes() {
        _courseNodes.value = listOf()
    }

    /**
     * 保存课程
     */
    fun saveCourse(tableId: UUID) {
        if (!validateForm()) {
            _saveState.value = SaveState.Error("请填写必要的课程信息")
            return
        }

        viewModelScope.launch {
            _saveState.value = SaveState.Loading

            try {
                var actualTableId = tableId
                // 首先检查课表ID是否有效
                if (tableId == AppConstants.Ids.INVALID_TABLE_ID || tableRepository.fetchTableById(
                        tableId
                    ) == null
                ) {
                    Log.e("AddCourseViewModel", "课表ID无效或未找到课表，创建默认课表")
                    val currentUserId = getLoginUserIdFlowUseCase().value
                    if (currentUserId == null) {
                        _saveState.value = SaveState.Error("用户未登录，无法创建课表")
                        return@launch
                    }
                    val userId = currentUserId
                    // 创建默认课表并获取新ID                    // 创建默认课表并获取新ID
                    val newTable = Table(
                        id = UUID.randomUUID(),
                        userId = userId,
                        tableName = AppConstants.Database.DEFAULT_TABLE_NAME,
                        startDate = AppConstants.Database.DEFAULT_TABLE_START_DATE,
                    )
                    Log.d("AddCourseViewModel", "Creating new table: $newTable")
                    actualTableId = tableRepository.addTable(newTable)
                }

                val course = Course(
                    id = UUID.randomUUID(),
                    courseName = _courseName.value,
                    color = _color.value,
                    room = _room.value.takeIf { it.isNotEmpty() },
                    teacher = _teacher.value.takeIf { it.isNotEmpty() },
                    credit = _credit.value.takeIf { it.isNotEmpty() }?.toFloatOrNull(),
                    courseCode = _courseCode.value.takeIf { it.isNotEmpty() },
                    nodes = _courseNodes.value.map { it.toDomain() }
                )

                val courseId = courseRepository.addCourse(course, actualTableId)
                Log.d("TableId", actualTableId.toString())
                _saveState.value = SaveState.Success(courseId)
            } catch (e: Exception) {
                _saveState.value =
                    SaveState.Error(e.message ?: "保存失败：${e.javaClass.simpleName}")
            }
        }
    }


    /**
     * 验证表单
     */
    private fun validateForm(): Boolean {
        return _courseName.value.isNotEmpty() && _courseNodes.value.isNotEmpty()
    }

    /**
     * 重置保存状态
     */
    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
}

/**
 * CourseNodeUiState 的扩展函数，用于转换为领域模型
 */
fun CourseNodeUiState.toDomain(): CourseNode {
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

/**
 * 保存状态
 */
sealed class SaveState {
    object Idle : SaveState()
    object Loading : SaveState()
    data class Success(val courseId: UUID) : SaveState()
    data class Error(val message: String) : SaveState()
}
