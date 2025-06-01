package com.example.todoschedule.di

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.todoschedule.data.sync.adapter.CourseAdapter
import com.example.todoschedule.data.sync.adapter.CourseNodeAdapter
import com.example.todoschedule.data.sync.adapter.OrdinaryScheduleAdapter
import com.example.todoschedule.data.sync.adapter.TableAdapter
import com.example.todoschedule.data.sync.adapter.TimeSlotAdapter
import com.tap.delight.metastore.DelightfulDatabase
import com.tap.delight.metastore.DelightfulMetastoreFactory
import com.tap.delight.metastore.config.MetastoreConfig
import com.tap.synk.Synk
import com.tap.synk.config.ClockStorageConfiguration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SynkModule {

    @Provides
    @Singleton
    fun provideSynk(
        context: Context,
        courseAdapter: CourseAdapter,
        tableAdapter: TableAdapter,
        courseNodeAdapter: CourseNodeAdapter,
        ordinaryScheduleAdapter: OrdinaryScheduleAdapter,
        timeSlotAdapter: TimeSlotAdapter,
    ): Synk {

        // ① 时钟文件配置
        val clockConfig = ClockStorageConfiguration.Presets.Android(context)

        // ② Delightful-Metastore
        val driver = AndroidSqliteDriver(
            DelightfulDatabase.Schema,
            context,
            "synk_meta.db"
        )
        val metaFactory = DelightfulMetastoreFactory(
            driver,
            MetastoreConfig(cacheSize = 500, warmCaches = true)
        )

        // ③ 构建 Synk
        return Synk.Builder(clockConfig)
            .registerSynkAdapter(courseAdapter)
            .registerSynkAdapter(tableAdapter)
            .registerSynkAdapter(courseNodeAdapter)
            .registerSynkAdapter(ordinaryScheduleAdapter)
            .registerSynkAdapter(timeSlotAdapter)
            .metaStoreFactory(metaFactory)
            .build()
    }
}
