package com.example.choretracker

enum class ChoreType(val displayName: String) {
    ONE_TIME("One-Time"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    override fun toString(): String = displayName
}