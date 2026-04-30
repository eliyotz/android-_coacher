package com.coach.screentime.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coach.screentime.data.db.dao.GoalDao
import com.coach.screentime.data.db.entities.GoalEntity
import com.coach.screentime.data.store.Mode
import com.coach.screentime.data.store.SettingsStore
import com.coach.screentime.data.store.Strictness
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val goalDao: GoalDao,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        settingsStore.strictness,
        settingsStore.mode,
        settingsStore.mindfulPauseSec,
        settingsStore.extensionMinutes,
        goalDao.observeActiveGoal(),
    ) { strictness, mode, pauseSec, extMin, goal ->
        SettingsUiState(strictness, mode, pauseSec, extMin, goal?.text.orEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState.Empty)

    fun setStrictness(s: Strictness) = viewModelScope.launch { settingsStore.setStrictness(s) }
    fun setMode(m: Mode) = viewModelScope.launch { settingsStore.setMode(m) }
    fun setPauseSec(s: Int) = viewModelScope.launch { settingsStore.setMindfulPauseSec(s) }
    fun setExtensionMinutes(m: Int) = viewModelScope.launch { settingsStore.setExtensionMinutes(m) }
    fun setGoal(text: String) = viewModelScope.launch {
        goalDao.deactivateAll()
        if (text.isNotBlank()) {
            goalDao.insert(GoalEntity(text = text.trim(), active = true, createdAt = System.currentTimeMillis()))
        }
    }
}

data class SettingsUiState(
    val strictness: Strictness,
    val mode: Mode,
    val pauseSec: Int,
    val extensionMinutes: Int,
    val goal: String,
) {
    companion object {
        val Empty = SettingsUiState(Strictness.BALANCED, Mode.OBSERVE, 5, 15, "")
    }
}
