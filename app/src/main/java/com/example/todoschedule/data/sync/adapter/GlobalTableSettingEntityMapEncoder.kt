package com.example.todoschedule.data.sync.adapter

import com.example.todoschedule.data.database.entity.GlobalTableSettingEntity
import com.tap.synk.encode.MapEncoder

class GlobalTableSettingEntityMapEncoder: MapEncoder<GlobalTableSettingEntity> {
    override fun decode(map: Map<String, String>): GlobalTableSettingEntity =
        GlobalTableSettingEntity(
            id = map["id"]!!.toIntOrNull() ?: 0,
            userId = map["userId"]!!.toIntOrNull() ?: 0,
            defaultTableIds = map["defaultTableIds"] ?: "",
            showWeekend = map["showWeekend"]?.toBoolean() ?: true,
            courseNotificationStyle = map["courseNotificationStyle"]?.toIntOrNull() ?: 0,
            notifyBeforeMinutes = map["notifyBeforeMinutes"]?.toIntOrNull() ?: 15,
            autoSwitchWeek = map["autoSwitchWeek"]?.toBoolean() ?: true,
            showCourseTime = map["showCourseTime"]?.toBoolean() ?: true
        )

    override fun encode(crdt: GlobalTableSettingEntity): Map<String, String> = buildMap {
        put("id", crdt.id.toString())
        put("userId", crdt.userId.toString())
        put("defaultTableIds", crdt.defaultTableIds)
        put("showWeekend", crdt.showWeekend.toString())
        put("courseNotificationStyle", crdt.courseNotificationStyle.toString())
        put("notifyBeforeMinutes", crdt.notifyBeforeMinutes.toString())
        put("autoSwitchWeek", crdt.autoSwitchWeek.toString())
        put("showCourseTime", crdt.showCourseTime.toString())
    }


}