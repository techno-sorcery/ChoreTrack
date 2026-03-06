package com.example.choretracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.choretracker.Chore
import com.example.choretracker.ChoreType
import com.example.choretracker.Person
import com.example.choretracker.R
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoreScreen(
    model: CTViewModel
) {
    var showAddChoreDialog by rememberSaveable { mutableStateOf(false) }
    var choreForAssignment by remember { mutableStateOf<Chore?>(null) }
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }

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

    val activeChores = model.choreList.filter { !it.completed }
    val completedHistoryChores = model.choreList
        .filter { it.completed || it.completionCount > 0 }
        .sortedByDescending { it.completedAtMillis ?: Long.MIN_VALUE }

    val filteredActiveChores = if (selectedTag == "All") {
        activeChores
    } else {
        activeChores.filter { it.tags.contains(selectedTag) }
    }

    val filteredCompletedChores = if (selectedTag == "All") {
        completedHistoryChores
    } else {
        completedHistoryChores.filter { it.tags.contains(selectedTag) }
    }

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 1) {
            choreForAssignment = null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { ChoreTrackerAppbar(text = "Chores", model = model) },
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

            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Active") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Completed") }
                )
            }

            if (selectedTabIndex == 0) {
                ChoreCards(
                    modifier = Modifier.fillMaxSize(),
                    chores = filteredActiveChores,
                    model = model,
                    onEditAssignment = { selectedChore ->
                        choreForAssignment = selectedChore
                    }
                )
            } else {
                CompletedChoreCards(
                    modifier = Modifier.fillMaxSize(),
                    chores = filteredCompletedChores
                )
            }
        }
    }

    if (showAddChoreDialog) {
        AddChoreDialog(
            people = model.personList,
            onDismiss = { showAddChoreDialog = false },
            onConfirm = { title, description, type, assigned, tags, dueAtMillis ->
                model.addChore(
                    title,
                    description,
                    type,
                    tags,
                    assigned,
                    dueAtMillis
                )
                showAddChoreDialog = false
            }
        )
    }

    choreForAssignment?.let { selectedChore ->
        EditChoreDialog(
            chore = selectedChore,
            people = model.personList,
            onDismiss = { choreForAssignment = null },
            onConfirm = { selectedPeople, updatedTags ->
                model.updateChoreDetails(
                    choreId = selectedChore.id,
                    assigned = selectedPeople,
                    tags = updatedTags
                )
                choreForAssignment = null
            }
        )
    }
}

@Composable
fun CompletedChoreCards(
    modifier: Modifier = Modifier,
    chores: List<Chore>
) {
    if (chores.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No completed chores yet.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = chores,
            key = { it.id }
        ) { chore ->
            CompletedChoreItemCard(
                chore = chore,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CompletedChoreItemCard(
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
            Text(
                text = chore.title.ifBlank { "Untitled chore" },
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Type: ${chore.type.displayName}",
                style = MaterialTheme.typography.bodySmall
            )

            val assignedPeopleText =
                if (chore.assigned.isEmpty())
                    "Assigned: None"
                else
                    "Assigned: ${chore.assigned.joinToString { it.name }}"

            Text(
                text = assignedPeopleText,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Times completed: ${chore.completionCount}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Last completed: ${formatCompletionDateTime(chore.completedAtMillis)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatCompletionDateTime(millis: Long?): String {
    if (millis == null) return "Unknown (older record)"

    val formatter = DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM,
        DateFormat.SHORT
    )
    return formatter.format(Date(millis))
}

private fun formatDueDate(millis: Long?): String {
    if (millis == null) return "None"
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
}

private fun isOverdue(dueAtMillis: Long): Boolean {
    val todayKey = localDayKey(System.currentTimeMillis())
    val dueKey = localDayKey(dueAtMillis)
    return dueKey < todayKey
}

private fun localDayKey(timeMillis: Long): Int {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    return calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
}

private fun normalizePickerDateToLocalNoon(pickerUtcMillis: Long): Long {
    val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    utcCalendar.timeInMillis = pickerUtcMillis

    val year = utcCalendar.get(Calendar.YEAR)
    val month = utcCalendar.get(Calendar.MONTH)
    val day = utcCalendar.get(Calendar.DAY_OF_MONTH)

    val localCalendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return localCalendar.timeInMillis
}

@Composable
fun ChoreCards(
    modifier: Modifier = Modifier,
    chores: List<Chore>,
    model: CTViewModel,
    onEditAssignment: (Chore) -> Unit
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
                onToggle = { model.toggleChore(it) },
                onEditAssignment = onEditAssignment,
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
    onConfirm: (String, String?, ChoreType, List<Person>, List<String>, Long?) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var tagsInput by rememberSaveable { mutableStateOf("") }

    var typeExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedType by rememberSaveable { mutableStateOf(ChoreType.ONE_TIME) }
    var selectedDueAtMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDueDatePicker by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Track selected household members (by index to keep it saveable)
    val selectedIndexes = rememberSaveable { mutableStateListOf<Int>() }

    fun dismissDialog() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = { dismissDialog() },
        title = { Text("Add Chore") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(scrollState)
                    .imePadding()
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

                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Tags (comma separated)") },
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

                Text(
                    text = "Due: ${formatDueDate(selectedDueAtMillis)}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { showDueDatePicker = true }
                    ) {
                        Text(
                            if (selectedDueAtMillis == null) "Set Due Date"
                            else "Change Due Date"
                        )
                    }

                    if (selectedDueAtMillis != null) {
                        TextButton(
                            onClick = { selectedDueAtMillis = null }
                        ) {
                            Text("Clear")
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
                                            if (index !in selectedIndexes) {
                                                selectedIndexes.add(index)
                                            }
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

                        val tags = tagsInput
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }

                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        onConfirm(
                                title.trim(),
                                description.takeIf { it.isNotBlank() }?.trim(),
                                selectedType,
                                assignedPeople,
                                tags,
                                selectedDueAtMillis
                            )
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = { dismissDialog() }) { Text("Cancel") }
        }
    )

    if (showDueDatePicker) {
        val datePickerState = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = datePickerState.selectedDateMillis
                        if (selected != null) {
                            selectedDueAtMillis = normalizePickerDateToLocalNoon(selected)
                        }
                        showDueDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDueDatePicker = false }
                ) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun EditChoreDialog(
    chore: Chore,
    people: List<Person>,
    onDismiss: () -> Unit,
    onConfirm: (List<Person>, List<String>) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val selectedPersonIds = remember(chore.id) {
        mutableStateListOf<Int>().apply {
            addAll(chore.assigned.map { it.id })
        }
    }
    var editableTags by remember(chore.id) {
        mutableStateOf(
            chore.tags
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
                .sortedBy { it.lowercase() }
        )
    }
    var newTagInput by rememberSaveable(chore.id) { mutableStateOf("") }
    val scrollState = rememberScrollState()

    fun dismissDialog() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = { dismissDialog() },
        title = { Text("Edit Chore") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(scrollState)
                    .imePadding()
            ) {
                Text(
                    text = chore.title.ifBlank { "Untitled chore" },
                    style = MaterialTheme.typography.titleMedium
                )

                if (people.isEmpty()) {
                    Text(
                        text = "No household members added yet.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    people.forEach { person ->
                        val checked = person.id in selectedPersonIds

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${person.emoji} ${person.name}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (person.id !in selectedPersonIds) {
                                            selectedPersonIds.add(person.id)
                                        }
                                    } else {
                                        selectedPersonIds.remove(person.id)
                                    }
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.titleMedium
                )

                if (editableTags.isEmpty()) {
                    Text(
                        text = "No tags yet",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    editableTags.forEach { tag ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            TextButton(
                                onClick = {
                                    editableTags = editableTags.filterNot { it == tag }
                                }
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    label = { Text("Add tag") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(
                    onClick = {
                        val normalized = newTagInput.trim()
                        if (
                            normalized.isNotBlank() &&
                            editableTags.none { it.equals(normalized, ignoreCase = true) }
                        ) {
                            editableTags = (editableTags + normalized)
                                .distinctBy { it.lowercase() }
                                .sortedBy { it.lowercase() }
                            newTagInput = ""
                        }
                    },
                    enabled = newTagInput.trim().isNotBlank()
                ) {
                    Text("Add Tag")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedPeople = people.filter { it.id in selectedPersonIds }
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    onConfirm(selectedPeople, editableTags)
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = { dismissDialog() }) { Text("Cancel") }
        }
    )
}

@Composable
fun ChoreItemCard(
    chore: Chore,
    onToggle: (Chore) -> Unit,
    onEditAssignment: (Chore) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onEditAssignment(chore) }
    ) {
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
                        text = "Type: ${chore.type.displayName}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    chore.dueAtMillis?.let { dueAt ->
                        val overdue = !chore.completed && isOverdue(dueAt)
                        val dueText = formatDueDate(dueAt)
                        Text(
                            text =
                                if (overdue) "Due: $dueText  (OVERDUE)"
                                else "Due: $dueText",
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                if (overdue) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

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
                    onCheckedChange = { onToggle(chore) }
                )
            }
        }
    }
}
