package com.example.todoschedule.data.repository

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoschedule.data.database.AppDatabase
import com.example.todoschedule.data.database.converter.ReminderType
import com.example.todoschedule.data.database.converter.ScheduleStatus
import com.example.todoschedule.data.database.converter.ScheduleType
import com.example.todoschedule.data.database.dao.OrdinaryScheduleDao
import com.example.todoschedule.data.database.dao.TimeSlotDao
import com.example.todoschedule.data.database.dao.UserDao
import com.example.todoschedule.data.database.entity.UserEntity
import com.example.todoschedule.domain.model.OrdinarySchedule
import com.example.todoschedule.domain.model.TimeSlot
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class OrdinaryScheduleRepositoryImplTest {

    // 规则确保 LiveData 和 Coroutines 能在测试线程上同步执行
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var ordinaryScheduleDao: OrdinaryScheduleDao
    private lateinit var timeSlotDao: TimeSlotDao
    private lateinit var userDao: UserDao // 假设你有 UserDao
    private lateinit var repository: OrdinaryScheduleRepositoryImpl

    private val testUser = UserEntity(id = 1, username = "testuser")

    @Before
    fun createDb() = runTest { // 使用 runTest 运行 suspend 函数
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            // 允许在主线程查询 (仅用于测试!)
            .allowMainThreadQueries()
            .build()
        ordinaryScheduleDao = database.ordinaryScheduleDao()
        timeSlotDao = database.timeSlotDao()
        userDao = database.userDao()

        repository = OrdinaryScheduleRepositoryImpl(ordinaryScheduleDao, timeSlotDao)

        // 插入测试用户，为外键约束做准备
        userDao.insertUser(testUser)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    // --- 测试 Create 操作 ---

    @Test
    @Throws(Exception::class)
    fun insertSchedule_insertsScheduleAndTimeSlotsCorrectly() = runTest {
        val timeSlot1 = createTestTimeSlot(
            userId = testUser.id,
            scheduleId = 0,
            startTimeOffset = 0
        ) // scheduleId 初始为 0
        val timeSlot2 = createTestTimeSlot(
            userId = testUser.id,
            scheduleId = 0,
            startTimeOffset = 3600_000
        ) // 1小时后
        val schedule =
            createTestSchedule(id = 0, timeSlots = listOf(timeSlot1, timeSlot2)) // id 初始为 0

        val insertedScheduleId = repository.insertSchedule(schedule)

        // 验证返回的 ID
        assertThat(insertedScheduleId).isGreaterThan(0L)

        // 验证 Schedule 是否插入
        val retrievedSchedule = repository.getScheduleById(insertedScheduleId.toInt()).first()
        assertThat(retrievedSchedule).isNotNull()
        assertThat(retrievedSchedule?.id).isEqualTo(insertedScheduleId.toInt())
        assertThat(retrievedSchedule?.title).isEqualTo(schedule.title)
        assertThat(retrievedSchedule?.status).isEqualTo(ScheduleStatus.TODO) // 验证默认/转换后的值

        // 验证 TimeSlots 是否插入并关联正确
        assertThat(retrievedSchedule?.timeSlots).hasSize(2)
        val retrievedTimeSlot1 =
            retrievedSchedule?.timeSlots?.find { it.startTime == timeSlot1.startTime }
        val retrievedTimeSlot2 =
            retrievedSchedule?.timeSlots?.find { it.startTime == timeSlot2.startTime }

        assertThat(retrievedTimeSlot1).isNotNull()
        assertThat(retrievedTimeSlot1?.scheduleId).isEqualTo(insertedScheduleId.toInt())
        assertThat(retrievedTimeSlot1?.scheduleType).isEqualTo(ScheduleType.ORDINARY)
        assertThat(retrievedTimeSlot1?.userId).isEqualTo(testUser.id)
        assertThat(retrievedTimeSlot1?.reminderType).isEqualTo(ReminderType.NOTIFICATION) // 验证默认/转换后的值

        assertThat(retrievedTimeSlot2).isNotNull()
        assertThat(retrievedTimeSlot2?.scheduleId).isEqualTo(insertedScheduleId.toInt())
        assertThat(retrievedTimeSlot2?.userId).isEqualTo(testUser.id)
    }

    @Test
    fun insertSchedules_insertsMultipleSchedulesAndTimeSlots() = runTest {
        val schedule1 = createTestSchedule(
            id = 0,
            title = "Schedule 1",
            timeSlots = listOf(createTestTimeSlot(testUser.id, 0))
        )
        val schedule2 = createTestSchedule(
            id = 0,
            title = "Schedule 2",
            timeSlots = listOf(
                createTestTimeSlot(testUser.id, 0),
                createTestTimeSlot(testUser.id, 0, 3600_000)
            )
        )

        repository.insertSchedules(listOf(schedule1, schedule2))

        val allSchedules = repository.getAllSchedules().first()
        assertThat(allSchedules).hasSize(2)

        val retrievedSchedule1 = allSchedules.find { it.title == "Schedule 1" }
        val retrievedSchedule2 = allSchedules.find { it.title == "Schedule 2" }

        assertThat(retrievedSchedule1).isNotNull()
        assertThat(retrievedSchedule1?.timeSlots).hasSize(1)
        assertThat(retrievedSchedule1?.timeSlots?.first()?.scheduleId).isEqualTo(retrievedSchedule1?.id)
        assertThat(retrievedSchedule1?.timeSlots?.first()?.userId).isEqualTo(testUser.id)


        assertThat(retrievedSchedule2).isNotNull()
        assertThat(retrievedSchedule2?.timeSlots).hasSize(2)
        assertThat(retrievedSchedule2?.timeSlots?.all { it.scheduleId == retrievedSchedule2.id }).isTrue()
        assertThat(retrievedSchedule2?.timeSlots?.all { it.userId == testUser.id }).isTrue()
    }

    // --- 测试 Read 操作 ---

    @Test
    fun getScheduleById_returnsCorrectSchedule() = runTest {
        val schedule = createTestSchedule(timeSlots = listOf(createTestTimeSlot(testUser.id, 0)))
        val insertedId = repository.insertSchedule(schedule)

        val retrieved = repository.getScheduleById(insertedId.toInt()).first()

        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.id).isEqualTo(insertedId.toInt())
        assertThat(retrieved?.title).isEqualTo(schedule.title)
        assertThat(retrieved?.timeSlots).hasSize(1)
    }

    @Test
    fun getScheduleById_returnsNullForNonExistentId() = runTest {
        val retrieved = repository.getScheduleById(999).first()
        assertThat(retrieved).isNull()
    }

    @Test
    fun getAllSchedules_returnsAllInsertedSchedules() = runTest {
        val schedule1 = createTestSchedule(
            title = "All 1",
            timeSlots = listOf(createTestTimeSlot(testUser.id, 0))
        )
        val schedule2 = createTestSchedule(
            title = "All 2",
            timeSlots = listOf(createTestTimeSlot(testUser.id, 0))
        )
        repository.insertSchedule(schedule1)
        repository.insertSchedule(schedule2)

        val all = repository.getAllSchedules().first()
        assertThat(all).hasSize(2)
        assertThat(all.any { it.title == "All 1" }).isTrue()
        assertThat(all.any { it.title == "All 2" }).isTrue()
    }

    @Test
    fun getAllSchedules_returnsEmptyListWhenDbIsEmpty() = runTest {
        val all = repository.getAllSchedules().first()
        assertThat(all).isEmpty()
    }

    // --- 测试 Update 操作 ---

    @Test
    fun updateSchedule_updatesTitleAndReplacesTimeSlots() = runTest {
        val initialTimeSlot = createTestTimeSlot(testUser.id, 0, startTimeOffset = 0)
        val initialSchedule = createTestSchedule(timeSlots = listOf(initialTimeSlot))
        val insertedId = repository.insertSchedule(initialSchedule)

        // 更新
        val updatedTimeSlot1 =
            createTestTimeSlot(testUser.id, insertedId.toInt(), startTimeOffset = 7200_000) // 新时间 1
        val updatedTimeSlot2 = createTestTimeSlot(
            testUser.id,
            insertedId.toInt(),
            startTimeOffset = 10800_000
        ) // 新时间 2
        val scheduleToUpdate = initialSchedule.copy(
            id = insertedId.toInt(),
            title = "Updated Title",
            status = ScheduleStatus.IN_PROGRESS,
            timeSlots = listOf(updatedTimeSlot1, updatedTimeSlot2) // 替换时间槽
        )

        repository.updateSchedule(scheduleToUpdate)

        // 验证
        val retrieved = repository.getScheduleById(insertedId.toInt()).first()
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.id).isEqualTo(insertedId.toInt())
        assertThat(retrieved?.title).isEqualTo("Updated Title")
        assertThat(retrieved?.status).isEqualTo(ScheduleStatus.IN_PROGRESS)
        assertThat(retrieved?.timeSlots).hasSize(2)

        // 检查旧 time slot 是否被删除 (通过时间判断)
        assertThat(retrieved?.timeSlots?.any { it.startTime == initialTimeSlot.startTime }).isFalse()
        // 检查新 time slot 是否存在
        assertThat(retrieved?.timeSlots?.any { it.startTime == updatedTimeSlot1.startTime }).isTrue()
        assertThat(retrieved?.timeSlots?.any { it.startTime == updatedTimeSlot2.startTime }).isTrue()
        // 检查新 time slot 的关联 ID
        assertThat(retrieved?.timeSlots?.all { it.scheduleId == insertedId.toInt() }).isTrue()
        assertThat(retrieved?.timeSlots?.all { it.userId == testUser.id }).isTrue()
    }

    // --- 测试 Delete 操作 ---

    @Test
    fun deleteSchedule_deletesScheduleAndAssociatedTimeSlots() = runTest {
        val timeSlot = createTestTimeSlot(testUser.id, 0)
        val schedule = createTestSchedule(timeSlots = listOf(timeSlot))
        val insertedId = repository.insertSchedule(schedule)

        // 确认插入成功
        var retrievedSchedule = repository.getScheduleById(insertedId.toInt()).first()
        assertThat(retrievedSchedule).isNotNull()
        var retrievedTimeSlots =
            timeSlotDao.getTimeSlotsBySchedule(ScheduleType.ORDINARY, insertedId.toInt()).first()
        assertThat(retrievedTimeSlots).isNotEmpty() // 直接查 TimeSlotDao 确认

        // 删除
        repository.deleteSchedule(retrievedSchedule!!) // 使用 !! 因为我们确认它非空

        // 验证 Schedule 是否删除
        retrievedSchedule = repository.getScheduleById(insertedId.toInt()).first()
        assertThat(retrievedSchedule).isNull()

        // 验证关联 TimeSlots 是否删除
        retrievedTimeSlots =
            timeSlotDao.getTimeSlotsBySchedule(ScheduleType.ORDINARY, insertedId.toInt()).first()
        assertThat(retrievedTimeSlots).isEmpty()
    }

    @Test
    fun deleteAllSchedules_deletesAllOrdinarySchedulesAndTheirTimeSlots() = runTest {
        // 插入两个普通日程
        val schedule1 = createTestSchedule(
            title = "DeleteAll 1",
            timeSlots = listOf(createTestTimeSlot(testUser.id, 0))
        )
        val schedule2 = createTestSchedule(
            title = "DeleteAll 2",
            timeSlots = listOf(createTestTimeSlot(testUser.id, 0))
        )
        repository.insertSchedule(schedule1)
        repository.insertSchedule(schedule2)

        // (可选) 插入一个非普通日程的时间槽，它不应被删除
        val courseTimeSlot = createTestTimeSlot(testUser.id, 99, startTimeOffset = 1000).toEntity(
            99,
            ScheduleType.COURSE.name
        )
        timeSlotDao.insertTimeSlot(courseTimeSlot)


        // 确认插入
        assertThat(repository.getAllSchedules().first()).hasSize(2)
        assertThat(timeSlotDao.getAllTimeSlots().first()).hasSize(3) // 2 ordinary + 1 course

        // 删除所有
        repository.deleteAllSchedules()

        // 验证普通日程已删除
        assertThat(repository.getAllSchedules().first()).isEmpty()

        // 验证普通日程的时间槽已删除，但其他类型的时间槽保留
        val remainingTimeSlots = timeSlotDao.getAllTimeSlots().first()
        assertThat(remainingTimeSlots).hasSize(1)
        assertThat(remainingTimeSlots.first().scheduleType).isEqualTo(ScheduleType.COURSE)
    }

    // --- 测试外键约束 (TimeSlot -> User) ---

    @Test
    fun insertTimeSlot_succeedsWithValidUserId() = runTest {
        // Repository 封装了 TimeSlot 插入，但我们可以直接测试 DAO 确认外键
        val timeSlotEntity =
            createTestTimeSlot(testUser.id, 1).toEntity(1, ScheduleType.ORDINARY.name)
        val result = timeSlotDao.insertTimeSlot(timeSlotEntity)
        assertThat(result).isGreaterThan(0L) // 确认插入成功

        val retrieved = timeSlotDao.getTimeSlotById(result.toInt()).first()
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.userId).isEqualTo(testUser.id)
    }

    // 注意: Room/SQLite 对于插入时外键约束的检查可能不总是立即失败。
    // 删除父记录是更可靠的测试级联删除的方法。
    // @Test(expected = SQLiteConstraintException::class) // 期望异常可能不可靠
    // fun insertTimeSlot_failsWithInvalidUserId() = runTest { ... }


    @Test
    fun deleteUser_cascadesToDeleteAssociatedTimeSlots() = runTest {
        // 插入与用户关联的时间槽
        val timeSlot = createTestTimeSlot(testUser.id, 1)
        val schedule = createTestSchedule(id = 1, timeSlots = listOf(timeSlot))
        repository.insertSchedule(schedule) // 通过 repository 插入

        // 确认 TimeSlot 存在
        var timeSlots = timeSlotDao.getAllTimeSlots().first()
        assertThat(timeSlots.any { it.userId == testUser.id }).isTrue()

        // 删除用户 (通过 UserDao)
        userDao.deleteUser(testUser.id)

        // 验证关联的 TimeSlot 是否已被级联删除
        timeSlots = timeSlotDao.getAllTimeSlots().first()
        assertThat(timeSlots.none { it.userId == testUser.id }).isTrue() // 确认没有该用户的 time slot 了
        // 或者如果确定此时 time_slot 表应为空
        // assertThat(timeSlots).isEmpty()
    }


    // --- 辅助函数创建测试数据 ---

    private fun createTestSchedule(
        Id: UUID = 0, // 让 Room 自动生成 ID 通常更好，但测试中指定方便验证
        title: String = "Test Schedule",
        status: ScheduleStatus = ScheduleStatus.TODO,
        timeSlots: List<TimeSlot> = emptyList()
    ): OrdinarySchedule {
        return OrdinarySchedule(
            id = id,
            title = title,
            description = "Test Description",
            location = "Test Location",
            category = "Test Category",
            color = "#FFFFFF",
            isAllDay = false,
            status = status,
            timeSlots = timeSlots
        )
    }

    private fun createTestTimeSlot(
        userId: UUID,
        scheduleId: UUID,
        startTimeOffset: Long = 0, // 相对当前时间的偏移量（毫秒）
        Id: UUID = 0
    ): TimeSlot {
        val now = Clock.System.now().toEpochMilliseconds()
        return TimeSlot(
            id = id,
            userId = userId,
            scheduleId = scheduleId, // 初始可能为 0，插入后更新
            startTime = now + startTimeOffset,
            endTime = now + startTimeOffset + 3600_000, // 持续 1 小时
            scheduleType = ScheduleType.ORDINARY, // Repository 会处理
            head = "Test Slot Head",
            priority = 1,
            isCompleted = false,
            isRepeated = false,
            repeatPattern = null,
            reminderType = ReminderType.NOTIFICATION, // 默认提醒
            reminderOffset = 900_000 // 提前 15 分钟
        )
    }

    // 需要一个 TimeSlot 的 toEntity 扩展函数，如果它不在 repository 或 mapper 中
    // （之前的步骤中已将其放在 repository 中作为私有函数，这里为了方便测试再写一个）
    // 如果你的 mapper 是公共的，可以直接导入使用。
    private fun TimeSlot.toEntity(
        scheduleId: UUID,
        scheduleTypeStr: String
    ): com.example.todoschedule.data.database.entity.TimeSlotEntity {
        val typeEnum = try {
            ScheduleType.valueOf(scheduleTypeStr)
        } catch (_: IllegalArgumentException) {
            ScheduleType.ORDINARY
        }
        return com.example.todoschedule.data.database.entity.TimeSlotEntity(
            id = this.id,
            userId = this.userId,
            startTime = this.startTime,
            endTime = this.endTime,
            scheduleType = typeEnum,
            scheduleId = scheduleId,
            head = this.head,
            priority = this.priority,
            isCompleted = this.isCompleted,
            isRepeated = this.isRepeated,
            repeatPattern = this.repeatPattern,
            reminderType = this.reminderType,
            reminderOffset = this.reminderOffset
        )
    }

}