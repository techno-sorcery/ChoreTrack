package com.example.choretracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.choretracker.ui.HomeScreen
import com.example.choretracker.ui.CTViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.choretracker.ui.ChoreScreen

import com.example.choretracker.ui.theme.ChoreTrackerTheme
import com.example.choretracker.ui.StatsScreen

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChoreTrackerTheme {
                ChoreTrackerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Composable
fun ChoreTrackerApp(
    model: CTViewModel = viewModel<CTViewModel>()
) {
    val context = LocalContext.current

    // load saved data on startup
    LaunchedEffect(Unit) {
        model.loadData(context)
    }
    // save whenever lists change
    LaunchedEffect(model.personList.size, model.choreList.size) {
        model.saveData(context)
    }

    when (model.screen) {

        AppDestinations.HOME ->
            HomeScreen(model)

        AppDestinations.CHORES ->
            ChoreScreen(model)

        AppDestinations.STATS ->
            StatsScreen(model)
    }
}







