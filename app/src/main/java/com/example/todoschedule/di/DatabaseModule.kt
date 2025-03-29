package com.example.todoschedule.di

import android.content.Context
import androidx.room.Room
import com.example.todoschedule.data.database.AppDatabase
import com.example.todoschedule.data.database.dao.CourseDao
import com.example.todoschedule.data.database.dao.TableDao
import com.example.todoschedule.data.database.dao.TimeConfigDao
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
            "todo_schedule.db"
        ).build()
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
    fun provideTimeConfigDao(database: AppDatabase): TimeConfigDao {
        return database.timeConfigDao()
    }
}