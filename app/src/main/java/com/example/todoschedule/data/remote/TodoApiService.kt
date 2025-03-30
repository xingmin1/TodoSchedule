package com.example.todoschedule.data.remote

import com.example.todoschedule.data.remote.model.ApiResponse
import com.example.todoschedule.data.remote.model.CourseDto
import com.example.todoschedule.data.remote.model.TableDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 待办课表 API 服务接口
 *
 * 注意：目前这只是一个样板接口，实际的API请根据后端进行实现
 */
interface TodoApiService {

    /**
     * 获取所有课表
     */
    @GET("tables")
    suspend fun getAllTables(): ApiResponse<List<TableDto>>

    /**
     * 获取课表详情
     */
    @GET("tables/{tableId}")
    suspend fun getTableById(@Path("tableId") tableId: Int): ApiResponse<TableDto>

    /**
     * 创建课表
     */
    @POST("tables")
    suspend fun createTable(@Body table: TableDto): ApiResponse<TableDto>

    /**
     * 更新课表
     */
    @PUT("tables/{tableId}")
    suspend fun updateTable(
        @Path("tableId") tableId: Int,
        @Body table: TableDto
    ): ApiResponse<TableDto>

    /**
     * 删除课表
     */
    @DELETE("tables/{tableId}")
    suspend fun deleteTable(@Path("tableId") tableId: Int): ApiResponse<Unit>

    /**
     * 获取课表的所有课程
     */
    @GET("tables/{tableId}/courses")
    suspend fun getCoursesByTableId(@Path("tableId") tableId: Int): ApiResponse<List<CourseDto>>

    /**
     * 获取特定周次的课程
     */
    @GET("tables/{tableId}/courses")
    suspend fun getCoursesByWeek(
        @Path("tableId") tableId: Int,
        @Query("week") week: Int
    ): ApiResponse<List<CourseDto>>

    /**
     * 获取课程详情
     */
    @GET("courses/{courseId}")
    suspend fun getCourseById(@Path("courseId") courseId: Int): ApiResponse<CourseDto>

    /**
     * 创建课程
     */
    @POST("courses")
    suspend fun createCourse(@Body course: CourseDto): ApiResponse<CourseDto>

    /**
     * 更新课程
     */
    @PUT("courses/{courseId}")
    suspend fun updateCourse(
        @Path("courseId") courseId: Int,
        @Body course: CourseDto
    ): ApiResponse<CourseDto>

    /**
     * 删除课程
     */
    @DELETE("courses/{courseId}")
    suspend fun deleteCourse(@Path("courseId") courseId: Int): ApiResponse<Unit>
} 