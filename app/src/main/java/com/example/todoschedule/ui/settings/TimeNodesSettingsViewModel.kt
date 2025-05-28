package com.example.todoschedule.ui.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.domain.model.TableTimeConfig
import com.example.todoschedule.domain.model.TableTimeConfigNode
import com.example.todoschedule.domain.repository.TableTimeConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import javax.inject.Inject

/**
 * 时间节点设置 ViewModel
 */
@HiltViewModel
class TimeNodesSettingsViewModel @Inject constructor(
    private val tableTimeConfigRepository: TableTimeConfigRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tableId: Int = checkNotNull(savedStateHandle[ARG_TABLE_ID])
    private val configId: Int = checkNotNull(savedStateHandle[ARG_CONFIG_ID])

    // UI 状态
    data class UiState(
        val isLoading: Boolean = true,
        val configName: String = "",
        val nodes: List<TableTimeConfigNode> = emptyList(),
        val error: String? = null,
        val success: String? = null,
        val dialogState: DialogState = DialogState.None
    )

    // 对话框状态
    sealed class DialogState {
        object None : DialogState()
        data class AddOrEditNode(
            val nodeToEdit: TableTimeConfigNode? = null, // null 表示添加
            val nodeName: String = nodeToEdit?.name ?: "",
            val startTime: LocalTime = nodeToEdit?.startTime ?: LocalTime(8, 0),
            val endTime: LocalTime = nodeToEdit?.endTime ?: LocalTime(8, 45),
            val nodeNumber: Int = nodeToEdit?.node ?: 0 // 添加模式下可能需要自动计算
        ) : DialogState()

        data class DeleteNode(val nodeId: Int, val nodeName: String) : DialogState()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 时间配置信息 Flow (包含名称和节点)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val timeConfigFlow: StateFlow<TableTimeConfig?> =
        MutableStateFlow(configId)
            .flatMapLatest { id ->
                tableTimeConfigRepository.getTimeConfigById(id)
            }
            .catch { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载时间配置详情失败: ${e.message}"
                )
                emit(null)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    init {
        observeTimeConfig()
    }

    private fun observeTimeConfig() {
        viewModelScope.launch {
            timeConfigFlow.filterNotNull().collect { config ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    configName = config.name,
                    nodes = config.nodes.sortedBy { it.node } // 按节次排序
                )
            }
        }
    }

    // --- 对话框管理 ---
    fun showAddNodeDialog() {
        val nextNodeNumber = (_uiState.value.nodes.maxOfOrNull { it.node } ?: 0) + 1
        _uiState.value = _uiState.value.copy(
            dialogState = DialogState.AddOrEditNode(nodeNumber = nextNodeNumber)
        )
    }

    fun showEditNodeDialog(node: TableTimeConfigNode) {
        _uiState.value =
            _uiState.value.copy(dialogState = DialogState.AddOrEditNode(nodeToEdit = node))
    }

    fun showDeleteNodeDialog(nodeId: Int, nodeName: String) {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.DeleteNode(nodeId, nodeName))
    }

    fun updateDialogState(newState: DialogState) {
        _uiState.value = _uiState.value.copy(dialogState = newState)
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(dialogState = DialogState.None)
    }

    // --- 节点 CRUD 操作 ---
    fun saveNode(dialogData: DialogState.AddOrEditNode) {
        viewModelScope.launch {
            val name = dialogData.nodeName
            val start = dialogData.startTime
            val end = dialogData.endTime
            val nodeNumber = dialogData.nodeNumber

            if (name.isBlank()) {
                _uiState.value = _uiState.value.copy(error = "节次名称不能为空")
                return@launch
            }
            if (start >= end) {
                _uiState.value = _uiState.value.copy(error = "开始时间必须早于结束时间")
                return@launch
            }
            // 可选：检查时间重叠

            try {
                val currentConfig = timeConfigFlow.value ?: run {
                    _uiState.value = _uiState.value.copy(
                        error = "无法保存，时间配置信息丢失",
                        dialogState = DialogState.None
                    )
                    return@launch
                }

                val nodeToSave = TableTimeConfigNode(
                    id = dialogData.nodeToEdit?.id ?: 0, // 0 表示新节点
                    name = name,
                    startTime = start,
                    endTime = end,
                    node = nodeNumber // 使用对话框中的节次
                )

                val updatedNodes = if (dialogData.nodeToEdit == null) {
                    // 添加
                    currentConfig.nodes + nodeToSave
                } else {
                    // 编辑
                    currentConfig.nodes.map { if (it.id == nodeToSave.id) nodeToSave else it }
                }

                val updatedConfig = currentConfig.copy(nodes = updatedNodes)
                tableTimeConfigRepository.updateTimeConfig(updatedConfig) // 更新整个配置

                _uiState.value = _uiState.value.copy(
                    success = if (dialogData.nodeToEdit == null) "节次已添加" else "节次已更新",
                    dialogState = DialogState.None
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "保存节次失败: ${e.message}",
                    dialogState = DialogState.None
                )
            }
        }
    }

    fun deleteNode(nodeId: Int) {
        viewModelScope.launch {
            try {
                val currentConfig = timeConfigFlow.value ?: run {
                    _uiState.value = _uiState.value.copy(
                        error = "无法删除，时间配置信息丢失",
                        dialogState = DialogState.None
                    )
                    return@launch
                }

                val updatedNodes = currentConfig.nodes.filter { it.id != nodeId }

                // 重新编号节次，确保连续 (可选，取决于需求)
                // val renumberedNodes = updatedNodes.sortedBy { it.startTime }.mapIndexed { index, node ->
                //     node.copy(node = index + 1)
                // }

                val updatedConfig = currentConfig.copy(nodes = updatedNodes) // 或 renumberedNodes
                tableTimeConfigRepository.updateTimeConfig(updatedConfig) // 更新整个配置

                _uiState.value = _uiState.value.copy(
                    success = "节次已删除",
                    dialogState = DialogState.None
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "删除节次失败: ${e.message}",
                    dialogState = DialogState.None
                )
            }
        }
    }

    // --- 消息清除 ---
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(success = null)
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    companion object {
        const val ARG_TABLE_ID = "tableId"
        const val ARG_CONFIG_ID = "configId"
    }
} 