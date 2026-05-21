package com.coach.screentime.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.coach.screentime.data.AppRegistry
import com.coach.screentime.data.store.SettingsStore
import com.coach.screentime.tracking.ForegroundAppService
import com.coach.screentime.ui.onboarding.OnboardingScreen
import com.coach.screentime.ui.theme.ScreenTimeCoachTheme
import com.coach.screentime.work.WorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsStore: SettingsStore
    @Inject lateinit var appRegistry: AppRegistry
    @Inject lateinit var workScheduler: WorkScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScreenTimeCoachTheme { Root() }
        }
        lifecycleScope.launch {
            if (settingsStore.onboarded.first()) {
                bootstrapPostOnboarding()
            }
        }
    }

    private fun bootstrapPostOnboarding() {
        ForegroundAppService.start(this)
        workScheduler.scheduleAll()
        lifecycleScope.launch { appRegistry.refreshInstalledApps() }
    }

    @Composable
    private fun Root() {
        val onboarded by settingsStore.onboarded.collectAsState(initial = false)
        if (onboarded) {
            MainNavigation()
        } else {
            OnboardingScreen(onDone = { bootstrapPostOnboarding() })
        }
    }
}
