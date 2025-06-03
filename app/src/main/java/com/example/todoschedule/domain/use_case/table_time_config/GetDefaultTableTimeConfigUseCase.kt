package com.example.todoschedule.domain.use_case.table_time_config

import com.example.todoschedule.domain.model.TableTimeConfig
import com.example.todoschedule.domain.repository.TableTimeConfigRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取指定课表默认时间配置的用例。
 */
class GetDefaultTableTimeConfigUseCase @Inject constructor(
    private val repository: TableTimeConfigRepository
) {
    operator fun invoke(tableId: UUID): Flow<TableTimeConfig?> {
        // 可以在这里添加验证或其他业务逻辑
        if (tableId <= 0) {
            // 处理无效的 tableId，例如返回 emptyFlow() 或抛出异常
            // import kotlinx.coroutines.flow.emptyFlow
            // return emptyFlow()
            throw IllegalArgumentException("无效的课表 ID: $tableId")
        }
        return repository.getDefaultTimeConfig(tableId)
    }
} 