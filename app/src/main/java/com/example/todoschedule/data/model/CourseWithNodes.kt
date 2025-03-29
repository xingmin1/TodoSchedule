package com.example.todoschedule.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.todoschedule.data.database.entity.CourseEntity
import com.example.todoschedule.data.database.entity.CourseNodeEntity

/**
 * 课程与节点关联模型
 */
data class CourseWithNodes(
    @Embedded val course: CourseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "courseId"
    )
    val nodes: List<CourseNodeEntity> = emptyList()
) 