package com.example.todoschedule.core.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 权限管理工具类
 * 负责处理Android运行时权限检查
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * 检查日历权限
     * @return 是否已经授予日历读写权限
     */
    fun hasCalendarPermissions(): Boolean {
        return hasPermissions(CALENDAR_PERMISSIONS)
    }

    /**
     * 检查是否授予了指定权限
     * @param permissions 需要检查的权限列表
     * @return 是否所有权限都已授予
     */
    fun hasPermissions(permissions: Array<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        // 日历权限
        val CALENDAR_PERMISSIONS = arrayOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
    }
} 