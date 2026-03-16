package com.example.choretracker.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.choretracker.ReminderScheduler
import com.example.choretracker.ReminderSettings

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    model: CTViewModel
) {
    val context = LocalContext.current
    var remindersEnabled by rememberSaveable {
        mutableStateOf(ReminderSettings.isEnabled(context))
    }
    var reminderHour by rememberSaveable {
        mutableIntStateOf(ReminderSettings.reminderHour(context))
    }
    var hourExpanded by rememberSaveable { mutableStateOf(false) }
    var savedMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    val notificationsGranted =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { ChoreTrackerAppbar(text = "Settings") },
        bottomBar = { ChoreTrackerBottomBar(model) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Reminders",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Daily notification for open chores",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Switch(
                                checked = remindersEnabled,
                                onCheckedChange = {
                                    remindersEnabled = it
                                    savedMessage = null
                                }
                            )
                        }

                        if (remindersEnabled) {
                            ExposedDropdownMenuBox(
                                expanded = hourExpanded,
                                onExpandedChange = { hourExpanded = !hourExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = formatReminderHour(reminderHour),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Reminder time") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = hourExpanded)
                                    },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )

                                ExposedDropdownMenu(
                                    expanded = hourExpanded,
                                    onDismissRequest = { hourExpanded = false }
                                ) {
                                    (0..23).forEach { hour ->
                                        DropdownMenuItem(
                                            text = { Text(formatReminderHour(hour)) },
                                            onClick = {
                                                reminderHour = hour
                                                hourExpanded = false
                                                savedMessage = null
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (remindersEnabled && !notificationsGranted) {
                            Text(
                                text = "Notifications permission is required for reminders.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(
                                onClick = {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            ) {
                                Text("Grant permission")
                            }
                        }

                        TextButton(
                            onClick = {
                                ReminderSettings.save(
                                    context = context,
                                    enabled = remindersEnabled,
                                    hour = reminderHour
                                )
                                ReminderScheduler.schedule(context)
                                savedMessage = "Settings saved"
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save")
                        }

                        savedMessage?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
private fun formatReminderHour(hour24: Int): String {
    val clamped = hour24.coerceIn(0, 23)
    val amPm = if (clamped < 12) "AM" else "PM"
    val hour12 = when (val h = clamped % 12) {
        0 -> 12
        else -> h
    }
    return String.format("%d:00 %s", hour12, amPm)
}
