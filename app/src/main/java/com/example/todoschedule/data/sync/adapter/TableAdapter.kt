package com.example.todoschedule.data.sync.adapter

import com.example.todoschedule.data.database.entity.TableEntity
import com.tap.synk.resolver.IDResolver
import javax.inject.Inject

/**
 * 课表实体的SynkAdapter实现
 */
class TableEntityIDResolver @Inject constructor() : IDResolver<TableEntity> {
    /* Synk-Adapter 接口映射 */
    override fun resolveId(crdt: TableEntity): String = crdt.crdtKey
} 
