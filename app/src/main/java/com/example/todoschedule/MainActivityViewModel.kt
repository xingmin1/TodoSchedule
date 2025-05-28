package com.example.todoschedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoschedule.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    sessionRepository: SessionRepository
) : ViewModel() {

    private val _isSessionLoaded = MutableLiveData(false)
    val isSessionLoaded: LiveData<Boolean> = _isSessionLoaded

    init {
        viewModelScope.launch {
            // 等待 sessionRepository.currentUserIdFlow 发出第一个值
            // Eagerly 启动的 StateFlow 应该会立即有一个初始值 (null)
            // 我们需要等待它从 DataStore 加载完成（变为非 null 或保持 null）
            // .first() 会挂起直到第一个值发出
            sessionRepository.currentUserIdFlow.first()
            // 一旦收到第一个值（无论是 null 还是 Long），就认为会话状态已确定
            _isSessionLoaded.value = true
        }
    }
} 