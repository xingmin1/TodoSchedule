package com.example.todoschedule.data.sync

import android.util.Log
import com.example.todoschedule.data.database.entity.CourseEntity
import com.example.todoschedule.data.database.entity.CourseNodeEntity
import com.example.todoschedule.data.database.entity.OrdinaryScheduleEntity
import com.example.todoschedule.data.database.entity.SyncMessageEntity
import com.example.todoschedule.data.database.entity.Syncable
import com.example.todoschedule.data.database.entity.TableEntity
import com.example.todoschedule.data.database.entity.TimeSlotEntity
import com.example.todoschedule.data.database.entity.toEntity
import com.example.todoschedule.data.repository.SyncRepository
import com.example.todoschedule.data.sync.adapter.SynkAdapter
import com.example.todoschedule.data.sync.adapter.SynkAdapterRegistry
import com.example.todoschedule.data.sync.dto.SyncMessageDto
import com.tap.hlc.HybridLogicalClock
import com.tap.hlc.NodeID
import com.tap.hlc.Timestamp
import com.tap.synk.Synk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 同步管理器
 *
 * 负责协调同步操作和处理CRDT冲突解决逻辑。这是应用程序中同步功能的核心组件，
 * 它使用HybridLogicalClock来维护分布式系统中事件的因果关系。
 */
@Singleton
class SyncManager @Inject constructor(
    private val syncRepository: SyncRepository,
    private val deviceIdManager: DeviceIdManager,
    private val synkAdapterRegistry: SynkAdapterRegistry,
    private val crdtKeyResolver: CrdtKeyResolver,
    private val synk: Synk,
) {
    companion object {
        private const val TAG = "SyncManager"
    }

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }


    /**
     * 初始化同步管理器
     *
     * 创建并配置HLC时钟，并启动周期性同步服务
     * @param coroutineScope 协程作用域
     */
    fun initialize(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            try {
                val deviceId = deviceIdManager.getOrCreateDeviceId()

                // 使用设备ID初始化HLC时钟，确保每个设备的时间戳都是唯一的
                val currentTime = Timestamp.now(kotlinx.datetime.Clock.System)
                hlcClock = HybridLogicalClock(
                    timestamp = currentTime,
                    node = NodeID(deviceId),
                    counter = 0
                )

                Log.d(TAG, "SyncManager initialized with device ID: $deviceId")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing SyncManager", e)
            }
        }
    }

    /**
     * 创建同步消息
     *
     * 为要同步的数据生成带有HLC时间戳的消息实体
     *
     * @param crdtKey 实体在分布式系统中的唯一标识符
     * @param entityType 实体类型
     * @param operationType 操作类型
     * @param userId 用户ID
     * @param payload 消息负载（实体JSON序列化数据）
     * @return 同步消息实体
     */
    suspend fun createSyncMessage(
        crdtKey: String,
        entityType: SyncConstants.EntityType,
        operationType: String,
        userId: Int,
        payload: String
    ): SyncMessageEntity {
        // 获取并更新混合逻辑时钟
        val updatedClock = synk.hlc.value

        // 创建带有时间戳的同步消息
        val messageEntity = SyncMessageEntity(
            syncId = 0,  // 自动生成的主键
            crdtKey = crdtKey,
            entityType = entityType.value,
            operationType = operationType,
            timestampWallClock = updatedClock.timestamp.epochMillis,
            timestampLogical = updatedClock.counter.toLong(),
            timestampNodeId = updatedClock.node.identifier,
            payload = payload,
            syncStatus = SyncConstants.SyncStatus.PENDING.name,
            userId = userId,
            deviceId = deviceIdManager.getOrCreateDeviceId(),
            createdAt = System.currentTimeMillis()
        )

        Log.d(TAG, "创建同步消息: $crdtKey, 类型: ${entityType.value}, 操作: $operationType")

        return messageEntity
    }

    /**
     * 创建并保存同步消息
     *
     * 使用实体类型的适配器将实体对象序列化为JSON，并创建带有时间戳的同步消息保存到本地数据库
     *
     * @param crdtKey 实体在分布式系统中的唯一标识符
     * @param entityType 实体类型
     * @param operationType 操作类型
     * @param userId 用户ID
     * @param entity 实体对象
     */
    /**
     * @param oldEntity 旧版本实体；若是新增则传 null
     */
    internal suspend inline fun <reified T : Syncable> createAndSaveSyncMessage(
        crdtKey: String,
        entityType: SyncConstants.EntityType,
        operationType: String,
        userId: Int,
        entity: T,
        oldEntity: T? = null,
    ) {
        try {
            // 获取实体类型对应的适配器
            val adapter = try {
                synkAdapterRegistry.getAdapter(entityType.value)
            } catch (e: Exception) {
                Log.e(TAG, "找不到实体类型的适配器: ${entityType.value}", e)
                return
            }

            // 序列化实体对象为Map
            // 使用两步转换来解决类型问题
            if (entity !is Syncable) {
                throw IllegalArgumentException("实体必须实现Syncable接口")
            }

            // 已由 Synk 直接处理序列化

            /* ---------------------------------------------------
             * 1️⃣ 使用 Synk 生成 Message 并序列化
             * --------------------------------------------------- */
            val message = synk.outbound(entity, oldEntity)
            val encodedMessage = synk.serializeOne(message)

            // 创建同步消息
            val messageEntity = createSyncMessage(
                crdtKey = crdtKey,
                entityType = entityType,
                operationType = operationType,
                userId = userId,
                payload = encodedMessage
            )

            // 将消息保存到本地数据库
            syncRepository.saveSyncMessage(messageEntity)

            Log.d(TAG, "同步消息已保存: $crdtKey, 类型: ${entityType.value}, 操作: $operationType")
        } catch (e: Exception) {
            Log.e(TAG, "创建和保存同步消息时出错: ${e.message}", e)
            throw e
        }
    }

    /**
     * 手动将Map<String, Any?>序列化为JSON字符串
     * 避免因为Any类型没有序列化器而导致的问题
     *
     * @param map 要序列化的Map
     * @return 序列化后的JSON字符串
     */
    private fun serializeMapToJson(map: Map<String, Any?>): String {
        val jsonObject = JSONObject()

        for ((key, value) in map) {
            when (value) {
                null -> jsonObject.put(key, JSONObject.NULL)
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    jsonObject.put(key, JSONObject(serializeMapToJson(value as Map<String, Any?>)))
                }

                is List<*> -> {
                    val jsonArray = org.json.JSONArray()
                    for (item in value) {
                        when (item) {
                            null -> jsonArray.put(JSONObject.NULL)
                            is Map<*, *> -> {
                                @Suppress("UNCHECKED_CAST")
                                jsonArray.put(serializeMapToJson(item as Map<String, Any?>))
                            }

                            else -> jsonArray.put(item.toString())
                        }
                    }
                    jsonObject.put(key, jsonArray)
                }

                else -> jsonObject.put(key, value.toString())
            }
        }
        return jsonObject.toString()
    }

    /**
     * 执行立即同步操作
     * @param ignoreExceptions 是否忽略同步中的异常，如果为true，则即使同步失败也不会影响调用方
     */
    suspend fun syncNow(ignoreExceptions: Boolean = false): Boolean {
        Log.d(TAG, "执行立即同步操作")

        try {
            _syncState.value = SyncState.SYNCING
            // 使用syncData方法代替syncAll，确保同时上传和下载消息
            syncRepository.syncData()
            _syncState.value = SyncState.SYNCED

            Log.d(TAG, "立即同步操作完成成功")
            return true
        } catch (e: Exception) {
            // 特殊处理协程取消异常 - 使用类名判断而不是类型检查
            if (e.javaClass.name.contains("JobCancellationException")) {
                Log.w(TAG, "同步作业被取消，但不影响本地操作: ${e.message}")
                _syncState.value = SyncState.CANCELED

                // 对于协程取消，我们认为这不是严重错误，总是允许本地操作继续
                return false
            }

            Log.e(TAG, "立即同步过程中发生错误: ${e.message}", e)
            _syncState.value = SyncState.FAILED

            if (ignoreExceptions) {
                Log.d(TAG, "已忽略同步异常，允许本地操作继续")
                return false
            } else {
                throw e
            }
        }
    }

    /**
     * 获取所有同步消息
     *
     * @return 同步消息流
     */
    fun getAllSyncMessages(): Flow<List<SyncMessageEntity>> {
        return syncRepository.getAllSyncMessages()
    }

    /**
     * 获取并更新混合逻辑时钟
     *
     * 为本地事件更新逻辑时钟，实现了HLC的localTick操作
     * @return 更新后的HybridLogicalClock
     */
    // 不能在suspend函数上使用@Synchronized，使用Mutex或其他机制来确保线程安全
    private suspend fun getAndUpdateClock(): HybridLogicalClock {
        var clock = hlcClock

        // 如果时钟尚未初始化，使用当前设备ID创建一个新的
        if (clock == null) {
            Log.d(TAG, "HLC时钟未初始化，创建新实例")

            val deviceId = deviceIdManager.getOrCreateDeviceId()
            val currentTime = Timestamp.now(kotlinx.datetime.Clock.System)

            clock = HybridLogicalClock(
                timestamp = currentTime,
                node = NodeID(deviceId),
                counter = 0
            )

            hlcClock = clock
        }

        // 获取当前物理时间
        val now = Timestamp.now(kotlinx.datetime.Clock.System)

        // 如果当前时间大于时钟的时间，使用当前时间更新时钟
        // 否则增加逻辑计数器，表示同一时刻的下一个事件
        val updatedClock = if (now.epochMillis > clock.timestamp.epochMillis) {
            clock.copy(
                timestamp = now,
                counter = 0
            )
        } else {
            clock.copy(
                counter = clock.counter + 1
            )
        }

        // 保存更新后的时钟
        hlcClock = updatedClock

        Log.d(TAG, "HLC时钟已更新: ${updatedClock.timestamp}, counter: ${updatedClock.counter}")

        return updatedClock
    }

    /**
     * 更新时钟与远程节点同步
     *
     * 接收远程消息时更新逻辑时钟，实现了HLC的remoteTock操作
     * @param remoteTimestamp 远程消息的时间戳
     * @param remoteCounter 远程消息的逻辑计数器
     * @param remoteNodeId 远程消息的节点ID
     * @return 更新后的HybridLogicalClock
     */
    // 不能在suspend函数上使用@Synchronized，使用Mutex或其他机制来确保线程安全
    private suspend fun updateClockWithRemote(
        remoteTimestamp: Long,
        remoteCounter: Int,
        remoteNodeId: String
    ): HybridLogicalClock {
        var clock = hlcClock

        // 如果时钟尚未初始化，使用当前设备ID创建一个新的
        if (clock == null) {
            Log.d(TAG, "HLC时钟未初始化，创建新实例")

            val deviceId = deviceIdManager.getOrCreateDeviceId()
            val currentTime = Timestamp.now(kotlinx.datetime.Clock.System)

            clock = HybridLogicalClock(
                timestamp = currentTime,
                node = NodeID(deviceId),
                counter = 0
            )

            hlcClock = clock
        }

        // 转换远程时间戳为Timestamp对象
        val remoteTime = Timestamp(remoteTimestamp)

        // 获取当前物理时间
        val now = Timestamp.now(kotlinx.datetime.Clock.System)

        // 计算最大时间戳：物理时钟、本地逻辑时钟和远程逻辑时钟的最大值
        // 使用手动比较而非 maxOf 函数，因为 Timestamp 实现了 Comparable 但不是直接在函数参数列表中支持
        val maxTimestamp = when {
            now.epochMillis > clock.timestamp.epochMillis && now.epochMillis > remoteTime.epochMillis -> now
            remoteTime.epochMillis > clock.timestamp.epochMillis && remoteTime.epochMillis > now.epochMillis -> remoteTime
            else -> clock.timestamp
        }

        // 根据时间戳比较结果更新计数器
        val updatedClock = when {
            // 如果远程时间戳大于本地时间戳，使用远程时间戳和计数器
            remoteTime.epochMillis > clock.timestamp.epochMillis -> {
                clock.copy(
                    timestamp = remoteTime,
                    counter = remoteCounter
                )
            }
            // 如果远程时间戳等于本地时间戳，使用更大的计数器
            remoteTime.epochMillis == clock.timestamp.epochMillis -> {
                clock.copy(
                    counter = Math.max(clock.counter, remoteCounter) + 1
                )
            }
            // 如果本地时间戳大于远程时间戳但物理时钟更新，使用物理时钟和重置计数器
            now.epochMillis > clock.timestamp.epochMillis -> {
                clock.copy(
                    timestamp = now,
                    counter = 0
                )
            }
            // 如果本地时间戳是最新的，增加计数器
            else -> {
                clock.copy(
                    counter = clock.counter + 1
                )
            }
        }

        // 保存更新后的时钟
        hlcClock = updatedClock

        Log.d(
            TAG,
            "HLC时钟已与远程节点同步: ${updatedClock.timestamp}, counter: ${updatedClock.counter}"
        )

        return updatedClock
    }

    /**
     * 处理从服务器接收到的消息
     *
     * 解析消息并应用CRDT合并逻辑，处理可能的冲突
     * @param messageDto 接收到的消息DTO
     */
    suspend fun processReceivedMessage(messageDto: SyncMessageDto) {
        try {
            Log.d(
                TAG,
                "【开始处理消息】实体类型=${messageDto.entityType}, 操作类型=${messageDto.operationType}, CRDT键=${messageDto.crdtKey}"
            )
            Log.d(
                TAG,
                "消息负载: ${messageDto.payload.take(100)}${if (messageDto.payload.length > 100) "..." else ""}"
            )


            // 根据实体类型获取对应的适配器
            val entityType = getEntityTypeFromString(messageDto.entityType)
            if (entityType == null) {
                Log.e(TAG, "【处理失败】未知的实体类型: ${messageDto.entityType}")
                return
            }

            val adapter = try {
                synkAdapterRegistry.getAdapter(entityType.value)
            } catch (e: Exception) {
                Log.e(TAG, "【处理失败】找不到实体类型的适配器: ${entityType.value}", e)
                return
            }

            Log.d(TAG, "【处理消息】开始处理${entityType.value}类型的消息")

            // 根据实体类型分发处理
            when (entityType) {
                SyncConstants.EntityType.COURSE -> processCourseEntity(messageDto, adapter)
                SyncConstants.EntityType.COURSE_NODE -> processCourseNodeEntity(messageDto, adapter)
                SyncConstants.EntityType.ORDINARY_SCHEDULE -> processOrdinaryScheduleEntity(
                    messageDto,
                    adapter
                )

                SyncConstants.EntityType.TABLE -> processTableEntity(messageDto, adapter)
                SyncConstants.EntityType.TIME_SLOT -> processTimeSlotEntity(messageDto, adapter)
                else -> Log.e(TAG, "【处理失败】无法处理的实体类型: ${entityType.value}")
            }

            Log.d(
                TAG,
                "【处理完成】成功处理消息 CRDT键=${messageDto.crdtKey}, 实体类型=${messageDto.entityType}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "【处理异常】处理消息 CRDT键=${messageDto.crdtKey} 时出错: ${e.message}", e)
        }
    }

    /**
     * 根据字符串获取实体类型枚举
     *
     * @param typeString 实体类型字符串
     * @return 实体类型枚举或null
     */
    private fun getEntityTypeFromString(typeString: String): SyncConstants.EntityType? {
        return SyncConstants.EntityType.values().find { it.value == typeString }
    }

    /**
     * 处理特定类型的实体消息
     *
     * @param messageDto 接收到的消息DTO
     * @param adapter 实体适配器
     */
    private suspend fun processCourseEntity(
        messageDto: SyncMessageDto,
        adapter: SynkAdapter<*>
    ) {
        try {
            Log.d(TAG, "【课程实体处理开始】CRDT键=${messageDto.crdtKey}")

            // 类型转换
            val typedAdapter = adapter as SynkAdapter<CourseEntity>

            // 解析消息负载
            val serializedMap = deserializeJsonToMap(messageDto.payload)
            val remoteEntity = typedAdapter.deserialize(serializedMap)

            Log.d(
                TAG,
                "【解析课程】课程名称=${remoteEntity.courseName}, 表ID=${remoteEntity.tableId}, 表CRDT键=${remoteEntity.tableCrdtKey}"
            )

            // 修复外键关系
            var fixedEntity = remoteEntity

            // 如果有tableCrdtKey，尝试找到对应的本地表ID
            if (!remoteEntity.tableCrdtKey.isNullOrBlank()) {
                val tableDao = syncRepository.getTableDao()
                val table = tableDao.getTableByCrdtKey(remoteEntity.tableCrdtKey!!)
                if (table != null) {
                    Log.d(
                        TAG,
                        "【课程实体修复】课程'${remoteEntity.courseName}'使用表CRDT键=${remoteEntity.tableCrdtKey}找到表ID=${table.id}"
                    )
                    fixedEntity = fixedEntity.copy(tableId = table.id)
                } else {
                    // 如果没有找到对应的表，尝试使用第一个可用的表
                    val allTables = tableDao.getAllTablesSync()
                    if (allTables.isNotEmpty()) {
                        val firstTable = allTables[0]
                        Log.d(
                            TAG,
                            "【课程实体修复】没有找到表CRDT键=${remoteEntity.tableCrdtKey}对应的表，使用第一个表ID=${firstTable.id}"
                        )
                        fixedEntity = fixedEntity.copy(tableId = firstTable.id)
                    } else {
                        Log.e(
                            TAG,
                            "【课程实体错误】没有可用的表ID，无法保存课程'${remoteEntity.courseName}'"
                        )
                    }
                }
            } else if (fixedEntity.tableId <= 0) {
                // 如果没有tableCrdtKey且tableId无效，尝试使用第一个可用的表
                val tableDao = syncRepository.getTableDao()
                val allTables = tableDao.getAllTablesSync()
                if (allTables.isNotEmpty()) {
                    val firstTable = allTables[0]
                    Log.d(
                        TAG,
                        "【课程实体修复】课程'${remoteEntity.courseName}'没有表CRDT键且表ID无效，使用第一个表ID=${firstTable.id}"
                    )
                    fixedEntity = fixedEntity.copy(tableId = firstTable.id)
                } else {
                    Log.e(
                        TAG,
                        "【课程实体错误】没有可用的表ID，无法保存课程'${remoteEntity.courseName}'"
                    )
                }
            }

            // 本地可能已经存在的实体
            Log.d(TAG, "【查找本地实体】尝试使用CRDT键 ${messageDto.crdtKey} 查找本地实体")
            val localEntity = syncRepository.getEntityByCrdtKey<CourseEntity>(messageDto.crdtKey)

            // 应用CRDT合并逻辑
            val mergedEntity = if (localEntity != null) {
                // 如果本地存在，执行冲突解决
                Log.d(TAG, "【合并实体】发现本地现有实体，执行CRDT合并")
                typedAdapter.merge(localEntity, fixedEntity)
            } else {
                // 如果本地不存在，直接使用修复后的实体
                Log.d(TAG, "【新建实体】本地不存在此实体，直接使用修复后的远程版本")
                fixedEntity
            }

            // 保存合并后的实体
            Log.d(TAG, "【保存实体】开始保存${messageDto.entityType}类型的实体到数据库")
            val saveSuccess = syncRepository.saveEntity(mergedEntity)

            if (saveSuccess) {
                Log.d(TAG, "【保存成功】实体已成功保存到数据库")

                // 保存同步消息以供追踪，并明确标记为已同步状态
                val localMessage = messageDto.toEntity()
                    .copy(syncStatus = SyncConstants.SyncStatus.SYNCED.name)  // 明确标记为已同步，防止被重新上传
                syncRepository.saveSyncMessage(localMessage)
                Log.d(TAG, "【消息标记】已将下载消息标记为SYNCED状态，防止重复上传")

                Log.d(TAG, "【实体处理完成】成功处理并保存 ${messageDto.entityType} 类型实体")
            } else {
                Log.e(
                    TAG,
                    "【保存失败】无法保存 ${messageDto.entityType} 类型实体到数据库，可能存在外键关系问题"
                )
                Log.e(
                    TAG,
                    "【课程信息】课程名称=${mergedEntity.courseName}, 表ID=${mergedEntity.tableId}, 表CRDT键=${mergedEntity.tableCrdtKey}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "【课程实体处理错误】处理课程实体时发生错误: ${e.message}", e)
            e.printStackTrace()
        }
    }

    /**
     * 处理特定类型的实体消息
     *
     * @param messageDto 接收到的消息DTO
     * @param adapter 实体适配器
     */
    private suspend fun processCourseNodeEntity(
        messageDto: SyncMessageDto,
        adapter: SynkAdapter<*>
    ) {
        try {
            Log.d(TAG, "【课程节点实体处理开始】CRDT键=${messageDto.crdtKey}")

            // 类型转换
            val typedAdapter = adapter as SynkAdapter<CourseNodeEntity>

            // 解析消息负载
            val serializedMap = deserializeJsonToMap(messageDto.payload)
            val remoteEntity = typedAdapter.deserialize(serializedMap)

            Log.d(
                TAG,
                "【解析课程节点】课程ID=${remoteEntity.courseId}, 课程CRDT键=${remoteEntity.courseCrdtKey}, 星期=${remoteEntity.day}"
            )

            // 修复外键关系
            var fixedEntity = remoteEntity

            // 如果有courseCrdtKey，尝试找到对应的本地课程ID
            if (!remoteEntity.courseCrdtKey.isNullOrBlank()) {
                val courseDao = syncRepository.getCourseDao()
                val course = courseDao.getCourseByCrdtKey(remoteEntity.courseCrdtKey!!)
                if (course != null) {
                    Log.d(
                        TAG,
                        "【课程节点实体修复】使用课程CRDT键=${remoteEntity.courseCrdtKey}找到课程ID=${course.id}"
                    )
                    fixedEntity = fixedEntity.copy(courseId = course.id)
                } else {
                    // 如果没有找到对应的课程，尝试使用第一个可用的课程
                    val allCourses = courseDao.getAllCourses()
                    if (allCourses.isNotEmpty()) {
                        val firstCourse = allCourses[0]
                        Log.d(
                            TAG,
                            "【课程节点实体修复】没有找到课程CRDT键=${remoteEntity.courseCrdtKey}对应的课程，使用第一个课程ID=${firstCourse.id}"
                        )
                        fixedEntity = fixedEntity.copy(courseId = firstCourse.id)
                    } else {
                        Log.e(TAG, "【课程节点实体错误】没有可用的课程ID，无法保存课程节点")
                    }
                }
            } else if (fixedEntity.courseId <= 0) {
                // 如果没有courseCrdtKey且courseId无效，尝试使用第一个可用的课程
                val courseDao = syncRepository.getCourseDao()
                val allCourses = courseDao.getAllCourses()
                if (allCourses.isNotEmpty()) {
                    val firstCourse = allCourses[0]
                    Log.d(
                        TAG,
                        "【课程节点实体修复】没有课程CRDT键且课程ID无效，使用第一个课程ID=${firstCourse.id}"
                    )
                    fixedEntity = fixedEntity.copy(courseId = firstCourse.id)
                } else {
                    Log.e(TAG, "【课程节点实体错误】没有可用的课程ID，无法保存课程节点")
                }
            }

            // 本地可能已经存在的实体
            Log.d(TAG, "【查找本地实体】尝试使用CRDT键 ${messageDto.crdtKey} 查找本地实体")
            val localEntity =
                syncRepository.getEntityByCrdtKey<CourseNodeEntity>(messageDto.crdtKey)

            // 应用CRDT合并逻辑
            val mergedEntity = if (localEntity != null) {
                // 如果本地存在，执行冲突解决
                Log.d(TAG, "【合并实体】发现本地现有实体，执行CRDT合并")
                typedAdapter.merge(localEntity, fixedEntity)
            } else {
                // 如果本地不存在，直接使用修复后的实体
                Log.d(TAG, "【新建实体】本地不存在此实体，直接使用修复后的远程版本")
                fixedEntity
            }

            // 保存合并后的实体
            Log.d(TAG, "【保存实体】开始保存${messageDto.entityType}类型的实体到数据库")
            val saveSuccess = syncRepository.saveEntity(mergedEntity)

            if (saveSuccess) {
                Log.d(TAG, "【保存成功】实体已成功保存到数据库")

                // 保存同步消息以供追踪，并明确标记为已同步状态
                val localMessage = messageDto.toEntity()
                    .copy(syncStatus = SyncConstants.SyncStatus.SYNCED.name)  // 明确标记为已同步，防止被重新上传
                syncRepository.saveSyncMessage(localMessage)
                Log.d(TAG, "【消息标记】已将下载消息标记为SYNCED状态，防止重复上传")

                Log.d(TAG, "【实体处理完成】成功处理并保存 ${messageDto.entityType} 类型实体")
            } else {
                Log.e(
                    TAG,
                    "【保存失败】无法保存 ${messageDto.entityType} 类型实体到数据库，可能存在外键关系问题"
                )
                Log.e(
                    TAG,
                    "【课程节点信息】课程ID=${mergedEntity.courseId}, 课程CRDT键=${mergedEntity.courseCrdtKey}, 星期=${mergedEntity.day}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "【课程节点实体处理错误】处理课程节点实体时发生错误: ${e.message}", e)
            e.printStackTrace()
        }
    }

    private suspend fun processTableEntity(
        messageDto: SyncMessageDto,
        adapter: SynkAdapter<*>
    ) {
        try {
            Log.d(TAG, "【表实体处理开始】CRDT键=${messageDto.crdtKey}")

            // 类型转换
            val typedAdapter = adapter as SynkAdapter<TableEntity>

            // 解析消息负载
            val serializedMap = deserializeJsonToMap(messageDto.payload)
            val remoteEntity = typedAdapter.deserialize(serializedMap)

            Log.d(TAG, "【解析表】表名称=${remoteEntity.tableName}")

            // 修复外键关系
            var fixedEntity = remoteEntity

            // 使用本地userId
            val userId = syncRepository.getUserIdFromSession()?.toInt() ?: 1
            Log.d(TAG, "【表实体修复】表'${remoteEntity.tableName}'使用本地用户ID: $userId")
            fixedEntity = fixedEntity.copy(userId = userId)

            // 本地可能已经存在的实体
            Log.d(TAG, "【查找本地实体】尝试使用CRDT键 ${messageDto.crdtKey} 查找本地实体")
            val localEntity = syncRepository.getEntityByCrdtKey<TableEntity>(messageDto.crdtKey)

            // 应用CRDT合并逻辑
            val mergedEntity = if (localEntity != null) {
                // 如果本地存在，执行冲突解决
                Log.d(TAG, "【合并实体】发现本地现有实体，执行CRDT合并")
                typedAdapter.merge(localEntity, fixedEntity)
            } else {
                // 如果本地不存在，直接使用修复后的实体
                Log.d(TAG, "【新建实体】本地不存在此实体，直接使用修复后的远程版本")
                fixedEntity
            }

            // 保存合并后的实体
            Log.d(TAG, "【保存实体】开始保存${messageDto.entityType}类型的实体到数据库")
            val saveSuccess = syncRepository.saveEntity(mergedEntity)

            if (saveSuccess) {
                Log.d(TAG, "【保存成功】实体已成功保存到数据库")

                // 保存同步消息以供追踪，并明确标记为已同步状态
                val localMessage = messageDto.toEntity()
                    .copy(syncStatus = SyncConstants.SyncStatus.SYNCED.name)  // 明确标记为已同步，防止被重新上传
                syncRepository.saveSyncMessage(localMessage)
                Log.d(TAG, "【消息标记】已将下载消息标记为SYNCED状态，防止重复上传")

                Log.d(TAG, "【实体处理完成】成功处理并保存 ${messageDto.entityType} 类型实体")
            } else {
                Log.e(
                    TAG,
                    "【保存失败】无法保存 ${messageDto.entityType} 类型实体到数据库，可能存在外键关系问题"
                )
                Log.e(
                    TAG,
                    "【表信息】表名称=${mergedEntity.tableName}, 用户ID=${mergedEntity.userId}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "【表实体处理错误】处理表实体时发生错误: ${e.message}", e)
            e.printStackTrace()
        }
    }

    private suspend fun processOrdinaryScheduleEntity(
        messageDto: SyncMessageDto,
        adapter: SynkAdapter<*>
    ) {
        try {
            Log.d(TAG, "【日程实体处理开始】CRDT键=${messageDto.crdtKey}")

            // 类型转换
            val typedAdapter = adapter as SynkAdapter<OrdinaryScheduleEntity>

            // 解析消息负载
            val serializedMap = deserializeJsonToMap(messageDto.payload)
            val remoteEntity = typedAdapter.deserialize(serializedMap)

            Log.d(TAG, "【解析日程】日程标题=${remoteEntity.title}, 用户ID=${remoteEntity.userId}")

            // 修复外键关系
            var fixedEntity = remoteEntity

            // 使用本地userId
            val userId = syncRepository.getUserIdFromSession()?.toInt() ?: 1
            Log.d(TAG, "【日程实体修复】日程'${remoteEntity.title}'使用本地用户ID: $userId")
            fixedEntity = fixedEntity.copy(userId = userId)

            // 本地可能已经存在的实体
            Log.d(TAG, "【查找本地实体】尝试使用CRDT键 ${messageDto.crdtKey} 查找本地实体")
            val localEntity =
                syncRepository.getEntityByCrdtKey<OrdinaryScheduleEntity>(messageDto.crdtKey)

            // 应用CRDT合并逻辑
            val mergedEntity = if (localEntity != null) {
                // 如果本地存在，执行冲突解决
                Log.d(TAG, "【合并实体】发现本地现有实体，执行CRDT合并")
                typedAdapter.merge(localEntity, fixedEntity)
            } else {
                // 如果本地不存在，直接使用修复后的实体
                Log.d(TAG, "【新建实体】本地不存在此实体，直接使用修复后的远程版本")
                fixedEntity
            }

            // 保存合并后的实体
            Log.d(TAG, "【保存实体】开始保存${messageDto.entityType}类型的实体到数据库")
            val saveSuccess = syncRepository.saveEntity(mergedEntity)

            if (saveSuccess) {
                Log.d(TAG, "【保存成功】实体已成功保存到数据库")

                // 保存同步消息以供追踪，并明确标记为已同步状态
                val localMessage = messageDto.toEntity()
                    .copy(syncStatus = SyncConstants.SyncStatus.SYNCED.name)  // 明确标记为已同步，防止被重新上传
                syncRepository.saveSyncMessage(localMessage)
                Log.d(TAG, "【消息标记】已将下载消息标记为SYNCED状态，防止重复上传")

                Log.d(TAG, "【实体处理完成】成功处理并保存 ${messageDto.entityType} 类型实体")
            } else {
                Log.e(
                    TAG,
                    "【保存失败】无法保存 ${messageDto.entityType} 类型实体到数据库，可能存在外键关系问题"
                )
                Log.e(
                    TAG,
                    "【日程信息】日程标题=${mergedEntity.title}, 用户ID=${mergedEntity.userId}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "【日程实体处理错误】处理日程实体时发生错误: ${e.message}", e)
            e.printStackTrace()
        }
    }

    private suspend fun processTimeSlotEntity(
        messageDto: SyncMessageDto,
        adapter: SynkAdapter<*>
    ) {
        try {
            Log.d(TAG, "【时间段实体处理开始】CRDT键=${messageDto.crdtKey}")

            // 类型转换
            val typedAdapter = adapter as SynkAdapter<TimeSlotEntity>

            // 解析消息负载
            val serializedMap = deserializeJsonToMap(messageDto.payload)
            val remoteEntity = typedAdapter.deserialize(serializedMap)

            Log.d(
                TAG,
                "【解析时间段】开始时间=${remoteEntity.startTime}, 结束时间=${remoteEntity.endTime}"
            )

            // 修复外键关系
            var fixedEntity = remoteEntity

            // 使用本地userId
            val userId = syncRepository.getUserIdFromSession()?.toInt() ?: 1
            fixedEntity = fixedEntity.copy(userId = userId)

            // 如果有scheduleCrdtKey，尝试找到对应的本地日程ID
            if (!remoteEntity.scheduleCrdtKey.isNullOrBlank()) {
                val scheduleDao = syncRepository.getOrdinaryScheduleDao()
                val schedule =
                    scheduleDao.getOrdinaryScheduleByCrdtKey(remoteEntity.scheduleCrdtKey!!)
                if (schedule != null) {
                    Log.d(
                        TAG,
                        "【时间段实体修复】使用日程CRDT键=${remoteEntity.scheduleCrdtKey}找到日程ID=${schedule.id}"
                    )
                    fixedEntity = fixedEntity.copy(scheduleId = schedule.id)
                } else {
                    // 如果没有找到对应的日程，尝试使用第一个可用的日程
                    val allSchedules = scheduleDao.fetchSchedulesByUserId(userId)
                    if (allSchedules.isNotEmpty()) {
                        val firstSchedule = allSchedules[0]
                        Log.d(
                            TAG,
                            "【时间段实体修复】没有找到日程CRDT键=${remoteEntity.scheduleCrdtKey}对应的日程，使用第一个日程ID=${firstSchedule.id}"
                        )
                        fixedEntity = fixedEntity.copy(scheduleId = firstSchedule.id)
                    } else {
                        Log.e(TAG, "【时间段实体错误】没有可用的日程ID，无法保存时间段")
                    }
                }
            } else if (fixedEntity.scheduleId <= 0) {
                // 如果没有scheduleCrdtKey且scheduleId无效，尝试使用第一个可用的日程
                val scheduleDao = syncRepository.getOrdinaryScheduleDao()
                val allSchedules = scheduleDao.fetchSchedulesByUserId(userId)
                if (allSchedules.isNotEmpty()) {
                    val firstSchedule = allSchedules[0]
                    Log.d(
                        TAG,
                        "【时间段实体修复】没有日程CRDT键且日程ID无效，使用第一个日程ID=${firstSchedule.id}"
                    )
                    fixedEntity = fixedEntity.copy(scheduleId = firstSchedule.id)
                } else {
                    Log.e(TAG, "【时间段实体错误】没有可用的日程ID，无法保存时间段")
                }
            }

            Log.d(
                TAG,
                "【时间段实体修复】时间段ID=${remoteEntity.id}使用本地用户ID: $userId, 日程ID: ${fixedEntity.scheduleId}"
            )

            // 本地可能已经存在的实体
            Log.d(TAG, "【查找本地实体】尝试使用CRDT键 ${messageDto.crdtKey} 查找本地实体")
            val localEntity = syncRepository.getEntityByCrdtKey<TimeSlotEntity>(messageDto.crdtKey)

            // 应用CRDT合并逻辑
            val mergedEntity = if (localEntity != null) {
                // 如果本地存在，执行冲突解决
                Log.d(TAG, "【合并实体】发现本地现有实体，执行CRDT合并")
                typedAdapter.merge(localEntity, fixedEntity)
            } else {
                // 如果本地不存在，直接使用修复后的实体
                Log.d(TAG, "【新建实体】本地不存在此实体，直接使用修复后的远程版本")
                fixedEntity
            }

            // 保存合并后的实体
            Log.d(TAG, "【保存实体】开始保存${messageDto.entityType}类型的实体到数据库")
            val saveSuccess = syncRepository.saveEntity(mergedEntity)

            if (saveSuccess) {
                Log.d(TAG, "【保存成功】实体已成功保存到数据库")

                // 保存同步消息以供追踪，并明确标记为已同步状态
                val localMessage = messageDto.toEntity()
                    .copy(syncStatus = SyncConstants.SyncStatus.SYNCED.name)  // 明确标记为已同步，防止被重新上传
                syncRepository.saveSyncMessage(localMessage)
                Log.d(TAG, "【消息标记】已将下载消息标记为SYNCED状态，防止重复上传")

                Log.d(TAG, "【实体处理完成】成功处理并保存 ${messageDto.entityType} 类型实体")
            } else {
                Log.e(
                    TAG,
                    "【保存失败】无法保存 ${messageDto.entityType} 类型实体到数据库，可能存在外键关系问题"
                )
                Log.e(
                    TAG,
                    "【时间段信息】日程ID=${mergedEntity.scheduleId}, 用户ID=${mergedEntity.userId}, 开始时间=${mergedEntity.startTime}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "【时间段实体处理错误】处理时间段实体时发生错误: ${e.message}", e)
            e.printStackTrace()
        }
    }

    /**
     * 将JSON字符串反序列化为Map<String, Any?>
     * 与serializeMapToJson方法配对使用
     *
     * @param json JSON字符串
     * @return 反序列化后的Map
     */
    private fun deserializeJsonToMap(json: String): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        val jsonObject = org.json.JSONObject(json)
        val keys = jsonObject.keys()

        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.get(key)

            result[key] = when {
                value == JSONObject.NULL -> null
                value is org.json.JSONObject -> deserializeJsonToMap(value.toString())
                value is org.json.JSONArray -> {
                    val list = mutableListOf<Any?>()
                    for (i in 0 until value.length()) {
                        val item = value.get(i)
                        list.add(
                            when {
                                item == JSONObject.NULL -> null
                                item is org.json.JSONObject -> deserializeJsonToMap(item.toString())
                                item is org.json.JSONArray -> throw IllegalArgumentException("不支持嵌套JSONArray")
                                else -> item
                            }
                        )
                    }
                    list
                }

                else -> value
            }
        }

        return result
    }

    /**
     * 同步状态枚举
     */
    enum class SyncState {
        IDLE,       // 空闲
        SYNCING,    // 同步中
        SYNCED,     // 同步完成
        FAILED,     // 同步失败
        CANCELED    // 同步被取消
    }

    /**
     * 获取同步仓库实例
     * 允许外部组件访问同步仓库以执行特定操作
     * @return SyncRepository实例
     */
    fun getSyncRepository(): SyncRepository {
        return syncRepository
    }
}
