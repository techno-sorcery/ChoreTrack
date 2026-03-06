package com.example.choretracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.choretracker.Chore
import com.example.choretracker.ChoreType
import com.example.choretracker.Person
import com.example.choretracker.R
import kotlin.collections.forEach
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope


@Composable
fun HomeScreen(
    model: CTViewModel
) {
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var showRemovePersonDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ChoreTrackerAppbar(
                text = "Household",
                model = model
            )
        },
        bottomBar = {
            ChoreTrackerBottomBar(model)
        },
        floatingActionButton = {
            HomeActionButton(
                onClickAdd = {
                    showAddPersonDialog = true
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },

    ) { innerPadding ->
        HomeCards(
            model = model,
            modifier = Modifier
                .padding(
                    innerPadding
                )
        )
    }

    if (showAddPersonDialog) {
        AddPersonDialog(
            onDismiss = { showAddPersonDialog = false },
            onConfirm = { name, emoji ->

                val success = model.addPerson(name, emoji)

                if (success) {
                    showAddPersonDialog = false
                } else {

                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Person already exists"
                        )
                    }

                }
            }
        )
    }

    if (showRemovePersonDialog) {
        RemovePersonDialog(
            people = model.personList,
            onDismiss = { showRemovePersonDialog = false },
            onRemove = { person ->
                model.removePerson(person)
                showRemovePersonDialog = false
            }
        )
    }
}

@Composable
fun HomeCards(
    modifier: Modifier = Modifier,
    model: CTViewModel
) {

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AssignedChoresCard(
                model = model,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }

        items(
            model.personList
        ) { person ->
            PersonCard(
                person = person,
                allChores = model.choreList,
                onDeletePerson = { model.removePerson(it) },
                modifier = Modifier.fillMaxWidth()
            )
        }

    }
}

@Composable
fun PersonCard(
    person: Person,
    allChores: List<Chore>,
    onDeletePerson: (Person) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val assignedChores = allChores.filter { chore ->
        !chore.completed && chore.assigned.any { it.id == person.id }
    }

    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${person.emoji} ${person.name}",
                    style = MaterialTheme.typography.titleLarge
                )

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_remove_24),
                        contentDescription = "Delete ${person.name}"
                    )
                }
            }

            if (assignedChores.isEmpty()) {
                Text(
                    text = "No assigned chores",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                assignedChores.forEach { chore ->
                    Text(
                        text = "• ${chore.title.ifBlank { "Untitled chore" }} (${chore.type.displayName})",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete person?") },
            text = { Text("Remove ${person.name} from the household?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePerson(person)
                        showDeleteDialog = false
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun RemovePersonDialog(
    people: List<Person>,
    onDismiss: () -> Unit,
    onRemove: (Person) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove Person") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                people.forEach { person ->
                    TextButton(
                        onClick = { onRemove(person) }
                    ) {
                        Text(person.name)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddPersonDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🙂") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add New Person")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text("Choose Emoji")

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()

                ) {
                    listOf("🙂","😎","🔥","💪","👑","🐐","🎯","⚡").forEach {

                        TextButton(
                            onClick = { emoji = it }
                        ) {
                            Text(it)
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), emoji)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AssignedChoresCard(
    model: CTViewModel,
    modifier: Modifier = Modifier
) {
    val openChores = model.choreList.count { !it.completed }
    val oneTimeOpen = model.choreList.count {
        !it.completed && it.type == ChoreType.ONE_TIME
    }
    val dailyOpen = model.choreList.count {
        !it.completed && it.type == ChoreType.DAILY
    }
    val weeklyOpen = model.choreList.count {
        !it.completed && it.type == ChoreType.WEEKLY
    }
    val monthlyOpen = model.choreList.count {
        !it.completed && it.type == ChoreType.MONTHLY
    }

    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Assigned Chores",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Open: $openChores",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "One-Time: $oneTimeOpen",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Daily: $dailyOpen",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Weekly: $weeklyOpen",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Monthly: $monthlyOpen",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun HomeActionButton(
    onClickAdd: () -> Unit
) {
    FloatingActionButton(
        onClick = onClickAdd
    ) {
        Icon(
            painter = painterResource(R.drawable.outline_add_24),
            contentDescription = "Add Person"
        )
    }
}
