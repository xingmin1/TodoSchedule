package com.example.todoschedule.ui.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.domain.model.Table
import com.example.todoschedule.domain.model.TableTimeConfig
import com.example.todoschedule.domain.repository.TableRepository
import com.example.todoschedule.domain.repository.TableTimeConfigRepository
import com.example.todoschedule.ui.navigation.AppRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
// import java.time.YearMonth // Not used
import javax.inject.Inject

/**
 * 单个课表设置 ViewModel
 */
@HiltViewModel
class SingleTableSettingsViewModel @Inject constructor(
    private val tableRepository: TableRepository,
    private val tableTimeConfigRepository: TableTimeConfigRepository, // 注入 TimeConfig Repo
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 从路由参数获取表ID
    private val tableId: UUID = checkNotNull(
        savedStateHandle.get<String>(AppRoutes.SingleTableSettings.ARG_TABLE_ID)?.toIntOrNull()
    )

    // UI 状态
    @OptIn(ExperimentalCoroutinesApi::class)
    data class UiState(
        val isLoading: Boolean = true,
        val table: Table? = null,
        val startDate: LocalDate? = null,
        val totalWeeks: Int = 20,
        val timeConfigs: List<TableTimeConfig> = emptyList(), // 添加时间配置列表状态
        val isSaving: Boolean = false,
        val error: String? = null,
        val success: String? = null,
        val dialogState: DialogState = DialogState.None // 添加对话框状态
    )

    // 对话框状态
    sealed class DialogState {
        object None : DialogState()
        data class AddTimeConfig(val name: String = "") : DialogState()
        data class EditTimeConfigName(
            val configId: UUID,
            val currentName: String,
            val newName: String = currentName
        ) : DialogState()

        data class DeleteTimeConfig(val configId: UUID, val configName: String) : DialogState()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _timeConfigs: StateFlow<List<TableTimeConfig>> =
        MutableStateFlow(tableId) // Start with the tableId
            .flatMapLatest { id ->
                tableTimeConfigRepository.getAllTimeConfigsForTable(id)
            }
            .catch { e ->
                _uiState.value = _uiState.value.copy(error = "加载时间配置失败: ${e.message}")
                emit(emptyList()) // Emit empty list on error
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    init {
        loadTableAndCombineStates()
    }

    /**
     * 加载课表信息并合并状态
     */
    private fun loadTableAndCombineStates() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val tableFlow = tableRepository.getTableById(tableId)

                combine(tableFlow, _timeConfigs) { table, timeConfigs ->
                    if (table != null) {
                        _uiState.value.copy(
                            isLoading = false,
                            table = table,
                            startDate = _uiState.value.startDate ?: table.startDate, // 保留UI上的临时更改
                            totalWeeks = _uiState.value.totalWeeks.takeIf { it != 20 }
                                ?: table.totalWeeks, // 保留UI上的临时更改
                            timeConfigs = timeConfigs,
                            error = null // Clear previous error on successful load
                        )
                    } else {
                        _uiState.value.copy(
                            isLoading = false,
                            error = "找不到指定的课表",
                            timeConfigs = timeConfigs // Still show time configs if table is somehow null
                        )
                    }
                }.collect { newState ->
                    _uiState.value = newState
                }

                // 第一次加载时，用数据库的值覆盖初始默认值
                val initialTable = tableFlow.first()
                if (initialTable != null && _uiState.value.startDate == null && _uiState.value.totalWeeks == 20) {
                    _uiState.value = _uiState.value.copy(
                        startDate = initialTable.startDate,
                        totalWeeks = initialTable.totalWeeks
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载课表信息失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 更新开学日期
     */
    fun updateStartDate(newDate: LocalDate) {
        _uiState.value = _uiState.value.copy(startDate = newDate)
    }

    /**
     * 更新总周数
     */
    fun updateTotalWeeks(weeks: Int) {
        if (weeks in 1..30) { // 设置合理范围
            _uiState.value = _uiState.value.copy(totalWeeks = weeks)
        }
    }

    /**
     * 保存课表设置 (开学日期和总周数)
     */
    fun saveTableSettings(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val currentTable = currentState.table

                if (currentTable == null) {
                    _uiState.value = currentState.copy(
                        error = "无法保存：课表信息丢失"
                    )
                    return@launch
                }

                _uiState.value = currentState.copy(isSaving = true)

                // 创建更新后的表对象
                val updatedTable = currentTable.copy(
                    startDate = currentState.startDate ?: currentTable.startDate,
                    totalWeeks = currentState.totalWeeks
                )

                // 更新表
                tableRepository.updateTable(updatedTable)

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    success = "课表设置已更新",
                    table = updatedTable // 更新本地状态中的 table
                )

                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "保存失败: ${e.message}"
                )
            }
        }
    }

    // --- 时间配置相关操作 ---

    /** 显示添加时间配置对话框 */
    fun showAddTimeConfigDialog() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.AddTimeConfig())
    }

    /** 显示编辑时间配置名称对话框 */
    fun showEditTimeConfigNameDialog(configId: UUID, currentName: String) {
        _uiState.value =
            _uiState.value.copy(dialogState = DialogState.EditTimeConfigName(configId, currentName))
    }

    /** 显示删除时间配置对话框 */
    fun showDeleteTimeConfigDialog(configId: UUID, configName: String) {
        _uiState.value =
            _uiState.value.copy(dialogState = DialogState.DeleteTimeConfig(configId, configName))
    }

    /** 更新对话框状态 (例如，当输入框内容改变时) */
    fun updateDialogState(newState: DialogState) {
        _uiState.value = _uiState.value.copy(dialogState = newState)
    }

    /** 关闭对话框 */
    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.None)
    }

    /** 添加新的时间配置 */
    fun addTimeConfig(name: String) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _uiState.value = _uiState.value.copy(error = "名称不能为空")
                return@launch
            }
            try {
                val newConfig = TableTimeConfig(
                    id = 0, // ID 由数据库生成
                    tableId = tableId,
                    name = name,
                    isDefault = false, // 新添加的默认不是默认配置
                    nodes = emptyList() // 初始为空，稍后在节次配置页面添加
                )
                tableTimeConfigRepository.addTimeConfig(newConfig)
                _uiState.value = _uiState.value.copy(
                    success = "时间配置 '$name' 已添加",
                    dialogState = DialogState.None
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "添加失败: ${e.message}",
                    dialogState = DialogState.None
                )
            }
        }
    }

    /** 更新时间配置名称 */
    fun updateTimeConfigName(configId: UUID, newName: String) {
        viewModelScope.launch {
            if (newName.isBlank()) {
                _uiState.value = _uiState.value.copy(error = "名称不能为空")
                return@launch
            }
            try {
                // 获取现有配置，只更新名称
                val configToUpdate =
                    _uiState.value.timeConfigs.find { it.id == configId }?.copy(name = newName)
                if (configToUpdate != null) {
                    tableTimeConfigRepository.updateTimeConfig(configToUpdate)
                    _uiState.value = _uiState.value.copy(
                        success = "名称已更新为 '$newName'",
                        dialogState = DialogState.None
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "未找到要更新的配置",
                        dialogState = DialogState.None
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "更新失败: ${e.message}",
                    dialogState = DialogState.None
                )
            }
        }
    }

    /** 删除时间配置 */
    fun deleteTimeConfig(configId: UUID) {
        viewModelScope.launch {
            try {
                tableTimeConfigRepository.deleteTimeConfig(configId)
                _uiState.value =
                    _uiState.value.copy(success = "时间配置已删除", dialogState = DialogState.None)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "删除失败: ${e.message}",
                    dialogState = DialogState.None
                )
            }
        }
    }

    /** 设置为默认时间配置 */
    fun setDefaultTimeConfig(configId: UUID) {
        viewModelScope.launch {
            try {
                tableTimeConfigRepository.setDefaultTimeConfig(tableId, configId)
                _uiState.value = _uiState.value.copy(success = "已设为默认时间配置")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "设置默认失败: ${e.message}")
            }
        }
    }

    /**
     * 清除成功消息
     */
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(success = null)
    }

    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(error = null)
    }
} 