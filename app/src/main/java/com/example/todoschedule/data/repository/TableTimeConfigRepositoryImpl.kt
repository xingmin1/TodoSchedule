package com.example.todoschedule.data.repository

import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.dao.TableTimeConfigDao
import com.example.todoschedule.data.database.entity.TableTimeConfigEntity
import com.example.todoschedule.data.database.entity.TableTimeConfigNodeDetaileEntity
import com.example.todoschedule.data.mapper.toDomainModel
import com.example.todoschedule.domain.model.TableTimeConfig
import com.example.todoschedule.domain.repository.TableTimeConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalTime
import javax.inject.Inject

/** TableTimeConfigRepository 的实现类。 */
class TableTimeConfigRepositoryImpl
@Inject
constructor(private val tableTimeConfigDao: TableTimeConfigDao) : TableTimeConfigRepository {

    override fun getDefaultTimeConfig(tableId: Int): Flow<TableTimeConfig?> {
        return tableTimeConfigDao.getDefaultTimeConfigWithNodes(tableId).map { configWithNodes ->
            configWithNodes?.toDomainModel() // 使用 Mapper 转换
        }
    }

    override fun getTimeConfigById(configId: Int): Flow<TableTimeConfig?> {
        return tableTimeConfigDao.getTimeConfigWithNodesById(configId).map { configWithNodes ->
            configWithNodes?.toDomainModel()
        }
    }

    /** 确保指定课表存在默认时间配置，如果不存在则创建。 */
    override suspend fun ensureDefaultTimeConfig(tableId: Int, userId: Int): Int? {
        // 1. 检查是否已存在默认配置
        val existingDefault =
            tableTimeConfigDao.getDefaultTimeConfigWithNodes(tableId).firstOrNull()
        if (existingDefault != null) {
            return existingDefault.config.id
        }

        // 2. 创建默认时间配置实体
        val newConfig =
            TableTimeConfigEntity(
                tableId = tableId,
                name = AppConstants.Database.DEFAULT_TIME_CONFIG_TABLE_NAME,
                isDefault = true
            )
        val newConfigId = tableTimeConfigDao.insertTimeConfig(newConfig)

        // 3. 创建默认的节次节点详情
        if (newConfigId > 0) {
            val defaultNodes = createDefaultTimeDetails(newConfigId.toInt())
            tableTimeConfigDao.insertNodeDetails(defaultNodes)
            return newConfigId.toInt()
        } else {
            println("Error: Failed to insert default time config for table $tableId")
            return null
        }
    }

    /** 创建默认时间节点详情列表。 (逻辑从旧的TableTimeConfigRepositoryImpl 迁移并调整) */
    private fun createDefaultTimeDetails(configId: Int): List<TableTimeConfigNodeDetaileEntity> {
        // 注意：这里使用了新的实体 TableTimeConfigNodeDetaileEntity
        return listOf(
            TableTimeConfigNodeDetaileEntity(
                tableTimeConfigId = configId,
                node = 1,
                name = "第一节",
                startTime = LocalTime(8, 0),
                endTime = LocalTime(8, 45)
            ),
            TableTimeConfigNodeDetaileEntity(
                tableTimeConfigId = configId,
                node = 2,
                name = "第二节",
                startTime = LocalTime(8, 55),
                endTime = LocalTime(9, 40)
            ),
            TableTimeConfigNodeDetaileEntity(
                tableTimeConfigId = configId,
                node = 3,
                name = "第三节",
                startTime = LocalTime(10, 10),
                endTime = LocalTime(10, 55)
            ),
            TableTimeConfigNodeDetaileEntity(
                tableTimeConfigId = configId,
                node = 4,
                name = "第四节",
                startTime = LocalTime(11, 5),
                endTime = LocalTime(11, 50)
            ),
            TableTimeConfigNodeDetaileEntity(
                tableTimeConfigId = configId,
                node = 5,
                name = "第五节",
                startTime = LocalTime(14, 0),
                endTime = LocalTime(14, 45)
            ),
            TableTimeConfigNodeDetaileEntity(
                tableTimeConfigId = configId,
                node = 6,
                name = "第六节",
                startTime = LocalTime(14, 55),
                endTime = LocalTime(15, 40)
            ),
            TableTimeConfigNodeDetaileEntity(
                tableTimeConfigId = configId,
                node = 7,
                name = "第七节",
                startTime = LocalTime(16, 0),
                endTime = LocalTime(16, 45)
            ),
            TableTimeConfigNodeDetaileEntity(
                tableTimeConfigId = configId,
                node = 8,
                name = "第八节",
                startTime = LocalTime(16, 55),
                endTime = LocalTime(17, 40)
            ),
            TableTimeConfigNodeDetaileEntity(
                tableTimeConfigId = configId,
                node = 9,
                name = "第九节",
                startTime = LocalTime(19, 0),
                endTime = LocalTime(19, 45)
            ),
            TableTimeConfigNodeDetaileEntity(
                tableTimeConfigId = configId,
                node = 10,
                name = "第十节",
                startTime = LocalTime(19, 55),
                endTime = LocalTime(20, 40)
            )
        )
    }
}
