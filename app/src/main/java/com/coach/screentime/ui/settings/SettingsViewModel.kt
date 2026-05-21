package com.coach.screentime.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coach.screentime.data.db.dao.GoalDao
import com.coach.screentime.data.db.entities.GoalEntity
import com.coach.screentime.data.store.Mode
import com.coach.screentime.data.store.SettingsStore
import com.coach.screentime.data.store.Strictness
import com.coach.screentime.auth.GoogleAuthRepository
import com.coach.screentime.export.JsonExporter
import com.coach.screentime.tasks.TasksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val goalDao: GoalDao,
    private val jsonExporter: JsonExporter,
    private val authRepo: GoogleAuthRepository,
    private val tasksRepo: TasksRepository,
) : ViewModel() {

    private val _googleConnectionTick = MutableStateFlow(0)
    val googleConnection: StateFlow<GoogleConnection> = _googleConnectionTick.map {
        GoogleConnection(
            configured = authRepo.isOAuthConfigured(),
            connected = authRepo.isConnected,
            email = authRepo.accountEmail,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, GoogleConnection(false, false, null))

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError = _connectionError.asStateFlow()

    fun onGoogleSignedIn(serverAuthCode: String, email: String?) {
        viewModelScope.launch {
            val result = authRepo.completeSignIn(serverAuthCode, email)
            if (result.isSuccess) {
                _connectionError.value = null
                tasksRepo.sync()
            } else {
                _connectionError.value = result.exceptionOrNull()?.message ?: "Connection failed"
            }
            _googleConnectionTick.value = _googleConnectionTick.value + 1
        }
    }

    fun clearConnectionError() { _connectionError.value = null }

    fun disconnectGoogle() {
        viewModelScope.launch {
            tasksRepo.signOut()
            tasksRepo.clearMirror()
            _googleConnectionTick.value = _googleConnectionTick.value + 1
        }
    }

    val state: StateFlow<SettingsUiState> = combine(
        settingsStore.strictness,
        settingsStore.mode,
        settingsStore.mindfulPauseSec,
        settingsStore.extensionMinutes,
        goalDao.observeActiveGoal(),
    ) { strictness, mode, pauseSec, extMin, goal ->
        SettingsUiState(strictness, mode, pauseSec, extMin, goal?.text.orEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState.Empty)

    private val _exportStatus = MutableStateFlow<ExportStatus>(ExportStatus.Idle)
    val exportStatus: StateFlow<ExportStatus> = _exportStatus.asStateFlow()

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

    fun exportTo(uri: Uri) = viewModelScope.launch {
        _exportStatus.value = ExportStatus.Running
        val result = jsonExporter.exportTo(uri)
        _exportStatus.value = result.fold(
            onSuccess = { ExportStatus.Done(it) },
            onFailure = { ExportStatus.Failed(it.message ?: "Export failed") },
        )
    }

    fun clearExportStatus() {
        _exportStatus.value = ExportStatus.Idle
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

sealed interface ExportStatus {
    data object Idle : ExportStatus
    data object Running : ExportStatus
    data class Done(val byteCount: Long) : ExportStatus
    data class Failed(val message: String) : ExportStatus
}

data class GoogleConnection(
    val configured: Boolean,
    val connected: Boolean,
    val email: String?,
)
