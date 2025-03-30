package com.example.todoschedule.data.repository

import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.dao.TimeConfigDao
import com.example.todoschedule.data.database.entity.TimeDetailEntity
import com.example.todoschedule.data.database.entity.TimeTableEntity
import com.example.todoschedule.domain.model.TimeDetail
import com.example.todoschedule.domain.repository.TimeConfigRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 时间配置仓库实现
 */
@Singleton
class TimeConfigRepositoryImpl @Inject constructor(
    private val timeConfigDao: TimeConfigDao
) : TimeConfigRepository {

    /**
     * 获取指定课表的时间节点
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getTimeDetailsByTableId(tableId: Int): Flow<List<TimeDetail>> {
        return timeConfigDao.getDefaultTimeTable(tableId)
            .flatMapLatest { timeTable ->
                if (timeTable != null) {
                    timeConfigDao.getTimeDetailsByTimeTableId(timeTable.id)
                        .map { details ->
                            details.map { it.toDomain() }
                        }
                } else {
                    // 如果没有找到默认时间配置，返回空列表
                    kotlinx.coroutines.flow.flowOf(emptyList())
                }
            }
    }

    /**
     * 创建默认时间配置
     */
    override suspend fun createDefaultTimeConfig(tableId: Int): Long {
        // 创建默认时间配置
        val timeTableId = timeConfigDao.insertTimeTable(
            TimeTableEntity(
                tableId = tableId,
                name = AppConstants.Database.DEFAULT_TIME_CONFIG_TABLE_NAME,
            )
        )

        // 创建默认节次详情
        val timeDetails = createDefaultTimeDetails(timeTableId.toInt())
        timeConfigDao.insertTimeDetails(timeDetails)

        return timeTableId
    }

    /**
     * 创建默认时间节点详情
     */
    private fun createDefaultTimeDetails(timeTableId: Int): List<TimeDetailEntity> {
        return listOf(
            TimeDetailEntity(
                tableTimeConfigId = timeTableId,
                node = 1,
                name = "第一节",
                startTime = LocalTime(8, 0),
                endTime = LocalTime(8, 45)
            ),
            TimeDetailEntity(
                tableTimeConfigId = timeTableId,
                node = 2,
                name = "第二节",
                startTime = LocalTime(8, 50),
                endTime = LocalTime(9, 35)
            ),
            TimeDetailEntity(
                tableTimeConfigId = timeTableId,
                node = 3,
                name = "第三节",
                startTime = LocalTime(9, 50),
                endTime = LocalTime(10, 35)
            ),
            TimeDetailEntity(
                tableTimeConfigId = timeTableId,
                node = 4,
                name = "第四节",
                startTime = LocalTime(10, 40),
                endTime = LocalTime(11, 25)
            ),
            TimeDetailEntity(
                tableTimeConfigId = timeTableId,
                node = 5,
                name = "第五节",
                startTime = LocalTime(11, 30),
                endTime = LocalTime(12, 15)
            ),
            TimeDetailEntity(
                tableTimeConfigId = timeTableId,
                node = 6,
                name = "第六节",
                startTime = LocalTime(13, 30),
                endTime = LocalTime(14, 15)
            ),
            TimeDetailEntity(
                tableTimeConfigId = timeTableId,
                node = 7,
                name = "第七节",
                startTime = LocalTime(14, 20),
                endTime = LocalTime(15, 5)
            ),
            TimeDetailEntity(
                tableTimeConfigId = timeTableId,
                node = 8,
                name = "第八节",
                startTime = LocalTime(15, 20),
                endTime = LocalTime(16, 5)
            ),
            TimeDetailEntity(
                tableTimeConfigId = timeTableId,
                node = 9,
                name = "第九节",
                startTime = LocalTime(16, 10),
                endTime = LocalTime(16, 55)
            ),
            TimeDetailEntity(
                tableTimeConfigId = timeTableId,
                node = 10,
                name = "第十节",
                startTime = LocalTime(17, 0),
                endTime = LocalTime(17, 45)
            ),
            TimeDetailEntity(
                tableTimeConfigId = timeTableId,
                node = 11,
                name = "第十一节",
                startTime = LocalTime(18, 30),
                endTime = LocalTime(19, 15)
            ),
            TimeDetailEntity(
                tableTimeConfigId = timeTableId,
                node = 12,
                name = "第十二节",
                startTime = LocalTime(19, 20),
                endTime = LocalTime(20, 5)
            )
        )
    }

    /**
     * 将时间详情实体转换为领域模型
     */
    private fun TimeDetailEntity.toDomain(): TimeDetail {
        return TimeDetail(
            node = node,
            name = name,
            startTime = startTime,
            endTime = endTime
        )
    }
} 