package com.self.kmp.database.internal

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface TodoDao {
    @Query("SELECT * FROM todo ORDER BY createdAt DESC")
    fun observeNewestFirst(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todo ORDER BY createdAt ASC")
    fun observeOldestFirst(): Flow<List<TodoEntity>>

    @Insert
    suspend fun insert(todo: TodoEntity)

    @Query("UPDATE todo SET isDone = :isDone WHERE id = :id")
    suspend fun setDone(
        id: Long,
        isDone: Boolean,
    )

    @Query("DELETE FROM todo WHERE id = :id")
    suspend fun delete(id: Long)
}
