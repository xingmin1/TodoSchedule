package com.example.todoschedule.ui.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.domain.model.Table
import com.example.todoschedule.domain.repository.TableRepository
import com.example.todoschedule.ui.navigation.AppRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * 单个课表设置 ViewModel
 */
@HiltViewModel
class SingleTableSettingsViewModel @Inject constructor(
    private val tableRepository: TableRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 从路由参数获取表ID
    private val tableId: Int = checkNotNull(
        savedStateHandle.get<String>(AppRoutes.SingleTableSettings.ARG_TABLE_ID)?.toIntOrNull()
    )
    
    // UI 状态
    data class UiState(
        val isLoading: Boolean = true,
        val table: Table? = null,
        val startDate: LocalDate? = null,
        val totalWeeks: Int = 20,
        val isSaving: Boolean = false,
        val error: String? = null,
        val success: String? = null
    )
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    init {
        loadTable()
    }
    
    /**
     * 加载课表信息
     */
    private fun loadTable() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val table = tableRepository.getTableById(tableId).first()
                
                if (table != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        table = table,
                        startDate = table.startDate,
                        totalWeeks = table.totalWeeks
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "找不到指定的课表"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载课表失败: ${e.message}"
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
     * 保存课表设置
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
                
                _uiState.value = currentState.copy(
                    isSaving = false,
                    success = "课表设置已更新",
                    table = updatedTable
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