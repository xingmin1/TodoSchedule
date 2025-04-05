package com.example.todoschedule.di

import android.content.Context
import androidx.room.Room
import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.database.AppDatabase
import com.example.todoschedule.data.database.dao.CourseDao
import com.example.todoschedule.data.database.dao.GlobalSettingDao
import com.example.todoschedule.data.database.dao.OrdinaryScheduleDao
import com.example.todoschedule.data.database.dao.TableDao
import com.example.todoschedule.data.database.dao.TableTimeConfigDao
import com.example.todoschedule.data.database.dao.TimeSlotDao
import com.example.todoschedule.data.database.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * 提供数据库实例
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppConstants.Database.DB_NAME,
        )
            .fallbackToDestructiveMigration() // 使用破坏性迁移，数据库版本变更时会删除旧数据
            .build()
    }

    /**
     * 提供用户DAO
     */
    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    /**
     * 提供全局设置DAO
     */
    @Provides
    @Singleton
    fun provideGlobalSettingDao(database: AppDatabase): GlobalSettingDao {
        return database.globalSettingDao()
    }

    /**
     * 提供课表DAO
     */
    @Provides
    @Singleton
    fun provideTableDao(database: AppDatabase): TableDao {
        return database.tableDao()
    }

    /**
     * 提供课程DAO
     */
    @Provides
    @Singleton
    fun provideCourseDao(database: AppDatabase): CourseDao {
        return database.courseDao()
    }

    /**
     * 提供时间详情DAO
     */
    @Provides
    @Singleton
    fun provideTimeConfigDao(database: AppDatabase): TableTimeConfigDao {
        return database.tableTimeConfigDao()
    }

    /**
     * 提供普通日程DAO
     */
    @Provides
    @Singleton
    fun provideOrdinaryScheduleDao(database: AppDatabase): OrdinaryScheduleDao {
        return database.ordinaryScheduleDao()
    }

    /**
     * 提供时间槽DAO
     */
    @Provides
    @Singleton
    fun provideTimeSlotDao(database: AppDatabase): TimeSlotDao {
        return database.timeSlotDao()
    }
}