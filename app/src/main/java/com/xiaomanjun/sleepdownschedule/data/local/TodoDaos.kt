package com.xiaomanjun.sleepdownschedule.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xiaomanjun.sleepdownschedule.model.CountdownEventEntity
import com.xiaomanjun.sleepdownschedule.model.TodoItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_items")
    fun observeAll(): Flow<List<TodoItemEntity>>

    @Query("SELECT * FROM todo_items WHERE id = :id")
    suspend fun get(id: Long): TodoItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: TodoItemEntity): Long

    @Query("UPDATE todo_items SET doneAt = :doneAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setDone(id: Long, doneAt: Long?, updatedAt: Long)

    @Query("DELETE FROM todo_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM todo_items WHERE doneAt IS NOT NULL")
    suspend fun deleteCompleted()
}

@Dao
interface CountdownDao {
    @Query("SELECT * FROM countdown_events")
    fun observeAll(): Flow<List<CountdownEventEntity>>

    @Query("SELECT * FROM countdown_events WHERE id = :id")
    suspend fun get(id: Long): CountdownEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: CountdownEventEntity): Long

    @Query("DELETE FROM countdown_events WHERE id = :id")
    suspend fun delete(id: Long)
}
