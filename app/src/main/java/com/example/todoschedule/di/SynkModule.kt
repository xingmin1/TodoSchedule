package com.example.todoschedule.di

import android.content.Context
import com.example.todoschedule.data.sync.adapter.CourseEntitySynkAdapter
import com.example.todoschedule.data.sync.adapter.TableEntitySynkAdapter
import com.example.todoschedule.data.sync.adapter.CourseNodeEntitySynkAdapter
import com.example.todoschedule.data.sync.adapter.OrdinaryScheduleEntitySynkAdapter
import com.example.todoschedule.data.sync.adapter.TimeSlotEntitySynkAdapter
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.tap.delight.metastore.DelightfulDatabase
import com.tap.delight.metastore.DelightfulMetastoreFactory
import com.tap.delight.metastore.config.MetastoreConfig
import com.tap.synk.Synk
import com.tap.synk.config.Android
import com.tap.synk.config.ClockStorageConfiguration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SynkModule {

    @Provides
    @Singleton
    fun provideSynk(
        @ApplicationContext context: Context,
    ): Synk {

        val clockConfig = ClockStorageConfiguration.Presets.Android(context)

        val driver = AndroidSqliteDriver(
            DelightfulDatabase.Schema,
            context,
            "synk_meta.db",
        )
        val metaFactory = DelightfulMetastoreFactory(
            driver,
            MetastoreConfig(cacheSize = 500, warmCaches = true),
        )

        // 直接使用 KSP 生成的适配器
        val courseAdapter = CourseEntitySynkAdapter()
        val tableAdapter = TableEntitySynkAdapter()
        val courseNodeAdapter = CourseNodeEntitySynkAdapter()
        val ordinaryScheduleAdapter = OrdinaryScheduleEntitySynkAdapter()
        val timeSlotAdapter = TimeSlotEntitySynkAdapter()

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
