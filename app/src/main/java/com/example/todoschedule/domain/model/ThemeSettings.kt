package com.example.todoschedule.domain.model

/**
 * UI 主题设置数据类
 */
data class ThemeSettings(
    val isDarkTheme: Boolean = false, // 默认非深色模式
    val useMaterialYou: Boolean = true // 默认启用 Material You
) 