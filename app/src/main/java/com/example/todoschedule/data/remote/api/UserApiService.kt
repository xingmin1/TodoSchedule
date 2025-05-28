package com.example.todoschedule.data.remote.api

import com.example.todoschedule.core.constants.AppConstants
import com.example.todoschedule.data.remote.dto.ApiResponse
import com.example.todoschedule.data.remote.dto.UserDto
import com.example.todoschedule.data.remote.dto.UserLoginRequest
import com.example.todoschedule.data.remote.dto.UserRegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 用户API服务接口
 */
interface UserApiService {

    /**
     * 用户登录
     */
    @POST(AppConstants.Api.Endpoints.LOGIN)
    suspend fun login(@Body request: UserLoginRequest): ApiResponse<UserDto>

    /**
     * 用户注册
     */
    @POST(AppConstants.Api.Endpoints.REGISTER)
    suspend fun register(@Body request: UserRegisterRequest): ApiResponse<UserDto>
} 