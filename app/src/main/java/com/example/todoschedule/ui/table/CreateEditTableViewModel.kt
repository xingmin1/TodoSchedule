package com.example.todoschedule.ui.table

// TODO: 可能需要 GlobalSettingRepository 来更新默认课表 ID
// import com.example.todoschedule.domain.repository.GlobalSettingRepository
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.domain.model.Table
import com.example.todoschedule.domain.repository.GlobalSettingRepository
import com.example.todoschedule.domain.repository.SessionRepository
import com.example.todoschedule.domain.repository.TableRepository
import com.example.todoschedule.ui.navigation.AppRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import javax.inject.Inject

@HiltViewModel
class CreateEditTableViewModel @Inject constructor(
    private val tableRepository: TableRepository,
    private val sessionRepository: SessionRepository,
    private val globalSettingRepository: GlobalSettingRepository, // 启用注入
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEditTableUiState())
    val uiState: StateFlow<CreateEditTableUiState> = _uiState.asStateFlow()

    // 从导航参数获取 tableId 用于编辑模式
    private val tableIdFromNav: Int? =
        savedStateHandle.get<Int>(AppRoutes.CreateEditTable.ARG_TABLE_ID)
            ?.takeIf { it != -1 } // 如果 ID 不是默认值 -1，则保留，否则为 null

    init {
        if (tableIdFromNav != null) {
            loadTableForEditing(tableIdFromNav)
        } else {
            // 创建模式
        }
    }

    private fun loadTableForEditing(tableId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, tableId = tableId) } // 设置 ID 并标记加载中
            val tableFlow = tableRepository.getTableById(tableId)
            tableFlow.firstOrNull()?.let { table ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tableName = table.tableName,
                        startDate = table.startDate,
                        totalWeeks = table.totalWeeks.toString(),
                        background = table.background
                    )
                }
            } ?: run {
                // 处理未找到课表的情况
                _uiState.update { it.copy(isLoading = false, errorMessage = "无法加载课表信息") }
            }
        }
    }

    fun onTableNameChange(name: String) {
        _uiState.update { it.copy(tableName = name, errorMessage = null) }
    }

    fun onStartDateSelected(date: LocalDate?) {
        _uiState.update {
            it.copy(
                startDate = date,
                showStartDatePicker = false,
                errorMessage = null
            )
        }
    }

    fun onTotalWeeksChange(weeks: String) {
        _uiState.update { it.copy(totalWeeks = weeks, errorMessage = null) }
    }

    fun onTermsChange(terms: String) {
        _uiState.update { it.copy(terms = terms, errorMessage = null) }
    }

    fun onBackgroundChange(background: String) {
        // TODO: 实现背景选择逻辑 (可能需要颜色选择器或图片选择器)
        // 暂时只更新状态
        _uiState.update { it.copy(background = background, errorMessage = null) }
    }

    fun showStartDatePicker() {
        _uiState.update { it.copy(showStartDatePicker = true) }
    }

    fun dismissStartDatePicker() {
        _uiState.update { it.copy(showStartDatePicker = false) }
    }

    fun saveTable() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val userId = sessionRepository.currentUserIdFlow.first()

            if (userId == null || userId == -1L) {
                _uiState.update { it.copy(errorMessage = "无法获取用户信息，请重新登录") }
                return@launch
            }
            if (currentState.tableName.isBlank()) {
                _uiState.update { it.copy(errorMessage = "课表名称不能为空") }
                return@launch
            }
            if (currentState.startDate == null) {
                _uiState.update { it.copy(errorMessage = "请选择学期开始日期") }
                return@launch
            }
            val totalWeeksInt = currentState.totalWeeks.toIntOrNull()
            if (totalWeeksInt == null || totalWeeksInt <= 0) {
                _uiState.update { it.copy(errorMessage = "总周数必须是正整数") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val table = Table(
                id = currentState.tableId ?: 0,
                userId = userId.toInt(),
                tableName = currentState.tableName,
                startDate = currentState.startDate,
                totalWeeks = totalWeeksInt,
                background = currentState.background ?: "",
                // listPosition 需要在获取列表时或保存后设置
            )

            try {
                if (currentState.isEditMode) {
                    // 确保 ID 正确传递
                    val tableToUpdate = table.copy(id = currentState.tableId!!)
                    // 校验 userId
                    val currentUserId = sessionRepository.currentUserIdFlow.first()
                    if (currentUserId == null || currentUserId.toInt() != tableToUpdate.userId) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "无法更新不属于当前用户的课表"
                            )
                        }
                        return@launch
                    }

                    tableRepository.updateTable(tableToUpdate)
                    Log.d("CreateTableVM", "Updating table: ${tableToUpdate.id}")
                    _uiState.update { it.copy(isLoading = false, isSaved = true) } // 更新成功

                } else {
                    val newTableId = tableRepository.addTable(table)
                    Log.d("CreateTableVM", "Added new table with ID: $newTableId")

                    // 将新创建的课表设为默认课表
                    val currentUserId = sessionRepository.currentUserIdFlow.first()
                    if (currentUserId != null && currentUserId != -1L) {
                        try {
                            globalSettingRepository.updateDefaultTableIds(
                                currentUserId.toInt(),
                                listOf(newTableId.toInt())
                            )
                            Log.i(
                                "CreateTableVM",
                                "Set new table $newTableId as default for user $currentUserId"
                            )
                        } catch (settingError: Exception) {
                            Log.e(
                                "CreateTableVM",
                                "Failed to set new table as default",
                                settingError
                            )
                            // 可以考虑是否要将此错误传递给 UI
                        }
                    } else {
                        Log.w(
                            "CreateTableVM",
                            "Cannot set default table because user ID is invalid."
                        )
                    }

                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                }
            } catch (e: Exception) {
                Log.e("CreateTableVM", "Failed to save table", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "保存失败: ${e.message}"
                    )
                }
            }
        }
    }

    fun consumeErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consumeSavedEvent() {
        _uiState.update { it.copy(isSaved = false) }
    }
} 