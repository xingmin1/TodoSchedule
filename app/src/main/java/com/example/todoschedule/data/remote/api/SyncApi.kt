package com.example.todoschedule.data.remote.api

import com.example.todoschedule.data.sync.dto.ApiSyncMessageDto
import com.example.todoschedule.data.sync.dto.DeviceRegistrationDto
import com.example.todoschedule.data.sync.dto.DeviceRegistrationResponseDto
import com.example.todoschedule.data.sync.dto.SyncMessagesDto
import com.example.todoschedule.data.sync.dto.SyncMessageDto
import com.example.todoschedule.data.sync.dto.UploadSyncResponseDto
import com.example.todoschedule.data.sync.dto.ApiResponse
import com.example.todoschedule.data.sync.dto.UploadSyncResult
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 数据同步API接口
 */
interface SyncApi {
    /**
     * 注册设备
     * @param deviceId 设备ID
     * @param deviceRegistration 设备注册信息
     * @return 注册结果
     */
    @POST("/sync/device/register")
    suspend fun registerDevice(
        @Header("X-Device-ID") deviceId: String,
        @Body deviceRegistration: DeviceRegistrationDto
    ): Response<DeviceRegistrationResponseDto>

    /**
     * 上传同步消息
     * @param deviceId 设备ID
     * @param entityType 实体类型
     * @param messages JSON字符串消息列表
     * @return 上传结果
     */
    @POST("/sync/messages/{entityType}")
    suspend fun uploadMessages(
        @Header("X-Device-ID") deviceId: String,
        @Path("entityType") entityType: String,
        @Body messages: List<String>
    ): Response<ApiResponse<UploadSyncResult>>

    /**
     * 获取所有同步消息
     * @param deviceId 设备ID
     * @return 所有同步消息
     */
    @GET("/sync/messages/all")
    suspend fun getAllMessages(
        @Header("X-Device-ID") deviceId: String
    ): Response<List<ApiSyncMessageDto>>

    /**
     * 获取指定类型的同步消息
     * @param deviceId 设备ID
     * @param entityType 实体类型
     * @return 指定类型的同步消息
     */
    @GET("/sync/messages/{entityType}")
    suspend fun getMessagesByEntityType(
        @Header("X-Device-ID") deviceId: String,
        @Path("entityType") entityType: String
    ): Response<List<ApiSyncMessageDto>>

    /**
     * 获取排除本设备的所有同步消息
     * @param deviceId 设备ID
     * @return 其他设备的同步消息
     */
    @GET("/sync/messages/all/exclude-origin")
    suspend fun getAllMessagesExcludeOrigin(
        @Header("X-Device-ID") deviceId: String
    ): Response<List<ApiSyncMessageDto>>

    /**
     * 获取排除本设备的指定类型同步消息
     * @param deviceId 设备ID
     * @param entityType 实体类型
     * @return 其他设备的指定类型同步消息
     */
    @GET("/sync/messages/{entityType}/exclude-origin")
    suspend fun getMessagesByEntityTypeExcludeOrigin(
        @Header("X-Device-ID") deviceId: String,
        @Path("entityType") entityType: String
    ): Response<List<ApiSyncMessageDto>>
} 