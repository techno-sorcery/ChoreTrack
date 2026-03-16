package com.example.choretracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.choretracker.ui.HomeScreen
import com.example.choretracker.ui.CTViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.choretracker.ui.ChoreScreen
import com.example.choretracker.ui.SettingsScreen

import com.example.choretracker.ui.theme.ChoreTrackerTheme
import com.example.choretracker.ui.StatsScreen

import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ReminderScheduler.schedule(this)
        enableEdgeToEdge()
        setContent {
            ChoreTrackerTheme {
                ChoreTrackerApp()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Composable
fun ChoreTrackerApp(
    model: CTViewModel = viewModel<CTViewModel>(
        factory = CTViewModel.Factory
    )
) {
    val context = LocalContext.current
    RequestNotificationPermissionIfNeeded()

    // load saved data on startup
    LaunchedEffect(Unit) {
        model.loadData(context)
    }
    // save whenever people/chores state changes (not only size changes)
    LaunchedEffect(model) {
        snapshotFlow {
            model.personList.toList() to model.choreList.toList()
        }.collectLatest {
            model.saveData(context)
        }
    }
    // detect recurring period changes while app is running
    LaunchedEffect(model, context) {
        while (true) {
            model.resetRecurringChoresIfNeeded(context)
            delay(60 * 60 * 1000L)
        }
    }

    when (model.screen) {

        AppDestinations.HOME ->
            HomeScreen(model)

        AppDestinations.CHORES ->
            ChoreScreen(model)

        AppDestinations.STATS ->
            StatsScreen(model)

        AppDestinations.SETTINGS ->
            SettingsScreen(model)
    }
}

@Composable
private fun RequestNotificationPermissionIfNeeded() {
    val context = LocalContext.current

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    if (!ReminderSettings.isEnabled(context)) return

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
