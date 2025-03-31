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
    object CourseDetail : AppRoutes("course_detail/{courseId}") {
        fun createRoute(courseId: Int) = "course_detail/$courseId"
    }

    /**
     * 编辑课程
     */
    object EditCourse : AppRoutes("edit_course/{courseId}") {
        fun createRoute(courseId: Int) = "edit_course/$courseId"
    }

    /**
     * 学校教务系统选择页面
     */
    object SchoolSelector : AppRoutes("schoolSelector")

    /**
     * WebView页面
     */
    object SchoolWebView : AppRoutes("webView/{encodedUrl}") {
        fun gotoWebView(encodedUrl: String) = "webView/$encodedUrl"
    }
} 