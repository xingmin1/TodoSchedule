package com.example.todoschedule.di

import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.remote.api.UserApiService
import com.example.todoschedule.data.remote.api.SyncApi
import com.example.todoschedule.domain.repository.SessionRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import android.util.Log

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TAG = "NetworkModule"

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(sessionRepository: SessionRepository): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            Log.d(TAG, "处理请求: ${request.method} ${request.url}")

            var newRequest: Request = request

            // 判断是否需要特殊处理设备注册请求
            val isDeviceRegisterRequest = request.url.encodedPath.contains("/sync/device/register")

            // 获取token (同步)
            runBlocking {
                val userId = sessionRepository.currentUserIdFlow.first()
                Log.d(TAG, "当前用户ID: $userId")

                if (userId != null) {
                    // 获取认证token
                    val token = sessionRepository.getUserToken()
                    Log.d(
                        TAG,
                        "获取到Token: ${token?.take(10)}${if (token?.length ?: 0 > 10) "..." else ""}"
                    )

                    if (!token.isNullOrEmpty()) {
                        val authHeader = "${AppConstants.Api.Headers.BEARER_PREFIX}$token"

                        if (isDeviceRegisterRequest) {
                            Log.d(TAG, "设备注册请求: ${request.url}，添加认证头")
                        }

                        Log.d(
                            TAG,
                            "为请求添加认证头: ${request.url}, Authorization: ${authHeader.take(15)}..."
                        )

                        newRequest = request.newBuilder()
                            .addHeader(AppConstants.Api.Headers.AUTHORIZATION, authHeader)
                            .build()

                        // 打印所有请求头
                        Log.d(TAG, "请求头: ${newRequest.headers}")
                    } else {
                        if (isDeviceRegisterRequest) {
                            Log.e(TAG, "警告: 正在尝试设备注册但没有有效token！")
                        }
                        Log.w(TAG, "用户已登录但没有token，无法添加认证头: ${request.url}")
                    }
                } else {
                    if (isDeviceRegisterRequest) {
                        Log.e(TAG, "错误: 尝试进行设备注册但用户未登录！")
                    }
                    Log.w(TAG, "用户未登录，不添加认证头: ${request.url}")
                }
            }

            val response = chain.proceed(newRequest)

            // 特殊处理设备注册响应
            if (isDeviceRegisterRequest) {
                Log.d(
                    TAG,
                    "设备注册响应状态码: ${response.code}, 错误信息: ${if (!response.isSuccessful) response.message else "无"}"
                )
            } else {
                Log.d(TAG, "请求响应状态码: ${response.code} (${request.url})")
            }

            response
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(AppConstants.Api.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(AppConstants.Api.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(AppConstants.Api.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AppConstants.Api.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideUserApiService(retrofit: Retrofit): UserApiService {
        return retrofit.create(UserApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSyncApiService(retrofit: Retrofit): SyncApi {
        return retrofit.create(SyncApi::class.java)
    }
} 