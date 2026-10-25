package com.xiaomanjun.sleepdownschedule.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 四象限。存储用枚举名，读取失败回退到「重要且紧急」，单条脏数据不拖垮整页。 */
enum class TodoQuadrant(val label: String) {
    IMPORTANT_URGENT("重要且紧急"),
    IMPORTANT_NOT_URGENT("重要不紧急"),
    URGENT_NOT_IMPORTANT("紧急不重要"),
    NEITHER("不重要不紧急");

    companion object {
        fun fromStorage(value: String?): TodoQuadrant =
            entries.firstOrNull { it.name == value } ?: IMPORTANT_URGENT
    }
}

/** 本地待办，不参与 .sleepdown 备份格式（v1）。 */
@Entity(
    tableName = "todo_items",
    indices = [
        Index("doneAt"),
        Index(value = ["quadrant", "doneAt", "dueDate"])
    ]
)
data class TodoItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val quadrant: String = TodoQuadrant.IMPORTANT_URGENT.name,
    /** ISO yyyy-MM-dd，null = 无截止 */
    val dueDate: String? = null,
    /** null = 未完成 */
    val doneAt: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
) {
    val quadrantValue: TodoQuadrant get() = TodoQuadrant.fromStorage(quadrant)
    val isDone: Boolean get() = doneAt != null
}

/** 倒数日事件，日期按用户所在时区的日历日解释，不跨时区换算。 */
@Entity(
    tableName = "countdown_events",
    indices = [Index("targetDate")]
)
data class CountdownEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** ISO yyyy-MM-dd */
    val targetDate: String,
    /** HH:mm，null = 全天 */
    val targetTime: String? = null,
    val repeatYearly: Boolean = false,
    val pinned: Boolean = false,
    val note: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)
