package com.example.todoschedule.data.mapper

import com.example.todoschedule.core.extensions.toUuid
import com.example.todoschedule.data.database.entity.GlobalTableSettingEntity
import com.example.todoschedule.domain.model.GlobalTableSetting

/** 将全局设置实体映射为全局设置领域模型 */
fun GlobalTableSettingEntity.toGlobalTableSetting(): GlobalTableSetting {
    return GlobalTableSetting(
        id = id,
        userId = userId,
        // 这里假设有一个TypeConverter已经处理了字符串转列表的过程
        // 在实际使用时，需要手动转换
        defaultTableIds =
            defaultTableIds.split(",").filter { it.isNotEmpty() }.map { it.toUuid() },
        showWeekend = showWeekend,
        courseNotificationStyle = courseNotificationStyle,
        notifyBeforeMinutes = notifyBeforeMinutes,
        autoSwitchWeek = autoSwitchWeek,
        showCourseTime = showCourseTime
    )
}

/** 将全局设置领域模型映射为全局设置实体 */
fun GlobalTableSetting.toGlobalTableSettingEntity(): GlobalTableSettingEntity {
    return GlobalTableSettingEntity(
        id = id,
        userId = userId,
        // 这里手动将列表转换为逗号分隔的字符串
        defaultTableIds = defaultTableIds.joinToString(","),
        showWeekend = showWeekend,
        courseNotificationStyle = courseNotificationStyle,
        notifyBeforeMinutes = notifyBeforeMinutes,
        autoSwitchWeek = autoSwitchWeek,
        showCourseTime = showCourseTime
    )
}
