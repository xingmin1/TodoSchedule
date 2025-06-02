package com.example.todoschedule.data.repository

import android.util.Log
import com.example.todoschedule.data.database.dao.TableDao
import com.example.todoschedule.data.database.entity.TableEntity
import com.example.todoschedule.data.sync.SyncConstants
import com.example.todoschedule.data.sync.SyncManager
import com.example.todoschedule.domain.model.Table
import com.example.todoschedule.domain.repository.SessionRepository
import com.example.todoschedule.domain.repository.TableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 课表仓库实现 */
@Singleton
class TableRepositoryImpl @Inject constructor(
    private val tableDao: TableDao,
    private val sessionRepository: SessionRepository,
    private val syncManager: SyncManager
) : TableRepository {

    private val TAG = "TableRepositoryImpl"

    /** 获取所有课表 */
    override fun getAllTables(): Flow<List<Table>> {
        return tableDao.getAllTables().map { entities -> entities.map { it.toDomain() } }
    }

    /** 获取默认课表 */
    override fun getDefaultTable(): Flow<Table?> {
        return tableDao.getDefaultTable().map { entity -> entity?.toDomain() }
    }

    /** 根据ID获取课表 */
    override fun getTableById(tableId: Int): Flow<Table?> {
        return tableDao.getTableById(tableId).map { it?.toDomain() }
    }

    /** 根据用户ID获取课表 */
    override fun getTableByUserId(userId: Int): Flow<List<Table>> {
        return tableDao.getTableByUserId(userId).map { flowList -> flowList.map { it.toDomain() } }
    }

    /** 根据ID获取课表 */
    override suspend fun fetchTableById(tableId: Int): Table? {
        return tableDao.fetchTableById(tableId)?.toDomain()
    }

    /** 根据用户ID获取课表 */
    override suspend fun fetchTablesByUserId(userId: Int): List<Table> {
        return tableDao.fetchTablesByUserId(userId).map { it.toDomain() }
    }

    /** 添加课表 */
    override suspend fun addTable(table: Table): Long {
        try {
            val tableEntity = table.toEntity()
            val tableId = tableDao.insertTable(tableEntity)
            val tableEntityWithId = tableEntity.copy(id = tableId.toInt())

            // 创建同步消息
            val userId = sessionRepository.currentUserIdFlow.first()?.toInt() ?: table.userId
            syncManager.createAndSaveSyncMessage(
                crdtKey = tableEntity.crdtKey,
                entityType = SyncConstants.EntityType.TABLE,
                operationType = SyncConstants.OperationType.ADD,
                userId = userId,
                entity = tableEntityWithId
            )

            Log.d(TAG, "添加课表已创建同步消息: ${tableEntity.crdtKey}")

            // 创建同步消息后立即触发同步过程
            try {
                Log.d(TAG, "正在主动触发同步过程来上传课表同步消息...")
                syncManager.syncNow(ignoreExceptions = true)
                Log.d(TAG, "主动触发同步完成")
            } catch (e: Exception) {
                Log.e(TAG, "主动触发同步失败: ${e.message}", e)
                // 同步失败不影响课表添加的结果
            }

            return tableId
        } catch (e: Exception) {
            Log.e(TAG, "添加课表及同步消息失败", e)
            throw e
        }
    }

    /** 更新课表 */
    override suspend fun updateTable(table: Table) {
        try {
            val tableEntity = table.toEntity()
            tableDao.updateTable(tableEntity)

            // 创建同步消息
            val userId = sessionRepository.currentUserIdFlow.first()?.toInt() ?: table.userId
            syncManager.createAndSaveSyncMessage(
                crdtKey = tableEntity.crdtKey,
                entityType = SyncConstants.EntityType.TABLE,
                operationType = SyncConstants.OperationType.UPDATE,
                userId = userId,
                entity = tableEntity
            )

            Log.d(TAG, "更新课表已创建同步消息: ${tableEntity.crdtKey}")

            // 创建同步消息后立即触发同步过程
            try {
                Log.d(TAG, "正在主动触发同步过程来上传课表更新同步消息...")
                syncManager.syncNow(ignoreExceptions = true)
                Log.d(TAG, "主动触发同步完成")
            } catch (e: Exception) {
                Log.e(TAG, "主动触发同步失败: ${e.message}", e)
                // 同步失败不影响课表更新的结果
            }
        } catch (e: Exception) {
            Log.e(TAG, "更新课表及同步消息失败", e)
            throw e
        }
    }

    /** 删除课表 */
    override suspend fun deleteTable(tableId: Int) {
        try {
            // 获取要删除的课表实体，用于创建同步消息
            val tableEntity = tableDao.fetchTableById(tableId) ?: return

            // 先创建同步消息，再删除课表
            val userId = sessionRepository.currentUserIdFlow.first()?.toInt() ?: tableEntity.userId
            syncManager.createAndSaveSyncMessage(
                crdtKey = tableEntity.crdtKey,
                entityType = SyncConstants.EntityType.TABLE,
                operationType = SyncConstants.OperationType.DELETE,
                userId = userId,
                entity = tableEntity
            )

            Log.d(TAG, "删除课表已创建同步消息: ${tableEntity.crdtKey}")

            // 执行删除操作
            tableDao.deleteTable(tableId)

            // 创建同步消息后立即触发同步过程
            try {
                Log.d(TAG, "正在主动触发同步过程来上传课表删除同步消息...")
                syncManager.syncNow(ignoreExceptions = true)
                Log.d(TAG, "主动触发同步完成")
            } catch (e: Exception) {
                Log.e(TAG, "主动触发同步失败: ${e.message}", e)
                // 同步失败不影响课表删除的结果
            }
        } catch (e: Exception) {
            Log.e(TAG, "删除课表及同步消息失败", e)
            throw e
        }
    }

    /** 将实体类转换为领域模型 */
    private fun TableEntity.toDomain(): Table {
        return Table(
            id = id,
            userId = userId,
            tableName = tableName,
            background = background,
            startDate = startDate,
            totalWeeks = totalWeeks
        )
    }

    /** 将领域模型转换为实体类 */
    private fun Table.toEntity(): TableEntity {
        return TableEntity(
            id = id,
            userId = userId,
            tableName = tableName,
            background = background,
            startDate = startDate,
            totalWeeks = totalWeeks
        )
    }
}
