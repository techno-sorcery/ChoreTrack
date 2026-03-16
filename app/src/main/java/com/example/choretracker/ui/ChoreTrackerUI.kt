package com.example.choretracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.choretracker.AppDestinations
import com.example.choretracker.R




@Composable
fun BottomBarClickable(
    text: String,
    icon: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = text
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text)
    }
}


@Composable
fun ChoreTrackerBottomBar (
    model: CTViewModel
) {
    BottomAppBar(
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                BottomBarClickable(
                    text = "Household",
                    icon = R.drawable.outline_home_24,
                    onClick = {
                        model.screen = AppDestinations.HOME
                    }
                )

                BottomBarClickable(
                    text = "Chores",
                    icon = R.drawable.outline_checklist_24,
                    onClick = {
                        model.screen = AppDestinations.CHORES
                    }
                )

                BottomBarClickable(
                    text = "Stats",
                    icon = R.drawable.outline_bar_chart_24,
                    onClick = {
                        model.screen = AppDestinations.STATS
                    }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoreTrackerAppbar(
    text: String,
    model: CTViewModel? = null
) {
    CenterAlignedTopAppBar(
        title = {
            Text (
                text = text,
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center
            )
        },
        actions = {
            val shouldShowSettings = model != null && model.screen != AppDestinations.SETTINGS
            if (shouldShowSettings) {
                IconButton(
                    onClick = { model.screen = AppDestinations.SETTINGS }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.outline_settings_24),
                        contentDescription = "Settings"
                    )
                }
            }
        },
        colors = TopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            scrolledContainerColor = MaterialTheme.colorScheme.primary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            subtitleContentColor = MaterialTheme.colorScheme.onPrimary
        ),

    )
}
