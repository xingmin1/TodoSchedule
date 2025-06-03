package com.example.todoschedule.data.sync.adapter

import com.example.todoschedule.data.database.entity.GlobalTableSettingEntity
import com.tap.synk.adapter.SynkAdapter
import com.tap.synk.encode.MapEncoder
import com.tap.synk.resolver.IDResolver

class GlobalTableSettingEntitySynkAdapter(
    private val idResolver: GlobalTableSettingEntityIdResolver,
    private val mapEncoder: GlobalTableSettingEntityMapEncoder,
): SynkAdapter<GlobalTableSettingEntity>,
    IDResolver<GlobalTableSettingEntity> by idResolver,
    MapEncoder<GlobalTableSettingEntity> by mapEncoder