package com.example.todoschedule.domain.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import com.example.todoschedule.core.extensions.toJavaCalendar
import com.example.todoschedule.ui.task.TaskItemUiModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 管理任务与设备日历的同步功能
 * 注意: 这个管理器仅处理本地设备日历同步，不包含服务器数据同步功能
 */
@Singleton
class CalendarSyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver
    private val TAG = "CalendarSyncManager"

    /**
     * 将任务同步到设备日历
     * @param task 要同步的任务
     * @param calendarId 目标日历ID
     * @return 同步是否成功
     */
    fun syncTaskToCalendar(task: TaskItemUiModel, calendarId: Long): Boolean {
        try {
            val startMillis = task.startTime.toJavaCalendar().timeInMillis
            val endMillis = task.endTime.toJavaCalendar().timeInMillis

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, task.title)
                put(CalendarContract.Events.DESCRIPTION, "来自TodoSchedule应用的任务")
                put(CalendarContract.Events.EVENT_LOCATION, getTaskLocation(task))
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)

                // 设置时区
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)

                // 添加提醒 (使用 Int 类型)
                put(CalendarContract.Events.HAS_ALARM, 1)
            }

            // 插入事件
            val eventUri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            if (eventUri != null) {
                // 设置提醒 (可选，在事件创建后)
                val eventId = eventUri.lastPathSegment?.toLong()
                if (eventId != null) {
                    addReminder(eventId)
                }
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "同步任务到日历失败: ${e.message}")
        }
        return false
    }

    /**
     * 批量同步任务到设备日历
     * @param tasks 要同步的任务列表
     * @param calendarId 目标日历ID
     * @return 成功同步的任务数量
     */
    fun batchSyncTasksToCalendar(tasks: List<TaskItemUiModel>, calendarId: Long): Int {
        var successCount = 0
        for (task in tasks) {
            try {
                val success = syncTaskToCalendar(task, calendarId)
                if (success) {
                    successCount++
                }
            } catch (e: Exception) {
                Log.e(TAG, "同步任务 ${task.title} 到日历失败: ${e.message}")
                // 继续处理下一个任务，即使当前任务失败
            }
        }
        return successCount
    }

    /**
     * 获取任务的位置信息
     */
    private fun getTaskLocation(task: TaskItemUiModel): String {
        return when (task) {
            is TaskItemUiModel.OrdinaryTask -> task.location ?: ""
            is TaskItemUiModel.CourseTask -> {
                val nodeInfo = task.id.substringAfter("course_").split("_")
                if (nodeInfo.size >= 3) {
                    try {
                        val courseId = nodeInfo[0].toInt()
                        val day = nodeInfo[1].toInt()
                        val startNode = nodeInfo[2].toInt()

                        // 这里可以根据课程ID、日期和节次查询具体教室
                        // 简化版本直接返回描述
                        "教室信息待补充"
                    } catch (e: Exception) {
                        ""
                    }
                } else {
                    ""
                }
            }
        }
    }

    /**
     * 添加提醒
     */
    private fun addReminder(eventId: Long) {
        try {
            val values = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                put(CalendarContract.Reminders.MINUTES, 15) // 提前15分钟提醒
            }
            contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
        } catch (e: Exception) {
            Log.e(TAG, "添加提醒失败: ${e.message}")
        }
    }

    /**
     * 获取可用的日历列表
     * @return 日历ID和名称的映射
     */
    fun getAvailableCalendars(): Map<Long, String> {
        val calendars = mutableMapOf<Long, String>()
        try {
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
            )

            contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val calendarId = cursor.getLong(0)
                    val displayName = cursor.getString(3) ?: cursor.getString(1) ?: "未命名日历"

                    calendars[calendarId] = displayName
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取可用日历失败: ${e.message}")
        }
        return calendars
    }
} 