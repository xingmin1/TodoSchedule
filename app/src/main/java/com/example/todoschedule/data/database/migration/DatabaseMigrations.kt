package com.example.todoschedule.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移策略
 */
object DatabaseMigrations {

    /**
     * 从版本5迁移到版本6
     * 主要变更：SyncMessageEntity使用自增主键取代UUID主键
     * 直接删除旧表并创建新表，不保留旧数据
     * 完全移除message_id字段，异步消息不需要全局唯一标识符
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 删除旧表
            database.execSQL("DROP TABLE IF EXISTS sync_message")

            // 创建新表 - 完全移除message_id字段
            database.execSQL(
                """
                CREATE TABLE sync_message (
                    sync_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    crdt_key TEXT NOT NULL,
                    entity_type TEXT NOT NULL,
                    operation_type TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    device_id TEXT NOT NULL,
                    timestamp_wall_clock INTEGER NOT NULL,
                    timestamp_logical INTEGER NOT NULL,
                    timestamp_node_id TEXT NOT NULL,
                    payload TEXT NOT NULL,
                    user_id INTEGER NOT NULL,
                    sync_status TEXT NOT NULL,
                    last_sync_attempt INTEGER,
                    sync_error TEXT
                )
                """
            )

            // 创建索引以保持查询性能 - 移除message_id索引
            database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_message_crdt_key ON sync_message(crdt_key)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_message_entity_type ON sync_message(entity_type)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_message_sync_status ON sync_message(sync_status)")
        }
    }

    /**
     * 从版本6迁移到版本7
     * 1. 为sync_message表添加索引
     * 2. 为time_slot表添加crdtKey和update_timestamp字段
     * 3. 为time_slot表的crdtKey字段添加索引
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 1. 处理sync_message表的索引
            // 先删除现有索引（如果存在）
            try {
                database.execSQL("DROP INDEX IF EXISTS index_sync_message_sync_status")
                database.execSQL("DROP INDEX IF EXISTS index_sync_message_entity_type")
                database.execSQL("DROP INDEX IF EXISTS index_sync_message_crdt_key")
            } catch (e: Exception) {
                // 如果索引不存在，忽略错误
            }

            // 创建索引，确保与实体类中的索引定义一致
            database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_message_sync_status ON sync_message(sync_status)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_message_entity_type ON sync_message(entity_type)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_message_crdt_key ON sync_message(crdt_key)")

            // 2. 为time_slot表添加crdtKey和update_timestamp字段
            try {
                // 添加crdtKey列，默认为随机UUID
                database.execSQL("ALTER TABLE time_slot ADD COLUMN crdtKey TEXT NOT NULL DEFAULT (hex(randomblob(16)))")
                // 添加update_timestamp列，可为null
                database.execSQL("ALTER TABLE time_slot ADD COLUMN update_timestamp INTEGER")
                // 为crdtKey添加索引
                database.execSQL("CREATE INDEX IF NOT EXISTS index_time_slot_crdt_key ON time_slot(crdtKey)")
            } catch (e: Exception) {
                // 如果出错，可能是列已存在，忽略错误
            }
        }
    }
}
