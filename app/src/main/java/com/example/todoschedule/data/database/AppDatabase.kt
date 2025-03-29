package com.example.todoschedule.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.todoschedule.data.database.dao.CourseDao
import com.example.todoschedule.data.database.dao.TableDao
import com.example.todoschedule.data.database.dao.TimeConfigDao
import com.example.todoschedule.data.database.entity.CourseEntity
import com.example.todoschedule.data.database.entity.CourseNodeEntity
import com.example.todoschedule.data.database.entity.TableEntity
import com.example.todoschedule.data.database.entity.TimeDetailEntity
import com.example.todoschedule.data.database.entity.TimeTableEntity

/**
 * 应用数据库
 */
@Database(
    entities = [
        TableEntity::class,
        CourseEntity::class,
        CourseNodeEntity::class,
        TimeTableEntity::class,
        TimeDetailEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    /**
     * 获取课表DAO
     */
    abstract fun tableDao(): TableDao

    /**
     * 获取课程DAO
     */
    abstract fun courseDao(): CourseDao

    /**
     * 获取时间配置DAO
     */
    abstract fun timeConfigDao(): TimeConfigDao
} 