package com.coach.screentime.ui.onboarding

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coach.screentime.data.store.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(buildState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun refresh() { _state.value = buildState() }

    fun finishOnboarding() = viewModelScope.launch {
        settingsStore.setObserveStart(System.currentTimeMillis())
        settingsStore.setOnboarded(true)
    }

    private fun buildState(): OnboardingUiState = OnboardingUiState(
        hasUsageAccess = hasUsageAccess(),
        hasOverlay = hasOverlay(),
        hasNotifications = hasNotifications(),
    )

    private fun hasUsageAccess(): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasOverlay(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true

    private fun hasNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

data class OnboardingUiState(
    val hasUsageAccess: Boolean,
    val hasOverlay: Boolean,
    val hasNotifications: Boolean,
) {
    val ready: Boolean get() = hasUsageAccess && hasOverlay && hasNotifications
}
