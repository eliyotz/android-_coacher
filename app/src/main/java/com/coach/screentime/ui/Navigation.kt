package com.coach.screentime.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.coach.screentime.ui.insights.InsightsScreen
import com.coach.screentime.ui.limits.LimitsScreen
import com.coach.screentime.ui.settings.SettingsScreen
import com.coach.screentime.ui.tasks.TasksScreen
import com.coach.screentime.ui.today.TodayScreen

private enum class Tab(val route: String, val label: String) {
    Today("today", "Today"),
    Tasks("tasks", "Tasks"),
    Insights("insights", "Insights"),
    Limits("limits", "Limits"),
    Settings("settings", "Settings"),
}

@Composable
fun MainNavigation() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.values().forEach { t ->
                    NavigationBarItem(
                        selected = currentRoute == t.route,
                        onClick = {
                            if (currentRoute != t.route) {
                                nav.navigate(t.route) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(iconFor(t), contentDescription = null) },
                        label = { Text(t.label) },
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Tab.Today.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.Today.route) { TodayScreen() }
            composable(Tab.Tasks.route) { TasksScreen() }
            composable(Tab.Insights.route) { InsightsScreen() }
            composable(Tab.Limits.route) { LimitsScreen() }
            composable(Tab.Settings.route) { SettingsScreen() }
        }
    }
}

@Composable
private fun iconFor(t: Tab) = when (t) {
    Tab.Today -> Icons.Filled.Today
    Tab.Tasks -> Icons.Filled.CheckCircle
    Tab.Insights -> Icons.Filled.Insights
    Tab.Limits -> Icons.Filled.Schedule
    Tab.Settings -> Icons.Filled.Settings
}
