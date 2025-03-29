package com.example.todoschedule.data.repository

import com.example.todoschedule.data.database.dao.TableDao
import com.example.todoschedule.data.database.entity.TableEntity
import com.example.todoschedule.domain.model.Table
import com.example.todoschedule.domain.repository.TableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 课表仓库实现
 */
@Singleton
class TableRepositoryImpl @Inject constructor(
    private val tableDao: TableDao
) : TableRepository {

    /**
     * 获取所有课表
     */
    override fun getAllTables(): Flow<List<Table>> {
        return tableDao.getAllTables().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * 获取默认课表
     */
    override fun getDefaultTable(): Flow<Table?> {
        return tableDao.getDefaultTable().map { entity ->
            entity?.toDomain()
        }
    }

    /**
     * 根据ID获取课表
     */
    override suspend fun getTableById(tableId: Int): Table? {
        return tableDao.getTableById(tableId)?.toDomain()
    }

    /**
     * 添加课表
     */
    override suspend fun addTable(table: Table): Long {
        return tableDao.insertTable(table.toEntity())
    }

    /**
     * 更新课表
     */
    override suspend fun updateTable(table: Table) {
        tableDao.updateTable(table.toEntity())
    }

    /**
     * 删除课表
     */
    override suspend fun deleteTable(tableId: Int) {
        tableDao.deleteTable(tableId)
    }

    /**
     * 将实体类转换为领域模型
     */
    private fun TableEntity.toDomain(): Table {
        return Table(
            id = id,
            tableName = tableName,
            background = background,
            startDate = startDate,
            totalWeeks = totalWeeks
        )
    }

    /**
     * 将领域模型转换为实体类
     */
    private fun Table.toEntity(): TableEntity {
        return TableEntity(
            id = id,
            tableName = tableName,
            background = background,
            startDate = startDate,
            totalWeeks = totalWeeks
        )
    }
} 