package com.self.kmp.database.internal

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "todo")
internal data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val isDone: Boolean,
    val createdAt: Long,
)
