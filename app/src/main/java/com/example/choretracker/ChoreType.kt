package com.example.choretracker

enum class ChoreType(val displayName: String) {
    ONE_TIME("One-Time"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    override fun toString(): String = displayName
}