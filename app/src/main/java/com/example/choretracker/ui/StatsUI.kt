package com.example.choretracker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StatsScreen(
    model: CTViewModel
) {

    val totalChores = model.choreList.size
    val completedChores = model.choreList.count { it.completed }
    val totalCompletions = model.choreList.sumOf { it.completionCount }

    val completionRate =
        if (totalChores == 0) 0
        else (completedChores * 100) / totalChores

    val leaderboard =
        model.personList.map { person ->

            val completed =
                model.choreList
                    .filter { chore ->
                        chore.assigned.any { assigned -> assigned.id == person.id }
                    }
                    .sumOf { it.completionCount }

            val assignedTotal =
                model.choreList.count { chore ->
                    chore.assigned.any { assigned -> assigned.id == person.id }
                }

            Triple(person, completed, assignedTotal)
        }
            .filter { it.second > 0 } // only people who completed chores
            .sortedByDescending { it.second }

    Scaffold(
        topBar = { ChoreTrackerAppbar("Stats", model = model) },
        bottomBar = { ChoreTrackerBottomBar(model) }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // household stats card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        Text(
                            text = "Household Stats",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text(
                            text = "Total Chores: $totalChores",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = "Completed: $completedChores",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = "Total Completion Events: $totalCompletions",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = "Completion Rate: $completionRate%",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        val progress =
                            if (totalChores == 0) 0f
                            else completedChores.toFloat() / totalChores

                        val animatedProgress =
                            animateFloatAsState(progress).value

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Leaderboard",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            if (leaderboard.isEmpty()) {
                item {
                    Text(
                        text = "No completed chores yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            itemsIndexed(leaderboard) { index, entry ->

                val person = entry.first
                val completed = entry.second
                val assignedTotal = entry.third

                val progress =
                    if (assignedTotal == 0) 0f
                    else (completed.toFloat() / assignedTotal).coerceAtMost(1f)

                val animatedProgress =
                    animateFloatAsState(
                        targetValue = progress
                    ).value

                val medalEmoji =
                    when (index) {
                        0 -> "🥇"
                        1 -> "🥈"
                        2 -> "🥉"
                        else -> "${index + 1}."
                    }

                val medalColor =
                    when (index) {
                        0 -> Color(0xFFFFD700)
                        1 -> Color(0xFFC0C0C0)
                        2 -> Color(0xFFCD7F32)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }

                Card(
                    colors =
                        if (index == 0)
                            CardDefaults.cardColors(
                                containerColor = medalColor.copy(alpha = 0.25f)
                            )
                        else
                            CardDefaults.cardColors(),

                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {

                        Row {

                            Text(
                                text = medalEmoji,
                                style = MaterialTheme.typography.titleLarge
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = person.name,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        Text(
                            text = "Completions: $completed (Assigned chores: $assignedTotal)",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
