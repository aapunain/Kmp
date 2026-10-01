package com.self.kmp.domain.todo

/** The business model for a todo. */
public data class Todo(
    val id: Long,
    val text: String,
    val isDone: Boolean,
    val createdAt: Long,
)

/** How the list is ordered. Alphabetical is deliberately not offered. */
public enum class TodoOrder { NEWEST_FIRST, OLDEST_FIRST }
