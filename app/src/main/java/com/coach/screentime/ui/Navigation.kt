package com.coach.screentime.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.coach.screentime.R
import com.coach.screentime.ui.insights.InsightsScreen
import com.coach.screentime.ui.limits.LimitsScreen
import com.coach.screentime.ui.settings.SettingsScreen
import com.coach.screentime.ui.tasks.TasksScreen
import com.coach.screentime.ui.theme.AccentTerracotta
import com.coach.screentime.ui.theme.Card
import com.coach.screentime.ui.theme.CoachType
import com.coach.screentime.ui.theme.Ink
import com.coach.screentime.ui.theme.InkMute
import com.coach.screentime.ui.theme.Rule
import com.coach.screentime.ui.today.TodayScreen

private enum class Tab(val route: String, val label: String, @DrawableRes val icon: Int) {
    Today("today",       "Today",    R.drawable.ic_today),
    Tasks("tasks",       "Tasks",    R.drawable.ic_tasks),
    Insights("insights", "Insights", R.drawable.ic_insights),
    Limits("limits",     "Limits",   R.drawable.ic_limits),
    Settings("settings", "Setup",    R.drawable.ic_settings),
}

@Composable
fun MainNavigation() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Card,
                modifier = Modifier
                    .drawBehind {
                        drawLine(
                            color = Rule,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx(),
                        )
                    },
            ) {
                Tab.values().forEach { t ->
                    val selected = currentRoute == t.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != t.route) {
                                nav.navigate(t.route) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Box(contentAlignment = Alignment.BottomCenter) {
                                Icon(
                                    painter = painterResource(t.icon),
                                    contentDescription = null,
                                    tint = if (selected) AccentTerracotta else InkMute,
                                )
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(4.dp)
                                            .background(AccentTerracotta, CircleShape),
                                    )
                                }
                            }
                        },
                        label = {
                            Text(
                                t.label,
                                style = CoachType.meta,
                                color = if (selected) AccentTerracotta else InkMute,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                            selectedIconColor = AccentTerracotta,
                            unselectedIconColor = InkMute,
                            selectedTextColor = AccentTerracotta,
                            unselectedTextColor = InkMute,
                        ),
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
            composable(Tab.Today.route)    { TodayScreen() }
            composable(Tab.Tasks.route)    { TasksScreen() }
            composable(Tab.Insights.route) { InsightsScreen() }
            composable(Tab.Limits.route)   { LimitsScreen() }
            composable(Tab.Settings.route) { SettingsScreen() }
        }
    }
}
