package com.example.todoschedule.data.sync.adapter

import com.example.todoschedule.data.database.entity.TimeSlotEntity
import com.tap.synk.annotation.SynkAdapter
import com.tap.synk.resolver.IDResolver
import javax.inject.Inject

@SynkAdapter
class TimeSlotEntityIDResolver @Inject constructor() : IDResolver<TimeSlotEntity> {
    override fun resolveId(crdt: TimeSlotEntity): String = crdt.crdtKey
}
