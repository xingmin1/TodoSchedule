package com.example.todoschedule.data.sync.adapter

import com.example.todoschedule.data.database.entity.CourseEntity
import com.tap.synk.resolver.IDResolver
import javax.inject.Inject

class CourseEntityIDResolver @Inject constructor() : IDResolver<CourseEntity> {
    override fun resolveId(crdt: CourseEntity): String = crdt.crdtKey
}
