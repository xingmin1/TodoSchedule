package com.example.todoschedule.di

import com.example.todoschedule.data.remote.TodoApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * 应用依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {


    /**
     * 提供API服务
     */
    @Provides
    @Singleton
    fun provideApiService(): TodoApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.example.com/") // 实际使用时请替换为真实的API地址
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TodoApiService::class.java)
    }
}