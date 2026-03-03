package com.example.choretracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoreScreen(
    model: CTViewModel
) {
    var showAddChoreDialog by rememberSaveable { mutableStateOf(false) }

    var tagExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedTag by rememberSaveable { mutableStateOf("All") }

    // Build dropdown options: "All" + unique tags from chores
    val allTags = model.choreList
        .flatMap { it.tags }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    val tagOptions = listOf("All") + allTags

    val filteredChores = if (selectedTag == "All") {
        model.choreList
    } else {
        model.choreList.filter { it.tags.contains(selectedTag) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { ChoreTrackerAppbar(text = "Chores") },
        bottomBar = { ChoreTrackerBottomBar(model) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddChoreDialog = true }) {
                Icon(
                    painter = painterResource(R.drawable.outline_add_24),
                    contentDescription = "Add Chore"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Dropdown at top
            ExposedDropdownMenuBox(
                expanded = tagExpanded,
                onExpandedChange = { tagExpanded = !tagExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                OutlinedTextField(
                    value = selectedTag,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filter by tag") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = tagExpanded,
                    onDismissRequest = { tagExpanded = false }
                ) {
                    tagOptions.forEach { tag ->
                        DropdownMenuItem(
                            text = { Text(tag) },
                            onClick = {
                                selectedTag = tag
                                tagExpanded = false
                            }
                        )
                    }
                }
            }

            ChoreCards(
                modifier = Modifier.fillMaxSize(),
                chores = filteredChores,
            )
        }
    }

    if (showAddChoreDialog) {
        AddChoreDialog(
            people = model.personList,
            onDismiss = { showAddChoreDialog = false },
            onConfirm = { chore ->
                model.choreList.add(chore)
                showAddChoreDialog = false
            }
        )
    }
}

@Composable
fun ChoreCards(
    modifier: Modifier = Modifier,
    chores: List<Chore>
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = chores,
            key = { it.id }
        ) { chore ->
            ChoreItemCard(
                chore = chore,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChoreDialog(
    people: List<Person>,
    onDismiss: () -> Unit,
    onConfirm: (Chore) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }

    var typeExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedType by rememberSaveable { mutableStateOf(ChoreType.ONE_TIME) }

    // Track selected household members (by index to keep it saveable)
    val selectedIndexes = rememberSaveable { mutableStateListOf<Int>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Chore") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedType.name.replace('_', ' '),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        ChoreType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name.replace('_', ' ')) },
                                onClick = {
                                    selectedType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                // --- Assignees section ---
                Text(
                    text = "Assign to",
                    style = MaterialTheme.typography.titleMedium
                )

                if (people.isEmpty()) {
                    Text(
                        text = "No household members added",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        people.forEachIndexed { index, person ->
                            val checked = index in selectedIndexes

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = person.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) {
                                            if (index !in selectedIndexes) selectedIndexes.add(index)
                                        } else {
                                            selectedIndexes.remove(index)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        val assignedPeople = selectedIndexes
                            .sorted()
                            .mapNotNull { idx -> people.getOrNull(idx) }

                        onConfirm(
                            Chore(
                                title = title.trim(),
                                description = description.takeIf { it.isNotBlank() }?.trim(),
                                type = selectedType,
                                assigned = assignedPeople
                            )
                        )
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ChoreItemCard(
    chore: Chore,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text = chore.title.ifBlank { "Untitled chore" },
                        style = MaterialTheme.typography.titleLarge
                    )

                    chore.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Text(
                        text = "Type: ${chore.type}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // Assigned People
                    val assignedPeopleText =
                        if (chore.assigned.isEmpty())
                            "Assigned: None"
                        else
                            "Assigned: ${chore.assigned.joinToString { it.name }}"

                    Text(
                        text = assignedPeopleText,
                        style = MaterialTheme.typography.bodySmall
                    )

                    // Tags
                    val tagsText =
                        if (chore.tags.isEmpty())
                            "Tags: None"
                        else
                            "Tags: ${chore.tags.joinToString()}"

                    Text(
                        text = tagsText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Checkbox(
                    checked = chore.completed,
                    onCheckedChange = { isChecked ->
                        chore.completed = isChecked
                    }
                )
            }
        }
    }
}