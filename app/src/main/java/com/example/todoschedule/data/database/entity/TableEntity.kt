package com.example.todoschedule.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 课表实体类
 */
@Entity(tableName = "table")
data class TableEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int = 1, // 默认用户ID为1
    val tableName: String, // 课表名称
    val background: String = "", // 背景颜色
    val listPosition: Int = 0, // 列表中的位置
    val terms: String = "", // 学期信息
    val startDate: String, // 学期开始日期
    val totalWeeks: Int = 20 // 总周数
) 