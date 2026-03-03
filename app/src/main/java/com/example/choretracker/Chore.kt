package com.example.choretracker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

var lastChoreId = 0

data class Chore(

val id: Int = lastChoreId++,
    val type: ChoreType = ChoreType.ONE_TIME,
    val title: String = "",
    val description: String? = null,
    val tags: List<String> = listOf(),
    val assigned: List<Person> = listOf()
) {
    var completed: Boolean by mutableStateOf(false)

}
