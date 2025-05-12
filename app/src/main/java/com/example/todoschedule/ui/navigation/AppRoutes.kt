package com.example.todoschedule.ui.navigation


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
        fun createRoute(tableId: Int) = "add_course/$tableId"
    }

    /**
     * 课程详情
     */
    object CourseDetail : AppRoutes("course_detail/{tableId}/{courseId}") {
        fun createRoute(tableId: Int, courseId: Int) = "course_detail/$tableId/$courseId"
    }

    /**
     * 编辑课程
     */
    object EditCourse : AppRoutes("edit_course/{tableId}/{courseId}") {
        fun createRoute(tableId: Int, courseId: Int) = "edit_course/$tableId/$courseId"
    }

    /**
     * 学校教务系统选择页面
     */
    object SchoolSelector : AppRoutes("schoolSelector/{tableId}") {
        fun createRoute(tableId: Int) = "schoolSelector/$tableId"
    }

    /**
     * WebView页面
     */
    object SchoolWebView : AppRoutes("webView/{encodedUrl}/{tableId}") {
        fun createRoute(encodedUrl: String, tableId: Int) = "webView/$encodedUrl/$tableId"
    }

    /**
     * 添加/编辑普通日程页面
     */
    object AddEditOrdinarySchedule :
        AppRoutes("add_edit_ordinary_schedule?scheduleId={scheduleId}") {
        fun createRoute(scheduleId: Int? = null): String {
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
        fun createRoute(scheduleId: Int) = "ordinary_schedule_detail/$scheduleId"
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
        fun createRoute(tableId: Int? = null): String {
            return if (tableId != null) {
                "create_edit_table?tableId=$tableId"
            } else {
                "create_edit_table"
            }
        }

        const val ARG_TABLE_ID = "tableId" // Argument name
    }

    // Settings Sub-Screens
    object TableManagement : AppRoutes("table_management")
    object DefaultDisplaySettings : AppRoutes("default_display_settings")
    object SingleTableSettings : AppRoutes("single_table_settings/{tableId}") {
        fun createRoute(tableId: Int) = "single_table_settings/$tableId"
        const val ARG_TABLE_ID = "tableId"
    }
    object TimeNodesSettings : AppRoutes("time_nodes_settings/{tableId}/{configId}") {
        fun createRoute(tableId: Int, configId: Int) = "time_nodes_settings/$tableId/$configId"
        const val ARG_TABLE_ID = "tableId"
        const val ARG_CONFIG_ID = "configId"
    }
    // Add other settings sub-screens here if they become dedicated screens
    // object AppearanceSettings : AppRoutes("appearance_settings")
    // object GeneralSettings : AppRoutes("general_settings")
}
