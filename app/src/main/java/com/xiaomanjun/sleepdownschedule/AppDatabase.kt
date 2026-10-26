package com.xiaomanjun.sleepdownschedule

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

internal const val APP_DATABASE_VERSION = 48

@Database(
    entities = [
        com.xiaomanjun.sleepdownschedule.model.CourseEntity::class,
        com.xiaomanjun.sleepdownschedule.model.ScheduleProfileEntity::class,
        com.xiaomanjun.sleepdownschedule.model.ScheduleConfigEntity::class,
        com.xiaomanjun.sleepdownschedule.model.PeriodEntity::class,
        com.xiaomanjun.sleepdownschedule.model.PeriodSchemeEntity::class,
        com.xiaomanjun.sleepdownschedule.model.PeriodSchemeTimeEntity::class,
        com.xiaomanjun.sleepdownschedule.model.PeriodSchemeLibraryMigrationEntity::class,
        com.xiaomanjun.sleepdownschedule.model.AgentDailySessionEntity::class,
        com.xiaomanjun.sleepdownschedule.model.AgentMessageEntity::class,
        com.xiaomanjun.sleepdownschedule.feature.widget.WidgetAppearanceEntity::class,
        com.xiaomanjun.sleepdownschedule.model.TodoItemEntity::class,
        com.xiaomanjun.sleepdownschedule.model.CountdownEventEntity::class
    ],
    version = APP_DATABASE_VERSION,
    exportSchema = true
)
@TypeConverters(com.xiaomanjun.sleepdownschedule.data.local.ScheduleConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): com.xiaomanjun.sleepdownschedule.data.local.CourseDao
    abstract fun configDao(): com.xiaomanjun.sleepdownschedule.data.local.ConfigDao
    abstract fun periodSchemeDao(): com.xiaomanjun.sleepdownschedule.data.local.PeriodSchemeDao
    abstract fun scheduleProfileDao(): com.xiaomanjun.sleepdownschedule.data.local.ScheduleProfileDao
    abstract fun widgetAppearanceDao(): com.xiaomanjun.sleepdownschedule.feature.widget.WidgetAppearanceDao
    abstract fun agentDao(): com.xiaomanjun.sleepdownschedule.data.local.AgentDao
    abstract fun todoDao(): com.xiaomanjun.sleepdownschedule.data.local.TodoDao
    abstract fun countdownDao(): com.xiaomanjun.sleepdownschedule.data.local.CountdownDao
}
