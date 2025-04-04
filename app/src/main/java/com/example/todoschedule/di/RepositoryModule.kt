package com.example.todoschedule.di

import com.example.todoschedule.data.repository.CourseRepositoryImpl
import com.example.todoschedule.data.repository.GlobalSettingRepositoryImpl
import com.example.todoschedule.data.repository.OrdinaryScheduleRepositoryImpl
import com.example.todoschedule.data.repository.TableRepositoryImpl
import com.example.todoschedule.data.repository.TimeConfigRepositoryImpl
import com.example.todoschedule.data.repository.UserRepositoryImpl
import com.example.todoschedule.domain.repository.CourseRepository
import com.example.todoschedule.domain.repository.GlobalSettingRepository
import com.example.todoschedule.domain.repository.OrdinaryScheduleRepository
import com.example.todoschedule.domain.repository.TableRepository
import com.example.todoschedule.domain.repository.TimeConfigRepository
import com.example.todoschedule.domain.repository.UserRepository
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
    /** 绑定用户仓库实现 */
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    /** 绑定全局设置仓库实现 */
    @Binds
    @Singleton
    abstract fun bindGlobalSettingRepository(
        globalSettingRepositoryImpl: GlobalSettingRepositoryImpl
    ): GlobalSettingRepository

    /** 绑定课程仓库实现 */
    @Binds
    @Singleton
    abstract fun bindCourseRepository(
        courseRepositoryImpl: CourseRepositoryImpl
    ): CourseRepository

    /** 绑定课表仓库实现 */
    @Binds
    @Singleton
    abstract fun bindTableRepository(
        tableRepositoryImpl: TableRepositoryImpl
    ): TableRepository

    /** 绑定时间配置仓库实现 */
    @Binds
    @Singleton
    abstract fun bindTimeConfigRepository(
        timeConfigRepositoryImpl: TimeConfigRepositoryImpl
    ): TimeConfigRepository

    /** 绑定普通日程仓库实现 */
    @Binds
    @Singleton
    abstract fun bindOrdinaryScheduleRepository(
        ordinaryScheduleRepositoryImpl: OrdinaryScheduleRepositoryImpl
    ): OrdinaryScheduleRepository
} 