package com.example.todoschedule.ui.course.load

// import com.example.todoschedule.ui.course.add.SaveState // 移除旧的 SaveState 引用（如果不再需要）
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.core.extensions.valid
import com.example.todoschedule.domain.model.Course
import com.example.todoschedule.domain.model.Table
import com.example.todoschedule.domain.repository.CourseRepository
import com.example.todoschedule.domain.repository.GlobalSettingRepository
import com.example.todoschedule.domain.repository.TableRepository
import com.example.todoschedule.domain.use_case.auth.GetLoginUserIdFlowUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

// 定义保存状态的密封类
sealed interface SaveCourseState {
    object Idle : SaveCourseState
    object Saving : SaveCourseState
    data class Success(val tableId: UUID) : SaveCourseState
    data class Error(val message: String) : SaveCourseState
}

@HiltViewModel
class WebViewScreenViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val tableRepository: TableRepository,
    private val globalSettingRepository: GlobalSettingRepository,
    private val getLoginUserIdFlowUseCase: GetLoginUserIdFlowUseCase
) : ViewModel() {
    // 使用 StateFlow 管理保存状态
    private val _saveState = MutableStateFlow<SaveCourseState>(SaveCourseState.Idle)
    val saveState: StateFlow<SaveCourseState> = _saveState.asStateFlow()

    /**
     * 保存课程
     */
    fun saveCourse(tableId: UUID, courses: List<Course>) {
        // 检查是否已在保存中，防止重复点击
        if (_saveState.value == SaveCourseState.Saving) {
            return
        }
        _saveState.value = SaveCourseState.Saving // 开始保存，更新状态
        viewModelScope.launch {
            withContext(NonCancellable) {

                try {
                    var actualTableId = tableId
                    val userId = getLoginUserIdFlowUseCase().filterNotNull().first() // 获取用户ID

                    // 检查课表ID是否有效或是否存在
                    val tableExists = tableId != AppConstants.Ids.INVALID_TABLE_ID &&
                            tableRepository.fetchTableById(tableId) != null

                    if (!tableExists) {
                        Log.d("WebViewScreenVM", "课表ID无效或未找到课表，创建默认课表")
                        assert(tableId.valid())
                        val newTable = Table(
                            id = tableId,
                            userId = userId,
                            tableName = AppConstants.Database.DEFAULT_TABLE_NAME, // 可以考虑让用户在导入前/后命名
                            startDate = AppConstants.Database.DEFAULT_TABLE_START_DATE,
                        )
                        Log.d("WebViewScreenVM", "Creating new table: $newTable")
                        actualTableId = tableRepository.addTable(newTable)
                        Log.d("WebViewScreenVM", "New table created with ID: $actualTableId")
                    } else {
                        Log.d("WebViewScreenVM", "Using existing table ID: $actualTableId")
                    }

                    // 确保我们有一个有效的课表 ID
                    if (actualTableId == AppConstants.Ids.INVALID_TABLE_ID) {
                        throw IllegalStateException("无法获取有效的课表ID进行课程保存")
                    }

                    Log.d("WebViewScreenVM", "Adding ${courses.size} courses to table ID: $actualTableId")
                    courseRepository.addCourses(courses, actualTableId)
                    Log.d("WebViewScreenVM", "Courses added successfully to table ID: $actualTableId")

                    // 将此课表设为默认
                    try {
                        globalSettingRepository.updateDefaultTableIds(userId, listOf(actualTableId))
                        Log.i("WebViewScreenVM", "Set table $actualTableId as default for user $userId")
                    } catch (settingError: Exception) {
                        Log.e(
                            "WebViewScreenVM",
                            "Failed to set table $actualTableId as default",
                            settingError
                        )
                        // 这里失败不应阻止导入成功，但需要记录日志
                    }

                    _saveState.value = SaveCourseState.Success(actualTableId) // 保存成功，更新状态
                } catch (e: Exception) {
                    Log.e("WebViewScreenVM", "Error saving course: ${e.message}", e)
                    _saveState.value = SaveCourseState.Error(e.message ?: "保存失败：${e.javaClass.simpleName}") // 保存失败，更新状态
                }
            }
        }
    }

    // 可以添加一个方法用于重置状态，例如用户关闭错误提示后
    fun resetSaveState() {
         _saveState.value = SaveCourseState.Idle
    }
}
