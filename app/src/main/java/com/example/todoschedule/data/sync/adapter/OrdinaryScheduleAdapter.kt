package com.example.todoschedule.data.sync.adapter

import com.example.todoschedule.data.database.entity.OrdinaryScheduleEntity
import com.tap.synk.annotation.SynkAdapter
import com.tap.synk.resolver.IDResolver
import javax.inject.Inject

@SynkAdapter
class OrdinaryScheduleEntityIDResolver @Inject constructor() : IDResolver<OrdinaryScheduleEntity> {
    override fun resolveId(crdt: OrdinaryScheduleEntity): String = crdt.crdtKey
}
