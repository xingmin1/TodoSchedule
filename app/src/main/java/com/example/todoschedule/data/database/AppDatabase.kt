package com.example.todoschedule.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.converter.Converters
import com.example.todoschedule.data.database.dao.CourseDao
import com.example.todoschedule.data.database.dao.GlobalSettingDao
import com.example.todoschedule.data.database.dao.OrdinaryScheduleDao
import com.example.todoschedule.data.database.dao.TableDao
import com.example.todoschedule.data.database.dao.TableTimeConfigDao
import com.example.todoschedule.data.database.dao.TimeSlotDao
import com.example.todoschedule.data.database.dao.UserDao
import com.example.todoschedule.data.database.entity.CourseEntity
import com.example.todoschedule.data.database.entity.CourseNodeEntity
import com.example.todoschedule.data.database.entity.GlobalTableSettingEntity
import com.example.todoschedule.data.database.entity.OrdinaryScheduleEntity
import com.example.todoschedule.data.database.entity.TableEntity
import com.example.todoschedule.data.database.entity.TableTimeConfigEntity
import com.example.todoschedule.data.database.entity.TableTimeConfigNodeDetaileEntity
import com.example.todoschedule.data.database.entity.TimeSlotEntity
import com.example.todoschedule.data.database.entity.UserEntity

/** 应用数据库 */
@Database(
    entities = [
        UserEntity::class,
        GlobalTableSettingEntity::class,
        TableEntity::class,
        CourseEntity::class,
        CourseNodeEntity::class,
        TableTimeConfigEntity::class,
        TableTimeConfigNodeDetaileEntity::class,
        OrdinaryScheduleEntity::class,
        TimeSlotEntity::class
    ],
    version = AppConstants.Database.DB_VERSION + 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    /** 获取用户DAO */
    abstract fun userDao(): UserDao

    /** 获取全局设置DAO */
    abstract fun globalSettingDao(): GlobalSettingDao

    /** 获取课表DAO */
    abstract fun tableDao(): TableDao

    /** 获取课程DAO */
    abstract fun courseDao(): CourseDao

    /** 获取时间配置DAO (新) */
    abstract fun tableTimeConfigDao(): TableTimeConfigDao

    /** 获取普通日程DAO */
    abstract fun ordinaryScheduleDao(): OrdinaryScheduleDao

    /** 获取时间槽DAO */
    abstract fun timeSlotDao(): TimeSlotDao
}
