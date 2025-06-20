package com.example.todoschedule.domain.repository

import com.example.todoschedule.domain.model.Table
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** 课表仓库接口 */
interface TableRepository {
    /** 获取所有课表 */
    fun getAllTables(): Flow<List<Table>>

    /** 获取默认课表 */
    fun getDefaultTable(): Flow<Table?>

    /** 根据ID获取课表 */
    fun getTableById(tableId: UUID): Flow<Table?>

    /** 根据用户ID获取课表 */
    fun getTableByUserId(userId: UUID): Flow<List<Table>>

    /** 根据ID获取课表 */
    suspend fun fetchTableById(tableId: UUID): Table?

    /** 根据用户ID获取课表 */
    suspend fun fetchTablesByUserId(userId: UUID): List<Table>

    /** 添加课表 */
    suspend fun addTable(table: Table): UUID

    /** 更新课表 */
    suspend fun updateTable(table: Table)

    /** 删除课表 */
    suspend fun deleteTable(tableId: UUID)
}
