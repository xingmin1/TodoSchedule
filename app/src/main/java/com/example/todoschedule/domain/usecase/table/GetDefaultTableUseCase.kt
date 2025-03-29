package com.example.todoschedule.domain.usecase.table

import com.example.todoschedule.domain.model.Table
import com.example.todoschedule.domain.repository.TableRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取默认课表用例
 */
class GetDefaultTableUseCase @Inject constructor(
    private val tableRepository: TableRepository
) {
    /**
     * 执行用例
     */
    operator fun invoke(): Flow<Table?> {
        return tableRepository.getDefaultTable()
    }
} 