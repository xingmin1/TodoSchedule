package com.example.todoschedule.ui.course.load

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.model.Table
import com.example.todoschedule.domain.repository.CourseRepository
import com.example.todoschedule.domain.repository.TableRepository
// import com.example.todoschedule.ui.course.add.SaveState // 移除旧的 SaveState 引用（如果不再需要）
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 定义保存状态的密封类
sealed interface SaveCourseState {
    object Idle : SaveCourseState
    object Saving : SaveCourseState
    data class Success(val tableId: Int) : SaveCourseState
    data class Error(val message: String) : SaveCourseState
}

@HiltViewModel
class WebViewScreenViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val tableRepository: TableRepository
) : ViewModel() {

    // 使用 StateFlow 管理保存状态
    private val _saveState = MutableStateFlow<SaveCourseState>(SaveCourseState.Idle)
    val saveState: StateFlow<SaveCourseState> = _saveState.asStateFlow()

    /**
     * 保存课程
     */
    fun saveCourse(tableId: Int, courses: List<Course>) {
        // 检查是否已在保存中，防止重复点击
        if (_saveState.value == SaveCourseState.Saving) {
            return
        }
        _saveState.value = SaveCourseState.Saving // 开始保存，更新状态
        viewModelScope.launch {
            try {
                var actualTableId = tableId
                // 首先检查课表ID是否有效
                if (tableId == AppConstants.Ids.INVALID_TABLE_ID || tableRepository.fetchTableById(
                        tableId
                    ) == null
                ) {
                    Log.d("WebViewScreenVM", "课表ID无效或未找到课表，创建默认课表")
                    // 创建默认课表并获取新ID
                    val newTable = Table(
                        tableName = AppConstants.Database.DEFAULT_TABLE_NAME,
                        startDate = AppConstants.Database.DEFAULT_TABLE_START_DATE,
                    )
                    Log.d("WebViewScreenVM", "Creating new table: $newTable")
                    actualTableId = tableRepository.addTable(newTable).toInt()
                    Log.d("WebViewScreenVM", "New table created with ID: $actualTableId")
                } else {
                     Log.d("WebViewScreenVM", "Using existing table ID: $actualTableId")
                }

                Log.d("WebViewScreenVM", "Adding ${courses.size} courses to table ID: $actualTableId")
                courseRepository.addCourses(courses, actualTableId) // 注意：addCourses 应该也返回一些状态或确认
                Log.d("WebViewScreenVM", "Courses added successfully to table ID: $actualTableId")
                _saveState.value = SaveCourseState.Success(actualTableId) // 保存成功，更新状态
            } catch (e: Exception) {
                 Log.e("WebViewScreenVM", "Error saving course: ${e.message}", e)
                _saveState.value = SaveCourseState.Error(e.message ?: "保存失败：${e.javaClass.simpleName}") // 保存失败，更新状态
            }
        }
    }

    // 可以添加一个方法用于重置状态，例如用户关闭错误提示后
    fun resetSaveState() {
         _saveState.value = SaveCourseState.Idle
    }
}
