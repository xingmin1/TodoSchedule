package com.example.todoschedule.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.domain.model.Table
import com.example.todoschedule.domain.repository.SessionRepository
import com.example.todoschedule.domain.repository.TableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * 课表管理 ViewModel
 */
@HiltViewModel
class TableManagementViewModel @Inject constructor(
    private val tableRepository: TableRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    // UI 状态
    data class UiState(
        val isLoading: Boolean = true,
        val tables: List<Table> = emptyList(),
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val tables: StateFlow<List<Table>> = sessionRepository.currentUserIdFlow
        .flatMapLatest { userId ->
            if (userId == null) {
                // 用户未登录时，返回空列表
                MutableStateFlow(emptyList())
            } else {
                tableRepository.getTableByUserId(userId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // 组合 tables 和 isRefreshing 更新 UI 状态
        viewModelScope.launch {
            combine(tables, _isRefreshing) { tablesList, isRefreshing ->
                if (isRefreshing) {
                    UiState(isLoading = true, tables = tablesList)
                } else {
                    UiState(isLoading = false, tables = tablesList)
                }
            }.collect { state ->
                _uiState.value = state
            }
        }

        // 初始加载数据
        refreshTables()
    }

    /**
     * 刷新课表列表
     */
    fun refreshTables() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                val userId = sessionRepository.currentUserIdFlow.first()
                if (userId != null) {
                    // 刷新操作 - 实际上只是触发 Flow，不需要额外操作
                    // 因为使用了 stateIn 和 flatMapLatest，tables 会自动更新

                    // 延迟一会儿以模拟网络请求，便于测试下拉刷新状态
                    kotlinx.coroutines.delay(1000)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "加载课表失败: ${e.message}"
                )
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * 删除课表
     */
    fun deleteTable(tableId: UUID, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                tableRepository.deleteTable(tableId)
                onSuccess()
            } catch (e: Exception) {
                onError("删除课表失败: ${e.message}")
            }
        }
    }
} 