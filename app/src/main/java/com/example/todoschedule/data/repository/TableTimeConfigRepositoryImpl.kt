package com.example.todoschedule.data.repository

// Assume these mappers exist or create them
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.dao.TableTimeConfigDao
import com.example.todoschedule.data.database.entity.TableTimeConfigEntity
import com.example.todoschedule.data.database.entity.TableTimeConfigNodeDetaileEntity
import com.example.todoschedule.data.mapper.toDomainModel
import com.example.todoschedule.data.mapper.toTableTimeConfigWithNodes
import com.example.todoschedule.domain.model.TableTimeConfig
import com.example.todoschedule.domain.repository.TableTimeConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalTime
import java.util.UUID
import javax.inject.Inject

/** TableTimeConfigRepository 的实现类。 */
class TableTimeConfigRepositoryImpl
@Inject
constructor(private val tableTimeConfigDao: TableTimeConfigDao) : TableTimeConfigRepository {

    override fun getDefaultTimeConfig(tableId: UUID): Flow<TableTimeConfig?> {
        return tableTimeConfigDao.getDefaultTimeConfigWithNodes(tableId).map { configWithNodes ->
            configWithNodes?.toDomainModel() // 使用 Mapper 转换
        }
    }

    override fun getTimeConfigById(configId: UUID): Flow<TableTimeConfig?> {
        return tableTimeConfigDao.getTimeConfigWithNodesById(configId).map { config ->
            config.toDomainModel() // Convert to domain model
        }
    }

    override suspend fun ensureDefaultTimeConfig(tableId: UUID, userId: UUID): UUID? {
        val existingDefault = tableTimeConfigDao.getDefaultTimeConfigForTable(tableId)
        if (existingDefault != null) {
            return existingDefault.id
        }

        val newConfig = TableTimeConfigEntity(
            tableId = tableId,
            name = AppConstants.Database.DEFAULT_TIME_CONFIG_TABLE_NAME,
            isDefault = true
            // id is auto-generated
        )
        val defaultNodes =
            createDefaultTimeDetails()

        try {
            // Use the transaction to insert config and nodes together
            val newConfigId = tableTimeConfigDao.insertConfigWithNodes(newConfig, defaultNodes)
            return newConfigId
        } catch (e: Exception) {
            // Log the error appropriately
            println("Error: Failed to insert default time config for table $tableId: ${e.message}")
            return null
        }
    }

    override suspend fun addTimeConfig(config: TableTimeConfig): UUID {
        // 不允许通过此方法添加默认配置
        if (config.isDefault) {
            throw IllegalArgumentException("Cannot add a default config using addTimeConfig. Use ensureDefaultTimeConfig or setDefaultTimeConfig.")
        }
        // 检查 tableId 是否有效 (例如, 确保对应的 TableEntity 存在) - 可选
        // val tableExists = tableDao.getTableById(config.tableId) != null
        // if (!tableExists) throw NoSuchElementException("Table with id ${config.tableId} not found.")

        // 将 Domain Model 转换为 Entities
        val tableTimeConfigWithNodes = config.toTableTimeConfigWithNodes()

        // 使用 DAO 事务插入
        return tableTimeConfigDao.insertConfigWithNodes(
            tableTimeConfigWithNodes.config,
            tableTimeConfigWithNodes.nodes
        )
    }

    override suspend fun updateTimeConfig(config: TableTimeConfig) {
        // 1. 检查配置是否存在
        val existingConfig = tableTimeConfigDao.getTimeConfigById(config.id)
            ?: throw NoSuchElementException("TimeConfig with id ${config.id} not found.")

        // 2. 不允许通过此方法修改 isDefault 状态
        if (existingConfig.isDefault != config.isDefault) {
            throw IllegalArgumentException("Cannot change 'isDefault' status using updateTimeConfig. Use setDefaultTimeConfig.")
        }
        // 3. 确保 tableId 没有被意外修改 (如果需要)
        // if (existingConfig.tableId != config.tableId) {
        //    throw IllegalArgumentException("Cannot change the tableId of an existing TimeConfig.")
        // }


        // 4. 将 Domain Model 转换为 Entities
        val tableTimeConfigWithNodes = config.toTableTimeConfigWithNodes()

        // 5. 使用 DAO 事务更新
        tableTimeConfigDao.updateConfigWithNodes(
            tableTimeConfigWithNodes.config,
            tableTimeConfigWithNodes.nodes
        )
    }

    override suspend fun deleteTimeConfig(configId: UUID) {
        // 1. 检查配置是否存在
        val configToDelete = tableTimeConfigDao.getTimeConfigById(configId)
            ?: throw NoSuchElementException("TimeConfig with id $configId not found.")

        // 2. 不允许删除默认配置
        if (configToDelete.isDefault) {
            // Consider allowing deletion if it's not the *only* config for the table
            // val otherConfigs = tableTimeConfigDao.getTimeConfigsForTable(configToDelete.tableId)
            // if (otherConfigs.size <= 1) {
            throw IllegalArgumentException("Cannot delete the default time configuration. Set another configuration as default first.")
            // }
        }

        // 3. 使用 DAO 事务删除
        tableTimeConfigDao.deleteConfigWithNodes(configId)
    }

    override fun getAllTimeConfigsForTable(tableId: UUID): Flow<List<TableTimeConfig>> {
        return tableTimeConfigDao.getAllTimeConfigsWithNodes(tableId).map { list ->
            list.map { it.toDomainModel() } // Map each ConfigWithNodes to TableTimeConfig
        }
    }

    override suspend fun setDefaultTimeConfig(tableId: UUID, configId: UUID) {
        // 1. 检查要设为默认的配置是否存在且属于该 tableId
        val configToSetDefault = tableTimeConfigDao.getTimeConfigById(configId)
            ?: throw NoSuchElementException("TimeConfig with id $configId not found.")
        if (configToSetDefault.tableId != tableId) {
            throw IllegalArgumentException("TimeConfig with id $configId does not belong to table $tableId.")
        }

        // 2. 如果它已经是默认的，则无需操作
        if (configToSetDefault.isDefault) {
            return
        }

        // 3. 使用 DAO 事务更新默认状态
        tableTimeConfigDao.setDefault(tableId, configId)
    }


    /** 创建默认时间节点详情列表。 */
    private fun createDefaultTimeDetails(): List<TableTimeConfigNodeDetaileEntity> {
        return listOf(
            TableTimeConfigNodeDetaileEntity(
                tableTimeConfigId = UUID.randomUUID(),
                node = 1,
                name = "第一节",
                startTime = LocalTime(8, 0),
                endTime = LocalTime(8, 45)
            ),
            TableTimeConfigNodeDetaileEntity(
                tableTimeConfigId = UUID.randomUUID(),
                node = 2,
                name = "第二节",
                startTime = LocalTime(8, 55),
                endTime = LocalTime(9, 40)
            ),
            TableTimeConfigNodeDetaileEntity(
                tableTimeConfigId = UUID.randomUUID(),
                node = 3,
                name = "第三节",
                startTime = LocalTime(10, 10),
                endTime = LocalTime(10, 55)
            ),
            TableTimeConfigNodeDetaileEntity(
                tableTimeConfigId = UUID.randomUUID(),
                node = 4,
                name = "第四节",
                startTime = LocalTime(11, 5),
                endTime = LocalTime(11, 50)
            ),
            TableTimeConfigNodeDetaileEntity(
                tableTimeConfigId = UUID.randomUUID(),
                node = 5,
                name = "第五节",
                startTime = LocalTime(14, 0),
                endTime = LocalTime(14, 45)
            ),
            TableTimeConfigNodeDetaileEntity(
                tableTimeConfigId = UUID.randomUUID(),
                node = 6,
                name = "第六节",
                startTime = LocalTime(14, 55),
                endTime = LocalTime(15, 40)
            ),
            TableTimeConfigNodeDetaileEntity(
                tableTimeConfigId = UUID.randomUUID(),
                node = 7,
                name = "第七节",
                startTime = LocalTime(16, 0),
                endTime = LocalTime(16, 45)
            ),
            TableTimeConfigNodeDetaileEntity(
                tableTimeConfigId = UUID.randomUUID(),
                node = 8,
                name = "第八节",
                startTime = LocalTime(16, 55),
                endTime = LocalTime(17, 40)
            ),
            TableTimeConfigNodeDetaileEntity(
                tableTimeConfigId = UUID.randomUUID(),
                node = 9,
                name = "第九节",
                startTime = LocalTime(19, 0),
                endTime = LocalTime(19, 45)
            ),
            TableTimeConfigNodeDetaileEntity(
                tableTimeConfigId = UUID.randomUUID(),
                node = 10,
                name = "第十节",
                startTime = LocalTime(19, 55),
                endTime = LocalTime(20, 40)
            )
            // Add more nodes as needed
        )
    }
}
