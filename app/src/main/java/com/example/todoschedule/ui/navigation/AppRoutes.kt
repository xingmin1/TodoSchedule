package com.example.todoschedule.ui.navigation

import java.util.UUID


/**
 * 应用导航路由
 */
sealed class AppRoutes(val route: String) {
    /**
     * 首页
     */
    object Home : AppRoutes("home")

    /**
     * 课表页面
     */
    object Schedule : AppRoutes("schedule")

    /**
     * 待办页面
     */
    object Todo : AppRoutes("todo")

    /**
     * 任务页面
     */
    object Task : AppRoutes("task")

    /**
     * 学习页面
     */
    object Study : AppRoutes("study")

    /**
     * 我的页面
     */
    object Profile : AppRoutes("profile")

    /**
     * 设置页面
     */
    object Settings : AppRoutes("settings")

    /**
     * 添加课程
     */
    object AddCourse : AppRoutes("add_course/{tableId}") {
        fun createRoute(tableId: UUID) = "add_course/$tableId"
    }

    /**
     * 课程详情
     */
    object CourseDetail : AppRoutes("course_detail/{tableId}/{courseId}") {
        fun createRoute(tableId: UUID, courseId: UUID) = "course_detail/$tableId/$courseId"
    }

    /**
     * 编辑课程
     */
    object EditCourse : AppRoutes("edit_course/{tableId}/{courseId}") {
        fun createRoute(tableId: UUID, courseId: UUID) = "edit_course/$tableId/$courseId"
    }

    /**
     * 学校教务系统选择页面
     */
    object SchoolSelector : AppRoutes("schoolSelector/{tableId}") {
        fun createRoute(tableId: UUID) = "schoolSelector/$tableId"
    }

    /**
     * WebView页面
     */
    object SchoolWebView : AppRoutes("webView/{encodedUrl}/{tableId}") {
        fun createRoute(encodedUrl: String, tableId: UUID) = "webView/$encodedUrl/$tableId"
    }

    /**
     * 添加/编辑普通日程页面
     */
    object AddEditOrdinarySchedule :
        AppRoutes("add_edit_ordinary_schedule?scheduleId={scheduleId}") {
        fun createRoute(scheduleId: UUID? = null): String {
            return if (scheduleId != null) {
                "add_edit_ordinary_schedule?scheduleId=$scheduleId"
            } else {
                "add_edit_ordinary_schedule"
            }
        }

        const val ARG_SCHEDULE_ID = "scheduleId" // Argument name constant
    }

    /**
     * 普通日程详情页面
     */
    object OrdinaryScheduleDetail : AppRoutes("ordinary_schedule_detail/{scheduleId}") {
        fun createRoute(scheduleId: UUID) = "ordinary_schedule_detail/$scheduleId"
        const val ARG_SCHEDULE_ID = "scheduleId" // Argument name constant
    }

    /**
     * 登录页面
     */
    object Login : AppRoutes("login")

    /**
     * 注册页面
     */
    object Register : AppRoutes("register")

    /**
     * 创建/编辑课表页面
     * 可选参数 tableId 用于编辑模式
     */
    object CreateEditTable : AppRoutes("create_edit_table?tableId={tableId}") {
        fun createRoute(tableId: UUID? = null): String {
            return if (tableId != null) {
                "create_edit_table?tableId=$tableId"
            } else {
                "create_edit_table"
            }
        }

        const val ARG_TABLE_ID = "tableId" // Argument name
    }

    /**
     * 任务提醒选择页面
     */
    object TaskReminder : AppRoutes("task_reminder") {
        fun createRoute() = "task_reminder"
    }

    /**
     * 任务日历同步页面
     */
    object TaskCalendarSync : AppRoutes("task_calendar_sync/{filter}") {
        const val ARG_FILTER = "filter"

        fun createRoute(filter: String): String {
            // 确保filter值被正确URL编码
            return "task_calendar_sync/$filter"
        }
    }

    /**
     * 同步设置路由
     */
    object SyncSettings : AppRoutes("syncSettings") {
        fun createRoute() = "syncSettings"
    }

    // Settings Sub-Screens
    data object TableManagement : AppRoutes("table_management")
    data object SingleTableSettings : AppRoutes("single_table_settings/{tableId}") {
        const val ARG_TABLE_ID = "tableId" // Argument name constant
        fun createRoute(tableId: UUID) = "single_table_settings/$tableId"
    }

    data object TimeNodesSettings : AppRoutes("time_nodes_settings/{configId}") {
        fun createRoute(configId: UUID) = "time_nodes_settings/$configId"
    }

    data object DefaultDisplaySettings : AppRoutes("default_display_settings")
}
