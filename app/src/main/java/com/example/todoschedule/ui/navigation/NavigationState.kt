package com.example.todoschedule.ui.navigation

import androidx.navigation.NavController

/**
 * 应用导航状态
 */
class NavigationState(
    val navController: NavController
) {
    /**
     * 获取当前路由
     */
    val currentRoute: String?
        get() = navController.currentDestination?.route

    /**
     * 导航到首页
     */
    fun navigateToHome() {
        navController.navigate(AppRoutes.Home.route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    /**
     * 导航到课表页面
     */
    fun navigateToSchedule() {
        navController.navigate(AppRoutes.Schedule.route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    /**
     * 导航到待办页面
     */
    fun navigateToTodo() {
        navController.navigate(AppRoutes.Todo.route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    /**
     * 导航到设置页面
     */
    fun navigateToSettings() {
        navController.navigate(AppRoutes.Settings.route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    /**
     * 导航到添加课程页面
     */
    fun navigateToAddCourse() {
        navController.navigate(AppRoutes.AddCourse.createRoute(1)) // 默认使用第一个课表ID
    }

    /**
     * 导航到课程详情页面
     */
    fun navigateToCourseDetail(courseId: Int) {
        navController.navigate(AppRoutes.CourseDetail.createRoute(courseId))
    }

    /**
     * 导航到编辑课程页面
     */
    fun navigateToEditCourse(courseId: Int) {
        navController.navigate(AppRoutes.EditCourse.createRoute(courseId))
    }

    /**
     * 返回上一页
     */
    fun navigateBack() {
        navController.popBackStack()
    }
} 