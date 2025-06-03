package com.example.todoschedule.data.sync.adapter

import com.example.todoschedule.data.database.entity.GlobalTableSettingEntity
import com.tap.synk.resolver.IDResolver

class GlobalTableSettingEntityIdResolver: IDResolver<GlobalTableSettingEntity> {
    override fun resolveId(crdt: GlobalTableSettingEntity): String = crdt.id.toString()
}