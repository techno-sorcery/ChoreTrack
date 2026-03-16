package com.example.choretracker.ui

import QuoteUiState
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.choretracker.MainApplication
import com.example.choretracker.QuoteRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.content.edit

class CTViewModel(
    private val quoteRepository: QuoteRepository
) : ViewModel() {

    var quoteState: QuoteUiState
            by mutableStateOf(QuoteUiState.Loading)
        private set
    var screen by mutableStateOf(AppDestinations.HOME)

    val choreList = mutableStateListOf<Chore>()
    val personList = mutableStateListOf<Person>()

    private var nextPersonId = 0
    private var nextChoreId = 1

    private val gson = Gson()

    companion object {

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {

                val application =
                    this[APPLICATION_KEY] as MainApplication

                CTViewModel(application.quoteRepository)
            }
        }
    }
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

    fun fetchQuote() {
        viewModelScope.launch(Dispatchers.IO) {

            quoteState = try {

                val quote = quoteRepository.getRandomQuote()

                if (quote != null)
                    QuoteUiState.Success(quote)
                else
                    QuoteUiState.Error("No quote returned")

            } catch (e: Exception) {
                QuoteUiState.Error(e.toString())
            }
        }
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
        val index = choreList.indexOfFirst { it.id == chore.id }
        if (index == -1) return

        val existing = choreList[index]
        if (existing.type == ChoreType.ONE_TIME && existing.completed) return

        val isCompletingNow = !existing.completed
        val nextCount =
            if (isCompletingNow) existing.completionCount + 1
            else existing.completionCount
        val completedAt =
            if (isCompletingNow) System.currentTimeMillis()
            else existing.completedAtMillis

        choreList[index] = existing.copy(
            completed = !existing.completed,
            completionCount = nextCount,
            completedAtMillis = completedAt
        )
    }

    fun updateChoreDetails(
        choreId: Int,
        assigned: List<Person>,
        tags: List<String>
    ) {
        val index = choreList.indexOfFirst { it.id == choreId }
        if (index == -1) return

        val existing = choreList[index]
        choreList[index] = existing.copy(
            assigned = assigned,
            tags = normalizeTags(tags)
        )
    }

    fun addChore(
        titleRaw: String,
        descriptionRaw: String?,
        type: ChoreType,
        tags: List<String>,
        assigned: List<Person>,
        dueAtMillis: Long? = null
    ): Boolean {
        val title = titleRaw.trim()
        if (title.isBlank()) return false

        val desc = descriptionRaw?.trim()?.takeIf { it.isNotBlank() }

        val normalizedTags = normalizeTags(tags)

        choreList.add(
            Chore(
                id = nextChoreId++,
                title = title,
                description = desc,
                type = type,
                tags = normalizedTags,
                assigned = assigned,
                dueAtMillis = dueAtMillis,
                completed = false
            )
        )
        return true
    }

    fun resetRecurringChoresIfNeeded(context: Context) {

        val prefs = context.getSharedPreferences("choretracker", Context.MODE_PRIVATE)

        val calendar = java.util.Calendar.getInstance()
        val currentDayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val currentWeek = calendar.get(java.util.Calendar.WEEK_OF_YEAR)
        val currentMonth = calendar.get(java.util.Calendar.MONTH)
        val currentYear = calendar.get(java.util.Calendar.YEAR)

        val lastDayOfYear = prefs.getInt("daily_reset_day_of_year", -1)
        val lastDayYear = prefs.getInt("daily_reset_year", -1)
        val lastWeek = prefs.getInt("weekly_reset_week", -1)
        val lastWeekYear = prefs.getInt("weekly_reset_year", -1)
        val lastMonth = prefs.getInt("monthly_reset_month", -1)
        val lastMonthYear = prefs.getInt("monthly_reset_year", -1)

        val shouldResetDaily =
            currentDayOfYear != lastDayOfYear || currentYear != lastDayYear
        val shouldResetWeekly =
            currentWeek != lastWeek || currentYear != lastWeekYear
        val shouldResetMonthly =
            currentMonth != lastMonth || currentYear != lastMonthYear

        // if any recurring period changed, reset those chores to open.
        if (shouldResetDaily || shouldResetWeekly || shouldResetMonthly) {

            for (i in choreList.indices) {
                val c = choreList[i]

                val shouldResetThisChore =
                    (c.type == ChoreType.DAILY && shouldResetDaily) ||
                    (c.type == ChoreType.WEEKLY && shouldResetWeekly) ||
                    (c.type == ChoreType.MONTHLY && shouldResetMonthly)

                if (shouldResetThisChore) {
                    choreList[i] = c.copy(completed = false)
                }
            }

            prefs.edit {
                putInt("daily_reset_day_of_year", currentDayOfYear)
                    .putInt("daily_reset_year", currentYear)
                    .putInt("weekly_reset_week", currentWeek)
                    .putInt("weekly_reset_year", currentYear)
                    .putInt("monthly_reset_month", currentMonth)
                    .putInt("monthly_reset_year", currentYear)
            }
        }
    }

    fun saveData(context: Context) {

        val prefs = context.getSharedPreferences("choretracker", Context.MODE_PRIVATE)

        prefs.edit {
            putString("people", gson.toJson(personList))
                .putString("chores", gson.toJson(choreList))
        }
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

        // migration safety: old saved chores may have completed=true but completionCount=0.
        for (i in choreList.indices) {
            val chore = choreList[i]
            if (chore.completed && chore.completionCount == 0) {
                choreList[i] = chore.copy(completionCount = 1)
            }
        }

        nextPersonId =
            if (personList.isEmpty()) 0
            else personList.maxOf { it.id } + 1

        nextChoreId =
            if (choreList.isEmpty()) 1
            else choreList.maxOf { it.id } + 1

        resetRecurringChoresIfNeeded(context)
    }

    private fun normalizeTags(tags: List<String>): List<String> {
        return tags
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sorted()
    }
}
