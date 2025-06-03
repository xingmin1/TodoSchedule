package com.example.todoschedule.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.domain.model.Table
import com.example.todoschedule.domain.repository.GlobalSettingRepository
import com.example.todoschedule.domain.repository.SessionRepository
import com.example.todoschedule.domain.repository.TableRepository
import com.example.todoschedule.domain.use_case.settings.ValidateDefaultTableOverlapUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DefaultDisplaySettingsViewModel @Inject constructor(
    private val tableRepository: TableRepository,
    private val globalSettingRepository: GlobalSettingRepository,
    private val sessionRepository: SessionRepository,
    private val validateOverlapUseCase: ValidateDefaultTableOverlapUseCase
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val allTables: List<Table> = emptyList(),
        val selectedTableIds: Set<Int> = emptySet(),
        val error: String? = null,
        val validationError: String? = null,
        val isSaving: Boolean = false,
        val saveSuccess: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val userIdFlow = sessionRepository.currentUserIdFlow
        .filterNotNull() // Ensure we have a user ID

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            userIdFlow.firstOrNull()?.let { userId ->
                try {
                    // Combine flows for all tables and default selected IDs
                    val allTablesFlow = tableRepository.getTableByUserId(userId.toInt())
                    val defaultIdsFlow = globalSettingRepository.getDefaultTableIds(userId.toInt())

                    combine(allTablesFlow, defaultIdsFlow) { tables, defaultIds ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                allTables = tables,
                                selectedTableIds = defaultIds.toSet(),
                                error = null // Clear previous error
                            )
                        }
                    }.catch { e ->
                        _uiState.update {
                            it.copy(isLoading = false, error = "加载数据失败: ${e.message}")
                        }
                    }.collect()

                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(isLoading = false, error = "加载初始化数据失败: ${e.message}")
                    }
                }
            } ?: run {
                _uiState.update { it.copy(isLoading = false, error = "无法获取用户ID") }
            }
        }
    }

    fun onTableSelectionChanged(tableId: UUID, isSelected: Boolean) {
        _uiState.update { currentState ->
            val currentSelection = currentState.selectedTableIds.toMutableSet()
            if (isSelected) {
                currentSelection.add(tableId)
            } else {
                currentSelection.remove(tableId)
            }
            currentState.copy(
                selectedTableIds = currentSelection,
                validationError = null,
                saveSuccess = false
            ) // Clear errors/success on change
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val selectedIds = currentState.selectedTableIds
            val selectedTables = currentState.allTables.filter { it.id in selectedIds }

            // 1. Validate Overlap
            val validationResult = validateOverlapUseCase(selectedTables)

            if (!validationResult.isValid) {
                val conflictMessage = validationResult.conflictingTables
                    ?.joinToString("\n") { "- ${it.first.tableName} 与 ${it.second.tableName}" }
                    ?: "未知冲突"
                _uiState.update {
                    it.copy(validationError = "课表时间存在重叠:\n$conflictMessage")
                }
                return@launch
            }

            // 2. Save if valid
            _uiState.update { it.copy(isSaving = true, validationError = null) }
            userIdFlow.firstOrNull()?.let { userId ->
                try {
                    globalSettingRepository.updateDefaultTableIds(
                        userId.toInt(),
                        selectedIds.toList()
                    )
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = "保存设置失败: ${e.message}"
                        )
                    }
                }
            } ?: run {
                _uiState.update { it.copy(isSaving = false, error = "无法获取用户ID以保存设置") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearValidationError() {
        _uiState.update { it.copy(validationError = null) }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
} 