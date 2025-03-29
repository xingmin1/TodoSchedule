package com.example.todoschedule.di

import com.example.todoschedule.data.repository.CourseRepositoryImpl
import com.example.todoschedule.data.repository.TableRepositoryImpl
import com.example.todoschedule.data.repository.TimeConfigRepositoryImpl
import com.example.todoschedule.domain.repository.CourseRepository
import com.example.todoschedule.domain.repository.TableRepository
import com.example.todoschedule.domain.repository.TimeConfigRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 仓库依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * 绑定课程仓库实现
     */
    @Binds
    @Singleton
    abstract fun bindCourseRepository(
        courseRepositoryImpl: CourseRepositoryImpl
    ): CourseRepository

    /**
     * 绑定课表仓库实现
     */
    @Binds
    @Singleton
    abstract fun bindTableRepository(
        tableRepositoryImpl: TableRepositoryImpl
    ): TableRepository

    /**
     * 绑定时间配置仓库实现
     */
    @Binds
    @Singleton
    abstract fun bindTimeConfigRepository(
        timeConfigRepositoryImpl: TimeConfigRepositoryImpl
    ): TimeConfigRepository
} 