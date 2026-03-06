package com.example.choretracker

data class Chore(
    val id: Int,
    val type: ChoreType = ChoreType.ONE_TIME,
    val title: String = "",
    val description: String? = null,
    val tags: List<String> = listOf(),
    val assigned: List<Person> = listOf(),
    val dueAtMillis: Long? = null,
    val completionCount: Int = 0,
    val completedAtMillis: Long? = null,
    var completed: Boolean = false
)
