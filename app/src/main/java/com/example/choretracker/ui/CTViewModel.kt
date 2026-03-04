package com.example.choretracker.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.choretracker.AppDestinations
import com.example.choretracker.Chore
import com.example.choretracker.ChoreType
import com.example.choretracker.Person
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CTViewModel : ViewModel() {

    var screen by mutableStateOf(AppDestinations.HOME)

    val choreList = mutableStateListOf<Chore>()
    val personList = mutableStateListOf<Person>()

    private var nextPersonId = 0
    private var nextChoreId = 1

    private val gson = Gson()

    fun addPerson(nameRaw: String, emoji: String = "🙂"): Boolean {
        val name = nameRaw.trim()
        if (
            name.isBlank() ||
            personList.any { it.name.equals(name, ignoreCase = true) }
        ) return false

        personList.add(
            Person(
                id = nextPersonId++,
                name = name,
                emoji = emoji
            )
        )
        return true
    }

    fun removePerson(person: Person) {
        personList.remove(person)

        // remove that person from any chore assignments
        // (keep chores; just unassign them)
        for (i in choreList.indices) {
            val c = choreList[i]
            if (c.assigned.any { it.id == person.id }) {
                val newAssigned = c.assigned.filterNot { it.id == person.id }
                choreList[i] = c.copy(assigned = newAssigned)
            }
        }
    }

    fun toggleChore(chore: Chore) {
        when (chore.type) {
            ChoreType.ONE_TIME -> {
                if (!chore.completed) {
                    chore.completed = true
                    choreList.remove(chore)
                }
            }
            ChoreType.WEEKLY,
            ChoreType.MONTHLY -> chore.completed = !chore.completed
            else -> chore.completed = !chore.completed
        }
    }

    fun addChore(
        titleRaw: String,
        descriptionRaw: String?,
        type: ChoreType,
        tags: List<String>,
        assigned: List<Person>
    ): Boolean {
        val title = titleRaw.trim()
        if (title.isBlank()) return false

        val desc = descriptionRaw?.trim()?.takeIf { it.isNotBlank() }

        // normalize tags: trim, drop blanks, de-dup (case-insensitive-ish)
        val normalizedTags = tags
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sorted()

        choreList.add(
            Chore(
                id = nextChoreId++,
                title = title,
                description = desc,
                type = type,
                tags = normalizedTags,
                assigned = assigned,
                completed = false
            )
        )
        return true
    }

    fun resetWeeklyChoresIfNeeded(context: Context) {

        val prefs = context.getSharedPreferences("choretracker", Context.MODE_PRIVATE)

        val calendar = java.util.Calendar.getInstance()
        val currentWeek = calendar.get(java.util.Calendar.WEEK_OF_YEAR)
        val currentYear = calendar.get(java.util.Calendar.YEAR)

        val lastWeek = prefs.getInt("weekly_reset_week", -1)
        val lastYear = prefs.getInt("weekly_reset_year", -1)

        // If the stored week/year differs from the current one, perform reset
        if (currentWeek != lastWeek || currentYear != lastYear) {

            for (i in choreList.indices) {
                val c = choreList[i]

                if (c.type == ChoreType.WEEKLY) {
                    choreList[i] = c.copy(completed = false)
                }
            }

            prefs.edit()
                .putInt("weekly_reset_week", currentWeek)
                .putInt("weekly_reset_year", currentYear)
                .apply()
        }
    }

    fun saveData(context: Context) {

        val prefs = context.getSharedPreferences("choretracker", Context.MODE_PRIVATE)

        prefs.edit()
            .putString("people", gson.toJson(personList))
            .putString("chores", gson.toJson(choreList))
            .apply()
    }

    fun loadData(context: Context) {

        val prefs = context.getSharedPreferences("choretracker", Context.MODE_PRIVATE)

        val peopleJson = prefs.getString("people", null)
        val choresJson = prefs.getString("chores", null)

        if (peopleJson != null) {
            val type = object : TypeToken<MutableList<Person>>() {}.type
            personList.clear()
            personList.addAll(gson.fromJson(peopleJson, type))
        }

        if (choresJson != null) {
            val type = object : TypeToken<MutableList<Chore>>() {}.type
            choreList.clear()
            choreList.addAll(gson.fromJson(choresJson, type))
        }

        nextPersonId =
            if (personList.isEmpty()) 0
            else personList.maxOf { it.id } + 1

        nextChoreId =
            if (choreList.isEmpty()) 1
            else choreList.maxOf { it.id } + 1

        resetWeeklyChoresIfNeeded(context)
    }
}