package com.example.todoschedule.data.sync.adapter

import com.example.todoschedule.data.database.entity.CourseNodeEntity
import com.tap.synk.resolver.IDResolver
import javax.inject.Inject

class CourseNodeEntityIDResolver @Inject constructor() : IDResolver<CourseNodeEntity> {
    override fun resolveId(crdt: CourseNodeEntity): String = crdt.id.toString()
}
